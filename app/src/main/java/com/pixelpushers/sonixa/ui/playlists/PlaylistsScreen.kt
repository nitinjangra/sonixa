package com.pixelpushers.sonixa.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelpushers.sonixa.data.local.entity.PlaylistEntity
import com.pixelpushers.sonixa.data.local.entity.SongEntity
import com.pixelpushers.sonixa.ui.components.DefaultAlbumArtPlaceholder
import com.pixelpushers.sonixa.ui.library.LibraryViewType
import com.pixelpushers.sonixa.ui.library.SongRowItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistEntity>,
    favorites: List<SongEntity>,
    recentlyPlayed: List<SongEntity>,
    mostPlayed: List<SongEntity>,
    allSongs: List<SongEntity> = emptyList(),
    currentPlayingSongId: Long? = null,
    onCreatePlaylist: (String) -> Unit,
    onAddSongToPlaylist: (playlistId: Long, songId: Long) -> Unit = { _, _ -> },
    onDeletePlaylist: (playlistId: Long) -> Unit = {},
    getSongsForPlaylist: (playlistId: Long) -> Flow<List<SongEntity>> = { emptyFlow() },
    getSongCountForPlaylist: (playlistId: Long) -> Flow<Int> = { emptyFlow() },
    onSongClick: (SongEntity, List<SongEntity>) -> Unit = { _, _ -> },
    onPlayAll: (List<SongEntity>) -> Unit = {},
    onShuffleAll: (List<SongEntity>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var viewType by remember { mutableStateOf(LibraryViewType.LIST) }

    var selectedPlaylistEntity by remember { mutableStateOf<PlaylistEntity?>(null) }
    var selectedAutoPlaylistTitle by remember { mutableStateOf<String?>(null) }
    var selectedAutoPlaylistSongs by remember { mutableStateOf<List<SongEntity>>(emptyList()) }

    val customPlaylistSongs = selectedPlaylistEntity?.let { playlist ->
        remember(playlist.id) { getSongsForPlaylist(playlist.id) }.collectAsState(initial = emptyList()).value
    } ?: emptyList()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlists",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // View Selector (List View vs Grid View) - LEFT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            .background(if (viewType == LibraryViewType.LIST) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewType = LibraryViewType.LIST },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "List View",
                            tint = if (viewType == LibraryViewType.LIST) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (viewType == LibraryViewType.GRID) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewType = LibraryViewType.GRID },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid View",
                            tint = if (viewType == LibraryViewType.GRID) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (viewType == LibraryViewType.LIST) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Auto Playlists
                    item {
                        Text(
                            text = "AUTO PLAYLISTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    item {
                        AutoPlaylistRow(
                            title = "Favorites",
                            songCount = favorites.size,
                            icon = Icons.Default.Favorite,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                selectedAutoPlaylistTitle = "Favorites"
                                selectedAutoPlaylistSongs = favorites
                            }
                        )
                    }

                    item {
                        AutoPlaylistRow(
                            title = "Recently Played",
                            songCount = recentlyPlayed.size,
                            icon = Icons.Default.History,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                selectedAutoPlaylistTitle = "Recently Played"
                                selectedAutoPlaylistSongs = recentlyPlayed
                            }
                        )
                    }

                    item {
                        AutoPlaylistRow(
                            title = "Most Played",
                            songCount = mostPlayed.size,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                selectedAutoPlaylistTitle = "Most Played"
                                selectedAutoPlaylistSongs = mostPlayed
                            }
                        )
                    }

                    // Custom Playlists Section
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "YOUR PLAYLISTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (playlists.isEmpty()) {
                        item {
                            Text(
                                text = "No custom playlists created yet",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(playlists, key = { it.id }) { playlist ->
                            CustomPlaylistRow(
                                playlist = playlist,
                                getSongCount = getSongCountForPlaylist,
                                onClick = { selectedPlaylistEntity = playlist }
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "AUTO PLAYLISTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    item {
                        AutoPlaylistCard(
                            title = "Favorites",
                            songCount = favorites.size,
                            icon = Icons.Default.Favorite,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                selectedAutoPlaylistTitle = "Favorites"
                                selectedAutoPlaylistSongs = favorites
                            }
                        )
                    }

                    item {
                        AutoPlaylistCard(
                            title = "Recently Played",
                            songCount = recentlyPlayed.size,
                            icon = Icons.Default.History,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                selectedAutoPlaylistTitle = "Recently Played"
                                selectedAutoPlaylistSongs = recentlyPlayed
                            }
                        )
                    }

                    item {
                        AutoPlaylistCard(
                            title = "Most Played",
                            songCount = mostPlayed.size,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                selectedAutoPlaylistTitle = "Most Played"
                                selectedAutoPlaylistSongs = mostPlayed
                            }
                        )
                    }

                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "YOUR PLAYLISTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (playlists.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "No custom playlists created yet",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(playlists, key = { it.id }) { playlist ->
                            CustomPlaylistCard(
                                playlist = playlist,
                                getSongCount = getSongCountForPlaylist,
                                onClick = { selectedPlaylistEntity = playlist }
                            )
                        }
                    }
                }
            }
        }

        val hasNowPlayingPill = currentPlayingSongId != null
        val plusButtonBottomPadding = if (hasNowPlayingPill) 168.dp else 88.dp

        // Center-aligned floating plus button just above footer pill
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = plusButtonBottomPadding),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(elevation = 10.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { showNewPlaylistDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Playlist",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (showNewPlaylistDialog) {
            AlertDialog(
                onDismissRequest = {
                    showNewPlaylistDialog = false
                    playlistNameInput = ""
                },
                title = { Text("New Playlist") },
                text = {
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmedName = playlistNameInput.trim()
                            if (trimmedName.isNotEmpty()) {
                                onCreatePlaylist(trimmedName)
                                playlistNameInput = ""
                                showNewPlaylistDialog = false
                            }
                        },
                        enabled = playlistNameInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "Create",
                            color = if (playlistNameInput.isNotBlank()) Color.Black else Color.Black.copy(alpha = 0.5f)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showNewPlaylistDialog = false
                            playlistNameInput = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Selected Auto Playlist Overlay
        selectedAutoPlaylistTitle?.let { title ->
            PlaylistDetailOverlay(
                title = title,
                songs = selectedAutoPlaylistSongs,
                playlistEntity = null,
                allSongs = allSongs,
                currentPlayingSongId = currentPlayingSongId,
                onBack = { selectedAutoPlaylistTitle = null },
                onDeletePlaylist = {},
                onAddSongToPlaylist = { _, _ -> },
                onSongClick = { song, list -> onSongClick(song, list) },
                onPlayAll = onPlayAll,
                onShuffleAll = onShuffleAll
            )
        }

        // Selected Custom Playlist Overlay
        selectedPlaylistEntity?.let { playlist ->
            PlaylistDetailOverlay(
                title = playlist.name,
                songs = customPlaylistSongs,
                playlistEntity = playlist,
                allSongs = allSongs,
                currentPlayingSongId = currentPlayingSongId,
                onBack = { selectedPlaylistEntity = null },
                onDeletePlaylist = onDeletePlaylist,
                onAddSongToPlaylist = onAddSongToPlaylist,
                onSongClick = { song, list -> onSongClick(song, list) },
                onPlayAll = onPlayAll,
                onShuffleAll = onShuffleAll
            )
        }
    }
}

@Composable
fun CustomPlaylistRow(
    playlist: PlaylistEntity,
    getSongCount: (Long) -> Flow<Int>,
    onClick: () -> Unit
) {
    val count by getSongCount(playlist.id).collectAsState(initial = 0)
    AutoPlaylistRow(
        title = playlist.name,
        songCount = count,
        icon = Icons.AutoMirrored.Filled.QueueMusic,
        iconTint = MaterialTheme.colorScheme.primary,
        onClick = onClick
    )
}

@Composable
fun CustomPlaylistCard(
    playlist: PlaylistEntity,
    getSongCount: (Long) -> Flow<Int>,
    onClick: () -> Unit
) {
    val count by getSongCount(playlist.id).collectAsState(initial = 0)
    AutoPlaylistCard(
        title = playlist.name,
        songCount = count,
        icon = Icons.AutoMirrored.Filled.QueueMusic,
        iconTint = MaterialTheme.colorScheme.primary,
        onClick = onClick
    )
}

@Composable
fun PlaylistDetailOverlay(
    title: String,
    songs: List<SongEntity>,
    playlistEntity: PlaylistEntity?,
    allSongs: List<SongEntity>,
    currentPlayingSongId: Long?,
    onBack: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddSongToPlaylist: (Long, Long) -> Unit,
    onSongClick: (SongEntity, List<SongEntity>) -> Unit,
    onPlayAll: (List<SongEntity>) -> Unit,
    onShuffleAll: (List<SongEntity>) -> Unit
) {
    var showAddSongsSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(16.dp))

            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (playlistEntity != null) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Playlist options",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Playlist", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeletePlaylist(playlistEntity.id)
                                    onBack()
                                }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playlist Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DefaultAlbumArtPlaceholder(
                    styleIndex = (playlistEntity?.id?.toInt() ?: title.hashCode()).let { Math.abs(it) % 6 },
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    initial = title,
                    iconSize = 36.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${songs.size} songs",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (songs.isEmpty()) {
                // Empty Playlist State (Matching user screenshot)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 60.dp)
                ) {
                    // Overlapping cards illustration
                    EmptyPlaylistIllustration(
                        onAddClick = { showAddSongsSheet = true }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Your playlist is waiting for its first song",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Add tracks from your library and make it a banger.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { showAddSongsSheet = true },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Add songs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            } else {
                // Non-empty playlist: Play & Shuffle buttons + Song list
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onPlayAll(songs) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Play", color = Color.Black)
                    }

                    Button(
                        onClick = { onShuffleAll(songs) },
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
                    contentPadding = PaddingValues(bottom = 160.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(songs, key = { it.id }) { song ->
                        SongRowItem(
                            song = song,
                            isPlaying = song.id == currentPlayingSongId,
                            onClick = { onSongClick(song, songs) }
                        )
                    }
                }
            }
        }

        // Add Songs Sheet
        if (showAddSongsSheet && playlistEntity != null) {
            AddSongsToPlaylistSheet(
                allSongs = allSongs,
                playlistSongs = songs,
                onToggleSong = { songId, isAdded ->
                    if (isAdded) {
                        onAddSongToPlaylist(playlistEntity.id, songId)
                    }
                },
                onDismiss = { showAddSongsSheet = false }
            )
        }
    }
}

