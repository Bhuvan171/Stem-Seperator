import subprocess
from pathlib import Path

AAC_BITRATE = "128k"


def transcode_to_aac(src_wav: Path, dest_dir: Path, stem_name: str) -> Path:
    """Transcode a WAV stem to AAC (~128kbps) so it's small enough to serve to a phone.
    No gain/limiting/filtering of any kind -- passes the model's output straight through."""
    dest_dir.mkdir(parents=True, exist_ok=True)
    dest_path = dest_dir / f"{stem_name}.m4a"
    result = subprocess.run(
        [
            "ffmpeg", "-y", "-loglevel", "error",
            "-i", str(src_wav),
            "-c:a", "aac", "-b:a", AAC_BITRATE,
            str(dest_path),
        ],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(f"ffmpeg transcode failed for {stem_name}: {result.stderr}")
    return dest_path
