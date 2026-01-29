package com.n180688.musicplayer

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

/**
 * Singleton для хранения состояния плеера
 * Используется и в MainActivity (XML), и в FullScreenPlayer (Compose)
 */
object PlayerState {

    // Compose State (реактивные переменные)
    private val _currentTrack = mutableStateOf<Track?>(null)
    val currentTrack: State<Track?> = _currentTrack

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _currentPosition = mutableStateOf(0)
    val currentPosition: State<Int> = _currentPosition

    private val _duration = mutableStateOf(0)
    val duration: State<Int> = _duration

    // Callback для команд из Compose в MainActivity
    var onPlayPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onSeek: ((Int) -> Unit)? = null

    // Методы для обновления состояния (вызываются из MainActivity)
    fun updateTrack(track: Track?) {
        _currentTrack.value = track
    }

    fun updatePlayingState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updatePosition(position: Int) {
        _currentPosition.value = position
    }

    fun updateDuration(dur: Int) {
        _duration.value = dur
    }
}