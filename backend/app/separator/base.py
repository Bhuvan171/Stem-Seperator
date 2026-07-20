from pathlib import Path
from typing import Callable, Protocol

STEM_NAMES = ["vocals", "drums", "bass", "guitar", "piano", "other"]

# Granular sub-states while a job's status is "processing". Only entered when the
# separator actually reaches that phase -- never guessed/simulated.
STAGE_UPLOADING = "uploading"
STAGE_SEPARATING = "separating"
STAGE_DOWNLOADING = "downloading"
STAGE_FINALIZING = "finalizing"

# Called as separation moves through real phases: (stage, percent). percent is only
# non-None during STAGE_SEPARATING, where it's the model's own chunk progress (0-100)
# -- never a synthetic/fake estimate.
ProgressCallback = Callable[[str, float | None], None]


class Separator(Protocol):
    def separate(
        self,
        input_path: Path,
        out_dir: Path,
        on_progress: ProgressCallback | None = None,
        lossless: bool = False,
    ) -> dict[str, Path]:
        """mix file -> {stem_name: path_to_AAC_stem}"""
        ...
