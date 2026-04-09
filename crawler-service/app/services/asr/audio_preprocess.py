from __future__ import annotations

import shutil
import subprocess
from pathlib import Path


SUPPORTED_EXTENSIONS = {
    ".mp3",
    ".wav",
    ".mp4",
    ".m4a",
    ".webm",
    ".ogg",
}


class AudioPreprocessError(RuntimeError):
    pass


def ensure_supported_extension(path: Path) -> None:
    suffix = path.suffix.lower()
    if suffix not in SUPPORTED_EXTENSIONS:
        raise AudioPreprocessError(
            f"Unsupported file type: {suffix or 'unknown'}. Supported types: mp3, wav, mp4, m4a, webm, ogg."
        )


def normalize_media_to_wav(source_path: Path, target_path: Path) -> None:
    if shutil.which("ffmpeg") is None:
        raise AudioPreprocessError("ffmpeg is not installed or is not available in PATH.")

    command = [
        "ffmpeg",
        "-y",
        "-i",
        str(source_path),
        "-vn",
        "-ac",
        "1",
        "-ar",
        "16000",
        "-c:a",
        "pcm_s16le",
        str(target_path),
    ]
    completed = subprocess.run(
        command,
        capture_output=True,
        text=True,
        check=False,
    )
    if completed.returncode != 0 or not target_path.exists():
        stderr = (completed.stderr or "").strip()
        raise AudioPreprocessError(stderr or "ffmpeg failed to extract or normalize audio.")

