from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
import json
from typing import Any
import re

from app.models import (
    LlmReasoningRequest,
    LlmReasoningResponse,
    LlmReportNarrativeRequest,
    LlmReportNarrativeResponse,
    LlmRewriteRequest,
    LlmRewriteResponse,
    LlmTranscriptIntentRequest,
    LlmTranscriptIntentResponse,
)
from app.services.llm.client import LlmClientError, invoke_chat, llm_enabled, llm_force_simulated, llm_model
from app.services.llm.prompts import (
    REASONING_SYSTEM_PROMPT,
    REPORT_SYSTEM_PROMPT,
    REWRITE_SYSTEM_PROMPT,
    TRANSCRIPT_INTENT_SYSTEM_PROMPT,
)


ENGLISH_STOP_WORDS = {
    "the", "and", "with", "for", "from", "into", "award", "winner", "innovation", "multiple",
    "pets", "pet", "cat", "dog", "dogs", "cats", "automatic", "portable", "replacement", "filters",
    "filter", "grey", "gray", "plastic", "stainless", "steel", "oz", "l", "ml", "pack", "new",
    "version", "upgrade", "premium", "large", "small",
}
TRANSCRIPT_STOP_WORDS = {
    "i", "want", "need", "find", "show", "me", "some", "a", "an", "the", "to", "for", "from",
    "that", "can", "be", "is", "are", "with", "and", "or", "of", "in", "on", "my", "please",
    "cheap", "budget", "trending", "products", "product", "sourcing", "source",
}
CATEGORY_KEYWORDS = {
    "kitchen storage": ["kitchen storage", "kitchen organizer", "cabinet organizer"],
    "pet fountain": ["pet fountain", "cat water fountain", "dog water dispenser"],
    "wireless earbuds": ["wireless earbuds", "bluetooth earbuds", "true wireless earbuds"],
    "desk organizer": ["desk organizer", "desktop organizer", "acrylic organizer"],
}
HAN_SEGMENT_PATTERN = re.compile(r"[\u4e00-\u9fff]{2,}")


def rewrite_title(request: LlmRewriteRequest) -> LlmRewriteResponse:
    try:
        if not llm_enabled():
            return _fallback_rewrite(request.sourceTitle, "LLM 改写接口未启用，已切换到模拟模式。")
        if llm_force_simulated():
            return _fallback_rewrite(request.sourceTitle, "LLM 改写接口被配置为模拟模式。")
        content = invoke_chat([
            {"role": "system", "content": REWRITE_SYSTEM_PROMPT},
            {"role": "user", "content": request.sourceTitle or ""},
        ])
        rewritten_text = _required_text(content, "rewritten_text")
        keywords = _required_string_list(content, "keywords")
        return LlmRewriteResponse(
            rewrittenText=rewritten_text,
            keywords=keywords,
            fallbackUsed=False,
            provider="GLM_CHAT",
            model=llm_model(),
            fallbackReason=None,
            generatedAt=_now_iso(),
        )
    except Exception as exc:
        return _fallback_rewrite(request.sourceTitle, str(exc))


def generate_report_narrative(request: LlmReportNarrativeRequest) -> LlmReportNarrativeResponse:
    try:
        if not llm_enabled():
            return _fallback_report(request, "LLM 报告接口未启用，已切换到模拟模式。")
        if llm_force_simulated():
            return _fallback_report(request, "LLM 报告接口被配置为模拟模式。")
        content = invoke_chat([
            {"role": "system", "content": REPORT_SYSTEM_PROMPT},
            {"role": "user", "content": _to_json(request)},
        ])
        return LlmReportNarrativeResponse(
            summaryText=_required_text(content, "summary_text"),
            recommendations=_required_string_list(content, "recommendations"),
            riskNotes=_required_string_list(content, "risk_notes"),
            fallbackUsed=False,
            provider="GLM_CHAT",
            model=llm_model(),
            fallbackReason=None,
            generatedAt=_now_iso(),
        )
    except Exception as exc:
        return _fallback_report(request, str(exc))


