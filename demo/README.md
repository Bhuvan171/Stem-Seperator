# Demo sample

A real, verifiable example of what this app produces — not synthetic/placeholder data.

- `input.flac` — the original, full-length track (`test_tracks/03.flac`, 4:07),
  unmodified.
- `output/{vocals,drums,bass,guitar,piano,other}.m4a` — the actual BS-Roformer output
  for that same track, transcoded to AAC 128kbps (the exact format the backend serves
  in production — see `backend/app/separator/transcode.py`). Transcoding these from the
  original ~522MB of raw WAV down to ~22MB is exactly what the app itself does before
  serving stems to a client, so this is a faithful, not degraded, sample.

Play `input.flac` alongside the 6 files in `output/` to hear the real separation
quality without needing your own GPU server. Generated from a real forward pass run on
2026-07-14 (see `backend/fixtures/README.md` for the full generation details, if that
directory still exists locally — it's excluded from the repo since the full raw-WAV
fixtures are ~1.1GB).
