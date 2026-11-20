package com.pixelpushers.sonixa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pixelpushers.sonixa.data.preferences.SongClickBehavior
import com.pixelpushers.sonixa.data.preferences.UserSettings
import com.pixelpushers.sonixa.ui.components.GlassNavigationBar
import com.pixelpushers.sonixa.ui.components.MainTab
import com.pixelpushers.sonixa.ui.components.MiniPlayerPill
import com.pixelpushers.sonixa.ui.equalizer.EqualizerScreen
import com.pixelpushers.sonixa.ui.equalizer.EqualizerViewModel
import com.pixelpushers.sonixa.ui.library.LibraryScreen
import com.pixelpushers.sonixa.ui.library.LibraryViewModel
import com.pixelpushers.sonixa.ui.lyrics.LyricsScreen
import com.pixelpushers.sonixa.ui.lyrics.LyricsViewModel
import com.pixelpushers.sonixa.ui.nowplaying.NowPlayingScreen
import com.pixelpushers.sonixa.ui.nowplaying.NowPlayingViewModel
import com.pixelpushers.sonixa.ui.permission.PermissionAndScanScreen
import com.pixelpushers.sonixa.ui.playlists.AddToPlaylistSheet
import com.pixelpushers.sonixa.ui.playlists.PlaylistsScreen
import com.pixelpushers.sonixa.ui.playlists.PlaylistsViewModel
import com.pixelpushers.sonixa.ui.queue.QueueScreen
import com.pixelpushers.sonixa.ui.search.SearchScreen
import com.pixelpushers.sonixa.ui.search.SearchViewModel
import com.pixelpushers.sonixa.ui.settings.SettingsScreen
import com.pixelpushers.sonixa.ui.settings.SettingsViewModel
import com.pixelpushers.sonixa.ui.splash.SplashScreen
import com.pixelpushers.sonixa.ui.sleeptimer.SleepTimerSheet
import com.pixelpushers.sonixa.ui.theme.SonixaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private val nowPlayingViewModel: NowPlayingViewModel by viewModels()
    private val equalizerViewModel: EqualizerViewModel by viewModels()
    private val lyricsViewModel: LyricsViewModel by viewModels()
    private val playlistsViewModel: PlaylistsViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private var hasAudioPermissionState = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions.values.any { it }
        hasAudioPermissionState.value = audioGranted
    }

    private var folderPickerLauncher: androidx.activity.result.ActivityResultLauncher<Uri?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val folderPickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val path = uri.path ?: ""
                    settingsViewModel.addIncludedFolder(path)
                    settingsViewModel.setHasScanned(false)
                    libraryViewModel.scanMusic()
                    settingsViewModel.setHasScanned(true)
                } catch (_: Exception) {}
            }
        }

        checkAudioPermission()

        setContent {
            val settings by settingsViewModel.settings.collectAsState()

            LaunchedEffect(hasAudioPermissionState.value, settings.hasScanned) {
                if (hasAudioPermissionState.value && !settings.hasScanned) {
                    libraryViewModel.scanMusic()
                    settingsViewModel.setHasScanned(true)
                }
            }

            SonixaTheme(
                appTheme = settings.theme,
                materialDesignEnabled = settings.materialDesignEnabled,
                materialYouEnabled = settings.materialYouEnabled,
                dynamicAccentColor = settings.customThemeColor?.let { Color(it.toULong()) }
            ) {
                MainAppContent(
                    hasPermission = hasAudioPermissionState.value,
                    onRequestPermission = { requestAudioPermissions() },
                    libraryViewModel = libraryViewModel,
                    nowPlayingViewModel = nowPlayingViewModel,
                    equalizerViewModel = equalizerViewModel,
                    lyricsViewModel = lyricsViewModel,
                    playlistsViewModel = playlistsViewModel,
                    searchViewModel = searchViewModel,
                    settingsViewModel = settingsViewModel,
                    settings = settings,
                    onChooseFolder = { folderPickerLauncher.launch(null) }
                )
            }
        }
    }

    private fun checkAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        hasAudioPermissionState.value = isGranted
    }

    private fun requestAudioPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

enum class ScreenOverlay {
    NONE, NOW_PLAYING, LYRICS, EQUALIZER, QUEUE
}

