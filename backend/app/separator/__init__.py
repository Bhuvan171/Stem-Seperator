from app.config import settings
from app.separator.base import STEM_NAMES, Separator
from app.separator.gpu import GpuSeparationError, GpuSeparator
from app.separator.mock import MockSeparator, SeparationFailedError

__all__ = [
    "STEM_NAMES", "Separator", "MockSeparator", "GpuSeparator",
    "SeparationFailedError", "GpuSeparationError", "get_separator",
]


def get_separator(engine: str | None = None) -> Separator:
    mode = engine or settings.separator_mode
    if mode == "mock":
        return MockSeparator()
    if mode == "real":
        return GpuSeparator()
    raise ValueError(f"Unknown separator engine: {mode!r}")