def generate_reasoning(request: LlmReasoningRequest) -> LlmReasoningResponse:
    try:
        if not llm_enabled():
            return _fallback_reasoning(request, "LLM 推理接口未启用，已切换到模拟模式。")
        if llm_force_simulated():
            return _fallback_reasoning(request, "LLM 推理接口被配置为模拟模式。")
        content = invoke_chat([
            {"role": "system", "content": REASONING_SYSTEM_PROMPT},
            {"role": "user", "content": _build_reasoning_user_prompt(request)},
        ])
        return LlmReasoningResponse(
            decision=_required_text(content, "decision"),
            explanation=_required_text(content, "explanation"),
            confidenceScore=_required_confidence(content.get("confidence_score")),
            fallbackUsed=False,
            provider="GLM_CHAT",
            model=llm_model(),
            fallbackReason=None,
            generatedAt=_now_iso(),
        )
    except Exception as exc:
        return _fallback_reasoning(request, str(exc))


def analyze_transcript(request: LlmTranscriptIntentRequest) -> LlmTranscriptIntentResponse:
    try:
        if not llm_enabled():
            return _fallback_transcript_intent(request, "LLM transcript 分析接口未启用，已切换到模拟模式。")
        if llm_force_simulated():
            return _fallback_transcript_intent(request, "LLM transcript 分析接口被配置为模拟模式。")
        content = invoke_chat([
            {"role": "system", "content": TRANSCRIPT_INTENT_SYSTEM_PROMPT},
            {"role": "user", "content": _build_transcript_user_prompt(request)},
        ])
        return LlmTranscriptIntentResponse(
            intent=_required_text(content, "intent"),
            category=_optional_text(content, "category"),
            market=_optional_text(content, "market"),
            priceLevel=_optional_text(content, "price_level"),
            sourcing=_optional_text(content, "sourcing"),
            keywords=_optional_string_list(content, "keywords"),
            sellingPoints=_optional_string_list(content, "selling_points"),
            painPoints=_optional_string_list(content, "pain_points"),
            useCases=_optional_string_list(content, "use_cases"),
            targetAudience=_optional_string_list(content, "target_audience"),
            fallbackUsed=False,
            provider="GLM_CHAT",
            model=llm_model(),
            fallbackReason=None,
            generatedAt=_now_iso(),
        )
    except Exception as exc:
        return _fallback_transcript_intent(request, str(exc))


def _fallback_rewrite(source_title: str, fallback_reason: str) -> LlmRewriteResponse:
    normalized = (source_title or "").lower()
    if "coffee" in normalized:
        return _rewrite_response(_build_coffee_rewrite(normalized), _build_coffee_keywords(normalized), fallback_reason)
    if "cat water fountain" in normalized or "pet fountain" in normalized or "dog water dispenser" in normalized or "water fountain" in normalized:
        keywords = ["宠物饮水机", "猫饮水机", "自动饮水器", "循环饮水器", "宠物饮水器"]
        if "filter" in normalized and "fountain" not in normalized and "dispenser" not in normalized:
            keywords.append("宠物饮水机滤芯")
        return _rewrite_response("宠物自动饮水机", keywords, fallback_reason)
    if "mouse" in normalized:
        return _rewrite_response("充电无线鼠标", ["无线鼠标", "静音鼠标", "办公鼠标"], fallback_reason)
    if "blender" in normalized:
        return _rewrite_response("便携果汁机", ["便携榨汁杯", "迷你果汁机", "充电榨汁杯"], fallback_reason)
    if "lamp" in normalized:
        return _rewrite_response("护眼 LED 台灯", ["护眼台灯", "桌面台灯", "可调光台灯"], fallback_reason)
    if "earbud" in normalized or "headphone" in normalized:
        return _rewrite_response("无线蓝牙耳机", ["蓝牙耳机", "入耳式耳机", "运动耳机"], fallback_reason)
    if "acrylic" in normalized or "organizer" in normalized:
        return _rewrite_response("亚克力透明收纳架", ["亚克力透明收纳架", "亚克力桌面收纳架", "亚克力展示架"], fallback_reason)

    rewritten, keywords = _build_rewrite_heuristic(source_title)
    return _rewrite_response(rewritten, keywords, fallback_reason)


