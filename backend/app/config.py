from pathlib import Path

from pydantic_settings import BaseSettings

BACKEND_ROOT = Path(__file__).resolve().parents[1]


class Settings(BaseSettings):
    separator_mode: str = "real"  # "mock" | "real"
    storage_dir: Path = BACKEND_ROOT / "storage"
    mock_stems_dir: Path = BACKEND_ROOT / "fixtures" / "results_bs6stem" / "03"

    # MockSeparator knobs (Phase 6 hardening uses these to simulate bad conditions)
    mock_delay_seconds: float = 3.0
    mock_fail: bool = False

    database_url: str = "postgresql://musicapp:musicapp@localhost:5432/musicapp"
    redis_url: str = "redis://localhost:6379/0"

    retention_days: int = 7

    # Local "drop a file in and separate" workflow (test_tracks/ folder)
    test_tracks_dir: Path = BACKEND_ROOT.parent / "test_tracks"

    # GpuSeparator: reaches the real BS-Roformer model over SSH on a100server2
    gpu_ssh_host: str = "192.168.3.20"
    gpu_ssh_user: str = "bhuvan23171"
    gpu_ssh_key_path: Path = Path.home() / ".ssh" / "id_ed25519"
    gpu_remote_workdir: str = "/home/bhuvan23171/stem-separation-validation"
    gpu_device_id: str = "1"

    class Config:
        env_prefix = "MUSICAPP_"


settings = Settings()
