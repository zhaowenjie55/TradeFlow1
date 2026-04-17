import asyncio
import os
import re
import logging
import random
import time
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Optional
from urllib.parse import quote

from playwright.async_api import Error as PlaywrightError
from playwright.async_api import TimeoutError as PlaywrightTimeoutError

from app.services.providers.domestic_browser_session import DomesticBrowserSessionManager

SEARCH_ENTRY_SELECTORS = (
    # These selectors are tuned for the current 1688 search result markup and
    # should be kept together so future page updates are easy to re-verify.
    "a.search-offer-wrapper[href*='offerId=']",
    "a[href*='detail.m.1688.com/page/index.html?offerId=']",
    "a[href*='detail.1688.com/offer/']",
    "a[href*='offer/'][href*='.html']",
)

DETAIL_READY_SELECTORS = (
    ".module-od-title",
    ".title-content",
    ".module-od-main-price",
    ".module-od-picture-gallery img",
)

RESULT_READY_SELECTORS = (
    "a.search-offer-wrapper[href*='offerId=']",
    "[data-autotest='major-list-wrapper']",
    "a[href*='detail.m.1688.com/page/index.html?offerId=']",
    "a[href*='detail.1688.com/offer/']",
    "a[href*='offer/'][href*='.html']",
    "[data-offer-id]",
)

TITLE_SELECTORS = (
    ".offer-title-row .title-text",
    ".offer-title-row",
    ".title-text",
    "[title]",
    "[class*='title']",
    "[class*='Title']",
    "img[alt]",
)

PRICE_SELECTORS = (
    ".offer-price-row .price-item",
    ".offer-hover-wrapper .price-item",
    ".price-item",
    "[class*='price']",
    "[class*='Price']",
    "[data-price]",
)

SHOP_SELECTORS = (
    ".offer-shop-row .desc-text",
    ".offer-shop-row .col-left .desc-text",
    ".offer-shop-row .offer-desc-item .desc-text",
    "[class*='shop']",
    "[class*='seller']",
    "[class*='company']",
)

SALES_SELECTORS = (
    ".offer-price-row .col-desc_after .desc-text",
    ".offer-price-row .col-desc_after",
    "[class*='sale']",
    "[class*='deal']",
    "[class*='transaction']",
    "[class*='sold']",
)

ANTI_BOT_MARKERS = (
    "_____tmd_____",
    "captcha",
    "滑动验证",
    "访问受限",
    "x5secdata",
)

EMPTY_RESULT_MARKERS = (
    "暂无相关商品",
    "没有找到相关商品",
    "未找到相关商品",
)

PRICE_PATTERN = re.compile(r"(\d+(?:\.\d+)?)")
ITEM_ID_PATTERN = re.compile(r"/offer/(\d+)\.html")
ITEM_ID_QUERY_PATTERN = re.compile(r"[?&]offerId=(\d+)")
# asyncio.Lock replaces threading.RLock — safe to create at module level in Python 3.10+
DOMESTIC_BROWSER_LOCK = asyncio.Lock()
DOMESTIC_SESSION_MANAGER = DomesticBrowserSessionManager()
logger = logging.getLogger(__name__)


class DomesticSearchError(RuntimeError):
    pass


class DomesticDetailError(RuntimeError):
    pass


class DomesticVerificationRequiredError(RuntimeError):
    pass


def resolve_user_data_dir() -> str:
    configured = os.getenv("PLAYWRIGHT_USER_DATA_DIR", "").strip()
    if configured:
        Path(configured).mkdir(parents=True, exist_ok=True)
        return configured
    default_dir = Path.home() / ".tradeflow" / "playwright-1688-profile"
    default_dir.mkdir(parents=True, exist_ok=True)
    return str(default_dir)


@dataclass
class DomesticSearchItem:
    platform: str
    externalItemId: Optional[str]
    title: Optional[str]
    price: Optional[float]
    currency: str
    imageUrl: Optional[str]
    productUrl: Optional[str]
    shopName: Optional[str]
    salesText: Optional[str]
    rawData: dict[str, Any]


async def search_1688_products(keyword: str, page: int) -> list[dict[str, Any]]:
    items = await _fetch_1688_search_results(keyword, page)
    return [asdict(item) for item in items]


