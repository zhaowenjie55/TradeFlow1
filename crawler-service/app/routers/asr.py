from __future__ import annotations

import tempfile
from pathlib import Path
from typing import Literal, Optional

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.models import AsrTranscriptionResponse
from app.services.asr.audio_preprocess import (
    AudioPreprocessError,
    ensure_supported_extension,
    normalize_media_to_wav,
)
from app.services.asr.whisper_service import WhisperInferenceError, transcribe_audio


router = APIRouter(tags=["asr"])


@router.post("/api/asr/transcribe", response_model=AsrTranscriptionResponse)
def transcribe(
    file: UploadFile = File(...),
    task: Literal["transcribe", "translate"] = Form(default="transcribe"),
    language: Optional[str] = Form(default=None),
) -> AsrTranscriptionResponse:
    filename = (file.filename or "upload.bin").strip()
    if not filename:
        raise HTTPException(status_code=400, detail="Uploaded file must include a filename.")

    suffix = Path(filename).suffix.lower()
    with tempfile.TemporaryDirectory(prefix="tradeflow-asr-") as temp_dir:
        temp_path = Path(temp_dir)
        source_path = temp_path / f"source{suffix or '.bin'}"
        normalized_path = temp_path / "normalized.wav"

        try:
            ensure_supported_extension(source_path)
            payload = file.file.read()
            if not payload:
                raise HTTPException(status_code=400, detail="Uploaded file is empty.")
            source_path.write_bytes(payload)
            normalize_media_to_wav(source_path, normalized_path)
            return transcribe_audio(normalized_path, task=task, language=(language or None))
        except HTTPException:
            raise
        except AudioPreprocessError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
        except WhisperInferenceError as exc:
            raise HTTPException(status_code=502, detail=str(exc)) from exc
        except Exception as exc:  # pragma: no cover - defensive boundary
            raise HTTPException(status_code=500, detail=f"ASR transcription failed: {exc}") from exc