def _fallback_report(request: LlmReportNarrativeRequest, fallback_reason: str) -> LlmReportNarrativeResponse:
    benchmark = request.benchmarkTitle or "当前数据库中最接近的历史货源"
    margin = _format_number(request.estimatedMargin, "--")
    profit = _format_currency(request.estimatedProfit, "--")
    summary = (
        "Agent 结合候选商品、历史货源和成本拆解后判断，"
        f"{request.productTitle} 在 {request.market} 站点当前具备约 {margin}% 的利润空间，"
        f"主要对标货源为 {benchmark}，当前可作为进一步人工复核与定向寻源的候选，最终预估利润约为 {profit}。"
    )
    recommendations = [
        f"优先围绕 “{request.rewrittenQuery}” 继续核对国内主卖点，确保展示中的标题改写和寻源逻辑一致。",
        f"以 {benchmark} 作为当前对标货源，补充 MOQ、发货时效和可定制能力，增强报告说服力。",
        f"将 {_format_currency(request.estimatedProfit, '¥0.00')} 的预估利润作为当前试算结果，并结合 MOQ、头程、税费和平台政策做二次复核。",
    ]
    risk_notes = [
        "当前结论优先服务于黑客松演示，真实上架前仍需补充运费、税费与平台政策校验。",
        "当前叙事由本地规则与模板化网关生成，不影响匹配、定价与利润测算的确定性结果。",
        _normalize_fallback_reason(fallback_reason),
    ]
    return LlmReportNarrativeResponse(
        summaryText=summary,
        recommendations=recommendations,
        riskNotes=risk_notes,
        fallbackUsed=True,
        provider="SIMULATED_LLM",
        model="simulated-llm",
        fallbackReason=_normalize_fallback_reason(fallback_reason),
        generatedAt=_now_iso(),
    )


def _build_coffee_rewrite(normalized: str) -> str:
    if "ground" in normalized:
        return "夏威夷咖啡粉" if "hawaiian" in normalized else "研磨咖啡粉"
    if "bean" in normalized:
        return "夏威夷咖啡豆" if "hawaiian" in normalized else "咖啡豆"
    if "instant" in normalized:
        return "速溶咖啡"
    if "hawaiian" in normalized:
        return "夏威夷咖啡"
    return "咖啡"


def _build_coffee_keywords(normalized: str) -> list[str]:
    keywords: list[str] = []
    if "hawaiian" in normalized:
        keywords.append("夏威夷咖啡")
    if "ground" in normalized:
        keywords.extend(["咖啡粉", "研磨咖啡"])
    elif "bean" in normalized:
        keywords.append("咖啡豆")
    elif "instant" in normalized:
        keywords.append("速溶咖啡")
    if "vanilla" in normalized:
        keywords.append("香草咖啡")
    if "macadamia" in normalized:
        keywords.append("夏威夷果咖啡")
    keywords.append("咖啡")
    deduped: list[str] = []
    for keyword in keywords:
        if keyword not in deduped:
            deduped.append(keyword)
    return deduped


def _fallback_reasoning(request: LlmReasoningRequest, fallback_reason: str) -> LlmReasoningResponse:
    step = request.stepName or "unknown"
    context = request.context or {}
    if step == "matching":
        best = context.get("best_match_title")
        decision = "Selected best domestic match." if best else "No high-confidence domestic match."
        explanation = "Rule-based scorer compared keyword overlap and price relationship, then chose the highest-score candidate."
        confidence = 0.72 if best else 0.35
    elif step == "profit_analysis":
        decision = "Profit computed with deterministic formula."
        explanation = "System used overseas price, domestic cost, fixed shipping, and platform fee ratio to estimate margin."
        confidence = 0.8
    elif step == "risk_analysis":
        decision = "Risk level derived from rule thresholds."
        explanation = "System weighted match confidence, margin floor, competition count, and rating to produce risk classification."
        confidence = 0.76
    elif step == "final_report":
        decision = "Generated narrative summary from computed facts."
        explanation = "Final explanation keeps deterministic outputs unchanged and only rewrites them for readability."
        confidence = 0.74
    else:
        decision = "Reasoning step completed."
        explanation = "Fallback reasoning generated in simulated mode."
        confidence = 0.6
    return LlmReasoningResponse(
        decision=decision,
        explanation=explanation,
        confidenceScore=confidence,
        fallbackUsed=True,
        provider="SIMULATED_LLM",
        model="simulated-llm",
        fallbackReason=_normalize_fallback_reason(fallback_reason),
        generatedAt=_now_iso(),
    )


