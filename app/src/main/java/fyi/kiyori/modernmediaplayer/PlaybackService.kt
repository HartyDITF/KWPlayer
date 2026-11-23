package fyi.kiyori.modernmediaplayer

import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import fyi.kiyori.modernmediaplayer.core.playback.PlaybackEqualizer
import fyi.kiyori.modernmediaplayer.data.database.KwDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setHandleAudioBecomingNoisy(true)
        }
        PlaybackEqualizer.attach(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                PlaybackEqualizer.attach(audioSessionId)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) persistPosition(player)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) persistPosition(player, completed = true)
            }
            override fun onEvents(player: Player, events: Player.Events) {
                if (!player.isPlaying && events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                    persistPosition(player)
                }
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                // Listener callbacks run on the player's application looper (main).
                // Capture player state here; only database work runs on Dispatchers.IO.
                val uri = mediaItem?.localConfiguration?.uri?.toString() ?: return
                serviceScope.launch {
                    val saved = runCatching {
                        KwDatabase.get(this@PlaybackService).mediaDao().getPosition(uri)
                    }.getOrNull()
                    if (saved != null && saved > 0L) {
                        withContext(Dispatchers.Main.immediate) {
                            if (player.currentMediaItem?.localConfiguration?.uri?.toString() == uri) {
                                player.seekTo(saved)
                            }
                        }
                    }
                }
            }
        })
        mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun persistPosition(player: Player, completed: Boolean = false) {
        // Read ExoPlayer state on its application looper before switching to IO.
        val uri = player.currentMediaItem?.localConfiguration?.uri?.toString() ?: return
        val duration = player.duration.takeIf { it > 0L } ?: return
        val position = if (completed) 0L else player.currentPosition.coerceIn(0L, duration)
        serviceScope.launch {
            runCatching {
                KwDatabase.get(this@PlaybackService).mediaDao()
                    .updatePlaybackProgress(uri, position, duration)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        (mediaSession?.player as? ExoPlayer)?.let { persistPosition(it) }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            (player as? ExoPlayer)?.let { persistPosition(it); it.release() }
            release()
        }
        PlaybackEqualizer.release()
        serviceScope.cancel()
        mediaSession = null
        super.onDestroy()
    }
}
