package com.pixelpushers.sonixa.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pixelpushers.sonixa.R
import com.pixelpushers.sonixa.data.local.entity.PlaylistEntity
import com.pixelpushers.sonixa.data.local.entity.SongEntity
import com.pixelpushers.sonixa.data.repository.AppNotification
import com.pixelpushers.sonixa.data.repository.NotificationCategory
import com.pixelpushers.sonixa.ui.components.formatFolderNameAndPath
import com.pixelpushers.sonixa.ui.playlists.AddToPlaylistSheet
import com.pixelpushers.sonixa.ui.theme.SonixaTheme
import java.io.File

enum class LibraryViewType {
    LIST, GRID
}

@Composable
fun LibraryScreen(
    songs: List<SongEntity>,
    currentPlayingSongId: Long?,
    notifications: List<AppNotification>,
    playlists: List<PlaylistEntity> = emptyList(),
    onSongClick: (SongEntity) -> Unit,
    onPlayAll: (List<SongEntity>) -> Unit,
    onShuffleAll: (List<SongEntity>) -> Unit,
    onClearNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    onBulkDeleteSongs: (List<SongEntity>) -> Unit = {},
    onBulkAddToPlaylist: (playlistId: Long, songIds: List<Long>) -> Unit = { _, _ -> },
    onCreatePlaylist: (String) -> Unit = {},
    isNowPlayingPillPresent: Boolean = false,
    onRescanMusic: () -> Unit = {},
    onChooseFolders: () -> Unit = {},
    pinnedAlbums: Set<String> = emptySet(),
    pinnedArtists: Set<String> = emptySet(),
    pinnedFolders: Set<String> = emptySet(),
    onTogglePinAlbum: (String) -> Unit = {},
    onTogglePinArtist: (String) -> Unit = {},
    onTogglePinFolder: (String) -> Unit = {},
) {
    var selectedSection by remember { mutableStateOf(LibrarySection.SONGS) }
    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    if (selectedAlbumName != null) {
        BackHandler { selectedAlbumName = null }
    }
    if (selectedArtistName != null) {
        BackHandler { selectedArtistName = null }
    }
    if (selectedFolderPath != null) {
        BackHandler { selectedFolderPath = null }
    }

    var libraryViewType by remember { mutableStateOf(LibraryViewType.LIST) }
    var isBulkSelectActive by remember { mutableStateOf(false) }
    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }

    var showBulkPlaylistSheet by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    val toggleSelectSong: (Long) -> Unit = { id ->
        selectedSongIds = if (selectedSongIds.contains(id)) {
            selectedSongIds - id
        } else {
            selectedSongIds + id
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Music Library",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                NotificationBell(
                    unreadCount = notifications.size,
                    onClick = { showNotificationsSheet = true }
                )
            }

            // Section Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibrarySection.entries.forEach { section ->
                    val isSelected = selectedSection == section
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSection = section
                            selectedAlbumName = null
                            if (section != LibrarySection.SONGS) {
                                isBulkSelectActive = false
                                selectedSongIds = emptySet()
                            }
                        },
                        label = { Text(section.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control Bar beneath the pills (View Type on Left, Count in Middle, Bulk Select on Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Type Options (List View vs Grid View) - LEFT
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (libraryViewType == LibraryViewType.LIST) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { libraryViewType = LibraryViewType.LIST },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "List View",
                            tint = if (libraryViewType == LibraryViewType.LIST) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (libraryViewType == LibraryViewType.GRID) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { libraryViewType = LibraryViewType.GRID },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid View",
                            tint = if (libraryViewType == LibraryViewType.GRID) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Middle: Count / Number
                val itemCount = remember(selectedSection, songs) {
                    when (selectedSection) {
                        LibrarySection.SONGS -> songs.size
                        LibrarySection.ALBUMS -> songs.groupBy { it.album }.size
                        LibrarySection.ARTISTS -> songs.groupBy { it.artist }.size
                        LibrarySection.FOLDERS -> songs.groupBy { it.folderPath }.size
                    }
                }
                val itemLabel = remember(selectedSection, itemCount) {
                    when (selectedSection) {
                        LibrarySection.SONGS -> if (itemCount == 1) "1 song" else "$itemCount songs"
                        LibrarySection.ALBUMS -> if (itemCount == 1) "1 album" else "$itemCount albums"
                        LibrarySection.ARTISTS -> if (itemCount == 1) "1 artist" else "$itemCount artists"
                        LibrarySection.FOLDERS -> if (itemCount == 1) "1 folder" else "$itemCount folders"
                    }
                }

                Text(
                    text = itemLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Bulk Select / Select All Trigger - RIGHT (Songs section only)
                if (selectedSection == LibrarySection.SONGS) {
                    val allSelected = songs.isNotEmpty() && selectedSongIds.size == songs.size
                    val buttonText = if (!isBulkSelectActive) "Bulk Select" else if (allSelected) "Deselect All" else "Select All"

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isBulkSelectActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                if (!isBulkSelectActive) {
                                    isBulkSelectActive = true
                                } else {
                                    if (allSelected) {
                                        selectedSongIds = emptySet()
                                    } else {
                                        selectedSongIds = songs.map { it.id }.toSet()
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isBulkSelectActive) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistAddCheck,
                            contentDescription = buttonText,
                            tint = if (isBulkSelectActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = buttonText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBulkSelectActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(100.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content by Section & View Type
            when (selectedSection) {
                LibrarySection.SONGS -> {
                    if (libraryViewType == LibraryViewType.LIST) {
                        SongsList(
                            songs = songs,
                            currentPlayingSongId = currentPlayingSongId,
                            isSelectable = isBulkSelectActive,
                            selectedSongIds = selectedSongIds,
                            onSelectToggle = toggleSelectSong,
                            onSongClick = onSongClick,
                            onRescanMusic = onRescanMusic,
                            onChooseFolders = onChooseFolders
                        )
                    } else {
                        SongsGrid(
                            songs = songs,
                            currentPlayingSongId = currentPlayingSongId,
                            isSelectable = isBulkSelectActive,
                            selectedSongIds = selectedSongIds,
                            onSelectToggle = toggleSelectSong,
                            onSongClick = onSongClick,
                            onRescanMusic = onRescanMusic,
                            onChooseFolders = onChooseFolders
                        )
                    }
                }

                LibrarySection.ALBUMS -> {
                    if (libraryViewType == LibraryViewType.GRID) {
                        AlbumsGrid(
                            songs = songs,
                            pinnedAlbums = pinnedAlbums,
                            onTogglePin = onTogglePinAlbum,
                            onAlbumClick = { albumName -> selectedAlbumName = albumName }
                        )
                    } else {
                        AlbumsList(
                            songs = songs,
                            pinnedAlbums = pinnedAlbums,
                            onTogglePin = onTogglePinAlbum,
                            onAlbumClick = { albumName -> selectedAlbumName = albumName }
                        )
                    }
                }

                LibrarySection.ARTISTS -> {
                    if (libraryViewType == LibraryViewType.LIST) {
                        ArtistsList(
                            songs = songs,
                            pinnedArtists = pinnedArtists,
                            onTogglePin = onTogglePinArtist,
                            onArtistClick = { selectedArtistName = it }
                        )
                    } else {
                        ArtistsGrid(
                            songs = songs,
                            pinnedArtists = pinnedArtists,
                            onTogglePin = onTogglePinArtist,
                            onArtistClick = { selectedArtistName = it }
                        )
                    }
                }

                LibrarySection.FOLDERS -> {
                    if (libraryViewType == LibraryViewType.LIST) {
                        FoldersList(
                            songs = songs,
                            pinnedFolders = pinnedFolders,
                            onTogglePin = onTogglePinFolder,
                            onFolderClick = { selectedFolderPath = it }
                        )
                    } else {
                        FoldersGrid(
                            songs = songs,
                            pinnedFolders = pinnedFolders,
                            onTogglePin = onTogglePinFolder,
                            onFolderClick = { selectedFolderPath = it }
                        )
                    }
                }
            }
        }

        val hasNowPlayingPill = isNowPlayingPillPresent || currentPlayingSongId != null
        val bulkActionBottomPadding = if (hasNowPlayingPill) 168.dp else 90.dp

        // Floating Bulk Options Side/Bottom Footer
        AnimatedVisibility(
            visible = isBulkSelectActive && selectedSection == LibrarySection.SONGS,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bulkActionBottomPadding, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedSongIds.size} Selected",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedSongIds.isEmpty()) "Tap songs to select" else "Choose action",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (selectedSongIds.isNotEmpty()) {
                                    showBulkPlaylistSheet = true
                                }
                            },
                            enabled = selectedSongIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "Add to playlist",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add to Playlist", fontSize = 13.sp, color = Color.Black)
                        }

                        IconButton(
                            onClick = {
                                if (selectedSongIds.isNotEmpty()) {
                                    showBulkDeleteDialog = true
                                }
                            },
                            enabled = selectedSongIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = if (selectedSongIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                isBulkSelectActive = false
                                selectedSongIds = emptySet()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel bulk select",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Album Detail View overlay
        selectedAlbumName?.let { albumName ->
            val albumSongs = remember(albumName, songs) { songs.filter { it.album == albumName } }
            val firstSong = albumSongs.firstOrNull()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable { }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "← Back",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { selectedAlbumName = null }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = firstSong?.artworkUri,
                            contentDescription = albumName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = albumName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = firstSong?.artist ?: "Unknown Artist",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${albumSongs.size} songs",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onPlayAll(albumSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Play", color = Color.Black)
                        }

                        Button(
                            onClick = { onShuffleAll(albumSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text("Shuffle", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 140.dp)
                    ) {
                        items(albumSongs) { song ->
                            SongRowItem(
                                song = song,
                                isPlaying = song.id == currentPlayingSongId,
                                onClick = { onSongClick(song) }
                            )
                        }
                    }
                }
            }
        }

        // Artist Detail View overlay
        selectedArtistName?.let { artistName ->
            val artistSongs = remember(artistName, songs) { songs.filter { it.artist == artistName } }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable { }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "← Back",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { selectedArtistName = null }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = artistName,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = artistName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${artistSongs.size} songs",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onPlayAll(artistSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Play", color = Color.Black)
                        }

                        Button(
                            onClick = { onShuffleAll(artistSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text("Shuffle", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 140.dp)
                    ) {
                        items(artistSongs) { song ->
                            SongRowItem(
                                song = song,
                                isPlaying = song.id == currentPlayingSongId,
                                onClick = { onSongClick(song) }
                            )
                        }
                    }
                }
            }
        }

        // Folder Detail View overlay
        selectedFolderPath?.let { folderPath ->
            val folderSongs = remember(folderPath, songs) { songs.filter { it.folderPath == folderPath } }
            val (folderName, displayPath) = remember(folderPath) { formatFolderNameAndPath(folderPath) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable { }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "← Back",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { selectedFolderPath = null }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = folderName,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = folderName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = displayPath,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${folderSongs.size} songs",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onPlayAll(folderSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Play", color = Color.Black)
                        }

                        Button(
                            onClick = { onShuffleAll(folderSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text("Shuffle", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 140.dp)
                    ) {
                        items(folderSongs) { song ->
                            SongRowItem(
                                song = song,
                                isPlaying = song.id == currentPlayingSongId,
                                onClick = { onSongClick(song) }
                            )
                        }
                    }
                }
            }
        }

        // Bulk Add to Playlist Sheet
        if (showBulkPlaylistSheet) {
            AddToPlaylistSheet(
                playlists = playlists,
                onTogglePlaylist = { playlistId, isChecked ->
                    if (isChecked) {
                        onBulkAddToPlaylist(playlistId, selectedSongIds.toList())
                    }
                },
                onCreateNewPlaylist = { name -> onCreatePlaylist(name) },
                onDismiss = {
                    showBulkPlaylistSheet = false
                    isBulkSelectActive = false
                    selectedSongIds = emptySet()
                }
            )
        }

        // Bulk Delete Dialog
        if (showBulkDeleteDialog) {
            val selectedSongsList = remember(selectedSongIds, songs) {
                songs.filter { selectedSongIds.contains(it.id) }
            }
            DeleteBulkSongsDialog(
                count = selectedSongIds.size,
                onConfirm = {
                    onBulkDeleteSongs(selectedSongsList)
                    showBulkDeleteDialog = false
                    isBulkSelectActive = false
                    selectedSongIds = emptySet()
                },
                onDismiss = { showBulkDeleteDialog = false }
            )
        }

        // Notifications Sheet
        if (showNotificationsSheet) {
            NotificationsSheet(
                notifications = notifications,
                onClear = onClearNotifications,
                onDismiss = { showNotificationsSheet = false }
            )
        }
    }
}