@Composable
fun EmptyPlaylistIllustration(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            // Card 1: Red/Orange dashed outline
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .offset(x = 24.dp)
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color(0xFFE94057),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect),
                            cornerRadius = CornerRadius(20.dp.toPx())
                        )
                    }
            )

            // Card 2: Blue dashed outline
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .offset(x = 12.dp)
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color(0xFF42A5F5),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect),
                            cornerRadius = CornerRadius(20.dp.toPx())
                        )
                    }
            )

            // Card 3: Yellow dashed outline
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color(0xFFFBC02D),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect),
                            cornerRadius = CornerRadius(20.dp.toPx())
                        )
                    }
            )

            // Card 4: Solid Mint Green Card with + Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = (-12).dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add songs",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToPlaylistSheet(
    allSongs: List<SongEntity>,
    playlistSongs: List<SongEntity>,
    onToggleSong: (songId: Long, isAdded: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val playlistSongIds = remember(playlistSongs) { playlistSongs.map { it.id }.toSet() }

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
            Text(
                text = "Add Songs to Playlist",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (allSongs.isEmpty()) {
                Text(
                    text = "No songs in library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(allSongs, key = { it.id }) { song ->
                        var isAdded by remember(playlistSongIds) { mutableStateOf(playlistSongIds.contains(song.id)) }

                        androidx.compose.material3.ListItem(
                            headlineContent = {
                                Text(
                                    text = song.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = song.artist,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = isAdded,
                                    onCheckedChange = {
                                        isAdded = it
                                        onToggleSong(song.id, it)
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isAdded = !isAdded
                                    onToggleSong(song.id, isAdded)
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Done",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AutoPlaylistRow(
    title: String,
    songCount: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$songCount songs",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AutoPlaylistCard(
    title: String,
    songCount: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$songCount songs",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
