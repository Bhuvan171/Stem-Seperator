import shutil
from datetime import datetime, timedelta, timezone

from app.config import settings
from app.db import SessionLocal
from app.models import Job
from app.storage import job_dir


def delete_expired_jobs() -> int:
    """Delete jobs (row + stems on disk) older than settings.retention_days. Returns count deleted."""
    cutoff = datetime.now(timezone.utc) - timedelta(days=settings.retention_days)
    db = SessionLocal()
    try:
        expired = db.query(Job).filter(Job.created_at < cutoff).all()
        for job in expired:
            shutil.rmtree(job_dir(job.id), ignore_errors=True)
            db.delete(job)
        db.commit()
        return len(expired)
    finally:
        db.close()


if __name__ == "__main__":
    deleted = delete_expired_jobs()
    print(f"Deleted {deleted} expired job(s)")
