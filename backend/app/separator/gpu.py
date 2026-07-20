import re
import shutil
import subprocess
import time
import uuid
from pathlib import Path

from app.config import settings
from app.separator.base import (
    STAGE_DOWNLOADING,
    STAGE_FINALIZING,
    STAGE_SEPARATING,
    STAGE_UPLOADING,
    STEM_NAMES,
    ProgressCallback,
)
from app.separator.transcode import transcode_to_aac

MSST_DIR = "msst"
MODEL_TYPE = "bs_roformer"
CONFIG_REL = "ckpts/bs_6stem_config.yaml"
CKPT_REL = "ckpts/bs_6stem.ckpt"

# inference.py's tqdm bar: "Processing audio chunks:  50%|####      | ..."
# This is the model's own real progress through the track, not a synthetic estimate.
PROGRESS_RE = re.compile(r"Processing audio chunks:\s+(\d+)%\|")

# Uploading a raw lossless source (FLAC/WAV, often ~90-100MB for a 4min track) over
# the SSH link to the GPU box is the single biggest chunk of pre-progress latency --
# measured ~18.6s for a 93MB FLAC vs ~2.6s for the same track at 256kbps MP3 (7.9MB).
# 256kbps is close to perceptually transparent, and MP3 is proven compatible with
# inference.py's librosa-based loader. Only used for the transport hop when the user
# picks "fast" -- "lossless" skips this and uploads the original file as-is.
TRANSPORT_BITRATE = "256k"


class GpuSeparationError(Exception):
    pass


def _prepare_upload_file(input_path: Path, tmp_dir: Path) -> Path:
    """Returns a smaller file to upload in place of input_path, if transcoding to
    MP3 actually shrinks it (skips already-compressed/small sources where it wouldn't
    help). Falls back to the original on any transcode failure."""
    transport_path = tmp_dir / f"{input_path.stem}_transport.mp3"
    result = subprocess.run(
        [
            "ffmpeg", "-y", "-loglevel", "error",
            "-i", str(input_path), "-vn",
            "-c:a", "libmp3lame", "-b:a", TRANSPORT_BITRATE,
            str(transport_path),
        ],
        capture_output=True, text=True,
    )
    if result.returncode == 0 and transport_path.exists() and transport_path.stat().st_size < input_path.stat().st_size:
        return transport_path
    transport_path.unlink(missing_ok=True)
    return input_path


def _ssh_base():
    return [
        "ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10",
        "-o", "StrictHostKeyChecking=accept-new",
        "-i", str(settings.gpu_ssh_key_path),
        f"{settings.gpu_ssh_user}@{settings.gpu_ssh_host}",
    ]


def _scp_base():
    return [
        "scp", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10",
        "-o", "StrictHostKeyChecking=accept-new",
        "-i", str(settings.gpu_ssh_key_path),
    ]


def _run(cmd: list[str], step: str, timeout: int):
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    if result.returncode != 0:
        raise GpuSeparationError(f"{step} failed: {result.stderr.strip() or result.stdout.strip()}")
    return result


def _run_streaming(cmd: list[str], step: str, timeout: int, on_progress: ProgressCallback | None):
    """Runs cmd, reading its merged output live to parse tqdm's carriage-return-delimited
    progress lines (a regular readline() would swallow \\r-only updates until the process
    exits). Keeps a tail of recent output for error messages."""
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    buf = ""
    tail_lines: list[str] = []
    last_reported = -1
    deadline = time.monotonic() + timeout

    try:
        while True:
            ch = proc.stdout.read(1)
            if ch == "":
                if proc.poll() is not None:
                    break
                continue
            if ch in ("\r", "\n"):
                if buf.strip():
                    tail_lines.append(buf)
                    if len(tail_lines) > 20:
                        tail_lines.pop(0)
                    if on_progress:
                        m = PROGRESS_RE.search(buf)
                        if m:
                            pct = int(m.group(1))
                            if pct != last_reported:
                                last_reported = pct
                                on_progress(STAGE_SEPARATING, float(pct))
                buf = ""
            else:
                buf += ch
            if time.monotonic() > deadline:
                proc.kill()
                raise GpuSeparationError(f"{step} timed out after {timeout}s")
    finally:
        proc.wait()

    if proc.returncode != 0:
        raise GpuSeparationError(f"{step} failed (exit {proc.returncode}): " + " | ".join(tail_lines[-5:]))


