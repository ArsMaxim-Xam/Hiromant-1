package com.aistudio.hiromant.kxsrwa.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Дата-класс, представляющий отдельную запись лога в приложении
data class LogEntry(
    val timestamp: Long, // Временная метка в миллисекундах
    val timeStr: String, // Отформатированная строка времени (yyyy-MM-dd HH:mm:ss.SSS)
    val level: String, // Уровень логирования: "I" (Info), "D" (Debug), "W" (Warning), "E" (Error)
    val tag: String, // Тег (источник) лога
    val message: String, // Текст сообщения лога
    val throwable: Throwable? = null // Объект исключения (если имеется ошибка)
)

// Синглтон-объект AppLogger для ведения системного и локального лога приложения
object AppLogger {
    private const val TAG = "AppLogger" // Постоянный тег для внутренних логов самого логгера
    private const val MAX_LOGS = 1000 // Максимальное количество записей в оперативной памяти
    // Форматировщик времени лога (год-месяц-день часы:минуты:секунды.миллисекунды)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    // Форматировщик даты и времени для имени лог-файла (год-месяц-день_часы-минуты-секунды)
    private val logFileNameFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    // Поток списка записей лога для отображения в отладочном UI в реальном времени
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow() // Публичный неизменяемый поток

    // Файл для долговременного сохранения логов в кэш-директории
    private var logFile: File? = null
    // Главный файлы лог-файла «Хиромант-LOG.txt» на накопителе телефона
    private var hiromantLogFile: File? = null
    private var apiRepository: com.aistudio.hiromant.kxsrwa.data.repository.ApiRepository? = null