async def fetch_1688_product_detail(external_item_id: str) -> dict[str, Any]:
    return await _fetch_1688_product_detail(external_item_id)


async def _fetch_1688_search_results(keyword: str, page: int) -> list[DomesticSearchItem]:
    url = build_1688_search_url(keyword, page)
    headless = env_flag("PLAYWRIGHT_HEADLESS", False)
    slow_mo = int(os.getenv("PLAYWRIGHT_SLOW_MO_MS", "0") or "0")
    navigation_timeout_ms = int(os.getenv("PLAYWRIGHT_NAVIGATION_TIMEOUT_MS", "120000") or "120000")
    debug_dir = os.getenv("PLAYWRIGHT_DEBUG_DIR", "").strip()
    storage_state_path = os.getenv("PLAYWRIGHT_STORAGE_STATE_PATH", "").strip()
    browser_channel = os.getenv("PLAYWRIGHT_BROWSER_CHANNEL", "chrome").strip()
    user_data_dir = resolve_user_data_dir()
    manual_solve_timeout_ms = int(os.getenv("PLAYWRIGHT_MANUAL_SOLVE_TIMEOUT_MS", "180000") or "180000")
    allow_manual_solve = env_flag("PLAYWRIGHT_ALLOW_MANUAL_SOLVE", True)
    cdp_url = os.getenv("PLAYWRIGHT_CDP_URL", "").strip()

    try:
        async with DOMESTIC_BROWSER_LOCK:
            launch_kwargs: dict[str, Any] = {
                "headless": headless,
                "slow_mo": slow_mo,
                "args": [
                    "--disable-blink-features=AutomationControlled",
                    "--lang=zh-CN",
                ],
            }
            if browser_channel:
                launch_kwargs["channel"] = browser_channel
            context_kwargs: dict[str, Any] = {
                "locale": "zh-CN",
                "timezone_id": "Asia/Shanghai",
                "viewport": {"width": 1600, "height": 1200},
                "user_agent": (
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
                ),
            }
            if storage_state_path and Path(storage_state_path).exists():
                context_kwargs["storage_state"] = storage_state_path
            _context, page_ref = await DOMESTIC_SESSION_MANAGER.acquire(
                cdp_url=cdp_url,
                user_data_dir=user_data_dir,
                launch_kwargs=launch_kwargs,
                context_kwargs=context_kwargs,
                storage_state_path=storage_state_path,
            )

            if await DOMESTIC_SESSION_MANAGER.has_pending_verification(looks_like_antibot):
                if allow_manual_solve and manual_solve_timeout_ms > 0 and not headless:
                    if await _await_manual_verification(
                            page_ref,
                            manual_solve_timeout_ms,
                            "1688 验证过程中浏览器会话已关闭，请重新打开会话后重试。",
                    ):
                        await DOMESTIC_SESSION_MANAGER.mark_success()
                if await looks_like_antibot(page_ref):
                    await dump_debug_artifacts(page_ref, debug_dir, "blocked_session")
                    raise DomesticVerificationRequiredError(
                        "1688 浏览器会话仍停留在风控/验证码页面，请在复用浏览器中先完成验证后重试。"
                    )

            try:
                await page_ref.goto(url, wait_until="domcontentloaded", timeout=navigation_timeout_ms)
            except PlaywrightTimeoutError as exc:
                if not page_ref.url or page_ref.url == "about:blank":
                    await DOMESTIC_SESSION_MANAGER.invalidate()
                    raise DomesticSearchError("1688 页面打开超时，请稍后重试。") from exc
            try:
                await page_ref.wait_for_load_state("networkidle", timeout=15000)
            except PlaywrightTimeoutError:
                # 1688 长轮询较多，networkidle 不稳定，不作为硬失败条件。
                pass

            if await looks_like_antibot(page_ref):
                DOMESTIC_SESSION_MANAGER.mark_verification_required(page_ref)
                if allow_manual_solve and manual_solve_timeout_ms > 0 and not headless:
                    if await _await_manual_verification(
                            page_ref,
                            manual_solve_timeout_ms,
                            "1688 验证过程中浏览器会话已关闭，请重新打开会话后重试。",
                    ):
                        await wait_for_results(page_ref)
                        items = await extract_items(page_ref)
                        if items:
                            await DOMESTIC_SESSION_MANAGER.mark_success()
                            return items
                await dump_debug_artifacts(page_ref, debug_dir, "anti_bot")
                raise DomesticVerificationRequiredError(
                    f"1688 返回了风控/验证码页面，已预留约 {manual_solve_timeout_ms // 1000} 秒人工登录/验证时间，请完成后重试。"
                )

            await wait_for_results(page_ref)
            items = await extract_items(page_ref)
            if not items:
                if await looks_like_empty_result(page_ref):
                    await DOMESTIC_SESSION_MANAGER.mark_success()
                    return []
                await dump_debug_artifacts(page_ref, debug_dir, "no_results")
                raise DomesticSearchError(
                    "1688 页面已打开，但未解析到商品卡片。需要我本地在浏览器中确认 selector。"
                )
            await DOMESTIC_SESSION_MANAGER.mark_success()
            logger.info("1688 search succeeded keyword='%s' page=%s count=%s", keyword, page, len(items))
            return items

    except DomesticSearchError:
        raise
    except PlaywrightError as exc:
        await DOMESTIC_SESSION_MANAGER.invalidate()
        raise DomesticSearchError(f"Playwright 启动或执行失败: {exc}") from exc