class GpuSeparator:
    """Runs the real BS-Roformer 6-stem model on a100server2 over SSH: upload the
    mix, run inference.py against the pre-loaded checkpoint, download the six stems,
    transcode to AAC. Requires SSH key access configured via settings.gpu_ssh_*.
    """

    def separate(
        self,
        input_path: Path,
        out_dir: Path,
        on_progress: ProgressCallback | None = None,
        lossless: bool = False,
    ) -> dict[str, Path]:
        job_tag = uuid.uuid4().hex[:12]
        remote_base = f"{settings.gpu_remote_workdir}/api_jobs/{job_tag}"
        remote_input_dir = f"{remote_base}/input"
        remote_output_dir = f"{remote_base}/output"
        upload_tmp_dir = out_dir / "_upload_tmp"

        try:
            if on_progress:
                on_progress(STAGE_UPLOADING, None)
            _run(_ssh_base() + [f"mkdir -p {remote_input_dir} {remote_output_dir}"], "remote mkdir", 20)

            if lossless:
                upload_path = input_path
            else:
                upload_tmp_dir.mkdir(parents=True, exist_ok=True)
                upload_path = _prepare_upload_file(input_path, upload_tmp_dir)

            remote_input_path = f"{remote_input_dir}/{upload_path.name}"
            _run(_scp_base() + [str(upload_path), f"{settings.gpu_ssh_user}@{settings.gpu_ssh_host}:{remote_input_path}"],
                 "upload mix", 900 if lossless else 300)

            infer_cmd = (
                f"cd {settings.gpu_remote_workdir}/{MSST_DIR} && "
                f"../.venv-msst/bin/python3 inference.py "
                f"--model_type {MODEL_TYPE} "
                f"--config_path {CONFIG_REL} "
                f"--start_check_point {CKPT_REL} "
                f"--input_folder {remote_input_dir} "
                f"--store_dir {remote_output_dir} "
                f"--device_ids {settings.gpu_device_id} "
                f"--pcm_type FLOAT"
            )
            if on_progress:
                on_progress(STAGE_SEPARATING, 0.0)
            _run_streaming(_ssh_base() + [infer_cmd], "GPU inference", 1800, on_progress)
            if on_progress:
                on_progress(STAGE_SEPARATING, 100.0)

            if on_progress:
                on_progress(STAGE_DOWNLOADING, None)
            local_tmp = out_dir / "_gpu_raw"
            local_tmp.mkdir(parents=True, exist_ok=True)
            _run(_scp_base() + ["-r", f"{settings.gpu_ssh_user}@{settings.gpu_ssh_host}:{remote_output_dir}/*",
                                 str(local_tmp)], "download stems", 300)

            if on_progress:
                on_progress(STAGE_FINALIZING, None)
            stems: dict[str, Path] = {}
            wav_files = list(local_tmp.rglob("*.wav"))
            for stem_name in STEM_NAMES:
                match = next((f for f in wav_files if f.stem == stem_name), None)
                if match is None:
                    raise GpuSeparationError(f"GPU output missing stem: {stem_name}")
                stems[stem_name] = transcode_to_aac(match, out_dir, stem_name)

            shutil.rmtree(local_tmp, ignore_errors=True)
            return stems
        finally:
            shutil.rmtree(upload_tmp_dir, ignore_errors=True)
            subprocess.run(_ssh_base() + [f"rm -rf {remote_base}"], capture_output=True, timeout=30)
