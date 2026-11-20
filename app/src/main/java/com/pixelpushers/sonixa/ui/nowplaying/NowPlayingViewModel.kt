package com.pixelpushers.sonixa.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpushers.sonixa.data.local.dao.SongDao
import com.pixelpushers.sonixa.data.local.entity.SongEntity
import com.pixelpushers.sonixa.data.preferences.UserSettingsRepository
import com.pixelpushers.sonixa.data.repository.NotificationCategory
import com.pixelpushers.sonixa.data.repository.NotificationRepository
import com.pixelpushers.sonixa.service.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class RepeatMode {
    OFF, ALL, ONE
}

data class PlayerState(
    val currentSong: SongEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isRepeatEnabled: Boolean = false,
    val isSleepTimerActive: Boolean = false,
    val sleepTimerRemainingMs: Long = 0L,
    val queue: List<SongEntity> = emptyList(),
    val currentIndex: Int = 0
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val songDao: SongDao,
    private val notificationRepository: NotificationRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val musicPlayerManager: MusicPlayerManager
) : ViewModel() {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var sleepTimerJob: Job? = null

    private var hasRestoredLastPlayed = false

    init {
        // Observe database changes so queue and currentSong are always in sync with DB updates
        viewModelScope.launch {
            songDao.getAllSongs().collect { allSongs ->
                val songMap = allSongs.associateBy { it.id }
                val current = _playerState.value
                val updatedCurrent = current.currentSong?.let { songMap[it.id] ?: it }
                val updatedQueue = current.queue.map { song -> songMap[song.id] ?: song }

                if (updatedCurrent != current.currentSong || updatedQueue != current.queue) {
                    _playerState.value = current.copy(
                        currentSong = updatedCurrent,
                        queue = updatedQueue
                    )
                }

                // Restore last played song if not loaded yet and settings allow
                if (!hasRestoredLastPlayed && allSongs.isNotEmpty()) {
                    hasRestoredLastPlayed = true
                    try {
                        val settings = userSettingsRepository.userSettingsFlow.first()
                        if (settings.resumePlaybackOnStart && settings.lastPlayedSongId != -1L) {
                            val targetSong = songMap[settings.lastPlayedSongId]
                            if (targetSong != null) {
                                val index = allSongs.indexOf(targetSong).coerceAtLeast(0)
                                val currentState = _playerState.value
                                _playerState.value = currentState.copy(
                                    currentSong = targetSong,
                                    isPlaying = true,
                                    currentPositionMs = settings.lastPlayedPositionMs,
                                    totalDurationMs = targetSong.duration,
                                    queue = allSongs,
                                    currentIndex = index
                                )
                                musicPlayerManager.playQueue(allSongs, index)
                                musicPlayerManager.seekTo(settings.lastPlayedPositionMs)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Periodically save last played state
        viewModelScope.launch {
            while (true) {
                delay(3000)
                val state = _playerState.value
                val song = state.currentSong
                if (song != null) {
                    userSettingsRepository.saveLastPlayedState(song.id, state.currentPositionMs)
                }
            }
        }

        // Observe real ExoPlayer playback state from MusicPlayerManager
        viewModelScope.launch {
            musicPlayerManager.playbackState.collect { playbackState ->
                val current = _playerState.value
                _playerState.value = current.copy(
                    isPlaying = playbackState.isPlaying,
                    currentPositionMs = playbackState.currentPositionMs,
                    totalDurationMs = if (playbackState.durationMs > 0) playbackState.durationMs else current.totalDurationMs
                )
                if (playbackState.isPlaying) {
                    userSettingsRepository.recordListeningTime(500L, notificationRepository)
                }
            }
        }

        // Observe queue index transitions from MusicPlayerManager (ExoPlayer auto-advance)
        viewModelScope.launch {
            musicPlayerManager.currentMediaIndex.collect { mediaIndex ->
                val state = _playerState.value
                if (state.queue.isNotEmpty() && mediaIndex in state.queue.indices && mediaIndex != state.currentIndex) {
                    val nextSong = state.queue[mediaIndex]
                    _playerState.value = state.copy(
                        currentIndex = mediaIndex,
                        currentSong = nextSong,
                        totalDurationMs = nextSong.duration,
                        currentPositionMs = 0L
                    )
                    songDao.incrementPlayCount(nextSong.id)
                }
            }
        }
    }

    fun playSong(song: SongEntity, playlist: List<SongEntity> = listOf(song)) {
        val file = File(song.path)
        if (song.path.isNotEmpty() && !file.exists()) {
            notificationRepository.addNotification(
                title = "Playback Issue",
                message = "Skipped missing file: ${song.title} (${song.artist})",
                category = NotificationCategory.PLAYBACK_ISSUES
            )
            val currentIndex = playlist.indexOf(song)
            if (playlist.size > 1 && currentIndex >= 0) {
                val nextIndex = (currentIndex + 1) % playlist.size
                playSong(playlist[nextIndex], playlist)
            }
            return
        }

        val index = playlist.indexOf(song).coerceAtLeast(0)
        val currentState = _playerState.value
        _playerState.value = currentState.copy(
            currentSong = song,
            isPlaying = true,
            currentPositionMs = 0L,
            totalDurationMs = song.duration,
            queue = playlist,
            currentIndex = index
        )

        musicPlayerManager.playQueue(playlist, index)

        viewModelScope.launch {
            songDao.incrementPlayCount(song.id)
        }
    }

    fun togglePlayPause() {
        musicPlayerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        musicPlayerManager.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun skipToNext() {
        musicPlayerManager.skipToNext()
    }

    fun skipToPrevious() {
        musicPlayerManager.skipToPrevious()
    }

    fun toggleShuffle() {
        val state = _playerState.value
        val newShuffle = !state.isShuffleEnabled
        val newQueue = if (newShuffle && state.queue.isNotEmpty()) {
            val current = state.currentSong
            val shuffled = state.queue.shuffled().toMutableList()
            if (current != null) {
                shuffled.remove(current)
                shuffled.add(0, current)
            }
            shuffled
        } else {
            state.queue
        }
        val newIndex = if (newQueue.isNotEmpty() && state.currentSong != null) {
            newQueue.indexOf(state.currentSong).coerceAtLeast(0)
        } else 0

        _playerState.value = state.copy(
            isShuffleEnabled = newShuffle,
            queue = newQueue,
            currentIndex = newIndex
        )
    }

    fun toggleRepeat() {
        val state = _playerState.value
        val nextMode = when (state.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playerState.value = state.copy(
            repeatMode = nextMode,
            isRepeatEnabled = nextMode != RepeatMode.OFF
        )
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.value = _playerState.value.copy(
                isSleepTimerActive = minutes == -1,
                sleepTimerRemainingMs = if (minutes == -1) -1L else 0L
            )
            return
        }

        val totalMs = minutes * 60 * 1000L
        _playerState.value = _playerState.value.copy(
            isSleepTimerActive = true,
            sleepTimerRemainingMs = totalMs
        )

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalMs
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _playerState.value = _playerState.value.copy(
                    sleepTimerRemainingMs = remaining.coerceAtLeast(0L)
                )
            }
            _playerState.value = _playerState.value.copy(
                isPlaying = false,
                isSleepTimerActive = false,
                sleepTimerRemainingMs = 0L
            )
            musicPlayerManager.togglePlayPause()
        }
    }

    fun toggleFavorite() {
        val song = _playerState.value.currentSong ?: return
        viewModelScope.launch {
            val updatedFavorite = !song.isFavorite
            songDao.setFavorite(song.id, updatedFavorite)
            val updatedSong = song.copy(isFavorite = updatedFavorite)
            val updatedQueue = _playerState.value.queue.map { item ->
                if (item.id == song.id) item.copy(isFavorite = updatedFavorite) else item
            }
            _playerState.value = _playerState.value.copy(
                currentSong = updatedSong,
                queue = updatedQueue
            )
        }
    }

    fun addToQueue(song: SongEntity) {
        val state = _playerState.value
        val currentQueue = state.queue.toMutableList()
        if (!currentQueue.contains(song)) {
            currentQueue.add(song)
            _playerState.value = state.copy(
                queue = currentQueue,
                currentSong = state.currentSong ?: song
            )
        }
    }
}