def _fallback_transcript_intent(request: LlmTranscriptIntentRequest, fallback_reason: str) -> LlmTranscriptIntentResponse:
    normalized = _normalize_transcript_for_intent((request.transcript or "").strip().lower())
    category = _detect_category(normalized)
    keywords = _derive_transcript_keywords(category, normalized)
    return LlmTranscriptIntentResponse(
        intent="product_sourcing" if any(token in normalized for token in ("source", "1688", "find")) else "media_analysis",
        category=category,
        market=_detect_market(normalized),
        priceLevel=_detect_price_level(normalized),
        sourcing=_detect_sourcing(normalized),
        keywords=keywords,
        sellingPoints=_derive_selling_points(normalized, category),
        painPoints=_derive_pain_points(normalized),
        useCases=_derive_use_cases(category),
        targetAudience=_derive_target_audience(normalized, category),
        fallbackUsed=True,
        provider="SIMULATED_LLM",
        model="simulated-llm",
        fallbackReason=_normalize_fallback_reason(fallback_reason),
        generatedAt=_now_iso(),
    )


def _rewrite_response(rewritten_text: str, keywords: list[str], fallback_reason: str) -> LlmRewriteResponse:
    return LlmRewriteResponse(
        rewrittenText=rewritten_text,
        keywords=keywords,
        fallbackUsed=True,
        provider="SIMULATED_LLM",
        model="simulated-llm",
        fallbackReason=_normalize_fallback_reason(fallback_reason),
        generatedAt=_now_iso(),
    )


def _build_rewrite_heuristic(source_title: str | None) -> tuple[str, list[str]]:
    if not source_title or not source_title.strip():
        return "跨境选品候选商品", ["跨境选品", "国内货源"]
    if _contains_han(source_title):
        normalized = re.sub(r"\s+", " ", source_title).strip()
        return normalized, [normalized]

    normalized = re.sub(r"\s+", " ", re.sub(r"[^a-z0-9\s]+", " ", source_title.lower())).strip()
    chinese_keywords: list[str] = []
    keyword_map = [
        ("water fountain", "饮水机"),
        ("water dispenser", "饮水器"),
        ("fountain", "饮水机"),
        ("organizer", "收纳架"),
        ("storage", "收纳"),
        ("display", "展示架"),
        ("blender", "榨汁机"),
        ("mouse", "无线鼠标"),
        ("lamp", "台灯"),
        ("earbud", "蓝牙耳机"),
        ("headphone", "蓝牙耳机"),
        ("pet", "宠物"),
        ("cat", "猫"),
        ("dog", "狗"),
        ("filter", "滤芯"),
        ("automatic", "自动"),
        ("acrylic", "亚克力"),
    ]
    for english_phrase, chinese_phrase in keyword_map:
        if english_phrase in normalized and chinese_phrase not in chinese_keywords:
            chinese_keywords.append(chinese_phrase)
    if "宠物" in chinese_keywords and "饮水机" in chinese_keywords:
        chinese_keywords = [value for value in chinese_keywords if value not in {"猫", "狗"}]
        for value in ("猫饮水机", "自动饮水器"):
            if value not in chinese_keywords:
                chinese_keywords.append(value)
    if chinese_keywords:
        return "".join(chinese_keywords[:3]), chinese_keywords

    fallback_keywords: list[str] = []
    for token in normalized.split():
        if not token or len(token) < 3 or token.isdigit() or token in ENGLISH_STOP_WORDS:
            continue
        fallback_keywords.append(token)
        if len(fallback_keywords) >= 4:
            break
    if not fallback_keywords:
        return "跨境选品候选商品", ["跨境选品", "国内货源"]
    rewritten = " ".join(fallback_keywords)
    return rewritten, fallback_keywords


def _normalize_transcript_for_intent(normalized: str) -> str:
    if not normalized or not normalized.strip():
        return ""
    corrected = re.sub(r"[^\u4e00-\u9fffa-z0-9\s]+", " ", normalized)
    corrected = re.sub(r"\bcatch water fountain\b", "cat water fountain", corrected)
    corrected = re.sub(r"\bcatch water dispenser\b", "cat water dispenser", corrected)
    corrected = re.sub(r"\bcatch water\b", "cat water", corrected)
    corrected = re.sub(r"\bcatch fountain\b", "cat fountain", corrected)
    corrected = re.sub(r"\bear buds\b", "earbuds", corrected)
    corrected = re.sub(r"\bair buds\b", "earbuds", corrected)
    corrected = re.sub(r"\bblue tooth\b", "bluetooth", corrected)
    corrected = re.sub(r"\s+", " ", corrected).strip()
    if "cat water" in corrected and "dispenser" in corrected and "cat water fountain" not in corrected:
        corrected = corrected + " cat water fountain"
    if "cat water" in corrected and "pet fountain" not in corrected:
        corrected = corrected + " pet fountain"
    return corrected