@Composable
fun SongsList(
    songs: List<SongEntity>,
    currentPlayingSongId: Long?,
    isSelectable: Boolean = false,
    selectedSongIds: Set<Long> = emptySet(),
    onSelectToggle: (Long) -> Unit = {},
    onSongClick: (SongEntity) -> Unit,
    onRescanMusic: () -> Unit = {},
    onChooseFolders: () -> Unit = {}
) {
    if (songs.isEmpty()) {
        EmptyLibraryState(
            onRescanMusic = onRescanMusic,
            onChooseFolders = onChooseFolders
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 160.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(songs, key = { it.id }) { song ->
                SongRowItem(
                    song = song,
                    isPlaying = song.id == currentPlayingSongId,
                    isSelectable = isSelectable,
                    isSelected = selectedSongIds.contains(song.id),
                    onSelectToggle = { onSelectToggle(song.id) },
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
fun SongsGrid(
    songs: List<SongEntity>,
    currentPlayingSongId: Long?,
    isSelectable: Boolean,
    selectedSongIds: Set<Long>,
    onSelectToggle: (Long) -> Unit,
    onSongClick: (SongEntity) -> Unit,
    onRescanMusic: () -> Unit = {},
    onChooseFolders: () -> Unit = {}
) {
    if (songs.isEmpty()) {
        EmptyLibraryState(
            onRescanMusic = onRescanMusic,
            onChooseFolders = onChooseFolders
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(songs, key = { it.id }) { song ->
                val isSelected = selectedSongIds.contains(song.id)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable {
                            if (isSelectable) onSelectToggle(song.id)
                            else onSongClick(song)
                        }
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = song.artworkUri,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )

                            if (isSelectable) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onSelectToggle(song.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = song.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (song.id == currentPlayingSongId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = song.artist,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (song.isFlac) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "FLAC",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            val bitDepthText = "${if (song.bitDepth > 0) song.bitDepth else 16}bit"
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = bitDepthText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = formatDuration(song.duration),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryState(
    onRescanMusic: () -> Unit,
    onChooseFolders: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Equalizer & Music notes graphic illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Floating notes
                Text(
                    text = "♪",
                    color = Color(0xFFF06CA6),
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.TopStart).offset(x = 60.dp, y = 10.dp)
                )
                Text(
                    text = "♫",
                    color = Color(0xFF4CD19C),
                    fontSize = 22.sp,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-70).dp, y = 20.dp)
                )
                Text(
                    text = "♪",
                    color = Color(0xFFF6C445),
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-40).dp, y = 50.dp)
                )
                Text(
                    text = "♫",
                    color = Color(0xFF6380F9),
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.TopStart).offset(x = 40.dp, y = 40.dp)
                )

                // Equalizer Bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(120.dp)
                ) {
                    val barColors = listOf(
                        Color(0xFFF2783F), // Orange
                        Color(0xFFF6C445), // Yellow
                        Color(0xFF4CD19C), // Mint Green
                        Color(0xFF6380F9), // Blue/Purple
                        Color(0xFFB174F5), // Light Purple
                        Color(0xFFF06CA6), // Pink
                        Color(0xFF42CDC9)  // Cyan
                    )
                    val barHeights = listOf(35.dp, 65.dp, 95.dp, 120.dp, 80.dp, 50.dp, 30.dp)

                    barHeights.forEachIndexed { index, height ->
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(height)
                                .clip(RoundedCornerShape(8.dp))
                                .background(barColors[index])
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Let's fill this place with music!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Add songs to your phone or point us to your music folders, and we'll do the rest.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRescanMusic,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Scan my music",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onChooseFolders,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Choose folders",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Format badges at the bottom
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("FLAC", "MP3", "AAC", "WAV").forEach { format ->
                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = format,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SongRowItem(
    song: SongEntity,
    isPlaying: Boolean,
    isSelectable: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectable) onSelectToggle()
                else onClick()
            }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectable) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        AsyncImage(
            model = song.artworkUri,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Standardized Right Aligned Audio Quality Pills + Duration
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            val bitDepthText = "${if (song.bitDepth > 0) song.bitDepth else 16}bit"

            if (song.isFlac) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "FLAC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = bitDepthText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatDuration(song.duration),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 36.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun AlbumsList(
    songs: List<SongEntity>,
    pinnedAlbums: Set<String> = emptySet(),
    onTogglePin: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit
) {
    val albums = remember(songs, pinnedAlbums) {
        songs.groupBy { it.album }
            .map { (albumName, list) ->
                AlbumItem(
                    albumName = albumName,
                    artistName = list.firstOrNull()?.artist ?: "Unknown Artist",
                    albumId = list.firstOrNull()?.albumId ?: 0L,
                    artworkUri = list.firstOrNull()?.artworkUri,
                    songCount = list.size
                )
            }
            .sortedWith(
                compareByDescending<AlbumItem> { pinnedAlbums.contains(it.albumName) }
                    .thenBy { it.albumName }
            )
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 160.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums) { album ->
            val isPinned = pinnedAlbums.contains(album.albumName)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlbumClick(album.albumName) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = album.artworkUri,
                    contentDescription = album.albumName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = album.albumName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "${album.artistName} • ${album.songCount} songs",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { onTogglePin(album.albumName) }) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin Album",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumsGrid(
    songs: List<SongEntity>,
    pinnedAlbums: Set<String> = emptySet(),
    onTogglePin: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit
) {
    val albums = remember(songs, pinnedAlbums) {
        songs.groupBy { it.album }
            .map { (albumName, list) ->
                AlbumItem(
                    albumName = albumName,
                    artistName = list.firstOrNull()?.artist ?: "Unknown Artist",
                    albumId = list.firstOrNull()?.albumId ?: 0L,
                    artworkUri = list.firstOrNull()?.artworkUri,
                    songCount = list.size
                )
            }
            .sortedWith(
                compareByDescending<AlbumItem> { pinnedAlbums.contains(it.albumName) }
                    .thenBy { it.albumName }
            )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums) { album ->
            val isPinned = pinnedAlbums.contains(album.albumName)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlbumClick(album.albumName) }
            ) {
                Box {
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = album.albumName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    IconButton(
                        onClick = { onTogglePin(album.albumName) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Album",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = album.albumName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = album.artistName,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ArtistsList(
    songs: List<SongEntity>,
    pinnedArtists: Set<String> = emptySet(),
    onTogglePin: (String) -> Unit = {},
    onArtistClick: (String) -> Unit
) {
    val artists = remember(songs, pinnedArtists) {
        songs.groupBy { it.artist }
            .map { (artistName, list) ->
                ArtistItem(
                    artistName = artistName,
                    songCount = list.size,
                    albumCount = list.map { it.album }.distinct().size
                )
            }
            .sortedWith(
                compareByDescending<ArtistItem> { pinnedArtists.contains(it.artistName) }
                    .thenBy { it.artistName }
            )
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 140.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists) { artist ->
            val isPinned = pinnedArtists.contains(artist.artistName)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist.artistName) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = artist.artistName,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = artist.artistName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "${artist.songCount} songs • ${artist.albumCount} albums",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { onTogglePin(artist.artistName) }) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin Artist",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistsGrid(
    songs: List<SongEntity>,
    pinnedArtists: Set<String> = emptySet(),
    onTogglePin: (String) -> Unit = {},
    onArtistClick: (String) -> Unit
) {
    val artists = remember(songs, pinnedArtists) {
        songs.groupBy { it.artist }
            .map { (artistName, list) ->
                ArtistItem(
                    artistName = artistName,
                    songCount = list.size,
                    albumCount = list.map { it.album }.distinct().size
                )
            }
            .sortedWith(
                compareByDescending<ArtistItem> { pinnedArtists.contains(it.artistName) }
                    .thenBy { it.artistName }
            )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists) { artist ->
            val isPinned = pinnedArtists.contains(artist.artistName)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { onArtistClick(artist.artistName) }
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = artist.artistName,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = artist.artistName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = "${artist.songCount} songs • ${artist.albumCount} albums",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onTogglePin(artist.artistName) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin Artist",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FoldersList(
    songs: List<SongEntity>,
    pinnedFolders: Set<String> = emptySet(),
    onTogglePin: (String) -> Unit = {},
    onFolderClick: (String) -> Unit
) {
    val folders = remember(songs, pinnedFolders) {
        songs.groupBy { it.folderPath }
            .map { (path, list) ->
                val (name, displayPath) = formatFolderNameAndPath(path)
                FolderItem(
                    folderPath = displayPath,
                    folderName = name,
                    songCount = list.size,
                    rawPath = path
                )
            }
            .sortedWith(
                compareByDescending<FolderItem> { pinnedFolders.contains(it.rawPath) }
                    .thenBy { it.folderName }
            )
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 140.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders) { folder ->
            val isPinned = pinnedFolders.contains(folder.rawPath)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFolderClick(folder.rawPath) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = folder.folderName,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = folder.folderName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        if (isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "${folder.songCount} songs • ${folder.folderPath}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                IconButton(onClick = { onTogglePin(folder.rawPath) }) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin Folder",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FoldersGrid(
    songs: List<SongEntity>,
    pinnedFolders: Set<String> = emptySet(),
    onTogglePin: (String) -> Unit = {},
    onFolderClick: (String) -> Unit
) {
    val folders = remember(songs, pinnedFolders) {
        songs.groupBy { it.folderPath }
            .map { (path, list) ->
                val (name, displayPath) = formatFolderNameAndPath(path)
                FolderItem(
                    folderPath = displayPath,
                    folderName = name,
                    songCount = list.size,
                    rawPath = path
                )
            }
            .sortedWith(
                compareByDescending<FolderItem> { pinnedFolders.contains(it.rawPath) }
                    .thenBy { it.folderName }
            )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders) { folder ->
            val isPinned = pinnedFolders.contains(folder.rawPath)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { onFolderClick(folder.rawPath) }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = folder.folderName,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = folder.folderName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        if (isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = "${folder.songCount} songs",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onTogglePin(folder.rawPath) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin Folder",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationsSheet(
    notifications: List<AppNotification>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedCategory by remember { mutableStateOf<NotificationCategory?>(null) }

    val filteredNotifications = remember(notifications, selectedCategory) {
        if (selectedCategory == null) notifications
        else notifications.filter { it.category == selectedCategory }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Clear All", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                NotificationCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredNotifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = R.drawable.placeholder,
                        contentDescription = "No notifications",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You're all caught up!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "No notifications in this category.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { notif ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = notif.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = notif.category.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notif.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DeleteBulkSongsDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Songs",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete $count selected songs from your library?",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun NotificationBell(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibilityLabel = when (unreadCount) {
        0 -> "Notifications, no unread"
        1 -> "Notifications, 1 unread"
        else -> "Notifications, $unreadCount unread"
    }

    val bellScale = remember { Animatable(1f) }

    LaunchedEffect(unreadCount) {
        if (unreadCount > 0) {
            repeat(3) {
                bellScale.animateTo(
                    targetValue = 1.15f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
                bellScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            }
        } else {
            bellScale.snapTo(1f)
        }
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                onClick = onClick,
                onClickLabel = accessibilityLabel
            )
            .semantics { contentDescription = accessibilityLabel },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.scale(bellScale.value),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            if (unreadCount == 1) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
            } else if (unreadCount > 1) {
                val badgeText = if (unreadCount > 99) "99+" else unreadCount.toString()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .padding(horizontal = if (badgeText.length > 1) 4.dp else 2.dp, vertical = 1.dp)
                        .sizeIn(minWidth = 14.dp, minHeight = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 9.sp
                    )
                }
            }
        }
    }
}

@Preview(name = "Notification Bell - No Alerts", showBackground = true)
@Composable
fun NotificationBellNoAlertsPreview() {
    SonixaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NotificationBell(unreadCount = 0, onClick = {})
        }
    }
}

@Preview(name = "Notification Bell - Unread Single", showBackground = true)
@Composable
fun NotificationBellUnreadPreview() {
    SonixaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NotificationBell(unreadCount = 1, onClick = {})
        }
    }
}

@Preview(name = "Notification Bell - Multiple Unread", showBackground = true)
@Composable
fun NotificationBellMultiplePreview() {
    SonixaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NotificationBell(unreadCount = 3, onClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LibraryScreenPreview() {
    SonixaTheme {
        LibraryScreen(
            songs = listOf(
                SongEntity(
                    id = 1L,
                    mediaStoreId = 101L,
                    title = "Symphony No.6 (1st movement)",
                    artist = "Ludwig van Beethoven",
                    album = "Classical Masterpieces",
                    albumId = 201L,
                    duration = 728000L,
                    path = "Music/Beethoven.flac",
                    uri = "content://media/external/audio/media/101",
                    artworkUri = null,
                    mimeType = "audio/flac",
                    isFlac = true,
                    bitDepth = 24,
                    folderPath = "Music"
                ),
                SongEntity(
                    id = 2L,
                    mediaStoreId = 102L,
                    title = "sample1",
                    artist = "<unknown>",
                    album = "Unknown Album",
                    albumId = 202L,
                    duration = 122000L,
                    path = "Music/sample1.flac",
                    uri = "content://media/external/audio/media/102",
                    artworkUri = null,
                    mimeType = "audio/flac",
                    isFlac = true,
                    bitDepth = 16,
                    folderPath = "Music"
                )
            ),
            currentPlayingSongId = 1L,
            notifications = emptyList(),
            onSongClick = {},
            onPlayAll = {},
            onShuffleAll = {},
            onClearNotifications = {}
        )
    }
}
