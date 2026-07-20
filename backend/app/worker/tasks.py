from app.db import SessionLocal
from app.models import Job, JobStatus
from app.separator import get_separator
from app.storage import job_dir


def run_separation(job_id: str, input_filename: str, engine: str | None = None, quality: str = "fast") -> None:
    db = SessionLocal()
    try:
        job = db.get(Job, job_id)
        if job is None:
            return

        job.status = JobStatus.processing
        db.commit()

        out_dir = job_dir(job_id)
        input_path = out_dir / f"input_{input_filename}"

        def on_progress(stage: str, pct: float | None) -> None:
            job.stage = stage
            job.progress = pct
            db.commit()

        try:
            stems = get_separator(engine).separate(
                input_path, out_dir, on_progress=on_progress, lossless=(quality == "lossless")
            )
        except Exception as e:
            job.status = JobStatus.failed
            job.error = str(e)
            db.commit()
            return
        finally:
            input_path.unlink(missing_ok=True)

        job.stem_paths = {name: path.name for name, path in stems.items()}
        job.status = JobStatus.done
        job.stage = None
        db.commit()
    finally:
        db.close()