async def _fetch_1688_product_detail(external_item_id: str) -> dict[str, Any]:
    url = f"https://detail.1688.com/offer/{external_item_id}.html?offerId={external_item_id}"
    headless = env_flag("PLAYWRIGHT_HEADLESS", False)
    slow_mo = int(os.getenv("PLAYWRIGHT_SLOW_MO_MS", "0") or "0")
    navigation_timeout_ms = int(os.getenv("PLAYWRIGHT_NAVIGATION_TIMEOUT_MS", "120000") or "120000")
    debug_dir = os.getenv("PLAYWRIGHT_DEBUG_DIR", "").strip()
    storage_state_path = os.getenv("PLAYWRIGHT_STORAGE_STATE_PATH", "").strip()
    browser_channel = os.getenv("PLAYWRIGHT_BROWSER_CHANNEL", "chrome").strip()
    user_data_dir = resolve_user_data_dir()
    manual_solve_timeout_ms = int(os.getenv("PLAYWRIGHT_MANUAL_SOLVE_TIMEOUT_MS", "180000") or "180000")
    allow_manual_solve = env_flag("PLAYWRIGHT_ALLOW_MANUAL_SOLVE", True)
    cdp_url = os.getenv("PLAYWRIGHT_CDP_URL", "").strip()

    try:
        async with DOMESTIC_BROWSER_LOCK:
            launch_kwargs: dict[str, Any] = {
                "headless": headless,
                "slow_mo": slow_mo,
                "args": [
                    "--disable-blink-features=AutomationControlled",
                    "--lang=zh-CN",
                ],
            }
            if browser_channel:
                launch_kwargs["channel"] = browser_channel
            context_kwargs: dict[str, Any] = {
                "locale": "zh-CN",
                "timezone_id": "Asia/Shanghai",
                "viewport": {"width": 1600, "height": 1200},
                "user_agent": (
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
                ),
            }
            if storage_state_path and Path(storage_state_path).exists():
                context_kwargs["storage_state"] = storage_state_path
            _context, page_ref = await DOMESTIC_SESSION_MANAGER.acquire(
                cdp_url=cdp_url,
                user_data_dir=user_data_dir,
                launch_kwargs=launch_kwargs,
                context_kwargs=context_kwargs,
                storage_state_path=storage_state_path,
            )

            if await DOMESTIC_SESSION_MANAGER.has_pending_verification(looks_like_antibot):
                if allow_manual_solve and manual_solve_timeout_ms > 0 and not headless:
                    if await _await_manual_verification(
                            page_ref,
                            manual_solve_timeout_ms,
                            "1688 详情验证过程中浏览器会话已关闭，请重新打开会话后重试。",
                    ):
                        await DOMESTIC_SESSION_MANAGER.mark_success()
                if await looks_like_antibot(page_ref):
                    await dump_debug_artifacts(page_ref, debug_dir, "detail_blocked_session")
                    raise DomesticVerificationRequiredError(
                        "1688 浏览器会话仍停留在风控/验证码页面，请先完成验证后重试详情抓取。"
                    )

            try:
                await page_ref.goto(url, wait_until="domcontentloaded", timeout=navigation_timeout_ms)
            except PlaywrightTimeoutError as exc:
                if not page_ref.url or page_ref.url == "about:blank":
                    await DOMESTIC_SESSION_MANAGER.invalidate()
                    raise DomesticDetailError("1688 详情页打开超时，请稍后重试。") from exc

            if await looks_like_antibot(page_ref):
                DOMESTIC_SESSION_MANAGER.mark_verification_required(page_ref)
                if allow_manual_solve and manual_solve_timeout_ms > 0 and not headless:
                    if await _await_manual_verification(
                            page_ref,
                            manual_solve_timeout_ms,
                            "1688 详情验证过程中浏览器会话已关闭，请重新打开会话后重试。",
                    ):
                        await wait_for_detail(page_ref)
                        detail = await extract_detail_payload(page_ref, external_item_id)
                        if detail:
                            await DOMESTIC_SESSION_MANAGER.mark_success()
                            return detail
                await dump_debug_artifacts(page_ref, debug_dir, "detail_anti_bot")
                raise DomesticVerificationRequiredError(
                    f"1688 详情页返回了风控/验证码页面，已预留约 {manual_solve_timeout_ms // 1000} 秒人工登录/验证时间，请完成后重试。"
                )

            await wait_for_detail(page_ref)
            detail = await extract_detail_payload(page_ref, external_item_id)
            if not detail:
                await dump_debug_artifacts(page_ref, debug_dir, "detail_no_data")
                raise DomesticDetailError("1688 详情页已打开，但未提取到结构化详情数据。")
            await DOMESTIC_SESSION_MANAGER.mark_success()
            logger.info("1688 detail succeeded external_item_id=%s", external_item_id)
            return detail
    except DomesticDetailError:
        raise
    except PlaywrightError as exc:
        await DOMESTIC_SESSION_MANAGER.invalidate()
        raise DomesticDetailError(f"Playwright 启动或执行详情抓取失败: {exc}") from exc


