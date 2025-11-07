package com.n180688.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    // UI элементы
    private lateinit var recyclerView: RecyclerView
    private lateinit var textCurrentTrack: TextView
    private lateinit var buttonPlay: Button
    private lateinit var buttonNext: Button
    private lateinit var buttonPrevious: Button
    private lateinit var buttonPlayMode: Button

    // Данные
    private val tracks = mutableListOf<Track>()  // Список всех треков
    private var currentTrackIndex = -1  // Индекс текущего трека (-1 = ничего не играет)

    // MediaPlayer для воспроизведения
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    // Режим воспроизведения
    private var playMode: PlayMode = PlayMode.NORMAL

    // Для shuffle режима - запоминаем порядок воспроизведения
    private var shuffledIndices = mutableListOf<Int>()

    // Адаптер для списка
    private lateinit var adapter: TrackAdapter

    // Запрос разрешений
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Разрешение получено - сканируем музыку
            loadMusicFiles()
        } else {
            // Разрешение отклонено
            Toast.makeText(this, "Нужно разрешение для доступа к музыке", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI элементов
        initViews()

        // Настройка RecyclerView
        setupRecyclerView()

        // Настройка кнопок
        setupButtons()

        // Проверка и запрос разрешений
        checkAndRequestPermissions()
    }

    // Привязка UI элементов к переменным
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewTracks)
        textCurrentTrack = findViewById(R.id.textCurrentTrack)
        buttonPlay = findViewById(R.id.buttonPlay)
        buttonNext = findViewById(R.id.buttonNext)
        buttonPrevious = findViewById(R.id.buttonPrevious)
        buttonPlayMode = findViewById(R.id.buttonPlayMode)
    }

    // Настройка RecyclerView (списка треков)
    private fun setupRecyclerView() {
        // LinearLayoutManager = вертикальный список
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Создаем адаптер с обработчиком клика
        adapter = TrackAdapter(tracks) { track ->
            // Это лямбда - вызывается при клике на трек
            val index = tracks.indexOf(track)
            playTrack(index)
        }

        recyclerView.adapter = adapter
    }

    // Настройка кнопок управления
    private fun setupButtons() {
        // Play/Pause
        buttonPlay.setOnClickListener {
            if (currentTrackIndex == -1 && tracks.isNotEmpty()) {
                // Ничего не играет - начинаем с первого трека
                playTrack(0)
            } else if (isPlaying) {
                // Играет - ставим на паузу
                pauseTrack()
            } else {// На паузе - возобновляем
                resumeTrack()
            }
        }

        // Next
        buttonNext.setOnClickListener {
            playNextTrack()
        }

        // Previous
        buttonPrevious.setOnClickListener {
            playPreviousTrack()
        }

        // Play Mode - переключение режимов
        buttonPlayMode.setOnClickListener {
            playMode = playMode.next()  // Переключаем режим
            buttonPlayMode.text = playMode.getIcon()  // Обновляем иконку

            // Если включили shuffle - создаем перемешанный список
            if (playMode == PlayMode.SHUFFLE) {
                createShuffleOrder()
            }

            // Показываем уведомление о смене режима
            val modeName = when (playMode) {
                PlayMode.NORMAL -> "По порядку"
                PlayMode.REPEAT_ALL -> "Повтор всех"
                PlayMode.REPEAT_ONE -> "Повтор трека"
                PlayMode.SHUFFLE -> "Случайный порядок"
            }
            Toast.makeText(this, modeName, Toast.LENGTH_SHORT).show()
        }
    }

    // Проверка и запрос разрешений
    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            // Android 12 и ниже
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // Разрешение уже есть
                loadMusicFiles()
            }
            else -> {
                // Запрашиваем разрешение
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    // Сканирование музыкальных файлов
    private fun loadMusicFiles() {
        tracks.clear()

        // ContentResolver для доступа к MediaStore
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,  // Путь к файлу
            MediaStore.Audio.Media.DURATION
        )

        // Запрос только музыки (не звуки уведомлений и т.д.)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"  // Сортировка по названию
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Неизвестный трек"
                val artist = cursor.getString(artistColumn) ?: "Неизвестный исполнитель"
                val path = cursor.getString(dataColumn)
                val duration = cursor.getLong(durationColumn)

                tracks.add(Track(id, title, artist, path, duration))
            }
        }

        // Обновляем список
        adapter.notifyDataSetChanged()

        if (tracks.isEmpty()) {
            Toast.makeText(this, "Музыкальные файлы не найдены", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Найдено треков: ${tracks.size}", Toast.LENGTH_SHORT).show()
        }
    }

    // Воспроизведение трека по индексу
    private fun playTrack(index: Int) {
        if (index < 0 || index >= tracks.size) return

        // Останавливаем текущий трек
        stopCurrentTrack()

        currentTrackIndex = index
        val track = tracks[index]

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(track.path)
                prepare()  // Синхронная подготовка (для prepareAsync нужен callback)
                start()
            }

            isPlaying = true
            updateUI()

            // Обработчик завершения трека
            mediaPlayer?.setOnCompletionListener {
                // Действие зависит от режима воспроизведения
                when (playMode) {
                    PlayMode.NORMAL -> {
                        // По порядку - если не последний, играем следующий
                        if (currentTrackIndex < tracks.size - 1) {
                            playNextTrack()
                        } else {
                            // Последний трек - останавливаемся
                            stopCurrentTrack()
                            updateUI()
                        }
                    }
                    PlayMode.REPEAT_ALL -> {
                        // Повтор всех - всегда следующий (зацикливается)
                        playNextTrack()
                    }
                    PlayMode.REPEAT_ONE -> {
                        // Повтор текущего - перезапускаем этот же трек
                        playTrack(currentTrackIndex)
                    }
                    PlayMode.SHUFFLE -> {
                        // Shuffle - следующий случайный
                        playNextTrack()
                    }
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка воспроизведения: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Пауза
    private fun pauseTrack() {
        mediaPlayer?.pause()
        isPlaying = false
        updateUI()
    }

    // Возобновление
    private fun resumeTrack() {
        mediaPlayer?.start()
        isPlaying = true
        updateUI()
    }

    // Остановка и освобождение MediaPlayer
    private fun stopCurrentTrack() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        isPlaying = false
    }

    // Следующий трек
    private fun playNextTrack() {
        if (tracks.isEmpty()) return

        currentTrackIndex = when (playMode) {
            PlayMode.SHUFFLE -> {
                // Shuffle: берем следующий из перемешанного списка
                val currentShuffleIndex = shuffledIndices.indexOf(currentTrackIndex)
                val nextShuffleIndex = (currentShuffleIndex + 1) % shuffledIndices.size
                shuffledIndices[nextShuffleIndex]
            }
            else -> {
                // Обычный/Repeat: следующий по кругу
                (currentTrackIndex + 1) % tracks.size
            }
        }

        playTrack(currentTrackIndex)
    }

    // Предыдущий трек
    private fun playPreviousTrack() {
        if (tracks.isEmpty()) return

        currentTrackIndex = when (playMode) {
            PlayMode.SHUFFLE -> {
                // Shuffle: берем предыдущий из перемешанного списка
                val currentShuffleIndex = shuffledIndices.indexOf(currentTrackIndex)
                val prevShuffleIndex = if (currentShuffleIndex <= 0) {
                    shuffledIndices.size - 1
                } else {
                    currentShuffleIndex - 1
                }
                shuffledIndices[prevShuffleIndex]
            }
            else -> {
                // Обычный/Repeat: предыдущий по кругу
                if (currentTrackIndex <= 0) {
                    tracks.size - 1
                } else {
                    currentTrackIndex - 1
                }
            }
        }

        playTrack(currentTrackIndex)
    }

    // Создает случайный порядок воспроизведения для Shuffle
    private fun createShuffleOrder() {
        shuffledIndices.clear()
        shuffledIndices.addAll(tracks.indices)  // Заполняем индексами 0, 1, 2, ...
        shuffledIndices.shuffle()  // Перемешиваем!
    }

    // Обновление UI
    private fun updateUI() {
        if (currentTrackIndex >= 0 && currentTrackIndex < tracks.size) {
            val track = tracks[currentTrackIndex]
            textCurrentTrack.text = "${track.title} - ${track.artist}"
        } else {
            textCurrentTrack.text = "Выберите трек"
        }

        // Меняем текст кнопки Play/Pause
        buttonPlay.text = if (isPlaying) "⏸" else "▶"
    }

    // Освобождение ресурсов при закрытии приложения
    override fun onDestroy() {
        super.onDestroy()
        stopCurrentTrack()
    }

    // Пауза при сворачивании приложения
    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            pauseTrack()
        }
    }
}