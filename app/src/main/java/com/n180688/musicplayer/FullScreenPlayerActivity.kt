package com.n180688.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.util.Log
import androidx.compose.ui.platform.LocalContext



class FullScreenPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FullScreenPlayerTheme {
                FullScreenPlayerScreen()
            }
        }
    }


    // Тема для Compose
    @Composable
    fun FullScreenPlayerTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Color(0xFFFFC107),  // Жёлтый акцент
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E)
            ),
            content = content
        )
    }

    @Composable
    fun FullScreenPlayerScreen() {
        // Подписываемся на состояние
        val currentTrack by PlayerState.currentTrack
        val isPlaying by PlayerState.isPlaying
        val currentPosition by PlayerState.currentPosition
        val duration by PlayerState.duration

        // Если нет трека - показываем заглушку
        if (currentTrack == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No track playing", color = Color.White)
            }
            return
        }

        FullScreenPlayer(
            track = currentTrack!!,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            onClose = { (this as? ComponentActivity)?.finish() },
            onPlayPause = { PlayerState.onPlayPause?.invoke() },
            onNext = { PlayerState.onNext?.invoke() },
            onPrevious = { PlayerState.onPrevious?.invoke() },
            onSeek = { PlayerState.onSeek?.invoke(it) }
        )
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun FullScreenPlayer(
        track: Track,
        isPlaying: Boolean,
        currentPosition: Int,
        duration: Int,
        onClose: () -> Unit,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onSeek: (Int) -> Unit
    ) {
        // State для анимации вращения
        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (isPlaying) 360f else 0f,
            animationSpec = infiniteRepeatable(

                animation = tween(10000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        // Градиентный фон
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF37474F),
                            Color(0xFF263238)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Кнопка "Назад"
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

//            // Обложка альбома
//            val albumArtUri = android.content.ContentUris.withAppendedId(
//                android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                albumId
//            )
//

                val context = LocalContext.current
                val bitmap = remember(track.albumId, track.path) {  // remember для recomposition
                    getAlbumArtBitmap(context.contentResolver, track.albumId, track.path)
                }

                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                        .rotate(rotation),
                    contentAlignment = Alignment.Center
                ) {
                    GlideImage(
                        model = bitmap ?: R.drawable.ic_music_note,  // Bitmap или drawable
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    ) {
                        it.error(R.drawable.ic_music_note)
                    }
                }


                Spacer(modifier = Modifier.height(48.dp))

                // Название трека
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Исполнитель
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Прогресс-бар
                var sliderPosition by remember { mutableFloatStateOf(currentPosition.toFloat()) }

                var isUserDragging by remember { mutableStateOf(false) }

                // Обновляем sliderPosition когда currentPosition меняется
                LaunchedEffect(currentPosition) {
                    if (!isUserDragging) {  // Только если пользователь не двигает
                        sliderPosition = currentPosition.toFloat()
                    }
                }


                Column {
                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            isUserDragging = true
                            sliderPosition = it
                        },
                        onValueChangeFinished = {
                            onSeek(sliderPosition.toInt())
                            isUserDragging = false
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFC107),
                            activeTrackColor = Color(0xFFFFC107),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    // Время
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),  // ← Используем currentPosition, не sliderPosition!
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = formatTime(duration),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Кнопки управления
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous
                    IconButton(onClick = onPrevious) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    }

                    // Play/Pause (большая кнопка)
                    // БЛЯТЬ

                    //ТУТ СКОРЕЕ ВСЕГО НЕ ПЕРЕРИСОВЫВАЕТСЯ ИЗ-ЗА ТРАБЛОВ С СОСТОЯНИЯМИ ТОЖЕ
                    FloatingActionButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(72.dp),
                        containerColor = Color(0xFFFFC107)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Black
                        )
                    }

                    // Next
                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Форматирование времени (мс -> "мм:сс")
    fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }


//ПОВЫНОСИТЬ ЭТО ОТДЕЛЬНО И УБРАТЬ ДУБЛИРОВАНИЕ

    fun getAlbumArtBitmap(
        contentResolver: ContentResolver,
        albumId: Long?,
        trackPath: String?  // Для fallback
    ): Bitmap? {
        if (albumId == null || albumId == 0L) return null

        val albumArtUri = Uri.withAppendedPath(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            albumId.toString()
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+: loadThumbnail, без _data
                contentResolver.loadThumbnail(albumArtUri, Size(512, 512), null)
            } else {
                // Старые: getBitmap
                MediaStore.Images.Media.getBitmap(contentResolver, albumArtUri)
            }
        } catch (e: Exception) {
            Log.e("AlbumArt", "MediaStore failed: ${e.message}")
            // Fallback на теги (синхронно, для одной картинки ок
            trackPath?.let { loadFromFileSync(it) }
        }
    }

    // Синхронный fallback (для мини-плеера/одиночных, не для списка)
    private fun loadFromFileSync(filePath: String): Bitmap? {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(filePath)
            val data = mmr.embeddedPicture
            mmr.release()
            data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (e: Exception) {
            Log.e("AlbumArt", "Fallback failed: ${e.message}")
            null
        }
    }
}