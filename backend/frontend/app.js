const STEM_NAMES = ["vocals", "drums", "bass", "guitar", "piano", "other"];
const POLL_INTERVAL_MS = 1500;
const DRIFT_CORRECT_MS = 2000;
const DRIFT_THRESHOLD_S = 0.15;

const STAGES = [
  { key: "queued", label: "Queued" },
  { key: "uploading", label: "Uploading track" },
  { key: "separating", label: "Separating stems" },
  { key: "downloading", label: "Downloading results" },
  { key: "finalizing", label: "Finalizing" },
  { key: "done", label: "Done" },
];

const views = {
  library: document.getElementById("view-library"),
  import: document.getElementById("view-import"),
  processing: document.getElementById("view-processing"),
  result: document.getElementById("view-result"),
};

let pollTimer = null;
let driftTimer = null;
let stems = {}; // name -> { audio, volume, muted }
let soloedStem = null;
let isPlaying = false;
let pendingLocalFilename = null;
let pendingUploadFile = null;

function showView(name) {
  Object.values(views).forEach((v) => v.classList.add("hidden"));
  views[name].classList.remove("hidden");
}

function fmtTime(sec) {
  if (!isFinite(sec)) return "0:00";
  const m = Math.floor(sec / 60);
  const s = Math.floor(sec % 60).toString().padStart(2, "0");
  return `${m}:${s}`;
}

function fmtBytes(bytes) {
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function engineBadge(engine) {
  if (engine === "real") {
    return `<span class="badge badge-gpu"><svg class="icon"><use href="#icon-cpu"/></svg>GPU</span>`;
  }
  return `<span class="badge badge-mock"><svg class="icon"><use href="#icon-cpu"/></svg>Mock</span>`;
}

function qualityBadge(quality) {
  if (quality === "lossless") {
    return `<span class="badge badge-lossless"><svg class="icon"><use href="#icon-waveform"/></svg>Lossless</span>`;
  }
  return `<span class="badge badge-fast"><svg class="icon"><use href="#icon-bolt"/></svg>Fast</span>`;
}

// ---------- Library: local test_tracks ----------

async function loadLocalTracks() {
  const res = await fetch("/local-tracks");
  const tracks = await res.json();
  const list = document.getElementById("local-track-list");
  const empty = document.getElementById("local-tracks-empty");
  list.innerHTML = "";
  empty.classList.toggle("hidden", tracks.length > 0);

  for (const track of tracks) {
    const li = document.createElement("li");
    li.className = "track-card";
    li.innerHTML = `
      <span class="track-icon"><svg class="icon"><use href="#icon-music"/></svg></span>
      <span class="track-info">
        <span class="track-name">${track.filename}</span>
        <span class="track-meta">${fmtBytes(track.size_bytes)}</span>
      </span>
      <span class="track-actions">
        <button class="btn btn-accent btn-sm separate-btn" data-filename="${track.filename}">Separate</button>
      </span>
    `;
    list.appendChild(li);
  }

  list.querySelectorAll(".separate-btn").forEach((btn) => {
    btn.addEventListener("click", () => openQualitySheet(btn.dataset.filename, null));
  });
}

// ---------- Quality picker sheet ----------

function openQualitySheet(filename, uploadFileObj) {
  pendingLocalFilename = uploadFileObj ? null : filename;
  pendingUploadFile = uploadFileObj || null;
  document.getElementById("quality-sheet-filename").textContent = filename;
  document.getElementById("quality-sheet-backdrop").classList.remove("hidden");
}

function closeQualitySheet() {
  document.getElementById("quality-sheet-backdrop").classList.add("hidden");
  pendingLocalFilename = null;
  pendingUploadFile = null;
}

document.getElementById("quality-sheet-cancel").addEventListener("click", closeQualitySheet);
document.getElementById("quality-sheet-backdrop").addEventListener("click", (e) => {
  if (e.target.id === "quality-sheet-backdrop") closeQualitySheet();
});
document.querySelectorAll(".choice-option").forEach((btn) => {
  btn.addEventListener("click", () => {
    const quality = btn.dataset.quality;
    const filename = pendingLocalFilename;
    const uploadFileObj = pendingUploadFile;
    closeQualitySheet();
    if (uploadFileObj) {
      uploadFile(uploadFileObj, quality);
    } else if (filename) {
      submitLocalJob(filename, quality);
    }
  });
});

async function submitLocalJob(filename, quality) {
  openProcessing(null, filename, "real", quality);
  const res = await fetch("/jobs/local", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ filename, quality }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    showProcessingError(body.detail || `Failed to start job (HTTP ${res.status})`);
    return;
  }
  const body = await res.json();
  pollJob(body.job_id);
}