def _detect_category(normalized: str) -> str | None:
    if "kitchen" in normalized:
        return "kitchen storage"
    if "厨房" in normalized or "收纳" in normalized:
        return "kitchen storage"
    if "earbud" in normalized or "headphone" in normalized:
        return "wireless earbuds"
    if "耳机" in normalized or "蓝牙耳机" in normalized:
        return "wireless earbuds"
    if "pet fountain" in normalized or "cat water fountain" in normalized or "dog water dispenser" in normalized or "cat water" in normalized or ("water" in normalized and "dispenser" in normalized):
        return "pet fountain"
    if "饮水机" in normalized or "饮水器" in normalized or "宠物饮水" in normalized or "猫咪自动饮水机" in normalized:
        return "pet fountain"
    if "organizer" in normalized or "storage" in normalized:
        return "desk organizer"
    if "收纳架" in normalized or "展示架" in normalized or "桌面收纳" in normalized:
        return "desk organizer"
    return None


def _detect_market(normalized: str) -> str:
    if any(token in normalized for token in ("united states", " us ", "u.s.", "america")):
        return "US"
    if "美国" in normalized:
        return "US"
    if "china" in normalized or "cn" in normalized:
        return "CN"
    if "中国" in normalized or "国内" in normalized:
        return "CN"
    if "europe" in normalized or "eu" in normalized:
        return "EU"
    if "欧洲" in normalized:
        return "EU"
    return "unknown"


def _detect_price_level(normalized: str) -> str:
    if any(token in normalized for token in ("cheap", "budget", "low cost")):
        return "low"
    if any(token in normalized for token in ("便宜", "低价", "低成本", "平价")):
        return "low"
    if any(token in normalized for token in ("premium", "luxury")):
        return "premium"
    if any(token in normalized for token in ("高端", "高价", "高客单")):
        return "premium"
    return "unknown" if not normalized.strip() else "mid"


def _detect_sourcing(normalized: str) -> str:
    if "1688" in normalized:
        return "1688"
    if "淘宝" in normalized:
        return "taobao"
    if "taobao" in normalized:
        return "taobao"
    if "亚马逊" in normalized:
        return "amazon"
    if "amazon" in normalized:
        return "amazon"
    return "unknown"


def _derive_transcript_keywords(category: str | None, normalized: str) -> list[str]:
    keywords: list[str] = []
    if category:
        for value in CATEGORY_KEYWORDS.get(category, [category]):
            if value and value not in keywords:
                keywords.append(value)
    for value in _extract_transcript_keywords(normalized):
        if value and value not in keywords:
            keywords.append(value)
    if "cheap" in normalized and "budget" not in keywords:
        keywords.append("budget")
    if "trending" in normalized and "trending" not in keywords:
        keywords.append("trending")
    return keywords[:6]


def _derive_selling_points(normalized: str, category: str | None) -> list[str]:
    points: list[str] = []
    if "trending" in normalized:
        points.append("具备趋势热度信号")
    if category == "wireless earbuds":
        points.append("无线便携，适合高频消费电子场景")
    if category == "pet fountain":
        points.append("自动循环饮水提升宠物喂养便利性")
    if not points:
        points.append("转写内容强调了明确的产品需求与采购目的")
    return points


def _derive_pain_points(normalized: str) -> list[str]:
    pains: list[str] = []
    if "cheap" in normalized or "budget" in normalized:
        pains.append("用户对采购成本敏感")
    if "便宜" in normalized or "低价" in normalized:
        pains.append("用户对采购成本敏感")
    if "source" in normalized or "sourced" in normalized:
        pains.append("需要稳定的国内供货渠道")
    if "进货" in normalized or "供货" in normalized or "1688" in normalized:
        pains.append("需要稳定的国内供货渠道")
    if not pains:
        pains.append("需要把口语化需求压缩成可检索的结构化关键词")
    return pains


def _derive_use_cases(category: str | None) -> list[str]:
    if category == "kitchen storage":
        return ["厨房收纳与整理"]
    if category == "wireless earbuds":
        return ["通勤与运动佩戴"]
    if category == "pet fountain":
        return ["家庭宠物日常喂养"]
    return ["跨境选品与国内寻源"]


