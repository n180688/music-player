package com.n180688.musicplayer

/**
 * Режимы воспроизведения треков
 */
enum class PlayMode {
    NORMAL,      // По порядку
    REPEAT_ALL,  // Повтор всех треков
    REPEAT_ONE,  // Повтор текущего трека
    SHUFFLE;     // Случайный порядок

    /**
     * Возвращает следующий режим (циклически)
     */
    fun next(): PlayMode {
        return when (this) {
            NORMAL -> REPEAT_ALL
            REPEAT_ALL -> REPEAT_ONE
            REPEAT_ONE -> SHUFFLE
            SHUFFLE -> NORMAL
        }
    }

    /**
     * Возвращает иконку для кнопки
     */
    fun getIcon(): String {
        return when (this) {
            NORMAL -> "➡️"
            REPEAT_ALL -> "🔁"
            REPEAT_ONE -> "🔂"
            SHUFFLE -> "🔀"
        }
    }
}
