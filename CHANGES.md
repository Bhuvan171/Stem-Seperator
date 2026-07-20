# Music Stem Separator — Project Summary

A web app that splits a music track into 6 stems (vocals, drums, bass, guitar, piano, other)
using a real BS-Roformer model running on a GPU server, with a browser UI to run separations
and play the results back.

Run it with: `docker compose up -d` (from the repo root), then open **http://localhost:8000**.

---

## Current functionality

- **Library screen** — shows audio files dropped into `test_tracks/` ("Ready to Separate"),
  plus a history of past separations with status, engine, and quality badges.
- **Drop-in workflow** — put any audio file in `test_tracks/`, it shows up in the app automatically.
- **Manual upload** — pick a file via the browser instead, if it's not in `test_tracks/`.
- **Quality picker** — choose per separation:
  - **Fast** — compresses the file to 256kbps MP3 before sending it to the GPU (much quicker upload).
  - **Lossless** — sends the original file untouched, no pre-processing at all.
- **Real GPU separation** — actually runs BS-Roformer on the remote A100 server over SSH
  (not a mock/simulation). Produces 6 real stems per track.
- **Live processing stages** — the UI shows the real pipeline as it happens: Queued →
  Uploading track → Separating stems (with a live % progress bar read from the model's own
  output) → Downloading results → Finalizing → Done. Nothing here is faked.
- **6-stem mixer** — on the result screen, all 6 stems play in sync with per-stem mute, solo,
  and volume, plus a shared play/pause and seek bar.
- **Job history** — every past separation is listed with when it ran, which engine/quality it
  used, and its status; tap one to reopen it (no re-processing needed).
- **Automatic cleanup** — a background job deletes old separations after a configurable number
  of days so disk usage stays bounded.

## What's intentionally NOT in the app

- No on-device/mock separation in the UI — every separation goes to the real GPU.
- No audio processing (limiting/normalizing/filtering) is applied to the output stems — what
  the model produces is what gets served, byte for byte (converted to AAC only, nothing else).
- No Android app yet — this is a local web app only, per the current phase of the project.

---

## How it works (brief)

- **Backend**: FastAPI + Postgres (job records) + Redis/RQ (job queue) + a worker process.
- **Separation**: the worker SSHs into the GPU server, uploads the track, runs the model,
  downloads the 6 stems, and converts them to AAC for the browser to stream.
- **Frontend**: plain HTML/CSS/JS (no framework), black-and-white design, no emojis.
- **Deployment**: everything runs via `docker compose up -d` — API, worker, cleanup job,
  Postgres, and Redis all as containers. Only the GPU model itself runs elsewhere, over SSH.

---

## Changes made, in order

1. **Backend scaffolding** — built the core `Separator` interface so the app can swap between
   a mock separator and a real one without changing any other code.
2. **Synchronous API** — first working version: upload a file, get stems back, all in one
   request (blocking).
3. **Async job queue** — switched to background processing: upload returns immediately, a
   worker does the actual work, the client polls for status. Added Postgres to track jobs and
   a cleanup job to delete old ones.
4. **Local web UI** — built the browser interface (Library / Import / Processing / Result
   screens) so the app could be used and tested end to end.
5. **Real GPU integration** — connected the app to the actual BS-Roformer model running on the
   GPU server over SSH: upload the track, run the model, download the 6 real stems.
6. **Drop-in track folder** — added the `test_tracks/` workflow so tracks can be dropped in
   and picked up automatically, without uploading through the browser.
7. **Dockerized the whole stack** — one `docker compose up` command starts everything,
   including SSH access to the GPU server from inside the containers.
8. **Design pass** — restyled the whole app, settling on a clean black-and-white look, no
   emojis, consistent icons throughout.
9. **Cleaned up test/demo clutter** — cleared out development test data and simplified the UI
   once the real pipeline was working, so the app looks production-ready rather than like a
   work-in-progress.
10. **Live progress bar** — instead of a fake/simulated progress bar, wired up real progress by
    reading the model's own output while it runs.
11. **Granular stage tracking** — expanded the coarse "processing" status into the actual
    pipeline stages (uploading, separating, downloading, finalizing) so the user can see
    exactly what's happening at any moment.
12. **Upload speed optimization** — added the "Fast" quality option that compresses the file
    before sending it to the GPU, cutting upload time significantly for large lossless files.
13. **Audio clipping investigation** — diagnosed and iterated on clipping/artifact issues in
    the output stems (tried a limiter, then a static gain trim). Ultimately reverted to
    applying **no audio processing at all** — the app now serves exactly what the model
    outputs, per current direction.

---

## Known limitations

- Very loud stems can occasionally clip at their peaks, since no audio processing is applied
  (this is intentional, per the latest direction — see item 13 above).
- The app depends on SSH access to the GPU server being available; if that server or the
  network link is down, separations will fail.
- No Android/mobile app yet — the original project spec includes one, but work so far has
  focused entirely on the local web app.
