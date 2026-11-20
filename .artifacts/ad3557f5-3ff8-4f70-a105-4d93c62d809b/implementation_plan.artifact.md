# Use Material 3 Components in the Playlist Section

Refactor the playlist section (`PlaylistsScreen.kt`, `AddToPlaylistSheet.kt`, and related playlist components) to fully leverage official Material 3 components (`ElevatedCard`, `OutlinedCard`, `ListItem`, `FilledTonalButton`, etc.).

## User Review Required

> [!IMPORTANT]
> This refactoring standardizes playlist cards, row items, and bottom sheet list items using Material 3 design tokens and components without altering existing database logic or business rules.

## Proposed Changes

### [PlaylistsScreen.kt](file:///Users/nitinjangra/AndroidStudioProjects/SonicCanvas/app/src/main/java/com/pixelpushers/sonixa/ui/playlists/PlaylistsScreen.kt)
- Replace custom Box/background containers with Material 3 `ElevatedCard` / `OutlinedCard` for auto playlists and custom playlists.
- Use Material 3 typography and color roles (`surfaceVariant`, `primaryContainer`, `onSurfaceVariant`).

### [AddToPlaylistSheet.kt](file:///Users/nitinjangra/AndroidStudioProjects/SonicCanvas/app/src/main/java/com/pixelpushers/sonixa/ui/playlists/AddToPlaylistSheet.kt)
- Use Material 3 `ListItem` or structured Material 3 rows inside the bottom sheet.
- Apply M3 button styles and dialog styling.

## Verification Plan

### Automated Tests
- Run `./gradlew testDebugUnitTest` and `./gradlew :app:assembleRelease`.

### Manual Verification
- Verify playlist UI rendering, list vs grid view switching, and bottom sheet interactions.
