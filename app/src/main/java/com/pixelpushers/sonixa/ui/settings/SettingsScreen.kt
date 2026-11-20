package com.pixelpushers.sonixa.ui.settings

import com.pixelpushers.sonixa.ui.components.formatFolderNameAndPath
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelpushers.sonixa.data.preferences.AppTheme
import com.pixelpushers.sonixa.data.preferences.SongClickBehavior
import com.pixelpushers.sonixa.data.preferences.UserSettings
import com.pixelpushers.sonixa.ui.components.DefaultAlbumArtPlaceholder
import com.pixelpushers.sonixa.ui.components.DefaultThumbnailStyle

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onSetTheme: (AppTheme) -> Unit,
    onSetMatchAlbumColors: (Boolean) -> Unit,
    onSetGaplessPlayback: (Boolean) -> Unit,
    onSetHighResOutput: (Boolean) -> Unit,
    onSetShowFormatBadges: (Boolean) -> Unit,
    onSetExcludeHiddenFolders: (Boolean) -> Unit,
    onSetSongClickBehavior: (SongClickBehavior) -> Unit,
    onSetDefaultThumbnailIndex: (Int) -> Unit,
    onSetResumePlaybackOnStart: (Boolean) -> Unit,
    onSetPauseOnAudioDisconnect: (Boolean) -> Unit,
    onGenerateRandomTheme: () -> Unit,
    onAddSkippedFolder: (String) -> Unit,
    onRemoveSkippedFolder: (String) -> Unit,
    onOpenEqualizer: () -> Unit,
    onRescanMusic: () -> Unit,
    onSetFilterShortTracks: (Boolean) -> Unit,
    onSetVibrationEnabled: (Boolean) -> Unit,
    onChooseScanFolder: () -> Unit,
    onSetMaterialDesignEnabled: (Boolean) -> Unit,
    onSetMaterialYouEnabled: (Boolean) -> Unit,
    onExportBackup: () -> Unit = {},
    onRestoreBackup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showThemeSheet by remember { mutableStateOf(false) }
    var showSongClickSheet by remember { mutableStateOf(false) }
    var showSkippedFoldersSheet by remember { mutableStateOf(false) }
    var showThumbnailSheet by remember { mutableStateOf(false) }

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

            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Appearance Section
                item {
                    SettingsSectionHeader("APPEARANCE")
                }
                item {
                    SettingsCard {
                        SettingsRowClickable(
                            title = "Theme",
                            subtitle = settings.theme.name.lowercase().replaceFirstChar { it.uppercase() },
                            onClick = { showThemeSheet = true }
                        )
                        SettingsRowClickable(
                            title = "Generate random vector theme",
                            subtitle = "Create and save a unique random accent color",
                            onClick = onGenerateRandomTheme
                        )
                        SettingsRowClickable(
                            title = "Default album thumbnail",
                            subtitle = DefaultThumbnailStyle.entries.getOrElse(settings.defaultThumbnailIndex) { DefaultThumbnailStyle.MINT_GLOW }.title,
                            onClick = { showThumbnailSheet = true }
                        )
                        SettingsRowSwitch(
                            title = "Match album artwork colors",
                            subtitle = "Adapt accent color to current playing track",
                            checked = settings.matchAlbumColors,
                            vibrationEnabled = settings.vibrationEnabled,
                            onCheckedChange = onSetMatchAlbumColors
                        )
                        SettingsRowSwitch(
                            title = "Material Design system",
                            subtitle = "Enable Material 3 components across the app",
                            checked = settings.materialDesignEnabled,
                            vibrationEnabled = settings.vibrationEnabled,
                            onCheckedChange = onSetMaterialDesignEnabled
                        )
                        SettingsRowSwitch(
                            title = "Material You dynamic color",
                            subtitle = "Use wallpaper-based dynamic theming (M3 sub-toggle)",
                            checked = settings.materialYouEnabled && settings.materialDesignEnabled,
                            vibrationEnabled = settings.vibrationEnabled,
                            onCheckedChange = {
                                if (settings.materialDesignEnabled) {
                                    onSetMaterialYouEnabled(it)
                                }
                            }
                        )
                    }
                }

                // Playback Section
                item {
                    SettingsSectionHeader("PLAYBACK")
                }
                item {
                    SettingsCard {
                        SettingsRowClickable(
                            title = "Equalizer",
                            subtitle = "Adjust frequency bands and bass boost",
                            onClick = onOpenEqualizer
                        )
                        SettingsRowClickable(
                            title = "On song click behavior",
                            subtitle = if (settings.songClickBehavior == SongClickBehavior.OPEN_NOW_PLAYING) "Open Now Playing" else "Play in background",
                            onClick = { showSongClickSheet = true }
                        )
                        SettingsRowSwitch(
                            title = "Resume playback on app opening",
                            subtitle = "Automatically resume last played song on startup",
                            checked = settings.resumePlaybackOnStart,
                            onCheckedChange = onSetResumePlaybackOnStart
                        )
                        SettingsRowSwitch(
                            title = "Pause on headset/Bluetooth disconnect",
                            subtitle = "Pause playback when headphones or Bluetooth disconnect",
                            checked = settings.pauseOnAudioDisconnect,
                            onCheckedChange = onSetPauseOnAudioDisconnect
                        )
                        SettingsRowSwitch(
                            title = "Gapless playback",
                            subtitle = "Seamless transition between tracks",
                            checked = settings.gaplessPlayback,
                            onCheckedChange = onSetGaplessPlayback
                        )
                        SettingsRowSwitch(
                            title = "Haptic feedback on playback controls",
                            subtitle = "Vibrate on play, pause, previous, and next taps",
                            checked = settings.vibrationEnabled,
                            onCheckedChange = onSetVibrationEnabled
                        )
                    }
                }

                // Audio Section
                item {
                    SettingsSectionHeader("AUDIO & QUALITY")
                }
                item {
                    SettingsCard {
                        SettingsRowSwitch(
                            title = "High-res output",
                            subtitle = "Enable 24-bit / 96 kHz direct output when available",
                            checked = settings.highResOutput,
                            onCheckedChange = onSetHighResOutput
                        )
                        SettingsRowSwitch(
                            title = "Show format badges",
                            subtitle = "Display FLAC badge on lossless tracks",
                            checked = settings.showFormatBadges,
                            onCheckedChange = onSetShowFormatBadges
                        )
                    }
                }

                // Library Section
                item {
                    SettingsSectionHeader("LIBRARY")
                }
                item {
                    SettingsCard {
                        SettingsRowSwitch(
                            title = "Filter tracks under 30 seconds",
                            subtitle = "Exclude ringtones and audio clips shorter than 30 seconds",
                            checked = settings.filterShortTracks,
                            onCheckedChange = onSetFilterShortTracks
                        )
                        SettingsRowSwitch(
                            title = "Exclude hidden folders",
                            subtitle = "Ignore audio files in hidden folders (starting with .)",
                            checked = settings.excludeHiddenFolders,
                            onCheckedChange = onSetExcludeHiddenFolders
                        )
                        SettingsRowClickable(
                            title = "Choose folders to scan",
                            subtitle = "${settings.includedFolderPaths.size} custom folders selected",
                            onClick = onChooseScanFolder
                        )
                        SettingsRowClickable(
                            title = "Manage skipped folders",
                            subtitle = "${settings.skippedFolderPaths.size} folders explicitly skipped",
                            onClick = { showSkippedFoldersSheet = true }
                        )
                        SettingsRowClickable(
                            title = "Rescan music library",
                            subtitle = "Scan device for new or removed audio files",
                            onClick = onRescanMusic
                        )
                    }
                }

                // Backup & Restore Section
                item {
                    SettingsSectionHeader("BACKUP & RESTORE")
                }
                item {
                    SettingsCard {
                        SettingsRowClickable(
                            title = "Export Backup",
                            subtitle = "Save playlists, settings, and pinned items to a JSON file",
                            onClick = onExportBackup
                        )
                        SettingsRowClickable(
                            title = "Import / Restore Backup",
                            subtitle = "Restore playlists, settings, and pinned items from a backup file",
                            onClick = onRestoreBackup
                        )
                    }
                }

                item {
                    SettingsSectionHeader("ABOUT")
                }
                item {
                    SettingsCard {
                        SettingsRowClickable(
                            title = "Sonixa",
                            subtitle = "Version 2.1.0 (High-Res Audio Player)",
                            onClick = { }
                        )
                        SettingsRowClickable(
                            title = "Audio Engine",
                            subtitle = "Media3 ExoPlayer with direct high-res bit-depth decoding",
                            onClick = { }
                        )
                        SettingsRowClickable(
                            title = "Privacy & Storage",
                            subtitle = "100% offline — Your music never leaves your device",
                            onClick = { }
                        )
                        SettingsRowClickable(
                            title = "Made with ❤️ in Bharat for Audiophiles",
                            subtitle = "Crafted for high-resolution offline music enjoyment",
                            onClick = { }
                        )
                    }
                }
            }
        }

        if (showThemeSheet) {
            ThemeSelectionSheet(
                currentTheme = settings.theme,
                onSelectTheme = {
                    onSetTheme(it)
                    showThemeSheet = false
                },
                onDismiss = { showThemeSheet = false }
            )
        }

        if (showSongClickSheet) {
            SongClickBehaviorSelectionSheet(
                currentBehavior = settings.songClickBehavior,
                onSelectBehavior = {
                    onSetSongClickBehavior(it)
                    showSongClickSheet = false
                },
                onDismiss = { showSongClickSheet = false }
            )
        }

        if (showSkippedFoldersSheet) {
            SkippedFoldersSheet(
                skippedFolders = settings.skippedFolderPaths,
                onAddFolder = onAddSkippedFolder,
                onRemoveFolder = onRemoveSkippedFolder,
                onDismiss = { showSkippedFoldersSheet = false }
            )
        }

        if (showThumbnailSheet) {
            DefaultThumbnailSelectionSheet(
                currentIndex = settings.defaultThumbnailIndex,
                onSelectIndex = {
                    onSetDefaultThumbnailIndex(it)
                    showThumbnailSheet = false
                },
                onDismiss = { showThumbnailSheet = false }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
fun SettingsRowClickable(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsRowSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    vibrationEnabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = {
                if (vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionSheet(
    currentTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Choose Theme",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            AppTheme.entries.forEach { theme ->
                val isSelected = currentTheme == theme
                val label = when (theme) {
                    AppTheme.SYSTEM -> "System Default"
                    AppTheme.LIGHT -> "Light Theme"
                    AppTheme.DARK -> "Dark Theme"
                    AppTheme.BLACK -> "OLED Pure Black"
                }

                val swatchColor = when (theme) {
                    AppTheme.SYSTEM -> Color.Gray
                    AppTheme.LIGHT -> Color(0xFFF4F6F8)
                    AppTheme.DARK -> Color(0xFF0F1115)
                    AppTheme.BLACK -> Color(0xFF000000)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTheme(theme) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongClickBehaviorSelectionSheet(
    currentBehavior: SongClickBehavior,
    onSelectBehavior: (SongClickBehavior) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "On Song Click Behavior",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            SongClickBehavior.entries.forEach { behavior ->
                val isSelected = currentBehavior == behavior
                val label = if (behavior == SongClickBehavior.OPEN_NOW_PLAYING) "Open Now Playing" else "Play in background"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectBehavior(behavior) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultThumbnailSelectionSheet(
    currentIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Default Album Thumbnail",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                items(DefaultThumbnailStyle.entries.size) { index ->
                    val isSelected = currentIndex == index

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectIndex(index) }
                    ) {
                        DefaultAlbumArtPlaceholder(
                            styleIndex = index,
                            modifier = Modifier.fillMaxSize(),
                            iconSize = 28.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkippedFoldersSheet(
    skippedFolders: Set<String>,
    onAddFolder: (String) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newFolderPath by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Skipped Folders",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newFolderPath,
                onValueChange = { newFolderPath = it },
                placeholder = { Text("e.g. /storage/emulated/0/Music/Ringtones") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (newFolderPath.isNotBlank()) {
                        onAddFolder(newFolderPath.trim())
                        newFolderPath = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Folder Path", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (skippedFolders.isEmpty()) {
                Text(
                    text = "No folders explicitly skipped.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(skippedFolders.toList()) { path ->
                        val (name, displayPath) = formatFolderNameAndPath(path)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = displayPath,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onRemoveFolder(path) }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
