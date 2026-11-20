package com.pixelpushers.sonixa.util

import android.content.Context
import android.net.Uri
import com.pixelpushers.sonixa.data.local.dao.PlaylistDao
import com.pixelpushers.sonixa.data.local.entity.PlaylistEntity
import com.pixelpushers.sonixa.data.local.entity.PlaylistSongCrossRef
import com.pixelpushers.sonixa.data.preferences.AppTheme
import com.pixelpushers.sonixa.data.preferences.SongClickBehavior
import com.pixelpushers.sonixa.data.preferences.UserSettingsRepository
import com.pixelpushers.sonixa.data.repository.NotificationCategory
import com.pixelpushers.sonixa.data.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: UserSettingsRepository,
    private val playlistDao: PlaylistDao,
    private val notificationRepository: NotificationRepository
) {

    suspend fun exportBackup(destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepository.userSettingsFlow.first()
            val playlists = playlistDao.getAllPlaylists().first()

            val json = JSONObject().apply {
                put("version", 1)
                put("timestamp", System.currentTimeMillis())

                // Settings
                put("theme", settings.theme.name)
                put("matchAlbumColors", settings.matchAlbumColors)
                put("gaplessPlayback", settings.gaplessPlayback)
                put("crossfadeSeconds", settings.crossfadeSeconds)
                put("highResOutput", settings.highResOutput)
                put("showFormatBadges", settings.showFormatBadges)
                put("excludeHiddenFolders", settings.excludeHiddenFolders)
                put("filterShortTracks", settings.filterShortTracks)
                put("songClickBehavior", settings.songClickBehavior.name)
                put("defaultThumbnailIndex", settings.defaultThumbnailIndex)
                put("resumePlaybackOnStart", settings.resumePlaybackOnStart)
                put("pauseOnAudioDisconnect", settings.pauseOnAudioDisconnect)
                put("vibrationEnabled", settings.vibrationEnabled)
                put("materialDesignEnabled", settings.materialDesignEnabled)
                put("materialYouEnabled", settings.materialYouEnabled)

                // Folders & Pinned
                put("skippedFolderPaths", JSONArray(settings.skippedFolderPaths))
                put("includedFolderPaths", JSONArray(settings.includedFolderPaths))
                put("pinnedFolders", JSONArray(settings.pinnedFolders))
                put("pinnedArtists", JSONArray(settings.pinnedArtists))
                put("pinnedAlbums", JSONArray(settings.pinnedAlbums))

                // Playlists
                val playlistsArray = JSONArray()
                for (playlist in playlists) {
                    val songs = playlistDao.getSongsForPlaylist(playlist.id).first()
                    val playlistJson = JSONObject().apply {
                        put("name", playlist.name)
                        put("songIds", JSONArray(songs.map { it.id }))
                    }
                    playlistsArray.put(playlistJson)
                }
                put("playlists", playlistsArray)
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { os ->
                os.write(json.toString(2).toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreBackup(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(sourceUri)?.use { isStream ->
                isStream.bufferedReader().readText()
            } ?: return@withContext false

            val json = JSONObject(jsonString)

            // Restore Settings
            if (json.has("theme")) {
                try {
                    settingsRepository.setTheme(AppTheme.valueOf(json.getString("theme")))
                } catch (_: Exception) {}
            }
            if (json.has("matchAlbumColors")) settingsRepository.setMatchAlbumColors(json.getBoolean("matchAlbumColors"))
            if (json.has("gaplessPlayback")) settingsRepository.setGaplessPlayback(json.getBoolean("gaplessPlayback"))
            if (json.has("crossfadeSeconds")) settingsRepository.setCrossfadeSeconds(json.getInt("crossfadeSeconds"))
            if (json.has("highResOutput")) settingsRepository.setHighResOutput(json.getBoolean("highResOutput"))
            if (json.has("showFormatBadges")) settingsRepository.setShowFormatBadges(json.getBoolean("showFormatBadges"))
            if (json.has("excludeHiddenFolders")) settingsRepository.setExcludeHiddenFolders(json.getBoolean("excludeHiddenFolders"))
            if (json.has("filterShortTracks")) settingsRepository.setFilterShortTracks(json.getBoolean("filterShortTracks"))
            if (json.has("songClickBehavior")) {
                try {
                    settingsRepository.setSongClickBehavior(SongClickBehavior.valueOf(json.getString("songClickBehavior")))
                } catch (_: Exception) {}
            }
            if (json.has("defaultThumbnailIndex")) settingsRepository.setDefaultThumbnailIndex(json.getInt("defaultThumbnailIndex"))
            if (json.has("resumePlaybackOnStart")) settingsRepository.setResumePlaybackOnStart(json.getBoolean("resumePlaybackOnStart"))
            if (json.has("pauseOnAudioDisconnect")) settingsRepository.setPauseOnAudioDisconnect(json.getBoolean("pauseOnAudioDisconnect"))
            if (json.has("vibrationEnabled")) settingsRepository.setVibrationEnabled(json.getBoolean("vibrationEnabled"))
            if (json.has("materialDesignEnabled")) settingsRepository.setMaterialDesignEnabled(json.getBoolean("materialDesignEnabled"))
            if (json.has("materialYouEnabled")) settingsRepository.setMaterialYouEnabled(json.getBoolean("materialYouEnabled"))

            // Restore Skipped/Included/Pinned
            if (json.has("skippedFolderPaths")) {
                val array = json.getJSONArray("skippedFolderPaths")
                for (i in 0 until array.length()) {
                    settingsRepository.addSkippedFolder(array.getString(i))
                }
            }
            if (json.has("includedFolderPaths")) {
                val array = json.getJSONArray("includedFolderPaths")
                for (i in 0 until array.length()) {
                    settingsRepository.addIncludedFolder(array.getString(i))
                }
            }
            if (json.has("pinnedFolders")) {
                val array = json.getJSONArray("pinnedFolders")
                for (i in 0 until array.length()) {
                    settingsRepository.togglePinFolder(array.getString(i))
                }
            }
            if (json.has("pinnedArtists")) {
                val array = json.getJSONArray("pinnedArtists")
                for (i in 0 until array.length()) {
                    settingsRepository.togglePinArtist(array.getString(i))
                }
            }
            if (json.has("pinnedAlbums")) {
                val array = json.getJSONArray("pinnedAlbums")
                for (i in 0 until array.length()) {
                    settingsRepository.togglePinAlbum(array.getString(i))
                }
            }

            // Restore Playlists
            if (json.has("playlists")) {
                val playlistsArray = json.getJSONArray("playlists")
                for (i in 0 until playlistsArray.length()) {
                    val playlistObj = playlistsArray.getJSONObject(i)
                    val name = playlistObj.optString("name", "Restored Playlist")
                    playlistDao.insertPlaylist(PlaylistEntity(name = name))

                    val allPlaylists = playlistDao.getAllPlaylists().first()
                    val targetPlaylist = allPlaylists.find { it.name == name }
                    if (targetPlaylist != null) {
                        val songIdsArray = playlistObj.optJSONArray("songIds")
                        if (songIdsArray != null) {
                            for (j in 0 until songIdsArray.length()) {
                                val songId = songIdsArray.getLong(j)
                                playlistDao.addSongToPlaylist(
                                    PlaylistSongCrossRef(playlistId = targetPlaylist.id, songId = songId)
                                )
                            }
                        }
                    }
                }
            }

            notificationRepository.addNotification(
                title = "Backup Restored Successfully",
                message = "All settings, playlists, and pinned items have been restored.",
                category = NotificationCategory.SYSTEM
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
