package com.n180688.musicplayer

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
    val albumArt: String? = null
)