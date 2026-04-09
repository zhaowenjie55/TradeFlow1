from __future__ import annotations

import json
import os
from typing import Any

import requests


class LlmClientError(RuntimeError):
    pass


def llm_enabled() -> bool:
    return os.getenv("LLM_GATEWAY_ENABLED", "true").strip().lower() == "true"


def llm_force_simulated() -> bool:
    return os.getenv("LLM_FORCE_SIMULATED", "false").strip().lower() == "true"


def llm_model() -> str:
    return os.getenv("LLM_MODEL", "glm-5").strip() or "glm-5"


def invoke_chat(messages: list[dict[str, str]]) -> dict[str, Any]:
    endpoint = os.getenv("LLM_CHAT_ENDPOINT", "https://open.bigmodel.cn/api/paas/v4/chat/completions").strip()
    api_key = os.getenv("LLM_API_KEY", "").strip()
    timeout_seconds = float(os.getenv("LLM_TIMEOUT_SECONDS", "60").strip() or "60")
    temperature = float(os.getenv("LLM_TEMPERATURE", "0.2").strip() or "0.2")

    if not endpoint or not api_key:
        raise LlmClientError("LLM chat endpoint/api key is not configured.")

    response = requests.post(
        endpoint,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        json={
            "model": llm_model(),
            "messages": messages,
            "temperature": temperature,
        },
        timeout=timeout_seconds,
    )
    response.raise_for_status()
    payload = response.json()
    try:
        content = payload["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as exc:
        raise LlmClientError("LLM chat gateway returned empty content.") from exc

    normalized = strip_markdown_fence(content)
    try:
        return json.loads(normalized)
    except json.JSONDecodeError as exc:
        raise LlmClientError("LLM chat gateway returned non-JSON response.") from exc


def strip_markdown_fence(content: Any) -> str:
    if not isinstance(content, str):
        return json.dumps(content, ensure_ascii=False)

    normalized = content.strip()
    if normalized.startswith("```"):
        normalized = normalized.removeprefix("```json").removeprefix("```JSON").removeprefix("```").strip()
        if normalized.endswith("```"):
            normalized = normalized[:-3].strip()
    return normalized
