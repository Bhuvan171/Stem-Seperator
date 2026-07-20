from pathlib import Path

from app.config import settings


def job_dir(job_id: str) -> Path:
    return settings.storage_dir / job_id
