package com.musicapp.stemseparator.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.musicapp.stemseparator.data.model.STEM_NAMES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import kotlin.math.abs

private const val MASTER_STEM = "vocals"
private const val DRIFT_CORRECT_MS = 2000L
private const val DRIFT_THRESHOLD_MS = 150L
private const val POSITION_TICK_MS = 200L

data class StemUiState(val volume: Float = 1f, val muted: Boolean = false)

data class MixerUiState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val stems: Map<String, StemUiState> = STEM_NAMES.associateWith { StemUiState() },
    val soloedStem: String? = null,
    val filename: String? = null,
)

/**
 * The mixer engine: 6 independent ExoPlayer instances (one per stem), "vocals" as
 * master clock, mute/solo/volume implemented as gain gating, and a periodic hard-snap
 * drift correction. A 1:1 port of app.js's openResult()/correctDrift()/
 * applyEffectiveVolume() (backend/frontend/app.js:328-463). Owned by
 * [com.musicapp.stemseparator.ui.result.ResultViewModel] -- playback is foreground-only
 * and stops when the user leaves the Result screen (`release()` is called from
 * `onCleared()`), so this class's lifetime is scoped to that ViewModel's.
 */
class StemMixerController(
    private val callFactory: Call.Factory,
    private val coroutineScope: CoroutineScope,
) {
    private val players: MutableMap<String, ExoPlayer> = mutableMapOf()
    private var currentJobId: String? = null
    private var driftJob: Job? = null
    private var positionTickJob: Job? = null

    private val _uiState = MutableStateFlow(MixerUiState())
    val uiState: StateFlow<MixerUiState> = _uiState.asStateFlow()

    val masterPlayer: ExoPlayer? get() = players[MASTER_STEM]

    /** Idempotent -- builds the 6 players once; loadJob() only swaps media items after this. */
    fun ensurePlayers(context: Context) {
        if (players.isNotEmpty()) return

        val dataSourceFactory = OkHttpDataSource.Factory(callFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)
        val sharedAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        for (name in STEM_NAMES) {
            // Only the master requests/handles audio focus; followers mirror whatever
            // the master's focus-driven play/pause state ends up being (see below), so
            // they don't each independently negotiate focus.
            val player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(sharedAttributes, /* handleAudioFocus = */ name == MASTER_STEM)
                .build()
            players[name] = player
        }

        val master = players.getValue(MASTER_STEM)
        master.addListener(object : Player.Listener {
            // Fires for both user/lock-screen-initiated pause and focus-driven pause --
            // one path mirrors both, rather than separate code for "user paused it" vs
            // "a phone call paused it".
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                followers().forEach { if (isPlaying) it.play() else it.pause() }
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startDriftCorrection() else stopDriftCorrection()
            }

            // Fires for seeks from any source (in-app UI, lock screen, programmatic) --
            // mirroring here means seekTo() below never needs to fan out manually.
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                followers().forEach { it.seekTo(newPosition.positionMs) }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_TIMELINE_CHANGED) || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    if (player.duration > 0) {
                        _uiState.update { it.copy(durationMs = player.duration) }
                    }
                }
            }
        })
        startPositionTicker()
    }

    private fun followers(): List<ExoPlayer> =
        STEM_NAMES.filter { it != MASTER_STEM }.mapNotNull { players[it] }

    /**
     * No-op if [jobId] is already loaded (e.g. re-binding to an already-playing job).
     * [stemUrls] are the *relative* paths the API returns (e.g. "/jobs/{id}/stems/
     * vocals") -- prefixed with the same placeholder base URL Retrofit uses, so
     * ServerUrlInterceptor rewrites them to the real configured server address at
     * request time, exactly as it does for the REST calls sharing [callFactory].
     */
    fun loadJob(jobId: String, filename: String, stemUrls: Map<String, String>) {
        if (jobId == currentJobId) return
        currentJobId = jobId
        _uiState.update {
            MixerUiState(isReady = false, filename = filename, stems = STEM_NAMES.associateWith { StemUiState() })
        }
        val placeholderBase = com.musicapp.stemseparator.data.network.ServerUrlInterceptor.PLACEHOLDER_BASE_URL.trimEnd('/')
        for (name in STEM_NAMES) {
            val relativePath = stemUrls[name] ?: continue
            val player = players[name] ?: continue
            val mediaItem = MediaItem.Builder()
                .setUri(placeholderBase + relativePath)
                .setMimeType(MimeTypes.AUDIO_MP4)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(filename).setArtist(name).build())
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
        }
        _uiState.update { it.copy(isReady = true) }
    }

    fun togglePlayPause() {
        val master = masterPlayer ?: return
        if (master.isPlaying) master.pause() else master.play()
    }

    fun seekTo(positionMs: Long) {
        masterPlayer?.seekTo(positionMs)
    }

    fun setStemVolume(name: String, volume: Float) {
        _uiState.update { state ->
            val updated = state.stems.toMutableMap()
            updated[name] = (updated[name] ?: StemUiState()).copy(volume = volume)
            state.copy(stems = updated)
        }
        applyEffectiveVolume(name)
    }

    fun toggleMute(name: String) {
        _uiState.update { state ->
            val updated = state.stems.toMutableMap()
            val current = updated[name] ?: StemUiState()
            updated[name] = current.copy(muted = !current.muted)
            state.copy(stems = updated)
        }
        applyEffectiveVolume(name)
    }

    /** Solo is exclusive by construction -- selecting a soloed stem again un-solos it. */
    fun toggleSolo(name: String) {
        _uiState.update { state ->
            state.copy(soloedStem = if (state.soloedStem == name) null else name)
        }
        STEM_NAMES.forEach { applyEffectiveVolume(it) }
    }

    private fun applyEffectiveVolume(name: String) {
        val state = _uiState.value
        val stemState = state.stems[name] ?: return
        val audible = !stemState.muted && (state.soloedStem == null || state.soloedStem == name)
        players[name]?.volume = if (audible) stemState.volume else 0f
    }

    private fun startPositionTicker() {
        positionTickJob?.cancel()
        positionTickJob = coroutineScope.launch {
            while (isActive) {
                masterPlayer?.let { master ->
                    _uiState.update {
                        it.copy(positionMs = master.currentPosition, durationMs = master.duration.coerceAtLeast(0))
                    }
                }
                delay(POSITION_TICK_MS)
            }
        }
    }

    private fun startDriftCorrection() {
        if (driftJob?.isActive == true) return
        driftJob = coroutineScope.launch {
            while (isActive) {
                delay(DRIFT_CORRECT_MS)
                val masterPos = masterPlayer?.currentPosition ?: continue
                followers().forEach { follower ->
                    if (abs(follower.currentPosition - masterPos) > DRIFT_THRESHOLD_MS) {
                        follower.seekTo(masterPos)
                    }
                }
            }
        }
    }

    private fun stopDriftCorrection() {
        driftJob?.cancel()
        driftJob = null
    }

    fun release() {
        stopDriftCorrection()
        positionTickJob?.cancel()
        players.values.forEach { it.release() }
        players.clear()
        currentJobId = null
        _uiState.update { MixerUiState() }
    }
}
