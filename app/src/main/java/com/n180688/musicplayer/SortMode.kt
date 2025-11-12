package com.n180688.musicplayer

/**
 * Режимы сортировки треков
 */
enum class SortMode {
    A_Z,        // По названию от A до Z
    Z_A,        // По названию от Z до A
    DATE_ADDED; // По дате добавления (новые первые)

    /**
     * Возвращает следующий режим сортировки (циклически)
     * A_Z → Z_A → DATE_ADDED → A_Z
     */
    fun next(): SortMode {
        return when (this) {
            A_Z -> Z_A
            Z_A -> DATE_ADDED
            DATE_ADDED -> A_Z
        }
    }

    /**
     * Сортирует список треков согласно текущему режиму
     * @param tracks Список треков для сортировки
     * @return Отсортированный список
     */
    fun sort(tracks: List<Track>): List<Track> {
        return when (this) {
            A_Z -> {
                // Сортировка по названию от A до Z (case-insensitive)
                tracks.sortedBy { it.title.lowercase() }
            }
            Z_A -> {
                // Сортировка по названию от Z до A
                tracks.sortedByDescending { it.title.lowercase() }
            }
            DATE_ADDED -> {
                // Сортировка по ID (ID присваивается при добавлении файла)
                // Большие ID = более новые файлы
                tracks.sortedByDescending { it.id }
            }
        }
    }
}