def build_1688_search_url(keyword: str, page: int) -> str:
    # 1688 search still expects a GBK-compatible keyword encoding on this route.
    # UTF-8 percent-encoding causes the search box to render mojibake and returns
    # unrelated products even though navigation itself succeeds.
    encoded = quote(keyword, encoding="gb18030", errors="ignore")
    if page <= 1:
        return f"https://s.1688.com/selloffer/offer_search.htm?keywords={encoded}"
    return f"https://s.1688.com/selloffer/offer_search.htm?keywords={encoded}&beginPage={page}"


async def wait_for_results(page_ref) -> None:
    for selector in RESULT_READY_SELECTORS:
        try:
            await page_ref.wait_for_selector(selector, timeout=10000)
            await page_ref.wait_for_timeout(1200)
            return
        except PlaywrightTimeoutError:
            continue
    await dump_debug_artifacts(page_ref, os.getenv("PLAYWRIGHT_DEBUG_DIR", "").strip(), "selector_timeout")
    raise DomesticSearchError("1688 搜索结果未正常渲染，未找到可用结果 selector。")


async def wait_for_detail(page_ref) -> None:
    for selector in DETAIL_READY_SELECTORS:
        try:
            await page_ref.wait_for_selector(selector, timeout=10000)
            await page_ref.wait_for_timeout(1000)
            return
        except PlaywrightTimeoutError:
            continue
    await dump_debug_artifacts(page_ref, os.getenv("PLAYWRIGHT_DEBUG_DIR", "").strip(), "detail_selector_timeout")
    raise DomesticDetailError("1688 详情页未正常渲染，未找到可用详情 selector。")