// ---------- Library: job history ----------

async function loadLibrary() {
  const res = await fetch("/jobs");
  const jobs = await res.json();
  const list = document.getElementById("library-list");
  const empty = document.getElementById("library-empty");
  list.innerHTML = "";
  empty.classList.toggle("hidden", jobs.length > 0);

  for (const job of jobs) {
    const li = document.createElement("li");
    li.className = "job-item";
    li.innerHTML = `
      <span class="job-icon"><svg class="icon"><use href="#icon-music"/></svg></span>
      <span class="job-info">
        <span class="job-name">${job.input_filename}</span>
        <span class="job-meta">${new Date(job.created_at).toLocaleString()} ${engineBadge(job.engine)} ${qualityBadge(job.quality)}</span>
      </span>
      <span class="status-pill ${job.status}">${job.status}</span>
    `;
    li.addEventListener("click", () => selectJob(job.job_id, job.status));
    list.appendChild(li);
  }
}

function selectJob(jobId, status) {
  if (status === "done") {
    openResult(jobId);
  } else {
    openProcessing(jobId, null, null, null);
  }
}

function refreshLibraryScreen() {
  loadLocalTracks();
  loadLibrary();
}

document.getElementById("btn-new").addEventListener("click", () => showView("import"));
document.getElementById("btn-back-from-import").addEventListener("click", () => showView("library"));
document.getElementById("btn-back-from-result").addEventListener("click", () => {
  teardownResult();
  showView("library");
  refreshLibraryScreen();
});

// ---------- Import + Upload (manual file picker, runs on the real GPU) ----------

document.getElementById("file-input").addEventListener("change", (e) => {
  const file = e.target.files[0];
  if (file) openQualitySheet(file.name, file);
});

function uploadFile(file, quality) {
  openProcessing(null, file.name, "real", quality);
  document.getElementById("upload-block").classList.remove("hidden");

  const form = new FormData();
  form.append("file", file);
  form.append("quality", quality);

  const xhr = new XMLHttpRequest();
  xhr.open("POST", "/jobs");

  xhr.upload.addEventListener("progress", (e) => {
    if (e.lengthComputable) {
      const pct = Math.round((e.loaded / e.total) * 100);
      document.getElementById("upload-fill").style.width = pct + "%";
      document.getElementById("upload-pct").textContent = pct + "%";
    }
  });

  xhr.addEventListener("load", () => {
    document.getElementById("upload-block").classList.add("hidden");
    if (xhr.status === 202) {
      const body = JSON.parse(xhr.responseText);
      pollJob(body.job_id);
    } else {
      showProcessingError(`Upload failed (HTTP ${xhr.status})`);
    }
  });

  xhr.addEventListener("error", () => showProcessingError("Upload failed (network error)"));
  xhr.send(form);
}

// ---------- Processing / polling ----------

function openProcessing(jobId, filename, engine, quality) {
  showView("processing");
  document.getElementById("processing-filename").textContent = filename || "";
  document.getElementById("processing-error").classList.add("hidden");
  document.getElementById("upload-block").classList.add("hidden");
  document.getElementById("processing-engine-badge").innerHTML = engine ? engineBadge(engine) : "";
  document.getElementById("processing-quality-badge").innerHTML = quality ? qualityBadge(quality) : "";
  renderStageStepper();
  updateStageStepper("queued", null, null);
  if (jobId) pollJob(jobId);
}

