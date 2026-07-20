import shutil
from pathlib import Path

from fastapi import Depends, FastAPI, Form, HTTPException, UploadFile
from fastapi.responses import FileResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from redis import Redis
from rq import Queue
from sqlalchemy import desc
from sqlalchemy.orm import Session

from app.config import settings
from app.db import Base, engine, get_db
from app.models import Job, JobStatus
from app.separator import STEM_NAMES
from app.storage import job_dir
from app.worker.run import QUEUE_NAME
from app.worker.tasks import run_separation

app = FastAPI(title="Music Stem Separation API")

redis_conn = Redis.from_url(settings.redis_url)
queue = Queue(QUEUE_NAME, connection=redis_conn)

AUDIO_EXTENSIONS = {".flac", ".wav", ".mp3", ".m4a", ".ogg"}
ENGINES = {"mock", "real"}
QUALITIES = {"fast", "lossless"}
DEFAULT_QUALITY = "fast"


@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(engine)
    settings.test_tracks_dir.mkdir(parents=True, exist_ok=True)


def _validate_engine(engine_value: str | None) -> str | None:
    if engine_value is not None and engine_value not in ENGINES:
        raise HTTPException(status_code=400, detail=f"engine must be one of {sorted(ENGINES)}")
    return engine_value


def _validate_quality(quality_value: str | None) -> str:
    if quality_value is None:
        return DEFAULT_QUALITY
    if quality_value not in QUALITIES:
        raise HTTPException(status_code=400, detail=f"quality must be one of {sorted(QUALITIES)}")
    return quality_value


@app.post("/jobs", status_code=202)
async def create_job(
    file: UploadFile,
    engine_choice: str | None = Form(None, alias="engine"),
    quality: str | None = Form(None),
    db: Session = Depends(get_db),
):
    _validate_engine(engine_choice)
    quality = _validate_quality(quality)
    job = Job(
        input_filename=file.filename, status=JobStatus.queued,
        engine=engine_choice or settings.separator_mode, quality=quality,
    )
    db.add(job)
    db.commit()

    out_dir = job_dir(job.id)
    out_dir.mkdir(parents=True, exist_ok=True)
    input_path = out_dir / f"input_{file.filename}"
    with input_path.open("wb") as f:
        shutil.copyfileobj(file.file, f)

    queue.enqueue(run_separation, job.id, file.filename, engine_choice, quality, job_timeout="30m")

    return {"job_id": job.id, "status": job.status}


@app.get("/local-tracks")
async def list_local_tracks():
    tracks = []
    for path in sorted(settings.test_tracks_dir.glob("*")):
        if path.is_file() and path.suffix.lower() in AUDIO_EXTENSIONS:
            stat = path.stat()
            tracks.append({"filename": path.name, "size_bytes": stat.st_size, "modified_at": stat.st_mtime})
    return tracks


class LocalJobRequest(BaseModel):
    filename: str
    engine: str | None = None
    quality: str | None = None


@app.post("/jobs/local", status_code=202)
async def create_local_job(body: LocalJobRequest, db: Session = Depends(get_db)):
    _validate_engine(body.engine)
    quality = _validate_quality(body.quality)

    # Reject path traversal / anything that isn't a bare filename in test_tracks_dir.
    if body.filename != Path(body.filename).name:
        raise HTTPException(status_code=400, detail="Invalid filename")
    source_path = settings.test_tracks_dir / body.filename
    if not source_path.is_file():
        raise HTTPException(status_code=404, detail="Track not found in test_tracks/")

    job = Job(
        input_filename=body.filename, status=JobStatus.queued,
        engine=body.engine or settings.separator_mode, quality=quality,
    )
    db.add(job)
    db.commit()

    out_dir = job_dir(job.id)
    out_dir.mkdir(parents=True, exist_ok=True)
    input_path = out_dir / f"input_{body.filename}"
    input_path.symlink_to(source_path.resolve())

    queue.enqueue(run_separation, job.id, body.filename, body.engine, quality, job_timeout="30m")

    return {"job_id": job.id, "status": job.status}


@app.get("/jobs")
async def list_jobs(db: Session = Depends(get_db)):
    jobs = db.query(Job).order_by(desc(Job.created_at)).limit(50).all()
    return [
        {
            "job_id": j.id, "status": j.status, "created_at": j.created_at,
            "input_filename": j.input_filename, "engine": j.engine, "quality": j.quality,
        }
        for j in jobs
    ]


@app.get("/jobs/{job_id}")
async def get_job(job_id: str, db: Session = Depends(get_db)):
    job = db.get(Job, job_id)
    if job is None:
        raise HTTPException(status_code=404, detail="Job not found")

    body = {
        "job_id": job.id, "status": job.status, "created_at": job.created_at,
        "engine": job.engine, "quality": job.quality, "input_filename": job.input_filename,
        "progress": job.progress, "stage": job.stage,
    }
    if job.status == JobStatus.done:
        body["stems"] = {name: f"/jobs/{job.id}/stems/{name}" for name in (job.stem_paths or {})}
    if job.status == JobStatus.failed:
        body["error"] = job.error
    return body


@app.get("/jobs/{job_id}/stems/{stem_name}")
async def get_job_stem(job_id: str, stem_name: str, db: Session = Depends(get_db)):
    if stem_name not in STEM_NAMES:
        raise HTTPException(status_code=404, detail=f"Unknown stem: {stem_name}")

    job = db.get(Job, job_id)
    if job is None or job.status != JobStatus.done or not job.stem_paths or stem_name not in job.stem_paths:
        raise HTTPException(status_code=404, detail="Stem not found")

    stem_path = job_dir(job_id) / job.stem_paths[stem_name]
    if not stem_path.exists():
        raise HTTPException(status_code=404, detail="Stem not found on disk")
    return FileResponse(stem_path, media_type="audio/mp4")


@app.get("/health")
async def health():
    return {"status": "ok", "separator_mode": settings.separator_mode}


@app.get("/")
async def root():
    return RedirectResponse(url="/ui/")


@app.get("/favicon.ico", status_code=204)
async def favicon():
    return None


FRONTEND_DIR = Path(__file__).resolve().parents[1] / "frontend"
app.mount("/ui", StaticFiles(directory=FRONTEND_DIR, html=True), name="ui")
