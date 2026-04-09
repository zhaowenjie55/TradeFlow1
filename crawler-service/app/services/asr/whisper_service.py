from __future__ import annotations

import os
import re
from functools import lru_cache
from pathlib import Path
from typing import Optional

from faster_whisper import WhisperModel

from app.models import AsrSegment, AsrTranscriptionResponse


class WhisperInferenceError(RuntimeError):
    pass


LOW_CONFIDENCE_ZH_TEXTS = {
    "讨论",
    "对了",
    "好的",
    "嗯",
    "啊",
    "哦",
    "你好",
    "测试",
}


def _model_size(language: Optional[str]) -> str:
    if language and language.lower().startswith("zh"):
        return os.getenv("ASR_MODEL_SIZE_ZH", os.getenv("ASR_MODEL_SIZE", "small"))
    return os.getenv("ASR_MODEL_SIZE", "small")


def _device() -> str:
    return os.getenv("ASR_DEVICE", "cpu")


def _compute_type() -> str:
    return os.getenv("ASR_COMPUTE_TYPE", "int8")


@lru_cache(maxsize=4)
def _load_model(model_size: str, device: str, compute_type: str) -> WhisperModel:
    try:
        return WhisperModel(model_size, device=device, compute_type=compute_type)
    except Exception as exc:  # pragma: no cover - model init failure depends on env
        raise WhisperInferenceError(f"Unable to initialize faster-whisper model: {exc}") from exc


def transcribe_audio(
    audio_path: Path,
    *,
    task: str = "transcribe",
    language: Optional[str] = None,
) -> AsrTranscriptionResponse:
    try:
        model = _load_model(_model_size(language), _device(), _compute_type())
        primary_text, primary_segments, info, primary_duration = _run_transcription(
            model,
            audio_path,
            task=task,
            language=language,
            vad_filter=True,
            beam_size=int(os.getenv("ASR_BEAM_SIZE", "8") or "8"),
            best_of=int(os.getenv("ASR_BEST_OF", "8") or "8"),
            initial_prompt=_initial_prompt(language),
        )

        if _needs_retry(primary_text, language):
            retry_text, retry_segments, retry_info, retry_duration = _run_transcription(
                model,
                audio_path,
                task=task,
                language=language,
                vad_filter=False,
                beam_size=max(int(os.getenv("ASR_BEAM_SIZE_RETRY", "10") or "10"), 8),
                best_of=max(int(os.getenv("ASR_BEST_OF_RETRY", "10") or "10"), 8),
                initial_prompt=_retry_initial_prompt(language),
            )
            if _is_better_retry(primary_text, retry_text, language):
                primary_text = retry_text
                primary_segments = retry_segments
                info = retry_info
                primary_duration = retry_duration
    except WhisperInferenceError:
        raise
    except Exception as exc:  # pragma: no cover - library failure depends on env
        raise WhisperInferenceError(f"faster-whisper transcription failed: {exc}") from exc

    return AsrTranscriptionResponse(
        success=True,
        language=language or getattr(info, "language", None),
        duration=primary_duration,
        text=primary_text,
        segments=primary_segments,
    )


def _run_transcription(
    model: WhisperModel,
    audio_path: Path,
    *,
    task: str,
    language: Optional[str],
    vad_filter: bool,
    beam_size: int,
    best_of: int,
    initial_prompt: str,
):
    segment_iter, info = model.transcribe(
        str(audio_path),
        task=task,
        language=language,
        vad_filter=vad_filter,
        vad_parameters={
            "min_silence_duration_ms": int(os.getenv("ASR_MIN_SILENCE_MS", "350") or "350"),
            "speech_pad_ms": int(os.getenv("ASR_SPEECH_PAD_MS", "250") or "250"),
        },
        beam_size=beam_size,
        best_of=best_of,
        temperature=0.0,
        condition_on_previous_text=False,
        initial_prompt=initial_prompt,
    )
    segments: list[AsrSegment] = []
    text_parts: list[str] = []
    duration = 0.0
    for segment in segment_iter:
        text = normalize_transcript_text((segment.text or "").strip(), language)
        if not text:
            continue
        start = float(segment.start or 0.0)
        end = float(segment.end or start)
        segments.append(AsrSegment(start=start, end=end, text=text))
        text_parts.append(text)
        duration = max(duration, end)

    if duration <= 0.0:
        info_duration = getattr(info, "duration", None)
        if info_duration is not None:
            duration = float(info_duration)

    combined = combine_segment_texts(text_parts, language)
    return combined, segments, info, duration


def _initial_prompt(language: Optional[str]) -> str:
    if language and language.lower().startswith("zh"):
        return os.getenv(
            "ASR_INITIAL_PROMPT_ZH",
            "这是一个跨境电商选品搜索场景。请优先准确识别中文商品名称和类目词，例如：书包、双肩包、电脑包、猫咪饮水机、宠物饮水机、蓝牙耳机、收纳架、厨房收纳、置物架、旅行背包、学生书包、办公用品。",
        )
    return os.getenv(
        "ASR_INITIAL_PROMPT",
        "This is an ecommerce sourcing search scenario. Prefer accurate product names, category terms, and functional keywords.",
    )


def _retry_initial_prompt(language: Optional[str]) -> str:
    if language and language.lower().startswith("zh"):
        return os.getenv(
            "ASR_RETRY_INITIAL_PROMPT_ZH",
            "请尽量逐字准确识别中文商品词，不要改写，不要联想，不要输出口语寒暄。重点识别：书包、双肩包、背包、电脑包、猫咪饮水机、宠物自动饮水机、蓝牙耳机、收纳盒、置物架、厨房用品、旅行用品。",
        )
    return os.getenv(
        "ASR_RETRY_INITIAL_PROMPT",
        "Transcribe the spoken product query literally and accurately. Avoid filler greetings.",
    )


def combine_segment_texts(text_parts: list[str], language: Optional[str]) -> str:
    if not text_parts:
        return ""
    if language and language.lower().startswith("zh"):
        return normalize_transcript_text("".join(text_parts), language)
    return normalize_transcript_text(" ".join(text_parts), language)


def normalize_transcript_text(text: str, language: Optional[str]) -> str:
    normalized = text.replace("\u3000", " ").strip()
    normalized = re.sub(r"\s+", " ", normalized)
    normalized = normalized.strip(" ,.;:!?，。！？、")
    if language and language.lower().startswith("zh"):
        normalized = normalized.replace(" ", "")
    return normalized


def _needs_retry(text: str, language: Optional[str]) -> bool:
    normalized = normalize_transcript_text(text, language)
    if not normalized:
        return True
    if language and language.lower().startswith("zh"):
        if normalized in LOW_CONFIDENCE_ZH_TEXTS:
            return True
        if len(normalized) <= 2:
            return True
        if re.fullmatch(r"[吗吧啊呀哦嗯哈呵啦]+", normalized):
            return True
    return False


def _is_better_retry(primary_text: str, retry_text: str, language: Optional[str]) -> bool:
    primary = normalize_transcript_text(primary_text, language)
    retry = normalize_transcript_text(retry_text, language)
    if not retry:
        return False
    if _needs_retry(primary, language) and not _needs_retry(retry, language):
        return True
    return len(retry) > len(primary) + 1