@Composable
fun MainAppContent(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    libraryViewModel: LibraryViewModel,
    nowPlayingViewModel: NowPlayingViewModel,
    equalizerViewModel: EqualizerViewModel,
    lyricsViewModel: LyricsViewModel,
    playlistsViewModel: PlaylistsViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel,
    settings: UserSettings,
    onChooseFolder: () -> Unit
) {
    val songs by libraryViewModel.songs.collectAsState()
    val scanProgress by libraryViewModel.scanProgress.collectAsState()
    val notifications by libraryViewModel.notifications.collectAsState()

    val playerState by nowPlayingViewModel.playerState.collectAsState()
    val eqState by equalizerViewModel.state.collectAsState()
    val lyricsState by lyricsViewModel.uiState.collectAsState()

    val playlists by playlistsViewModel.playlists.collectAsState()
    val favorites by playlistsViewModel.favorites.collectAsState()
    val recentlyPlayed by playlistsViewModel.recentlyPlayed.collectAsState()
    val mostPlayed by playlistsViewModel.mostPlayed.collectAsState()

    val searchQuery by searchViewModel.query.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()

    var selectedTab by remember { mutableStateOf(MainTab.LIBRARY) }
    var activeOverlay by remember { mutableStateOf(ScreenOverlay.NONE) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }

    LaunchedEffect(playerState.currentSong?.id) {
        playerState.currentSong?.id?.let { songId ->
            lyricsViewModel.loadLyricsForSong(songId)
        }
    }

    val playSongWithBehavior: (com.pixelpushers.sonixa.data.local.entity.SongEntity, List<com.pixelpushers.sonixa.data.local.entity.SongEntity>) -> Unit = { song, list ->
        nowPlayingViewModel.playSong(song, list)
        if (settings.songClickBehavior == SongClickBehavior.OPEN_NOW_PLAYING) {
            activeOverlay = ScreenOverlay.NOW_PLAYING
        }
    }

    var showSplashScreen by remember { mutableStateOf(true) }

    if (showSplashScreen) {
        SplashScreen(
            appTheme = settings.theme,
            onSplashFinished = { showSplashScreen = false }
        )
    } else if (!hasPermission || scanProgress.isScanning) {
        PermissionAndScanScreen(
            hasPermission = hasPermission,
            scanProgress = scanProgress,
            onRequestPermission = onRequestPermission,
            onContinueWithoutPermission = { },
            onCancelScan = {
                libraryViewModel.cancelScan()
                settingsViewModel.setHasScanned(true)
            },
            onChooseFolder = onChooseFolder
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tab Content
                when (selectedTab) {
                    MainTab.LIBRARY -> LibraryScreen(
                        songs = songs,
                        currentPlayingSongId = playerState.currentSong?.id,
                        isNowPlayingPillPresent = playerState.currentSong != null,
                        notifications = notifications,
                        playlists = playlists,
                        pinnedAlbums = settings.pinnedAlbums,
                        pinnedArtists = settings.pinnedArtists,
                        pinnedFolders = settings.pinnedFolders,
                        onTogglePinAlbum = { settingsViewModel.togglePinAlbum(it) },
                        onTogglePinArtist = { settingsViewModel.togglePinArtist(it) },
                        onTogglePinFolder = { settingsViewModel.togglePinFolder(it) },
                        onSongClick = { song -> playSongWithBehavior(song, songs) },
                        onPlayAll = { playlist -> playSongWithBehavior(playlist.first(), playlist) },
                        onShuffleAll = { playlist -> playSongWithBehavior(playlist.shuffled().first(), playlist.shuffled()) },
                        onClearNotifications = { libraryViewModel.clearNotifications() },
                        onBulkDeleteSongs = { songsToDelete ->
                            libraryViewModel.deleteSongs(songsToDelete.map { it.id })
                        },
                        onBulkAddToPlaylist = { playlistId, songIds ->
                            songIds.forEach { songId ->
                                playlistsViewModel.addSongToPlaylist(playlistId, songId)
                            }
                        },
                        onCreatePlaylist = { name -> playlistsViewModel.createPlaylist(name) },
                        onRescanMusic = { settingsViewModel.rescanLibrary() },
                        onChooseFolders = { selectedTab = MainTab.SETTINGS }
                    )

                    MainTab.SEARCH -> SearchScreen(
                        query = searchQuery,
                        searchResults = searchResults,
                        currentPlayingSongId = playerState.currentSong?.id,
                        playlists = playlists,
                        onQueryChange = { searchViewModel.updateQuery(it) },
                        onClearQuery = { searchViewModel.clearQuery() },
                        onSongClick = { song -> playSongWithBehavior(song, searchResults) },
                        onAddToQueue = { song -> nowPlayingViewModel.addToQueue(song) },
                        onTogglePlaylist = { playlistId, songId -> playlistsViewModel.addSongToPlaylist(playlistId, songId) },
                        onCreatePlaylist = { name -> playlistsViewModel.createPlaylist(name) },
                        onDeleteSong = { song -> searchViewModel.deleteSong(song) }
                    )

                    MainTab.PLAYLISTS -> PlaylistsScreen(
                        playlists = playlists,
                        favorites = favorites,
                        recentlyPlayed = recentlyPlayed,
                        mostPlayed = mostPlayed,
                        allSongs = songs,
                        currentPlayingSongId = playerState.currentSong?.id,
                        onCreatePlaylist = { playlistsViewModel.createPlaylist(it) },
                        onAddSongToPlaylist = { playlistId, songId ->
                            playlistsViewModel.addSongToPlaylist(playlistId, songId)
                        },
                        onDeletePlaylist = { playlistId ->
                            playlistsViewModel.deletePlaylist(playlistId)
                        },
                        getSongsForPlaylist = { playlistId ->
                            playlistsViewModel.getSongsForPlaylist(playlistId)
                        },
                        getSongCountForPlaylist = { playlistId ->
                            playlistsViewModel.getSongCountForPlaylist(playlistId)
                        },
                        onSongClick = { song, list -> playSongWithBehavior(song, list) },
                        onPlayAll = { list -> playSongWithBehavior(list.first(), list) },
                        onShuffleAll = { list -> playSongWithBehavior(list.shuffled().first(), list.shuffled()) }
                    )

                    MainTab.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onSetTheme = { settingsViewModel.setTheme(it) },
                        onSetMatchAlbumColors = { settingsViewModel.setMatchAlbumColors(it) },
                        onSetGaplessPlayback = { settingsViewModel.setGaplessPlayback(it) },
                        onSetHighResOutput = { settingsViewModel.setHighResOutput(it) },
                        onSetShowFormatBadges = { settingsViewModel.setShowFormatBadges(it) },
                        onSetExcludeHiddenFolders = { settingsViewModel.setExcludeHiddenFolders(it) },
                        onSetSongClickBehavior = { settingsViewModel.setSongClickBehavior(it) },
                        onSetDefaultThumbnailIndex = { settingsViewModel.setDefaultThumbnailIndex(it) },
                        onSetResumePlaybackOnStart = { settingsViewModel.setResumePlaybackOnStart(it) },
                        onSetPauseOnAudioDisconnect = { settingsViewModel.setPauseOnAudioDisconnect(it) },
                        onGenerateRandomTheme = { settingsViewModel.generateRandomVectorTheme() },
                        onAddSkippedFolder = { settingsViewModel.addSkippedFolder(it) },
                        onRemoveSkippedFolder = { settingsViewModel.removeSkippedFolder(it) },
                        onOpenEqualizer = { activeOverlay = ScreenOverlay.EQUALIZER },
                        onRescanMusic = { settingsViewModel.rescanLibrary() },
                        onSetFilterShortTracks = { settingsViewModel.setFilterShortTracks(it) },
                        onSetVibrationEnabled = { settingsViewModel.setVibrationEnabled(it) },
                        onChooseScanFolder = onChooseFolder,
                        onSetMaterialDesignEnabled = { settingsViewModel.setMaterialDesignEnabled(it) },
                        onSetMaterialYouEnabled = { settingsViewModel.setMaterialYouEnabled(it) }
                    )
                }

                // Floating Glass Pills (Mini Player + Navigation Bar)
                val isPillVisible = activeOverlay == ScreenOverlay.NONE

                AnimatedVisibility(
                    visible = isPillVisible,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp)
                    ) {
                        if (playerState.currentSong != null) {
                            MiniPlayerPill(
                                song = playerState.currentSong,
                                isPlaying = playerState.isPlaying,
                                vibrationEnabled = settings.vibrationEnabled,
                                onTogglePlayPause = { nowPlayingViewModel.togglePlayPause() },
                                onNextTrack = { nowPlayingViewModel.skipToNext() },
                                onClick = { activeOverlay = ScreenOverlay.NOW_PLAYING },
                                onToggleFavorite = { nowPlayingViewModel.toggleFavorite() }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        GlassNavigationBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                }

                // Fullscreen Overlays
                when (activeOverlay) {
                    ScreenOverlay.NOW_PLAYING -> NowPlayingScreen(
                        queue = playerState.queue,
                        currentIndex = playerState.currentIndex,
                        isPlaying = playerState.isPlaying,
                        currentPositionMs = playerState.currentPositionMs,
                        totalDurationMs = playerState.totalDurationMs,
                        isShuffleEnabled = playerState.isShuffleEnabled,
                        isRepeatEnabled = playerState.isRepeatEnabled,
                        repeatMode = playerState.repeatMode,
                        isSleepTimerActive = playerState.isSleepTimerActive,
                        sleepTimerRemainingMs = playerState.sleepTimerRemainingMs,
                        isEqualizerEnabled = eqState.isEnabled,
                        lyricsState = lyricsState,
                        onSaveLyrics = { id, text -> lyricsViewModel.saveLyrics(id, text) },
                        onSeekToLyric = { pos -> nowPlayingViewModel.seekTo(pos) },
                        onTogglePlayPause = { nowPlayingViewModel.togglePlayPause() },
                        onSeekTo = { nowPlayingViewModel.seekTo(it) },
                        onSkipToNext = { nowPlayingViewModel.skipToNext() },
                        onSkipToPrevious = { nowPlayingViewModel.skipToPrevious() },
                        onToggleFavorite = { nowPlayingViewModel.toggleFavorite() },
                        onToggleShuffle = { nowPlayingViewModel.toggleShuffle() },
                        onToggleRepeat = { nowPlayingViewModel.toggleRepeat() },
                        onOpenAddToPlaylist = { showAddToPlaylistSheet = true },
                        onSongSelected = { index ->
                            if (index in playerState.queue.indices) {
                                nowPlayingViewModel.playSong(playerState.queue[index], playerState.queue)
                            }
                        },
                        onDismiss = { activeOverlay = ScreenOverlay.NONE },
                        onOpenEqualizer = { activeOverlay = ScreenOverlay.EQUALIZER },
                        onOpenSleepTimer = { showSleepTimerSheet = true },
                        onOpenLyrics = {
                            playerState.currentSong?.let { lyricsViewModel.loadLyricsForSong(it.id) }
                            activeOverlay = ScreenOverlay.LYRICS
                        },
                        onOpenQueue = { activeOverlay = ScreenOverlay.QUEUE }
                    )

                    ScreenOverlay.LYRICS -> LyricsScreen(
                        song = playerState.currentSong,
                        uiState = lyricsState,
                        currentPositionMs = playerState.currentPositionMs,
                        onSeekTo = { nowPlayingViewModel.seekTo(it) },
                        onSaveLyrics = { id, text -> lyricsViewModel.saveLyrics(id, text) },
                        onBack = { activeOverlay = ScreenOverlay.NOW_PLAYING }
                    )

                    ScreenOverlay.EQUALIZER -> EqualizerScreen(
                        state = eqState,
                        onToggleEnabled = { equalizerViewModel.toggleEnabled(it) },
                        onBandLevelChange = { b, l -> equalizerViewModel.setBandLevel(b, l) },
                        onPresetSelect = { equalizerViewModel.selectPreset(it) },
                        onBassBoostChange = { equalizerViewModel.setBassBoost(it) },
                        onVirtualizerChange = { equalizerViewModel.setVirtualizer(it) },
                        onBack = { activeOverlay = ScreenOverlay.NOW_PLAYING }
                    )

                    ScreenOverlay.QUEUE -> QueueScreen(
                        queue = playerState.queue,
                        currentIndex = playerState.currentIndex,
                        onSongSelect = { index ->
                            if (index in playerState.queue.indices) {
                                nowPlayingViewModel.playSong(playerState.queue[index], playerState.queue)
                            }
                        },
                        onRemoveFromQueue = { },
                        onClearQueue = { },
                        onBack = { activeOverlay = ScreenOverlay.NOW_PLAYING }
                    )

                    ScreenOverlay.NONE -> {}
                }

                // Sleep Timer Sheet
                if (showSleepTimerSheet) {
                    SleepTimerSheet(
                        onDismiss = { showSleepTimerSheet = false },
                        onStartTimer = { minutes ->
                            nowPlayingViewModel.startSleepTimer(minutes)
                        }
                    )
                }

                // Add to Playlist Sheet
                if (showAddToPlaylistSheet) {
                    AddToPlaylistSheet(
                        playlists = playlists,
                        onTogglePlaylist = { playlistId, isChecked ->
                            playerState.currentSong?.let { song ->
                                if (isChecked) {
                                    playlistsViewModel.addSongToPlaylist(playlistId, song.id)
                                }
                            }
                        },
                        onCreateNewPlaylist = { name ->
                            playlistsViewModel.createPlaylist(name)
                        },
                        onDismiss = { showAddToPlaylistSheet = false }
                    )
                }
            }
        }
    }
}