async def _await_manual_verification(page_ref, timeout_ms: int, closed_message: str) -> bool:
    try:
        await page_ref.bring_to_front()
    except PlaywrightError:
        await DOMESTIC_SESSION_MANAGER.invalidate()
        raise DomesticVerificationRequiredError(closed_message)

    started_at = time.monotonic()
    try:
        x5secdata = await extract_x5secdata_marker(page_ref)
        logger.warning(
            "verification.detected x5secdata=%s url=%s",
            x5secdata,
            page_ref.url,
        )
    except PlaywrightError:
        await DOMESTIC_SESSION_MANAGER.invalidate()
        raise DomesticVerificationRequiredError(closed_message)

    deadline = started_at + (timeout_ms / 1000.0)
    while time.monotonic() < deadline:
        try:
            if not await looks_like_antibot(page_ref):
                elapsed_ms = int((time.monotonic() - started_at) * 1000)
                logger.info(
                    "verification.cleared elapsed_ms=%s url=%s",
                    elapsed_ms,
                    page_ref.url,
                )
                return True
        except PlaywrightError as exc:
            await DOMESTIC_SESSION_MANAGER.invalidate()
            raise DomesticVerificationRequiredError(closed_message) from exc

        delay_ms = max(200, 1500 + random.randint(-300, 300))
        await page_ref.wait_for_timeout(delay_ms)

    try:
        return not await looks_like_antibot(page_ref)
    except PlaywrightError as exc:
        await DOMESTIC_SESSION_MANAGER.invalidate()
        raise DomesticVerificationRequiredError(closed_message) from exc


async def extract_items(page_ref) -> list[DomesticSearchItem]:
    items = await page_ref.evaluate(
        """(config) => {
            const normalizeText = (value) => {
              if (!value) return null;
              const text = String(value).replace(/\\s+/g, ' ').trim();
              return text || null;
            };
            const firstText = (root, selectors) => {
              for (const selector of selectors) {
                const element = root.querySelector(selector);
                if (!element) continue;
                const text = normalizeText(element.textContent || element.getAttribute('title') || element.getAttribute('alt'));
                if (text) return text;
              }
              return null;
            };
            const firstHref = (root) => {
              const links = root.querySelectorAll("a[href]");
              for (const link of links) {
                const href = link.href || link.getAttribute("href");
                if (!href) continue;
                if (href.includes("offerId=")) return href;
                if (href.includes("offer/") && href.includes(".html")) return href;
              }
              return null;
            };
            const firstImage = (root) => {
              const image = root.querySelector("img");
              if (!image) return null;
              return image.currentSrc || image.src || image.getAttribute("src") || image.getAttribute("data-src") || null;
            };
            const collectCandidates = () => {
              const entries = [];
              for (const selector of config.entrySelectors) {
                document.querySelectorAll(selector).forEach((element) => entries.push(element));
              }
              return entries;
            };

            const seen = new Set();
            const results = [];

            for (const anchor of collectCandidates()) {
              const href = anchor.href || anchor.getAttribute("href");
              if (!href || seen.has(href)) continue;
              seen.add(href);
              const card =
                (anchor.matches("a.search-offer-wrapper") ? anchor : null) ||
                anchor.closest("a.search-offer-wrapper, [data-offer-id], li, [class*='offer'], [class*='item'], [class*='card'], [class*='Card']") ||
                anchor.parentElement;
              if (!card) continue;

              const title =
                firstText(card, config.titleSelectors) ||
                normalizeText(anchor.getAttribute("title"));
              const imageUrl = firstImage(card);
              const productUrl =
                (href.includes("offerId=") || (href.includes("offer/") && href.includes(".html")) ? href : null) ||
                firstHref(card);
              const priceText = firstText(card, config.priceSelectors) || normalizeText(card.innerText?.match(/[¥￥]\\s?\\d+(?:\\.\\d+)?/)?.[0]);
              const shopName = firstText(card, config.shopSelectors);
              const salesText = firstText(card, config.salesSelectors);

              results.push({
                title,
                productUrl,
                imageUrl,
                priceText,
                shopName,
                salesText,
                cardText: normalizeText(card.innerText),
              });
            }

            return results;
        }""",
        {
            "entrySelectors": list(SEARCH_ENTRY_SELECTORS),
            "titleSelectors": list(TITLE_SELECTORS),
            "priceSelectors": list(PRICE_SELECTORS),
            "shopSelectors": list(SHOP_SELECTORS),
            "salesSelectors": list(SALES_SELECTORS),
        },
    )

    normalized: list[DomesticSearchItem] = []
    for raw in items:
        product_url = normalize_url(raw.get("productUrl"))
        title = normalize_text(raw.get("title"))
        if not product_url or not title:
            continue
        price_text = normalize_text(raw.get("priceText"))
        normalized.append(
            DomesticSearchItem(
                platform="1688",
                externalItemId=extract_external_item_id(product_url),
                title=title,
                price=parse_price(price_text),
                currency="CNY",
                imageUrl=normalize_url(raw.get("imageUrl")),
                productUrl=product_url,
                shopName=normalize_text(raw.get("shopName")),
                salesText=normalize_text(raw.get("salesText")),
                rawData={
                    "priceText": price_text,
                    "shopNameText": normalize_text(raw.get("shopName")),
                    "salesText": normalize_text(raw.get("salesText")),
                    "cardText": normalize_text(raw.get("cardText")),
                },
            )
        )
    return normalized


