package com.pixelpushers.sonixa.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.pixelpushers.sonixa.data.local.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val visualizerController: VisualizerController
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null
        private set

    private val _playbackState = MutableStateFlow(PlaybackStateModel())
    val playbackState: StateFlow<PlaybackStateModel> = _playbackState.asStateFlow()

    private val _currentMediaIndex = MutableStateFlow(0)
    val currentMediaIndex: StateFlow<Int> = _currentMediaIndex.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()?.apply {
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            val active = isPlaying || (playWhenReady && playbackState != Player.STATE_ENDED)
                            _playbackState.value = _playbackState.value.copy(isPlaying = active)
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val active = isPlaying || (playWhenReady && playbackState != Player.STATE_ENDED)
                            if (playbackState == Player.STATE_READY) {
                                _playbackState.value = _playbackState.value.copy(
                                    isPlaying = active,
                                    durationMs = duration.coerceAtLeast(0L)
                                )
                            } else if (playbackState == Player.STATE_ENDED) {
                                if (hasNextMediaItem()) {
                                    seekToNextMediaItem()
                                    prepare()
                                    play()
                                } else {
                                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                                }
                            }
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            _currentMediaIndex.value = currentMediaItemIndex.coerceAtLeast(0)
                        }
                    })
                }
            } catch (_: Exception) {}
        }, MoreExecutors.directExecutor())

        scope.launch {
            while (true) {
                delay(500)
                mediaController?.let { controller ->
                    val active = controller.isPlaying || (controller.playWhenReady && controller.playbackState != Player.STATE_ENDED)
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = active,
                        currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                        durationMs = controller.duration.coerceAtLeast(0L)
                    )
                }
            }
        }
    }

    fun playQueue(queue: List<SongEntity>, startIndex: Int) {
        scope.launch {
            try {
                val controller = mediaController
                if (controller != null && queue.isNotEmpty()) {
                    val mediaItems = queue.map { song ->
                        val metadata = MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setArtworkUri(if (!song.artworkUri.isNullOrEmpty()) Uri.parse(song.artworkUri) else null)
                            .build()

                        MediaItem.Builder()
                            .setUri(song.uri)
                            .setMediaMetadata(metadata)
                            .build()
                    }

                    controller.setMediaItems(mediaItems)
                    controller.seekTo(startIndex.coerceIn(0, queue.size - 1), 0L)
                    controller.prepare()
                    controller.play()
                }
            } catch (_: Exception) {}
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}

data class PlaybackStateModel(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L
)
