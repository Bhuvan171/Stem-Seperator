# Stem Separator

Splits a music track into 6 stems — vocals, drums, bass, guitar, piano, other — using a
real GPU model over SSH, with a web app and a native Android app to run separations and
play the results back with a per-stem mixer (mute/solo/volume, synced playback).

## What it does

- Drop an audio file into `test_tracks/` (or upload one manually) and pick a quality
  (Fast = compressed transport to the GPU, Lossless = original bytes).
- The backend uploads it to a remote GPU box, runs the separation model, and streams
  live progress back (queued → uploading → separating → downloading → finalizing).
- Play back all 6 stems in sync, with independent mute/solo/volume per stem.
- Past separations are kept as history and can be reopened without reprocessing;
  old ones are cleaned up automatically after a configurable retention period.

## Stack

- **Backend**: FastAPI + PostgreSQL (job records) + Redis/RQ (job queue) + a worker
  process, all containerized via Docker Compose.
- **Separation**: the worker SSHs into a remote GPU server, runs the model, pulls the 6
  stems back, and transcodes them to AAC for streaming.
- **Web frontend**: plain HTML/CSS/JS, no framework — served directly by the API.
- **Android app**: Kotlin + Jetpack Compose, Retrofit/OkHttp, Media3 (ExoPlayer) for the
  6-stem mixer.

## Model

BS-Roformer (6-stem configuration), run on a remote NVIDIA GPU server and reached over
SSH — the model itself is not bundled in this repo.

## Setup

1. **Configure the GPU connection** — copy `.env.example` to `.env` (or create `.env`)
   and set:
   ```
   SEPARATOR=real
   GPU_SSH_KEY=/path/to/your/ssh/private_key
   GPU_SSH_HOST=<gpu-server-ip>
   GPU_SSH_USER=<gpu-server-username>
   GPU_REMOTE_WORKDIR=<path to the model's working directory on the GPU server>
   GPU_DEVICE_ID=<CUDA device index to use>
   ```
   Use `SEPARATOR=mock` instead to run without a GPU, for local UI testing.

2. **Start the stack**:
   ```
   docker compose up -d
   ```

3. **Web app**: open `http://localhost:8000`.

4. **Android app**: build `android/` in Android Studio (or `./gradlew assembleDebug`),
   install it on a phone on the same LAN as the backend, and enter
   `http://<backend-host-ip>:8000` on first launch.

5. **Try it**: drop an audio file into `test_tracks/` and it'll show up in both apps
   under "Ready to Separate." No GPU handy? See `demo/` for a real input track plus its
   actual separated stems, so you can verify output quality without running a job.
