package com.pixelpushers.sonixa.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.pixelpushers.sonixa.data.local.dao.SongDao
import com.pixelpushers.sonixa.data.local.entity.SongEntity
import com.pixelpushers.sonixa.data.repository.NotificationCategory
import com.pixelpushers.sonixa.data.repository.NotificationRepository
import com.pixelpushers.sonixa.util.LocalLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ScanProgress(
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val currentFolder: String = ""
)

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val localLogger: LocalLogger,
    private val notificationRepository: NotificationRepository
) {
    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    fun cancelScan() {
        _scanProgress.value = ScanProgress(isScanning = false, scannedCount = 0, currentFolder = "")
    }

    suspend fun scanMusic(
        minDurationMs: Long = 30000L,
        excludeHiddenFolders: Boolean = true,
        skippedFolderPaths: Set<String> = emptySet(),
        includedFolderPaths: Set<String> = emptySet()
    ) = withContext(Dispatchers.IO) {
        _scanProgress.value = ScanProgress(isScanning = true, scannedCount = 0, currentFolder = "")

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.YEAR
        )

        val selection = """
            (${MediaStore.Audio.Media.IS_MUSIC} != 0 
             OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE '%audio%' 
             OR ${MediaStore.Audio.Media.DATA} LIKE '%.m4a'
             OR ${MediaStore.Audio.Media.DATA} LIKE '%.mp4'
             OR ${MediaStore.Audio.Media.DATA} LIKE '%.flac'
             OR ${MediaStore.Audio.Media.DATA} LIKE '%.mp3'
             OR ${MediaStore.Audio.Media.DATA} LIKE '%.wav'
             OR ${MediaStore.Audio.Media.DATA} LIKE '%.aac'
             OR ${MediaStore.Audio.Media.DATA} LIKE '%.ogg')
            AND ${MediaStore.Audio.Media.DURATION} >= ?
        """.trimIndent()
        val selectionArgs = arrayOf(minDurationMs.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val scannedSongs = mutableListOf<SongEntity>()
        val scannedIds = mutableListOf<Long>()

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                val retriever = MediaMetadataRetriever()

                while (cursor.moveToNext()) {
                    if (!_scanProgress.value.isScanning) {
                        break
                    }
                    val mediaStoreId = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Track"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    val duration = cursor.getLong(durationColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "audio/*"
                    val year = cursor.getInt(yearColumn)

                    val file = File(path)
                    val folderPath = file.parent ?: ""

                    // Check hidden folder exclusion
                    if (excludeHiddenFolders) {
                        val isHidden = folderPath.split(File.separatorChar).any { it.startsWith(".") }
                        if (isHidden) continue
                    }

                    // Check explicit skipped folder paths
                    if (skippedFolderPaths.any { folderPath.startsWith(it, ignoreCase = true) }) {
                        continue
                    }

                    // Check explicit included folder paths if specified
                    if (includedFolderPaths.isNotEmpty()) {
                        if (!includedFolderPaths.any { folderPath.startsWith(it, ignoreCase = true) }) {
                            continue
                        }
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaStoreId
                    ).toString()

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    val isFlac = mimeType.contains("flac", ignoreCase = true) || path.endsWith(".flac", ignoreCase = true)
                    var sampleRate = 44100
                    var bitDepth = 16
                    var bitRate = 0

                    if (isFlac && path.isNotEmpty() && file.exists()) {
                        try {
                            retriever.setDataSource(path)
                            val sr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                            val br = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                            if (sr != null) sampleRate = sr.toIntOrNull() ?: 44100
                            if (br != null) bitRate = (br.toIntOrNull() ?: 0) / 1000

                            if (sampleRate >= 88200 || bitRate > 1000) {
                                bitDepth = 24
                            }
                        } catch (_: Exception) {
                            // Fallback defaults
                        }
                    }

                    val song = SongEntity(
                        id = mediaStoreId,
                        mediaStoreId = mediaStoreId,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        duration = duration,
                        path = path,
                        uri = contentUri,
                        artworkUri = albumArtUri,
                        mimeType = mimeType,
                        isFlac = isFlac,
                        sampleRate = sampleRate,
                        bitDepth = bitDepth,
                        bitRate = bitRate,
                        folderPath = folderPath,
                        year = year
                    )

                    scannedSongs.add(song)
                    scannedIds.add(mediaStoreId)

                    _scanProgress.value = ScanProgress(
                        isScanning = true,
                        scannedCount = scannedSongs.size,
                        currentFolder = folderPath
                    )
                }
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }

            songDao.insertSongs(scannedSongs)
            if (scannedIds.isNotEmpty()) {
                songDao.deleteOrphanedSongs(scannedIds)
            }
        } catch (e: Exception) {
            if (_scanProgress.value.isScanning) {
                localLogger.log("MediaStoreScanner", "Scan failed: ${e.message}")
                notificationRepository.addNotification(
                    title = "⚠️ Library Scan Notice",
                    message = "Initial music scan encountered an issue. You can rescan or send a diagnostic report to the developer via Settings / Local Logs.",
                    category = NotificationCategory.PLAYBACK_ISSUES
                )
            }
        } finally {
            _scanProgress.value = ScanProgress(
                isScanning = false,
                scannedCount = scannedSongs.size,
                currentFolder = ""
            )
        }
    }
}
