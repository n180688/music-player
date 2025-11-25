package com.n180688.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        val imageTrackAlbumArt: ImageView = itemView.findViewById(R.id.imageTrackAlbumArt)
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

        //загружаем обложку
        loadAlbumArtWithGlide(track.albumId, track,  holder.imageTrackAlbumArt)

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



    /**
     * Загружает обложку для элемента списка с помощью Glide (асинхронно, с кэшем)
     * Если MediaStore не знает обложку - пробует извлечь из файла
     */
    private fun loadAlbumArtWithGlide(albumId: Long?, track: Track, imageView: ImageView) {
        if (albumId == null || albumId == 0L) {
            // Нет albumId - пробуем фоллбэк
            loadAlbumArtFromFileFallback(track.path, imageView)
            return
        }

        val albumArtUri = android.content.ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            albumId
        )

        // Glide с обработкой ошибки
        com.bumptech.glide.Glide.with(imageView.context)
            .load(albumArtUri)
            .placeholder(R.drawable.ic_music_note)
            .error(R.drawable.ic_music_note)
            .centerCrop()
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {

                // Вызывается при ОШИБКЕ загрузки
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    // MediaStore не дал обложку - пробуем из файла!
                    android.util.Log.d("AlbumArt", "MediaStore failed for ${track.title}, trying file fallback")
                    loadAlbumArtFromFileFallback(track.path, imageView)
                    return true  // true = мы обработали ошибку сами
                }

                // Вызывается при УСПЕШНОЙ загрузке
                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false  // false = пусть Glide сам установит картинку
                }
            })
            .into(imageView)
    }


    /**
     * Фоллбэк: извлекает обложку из MP3/M4A файла напрямую
     */
    private fun loadAlbumArtFromFileFallback(filePath: String, imageView: ImageView) {
        // Запускаем в  фоновом потоке
        Thread {
            try {
                val mmr = android.media.MediaMetadataRetriever()
                mmr.setDataSource(filePath)
                val data = mmr.embeddedPicture

                if (data != null) {
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)

                    // Переключаемся на UI поток для установки картинки
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        com.bumptech.glide.Glide.with(imageView.context)
                            .load(bitmap)
                            .centerCrop()
                            .into(imageView)
                    }

                    android.util.Log.d("AlbumArt", "Loaded from file tags")
                } else {
                    // Нет обложки - устанавливаем placeholder на UI потоке
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        com.bumptech.glide.Glide.with(imageView.context)
                            .load(R.drawable.ic_music_note)
                            .centerCrop()
                            .into(imageView)
                    }
                }
                mmr.release()
            } catch (e: Exception) {
                android.util.Log.e("AlbumArt", "Failed to load from file: ${e.message}")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    com.bumptech.glide.Glide.with(imageView.context)
                        .load(R.drawable.ic_music_note)
                        .centerCrop()
                        .into(imageView)
                }
            }
        }.start()
    }

}
