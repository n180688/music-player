package com.n180688.musicplayer


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Адаптер для отображения списка треков в RecyclerView
 *
 * @param tracks - список треков для отображения
 * @param onTrackClick - callback функция, вызывается при клике на трек
 */
class TrackAdapter(
    private val tracks: List<Track>,
    private val onTrackClick: (Track) -> Unit  // Лямбда для обработки клика
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    /**
     * ViewHolder - контейнер для View элементов одного трека
     * Хранит ссылки на TextView, чтобы не искать их каждый раз через findViewById
     */
    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTrackName: TextView = itemView.findViewById(R.id.textTrackName)
        val textArtistName: TextView = itemView.findViewById(R.id.textArtistName)
    }

    /**
     * Создает новый ViewHolder
     * Вызывается когда RecyclerView нужен новый элемент списка
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        // Inflate = "раздуть" XML в View объект
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)

        return TrackViewHolder(view)
    }

    /**
     * Заполняет ViewHolder данными трека
     * Вызывается для каждого видимого элемента списка
     *
     * @param holder - ViewHolder который нужно заполнить
     * @param position - позиция элемента в списке
     */
    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]

        // Заполняем TextView данными
        holder.textTrackName.text = track.title
        holder.textArtistName.text = track.artist

        // Обработчик клика на весь элемент списка
        holder.itemView.setOnClickListener {
            onTrackClick(track)  // Вызываем callback с выбранным треком
        }
    }

    /**
     * Возвращает количество элементов в списке
     * RecyclerView использует это чтобы понять сколько элементов отрисовывать
     */
    override fun getItemCount(): Int = tracks.size
}