async def extract_detail_payload(page_ref, external_item_id: str) -> Optional[dict[str, Any]]:
    payload = await page_ref.evaluate(
        """() => {
            const clone = (value) => {
              try {
                return JSON.parse(JSON.stringify(value));
              } catch (_error) {
                return null;
              }
            };
            return {
              contextData: clone(window.context?.result?.data || null),
              bodyText: document.body?.innerText || "",
              titleText: document.querySelector(".module-od-title .title-content")?.textContent || document.title || "",
              shopText: document.querySelector(".module-od-title [class*='shop']")?.textContent || "",
              priceText:
                document.querySelector(".module-od-main-price .price-comp")?.textContent ||
                document.querySelector(".module-od-main-price .price-info")?.textContent ||
                "",
              shippingText: document.querySelector(".module-od-shipping-services")?.textContent || "",
              attributePairs: (() => {
                const root = document.querySelector(".module-od-product-attributes");
                if (!root) return [];
                const tokens = (root.innerText || "")
                  .split("\\n")
                  .map((item) => item.replace(/\\s+/g, " ").trim())
                  .filter(Boolean)
                  .filter((item) => item !== "商品属性" && item !== "展开全部");
                const pairs = [];
                for (let index = 0; index + 1 < tokens.length; index += 2) {
                  pairs.push({ name: tokens[index], value: tokens[index + 1] });
                }
                return pairs;
              })(),
            };
        }"""
    )
    context_data = payload.get("contextData") if isinstance(payload, dict) else None
    if not isinstance(context_data, dict):
        return None

    title = normalize_text(
        resolve_data_path(context_data, "productTitle", "fields", "title")
        or payload.get("titleText")
    )
    if not title:
        return None

    shop_name = normalize_text(
        resolve_data_path(context_data, "productTitle", "fields", "shopInfo", "companyName")
        or resolve_data_path(context_data, "productTitle", "fields", "shopInfo", "authCompanyName")
        or payload.get("shopText")
    )
    price_value = (
        resolve_data_path(
            context_data,
            "mainPrice",
            "fields",
            "finalPriceModel",
            "tradeWithoutPromotion",
            "offerMinPrice",
        )
        or resolve_data_path(context_data, "mainPrice", "fields", "priceModel", "currentPrices", 0, "price")
        or resolve_data_path(context_data, "mainPrice", "fields", "priceModel", "currentPrices", 0, "value")
        or payload.get("priceText")
    )
    price = parse_price(price_value)

    images = extract_images(resolve_data_path(context_data, "gallery", "fields"))
    image_url = images[0] if images else None
    product_url = normalize_url(page_ref.url) or f"https://detail.1688.com/offer/{external_item_id}.html"

    attribute_pairs = payload.get("attributePairs") if isinstance(payload.get("attributePairs"), list) else []
    attributes = extract_attribute_pairs(resolve_data_path(context_data, "productAttributes"))
    for pair in attribute_pairs:
        if not isinstance(pair, dict):
            continue
        name = normalize_text(pair.get("name"))
        value = normalize_text(pair.get("value"))
        if name and value:
            attributes[name] = value
    brand = normalize_text(
        attributes.get("品牌")
        or attributes.get("品牌名")
        or attributes.get("brand")
        or resolve_data_path(context_data, "productTitle", "fields", "brand")
    )
    description = normalize_text(
        resolve_data_path(context_data, "description", "fields", "description")
        or resolve_data_path(context_data, "description", "fields", "desc")
        or title
    )

    shipping_text = build_shipping_text(resolve_data_path(context_data, "shippingServices", "fields"), payload.get("shippingText"))
    sales_text = build_sales_text(resolve_data_path(context_data, "productTitle", "fields", "scrollInfo"))
    sku_data = resolve_data_path(
        context_data,
        "mainPrice",
        "fields",
        "finalPriceModel",
        "tradeWithoutPromotion",
        "skuMapOriginal",
    ) or {}
    if not isinstance(sku_data, dict):
        sku_data = {"items": sku_data}

    raw_data = {
        "productTitle": resolve_data_path(context_data, "productTitle", "fields") or {},
        "mainPrice": resolve_data_path(context_data, "mainPrice", "fields") or {},
        "shippingServices": resolve_data_path(context_data, "shippingServices", "fields") or {},
        "gallery": resolve_data_path(context_data, "gallery", "fields") or {},
        "productPackInfo": resolve_data_path(context_data, "productPackInfo", "fields") or {},
        "productAttributes": resolve_data_path(context_data, "productAttributes") or {},
        "attributePairs": attribute_pairs,
        "bodyTextExcerpt": normalize_text((payload.get("bodyText") or "")[:1500]),
    }

    return {
        "success": True,
        "platform": "1688",
        "externalItemId": external_item_id,
        "title": title,
        "price": price,
        "currency": "CNY",
        "imageUrl": image_url,
        "productUrl": product_url,
        "shopName": shop_name,
        "brand": brand,
        "description": description,
        "images": images,
        "attributes": attributes,
        "shippingText": shipping_text,
        "salesText": sales_text,
        "skuData": sku_data,
        "rawData": raw_data,
    }


