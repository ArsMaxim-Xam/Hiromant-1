package com.aistudio.hiromant.kxsrwa.utils

import android.content.Context
import com.aistudio.hiromant.kxsrwa.data.local.BillingStateEntity
import com.aistudio.hiromant.kxsrwa.data.local.PalmistDao
import com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity
import com.aistudio.hiromant.kxsrwa.data.local.ReadingEntity
import com.aistudio.hiromant.kxsrwa.data.local.TokenUsageEntity
import com.aistudio.hiromant.kxsrwa.data.local.UserProfileEntity
import com.aistudio.hiromant.kxsrwa.data.repository.PalmistRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Результат выполнения операции восстановления из резервной копии
data class RestoreResult(
    val isSuccess: Boolean, // Успешно ли прошло восстановление
    val restoredUsersCount: Int = 0, // Количество восстановленных профилей пользователей
    val restoredReadingsCount: Int = 0, // Количество восстановленных интерпретаций/анализов
    val restoredPaymentsCount: Int = 0, // Количество восстановленных платежей и бонусов
    val restoredTokensCount: Int = 0, // Количество восстановленных логов токенов
    val errorMessage: String? = null // Текст ошибки в случае сбоя
)

// Менеджер создания и восстановления полных бэкапов данных приложения в формате CSV
object BackupManager {

    // Функция эскейпинга значения для корректного сохранения в формат CSV
    private fun escapeCsv(value: Any?): String {
        if (value == null) return "\"\"" // Если значение null, возвращаем пустую кавычку
        val str = value.toString() // Преобразование значения в строку
        val escaped = str.replace("\"", "\"\"") // Замена двойных кавычек на сдвоенные кавычки по стандарту RFC 4180
        return "\"$escaped\"" // Оборачивание значения в двойные кавычки
    }

    // Функция разбора строки CSV с учетом кавычек и разделителей точка с запятой
    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>() // Список распарсенных колонок
        val sb = StringBuilder() // Буфер текущего элемента
        var inQuotes = false // Флаг нахождения внутри кавычек
        var i = 0 // Индекс символа

