package com.example.zplayer.data.model

import android.net.Uri

data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val albumArtUri: Uri?,
    val duration: Long
)