def normalize_text(value: Any) -> Optional[str]:
    if value is None:
        return None
    text = str(value).replace("\xa0", " ")
    text = re.sub(r"\s+", " ", text).strip()
    return text or None


def normalize_url(value: Any) -> Optional[str]:
    text = normalize_text(value)
    if not text:
        return None
    if text.startswith("//"):
        return "https:" + text
    return text


def parse_price(value: Any) -> Optional[float]:
    if isinstance(value, (int, float)):
        return float(value)
    text = normalize_text(value)
    if not text:
        return None
    match = PRICE_PATTERN.search(text.replace(",", ""))
    if not match:
        return None
    try:
        return float(match.group(1))
    except ValueError:
        return None


def extract_external_item_id(product_url: Optional[str]) -> Optional[str]:
    if not product_url:
        return None
    match = ITEM_ID_PATTERN.search(product_url)
    if match:
        return match.group(1)
    query_match = ITEM_ID_QUERY_PATTERN.search(product_url)
    if query_match:
        return query_match.group(1)
    return None


def env_flag(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


async def looks_like_antibot(page_ref) -> bool:
    current_url = page_ref.url or ""
    if any(marker in current_url for marker in ANTI_BOT_MARKERS):
        return True
    try:
        content = await page_ref.content()
    except PlaywrightError:
        return False
    lowered = content.lower()
    return any(marker.lower() in lowered for marker in ANTI_BOT_MARKERS)


async def extract_x5secdata_marker(page_ref) -> str:
    haystacks = [page_ref.url or ""]
    try:
        haystacks.append(await page_ref.content())
    except PlaywrightError:
        pass
    for haystack in haystacks:
        match = re.search(r"x5secdata[^&\"'<>\\s]*", haystack, flags=re.IGNORECASE)
        if match:
            return match.group(0)[:40]
    return ""


async def looks_like_empty_result(page_ref) -> bool:
    try:
        content = await page_ref.content()
    except PlaywrightError:
        return False
    return any(marker in content for marker in EMPTY_RESULT_MARKERS)


async def dump_debug_artifacts(page_ref, debug_dir: str, label: str) -> None:
    if not debug_dir:
        return
    output_dir = Path(debug_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    html_path = output_dir / f"1688_{label}.html"
    png_path = output_dir / f"1688_{label}.png"
    try:
        html_path.write_text(await page_ref.content(), encoding="utf-8")
        await page_ref.screenshot(path=str(png_path), full_page=True)
    except Exception:
        return


def resolve_data_path(node: Any, *path: Any) -> Any:
    current = node
    for key in path:
        if current is None:
            return None
        if isinstance(key, int):
            if not isinstance(current, list) or key >= len(current):
                return None
            current = current[key]
            continue
        if not isinstance(current, dict):
            return None
        current = current.get(key)
    return current


def extract_images(gallery_fields: Any) -> list[str]:
    image_candidates: list[str] = []
    if isinstance(gallery_fields, dict):
        for key in ("offerImgList", "mainImage", "images", "skuImageList"):
            value = gallery_fields.get(key)
            if not value:
                continue
            if isinstance(value, list):
                for item in value:
                    if isinstance(item, dict):
                        candidate = normalize_url(
                            item.get("imageUrl") or item.get("url") or item.get("originalImageURI") or item.get("fullPathImageURI")
                        )
                    else:
                        candidate = normalize_url(item)
                    if candidate and candidate not in image_candidates:
                        image_candidates.append(candidate)
            elif isinstance(value, dict):
                candidate = normalize_url(
                    value.get("imageUrl") or value.get("url") or value.get("originalImageURI") or value.get("fullPathImageURI")
                )
                if candidate and candidate not in image_candidates:
                    image_candidates.append(candidate)
    return image_candidates


def extract_attribute_pairs(node: Any) -> dict[str, Any]:
    attributes: dict[str, Any] = {}

    def walk(value: Any) -> None:
        if isinstance(value, dict):
            pairs = (
                ("attributeName", "attributeValue"),
                ("attrName", "attrValue"),
                ("name", "value"),
                ("prop", "value"),
                ("key", "value"),
                ("title", "value"),
            )
            for name_key, value_key in pairs:
                name = normalize_text(value.get(name_key))
                attr_value = normalize_text(value.get(value_key))
                if name and attr_value and name not in attributes:
                    attributes[name] = attr_value
            for child in value.values():
                walk(child)
        elif isinstance(value, list):
            for child in value:
                walk(child)

    walk(node)
    return attributes


def collect_text_fragments(node: Any) -> list[str]:
    fragments: list[str] = []

    def walk(value: Any) -> None:
        if value is None:
            return
        if isinstance(value, dict):
            for child in value.values():
                walk(child)
            return
        if isinstance(value, list):
            for child in value:
                walk(child)
            return
        text = normalize_text(value)
        if text and text not in fragments:
            fragments.append(text)

    walk(node)
    return fragments


def build_shipping_text(shipping_fields: Any, fallback_text: Any) -> Optional[str]:
    if not isinstance(shipping_fields, dict):
        return normalize_text(fallback_text)

    parts: list[str] = []
    post_fee = normalize_text(shipping_fields.get("postFeeValue"))
    if post_fee:
        parts.append(f"运费 ¥{post_fee} 起")

    delivery_limit = normalize_text(shipping_fields.get("deliveryLimitText"))
    if delivery_limit:
        parts.append(delivery_limit)

    freight_info = shipping_fields.get("freightInfo")
    if isinstance(freight_info, dict):
        location = normalize_text(freight_info.get("location"))
        receive_address = normalize_text(freight_info.get("recieveAddress"))
        logistics_text = normalize_text(freight_info.get("logisticsText"))
        for fragment in (location, receive_address, logistics_text):
            if fragment and fragment not in parts:
                parts.append(fragment)

    if parts:
        return " | ".join(parts)
    return normalize_text(fallback_text)


def build_sales_text(scroll_info: Any) -> Optional[str]:
    if isinstance(scroll_info, list):
        parts: list[str] = []
        for item in scroll_info:
            if not isinstance(item, dict):
                continue
            fragment = normalize_text(
                f"{normalize_text(item.get('prefix')) or ''}{normalize_text(item.get('text')) or ''}{normalize_text(item.get('suffix')) or ''}"
            )
            if fragment:
                parts.append(fragment)
        if parts:
            return " | ".join(parts[:4])
    fragments = [fragment for fragment in collect_text_fragments(scroll_info) if "售" in fragment or "销量" in fragment]
    if not fragments:
        return None
    return " | ".join(fragments[:4])
