package com.n180688.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrackAdapter(
    private val tracks: List<Track>,
    private val onTrackClick: (Track) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTrackName: TextView = itemView.findViewById(R.id.textTrackName)
        val textArtistName: TextView = itemView.findViewById(R.id.textArtistName)
        val textTrackDuration: TextView = itemView.findViewById(R.id.textTrackDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]

        holder.textTrackName.text = track.title
        holder.textArtistName.text = track.artist
        holder.textTrackDuration.text = formatDuration(track.duration)

        holder.itemView.setOnClickListener {
            onTrackClick(track)
        }
    }

    override fun getItemCount(): Int = tracks.size

    /**
     * Форматирует длительность из миллисекунд в формат "мм:сс"
     */
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        // Форматируем: минуты:секунды (секунды всегда 2 цифры)
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
}
