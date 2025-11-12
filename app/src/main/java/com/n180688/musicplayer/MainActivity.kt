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
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {

    // UI элементы
    private lateinit var recyclerView: RecyclerView
    private lateinit var textCurrentTrack: TextView
    private lateinit var buttonPlay: Button
    private lateinit var buttonNext: Button
    private lateinit var buttonPrevious: Button
    private lateinit var buttonPlayMode: Button
    private lateinit var buttonSort: Button
    private lateinit var toolbarNormal: LinearLayout
    private lateinit var toolbarSearch: LinearLayout
    private lateinit var editTextSearch: EditText
    private lateinit var buttonSearchBack: Button

    // Список для отфильтрованных треков
    private val filteredTracks = mutableListOf<Track>()
    private var isSearchMode = false  // Флаг режима поиска

    // Данные
    private val tracks = mutableListOf<Track>()  // Список всех треков
    private val originalTracks = mutableListOf<Track>()  // Оригинальный порядок (для восстановления)
    private var currentTrackIndex = -1  // Индекс текущего трека (-1 = ничего не играет)

    // MediaPlayer для воспроизведения
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    // Режим воспроизведения
    private var playMode: PlayMode = PlayMode.NORMAL

    // Режим сортировки
    private var sortMode: SortMode = SortMode.DATE_ADDED

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

        //настройка поиска
        setupSearch()

        //обработчик нажатия кнопки Назад
        setupBackPressHandler()

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
        buttonSort = findViewById(R.id.buttonSort)
        toolbarNormal = findViewById(R.id.toolbarNormal)
        toolbarSearch = findViewById(R.id.toolbarSearch)
        editTextSearch = findViewById(R.id.editTextSearch)
        buttonSearchBack = findViewById(R.id.buttonSearchBack)
    }


    // Настройка RecyclerView (списка треков)
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Используем filteredTracks вместо tracks
        adapter = TrackAdapter(filteredTracks) { track ->
            val index = if (isSearchMode) {
                // В режиме поиска ищем трек в ОСНОВНОМ списке
                tracks.indexOf(track)
            } else {
                filteredTracks.indexOf(track)
            }
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
            } else {
                // На паузе - возобновляем
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

        buttonSort.setOnClickListener {
            sortMode = sortMode.next()

            //обновляем текст кнопки

            val sortText = when (sortMode) {
                SortMode.A_Z -> "A-Z"
                SortMode.Z_A -> "Z-A"
                SortMode.DATE_ADDED -> "Дата"
            }
            buttonSort.text = sortText

            applySorting()

            val sortName = when (sortMode) {
                SortMode.A_Z -> "Сортировка A-Z"
                SortMode.Z_A -> "Сортировка Z-A"
                SortMode.DATE_ADDED -> "Сортировка по дате"
            }
            Toast.makeText(this, sortName, Toast.LENGTH_SHORT).show()
        }

        // Play Mode - переключение режимов
        buttonPlayMode.setOnClickListener {
            playMode = playMode.next()  // Переключаем режим
            // Обновляем иконку и текст

            //ДОБАВИТЬ НОРМАЛЬНУЮ  ИКОНКУ СТРЕЛКУ ДЛЯ ОБЫЧНОГО РЕЖИМА

            val iconRes = when (playMode) {
                PlayMode.NORMAL -> R.drawable.ic_repeat
                PlayMode.REPEAT_ALL -> R.drawable.ic_repeat
                PlayMode.REPEAT_ONE -> R.drawable.ic_repeat_one
                PlayMode.SHUFFLE -> R.drawable.ic_shuffle
            }

            buttonPlayMode.setCompoundDrawablesWithIntrinsicBounds(0, iconRes, 0,0)

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


    /**
     * Настройка поиска
     */
    private fun setupSearch() {
        // Кнопка "Назад" - выход из режима поиска
        buttonSearchBack.setOnClickListener {
            exitSearchMode()
        }

        // Слушатель изменения текста в поле поиска
        editTextSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Не используется
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Вызывается при каждом изменении текста
                filterTracks(s.toString())
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                // Не используется
            }
        })
    }

    /**
     * Переключение в режим поиска
     */
    private fun enterSearchMode() {
        isSearchMode = true

        // СКРЫВАЕМ ACTION BAR
        supportActionBar?.hide()

        // Показываем панель поиска, скрываем обычную
        toolbarNormal.visibility = android.view.View.GONE
        toolbarSearch.visibility = android.view.View.VISIBLE

        // Фокус на поле ввода + показываем клавиатуру
        editTextSearch.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editTextSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)

        // Изначально показываем все треки
        filteredTracks.clear()
        filteredTracks.addAll(tracks)
        adapter.notifyDataSetChanged()
    }

    /**
     * Выход из режима поиска
     */
    private fun exitSearchMode() {
        isSearchMode = false

        // ПОКАЗЫВАЕМ ACTION BAR ОБРАТНО
        supportActionBar?.show()

        // Возвращаем обычную панель
        toolbarNormal.visibility = android.view.View.VISIBLE
        toolbarSearch.visibility = android.view.View.GONE

        // Очищаем поле поиска
        editTextSearch.text.clear()

        // Прячем клавиатуру
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(editTextSearch.windowToken, 0)

        // Возвращаем полный список
        filteredTracks.clear()
        filteredTracks.addAll(tracks)
        adapter.notifyDataSetChanged()
    }

    /**
     * Фильтрация треков по запросу
     */
    private fun filterTracks(query: String) {
        filteredTracks.clear()

        if (query.isEmpty()) {
            // Пустой запрос - показываем все
            filteredTracks.addAll(tracks)
        } else {
            // Ищем треки где название или артист содержат запрос (без учета регистра)
            val lowerQuery = query.lowercase()
            filteredTracks.addAll(
                tracks.filter { track ->
                    track.title.lowercase().contains(lowerQuery) ||
                            track.artist.lowercase().contains(lowerQuery)
                }
            )
        }

        adapter.notifyDataSetChanged()
    }



    /**
     * Настройка обработки кнопки "Назад" (современный способ)
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSearchMode) {
                    // В режиме поиска - выходим из него
                    exitSearchMode()
                } else {
                    // Иначе стандартное поведение (выход)
                    isEnabled = false  // Отключаем callback
                    onBackPressedDispatcher.onBackPressed()  // Вызываем стандартную обработку
                }
            }
        })
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
        originalTracks.clear()

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

                val track = Track(id, title, artist, path, duration)
                originalTracks.add(track)  // Сохраняем оригинальный порядок
            }
        }

        // Применяем сортировку
        applySorting()

        //Инициализируем filteredTracks
        filteredTracks.clear()
        filteredTracks.addAll(tracks)

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
        shuffledIndices.shuffle()  // Перемешиваем
    }

    /**
     * Применяет текущий режим сортировки к списку треков
     */
    private fun applySorting() {
        // Сохраняем текущий играющий трек
        val currentTrack = if (currentTrackIndex >= 0 && currentTrackIndex < tracks.size) {
            tracks[currentTrackIndex]
        } else {
            null
        }

        // Очищаем список
        tracks.clear()

        // Применяем сортировку
        val sortedTracks = sortMode.sort(originalTracks)
        tracks.addAll(sortedTracks)

        // Находим новый индекс текущего трека (если он играет)
        if (currentTrack != null) {
            currentTrackIndex = tracks.indexOfFirst { it.id == currentTrack.id }
        }

        // Обновляем список
        adapter.notifyDataSetChanged()

        // filteredTracks тоже обновляем
        if (isSearchMode) {
            val query = editTextSearch.text.toString()
            filterTracks(query)  // Пересортировка влияет на результаты поиска
        } else {
            filteredTracks.clear()
            filteredTracks.addAll(tracks)
            adapter.notifyDataSetChanged()
        }

        // Если включен shuffle - пересоздаем порядок
        if (playMode == PlayMode.SHUFFLE) {
            createShuffleOrder()
        }
    }

    // Обновление UI
    private fun updateUI() {
        if (currentTrackIndex >= 0 && currentTrackIndex < tracks.size) {
            val track = tracks[currentTrackIndex]
            textCurrentTrack.text = "${track.title} - ${track.artist}"
        } else {
            textCurrentTrack.text = "Выберите трек"
        }

        // Меняем иконку кнопки Play/Pause
        val playPauseIcon = if (isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }
        buttonPlay.setCompoundDrawablesWithIntrinsicBounds(0, playPauseIcon, 0, 0)
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


    /**
     * Создание меню в Action Bar
     */
    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * Обработка кликов по элементам меню
     */
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                enterSearchMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



}