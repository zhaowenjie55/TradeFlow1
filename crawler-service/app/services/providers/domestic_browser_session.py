from __future__ import annotations

import logging
import os
import random
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Optional

from playwright.sync_api import Error as PlaywrightError
from playwright.sync_api import Browser
from playwright.sync_api import BrowserContext
from playwright.sync_api import Page
from playwright.sync_api import Playwright
from playwright.sync_api import sync_playwright


logger = logging.getLogger(__name__)


SESSION_IDLE_TTL_SECONDS = int(os.getenv("PLAYWRIGHT_SESSION_IDLE_TTL_SECONDS", "900") or "900")
ANTI_BOT_COOLDOWN_SECONDS = int(os.getenv("PLAYWRIGHT_ANTI_BOT_COOLDOWN_SECONDS", "25") or "25")
NAVIGATION_JITTER_MIN_MS = int(os.getenv("PLAYWRIGHT_NAVIGATION_JITTER_MIN_MS", "800") or "800")
NAVIGATION_JITTER_MAX_MS = int(os.getenv("PLAYWRIGHT_NAVIGATION_JITTER_MAX_MS", "1800") or "1800")
PROFILE_LOCK_ERROR_MARKERS = (
    "ProcessSingleton",
    "SingletonLock",
    "profile is already in use",
)
STEALTH_INIT_SCRIPT = """
(() => {
  const patch = (obj, key, value) => {
    try {
      Object.defineProperty(obj, key, {
        get: () => value,
        configurable: true,
      });
    } catch (_) {
      // Ignore non-configurable surfaces.
    }
  };

  patch(Navigator.prototype, 'webdriver', undefined);
  patch(navigator, 'webdriver', undefined);
  patch(navigator, 'platform', 'MacIntel');
  patch(navigator, 'hardwareConcurrency', 8);
  patch(navigator, 'deviceMemory', 8);
  patch(navigator, 'languages', ['zh-CN', 'zh', 'en-US', 'en']);
  patch(navigator, 'plugins', [1, 2, 3, 4, 5]);

  window.chrome = window.chrome || { runtime: {}, app: {} };

  const originalQuery = window.navigator.permissions && window.navigator.permissions.query;
  if (originalQuery) {
    window.navigator.permissions.query = (parameters) => (
      parameters && parameters.name === 'notifications'
        ? Promise.resolve({ state: Notification.permission })
        : originalQuery(parameters)
    );
  }
})();
"""


@dataclass
class DomesticBrowserSession:
    playwright: Playwright
    context: BrowserContext
    page: Page
    browser: Optional[Browser]
    mode: str
    storage_state_path: str
    last_used_at: float
    cooldown_until: float = 0.0
    verification_required: bool = False
    last_verification_url: str | None = None


