package com.pixelpushers.sonixa.ui.components

fun formatFolderNameAndPath(rawPath: String): Pair<String, String> {
    if (rawPath.isBlank()) return Pair("Music Library", "/Internal Storage")

    val normalized = rawPath.replace("/storage/emulated/0", "Internal Storage")
        .replace("primary:", "Internal Storage/")
        .trimEnd('/')

    val lastSegment = normalized.substringAfterLast('/')
    val folderName = when {
        lastSegment.isBlank() || lastSegment == "0" -> "Music Library"
        else -> lastSegment
    }

    val displayPath = if (normalized.startsWith("Internal Storage")) "/$normalized" else normalized

    return Pair(folderName, displayPath)
}