function renderStageStepper() {
  const container = document.getElementById("stage-stepper");
  container.innerHTML = STAGES.map((s) => `
    <div class="stage-step" data-stage="${s.key}">
      <span class="stage-step-dot"></span>
      <span class="stage-step-body">
        <span class="stage-step-label">${s.label}</span>
        ${s.key === "separating" ? `
          <div class="progress-block hidden" id="separation-block">
            <div class="progress-bar"><div id="separation-fill" class="progress-fill"></div></div>
          </div>
        ` : ""}
      </span>
    </div>
  `).join("");
}

function updateStageStepper(status, stage, progress) {
  const isDone = status === "done";
  const isFailed = status === "failed";
  const currentKey = isDone ? "done" : (stage || "queued");
  const currentIdx = STAGES.findIndex((s) => s.key === currentKey);

  document.querySelectorAll(".stage-step").forEach((el) => {
    const idx = STAGES.findIndex((s) => s.key === el.dataset.stage);
    const complete = idx < currentIdx || (isDone && idx <= currentIdx);
    const active = !isDone && !isFailed && idx === currentIdx;
    el.classList.toggle("complete", complete);
    el.classList.toggle("active", active);
    el.querySelector(".stage-step-dot").innerHTML = complete
      ? '<svg class="icon"><use href="#icon-check"/></svg>'
      : "";
  });

  setSeparationProgress(!isDone && !isFailed && stage === "separating" ? progress : null);
}

function setSeparationProgress(pct) {
  const block = document.getElementById("separation-block");
  if (!block) return;
  if (pct === null || pct === undefined) {
    block.classList.add("hidden");
    return;
  }
  block.classList.remove("hidden");
  document.getElementById("separation-fill").style.width = Math.round(pct) + "%";
}

function showProcessingError(msg) {
  const el = document.getElementById("processing-error");
  el.textContent = msg;
  el.classList.remove("hidden");
}

function pollJob(jobId) {
  clearInterval(pollTimer);
  showView("processing");

  const tick = async () => {
    const res = await fetch(`/jobs/${jobId}`);
    if (!res.ok) {
      clearInterval(pollTimer);
      showProcessingError("Job not found");
      return;
    }
    const job = await res.json();
    document.getElementById("processing-filename").textContent = job.input_filename || "";
    document.getElementById("processing-engine-badge").innerHTML = engineBadge(job.engine);
    document.getElementById("processing-quality-badge").innerHTML = qualityBadge(job.quality);
    updateStageStepper(job.status, job.stage, job.progress);

    if (job.status === "done") {
      clearInterval(pollTimer);
      openResult(jobId);
    } else if (job.status === "failed") {
      clearInterval(pollTimer);
      showProcessingError(job.error || "Separation failed");
    }
  };

  tick();
  pollTimer = setInterval(tick, POLL_INTERVAL_MS);
}

// ---------- Result / stem playback ----------

