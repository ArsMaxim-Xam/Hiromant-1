package com.aistudio.hiromant.kxsrwa.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

// Объект-утилита для работы с растровыми изображениями (Bitmap), кодированием и генерацией заглушек ладоней
object BitmapUtils {
    
    // Расширение для класса Bitmap: преобразует картинку в строку формата Base64 для передачи на сервер/API
    fun Bitmap.toBase64(quality: Int = 70): String {
        val outputStream = ByteArrayOutputStream() // Создание выходного буфера байт
        this.compress(Bitmap.CompressFormat.JPEG, quality, outputStream) // Сжатие изображения в JPEG с заданным качеством
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP) // Кодирование массива байт в строку Base64 без переносов
    }

    // Преобразует Uri-ссылку в полноценный объект Bitmap с помощью контент-провайдера устройства
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri) // Открытие потока ввода по Uri
            BitmapFactory.decodeStream(inputStream) // Декодирование потока в растровое изображение
        } catch (e: Exception) {
            e.printStackTrace() // Логирование ошибок в консоль
            null // Возврат null в случае неудачи
        }
    }

    // Генерирует красивое мистическое изображение ладони (вместо фото пользователя при работе в оффлайне или для тестов)
    fun generateMysticHandBitmap(context: Context, slot: String, isRussian: Boolean): Bitmap {
        val width = 1024 // Ширина генерируемого изображения
        val height = 1024 // Высота генерируемого изображения
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) // Создание пустого Bitmap с прозрачностью
        val canvas = android.graphics.Canvas(bitmap) // Инициализация холста рисования на Bitmap
        
        // Настройка кисти для фона (Мистический космический градиент темно-синего и фиолетового цветов)
        val bgPaint = android.graphics.Paint().apply {
            isAntiAlias = true // Включение сглаживания
            style = android.graphics.Paint.Style.FILL // Стиль заливки
        }
        val gradient = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            android.graphics.Color.parseColor("#0F0C1B"), // Верхний цвет градиента (космическая ночь)
            android.graphics.Color.parseColor("#1C1635"), // Нижний цвет градиента (звездные сумерки)
            android.graphics.Shader.TileMode.CLAMP
        )
        bgPaint.shader = gradient // Установка градиента в качестве шейдера кисти
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint) // Заливка всего холста фоном
        
        // Рисование размытого золотого свечения ауры в центре ладони
        val auraPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            color = android.graphics.Color.parseColor("#44D4AF37") // Полупрозрачный золотой цвет
            maskFilter = android.graphics.BlurMaskFilter(150f, android.graphics.BlurMaskFilter.Blur.NORMAL) // Эффект размытия краев
        }
        canvas.drawCircle(width / 2f, height / 2f, 250f, auraPaint) // Отрисовка круга ауры
        
        // Рисование золотого мистического кольца (внешней рамки)
        val framePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE // Рисование только контура
            strokeWidth = 3f // Толщина линии контура
            color = android.graphics.Color.parseColor("#D4AF37") // Золотой цвет
        }
        canvas.drawCircle(width / 2f, height / 2f, 480f, framePaint) // Внешнее тонкое кольцо
        
        framePaint.strokeWidth = 1f // Сужение кисти для дополнительного внутреннего кольца
        canvas.drawCircle(width / 2f, height / 2f, 465f, framePaint) // Внутреннее золотое кольцо
        
        // Настройка кисти для отрисовки звезд на заднем плане
        val starPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            color = android.graphics.Color.parseColor("#FFFFFF") // Белый цвет звезд
        }
        val random = java.util.Random(slot.hashCode().toLong()) // Инициализация генератора случайных чисел сидом слота
        for (i in 0..40) {
            val sx = random.nextFloat() * width // Случайная координата X
            val sy = random.nextFloat() * height // Случайная координата Y
            val r = 1f + random.nextFloat() * 3f // Случайный радиус звезды (от 1 до 4 пикселей)
            starPaint.alpha = 100 + random.nextInt(155) // Случайная прозрачность мерцания звезд
            canvas.drawCircle(sx, sy, r, starPaint) // Отрисовка звезды
        }
        
        // Конструирование векторного пути золотой ладони
        val handPath = android.graphics.Path()
        val cx = width / 2f
        val cy = height / 2f + 50f // Смещение центра руки чуть ниже центра изображения
        
        // Координаты построения элегантного контура ладони и пальцев
        handPath.moveTo(cx - 150f, cy + 200f) // Начало у запястья слева
        handPath.lineTo(cx - 180f, cy + 50f)  // Основание большого пальца
        handPath.lineTo(cx - 260f, cy + 10f)  // Кончик большого пальца
        handPath.lineTo(cx - 210f, cy - 30f)  // Внутренний сустав большого пальца
        
        // Указательный палец
        handPath.lineTo(cx - 160f, cy - 80f)
        handPath.lineTo(cx - 150f, cy - 320f) // Кончик указательного
        handPath.lineTo(cx - 80f, cy - 320f)
        handPath.lineTo(cx - 70f, cy - 80f)
        
        // Средний палец
        handPath.lineTo(cx - 60f, cy - 100f)
        handPath.lineTo(cx - 50f, cy - 360f) // Кончик среднего
        handPath.lineTo(cx + 20f, cy - 360f)
        handPath.lineTo(cx + 30f, cy - 100f)
        
        // Безымянный палец
        handPath.lineTo(cx + 40f, cy - 90f)
        handPath.lineTo(cx + 50f, cy - 330f) // Кончик безымянного
        handPath.lineTo(cx + 120f, cy - 330f)
        handPath.lineTo(cx + 130f, cy - 90f)
        
        // Мизинец
        handPath.lineTo(cx + 140f, cy - 60f)
        handPath.lineTo(cx + 150f, cy - 260f) // Кончик мизинца
        handPath.lineTo(cx + 210f, cy - 260f)
        handPath.lineTo(cx + 200f, cy - 20f)
        
        // Внешнее ребро ладони
        handPath.lineTo(cx + 220f, cy + 150f)
        handPath.lineTo(cx + 150f, cy + 200f) // Запястье справа
        handPath.close() // Замыкание векторной фигуры
        
        // Кисть для отрисовки контура ладони
        val handPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6f // Жирный золотой контур
            color = android.graphics.Color.parseColor("#D4AF37")
            strokeJoin = android.graphics.Paint.Join.ROUND // Округлые углы соединений
            strokeCap = android.graphics.Paint.Cap.ROUND // Округлые концы линий
        }
        canvas.drawPath(handPath, handPaint) // Отрисовка контура руки
        
        // Мягкая золотая полупрозрачная заливка внутренней части ладони
        val fillPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            color = android.graphics.Color.parseColor("#15D4AF37") // Золото с высокой прозрачностью
        }
        canvas.drawPath(handPath, fillPaint) // Заливка ладони
        
        // Кисть для отрисовки ключевых линий ладони (Жизни, Головы, Сердца, Судьбы)
        val linesPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f // Тонкие линии ладони
            color = android.graphics.Color.parseColor("#E5C158") // Золотисто-желтый цвет
            strokeCap = android.graphics.Paint.Cap.ROUND
        }
        
        // Линия Сердца (верхняя горизонтальная изогнутая линия под пальцами)
        val heartPath = android.graphics.Path().apply {
            moveTo(cx + 160f, cy + 20f)
            quadTo(cx, cy - 20f, cx - 110f, cy - 50f)
        }
        canvas.drawPath(heartPath, linesPaint) // Рисование Линии Сердца
        
        // Линия Головы/Ума (средняя диагональная линия поперек ладони)
        val headPath = android.graphics.Path().apply {
            moveTo(cx - 130f, cy + 40f)
            quadTo(cx, cy + 60f, cx + 150f, cy + 100f)
        }
        canvas.drawPath(headPath, linesPaint) // Рисование Линии Ума
        
        // Линия Жизни (дуговая линия, огибающая холм Венеры вокруг большого пальца)
        val lifePath = android.graphics.Path().apply {
            moveTo(cx - 130f, cy + 40f)
            quadTo(cx - 50f, cy + 100f, cx - 100f, cy + 190f)
        }
        canvas.drawPath(lifePath, linesPaint) // Рисование Линии Жизни
        
        // Линия Судьбы (вертикальная линия, поднимающаяся от запястья к среднему пальцу)
        val destinyPath = android.graphics.Path().apply {
            moveTo(cx + 10f, cy + 190f)
            lineTo(cx - 20f, cy - 10f)
        }
        linesPaint.color = android.graphics.Color.parseColor("#C5A028") // Тёмно-золотой цвет для контраста линии Судьбы
        canvas.drawPath(destinyPath, linesPaint) // Рисование Линии Судьбы
        
        // Кисть для надписей планетарных холмов на ладони
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#E5C158")
            textSize = 28f // Размер шрифта названий холмов
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD_ITALIC) // Шрифт с засечками, курсивный полужирный
        }
        
        // Размещение названий холмов согласно правилам западной хиромантии
        canvas.drawText(" Юпитер ", cx - 110f, cy - 100f, textPaint) // Холм Юпитера под указательным пальцем
        canvas.drawText(" Сатурн ", cx - 35f, cy - 120f, textPaint) // Холм Сатурна под средним пальцем
        canvas.drawText(" Солнце ", cx + 55f, cy - 110f, textPaint) // Холм Солнца/Аполлона под безымянным пальцем
        canvas.drawText(" Меркурий ", cx + 140f, cy - 80f, textPaint) // Холм Меркурия под мизинцем
        canvas.drawText(" Венера ", cx - 120f, cy + 120f, textPaint) // Холм Венеры внутри дуги Линии Жизни
        canvas.drawText(" Луна ", cx + 140f, cy + 130f, textPaint) // Холм Луны внизу внешней стороны ладони
        
        // Определение красивого заголовка ладони в зависимости от выбранного слота
        val titleText = when (slot) {
            "left_palm" -> if (isRussian) "ЛЕВАЯ ЛАДОНЬ (ПАССИВНАЯ)" else "LEFT PALM (INHERITED)"
            "left_back" -> if (isRussian) "ЛЕВАЯ ТЫЛЬНАЯ СТОРОНА" else "LEFT BACK (PROTECTION)"
            "right_palm" -> if (isRussian) "ПРАВАЯ ЛАДОНЬ (АКТИВНАЯ)" else "RIGHT PALM (REALIZED)"
            "right_back" -> if (isRussian) "ПРАВАЯ ТЫЛЬНАЯ СТОРОНА" else "RIGHT BACK (EXPRESSION)"
            else -> if (isRussian) "СВЯЩЕННАЯ ЛАДОНЬ" else "SACRED PALM"
        }
        
        // Настройка кисти для главного заголовка карты руки
        val titlePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FFFFFF") // Белый цвет заголовка
            textSize = 36f // Размер текста
            textAlign = android.graphics.Paint.Align.CENTER
            letterSpacing = 0.1f // Межбуквенный интервал
            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD) // Жирный шрифт с засечками
        }
        canvas.drawText(titleText, cx, 150f, titlePaint) // Отрисовка заголовка вверху холста
        
        // Выбор и отрисовка древней философской цитаты хиромантов внизу изображения
        val quoteText = if (isRussian) {
            "«Что начертано звёздами — отражено на твоей руке»"
        } else {
            "\"What is written in the stars is reflected on your hand\""
        }
        val quotePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#8E80B5") // Космический фиолетово-серый цвет для цитаты
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.ITALIC) // Курсив
        }
        canvas.drawText(quoteText, cx, 920f, quotePaint) // Печать цитаты внизу изображения
        
        return bitmap // Возвращаем сгенерированное растровое изображение
    }

    /**
     * Сохраняет созданное изображение (Bitmap) в галерею устройства с использованием MediaStore API.
     * Совместимо со всеми версиями Android (включая Android 10+ без запроса разрешений WRITE_EXTERNAL_STORAGE).
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
        val resolver = context.contentResolver // Контент-резолвер для записи данных в галерею
        // Определение коллекции изображений в зависимости от версии Android
        val imageCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        // Конфигурирование параметров сохраняемого файла картинки
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$title-${System.currentTimeMillis()}.jpg") // Имя файла
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg") // Формат сжатия JPEG
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1) // Временный флаг блокировки во время записи
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Palmist") // Папка сохранения
            }
        }

        // Вставка новой записи в галерею устройства
        val imageUri = resolver.insert(imageCollection, contentValues) ?: return false

        return try {
            // Открытие потока вывода в созданную запись галереи
            resolver.openOutputStream(imageUri)?.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream) // Сохранение картинки со 100% качеством
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0) // Снятие флага блокировки после записи
                resolver.update(imageUri, contentValues, null, null) // Обновление записи в БД MediaStore
            }
            true // Успешное сохранение в галерею
        } catch (e: Exception) {
            e.printStackTrace() // Запись ошибки при сбое сохранения
            try {
                resolver.delete(imageUri, null, null) // Очистка неудачной записи из галереи
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            false // Возврат ошибки сохранения
        }
    }

    /**
     * Сохраняет изображение в галерею и возвращает его Uri в системе Android для хранения в локальной базе данных.
     * Каждая строка снабжена подробным комментарием на русском языке в строгом соответствии с запросом.
     */
    fun saveBitmapToGalleryAndGetUri(context: Context, bitmap: Bitmap, title: String): Uri? {
        // Получаем объект ContentResolver для безопасного взаимодействия с системным провайдером медиафайлов Android
        val resolver = context.contentResolver
        // Определяем системную коллекцию для сохранения изображений в зависимости от текущей версии операционной системы Android (с поддержкой Scoped Storage)
        val imageCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Для Android 10 (API 29) и новее используем современный URI для основного тома внешнего хранилища
            android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            // Для более старых версий Android используем классический стандартный URI внешнего хранилища
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        // Создаем контейнер ContentValues для передачи метаданных сохраняемой фотографии в базу данных MediaStore
        val contentValues = android.content.ContentValues().apply {
            // Формируем уникальное имя файла изображения, используя переданный заголовок и текущее системное время в миллисекундах
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$title-${System.currentTimeMillis()}.jpg")
            // Указываем стандартный MIME-тип файла, соответствующий формату изображений JPEG
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // Если версия Android 10 (Q) или выше, применяем современные параметры Scoped Storage
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Устанавливаем флаг IS_PENDING в значение 1, чтобы другие приложения не видели файл, пока мы не завершим его запись
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                // Задаем относительный путь сохранения в стандартную папку Pictures во вложенный каталог под названием Palmist
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Palmist")
            }
        }

        // Выполняем вставку записи в базу данных медиафайлов Android и получаем уникальный системный URI для записи
        val imageUri = resolver.insert(imageCollection, contentValues) ?: return null

        // Оборачиваем процесс записи в блок try-catch для безопасной обработки возможных ошибок ввода-вывода (I/O Exception)
        return try {
            // Открываем выходной поток данных для записи байтов изображения по созданному системному URI
            resolver.openOutputStream(imageUri)?.use { outStream ->
                // Сжимаем растровое изображение в формат JPEG со 100% качеством и записываем его в поток вывода
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            }
            // Проверяем, требуется ли обновить статус файла в версиях Android 10 (Q) и выше
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Очищаем предыдущие значения в контейнере метаданных для повторного использования
                contentValues.clear()
                // Снимаем статус ожидания (IS_PENDING = 0), сообщая системе, что запись файла успешно завершена и он готов для общего доступа
                contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                // Обновляем параметры файла в базе данных MediaStore по его системному URI
                resolver.update(imageUri, contentValues, null, null)
            }
            // Возвращаем успешно созданный и сохраненный системный Uri файла в галерее устройства
            imageUri
        } catch (e: Exception) {
            // В случае возникновения исключения выводим полный стек ошибки в логгер для облегчения отладки
            e.printStackTrace()
            // Пытаемся безопасно удалить некорректно созданную пустую запись из медиа-хранилища во избежание захламления памяти
            try {
                // Удаляем запись по её Uri, если в процессе записи возникла ошибка
                resolver.delete(imageUri, null, null)
            } catch (ex: Exception) {
                // Логируем возможные дополнительные исключения при удалении поврежденной записи
                ex.printStackTrace()
            }
            // Возвращаем null, сигнализируя о том, что процесс сохранения завершился неудачно
            null
        }
    }

    /**
     * Копирует поток данных по переданному Uri во внутреннее хранилище приложения и возвращает абсолютный путь к созданному локальному файлу.
     * Позволяет сохранить аватар пользователя постоянным файлом, который не теряет доступ после перезапуска приложения.
     */
    fun saveUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
        // Оборачиваем процесс чтение и записи в блок try-catch для предотвращения падения приложения при ошибках ввода-вывода
        return try {
            // Открываем входной поток данных по Uri через системный ContentResolver
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            // Создаем объект локального файла в закрытом каталоге файлов приложения
            val file = java.io.File(context.filesDir, fileName)
            // Инициализируем поток записи в локальный файл с автоматическим закрытием потока по завершении
            file.outputStream().use { output ->
                // Копируем все байты из входного потока в локальный файл
                inputStream.copyTo(output)
            }
            // Возвращаем абсолютный путь к созданному локальному файлу на диске
            file.absolutePath
        } catch (e: Exception) {
            // Выводим информацию об ошибке в консоль логов
            e.printStackTrace()
            // Возвращаем null в случае неудачи сохранения файла
            null
        }
    }
}