    // Инициализация логгера (вызывается из PalmistApplication)
    fun init(context: Context) {
        try {
            val cacheDir = context.cacheDir // Директория кэша приложения
            logFile = File(cacheDir, "operations_log.txt") // Файл operations_log.txt
            if (logFile?.exists() == false) {
                logFile?.createNewFile() // Создание нового файла, если его не существует
            }

            // Определение папки накопителя (внутренней/внешней памяти устройства)
            val externalDir = context.getExternalFilesDir(null) ?: context.filesDir
            if (!externalDir.exists()) {
                externalDir.mkdirs() // Создание директории при необходимости
            }
            // Создание основного отладочного файла «Хиромант-LOG_[Дата_Время].txt» с полной датой и временем старта
            val currentDateTimeStr = logFileNameFormat.format(Date()) // Форматирование даты и времени старта
            hiromantLogFile = File(externalDir, "Хиромант-LOG_$currentDateTimeStr.txt")
            if (hiromantLogFile?.exists() == false) {
                hiromantLogFile?.createNewFile()
            }

            apiRepository = com.aistudio.hiromant.kxsrwa.data.repository.ApiRepository(context.applicationContext)
            
            // Запись стартового заголовка сессии в лог-файл
            val startHeader = "========================================\n" +
                    "ЗАПУСК ПРИЛОЖЕНИЯ «ХИРОМАНТ» [${timeFormat.format(Date())}]\n" +
                    "Файл логов: ${hiromantLogFile?.absolutePath}\n" +
                    "Устройство: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})\n" +
                    "========================================\n"
            writeRawToHiromantLog(startHeader)

            log("I", TAG, "Логгер успешно инициализирован. Путь к главному логу: ${hiromantLogFile?.absolutePath}")
            
            // Проверка наличия сохраненных данных о предыдущем падении приложения
            val sharedPrefs = context.getSharedPreferences("palmist_prefs", Context.MODE_PRIVATE)
            val lastCrash = sharedPrefs.getString("last_crash_log", null)
            if (lastCrash != null) {
                // Если краш-лог найден, выводим его как ошибку в логгер
                log("E", "CrashReporter", "Обнаружен отчет о предыдущем падении приложения:\n$lastCrash")
                // Очистка отчета в настройках после его прочтения и записи в логгер
                sharedPrefs.edit().remove("last_crash_log").apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось инициализировать лог-файл", e) // Системный вывод в Logcat
        }
    }

    // Вспомогательная функция прямой записи текста в файл Хиромант-LOG.txt
    private fun writeRawToHiromantLog(text: String) {
        try {
            hiromantLogFile?.let { file ->
                val writer = FileWriter(file, true) // Режим дополнения текста в конец файла
                writer.write(text)
                writer.flush() // Принудительное выталкивание из буфера
                writer.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка записи в файл Хиромант-LOG.txt", e)
        }
    }

    // Синхронизированный метод добавления лога во избежание конфликтов из параллельных потоков
    @Synchronized
    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = System.currentTimeMillis() // Текущее время в миллисекундах
        val timeStr = timeFormat.format(Date(timestamp)) // Преобразование в форматированную строку
        val entry = LogEntry(timestamp, timeStr, level, tag, message, throwable) // Создание объекта лога

        // Дублирование лога в стандартную утилиту Android Logcat
        when (level) {
            "D" -> Log.d(tag, message, throwable)
            "W" -> Log.w(tag, message, throwable)
            "E" -> Log.e(tag, message, throwable)
            else -> Log.i(tag, message, throwable)
        }

        // Добавление записи лога в реактивный список в памяти
        val currentList = _logs.value.toMutableList()
        currentList.add(entry)
        if (currentList.size > MAX_LOGS) {
            currentList.removeAt(0) // Удаление самой старой записи при переполнении лимита
        }
        _logs.value = currentList // Применение обновленного списка логов

        // Запись строки лога в файлы operations_log.txt и Хиромант-LOG.txt
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable?.printStackTrace(pw) // Сброс трассировки стека ошибки в буфер
            val stackTrace = sw.toString() // Преобразование трейса ошибки в строку

            // Форматирование финальной строки лога для текстового файла
            val line = if (stackTrace.isNotEmpty()) {
                "[$timeStr] [$level/$tag] $message\n$stackTrace\n"
            } else {
                "[$timeStr] [$level/$tag] $message\n"
            }

            // Запись в кэш-лог operations_log.txt
            logFile?.let { file ->
                val writer = FileWriter(file, true) // Открытие писателя в режиме добавления в конец
                writer.write(line) // Запись в файл
                writer.flush() // Выталкивание данных из буфера
                writer.close() // Закрытие писателя
            }

            // Запись в основной отладочный лог Хиромант-LOG.txt на накопителе смартфона
            writeRawToHiromantLog(line)
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось записать лог в файл", e) // Вывод ошибки записи в консоль Android
        }

        // Автоматическая фоновая отправка сообщений об ошибках (уровень "E") на удаленный REST API сервер
        if (level == "E" && tag != "ApiRepository" && tag != "FastApiRetrofitClient" && apiRepository != null) {
            val repo = apiRepository
            val sw = StringWriter()
            throwable?.printStackTrace(PrintWriter(sw))
            val trace = sw.toString()
            val errorContent = if (trace.isNotBlank()) "[$tag] $message\n$trace" else "[$tag] $message"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repo?.logError(errorContent)
                } catch (ignored: Exception) {
                    // Игнорируем ошибки при самой попытке логирования
                }
            }
        }
    }

    // Вспомогательные функции для быстрой записи логов различных уровней
    fun d(tag: String, message: String) = log("D", tag, message) // Отладка
    fun i(tag: String, message: String) = log("I", tag, message) // Информирование
    fun w(tag: String, message: String, throwable: Throwable? = null) = log("W", tag, message, throwable) // Предупреждение
    fun e(tag: String, message: String, throwable: Throwable? = null) = log("E", tag, message, throwable) // Ошибка

    // Метод получения полного текста накопленных логов в виде единой отформатированной строки
    fun getLogText(): String {
        return _logs.value.joinToString("\n") { entry ->
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            entry.throwable?.printStackTrace(pw) // Получение стека ошибки для записи
            val stackTrace = sw.toString()
            val traceStr = if (stackTrace.isNotEmpty()) "\n$stackTrace" else ""
            "[${entry.timeStr}] [${entry.level}/${entry.tag}] ${entry.message}$traceStr" // Сборка строки лога
        }
    }

    // Полная очистка накопленных логов в памяти и пересоздание чистого файла лога на диске
    fun clear() {
        _logs.value = emptyList() // Очистка списка в памяти
        try {
            logFile?.let { file ->
                if (file.exists()) {
                    file.delete() // Удаление старого файла лога
                    file.createNewFile() // Создание пустого файла заново
                }
            }
            hiromantLogFile?.let { file ->
                if (file.exists()) {
                    file.delete() // Удаление старого отладочного файла
                    file.createNewFile() // Создание пустого файла заново
                }
            }
            log("I", TAG, "Лог-файлы успешно очищены.")
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось очистить лог-файл", e) // Вывод ошибки в стандартный лог Android
        }
    }

    // Возвращает имя файла лога
    fun getHiromantLogFileName(): String {
        val file = hiromantLogFile
        return if (file != null) {
            file.name
        } else {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            "Хиромант LOG $dateStr.txt"
        }
    }

    // Возвращает полное содержимое файла лога
    fun getHiromantLogContent(): String {
        return try {
            val file = hiromantLogFile
            if (file != null && file.exists() && file.length() > 0) {
                file.readText()
            } else {
                getLogText()
            }
        } catch (e: Exception) {
            getLogText()
        }
    }

    // Возвращает абсолютный путь к созданному текстовому файлу «Хиромант-LOG.txt»
    fun getHiromantLogFilePath(): String {
        return hiromantLogFile?.absolutePath ?: "Файл не найден"
    }

    // Возвращает сам объект файла «Хиромант-LOG.txt»
    fun getHiromantLogFile(): File? {
        return hiromantLogFile
    }
}