def _derive_target_audience(normalized: str, category: str | None) -> list[str]:
    audience: list[str] = []
    if category == "pet fountain":
        audience.append("猫狗等宠物家庭")
    if category == "wireless earbuds":
        audience.append("消费电子用户")
    if "us" in normalized:
        audience.append("美国市场用户")
    if "美国" in normalized:
        audience.append("美国市场用户")
    if not audience:
        audience.append("跨境电商卖家")
    return audience


def _extract_transcript_keywords(normalized: str) -> list[str]:
    if not normalized.strip():
        return []
    if _contains_han(normalized):
        if "饮水机" in normalized or "饮水器" in normalized:
            return ["宠物自动饮水机", "猫饮水机", "自动饮水器", "宠物饮水机"]
        if "耳机" in normalized:
            return ["wireless earbuds", "bluetooth earbuds", "true wireless earbuds"]
        if "收纳" in normalized or "展示架" in normalized:
            return ["desk organizer", "desktop organizer", "acrylic organizer"]
        results: list[str] = []
        for match in HAN_SEGMENT_PATTERN.finditer(normalized):
            candidate = match.group().strip()
            if candidate and candidate not in results:
                results.append(candidate)
            if len(results) >= 4:
                break
        return results

    cleaned = re.sub(r"[^a-z0-9\s]+", " ", normalized)
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    meaningful_tokens = [
        token for token in cleaned.split()
        if token and len(token) >= 3 and not token.isdigit() and token not in TRANSCRIPT_STOP_WORDS
    ]
    phrase_keywords: list[str] = []
    for index, token in enumerate(meaningful_tokens):
        if token not in phrase_keywords:
            phrase_keywords.append(token)
        if index + 1 < len(meaningful_tokens):
            phrase = f"{token} {meaningful_tokens[index + 1]}"
            if phrase not in phrase_keywords:
                phrase_keywords.append(phrase)
        if len(phrase_keywords) >= 6:
            break
    return phrase_keywords


def _build_reasoning_user_prompt(request: LlmReasoningRequest) -> str:
    return f"step_name: {request.stepName}\nprompt: {request.prompt}\ncontext: {request.context}"


def _build_transcript_user_prompt(request: LlmTranscriptIntentRequest) -> str:
    return f"source_type: {request.sourceType}\ntranscript: {request.transcript}"


def _required_text(content: dict[str, Any], field_name: str) -> str:
    value = content.get(field_name)
    if not isinstance(value, str) or not value.strip():
        raise LlmClientError(f"LLM response field `{field_name}` is missing.")
    return value.strip()


def _optional_text(content: dict[str, Any], field_name: str) -> str | None:
    value = content.get(field_name)
    if isinstance(value, str) and value.strip():
        return value.strip()
    return None


def _required_string_list(content: dict[str, Any], field_name: str) -> list[str]:
    values = _optional_string_list(content, field_name)
    if not values:
        raise LlmClientError(f"LLM response field `{field_name}` is missing.")
    return values


def _optional_string_list(content: dict[str, Any], field_name: str) -> list[str]:
    value = content.get(field_name)
    if not isinstance(value, list):
        return []
    return [str(item).strip() for item in value if str(item).strip()]


def _required_confidence(value: Any) -> float:
    try:
        confidence = float(value)
    except (TypeError, ValueError) as exc:
        raise LlmClientError("LLM response field `confidence_score` is invalid.") from exc
    return max(0.0, min(1.0, confidence))


def _normalize_fallback_reason(fallback_reason: str | None) -> str:
    if not fallback_reason or not fallback_reason.strip():
        return "LLM 实时接口未启用，当前使用结构化叙事模板生成说明文案。"
    return fallback_reason.strip()


def _format_currency(value: float | None, fallback: str) -> str:
    if value is None:
        return fallback
    return f"¥{Decimal(str(value)).normalize()}"


def _format_number(value: float | None, fallback: str) -> str:
    if value is None:
        return fallback
    return str(Decimal(str(value)).normalize())


def _contains_han(value: str) -> bool:
    return any("\u4e00" <= char <= "\u9fff" for char in value)


def _now_iso() -> str:
    return datetime.now(timezone.utc).astimezone().isoformat()


def _to_json(model: Any) -> str:
    if hasattr(model, "model_dump"):
        return json.dumps(model.model_dump(), ensure_ascii=False)
    if hasattr(model, "dict"):
        return json.dumps(model.dict(), ensure_ascii=False)
    return json.dumps(model, ensure_ascii=False)
