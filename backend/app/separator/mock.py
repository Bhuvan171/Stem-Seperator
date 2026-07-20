import time
from pathlib import Path

from app.config import settings
from app.separator.base import STEM_NAMES, ProgressCallback
from app.separator.transcode import transcode_to_aac


class SeparationFailedError(Exception):
    pass


class MockSeparator:
    """Serves the six pre-rendered stems from mock_stems_dir instead of running a model.

    Knobs (env-configurable via Settings, or overridden per-instance) let Phase 6
    hardening simulate failures and slow jobs without touching real infra.
    """

    def __init__(
        self,
        delay_seconds: float | None = None,
        fail: bool | None = None,
        source_dir: Path | None = None,
    ):
        self.delay_seconds = settings.mock_delay_seconds if delay_seconds is None else delay_seconds
        self.fail = settings.mock_fail if fail is None else fail
        self.source_dir = source_dir or settings.mock_stems_dir

    def separate(
        self,
        input_path: Path,
        out_dir: Path,
        on_progress: ProgressCallback | None = None,
        lossless: bool = False,
    ) -> dict[str, Path]:
        # No stage/progress reporting here on purpose: there's no real phased work to
        # measure, and faking it would defeat the point of the real ones being real.
        if self.delay_seconds > 0:
            time.sleep(self.delay_seconds)

        if self.fail:
            raise SeparationFailedError("MockSeparator configured to fail (mock_fail=True)")

        missing = [s for s in STEM_NAMES if not (self.source_dir / f"{s}.wav").exists()]
        if missing:
            raise FileNotFoundError(
                f"Mock stem source missing {missing} in {self.source_dir}. "
                "Place the six *.wav fixtures there (see backend/fixtures/README)."
            )

        stems: dict[str, Path] = {}
        for stem_name in STEM_NAMES:
            src_wav = self.source_dir / f"{stem_name}.wav"
            stems[stem_name] = transcode_to_aac(src_wav, out_dir, stem_name)
        return stems
