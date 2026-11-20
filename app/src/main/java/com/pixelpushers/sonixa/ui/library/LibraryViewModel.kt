package com.pixelpushers.sonixa.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpushers.sonixa.data.local.dao.SongDao
import com.pixelpushers.sonixa.data.local.entity.SongEntity
import com.pixelpushers.sonixa.data.repository.AppNotification
import com.pixelpushers.sonixa.data.repository.NotificationRepository
import com.pixelpushers.sonixa.data.scanner.MediaStoreScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibrarySection {
    SONGS, ALBUMS, ARTISTS, FOLDERS
}

data class AlbumItem(
    val albumName: String,
    val artistName: String,
    val albumId: Long,
    val artworkUri: String?,
    val songCount: Int
)

data class ArtistItem(
    val artistName: String,
    val songCount: Int,
    val albumCount: Int
)

data class FolderItem(
    val folderPath: String,
    val folderName: String,
    val songCount: Int,
    val rawPath: String = folderPath
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songDao: SongDao,
    private val scanner: MediaStoreScanner,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val songs: StateFlow<List<SongEntity>> = songDao.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanProgress = scanner.scanProgress
    val notifications: StateFlow<List<AppNotification>> = notificationRepository.notifications

    fun scanMusic() {
        viewModelScope.launch {
            scanner.scanMusic()
        }
    }

    fun cancelScan() {
        scanner.cancelScan()
    }

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch {
            songDao.setFavorite(song.id, !song.isFavorite)
        }
    }

    fun clearNotifications() {
        notificationRepository.clearNotifications()
    }

    fun deleteSongs(songIds: List<Long>) {
        viewModelScope.launch {
            songDao.deleteSongsByIds(songIds)
        }
    }
}