class DomesticBrowserSessionManager:
    def __init__(self) -> None:
        self._session: DomesticBrowserSession | None = None
        self._last_navigation_at = 0.0

    def acquire(
        self,
        *,
        cdp_url: str,
        user_data_dir: str,
        launch_kwargs: dict[str, Any],
        context_kwargs: dict[str, Any],
        storage_state_path: str,
    ) -> tuple[BrowserContext, Page]:
        now = time.monotonic()
        self._close_if_idle(now)

        if self._session is None or not self._is_usable(self._session):
            self.invalidate()
            self._session = self._start_session(
                cdp_url=cdp_url,
                user_data_dir=user_data_dir,
                launch_kwargs=launch_kwargs,
                context_kwargs=context_kwargs,
                storage_state_path=storage_state_path,
            )
            logger.info("domestic browser session started mode=%s", self._session.mode)
        else:
            self._refresh_page_reference(self._session)

        self._apply_pacing(now)
        self._session.last_used_at = time.monotonic()
        return self._session.context, self._session.page

    def mark_success(self) -> None:
        if self._session is None:
            return
        self._session.last_used_at = time.monotonic()
        self._session.verification_required = False
        self._session.last_verification_url = None
        self._persist_storage_state()

    def mark_verification_required(self, page: Page) -> None:
        if self._session is None:
            return
        self._session.last_used_at = time.monotonic()
        self._session.verification_required = True
        self._session.last_verification_url = page.url
        self._session.cooldown_until = time.monotonic() + ANTI_BOT_COOLDOWN_SECONDS
        logger.warning("domestic anti-bot detected; session enters cooldown until %.2f", self._session.cooldown_until)

    def has_pending_verification(self, anti_bot_detector) -> bool:
        if self._session is None or not self._is_usable(self._session):
            return False
        page = self._session.page
        if not page.url or page.url == "about:blank":
            return False
        try:
            blocked = anti_bot_detector(page)
        except PlaywrightError:
            self.invalidate()
            return False
        if blocked:
            self._session.verification_required = True
            self._session.last_verification_url = page.url
        return blocked

    def invalidate(self) -> None:
        if self._session is None:
            return
        try:
            self._session.context.close()
        except Exception:
            pass
        try:
            if self._session.browser is not None:
                self._session.browser.close()
        except Exception:
            pass
        try:
            self._session.playwright.stop()
        except Exception:
            pass
        self._session = None

    def status(self) -> dict[str, Any]:
        now = time.monotonic()
        self._close_if_idle(now)
        session = self._session
        if session is None or not self._is_usable(session):
            return {
                "active": False,
                "mode": None,
                "verificationRequired": False,
                "lastVerificationUrl": None,
                "cooldownRemainingSeconds": 0,
                "currentUrl": None,
                "idleTtlSeconds": SESSION_IDLE_TTL_SECONDS,
            }
        cooldown_remaining = max(0, int(session.cooldown_until - now))
        current_url = None
        try:
            current_url = session.page.url
        except PlaywrightError:
            current_url = None
        return {
            "active": True,
            "mode": session.mode,
            "verificationRequired": session.verification_required,
            "lastVerificationUrl": session.last_verification_url,
            "cooldownRemainingSeconds": cooldown_remaining,
            "currentUrl": current_url,
            "idleTtlSeconds": SESSION_IDLE_TTL_SECONDS,
        }

    def _start_session(
        self,
        *,
        cdp_url: str,
        user_data_dir: str,
        launch_kwargs: dict[str, Any],
        context_kwargs: dict[str, Any],
        storage_state_path: str,
    ) -> DomesticBrowserSession:
        playwright = sync_playwright().start()
        browser: Browser | None = None
        launch_kwargs = self._normalize_launch_kwargs(launch_kwargs)

        if cdp_url:
            browser = playwright.chromium.connect_over_cdp(cdp_url)
            context = browser.contexts[0] if browser.contexts else browser.new_context(**context_kwargs)
            page = self._select_preferred_page(context) or context.new_page()
            mode = "cdp"
        elif user_data_dir:
            persistent_kwargs = dict(context_kwargs)
            persistent_kwargs.pop("storage_state", None)
            try:
                context = playwright.chromium.launch_persistent_context(
                    user_data_dir,
                    **launch_kwargs,
                    **persistent_kwargs,
                )
                page = self._select_preferred_page(context) or context.new_page()
                mode = "persistent"
            except PlaywrightError as exc:
                if not self._is_profile_lock_error(exc):
                    raise
                logger.warning("persistent domestic profile is locked; falling back to ephemeral browser")
                browser = playwright.chromium.launch(**launch_kwargs)
                context = browser.new_context(**context_kwargs)
                page = context.new_page()
                mode = "ephemeral"
        else:
            browser = playwright.chromium.launch(**launch_kwargs)
            context = browser.new_context(**context_kwargs)
            page = context.new_page()
            mode = "ephemeral"

        context.add_init_script(STEALTH_INIT_SCRIPT)

        return DomesticBrowserSession(
            playwright=playwright,
            context=context,
            page=page,
            browser=browser,
            mode=mode,
            storage_state_path=storage_state_path,
            last_used_at=time.monotonic(),
        )

    def _refresh_page_reference(self, session: DomesticBrowserSession) -> None:
        preferred_page = self._select_preferred_page(session.context)
        if preferred_page is not None and preferred_page is not session.page:
            session.page = preferred_page

    def _select_preferred_page(self, context: BrowserContext) -> Optional[Page]:
        candidates: list[tuple[int, Page]] = []
        for page in context.pages:
            try:
                if page.is_closed():
                    continue
                url = page.url or ""
            except PlaywrightError:
                continue
            score = 0
            if url and url != "about:blank":
                score += 2
            if "1688.com" in url:
                score += 4
            if "_____tmd_____" not in url and "captcha" not in url and "punish" not in url:
                score += 1
            candidates.append((score, page))

        if not candidates:
            return None
        candidates.sort(key=lambda item: item[0], reverse=True)
        return candidates[0][1]

    def _apply_pacing(self, now: float) -> None:
        delay_seconds = 0.0
        if self._session is not None and self._session.cooldown_until > now:
            delay_seconds = max(delay_seconds, self._session.cooldown_until - now)
        jitter_ms = random.randint(NAVIGATION_JITTER_MIN_MS, max(NAVIGATION_JITTER_MAX_MS, NAVIGATION_JITTER_MIN_MS))
        elapsed_since_last_navigation = now - self._last_navigation_at if self._last_navigation_at else float("inf")
        delay_seconds = max(delay_seconds, max(0.0, (jitter_ms / 1000.0) - elapsed_since_last_navigation))
        if delay_seconds > 0:
            time.sleep(delay_seconds)
        self._last_navigation_at = time.monotonic()

    def _close_if_idle(self, now: float) -> None:
        if self._session is None:
            return
        if now - self._session.last_used_at <= SESSION_IDLE_TTL_SECONDS:
            return
        logger.info("domestic browser session closed after %.0fs idle", now - self._session.last_used_at)
        self.invalidate()

    def _is_usable(self, session: DomesticBrowserSession) -> bool:
        try:
            if self._context_is_closed(session.context):
                return False
            if session.page.is_closed():
                session.page = session.context.new_page()
            return True
        except PlaywrightError:
            return False

    def _context_is_closed(self, context: BrowserContext) -> bool:
        is_closed = getattr(context, "is_closed", None)
        if callable(is_closed):
            try:
                return bool(is_closed())
            except PlaywrightError:
                return True
        try:
            _ = context.pages
            return False
        except PlaywrightError:
            return True

    def _persist_storage_state(self) -> None:
        if self._session is None:
            return
        path = self._session.storage_state_path
        if not path or self._session.mode == "persistent":
            return
        try:
            Path(path).parent.mkdir(parents=True, exist_ok=True)
            self._session.context.storage_state(path=path)
        except PlaywrightError:
            logger.debug("failed to persist storage state", exc_info=True)

    def _is_profile_lock_error(self, exc: Exception) -> bool:
        message = str(exc)
        return any(marker in message for marker in PROFILE_LOCK_ERROR_MARKERS)

    def _normalize_launch_kwargs(self, launch_kwargs: dict[str, Any]) -> dict[str, Any]:
        normalized = dict(launch_kwargs)
        ignore_default_args = list(normalized.get("ignore_default_args") or [])
        if "--enable-automation" not in ignore_default_args:
            ignore_default_args.append("--enable-automation")
        normalized["ignore_default_args"] = ignore_default_args
        return normalized