        while (i < line.length) { // Цикл по каждому символу строки
            val c = line[i] // Текущий символ
            if (c == '"') { // Если встретилась кавычка
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"') // Добавление экранированной кавычки
                    i++ // Пропуск следующей кавычки
                } else {
                    inQuotes = !inQuotes // Переключение состояния внутри кавычек
                }
            } else if (c == ';' && !inQuotes) { // Если разделитель колонок вне кавычек
                tokens.add(sb.toString()) // Добавление колонки в результат
                sb.setLength(0) // Очистка буфера
            } else {
                sb.append(c) // Накопление обычного символа
            }
            i++ // Переход к следующему символу
        }
        tokens.add(sb.toString()) // Добавление последней колонки
        return tokens // Возврат списка распарсенных колонок
    }

    // Вспомогательный метод разбиения CSV-файла на строки с сохранением экранированных переносов строк внутри кавычек
    private fun splitCsvIntoLogicalLines(csvText: String): List<String> {
        val resultLines = mutableListOf<String>() // Список итоговых логических строк
        val sb = StringBuilder() // Буфер текущей строки
        var inQuotes = false // Флаг нахождения внутри кавычек

        for (i in 0 until csvText.length) { // Итерация по всем символам текста CSV
            val c = csvText[i] // Текущий символ
            if (c == '"') { // Если встретилась кавычка
                if (inQuotes && i + 1 < csvText.length && csvText[i + 1] == '"') {
                    sb.append('"') // Экранированная кавычка
                } else {
                    inQuotes = !inQuotes // Переключение флага
                }
                sb.append(c) // Сохранение кавычки
            } else if ((c == '\n' || c == '\r') && !inQuotes) { // Если перевод строки вне кавычек
                if (c == '\r' && i + 1 < csvText.length && csvText[i + 1] == '\n') {
                    // Пропускаем \r перед \n
                } else {
                    val line = sb.toString().trim() // Получение готовой строки
                    if (line.isNotEmpty()) { // Если строка не пустая
                        resultLines.add(line) // Добавление логической строки
                    }
                    sb.setLength(0) // Очистка буфера
                }
            } else {
                sb.append(c) // Накопление символа
            }
        }
        val lastLine = sb.toString().trim() // Завершающая строка
        if (lastLine.isNotEmpty()) { // Если есть остаток
            resultLines.add(lastLine) // Добавление остатка
        }
        return resultLines // Возврат логических строк CSV
    }

    // Формирование дефолтного имени файла бэкапа (Название + Версия + Дата и Время)
    fun generateBackupFileName(): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date()) // Форматирование текущей даты и времени
        return "Hiromant_v1.022_Backup_$dateStr.csv" // Имя файла бэкапа по ТЗ
    }

    // Полный экспорт всех настроек и данных приложения в читаемый CSV формат
    suspend fun createBackupCsv(context: Context, repository: PalmistRepository, dao: PalmistDao): String {
        val sb = StringBuilder() // Буфер для построения CSV файла
        val now = Date() // Текущая дата и время
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(now) // Форматирование даты

        // 1. Метаданные файла бэкапа
        sb.append("# HIROMANT BACKUP FILE\n") // Заголовок файла резервной копии
        sb.append("# APP_NAME;Хиромант\n") // Название приложения
        sb.append("# APP_VERSION;1.022\n") // Версия программы
        sb.append("# CREATED_AT;$dateStr\n") // Дата и время создания бэкапа
        sb.append("# TIMESTAMP;${now.time}\n\n") // Временная метка в миллисекундах

        // 2. Секция Настроек Программы
        sb.append("=== SETTINGS ===\n") // Заголовок секции настроек
        sb.append("Key;Value\n") // Колоночные заголовки параметров настроек
        sb.append("app_language;${escapeCsv(repository.getSelectedLanguage())}\n") // Язык интерфейса
        sb.append("is_language_selected;${escapeCsv(repository.isLanguageSelected())}\n") // Флаг выбора языка
        sb.append("app_font_scale;${escapeCsv(repository.getFontScale())}\n") // Масштаб шрифта
        sb.append("app_tts_enabled;${escapeCsv(repository.getTtsEnabled())}\n") // Статус озвучивания
        sb.append("app_tts_gender;${escapeCsv(repository.getTtsGender())}\n") // Пол голоса озвучки
        sb.append("app_tts_voice_index;${escapeCsv(repository.getTtsVoiceIndex())}\n") // Индекс голоса
        sb.append("app_tts_speech_rate;${escapeCsv(repository.getTtsSpeechRate())}\n") // Скорость озвучки
        sb.append("app_tts_pitch;${escapeCsv(repository.getTtsPitch())}\n") // Высота тона речи

        val billing = dao.getBillingStateSync() // Получение состояния биллинга и баланса из БД
        sb.append("freeAnalyses;${escapeCsv(billing?.freeAnalyses ?: 3)}\n") // Бесплатные интерпретации
        sb.append("paidAnalyses;${escapeCsv(billing?.paidAnalyses ?: 0)}\n") // Платные интерпретации
        sb.append("isPremiumSubscribed;${escapeCsv(billing?.isPremiumSubscribed ?: false)}\n\n") // Режим премиум

        // 3. Секция Пользователей
        val users = dao.getAllUserProfilesSync() // Получение списка всех пользователей
        sb.append("=== USERS ===\n") // Заголовок секции пользователей
        sb.append("id;name;gender;age;height;dominantHand;email;phone;isRegistered;photoUri;registrationTimestamp\n") // Колонки пользователей
        users.forEach { user -> // Итерация по пользователям
            sb.append("${escapeCsv(user.id)};${escapeCsv(user.name)};${escapeCsv(user.gender)};${escapeCsv(user.age)};${escapeCsv(user.height)};${escapeCsv(user.dominantHand)};${escapeCsv(user.email)};${escapeCsv(user.phone)};${escapeCsv(user.isRegistered)};${escapeCsv(user.photoUri)};${escapeCsv(user.registrationTimestamp)}\n") // Запись профиля
        }
        sb.append("\n") // Пустая строка-разделитель

        // 4. Секция Интерпретаций и Анализов (с сохраненными дополнительными вопросами)
        val readings = dao.getAllReadingsSync() // Получение списка всех сохраненных анализов
        sb.append("=== READINGS ===\n") // Заголовок секции интерпретаций
        sb.append("id;timestamp;name;gender;age;height;dominantHand;analysisType;resultJson;imageUrl;partnerName;partnerImageUrl;leftPalmPath;leftBackPath;rightPalmPath;rightBackPath;videoPath;followUpQuestionsJson\n") // Колонки анализов
        readings.forEach { r -> // Итерация по анализам
            sb.append("${escapeCsv(r.id)};${escapeCsv(r.timestamp)};${escapeCsv(r.name)};${escapeCsv(r.gender)};${escapeCsv(r.age)};${escapeCsv(r.height)};${escapeCsv(r.dominantHand)};${escapeCsv(r.analysisType)};${escapeCsv(r.resultJson)};${escapeCsv(r.imageUrl)};${escapeCsv(r.partnerName)};${escapeCsv(r.partnerImageUrl)};${escapeCsv(r.leftPalmPath)};${escapeCsv(r.leftBackPath)};${escapeCsv(r.rightPalmPath)};${escapeCsv(r.rightBackPath)};${escapeCsv(r.videoPath)};${escapeCsv(r.followUpQuestionsJson)}\n") // Запись анализа
        }
        sb.append("\n") // Пустая строка-разделитель

        // 5. Секция Истории Пополнений и Платежей
        val payments = dao.getAllPaymentsSync() // Получение истории платежей
        sb.append("=== PAYMENTS ===\n") // Заголовок секции оплат
        sb.append("id;timestamp;amountRub;paymentSystem;status;readingType;userName;userAge;leftPalmPath;leftBackPath;rightPalmPath;rightBackPath;grantedAnalyses;remainingAnalysesAfterPayment\n") // Колонки оплат
        payments.forEach { p -> // Итерация по платежам
            sb.append("${escapeCsv(p.id)};${escapeCsv(p.timestamp)};${escapeCsv(p.amountRub)};${escapeCsv(p.paymentSystem)};${escapeCsv(p.status)};${escapeCsv(p.readingType)};${escapeCsv(p.userName)};${escapeCsv(p.userAge)};${escapeCsv(p.leftPalmPath)};${escapeCsv(p.leftBackPath)};${escapeCsv(p.rightPalmPath)};${escapeCsv(p.rightBackPath)};${escapeCsv(p.grantedAnalyses)};${escapeCsv(p.remainingAnalysesAfterPayment)}\n") // Запись платежа
        }
        sb.append("\n") // Пустая строка-разделитель

        // 6. Секция Логирования Токенов
        val tokenUsages = dao.getAllTokenUsagesSync() // Получение логов токенов
        sb.append("=== TOKEN_USAGE ===\n") // Заголовок секции расхода токенов
        sb.append("id;timestamp;userName;userEmail;analysisType;promptTokens;candidatesTokens;totalTokens;dateString\n") // Колонки расхода токенов
        tokenUsages.forEach { t -> // Итерация по записям токенов
            sb.append("${escapeCsv(t.id)};${escapeCsv(t.timestamp)};${escapeCsv(t.userName)};${escapeCsv(t.userEmail)};${escapeCsv(t.analysisType)};${escapeCsv(t.promptTokens)};${escapeCsv(t.candidatesTokens)};${escapeCsv(t.totalTokens)};${escapeCsv(t.dateString)}\n") // Запись лога токенов
        }

        return sb.toString() // Возврат сформулированного CSV текста
    }

    // Импорт и полное восстановление всех данных и настроек из CSV текста резервной копии
    suspend fun restoreFromBackupCsv(
        context: Context,
        repository: PalmistRepository,
        dao: PalmistDao,
        csvText: String
    ): RestoreResult {
        return try {
            if (!csvText.contains("=== SETTINGS ===") && !csvText.contains("# HIROMANT BACKUP FILE")) {
                return RestoreResult(isSuccess = false, errorMessage = "Неверный формат файла бэкапа") // Ошибка формата
            }

            val lines = splitCsvIntoLogicalLines(csvText) // Разбиение на логические строки
            var currentSection = "" // Текущая обрабатываемая секция

            var restoredUsers = 0 // Счётчик пользователей
            var restoredReadings = 0 // Счётчик анализов
            var restoredPayments = 0 // Счётчик оплат
            var restoredTokens = 0 // Счётчик токенов

            var freeAnalysesRestored = 3 // Дефолтные бесплатные анализы
            var paidAnalysesRestored = 0 // Дефолтные платные анализы
            var isPremiumRestored = false // Дефолтный премиум

            lines.forEach { line -> // Обход каждой строки CSV
                val trimmed = line.trim() // Обрезка пробелов
                if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                    currentSection = trimmed // Смена секции
                    return@forEach // Переход к следующей строке
                }
                if (trimmed.startsWith("#") || trimmed.isEmpty()) {
                    return@forEach // Пропуск комментариев и пустых строк
                }

                when (currentSection) {
                    "=== SETTINGS ===" -> { // Секция Настроек
                        val parts = parseCsvLine(trimmed) // Разбор пары ключ-значение
                        if (parts.size >= 2) { // Если найдены ключ и значение
                            val key = parts[0].trim() // Ключ настройки
                            val value = parts[1].trim() // Значение настройки
                            when (key) {
                                "app_language" -> if (value.isNotEmpty()) repository.setSelectedLanguage(value) // Восстановление языка
                                "is_language_selected" -> repository.setLanguageSelected(value.toBooleanStrictOrNull() ?: true) // Восстановление флага выбора языка
                                "app_font_scale" -> value.toFloatOrNull()?.let { repository.setFontScale(it) } // Восстановление шрифта
                                "app_tts_enabled" -> value.toBooleanStrictOrNull()?.let { repository.setTtsEnabled(it) } // Восстановление TTS
                                "app_tts_gender" -> if (value.isNotEmpty()) repository.setTtsGender(value) // Восстановление пола голоса
                                "app_tts_voice_index" -> value.toIntOrNull()?.let { repository.setTtsVoiceIndex(it) } // Восстановление индекса голоса
                                "app_tts_speech_rate" -> value.toFloatOrNull()?.let { repository.setTtsSpeechRate(it) } // Восстановление скорости
                                "app_tts_pitch" -> value.toFloatOrNull()?.let { repository.setTtsPitch(it) } // Восстановление тона
                                "freeAnalyses" -> value.toIntOrNull()?.let { freeAnalysesRestored = it } // Бесплатные анализы
                                "paidAnalyses" -> value.toIntOrNull()?.let { paidAnalysesRestored = it } // Платные анализы
                                "isPremiumSubscribed" -> value.toBooleanStrictOrNull()?.let { isPremiumRestored = it } // Премиум
                            }
                        }
                    }

                    "=== USERS ===" -> { // Секция Пользователей
                        if (trimmed.startsWith("id;name;")) return@forEach // Пропуск строки заголовка
                        val cols = parseCsvLine(trimmed) // Разбор колонок
                        if (cols.size >= 9) { // Проверка количества колонок
                            val user = UserProfileEntity(
                                id = cols[0].ifBlank { "current_user" },
                                name = cols[1].ifBlank { "Максим" },
                                gender = cols[2].ifBlank { "Male" },
                                age = cols[3].toIntOrNull() ?: 44,
                                height = cols[4].toIntOrNull() ?: 175,
                                dominantHand = cols[5].ifBlank { "Right" },
                                email = cols[6].ifBlank { null },
                                phone = cols[7].ifBlank { null },
                                isRegistered = cols[8].toBooleanStrictOrNull() ?: false,
                                photoUri = if (cols.size > 9) cols[9].ifBlank { null } else null,
                                registrationTimestamp = if (cols.size > 10) cols[10].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
                            )
                            dao.insertUserProfile(user) // Сохранение пользователя в базу данных
                            restoredUsers++ // Увеличение счетчика
                        }
                    }

                    "=== READINGS ===" -> { // Секция Интерпретаций и Анализов
                        if (trimmed.startsWith("id;timestamp;")) return@forEach // Пропуск строки заголовка
                        val cols = parseCsvLine(trimmed) // Разбор колонок
                        if (cols.size >= 9) { // Проверка структуры
                            val reading = ReadingEntity(
                                id = cols[0].toLongOrNull() ?: 0L,
                                timestamp = cols[1].toLongOrNull() ?: System.currentTimeMillis(),
                                name = cols[2],
                                gender = cols[3],
                                age = cols[4].toIntOrNull() ?: 18,
                                height = cols[5].toIntOrNull() ?: 170,
                                dominantHand = cols[6],
                                analysisType = cols[7],
                                resultJson = cols[8],
                                imageUrl = if (cols.size > 9) cols[9].ifBlank { null } else null,
                                partnerName = if (cols.size > 10) cols[10].ifBlank { null } else null,
                                partnerImageUrl = if (cols.size > 11) cols[11].ifBlank { null } else null,
                                leftPalmPath = if (cols.size > 12) cols[12].ifBlank { null } else null,
                                leftBackPath = if (cols.size > 13) cols[13].ifBlank { null } else null,
                                rightPalmPath = if (cols.size > 14) cols[14].ifBlank { null } else null,
                                rightBackPath = if (cols.size > 15) cols[15].ifBlank { null } else null,
                                videoPath = if (cols.size > 16) cols[16].ifBlank { null } else null,
                                followUpQuestionsJson = if (cols.size > 17) cols[17].ifBlank { null } else null
                            )
                            dao.insertReading(reading) // Сохранение анализа в БД
                            restoredReadings++ // Увеличение счетчика
                        }
                    }

                    "=== PAYMENTS ===" -> { // Секция Платежей
                        if (trimmed.startsWith("id;timestamp;")) return@forEach // Пропуск строки заголовка
                        val cols = parseCsvLine(trimmed) // Разбор колонок
                        if (cols.size >= 8) { // Проверка полей
                            val payment = PaymentHistoryEntity(
                                id = cols[0].toLongOrNull() ?: 0L,
                                timestamp = cols[1].toLongOrNull() ?: System.currentTimeMillis(),
                                amountRub = cols[2].toIntOrNull() ?: 0,
                                paymentSystem = cols[3],
                                status = cols[4],
                                readingType = cols[5],
                                userName = cols[6],
                                userAge = cols[7].toIntOrNull() ?: 18,
                                leftPalmPath = if (cols.size > 8) cols[8].ifBlank { null } else null,
                                leftBackPath = if (cols.size > 9) cols[9].ifBlank { null } else null,
                                rightPalmPath = if (cols.size > 10) cols[10].ifBlank { null } else null,
                                rightBackPath = if (cols.size > 11) cols[11].ifBlank { null } else null,
                                grantedAnalyses = if (cols.size > 12) cols[12].toIntOrNull() ?: 0 else 0,
                                remainingAnalysesAfterPayment = if (cols.size > 13) cols[13].toIntOrNull() ?: 0 else 0
                            )
                            dao.insertPayment(payment) // Запись платежа в БД
                            restoredPayments++ // Увеличение счетчика
                        }
                    }

                    "=== TOKEN_USAGE ===" -> { // Секция расхода токенов
                        if (trimmed.startsWith("id;timestamp;")) return@forEach // Пропуск строки заголовка
                        val cols = parseCsvLine(trimmed) // Разбор колонок
                        if (cols.size >= 9) { // Проверка структуры
                            val tokenUsage = TokenUsageEntity(
                                id = cols[0].toLongOrNull() ?: 0L,
                                timestamp = cols[1].toLongOrNull() ?: System.currentTimeMillis(),
                                userName = cols[2],
                                userEmail = cols[3].ifBlank { null },
                                analysisType = cols[4],
                                promptTokens = cols[5].toIntOrNull() ?: 0,
                                candidatesTokens = cols[6].toIntOrNull() ?: 0,
                                totalTokens = cols[7].toIntOrNull() ?: 0,
                                dateString = cols[8]
                            )
                            dao.insertTokenUsage(tokenUsage) // Запись использования токенов в БД
                            restoredTokens++ // Увеличение счетчика
                        }
                    }
                }
            }

            // Обновление состояния баланса и подписки после успешного применения
            val updatedBilling = BillingStateEntity(
                id = 1,
                freeAnalyses = freeAnalysesRestored,
                paidAnalyses = paidAnalysesRestored,
                remainingAnalyses = freeAnalysesRestored + paidAnalysesRestored,
                isPremiumSubscribed = isPremiumRestored
            )
            dao.insertBillingState(updatedBilling) // Применение состояния оплат

            RestoreResult(
                isSuccess = true,
                restoredUsersCount = restoredUsers,
                restoredReadingsCount = restoredReadings,
                restoredPaymentsCount = restoredPayments,
                restoredTokensCount = restoredTokens
            )
        } catch (e: Exception) {
            RestoreResult(isSuccess = false, errorMessage = e.localizedMessage ?: "Ошибка обработки файла резервной копии") // Возврат ошибки
        }
    }
}