async function openResult(jobId) {
  teardownResult();
  const res = await fetch(`/jobs/${jobId}`);
  const job = await res.json();
  if (job.status !== "done") {
    openProcessing(jobId, job.input_filename, job.engine, job.quality);
    return;
  }

  showView("result");
  document.getElementById("result-filename").textContent = job.input_filename;
  document.getElementById("result-engine-badge").innerHTML = engineBadge(job.engine);
  document.getElementById("result-quality-badge").innerHTML = qualityBadge(job.quality);

  const stemList = document.getElementById("stem-list");
  stemList.innerHTML = "";
  soloedStem = null;
  isPlaying = false;
  setPlayIcon(false);

  for (const name of STEM_NAMES) {
    const url = job.stems[name];
    if (!url) continue;

    const audio = new Audio(url);
    audio.preload = "auto";
    audio.loop = false;
    stems[name] = { audio, volume: 1, muted: false };

    const row = document.createElement("div");
    row.className = "stem-row";
    row.innerHTML = `
      <span class="stem-name">${name}</span>
      <button class="stem-btn mute-btn" data-stem="${name}" aria-label="Mute ${name}"><svg class="icon"><use href="#icon-mute"/></svg></button>
      <button class="stem-btn solo-btn" data-stem="${name}" aria-label="Solo ${name}"><svg class="icon"><use href="#icon-solo"/></svg></button>
      <input type="range" min="0" max="1" step="0.01" value="1" data-stem="${name}" class="volume-slider" aria-label="${name} volume" />
    `;
    stemList.appendChild(row);
  }

  stemList.querySelectorAll(".mute-btn").forEach((btn) => {
    btn.addEventListener("click", () => toggleMute(btn.dataset.stem));
  });
  stemList.querySelectorAll(".solo-btn").forEach((btn) => {
    btn.addEventListener("click", () => toggleSolo(btn.dataset.stem));
  });
  stemList.querySelectorAll(".volume-slider").forEach((slider) => {
    slider.addEventListener("input", () => setVolume(slider.dataset.stem, parseFloat(slider.value)));
  });

  // Master timeline driven by the vocals stem (all stems are time-aligned by construction).
  const master = stems["vocals"].audio;
  master.addEventListener("loadedmetadata", () => {
    document.getElementById("seek").max = master.duration;
    updateTimeDisplay();
  });
  master.addEventListener("timeupdate", () => {
    document.getElementById("seek").value = master.currentTime;
    updateTimeDisplay();
  });
  master.addEventListener("ended", () => {
    isPlaying = false;
    setPlayIcon(false);
    clearInterval(driftTimer);
  });

  driftTimer = setInterval(correctDrift, DRIFT_CORRECT_MS);
}

function updateTimeDisplay() {
  const master = stems["vocals"]?.audio;
  if (!master) return;
  document.getElementById("time-display").textContent = `${fmtTime(master.currentTime)} / ${fmtTime(master.duration)}`;
}

function applyEffectiveVolume(name) {
  const s = stems[name];
  if (!s) return;
  const audible = !s.muted && (soloedStem === null || soloedStem === name);
  s.audio.volume = audible ? s.volume : 0;
}

function toggleMute(name) {
  stems[name].muted = !stems[name].muted;
  document.querySelector(`.mute-btn[data-stem="${name}"]`).classList.toggle("mute-on", stems[name].muted);
  applyEffectiveVolume(name);
}

function toggleSolo(name) {
  soloedStem = soloedStem === name ? null : name;
  document.querySelectorAll(".solo-btn").forEach((btn) => {
    btn.classList.toggle("solo-on", btn.dataset.stem === soloedStem);
  });
  STEM_NAMES.forEach(applyEffectiveVolume);
}

function setVolume(name, value) {
  stems[name].volume = value;
  applyEffectiveVolume(name);
}

function correctDrift() {
  if (!isPlaying) return;
  const master = stems["vocals"]?.audio;
  if (!master) return;
  for (const name of STEM_NAMES) {
    const s = stems[name];
    if (!s || s.audio === master) continue;
    if (Math.abs(s.audio.currentTime - master.currentTime) > DRIFT_THRESHOLD_S) {
      s.audio.currentTime = master.currentTime;
    }
  }
}

function setPlayIcon(playing) {
  document.getElementById("playpause-icon").innerHTML = `<use href="#icon-${playing ? "pause" : "play"}"/>`;
  document.getElementById("btn-playpause").setAttribute("aria-label", playing ? "Pause" : "Play");
}

document.getElementById("btn-playpause").addEventListener("click", () => {
  isPlaying = !isPlaying;
  setPlayIcon(isPlaying);
  for (const name of STEM_NAMES) {
    const s = stems[name];
    if (!s) continue;
    if (isPlaying) s.audio.play();
    else s.audio.pause();
  }
});

document.getElementById("seek").addEventListener("input", (e) => {
  const t = parseFloat(e.target.value);
  for (const name of STEM_NAMES) {
    if (stems[name]) stems[name].audio.currentTime = t;
  }
});

function teardownResult() {
  clearInterval(driftTimer);
  for (const name of Object.keys(stems)) {
    stems[name].audio.pause();
    stems[name].audio.src = "";
  }
  stems = {};
}

// ---------- init ----------
showView("library");
refreshLibraryScreen();
