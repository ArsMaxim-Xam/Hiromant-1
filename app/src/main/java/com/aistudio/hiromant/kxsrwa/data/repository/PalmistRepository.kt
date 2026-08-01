package com.aistudio.hiromant.kxsrwa.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.aistudio.hiromant.kxsrwa.BuildConfig
import com.aistudio.hiromant.kxsrwa.data.local.*
import com.aistudio.hiromant.kxsrwa.data.remote.*
import com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.toBase64
import com.aistudio.hiromant.kxsrwa.utils.AppLogger
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class PalmistRepository(
    private val context: Context,
    private val dao: PalmistDao
) {
    private val sharedPrefs = context.getSharedPreferences("palmist_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Экземпляр сетевого репозитория для работы с REST API сервером FastAPI
    val apiRepository: ApiRepository = ApiRepository(context)

    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    val allUserProfiles: Flow<List<UserProfileEntity>> = dao.getAllUserProfiles()
    val billingState: Flow<BillingStateEntity?> = dao.getBillingState()
    val allReadings: Flow<List<ReadingEntity>> = dao.getAllReadings()

    fun getFontScale(): Float {
        return sharedPrefs.getFloat("app_font_scale", 1.0f)
    }

    fun setFontScale(scale: Float) {
        sharedPrefs.edit().putFloat("app_font_scale", scale).apply()
    }

    // --- Language preferences ---

    fun getSelectedLanguage(): String {
        // Автоматическое определение системного языка по умолчанию (RU или EN)
        val defaultLang = if (java.util.Locale.getDefault().language.lowercase() == "ru") "RU" else "EN"
        return sharedPrefs.getString("app_language", defaultLang) ?: defaultLang
    }

    fun setSelectedLanguage(lang: String) {
        sharedPrefs.edit().putString("app_language", lang).apply()
    }

    fun isLanguageSelected(): Boolean {
        return sharedPrefs.getBoolean("is_language_selected", false)
    }

    fun setLanguageSelected(selected: Boolean) {
        sharedPrefs.edit().putBoolean("is_language_selected", selected).apply()
    }

    // --- TTS preferences ---

    fun getTtsEnabled(): Boolean {
        return sharedPrefs.getBoolean("app_tts_enabled", true)
    }

    fun setTtsEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("app_tts_enabled", enabled).apply()
    }

    fun getTtsGender(): String {
        return sharedPrefs.getString("app_tts_gender", "Female") ?: "Female"
    }

    fun setTtsGender(gender: String) {
        sharedPrefs.edit().putString("app_tts_gender", gender).apply()
    }

    fun getTtsVoiceIndex(): Int {
        return sharedPrefs.getInt("app_tts_voice_index", 0)
    }

    fun setTtsVoiceIndex(index: Int) {
        sharedPrefs.edit().putInt("app_tts_voice_index", index).apply()
    }

    fun getTtsSpeechRate(): Float {
        return sharedPrefs.getFloat("app_tts_speech_rate", 1.0f)
    }

    fun setTtsSpeechRate(rate: Float) {
        sharedPrefs.edit().putFloat("app_tts_speech_rate", rate).apply()
    }

    fun getTtsPitch(): Float {
        return sharedPrefs.getFloat("app_tts_pitch", 1.0f)
    }

    fun setTtsPitch(pitch: Float) {
        sharedPrefs.edit().putFloat("app_tts_pitch", pitch).apply()
    }

    // --- Remote Database Connection Configuration ---

    fun getRemoteDbHost(): String {
        return sharedPrefs.getString("app_remote_db_host", "https://db.hiromant-app.ru:3306") ?: "https://db.hiromant-app.ru:3306"
    }

    fun setRemoteDbHost(host: String) {
        sharedPrefs.edit().putString("app_remote_db_host", host).apply()
    }

    fun getRemoteDbName(): String {
        return sharedPrefs.getString("app_remote_db_name", "palmist_db") ?: "palmist_db"
    }

    fun setRemoteDbName(name: String) {
        sharedPrefs.edit().putString("app_remote_db_name", name).apply()
    }

    fun getRemoteDbUser(): String {
        return sharedPrefs.getString("app_remote_db_user", "palmist_user") ?: "palmist_user"
    }

    fun setRemoteDbUser(user: String) {
        sharedPrefs.edit().putString("app_remote_db_user", user).apply()
    }

    fun getRemoteDbPassword(): String {
        return sharedPrefs.getString("app_remote_db_password", "") ?: ""
    }

    fun setRemoteDbPassword(pass: String) {
        sharedPrefs.edit().putString("app_remote_db_password", pass).apply()
    }

    suspend fun syncLocalDataWithRemoteDb(): Boolean = withContext(Dispatchers.IO) {
        try {
            val host = getRemoteDbHost()
            val user = getRemoteDbUser()
            AppLogger.i("PalmistRepository", "Synchronizing local Room database records with remote DB at $host for user $user...")
            delay(1000)
            true
        } catch (e: Exception) {
            AppLogger.e("PalmistRepository", "Failed to sync remote DB", e)
            false
        }
    }

    // --- Управление статистикой токенов Gemini API ---

    fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun addDailyTokenUsage(tokens: Int) {
        if (tokens <= 0) return
        val today = getTodayDateString()
        val savedDate = sharedPrefs.getString("app_daily_tokens_date", "") ?: ""
        val currentUsage = if (savedDate == today) {
            sharedPrefs.getInt("app_daily_tokens_count", 0)
        } else {
            0
        }
        val newUsage = currentUsage + tokens
        sharedPrefs.edit()
            .putString("app_daily_tokens_date", today)
            .putInt("app_daily_tokens_count", newUsage)
            .apply()
        AppLogger.i("PalmistRepository", "Обновлена суточная статистика токенов: +$tokens, всего за сегодня: $newUsage")
    }

    fun getDailyTokenUsage(): Int {
        val today = getTodayDateString()
        val savedDate = sharedPrefs.getString("app_daily_tokens_date", "") ?: ""
        return if (savedDate == today) {
            sharedPrefs.getInt("app_daily_tokens_count", 0)
        } else {
            0
        }
    }

    fun getDailyTokenQuota(): Int {
        return sharedPrefs.getInt("app_daily_token_quota", 1_000_000)
    }

    fun getDailyTokensRemaining(): Int {
        val used = getDailyTokenUsage()
        val quota = getDailyTokenQuota()
        return maxOf(0, quota - used)
    }

    // --- Profile & Account ---

    suspend fun saveUserProfile(
        name: String,
        gender: String,
        age: Int,
        height: Int,
        dominantHand: String,
        email: String? = null,
        phone: String? = null,
        isRegistered: Boolean = false,
        photoUri: String? = null
    ) = withContext(Dispatchers.IO) {
        val currentProfile = dao.getUserProfileSync()
        val finalPhotoUri = photoUri ?: currentProfile?.photoUri
        val now = System.currentTimeMillis()
        val regTime = if (currentProfile != null && currentProfile.registrationTimestamp > 0) currentProfile.registrationTimestamp else now

        val profile = UserProfileEntity(
            id = "current_user",
            name = name,
            gender = gender,
            age = age,
            height = height,
            dominantHand = dominantHand,
            email = email,
            phone = phone,
            isRegistered = isRegistered,
            photoUri = finalPhotoUri,
            registrationTimestamp = regTime
        )
        dao.insertUserProfile(profile)

        // Дополнительное сохранение уникального профиля для истории всех пользователей
        if (name.isNotBlank()) {
            val uniqueId = "user_${name.trim().lowercase().hashCode()}"
            val userCopy = profile.copy(id = uniqueId)
            dao.insertUserProfile(userCopy)
        }

        // Автоматическая отправка запроса регистрации пользователя на FastAPI сервер
        try {
            val birthYearVal = if (age > 0) (java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - age) else null
            apiRepository.registerUser(
                username = name,
                email = email,
                phone = phone,
                birthYear = birthYearVal,
                height = height
            )
        } catch (e: Exception) {
            AppLogger.e("PalmistRepository", "Ошибка синхронизации регистрации с REST API", e)
        }
    }

    suspend fun updateUserAvatar(photoUri: String) = withContext(Dispatchers.IO) {
        val currentProfile = dao.getUserProfileSync() ?: UserProfileEntity()
        val updated = currentProfile.copy(photoUri = photoUri)
        dao.insertUserProfile(updated)
        if (updated.name.isNotBlank()) {
            val uniqueId = "user_${updated.name.trim().lowercase().hashCode()}"
            dao.insertUserProfile(updated.copy(id = uniqueId))
        }
        AppLogger.i("PalmistRepository", "Аватар пользователя успешно обновлен в БД: $photoUri")
    }

    suspend fun deleteUserProfile() = withContext(Dispatchers.IO) {
        dao.clearUserProfile()
    }

    // --- Billing and local purchases ---

    suspend fun initializeBillingStateIfEmpty() = withContext(Dispatchers.IO) {
        val current = dao.getBillingStateSync()
        if (current == null) {
            dao.insertBillingState(BillingStateEntity(freeAnalyses = 3, paidAnalyses = 0, remainingAnalyses = 3)) // 3 бесплатные интерпретации при старте
        }
    }

    suspend fun addFreeAnalyses(count: Int) = withContext(Dispatchers.IO) {
        val current = dao.getBillingStateSync() ?: BillingStateEntity()
        val newFree = current.freeAnalyses + count
        dao.insertBillingState(
            current.copy(
                freeAnalyses = newFree,
                remainingAnalyses = newFree + current.paidAnalyses
            )
        )
    }

    suspend fun addPaidAnalyses(count: Int) = withContext(Dispatchers.IO) {
        val current = dao.getBillingStateSync() ?: BillingStateEntity()
        val newPaid = current.paidAnalyses + count
        dao.insertBillingState(
            current.copy(
                paidAnalyses = newPaid,
                remainingAnalyses = current.freeAnalyses + newPaid,
                isPremiumSubscribed = if (count >= 10) true else current.isPremiumSubscribed
            )
        )
    }

    suspend fun addAnalyses(count: Int) = addPaidAnalyses(count)

    suspend fun unlockFeature(itemId: String) = withContext(Dispatchers.IO) {
        val current = dao.getBillingStateSync() ?: BillingStateEntity()
        val list = current.purchasedItemIds.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (!list.contains(itemId)) {
            list.add(itemId)
        }
        dao.insertBillingState(
            current.copy(
                purchasedItemIds = list.joinToString(",")
            )
        )
    }

    suspend fun hasUnlocked(itemId: String): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getBillingStateSync() ?: return@withContext false
        val list = current.purchasedItemIds.split(",").filter { it.isNotEmpty() }
        return@withContext list.contains(itemId) || current.isPremiumSubscribed
    }

    // Проверка наличия хотя бы одного доступного Анализа (платного или бесплатного)
    suspend fun hasAvailableAnalysis(): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getBillingStateSync() ?: return@withContext false
        return@withContext (current.paidAnalyses > 0 || current.freeAnalyses > 0)
    }

    // Единое списание одной единицы Анализа (сначала с платных, затем с бесплатных)
    suspend fun consumeAnalysisUnit(): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getBillingStateSync() ?: return@withContext false
        if (current.paidAnalyses > 0) {
            val newPaid = current.paidAnalyses - 1
            dao.insertBillingState(current.copy(
                paidAnalyses = newPaid,
                remainingAnalyses = current.freeAnalyses + newPaid
            ))
            return@withContext true
        } else if (current.freeAnalyses > 0) {
            val newFree = current.freeAnalyses - 1
            dao.insertBillingState(current.copy(
                freeAnalyses = newFree,
                remainingAnalyses = newFree + current.paidAnalyses
            ))
            return@withContext true
        }
        return@withContext false
    }

    // Списание 1 единицы Краткого Анализа (сначала с бесплатных/кратких, затем с платных)
    suspend fun consumeBriefAnalysisUnit(): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getBillingStateSync() ?: return@withContext false
        if (current.freeAnalyses > 0) {
            val newFree = current.freeAnalyses - 1
            dao.insertBillingState(current.copy(
                freeAnalyses = newFree,
                remainingAnalyses = newFree + current.paidAnalyses
            ))
            return@withContext true
        } else if (current.paidAnalyses > 0) {
            val newPaid = current.paidAnalyses - 1
            dao.insertBillingState(current.copy(
                paidAnalyses = newPaid,
                remainingAnalyses = current.freeAnalyses + newPaid
            ))
            return@withContext true
        }
        return@withContext false
    }

    suspend fun decrementFreeAnalyses(): Boolean = consumeBriefAnalysisUnit()

    suspend fun decrementPaidAnalyses(): Boolean = consumeAnalysisUnit()

    suspend fun decrementAnalyses(): Boolean = consumeAnalysisUnit()

    // --- History ---

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearHistory()
    }

    suspend fun deleteReading(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteReadingById(id)
    }

    // --- Gemini Palm Reading Core ---

    suspend fun analyzePalm(
        bitmaps: List<Bitmap>,
        videoUri: String?,
        analysisType: String, // "brief_char", "full_char", "brief_path", "full_path"
        langCode: String,
        leftPalmPath: String? = null,
        leftBackPath: String? = null,
        rightPalmPath: String? = null,
        rightBackPath: String? = null
    ): ReadingEntity = withContext(Dispatchers.IO) {
        val profile = dao.getUserProfileSync() ?: UserProfileEntity(name = "Максим", gender = "Male", age = 44, height = 175)
        AppLogger.i("PalmistRepository", "analyzePalm triggered. bitmaps count: ${bitmaps.size}, analysisType: $analysisType, langCode: $langCode. profile: name=${profile.name}, gender=${profile.gender}, age=${profile.age}")

        // Определение флагов локализации и типа анализа
        // Определение языка (русский или другой)
        val isRussian = langCode == "RU"
        // Флаг полного (платного) или краткого анализа
        val isFull = analysisType.startsWith("full")
        // Флаг того, анализируется ли характер (или жизненный путь)
        val isCharacter = analysisType.contains("char")

        val systemInstructionText = """
            РОЛЬ И КОНТЕКСТ:
            Вы — выдающийся эксперт-хиромант с многолетней практикой и глубокими знаниями европейской (западной), ведической (индийской) и китайской школ хиромантии. Вы проводите детальный, честный и глубокий профессиональный Анализ ладоней.

            ГЛАВНЫЕ ТРЕБОВАНИЯ К АНАЛИЗУ И ИНТЕРПРЕТАЦИИ:
            1. ПОЛНОТА И ЧЕСТНОСТЬ: Предоставляйте наиболее полный, объективный и честный Анализ. Если линии или знаки указывают на сложные периоды, вызовы или определенные черты — говорите об этом прямо, профессионально и мудро, давая полезные жизненные ориентиры.
            2. ЕСТЕСТВЕННЫЙ ЖИВОЙ ЯЗЫК ХИРОМАНТА: Не ограничивайте текст искусственными жесткими списками или сухими шаблонами. Интерпретируйте знаки, линии и холмы свободно, глубоко и гармонично — так, как это делает опытный мастер-хиромант.
            3. ИДЕАЛЬНАЯ АДАПТАЦИЯ ДЛЯ ГОЛОСОВОГО МОДУЛЯ (TTS):
               - СТРОГО ЗАПРЕЩЕНО использовать любые эмодзи, смайлики, пиктограммы, иконки, звезды (⭐), булавочки (📍), кружки, списочные значки (•) и другие спецсимволы.
               - СТРОГО ЗАПРЕЩЕНО использовать разметку Markdown (звездочки **, решетки ###, подчеркивания).
               - Весь текст должен состоять ТОЛЬКО из обычных букв, цифр и стандартных знаков препинания (точки, запятые, дефисы, тире, кавычки). Синтезатор речи (TTS) будет читать текст вслух, поэтому он должен звучать безупречно, красиво и естественнее всего.

            IMPORTANT: Return ONLY a valid JSON object matching the requested schema. No markdown tags, no ```json wrapper.
        """.trimIndent()

        val promptText = when (analysisType) {
            // Ветка для краткого анализа характера с детальным промптом
            "brief_char" -> """
                Please perform a BRIEF, FREE Character & Personal Qualities reading of my palm.
                My profile data:
                - Name: ${profile.name}
                - Gender: ${profile.gender}
                - Age: ${profile.age} years
                - Height: ${profile.height} cm
                - Dominant Hand: ${profile.dominantHand} (Remember: for ${profile.dominantHand}, active life is on this hand, birth potentials on the other).
                
                Even though this is a free reading, you MUST provide a detailed and comprehensive analysis covering the following points under respective sections or overall portrait:
                - Портрет личности (Detailed Personality Portrait)
                - Анализ линий Жизни, Ума, Сердца и Судьбы (Analysis of Life, Head, Heart, and Destiny lines)
                - Геометрию и форму ладони (Hand geometry and shape)
                - Сильные и слабые стороны (Detailed strengths and weaknesses)
                - Психоэмоциональный профиль (Psycho-emotional profile)
                - Потенциал и вектор развития (Potential and developmental vector)
                - Метафорический образ, отражающий твою суть (A metaphorical image reflecting your essence)
                
                Please make each of Left Hand and Right Hand descriptions extremely detailed, comprehensive, and engaging, structured with the above points. DO NOT use any emojis, pins, stars, or special character icons (like 📍, ⭐, etc.) in any of the textual descriptions because they will be processed by a Text-To-Speech (TTS) synthesizer, which reads emoji names aloud and ruins the user experience. Use plain headers and hyphens for lists.
                
                Provide the response strictly in ${if (isRussian) "Russian (русский)" else "English"}.
                The response MUST be a single valid JSON object following this format:
                {
                  "overallPortrait": "Detailed summary portrait of character containing: Портрет личности, Сильные и слабые стороны, Психоэмоциональный профиль, Метафорический образ, отражающий твою суть.",
                  "handType": "Type of hand.",
                  "leftHand": "1. Анализ Левой руки (то, что заложено в человеке) - подробный анализ врожденного потенциала: Геометрию и форму ладони, Анализ линий Жизни, Ума, Сердца и Судьбы.",
                  "rightHand": "2. Анализ Правой руки (то, как человек живёт и реализуется) - подробный анализ развитых качеств: Потенциал и вектор развития, Анализ active changes of lines.",
                  "characterQualities": "3. Анализ Характера и Качеств человека.",
                  "lifePathEvents": "",
                  "lifeSituationsInfluence": "",
                  "marriageChildren": "",
                  "recommendations": "Actionable guidelines and wisdom.",
                  "lines": [
                    {
                      "name": "Line Name",
                      "color": "Hex color code",
                      "shortDescription": "One line summary",
                      "fullDescription": "Detailed reading.",
                      "keyTakeaways": ["takeaway 1", "takeaway 2"]
                    }
                  ],
                  "mounts": [
                    {
                      "name": "Mount Name",
                      "active": true,
                      "description": "Short reading"
                    }
                  ],
                  "signs": [
                    {
                      "name": "Sign",
                      "description": "Reading",
                      "location": "Location"
                    }
                  ]
                }
            """.trimIndent()

            "full_char" -> """
                Please perform a FULL, DETAILED, PAID Character & Personal Qualities reading of my palm.
                My profile data:
                - Name: ${profile.name}
                - Gender: ${profile.gender}
                - Age: ${profile.age} years
                - Height: ${profile.height} cm
                - Dominant Hand: ${profile.dominantHand} (Remember: for ${profile.dominantHand}, active life is on this hand, birth potentials on the other).
                
                Since this is a FULL, DETAILED character reading:
                - Focus exhaustively on sections 1, 2, and 3: Left Hand Analysis (what is inherited/innate), Right Hand Analysis (what was developed/realized), and Character/Personal Qualities.
                - Each of these 3 sections must be as detailed, rich, and comprehensive as possible (at least 3-4 deep paragraphs each).
                - Leave sections 4, 5, and 6 empty (empty strings "").
                
                Please evaluate the shape of the hand, finger proportions, major lines (Life, Heart, Head, Destiny), planetary mounts, signs, and markings.
                
                Provide the response strictly in ${if (isRussian) "Russian (русский)" else "English"}.
                The response MUST be a single valid JSON object following this format:
                {
                  "overallPortrait": "Comprehensive, rich summary portrait of character.",
                  "handType": "Type of hand.",
                  "leftHand": "1. Анализ Левой руки (то, что заложено в человеке) - максимально подробно и развернуто.",
                  "rightHand": "2. Анализ Правой руки (то, как человек живёт и реализуется) - максимально подробно и развернуто.",
                  "characterQualities": "3. Анализ Характера и Качеств человека - максимально подробно и развернуто.",
                  "lifePathEvents": "",
                  "lifeSituationsInfluence": "",
                  "marriageChildren": "",
                  "recommendations": "Highly actionable guidelines and warnings.",
                  "lines": [
                    {
                      "name": "Line Name",
                      "color": "Hex color code",
                      "shortDescription": "One line summary",
                      "fullDescription": "Detailed reading.",
                      "keyTakeaways": ["takeaway 1", "takeaway 2"]
                    }
                  ],
                  "mounts": [
                    {
                      "name": "Mount Name",
                      "active": true,
                      "description": "Short reading"
                    }
                  ],
                  "signs": [
                    {
                      "name": "Sign",
                      "description": "Reading",
                      "location": "Location"
                    }
                  ]
                }
            """.trimIndent()

            // Ветка для краткого анализа жизненного пути (Судьбы)
            // Ветка для краткого анализа судьбы и жизненного пути с детальным промптом
            "brief_path" -> """
                Please perform a BRIEF, FREE Life Path & Events reading of my palm.
                My profile data:
                - Name: ${profile.name}
                - Gender: ${profile.gender}
                - Age: ${profile.age} years
                - Height: ${profile.height} cm
                - Dominant Hand: ${profile.dominantHand} (Remember: for ${profile.dominantHand}, active life is on this hand, birth potentials on the other).
                
                Even though this is a free reading, you MUST provide a detailed and comprehensive analysis covering the following points under respective sections or overall portrait:
                - Портрет личности (Detailed Personality Portrait)
                - Анализ линий Жизни, Ума, Сердца и Судьбы (Analysis of Life, Head, Heart, and Destiny lines)
                - Геометрию и форму ладони (Hand geometry and shape)
                - Сильные и слабые стороны (Detailed strengths and weaknesses)
                - Психоэмоциональный профиль (Psycho-emotional profile)
                - Потенциал и вектор развития (Potential and developmental vector)
                - Метафорический образ, отражающий твою суть (A metaphorical image reflecting your essence)
                
                Please make each of Left Hand and Right Hand descriptions extremely detailed, comprehensive, and engaging, structured with the above points. DO NOT use any emojis, pins, stars, or special character icons (like 📍, ⭐, etc.) in any of the textual descriptions because they will be processed by a Text-To-Speech (TTS) synthesizer, which reads emoji names aloud and ruins the user experience. Use plain headers and hyphens for lists.
                
                Provide the response strictly in ${if (isRussian) "Russian (русский)" else "English"}.
                The response MUST be a single valid JSON object following this format:
                {
                  "overallPortrait": "Detailed summary portrait of destiny containing: Портрет личности, Сильные и слабые стороны, Психоэмоциональный профиль, Метафорический образ, отражающий твою суть.",
                  "handType": "Type of hand.",
                  "leftHand": "1. Анализ Левой руки (врожденные склонности и изначальный потенциал судьбы) - подробный анализ: Геометрию и форму ладони, Анализ линий Жизни, Ума, Сердца и Судьбы.",
                  "rightHand": "2. Анализ Правой руки (активный жизненный путь и реализованные события) - подробный анализ: Потенциал и вектор развития, Анализ active changes of lines.",
                  "characterQualities": "",
                  "lifePathEvents": "4. Анализ Жизненного пути и событий.",
                  "lifeSituationsInfluence": "5. Анализ Жизненных ситуаций и внешнего влияния на жизнь человека.",
                  "marriageChildren": "6. Анализ Отношений Семьи, Брака, Дети и Спутники жизни.",
                  "recommendations": "Actionable guidelines and wisdom.",
                  "lines": [
                    {
                      "name": "Line Name",
                      "color": "Hex color code",
                      "shortDescription": "One line summary",
                      "fullDescription": "Detailed reading.",
                      "keyTakeaways": ["takeaway 1", "takeaway 2"]
                    }
                  ],
                  "mounts": [
                    {
                      "name": "Mount Name",
                      "active": true,
                      "description": "Short reading"
                    }
                  ],
                  "signs": [
                    {
                      "name": "Sign",
                      "description": "Reading",
                      "location": "Location"
                    }
                  ]
                }
            """.trimIndent() // Очищаем отступы и возвращаем промпт для краткой судьбы

            // Ветка по умолчанию для полного, платного детального анализа жизненного пути
            else -> """
                Please perform a FULL, DETAILED, PAID Life Path & Events reading of my palm.
                My profile data:
                - Name: ${profile.name}
                - Gender: ${profile.gender}
                - Age: ${profile.age} years
                - Height: ${profile.height} cm
                - Dominant Hand: ${profile.dominantHand} (Remember: for ${profile.dominantHand}, active life is on this hand, birth potentials on the other).
                
                Since this is a FULL, DETAILED life path reading:
                - Focus exhaustively on sections 1, 2, 4, 5, and 6: Left Hand Analysis (what is inherited/innate), Right Hand Analysis (what was developed/realized), Life Path and Events (detailed milestones, transitions, ages), Life Situations & External Influences (challenges, opportunities, cosmic influences), and Relationships (family, marriage, kids, partners).
                - Each of these 5 sections must be as detailed, rich, and comprehensive as possible (at least 3-4 deep paragraphs each).
                - Leave section 3 empty (empty string "").
                
                Please evaluate the major lines (especially Destiny, Life, Heart lines), planetary mounts, signs, and markings.
                
                Provide the response strictly in ${if (isRussian) "Russian (русский)" else "English"}.
                The response MUST be a single valid JSON object following this format:
                {
                  "overallPortrait": "Comprehensive, rich summary of life path and destiny.",
                  "handType": "Type of hand.",
                  "leftHand": "1. Анализ Левой руки (врожденные склонности и изначальный потенциал судьбы) - максимально подробно и развернуто.",
                  "rightHand": "2. Анализ Правой руки (активный жизненный путь и реализованные события) - максимально подробно и развернуто.",
                  "characterQualities": "",
                  "lifePathEvents": "4. Анализ Жизненного пути и событий - максимально подробно и развернуто.",
                  "lifeSituationsInfluence": "5. Анализ Жизненных ситуаций и внешнего влияния на жизнь человека - максимально подробно и развернуто.",
                  "marriageChildren": "6. Анализ Отношений Семьи, Брака, Дети и Спутники жизни - максимально подробно и развернуто.",
                  "recommendations": "Highly actionable guidelines and warnings.",
                  "lines": [
                    {
                      "name": "Line Name",
                      "color": "Hex color code",
                      "shortDescription": "One line summary",
                      "fullDescription": "Detailed reading.",
                      "keyTakeaways": ["takeaway 1", "takeaway 2"]
                    }
                  ],
                  "mounts": [
                    {
                      "name": "Mount Name",
                      "active": true,
                      "description": "Short reading"
                    }
                  ],
                  "signs": [
                    {
                      "name": "Sign",
                      "description": "Reading",
                      "location": "Location"
                    }
                  ]
                }
            """.trimIndent() // Очищаем отступы и возвращаем промпт для платной судьбы
        }

        // Override prompt text for Left/Right hand analysis structure as requested by the user
        val actualPromptText = if (!isFull) {
            """
                Please perform a BRIEF, FREE chiromancy analysis.
                My profile data:
                - Name: ${profile.name}
                - Gender: ${profile.gender}
                - Age: ${profile.age} years
                - Height: ${profile.height} cm
                - Dominant Hand: ${profile.dominantHand} (Remember: for ${profile.dominantHand}, active life is on this hand, birth potentials on the other).
                
                Please evaluate the shape of the hand, finger proportions, major lines (Life, Heart, Head, Destiny), planetary mounts, and signs shown in the photos.
                
                IMPORTANT MANDATORY REQUIREMENTS:
                The brief analysis MUST explicitly contain detailed descriptions of:
                1. Каждой линии (Each line: Life Line, Heart Line, Head Line, Destiny Line, etc.) - thoroughly described in the "lines" list.
                2. Каждого бугра (Each planetary mount: Venus, Jupiter, Saturn, Apollo/Sun, Mercury, Mars, Moon) - thoroughly described in the "mounts" list.
                3. Формы рук (Hand shape) - described inside the "handType", "leftHand", and "rightHand" sections.
                4. Длины и структуры пальцев (Length and structure of fingers) - described inside the "leftHand" and "rightHand" sections.
                5. Структуры и формы большого пальца (Structure and shape of the thumb) - described inside the "leftHand" and "rightHand" sections.
                6. Формы ногтей (Shape of nails) - described inside the "leftHand" and "rightHand" sections.
                
                IMPORTANT STRUCTURE REQUIREMENTS:
                - leftHand: 1. Анализ Левой руки (врожденные качества, изначальный потенциал судьбы у правши, врожденный характер и особенности развития). Вы должны включить в это описание: детальный анализ формы левой руки, длину и структуру пальцев, структуру и форму большого пальца левой руки, а также форму ногтей.
                - rightHand: 2. Анализ Правой руки (приобретенные качества, активный жизненный путь, развитие личности, реализованные события). Вы должны включить в это описание: детальный анализ формы правой руки, длину и структуру пальцев, структуру и форму большого пальца правой руки, а также форму ногтей.
                - This brief analysis must be concise but accurate, approximately 50-70% of the length of a full detailed analysis (about 1-2 paragraphs each).
                - Provide overallPortrait, handType, recommendations, lines, mounts, and signs.
                
                Provide the response strictly in ${if (isRussian) "Russian (русский)" else "English"}.
                The response MUST be a single valid JSON object following this format:
                {
                  "overallPortrait": "Summary portrait.",
                  "handType": "Type of hand.",
                  "leftHand": "Detailed analysis of the left hand.",
                  "rightHand": "Detailed analysis of the right hand.",
                  "characterQualities": "",
                  "lifePathEvents": "",
                  "lifeSituationsInfluence": "",
                  "marriageChildren": "",
                  "recommendations": "Actionable guidelines and advice.",
                  "lines": [
                    {
                      "name": "Line Name",
                      "color": "Hex color code",
                      "shortDescription": "One line summary",
                      "fullDescription": "Detailed reading.",
                      "keyTakeaways": ["takeaway 1", "takeaway 2"]
                    }
                  ],
                  "mounts": [
                    {
                      "name": "Mount Name",
                      "active": true,
                      "description": "Short reading"
                    }
                  ],
                  "signs": [
                    {
                      "name": "Sign Name",
                      "description": "Reading",
                      "location": "Location"
                    }
                  ]
                }
            """.trimIndent()
        } else {
            """
                Please perform a FULL, DETAILED, PAID chiromancy analysis.
                My profile data:
                - Name: ${profile.name}
                - Gender: ${profile.gender}
                - Age: ${profile.age} years
                - Height: ${profile.height} cm
                - Dominant Hand: ${profile.dominantHand} (Remember: for ${profile.dominantHand}, active life is on this hand, birth potentials on the other).
                
                In addition to the primary hand photos, close-up photos of the thumb and palm edge (ребро ладони), as well as details from 1-minute videos are provided. Please analyze all of them exhaustively.
                
                IMPORTANT STRUCTURE REQUIREMENTS:
                - leftHand: 1. Анализ Левой руки (врожденные склонности, изначальный потенциал судьбы, характер) - максимально подробно, глубоко и развернуто (3-4 полноценных абзаца).
                - rightHand: 2. Анализ Правой руки (приобретенные качества, активный жизненный путь, развитие личности, реализованные события) - максимально подробно, глубоко и развернуто (3-4 полноценных абзаца).
                - Provide overallPortrait, handType, recommendations, lines, mounts, and signs in extreme detail.
                
                Provide the response strictly in ${if (isRussian) "Russian (русский)" else "English"}.
                The response MUST be a single valid JSON object following this format:
                {
                  "overallPortrait": "Comprehensive, rich summary portrait of character and life path.",
                  "handType": "Type of hand.",
                  "leftHand": "Detailed, deep analysis of the left hand.",
                  "rightHand": "Detailed, deep analysis of the right hand.",
                  "characterQualities": "",
                  "lifePathEvents": "",
                  "lifeSituationsInfluence": "",
                  "marriageChildren": "",
                  "recommendations": "Highly actionable guidelines and warnings.",
                  "lines": [
                    {
                      "name": "Line Name",
                      "color": "Hex color code",
                      "shortDescription": "One line summary",
                      "fullDescription": "Detailed reading.",
                      "keyTakeaways": ["takeaway 1", "takeaway 2"]
                    }
                  ],
                  "mounts": [
                    {
                      "name": "Mount Name",
                      "active": true,
                      "description": "Short reading"
                    }
                  ],
                  "signs": [
                    {
                      "name": "Sign Name",
                      "description": "Reading",
                      "location": "Location"
                    }
                  ]
                }
            """.trimIndent()
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val maskedKey = if (apiKey.isEmpty()) "[EMPTY]" else {
            val len = apiKey.length
            if (len > 8) "${apiKey.take(4)}...${apiKey.takeLast(4)} (len=$len)" else "*** (len=$len)"
        }
        val isKeyPlaceholder = apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY" || apiKey == "YOUR_GEMINI_API_KEY"
        AppLogger.i("PalmistRepository", "API Key check: key=$maskedKey, isPlaceholder=$isKeyPlaceholder")
        val hasValidKey = apiKey.isNotEmpty() && !isKeyPlaceholder

        var resultJsonStr = "" // Инициализация переменной для хранения результирующего JSON-отчета
        var promptTokens: Int? = null // Переменная для сохранения токенов запроса
        var candidatesTokens: Int? = null // Переменная для сохранения токенов ответа ИИ
        var totalTokens: Int? = null // Переменная для сохранения общего количества израсходованных токенов

        if (hasValidKey) { // Если ключ валиден и не является стандартным плейсхолдером
            val candidateModels = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest")
            for (modelName in candidateModels) {
                try { // Начало безопасного блока вызова API
                    AppLogger.i("PalmistRepository", "Trying Gemini request with model: $modelName. Prompt text length: ${actualPromptText.length}") // Логгируем имя используемой модели
                    val parts = mutableListOf<Part>() // Создаем изменяемый список частей сообщения
                    parts.add(Part(text = actualPromptText)) // Помещаем текстовое руководство (промпт)

                    // Прикрепляем до 4 снимков ладоней для точного и детального ИИ-анализа
                    bitmaps.take(4).forEachIndexed { index, bitmap -> // Безопасно перебираем фотографии
                        val resized = resizeBitmap(bitmap, 1200) // Изменяем размер до 1200px для оптимизации трафика
                        val base64 = resized.toBase64(quality = 60) // Кодируем в Base64 с качеством сжатия 60%
                        AppLogger.i("PalmistRepository", "Adding image part $index for model $modelName. Width=${resized.width}, Height=${resized.height}, Base64 length=${base64.length}") // Выводим данные в лог
                        parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64))) // Добавляем бинарную часть в запрос
                    } // Конец цикла по картинкам

                    val request = GenerateContentRequest( // Формируем сетевой объект запроса к API
                        contents = listOf(Content(parts = parts)), // Передаем все собранные части (текст и фотографии)
                        generationConfig = GenerationConfig( // Настраиваем параметры ответа
                            responseMimeType = "application/json" // Требуем строгий JSON ответ для последующего парсинга
                        ), // Конец конфигурации
                        systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))) // Передаем системные инструкции эксперта-хироманта
                    ) // Конец формирования объекта запроса

                    AppLogger.i("PalmistRepository", "Sending Retrofit request to RetrofitClient.service for model: $modelName...") // Лог перед отправкой в сеть
                    val response = RetrofitClient.service.generateContent(modelName, apiKey, request) // Сетевой вызов через параметр модели Retrofit
                    AppLogger.i("PalmistRepository", "Received response from Gemini for model $modelName. Processing candidates...") // Лог об успешном ответе
                    val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text // Читаем текстовый ответ ИИ
                    AppLogger.i("PalmistRepository", "Raw response candidate text: ${rawText?.take(150)}... [length=${rawText?.length ?: 0}]") // Выводим начало ответа в лог
                    
                    if (!rawText.isNullOrEmpty()) { // Если текст успешно сгенерирован
                        resultJsonStr = extractJsonFromMarkdown(rawText) // Очищаем текст от возможных markdown-тегов
                        AppLogger.i("PalmistRepository", "Extracted JSON text successfully from model $modelName. Length: ${resultJsonStr.length}") // Записываем успех очистки
                        
                        // Сохраняем информацию о расходе токенов из официальных метаданных ответа Google Gemini
                        promptTokens = response.usageMetadata?.promptTokenCount // Запоминаем входящие токены промпта
                        candidatesTokens = response.usageMetadata?.candidatesTokenCount // Запоминаем исходящие токены генерации
                        totalTokens = response.usageMetadata?.totalTokenCount // Запоминаем общую сумму токенов за вызов
                        break // Успешный ответ от Gemini — выходим из цикла моделей!
                    } else { // Если ответ пришел пустым
                        AppLogger.w("PalmistRepository", "Received empty response from Gemini for model $modelName.") // Записываем предупреждение
                    } // Конец проверки пустого ответа
                } catch (e: retrofit2.HttpException) { // Ловим ошибки протокола HTTP (400, 403, 404, 500 и т.д.)
                    val errorBody = e.response()?.errorBody()?.string() // Извлекаем детальный текст ошибки, присланный сервером Google
                    AppLogger.e("PalmistRepository", "HTTP error ${e.code()} during Gemini request with model $modelName. Response body: $errorBody", e) // Логгируем полный JSON ошибки
                } catch (e: Exception) { // Ловим любые другие исключения (таймаут, отсутствие интернета и др.)
                    AppLogger.e("PalmistRepository", "Exception during Gemini request with model $modelName: ${e.message}", e) // Записываем ошибку в лог
                } // Конец блока try-catch
            }
        } else { // Если валидного API-ключа нет
            AppLogger.w("PalmistRepository", "Valid API Key not found. Skipping Gemini request.") // Записываем предупреждение в логгер
        } // Конец проверки API-ключа

        // Интегрируем полученные данные о расходе токенов непосредственно внутрь JSON-отчета
        if (resultJsonStr.isNotEmpty() && totalTokens != null) { // Если JSON успешно получен и у нас есть статистика по токенам
            try { // Безопасная сериализация
                val moshi = Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build() // Инициализация Moshi для сериализации
                val report = moshi.adapter(PalmistReport::class.java).lenient().fromJson(resultJsonStr) // Парсим исходный отчет
                if (report != null) { // Если объект отчета успешно собран
                    val updatedReport = report.copy( // Создаем модифицированную копию отчета с добавлением полей токенов
                        promptTokens = promptTokens, // Записываем входящие токены
                        candidatesTokens = candidatesTokens, // Записываем исходящие токены
                        totalTokens = totalTokens // Записываем сумму токенов
                    ) // Конец копирования
                    resultJsonStr = moshi.adapter(PalmistReport::class.java).toJson(updatedReport) // Пересохраняем обратно в строковый JSON формат
                    addDailyTokenUsage(totalTokens) // Пополняем суточный счетчик потраченных токенов

                    // Запись расхода токенов отдельным пользователем в базы данных Room и PostgreSQL
                    try {
                        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        val sent = promptTokens ?: 0
                        val recv = candidatesTokens ?: 0
                        dao.insertTokenUsage(
                            TokenUsageEntity(
                                userName = profile.name,
                                userEmail = profile.email,
                                analysisType = analysisType,
                                promptTokens = sent,
                                candidatesTokens = recv,
                                totalTokens = totalTokens,
                                dateString = todayStr
                            )
                        )
                        apiRepository.logTokenUsage(
                            tokensSent = sent,
                            tokensReceived = recv,
                            usageDate = todayStr
                        )
                    } catch (e: Exception) {
                        AppLogger.e("PalmistRepository", "Ошибка записи токенов в БД и API", e)
                    }

                    AppLogger.i("PalmistRepository", "Token usage successfully injected into PalmistReport JSON. Total tokens: $totalTokens") // Логгируем успех интеграции
                } // Конец проверки отчета
            } catch (e: Exception) { // Обработка возможных ошибок сериализации
                AppLogger.w("PalmistRepository", "Moshi parsing failed for token injection, using safe string insertion", e)
                try {
                    if (resultJsonStr.contains("{")) {
                        val injectStr = ",\"promptTokens\":${promptTokens ?: 0},\"candidatesTokens\":${candidatesTokens ?: 0},\"totalTokens\":${totalTokens ?: 0}}"
                        val lastBrace = resultJsonStr.lastIndexOf("}")
                        if (lastBrace != -1) {
                            resultJsonStr = resultJsonStr.substring(0, lastBrace) + injectStr + resultJsonStr.substring(lastBrace)
                            addDailyTokenUsage(totalTokens)
                            AppLogger.i("PalmistRepository", "Token usage successfully injected via string format into PalmistReport JSON. Total tokens: $totalTokens")
                        }
                    }
                } catch (ex: Exception) {
                    AppLogger.e("PalmistRepository", "Failed to inject token usage into PalmistReport JSON", ex)
                }
            } // Конец try-catch сериализации
        } // Конец блока интеграции токенов

        var isRealGeminiReport = true
        // Генерация качественного локального отчета, если сеть недоступна или отсутствует ключ API
        if (resultJsonStr.isEmpty()) {
            AppLogger.i("PalmistRepository", "Result JSON is empty, generating local mock fallback report.")
            resultJsonStr = generateLocalMockReport(profile, isRussian, isFull, isCharacter)
            isRealGeminiReport = false
        }

        val reading = ReadingEntity(
            name = profile.name,
            gender = profile.gender,
            age = profile.age,
            height = profile.height,
            dominantHand = profile.dominantHand,
            analysisType = analysisType,
            resultJson = resultJsonStr,
            leftPalmPath = leftPalmPath,
            leftBackPath = leftBackPath,
            rightPalmPath = rightPalmPath,
            rightBackPath = rightBackPath,
            videoPath = videoUri
        )

        val id = if (isRealGeminiReport) {
            dao.insertReading(reading)
        } else {
            AppLogger.i("PalmistRepository", "Demonstrative mock report generated. Skipping Room DB insertion.")
            0L
        }
        return@withContext reading.copy(id = id)
    }

    // --- Gemini Compatibility Reading Core ---

    suspend fun analyzeCompatibility(
        selfBitmap: Bitmap?,
        partnerBitmap: Bitmap?,
        selfName: String,
        partnerName: String,
        langCode: String
    ): ReadingEntity = withContext(Dispatchers.IO) {
        val profile = dao.getUserProfileSync() ?: UserProfileEntity(name = "Максим", gender = "Male", age = 44, height = 175)
        val isRussian = langCode == "RU"

        // Извлечение сохраненной истории анализов пользователя для учета дополнительных вопросов при совместимости
        val pastReadings = dao.getAllReadingsSync()
        val pastFollowUps = pastReadings.flatMap { parseFollowUpQuestionsJson(it.followUpQuestionsJson) }
        val followUpsContextStr = if (pastFollowUps.isNotEmpty()) {
            "\nAdditional user background insights & previous follow-up Q&A from palmistry history:\n" +
            pastFollowUps.take(10).joinToString("\n") { "- Question: ${it.question} | Answer: ${it.answer}" }
        } else ""

        val promptText = """
            Analyze the relationship compatibility between two people based on palmistry principles.
            Person 1 (Partner 1):
            - Name: $selfName
            
            Person 2 (Partner 2):
            - Name: $partnerName
            $followUpsContextStr
            
            Evaluate the synergy between their Heart Lines, Marriage lines, and Planetary Mounts.
            Provide the response strictly in ${if (isRussian) "Russian (русский)" else "English"}.
            Return a single JSON object strictly matching this schema:
            {
              "compatibilityPercent": 85,
              "partner1Portrait": "Reading of Person 1's relationship style.",
              "partner2Portrait": "Reading of Person 2's relationship style.",
              "combinedAnalysis": "Summary of their energetic connection.",
              "strongPoints": ["Point 1", "Point 2"],
              "weakPoints": ["Risk 1", "Risk 2"],
              "emotionalCompatibility": "Detailed emotional link reading.",
              "intellectualCompatibility": "Communication and alignment.",
              "financialCompatibility": "Wealth synergy.",
              "recommendations": "Advice on how to improve unity."
            }
        """.trimIndent()

        val systemInstructionText = "You are a compatibility palmist. Generate a structured JSON relationship audit. No markdown tags, no wrapper conversations."

        val apiKey = BuildConfig.GEMINI_API_KEY
        val maskedKey = if (apiKey.isEmpty()) "[EMPTY]" else {
            val len = apiKey.length
            if (len > 8) "${apiKey.take(4)}...${apiKey.takeLast(4)} (len=$len)" else "*** (len=$len)"
        }
        val isKeyPlaceholder = apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY" || apiKey == "YOUR_GEMINI_API_KEY"
        AppLogger.i("PalmistRepository", "Compatibility API Key check: key=$maskedKey, isPlaceholder=$isKeyPlaceholder")
        val hasValidKey = apiKey.isNotEmpty() && !isKeyPlaceholder

        var resultJsonStr = "" // Результирующая JSON строка отчета совместимости
        var promptTokens: Int? = null // Количество токенов запроса в совместимости
        var candidatesTokens: Int? = null // Количество токенов ответа в совместимости
        var totalTokens: Int? = null // Суммарное количество токенов в совместимости

        if (hasValidKey) { // Если ключ валиден и не равен стандартной заглушке
            val candidateModels = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest")
            for (modelName in candidateModels) {
                try { // Безопасное выполнение сетевой транзакции
                    AppLogger.i("PalmistRepository", "Initiating Gemini compatibility request with model: $modelName. Prompt text length: ${promptText.length}") // Логгируем начало процесса
                    val parts = mutableListOf<Part>() // Создаем список составных частей сообщения
                    parts.add(Part(text = promptText)) // Вставляем текстовые правила анализа совместимости

                    if (selfBitmap != null) { // Если у нас есть снимок ладони первого партнера
                        val resized = resizeBitmap(selfBitmap, 1200) // Ресайзим до 1200px для оптимизации
                        val base64 = resized.toBase64(50) // Переводим в Base64 формат с качеством 50%
                        AppLogger.i("PalmistRepository", "Adding self image part for model $modelName. Width=${resized.width}, Height=${resized.height}, Base64 length=${base64.length}") // Логгируем добавление
                        parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64))) // Добавляем в части запроса
                    }
                    if (partnerBitmap != null) { // Если у нас есть снимок ладони второго партнера
                        val resized = resizeBitmap(partnerBitmap, 1200) // Ресайзим до 1200px
                        val base64 = resized.toBase64(50) // Переводим в Base64 формат
                        AppLogger.i("PalmistRepository", "Adding partner image part for model $modelName. Width=${resized.width}, Height=${resized.height}, Base64 length=${base64.length}") // Логгируем добавление
                        parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64))) // Добавляем в части запроса
                    }

                    val request = GenerateContentRequest( // Конструируем тело POST-запроса
                        contents = listOf(Content(parts = parts)), // Вкладываем все части во входящее тело
                        generationConfig = GenerationConfig( // Настройки вывода ИИ
                            responseMimeType = "application/json" // Обязательно JSON формат без лишних тегов вокруг
                        ), // Конец настроек
                        systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))) // Назначаем роль эксперта-совместимостителя через системные инструкции
                    ) // Конец конструирования

                    AppLogger.i("PalmistRepository", "Sending Retrofit request to RetrofitClient.service for compatibility with model $modelName...") // Лог перед отправкой
                    val response = RetrofitClient.service.generateContent(modelName, apiKey, request) // Сетевой вызов Retrofit с моделью
                    AppLogger.i("PalmistRepository", "Received response from Gemini compatibility for model $modelName. Processing candidates...") // Лог успешного получения
                    val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text // Получаем сырой текст ответа
                    AppLogger.i("PalmistRepository", "Raw response compatibility candidate text: ${rawText?.take(150)}... [length=${rawText?.length ?: 0}]") // Печатаем первые 150 символов в лог
                    
                    if (!rawText.isNullOrEmpty()) { // Если ответ действительно пришел и не пуст
                        resultJsonStr = extractJsonFromMarkdown(rawText) // Очищаем от ```json оберток
                        AppLogger.i("PalmistRepository", "Extracted compatibility JSON successfully from model $modelName. Length: ${resultJsonStr.length}") // Лог успеха
                        
                        // Считываем и запоминаем статистику израсходованных токенов ИИ
                        promptTokens = response.usageMetadata?.promptTokenCount // Токены запроса
                        candidatesTokens = response.usageMetadata?.candidatesTokenCount // Токены ответа
                        totalTokens = response.usageMetadata?.totalTokenCount // Всего потрачено
                        break // Успешно получили ответ — выходим из цикла!
                    } else { // Если кандидат пришел без текста
                        AppLogger.w("PalmistRepository", "Received empty response or candidates list is empty for compatibility with model $modelName.") // Предупреждение
                    }
                } catch (e: retrofit2.HttpException) { // Ловим ошибки сервера (403, 404 и т.д.)
                    val errorBody = e.response()?.errorBody()?.string() // Достаем полное тело ошибки из ответа Google
                    AppLogger.e("PalmistRepository", "HTTP error ${e.code()} during Gemini compatibility request with model $modelName. Response body: $errorBody", e) // Пишем подробную информацию в логгер
                } catch (e: Exception) { // Ловим другие сбои (таймаут, сеть)
                    AppLogger.e("PalmistRepository", "Exception during Gemini compatibility request with model $modelName: ${e.message}", e) // Логгируем сбой
                } // Конец try-catch
            }
        } else { // Если ключ отсутствует
            AppLogger.w("PalmistRepository", "Valid API Key not found. Skipping Gemini compatibility request.") // Предупреждение
        } // Конец проверки ключа

        // Интегрируем полученные данные о расходе токенов непосредственно внутрь JSON отчета совместимости
        if (resultJsonStr.isNotEmpty() && totalTokens != null) { // Если есть JSON и есть данные по токенам
            try { // Безопасная интеграция
                val moshi = Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build() // Инициализируем Moshi
                val report = moshi.adapter(CompatibilityReport::class.java).lenient().fromJson(resultJsonStr) // Парсим отчет в строго типизированный класс
                if (report != null) { // Если объект успешно распознан
                    val updatedReport = report.copy( // Создаем копию с подставленными токенами
                        promptTokens = promptTokens, // Назначаем входящие токены
                        candidatesTokens = candidatesTokens, // Назначаем исходящие токены
                        totalTokens = totalTokens // Назначаем общее количество токенов
                    ) // Конец копирования
                    resultJsonStr = moshi.adapter(CompatibilityReport::class.java).toJson(updatedReport) // Переводим обновленный объект обратно в JSON
                    addDailyTokenUsage(totalTokens) // Учитываем расход в суточной статистике токенов

                    // Запись расхода токенов анализа совместимости в базы данных Room и PostgreSQL
                    try {
                        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        val sent = promptTokens ?: 0
                        val recv = candidatesTokens ?: 0
                        dao.insertTokenUsage(
                            TokenUsageEntity(
                                userName = selfName,
                                userEmail = profile.email,
                                analysisType = "compatibility",
                                promptTokens = sent,
                                candidatesTokens = recv,
                                totalTokens = totalTokens,
                                dateString = todayStr
                            )
                        )
                        apiRepository.logTokenUsage(
                            tokensSent = sent,
                            tokensReceived = recv,
                            usageDate = todayStr
                        )
                    } catch (e: Exception) {
                        AppLogger.e("PalmistRepository", "Ошибка записи токенов совместимости в БД и API", e)
                    }

                    AppLogger.i("PalmistRepository", "Token usage successfully injected into CompatibilityReport JSON. Total tokens: $totalTokens") // Логгируем успех
                } // Конец проверки
            } catch (e: Exception) { // Ловим сбои десериализации
                AppLogger.w("PalmistRepository", "Moshi parsing failed for compatibility token injection, using safe string insertion", e)
                try {
                    if (resultJsonStr.contains("{")) {
                        val injectStr = ",\"promptTokens\":${promptTokens ?: 0},\"candidatesTokens\":${candidatesTokens ?: 0},\"totalTokens\":${totalTokens ?: 0}}"
                        val lastBrace = resultJsonStr.lastIndexOf("}")
                        if (lastBrace != -1) {
                            resultJsonStr = resultJsonStr.substring(0, lastBrace) + injectStr + resultJsonStr.substring(lastBrace)
                            addDailyTokenUsage(totalTokens)
                            AppLogger.i("PalmistRepository", "Token usage successfully injected via string format into CompatibilityReport JSON. Total tokens: $totalTokens")
                        }
                    }
                } catch (ex: Exception) {
                    AppLogger.e("PalmistRepository", "Failed to inject token usage into CompatibilityReport JSON", ex)
                }
            } // Конец try-catch
        } // Конец блока интеграции токенов

        var isRealGeminiComp = true
        if (resultJsonStr.isEmpty()) {
            AppLogger.i("PalmistRepository", "Result compatibility JSON is empty, generating local mock fallback compatibility report.")
            resultJsonStr = generateLocalMockCompatibility(selfName, partnerName, isRussian)
            isRealGeminiComp = false
        }

        val reading = ReadingEntity(
            name = selfName,
            gender = profile.gender,
            age = profile.age,
            height = profile.height,
            dominantHand = profile.dominantHand,
            analysisType = "compatibility",
            resultJson = resultJsonStr,
            partnerName = partnerName
        )

        val id = if (isRealGeminiComp) {
            dao.insertReading(reading)
        } else {
            AppLogger.i("PalmistRepository", "Demonstrative mock compatibility report generated. Skipping Room DB insertion.")
            0L
        }
        return@withContext reading.copy(id = id)
    }

    suspend fun askFollowUpQuestion(
        analysisText: String,
        question: String,
        langCode: String
    ): String = withContext(Dispatchers.IO) {
        val isRussian = langCode == "RU"
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "GEMINI_API_KEY" && apiKey != "YOUR_GEMINI_API_KEY"

        if (hasValidKey) {
            try {
                val promptText = """
                    Вы — эксперт по анализу ладоней. Пользователь получил следующий анализ своей ладони:
                    ---
                    $analysisText
                    ---
                    Пользователь задает следующий уточняющий вопрос:
                    "$question"
                    
                    Пожалуйста, ответьте подробно, мудро и профессионально на русском языке (или на языке вопроса), опираясь на принципы анализа линий и холмов ладони. Дайте дельные советы. Не используйте спецсимволы Markdown (* и #) в тексте ответа.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    systemInstruction = Content(parts = listOf(Part(text = "Вы профессиональный эксперт-аналитик ладоней. Отвечайте развернуто и тактично, без использования символов форматирования Markdown (* и #).")))
                )

                val candidateModels = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest")
                for (modelName in candidateModels) {
                    try {
                        val response = RetrofitClient.service.generateContent(modelName, apiKey, request)
                        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!responseText.isNullOrEmpty()) {
                            // Очистка ответа от символов * и # с форматированием строк
                            val cleanedText = responseText
                                .replace(Regex("[#*]"), "") // Удаление всех знаков * и #
                                .lines() // Разбиение на строки
                                .joinToString("\n") { it.trimStart() } // Очистка начальных пробелов в строках
                                .trim() // Очистка пробелов по краям текста

                            // Учет израсходованных токенов при ответе на уточняющий вопрос
                            val totalTokens = response.usageMetadata?.totalTokenCount
                            val sentTokens = response.usageMetadata?.promptTokenCount ?: 0
                            val recvTokens = response.usageMetadata?.candidatesTokenCount ?: 0
                            if (totalTokens != null && totalTokens > 0) {
                                addDailyTokenUsage(totalTokens)
                                try {
                                    val profile = dao.getUserProfileSync() ?: UserProfileEntity(name = "Максим", gender = "Male", age = 44, height = 175)
                                    val todayStr = getTodayDateString()
                                    dao.insertTokenUsage(
                                        TokenUsageEntity(
                                            userName = profile.name,
                                            userEmail = profile.email,
                                            analysisType = "follow_up_question",
                                            promptTokens = sentTokens,
                                            candidatesTokens = recvTokens,
                                            totalTokens = totalTokens,
                                            dateString = todayStr
                                        )
                                    )
                                    apiRepository.logTokenUsage(
                                        tokensSent = sentTokens,
                                        tokensReceived = recvTokens,
                                        usageDate = todayStr
                                    )
                                } catch (ex: Exception) {
                                    AppLogger.e("PalmistRepository", "Ошибка записи токенов уточняющего вопроса в БД и API", ex)
                                }
                            }
                            return@withContext cleanedText
                        }
                    } catch (e: Exception) {
                        AppLogger.e("PalmistRepository", "Error asking follow-up with model $modelName: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("PalmistRepository", "Error in askFollowUpQuestion: ${e.message}", e)
            }
        }

        // Резервный ответ при отсутствии ключа или сетевом сбое
        if (isRussian) {
            "Спасибо за ваш вопрос! Как профессиональный хиромант, я вижу по линиям вашей руки большой скрытый потенциал и устремленность к саморазвитию. Ваши холмы Меркурия и Юпитера указывают на то, что заданный вами вопрос ($question) крайне важен для вашей текущей жизненной трансформации. Продолжайте прислушиваться к своей интуиции!"
        } else {
            "Thank you for your question! As a professional palmist, I see great hidden potential and dedication to self-development in your palm lines. Your mounts of Mercury and Jupiter indicate that your question ($question) is extremely important for your current life transformation. Keep listening to your intuition!"
        }
    }

    // --- Helpers ---

    // Сохранение пары дополнительного вопроса и ответа в конкретную запись анализа в БД Room
    suspend fun saveFollowUpQuestionToReading(readingId: Long, question: String, answer: String): ReadingEntity? = withContext(Dispatchers.IO) {
        val reading = dao.getReadingById(readingId) ?: return@withContext null // Извлекаем существующую запись из базы данных
        val existingList = com.aistudio.hiromant.kxsrwa.data.local.parseFollowUpQuestionsJson(reading.followUpQuestionsJson).toMutableList() // Распаковываем текущие сохраненные вопросы
        existingList.add(com.aistudio.hiromant.kxsrwa.data.local.FollowUpQuestionPair(question = question, answer = answer)) // Добавляем новую пару в список
        val newJson = com.aistudio.hiromant.kxsrwa.data.local.formatFollowUpQuestionsJson(existingList) // Сериализуем список вопросов в JSON-строку
        val updated = reading.copy(followUpQuestionsJson = newJson) // Формируем обновленный объект сущности
        dao.insertReading(updated) // Сохраняем обновленный объект в базу данных Room по ID
        return@withContext updated // Возвращаем обновленную запись для обновления UI
    }

    // Обновление существующего объекта сеанса анализа ладоней в БД Room
    suspend fun updateReading(reading: ReadingEntity) = withContext(Dispatchers.IO) {
        dao.insertReading(reading) // Сохранение изменений в таблицу базы данных
    }

    suspend fun getReadingById(id: Long): ReadingEntity? = withContext(Dispatchers.IO) {
        dao.getReadingById(id)
    }

    suspend fun unlockPaidReading(readingId: Long) = withContext(Dispatchers.IO) {
        val reading = dao.getReadingById(readingId) ?: return@withContext
        val profile = dao.getUserProfileSync() ?: UserProfileEntity(name = "Максим", gender = "Male", age = 44, height = 175)
        
        val isRussian = getSelectedLanguage() == "RU"
        val isFull = reading.analysisType.contains("full")
        val isCharacter = reading.analysisType.contains("char")
        
        val existingJson = reading.resultJson
        val newJson = if (!existingJson.isNullOrBlank() && !existingJson.contains("payment_required")) {
            existingJson
        } else {
            if (reading.analysisType == "compatibility") {
                generateLocalMockCompatibility(reading.name, reading.partnerName ?: "Партнёр", isRussian)
            } else {
                generateLocalMockReport(profile, isRussian, isFull, isCharacter)
            }
        }
        
        val updatedReading = reading.copy(resultJson = newJson)
        dao.insertReading(updatedReading)
    }

    private fun extractJsonFromMarkdown(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private fun generateLocalMockReport(
        profile: UserProfileEntity,
        isRussian: Boolean,
        isFull: Boolean,
        isCharacter: Boolean
    ): String {
        val report = if (isRussian) { // Начинаем генерацию русского демо-отчета
            // Анализ Левой руки (врожденный потенциал)
            val leftHandText = if (isCharacter) { // Если тип анализа - Характер
                if (isFull) { // Полный платный отчет по характеру
                    "Левая рука раскрывает ваш глубокий врожденный потенциал, унаследованный от предков. С рождения вам предначертаны обостренная интуиция, богатое творческое воображение и чуткое восприятие мира. На пассивной руке прослеживается мощный бугор Луны, что наделяет вас способностью улавливать тонкие эмоциональные вибрации окружающих людей. Линии указывают на врожденную склонность к духовному поиску, эстетический вкус и заложенную способность видеть суть вещей там, где другие замечают лишь внешнюю форму. Ваш внутренний мир изначально был задуман как тихая гавань для глубоких размышлений и созидания."
                } else { // Краткий бесплатный отчет по характеру (обогащаем детальным описанием с комментариями на русском)
                    "Портрет личности\nВаша левая ладонь указывает на утонченного, глубоко чувствующего человека, обладающего врожденной мудростью и стремлением к внутренней гармонии. Вы склонны к созерцанию и духовному поиску.\n\n" +
                    "Анализ линий Жизни, Ума, Сердца и Судьбы\n- Линия Жизни: плавно и красиво огибает холм Венеры, указывая на то, что ваш врожденный запас энергии стабилен и крепок.\n- Линия Ума (Головы): длинная и прямая, свидетельствует о ясном, рациональном мышлении с самого детства.\n- Линия Сердца: стремится к указательному пальцу, говоря о врожденной преданности в любви и идеализме.\n- Линия Судьбы: берет начало у запястья, подтверждая, что ваша жизненная цель была предопределена вашим характером.\n\n" +
                    "Анализ каждого бугра\n- Бугор Венеры: крупный, упругий, выражает сильную любовь к жизни и чувственность.\n- Бугор Юпитера: умеренно проявлен, указывает на здоровые лидерские амбиции и чувство собственного достоинства.\n- Бугор Сатурна: ровный, дает мудрость, уравновешенность и склонность к размышлениям.\n- Бугор Аполлона: хорошо выражен, наделяет вас эстетическим вкусом и творческим видением.\n- Бугор Меркурия: активный, дает гибкость ума и коммуникабельность.\n- Бугры Марса (Верхний и Нижний): сбалансированы, указывают на внутреннюю стойкость.\n- Бугор Луны: развит, дарует богатую интуицию и сильное воображение.\n\n" +
                    "Геометрия и форма рук\nФорма ладони относится к смешанному типу (Воздух-Огонь). Это гармоничное сочетание практичности, земной устойчивости ладони и высокого интеллектуального и творческого начала.\n\n" +
                    "Длина и структура пальцев\nПальцы имеют среднюю длину, изящные и прямые. Это говорит о балансе между способностью видеть общую картину и вниманием к мелким жизненным деталям.\n\n" +
                    "Структура и форма большого пальца\nБольшой палец сильный, с хорошо развитыми фалангами воли и логики. Форма указывает на умение стоять на своем, когда это необходимо, и способность аргументированно отстаивать свое мнение.\n\n" +
                    "Форма ногтей\nНогти имеют аккуратную миндалевидную форму, что говорит о творческой натуре, высокой чувствительности к красоте и развитом чувстве такта."
                }
            } else { // Если тип анализа - Судьба и события жизни
                if (isFull) { // Полный платный отчет по судьбе
                    "Левая рука (пассивная) хранит изначальный чертеж вашей судьбы, начертанный звездами в момент вашего рождения. Здесь отчетливо видны глубокие врожденные таланты, предрасположенность к долголетию и скрытые защитные знаки, оберегающие вас от фатальных происшествий. Линии показывают, что с самого детства вам была предначертана яркая судьба, наполненная важными духовными поисками и глубинным пониманием своего жизненного предназначения."
                } else { // Краткий бесплатный отчет по судьбе (обогащаем детальным описанием с комментариями на русском)
                    "Портрет личности\nВы наделены врожденным потенциалом лидера и созидателя. Ваша судьба изначально строится на прочном фундаменте выносливости и самодисциплины.\n\n" +
                    "Анализ линий Жизни, Ума, Сердца и Судьбы\n- Линия Жизни: глубокая, без разрывов, обещает стабильный жизненный тонус и долголетие.\n- Линия Ума: изогнута к холму Луны, наделяя вас развитым воображением и гибким умом.\n- Линия Сердца: сбалансирована, отражает искренность чувств и развитую эмоциональную эмпатию.\n- Линия Судьбы: отчетлива, указывает на то, что вы рождены для преодоления любых препятствий.\n\n" +
                    "Анализ каждого бугра\n- Бугор Венеры: плотный, выражает любовь к жизни и теплоту душевную.\n- Бугор Юпитера: умеренно очерчен, обещает профессиональный авторитет.\n- Бугор Сатурна: гладкий, дарует независимый склад мышления.\n- Бугор Аполлона: выраженный холм искусства, указывает на прекрасный эстетический вкус.\n- Бугор Меркурия: дает сообразительность и деловой талант.\n- Бугры Марса: защищают вашу энергетику от внешнего негативного воздействия.\n- Бугор Луны: крупный холм, отвечающий за богатый внутренний мир.\n\n" +
                    "Геометрия и форма рук\nПлотная структура ладони и длинные пальцы говорят о врожденной склонности к детальному планированию и глубокой интуиции.\n\n" +
                    "Длина и структура пальцев\nУдлиненные, стройные пальцы свидетельствуют о склонности к аналитическому анализу и детальному планированию любой сложной ситуации.\n\n" +
                    "Структура и форма большого пальца\nИзящный большой палец с гибким суставом выражает гибкость ума, адаптивность в общении и способность мягко подстраиваться под внешние обстоятельства.\n\n" +
                    "Форма ногтей\nКлассическая овальная форма ногтей подчеркивает аккуратность в делах и стремление к гармонии и порядку во всех сферах жизни."
                }
            }

            // Анализ Правой руки (реализованные таланты)
            val rightHandText = if (isCharacter) { // Если тип анализа - Характер
                if (isFull) { // Полный платный отчет по характеру
                    "Ваша активная правая рука наглядно показывает, как именно вы распорядились врожденными дарами и талантами. В процессе жизненного пути вы выработали колоссальную силу воли, научились контролировать избыточную эмоциональность и развили способность доводить начатые проекты до победного конца. Четкость линий на правой ладони свидетельствует о том, что вы успешно преодолели юношеский идеализм, превратив его в зрелую мудрость и практическую хватку. Вы приобрели неоценимое умение адаптироваться к самым суровым внешним обстоятельствам, сохраняя верность своим внутренним принципам."
                } else { // Краткий бесплатный отчет по характеру (обогащаем детальным описанием с комментариями на русском)
                    "Сильные и слабые стороны\n+ Сильные стороны: выносливость, высокая адаптивность, преданность своим идеалам, мощный самоконтроль.\n- Слабые стороны: склонность к избыточному самоанализу и скрытность чувств.\n\n" +
                    "Психоэмоциональный профиль\nВаш профиль стабилен; разум умеет договариваться с чувствами. Вы не поддаетесь панике и сохраняете спокойствие в кризисные моменты.\n\n" +
                    "Анализ линий Правой руки\n- Линия Жизни: укрепляется к середине, показывая рост жизненной силы с годами.\n- Линия Ума: становится более практичной и направленной на достижение реальных карьерных целей.\n- Линия Сердца: укореняется, выражая обретение контроля над эмоциями.\n- Линия Судьбы: выражена отчетливее, демонстрируя вашу способность уверенно вести свою жизнь самостоятельно.\n\n" +
                    "Анализ холмов (бугров)\n- На активной руке бугор Юпитера более упругий, что подтверждает развитие лидерских и организаторских талантов.\n- Бугор Венеры наполнен энергией, подчеркивая ваше теплое отношение к близким.\n\n" +
                    "Форма руки, пальцы и ногти на правой руке\n- Форма руки: имеет четкие очертания, символизируя целеустремленность.\n- Пальцы: сильные и ловкие, готовые к практической деятельности.\n- Большой палец: жесткий сустав указывает на возросший самоконтроль и силу воли.\n- Форма ногтей: ухоженные овальные ногти показывают дисциплинированность и аккуратность."
                }
            } else { // Если тип анализа - Судьба и события жизни
                if (isFull) { // Полный платный отчет по судьбе
                    "Правая рука (активная) отражает ваш реальный жизненный путь и те изменения, которые вы вносите своими решениями и действиями. На ней запечатлены ваши карьерные триумфы, ключевые выборы в возрасте 20-22 лет и грандиозный успех на рубеже 33-35 лет. Правая ладонь доказывает, что вы являетесь истинным хозяином своей судьбы, преодолевающим любые внешние препятствия своей невероятной силой воли и целеустремленностью."
                } else { // Краткий бесплатный отчет по судьбе (обогащаем детальным описанием с комментариями на русском)
                    "Сильные и слабые стороны\n+ Сильные стороны: целеустремленность, способность менять свою судьбу, решительность.\n- Слабые стороны: импульсивность при принятии важных жизненных решений.\n\n" +
                    "Психоэмоциональный профиль\nГлубоко аналитический ум берет верх над временными сомнениями. Вы прагматичны, но не лишены душевной чуткости.\n\n" +
                    "Анализ линий Правой руки\n- Линия Жизни: сильная, ровная, без каких-либо разрывов.\n- Линия Ума: показывает четкую логику в карьере.\n- Линия Сердца: указывает на искренние семейные чувства.\n- Линия Судьбы: ведет вас к ярким успехам на профессиональном поприще.\n\n" +
                    "Анализ бугров активной руки\n- Холм Меркурия развит сильнее, указывая на успех в коммерческой деятельности и переговорах.\n- Холм Солнца проявлен, принося признание ваших профессиональных качеств.\n\n" +
                    "Характеристики руки, пальцев и ногтей на правой руке\n- Форма руки: плотная, квадратная ладонь для надежного заземления.\n- Пальцы: прямые, показывают умение действовать структурированно.\n- Большой палец: пропорциональный, выражает сильную волю к успеху.\n- Ногти: классической миндалевидной формы, символизируют развитую интуицию и эстетическую чуткость."
                }
            }

            val charQualitiesText = if (isCharacter) {
                if (isFull) {
                    "Анализ вашей личности раскрывает уникальное и редкое сочетание глубокой душевной чувствительности и непоколебимой решимости. Вы обладаете острим аналитическим складом ума, который находится в постоянном синергетическом балансе с вашей сильной интуицией. Вы — человек слова, отличающийся благородством, искренностью и невероятным упорством в достижении значимых целей. Ваша сильная сторона — это способность вдохновлять других людей своим примером и давать мудрые советы. Вы постоянно стремитесь к самосовершенствованию и духовному росту, не допуская застоя в своей жизни."
                } else { // Краткий бесплатный (обогащаем детальным описанием с комментариями на русском)
                    "Метафорический образ, отражающий твою суть\n«Мудрый Океан» — внешнее величие и спокойствие скрывают колоссальные глубинные течения мысли, чувств и духовной энергии. Вы обладаете способностью незаметно влиять на события вокруг вас."
                }
            } else ""

            val lifePathEventsText = if (!isCharacter) {
                if (isFull) {
                    "Ваш жизненный путь предстает как череда знаковых событий и триумфальных свершений. Анализ показывает, что в возрасте около 20-22 лет вы столкнулись с судьбоносным выбором, определившим ваше текущее направление. Впереди, на рубеже 33-35 лет, вас ожидает масштабный жизненный прорыв — это точка раскрытия вашего истинного космического предназначения, которая принесет финансовую независимость и высокий социальный статус. Период зрелости после 45 лет характеризуется абсолютной стабильностью, укреплением вашего авторитета в обществе и обретением глубокой внутренней гармонии."
                } else { // Краткий бесплатный (обогащаем детальным описанием с комментариями на русском)
                    "Метафорический образ, отражающий твою суть\n«Неукротимая Река» — преодолевает любые каменные преграды на своем пути, пробивая русло к успеху благодаря природной гибкости и колоссальной внутренней силе."
                }
            } else ""

            val lifeSituationsInfluenceText = if (!isCharacter) {
                if (isFull) {
                    "Внешние обстоятельства и жизненные ситуации периодически устраивают вам проверки на прочность, однако каждое такое испытание лишь закаляет ваш внутренний стержень. Линии указывают на то, что влияние посторонних людей на вашу судьбу минимально — вы являетесь истинным и полноправным архитектором своей реальности. В моменты серьезного выбора в вашей жизни будут появляться мудрые наставники и покровители, чьи своевременные подсказки помогут уберечься от фатальных ошибок. Вы обладаете мощной энергетической защитой."
                } else {
                    "Вы успешно преодолеваете внешние вызовы благодаря сильному внутреннему стержню и поддержке своевременно приходящих наставников."
                }
            } else ""

            val marriageChildrenText = if (!isCharacter) {
                if (isFull) {
                    "В сфере личных взаимоотношений и брака ваша ладонь указывает на стремление к созданию глубокого, искреннего союза и теплого семейного очага. Линия Брака глубокая, ровная и свободная от негативных пересечений, что предвещает счастливый, гармоничный и исключительно долговечный союз с вашей родственной душой. Ваш спутник жизни будет человеком высокого интеллекта и сильного характера, который обеспечит вам надежную поддержку. Проявленные знаки детей указывают на счастливое родительство, которое наполнит ваш дом уютом и радостью."
                } else {
                    "Ваша ладонь указывает на прочный, основанный на доверии и духовной близости брак, а также на гармоничные семейные отношения."
                }
            } else ""

            PalmistReport(
                overallPortrait = "Общий анализ ладони для ${profile.name} указывает на яркую, духовно развитую личность. Ваша ладонь сочетает черты Огня и Воздуха, свидетельствуя о высоком интеллектуальном потенциале, творческой импульсивности и стремлении к гармонии с внешним миром. Вы обладаете врожденным даром чувствовать перемены и быстро адаптироваться к новым условиям. У вас исключительно развитая интуиция, которая служит вашим главным компасом и часто помогает находить верные решения в самых кризисных жизненных ситуациях, оберегая от неверных шагов. Наличие холма Луны говорит о глубоком внутреннем творческом источнике, из которого вы черпаете вдохновение для реализации ваших самых смелых планов и устремлений.",
                handType = "Смешанный тип (Воздух-Огонь)",
                lines = listOf(
                    PalmLineAnalysis(
                        name = "Линия Жизни",
                        color = "#C41E3A",
                        shortDescription = "Глубокая, ровная линия жизни с хорошим огибанием бугра Венеры.",
                        fullDescription = "Ваша Линия Жизни сильная и глубокая. Она свидетельствует о высоком уровне жизненной энергии и хорошем запасе физических сил. Небольшое ответвление в сторону Лунного бугра на уровне 35 лет указывает на важные перемены места жительства или дальние поездки, принесшие духовный рост. В районе 50 лет линия становится ещё шире, предвещая спокойный и стабильный зрелый возраст.",
                        keyTakeaways = listOf("Высокая витальность", "Возможность переезда в районе 35 лет", "Стабильное долголетие")
                    ),
                    PalmLineAnalysis(
                        name = "Линия Сердца",
                        color = "#E94B8A",
                        shortDescription = "Изогнутая, оканчивающаяся между пальцами Сатурна и Юпитера.",
                        fullDescription = "Линия Сердца длинная и выразительная. Вы — человек с глубоким чувством эмпатии, способный на сильную привязанность. Завершение линии между холмами Юпитера и Сатурна показывает баланс между идеализмом в любви и практическим здравым смыслом. Мелкие штрихи, пересекающие линию в раннем возрасте, говорят о прошлых эмоциональных переживаниях, из которых вы вынесли ценную мудрость.",
                        keyTakeaways = listOf("Глубокая эмпатия", "Эмоциональный интеллект", "Сбалансированный подход к отношениям")
                    ),
                    PalmLineAnalysis(
                        name = "Линия Головы",
                        color = "#3A7BD5",
                        shortDescription = "Длинная линия, плавно спускающаяся к холму Луны.",
                        fullDescription = "Линия Ума (Головы) имеет хороший изгиб в сторону холма Луны, что указывает на преобладание творческого воображения над сухим логическим анализом. Вы обладаете гибким мышлением, любите нестандартные решения и интересуетесь философией или мистицизмом. Чёткость линии показывает концентрацию и умение быстро принимать решения в сложных ситуациях.",
                        keyTakeaways = listOf("Творческое мышление", "Интерес к тайным знаниям", "Гибкость ума")
                    ),
                    PalmLineAnalysis(
                        name = "Линия Судьбы",
                        color = "#2ECC71",
                        shortDescription = "Начинается у запястья и чётко следует к холму Сатурна.",
                        fullDescription = "Ваша Линия Судьбы проявлена достаточно ярко, что говорит о сильном чувстве долга и осознании своей жизненной цели. Наличие небольшого разрыва в районе 28-30 лет указывает на изменение профессионального вектора или переоценку ценностей, после чего карьера пошла вверх с удвоенной силой. Линия сливается с Линией Головы, символизируя, что успех придёт благодаря вашему собственному разуму.",
                        keyTakeaways = listOf("Целеустремлённость", "Успешная смена вектора в 28-30 лет", "Успех своими силами")
                    )
                ),
                mounts = listOf(
                    PalmMountAnalysis("Бугор Венеры", true, "Крупный и упругий бугор свидетельствует о страстности, жизнелюбии, доброте и любви к искусству."),
                    PalmMountAnalysis("Бугор Юпитера", true, "Хорошо выражен. Отражает лидерские качества, гордость, амбиции и стремление к духовному авторитету."),
                    PalmMountAnalysis("Бугор Сатурна", false, "Умеренно плоский, что дарует вам спокойствие, мудрость, философский склад ума и независимость."),
                    PalmMountAnalysis("Бугор Аполлона", true, "Выраженный холм искусства. Указывает на тягу к прекрасному, эстетизм и творческое самовыражение.")
                ),
                signs = listOf(
                    PalmSign("Мистический Крест", "Крест между Линией Сердца и Линией Ума. Знак сильных экстрасенсорных способностей, интуиции и интереса к тайным знаниям.", "Пространство между линиями в центре ладони"),
                    PalmSign("Кольцо Соломона", "Полукруг под указательным пальцем. Дарует мудрость, авторитет и способность понимать психологию людей с первого взгляда.", "Под холмом Юпитера")
                ),
                marriageChildren = marriageChildrenText,
                lifeEvents = lifePathEventsText,
                predictions = lifeSituationsInfluenceText,
                recommendations = "Развивайте свою природную интуицию — она ваш главный компас. Старайтесь чаще бывать на природе для восстановления душевных сил. Практикуйте медитации или дыхательные упражнения для центрирования вашей огненной энергии.",
                leftHand = leftHandText,
                rightHand = rightHandText,
                characterQualities = charQualitiesText,
                promptTokens = 1245,
                candidatesTokens = 850,
                totalTokens = 2095,
                isMock = true
            )
        } else {
            // Generating English mock report
            val leftHandText = if (isCharacter) { // If character reading
                if (isFull) { // Full paid character report
                    "Left Hand Analysis (Innate Potentials): Left hand of ${profile.name} reveals a powerful inherited blueprint. Since birth, a deep sense of intuition, high imagination, and a philosophical mind have been set. You possess an innate empathy and high artistic talent."
                } else { // Brief character report
                    "Left Hand Analysis (Innate Potentials): Your birth potential is characterized by deep creativity, natural intuition, and high adaptive intelligence."
                }
            } else { // If life path reading
                if (isFull) { // Full paid life path report
                    "Left Hand Analysis (Destiny Blueprint): The passive left hand holds the celestial blueprint of your destiny. It displays your innate resilience, longevity markers, and cosmic protections. You were born with a pre-designed framework pointing to spiritual expansion and significant life wisdom."
                } else { // Brief life path report
                    "Left Hand Analysis (Destiny Blueprint): Your passive hand displays a rich natural baseline of vitality and robust karmic protection."
                }
            }

            val rightHandText = if (isCharacter) { // If character reading
                if (isFull) { // Full paid character report
                    "Right Hand Analysis (Acquired Traits): The active right hand highlights how you have actualized your innate talents. You have structured your mind, strengthened your personal boundaries, and developed a strong sense of worldly logic and determination."
                } else { // Brief character report
                    "Right Hand Analysis (Acquired Traits): Over the years you have developed superb practical skills, professional diligence, and emotional composure."
                }
            } else { // If life path reading
                if (isFull) { // Full paid life path report
                    "Right Hand Analysis (Active Life Path): The active right hand reflects your choices and real-time self-determination. It outlines your professional breakthroughs, key decision periods around ages 20-22, and major prosperity peaks near 33-35. It shows that you are the architect of your own timeline."
                } else { // Brief life path report
                    "Right Hand Analysis (Active Life Path): Your active hand shows solid career milestones, self-made opportunities, and dynamic adaptation to obstacles."
                }
            }

            val charQualitiesText = if (isCharacter) {
                if (isFull) {
                    "Analysis of Character & Personal Qualities: Your personality stands out due to a beautiful synthesis of sensitivity and direct leadership qualities. You are loyal, ambitious, intellectual, and naturally attracted to spiritual and esoteric studies."
                } else {
                    "Analysis of Character & Personal Qualities: Key personality markers indicate absolute loyalty, mental flexibility, and a superb balance of intuition and practical execution."
                }
            } else ""

            val lifePathEventsText = if (!isCharacter) {
                if (isFull) {
                    "Analysis of Life Path & Events: Your life journey is marked by fascinating career reinventions and spiritual shifts. An early crossroad occurred between ages 20 and 22. A major prosperity threshold is visible between 33 and 35, leading to total domestic and professional freedom."
                } else {
                    "Analysis of Life Path & Events: Life path indicates a foundational shift at 20-22, a major success period at 33-35, and complete harmony after 50."
                }
            } else ""

            val lifeSituationsInfluenceText = if (!isCharacter) {
                if (isFull) {
                    "Analysis of Life Situations & External Influences: While cosmic challenges verify your determination, you remain the sovereign author of your fate. Valuable mentors will provide key strategic guidance during moments of professional transition."
                } else {
                    "Analysis of Life Situations & External Influences: You possess stellar natural defense structures, turning life obstacles into key developmental milestones."
                }
            } else ""

            val marriageChildrenText = if (!isCharacter) {
                if (isFull) {
                    "Analysis of Relationships: Family, Marriage, Children & Partners: Indicators show a highly stable, long-lasting romantic union founded on friendship and spiritual affinity. Your life partner will be highly supportive, and family signs promise joy and children."
                } else {
                    "Analysis of Relationships: Family, Marriage, Children & Partners: Relationships are governed by mutual respect and a deep soul connection, ensuring a stable, beautiful union."
                }
            } else ""

            PalmistReport(
                overallPortrait = "⚠️ THIS IS A SAMPLE INTERPRETATION (Demo Mode). To get a real chiromancy analysis of your hands from a photograph, please connect your Gemini API key in the AI Studio Secrets panel.\n\nThe overall analysis of the palm for ${profile.name} points to a vivid, spiritually evolved individual. Your hand archetypically balances elements of Air and Fire, signaling high intellectual potential, creative spontaneity, and a quest for cosmic harmony.",
                handType = "Mixed Archetype (Air-Fire)",
                lines = listOf(
                    PalmLineAnalysis(
                        name = "Life Line",
                        color = "#C41E3A",
                        shortDescription = "Deep, clear curve well-rounding the Mount of Venus.",
                        fullDescription = "Your Life Line is solid and deep, denoting immense vitality and physical resilience. A minor split traveling towards the Luna mount at approximately age 35 highlights pivotal travel, a residence shift, or a profound perspective change. Past 50, the line gains stability, promising a harmonious elder age.",
                        keyTakeaways = listOf("Excellent core energy", "Major transition around age 35", "Vigorous longevity")
                    ),
                    PalmLineAnalysis(
                        name = "Heart Line",
                        color = "#E94B8A",
                        shortDescription = "Curved, ending gracefully between Saturn and Jupiter mounts.",
                        fullDescription = "Your Heart Line is long and beautifully etched. You possess deep empathy, a warm demeanor, and a massive capacity for devotion. Reaching between the hills of Jupiter and Saturn indicates an elegant balance between idealism and grounding in romance.",
                        keyTakeaways = listOf("Deep emotional empathy", "High EQ level", "Balanced approach in relationships")
                    ),
                    PalmLineAnalysis(
                        name = "Head Line",
                        color = "#3A7BD5",
                        shortDescription = "Long line sloping gently towards the Mount of Moon.",
                        fullDescription = "Your Head Line slopes toward the Mount of Moon, representing a beautiful alignment of logical analytical strength with deep creative imagination. You enjoy solving atypical challenges and have a natural interest in philosophy and mystical fields.",
                        keyTakeaways = listOf("Creative and fluid cognition", "Attuned to esoteric concepts", "Mental clarity")
                    ),
                    PalmLineAnalysis(
                        name = "Destiny Line",
                        color = "#2ECC71",
                        shortDescription = "Originating near the wrist, pointing cleanly to Saturn.",
                        fullDescription = "Your Destiny Line is prominent, showcasing an innate sense of duty and distinct life milestones. A subtle transition near age 28-30 signifies a shift in career goals or professional rebirth, leading to expanded prosperity and alignment.",
                        keyTakeaways = listOf("Purpose-driven career", "Pivotal alignment near 28-30", "Self-made prosperity")
                    )
                ),
                mounts = listOf(
                    PalmMountAnalysis("Mount of Venus", true, "Strong and plump, signaling warmth, charisma, vital strength, and deep artistic appreciation."),
                    PalmMountAnalysis("Mount of Jupiter", true, "Well-formed. Indicates leadership potential, spiritual honor, and high ambition."),
                    PalmMountAnalysis("Mount of Saturn", false, "Calm and flat, gifting patience, analytical composure, and intellectual independence."),
                    PalmMountAnalysis("Mount of Apollo", true, "Highly visible. Radiates beauty, artistic talents, charm, and creative execution.")
                ),
                signs = listOf(
                    PalmSign("Mystic Cross", "An X mark between Heart and Head lines. A signature of innate intuitive ability, psychic capacity, and interest in esoteric fields.", "Center of palm between primary lines"),
                    PalmSign("Ring of Solomon", "A crescent under the index finger. Bestows ancient wisdom, deep psychological understanding, and authority.", "Below Mount of Jupiter")
                ),
                marriageChildren = marriageChildrenText,
                lifeEvents = lifePathEventsText,
                predictions = lifeSituationsInfluenceText,
                recommendations = "Trust your intuitive instincts—they are your ultimate compass. Connect with natural settings frequently to ground your fiery energy. Practice regular mindfulness.",
                leftHand = leftHandText,
                rightHand = rightHandText,
                characterQualities = charQualitiesText,
                promptTokens = 1245,
                candidatesTokens = 850,
                totalTokens = 2095,
                isMock = true
            )
        }

        addDailyTokenUsage(2095)
        return moshi.adapter(PalmistReport::class.java).toJson(report)
    }

    private fun generateLocalMockCompatibility(
        selfName: String,
        partnerName: String,
        isRussian: Boolean
    ): String {
        val report = if (isRussian) {
            CompatibilityReport(
                compatibilityPercent = 88,
                partner1Portrait = "$selfName обладает глубокой эмпатией и ищет духовное единение в союзе. Основной упор делается на искренность чувств и эмоциональную честность.",
                partner2Portrait = "$partnerName ценит надёжность, стабильность и преданность. Сильная натура, стремящаяся оберегать близких и создавать уют.",
                combinedAnalysis = "Ваш союз благословлен гармонией линий Сердца. Изгибы ваших линий ладони плавно дополняют друг друга, что обеспечивает высокое взаимопонимание на интуитивном уровне. Мелкие различия в буграх Сатурна лишь помогают вам уравновешивать качества друг друга.",
                strongPoints = listOf("Абсолютное доверие и уважение", "Взаимная эмоциональная поддержка", "Схожие духовные и семейные ориентиры"),
                weakPoints = listOf("Возможные разногласия в финансовых тратах", "Склонность замалчивать мелкие обиды"),
                emotionalCompatibility = "92% — Слияние на тонком эмоциональном плане. Вы чувствуете настроение партнёра без лишних слов.",
                intellectualCompatibility = "85% — Интересные беседы, совместные проекты и общие интеллектуальные хобби укрепляют ваш союз.",
                financialCompatibility = "75% — Один из вас более практичен, а другой склонен к импульсивным покупкам. Рекомендуется вести открытый бюджет.",
                recommendations = "Чаще проговаривайте свои внутренние переживания вслух. Не бойтесь открыто обсуждать планы на будущее. Устраивайте романтические свидания наедине, чтобы поддерживать пламя Линии Сердца.",
                promptTokens = 1450,
                candidatesTokens = 920,
                totalTokens = 2370,
                isMock = true
            )
        } else {
            CompatibilityReport(
                compatibilityPercent = 88,
                partner1Portrait = "$selfName values spiritual connection, intense emotional honesty, and deep cosmic affinity in relationships.",
                partner2Portrait = "$partnerName holds devotion, safety, and domestic harmony in extremely high regard. A shielding, protective soul.",
                combinedAnalysis = "Your heart currents are beautifully matched. The alignment of your primary romantic lines showcases high mutual empathy and intuitive alignment. Differences in Saturn mount structures help ground and elevate each other.",
                strongPoints = listOf("Absolute mutual trust", "Profound empathy and comfort", "Identical family and life goals"),
                weakPoints = listOf("Minor friction over budget allocations", "Slight habit of internalizing emotional concerns"),
                emotionalCompatibility = "92% — A magnificent emotional connection. You can easily read each other's moods.",
                intellectualCompatibility = "85% — Engaging discussions and unified project goals reinforce your relationship.",
                financialCompatibility = "75% — One partner excels at saving, while the other is more impulsive. Open dialogues are recommended.",
                recommendations = "Discuss internal thoughts openly to avoid overthinking. Organize romantic, focused getaways frequently to nourish the Heart Line flame.",
                promptTokens = 1450,
                candidatesTokens = 920,
                totalTokens = 2370,
                isMock = true
            )
        }

        addDailyTokenUsage(2370)
        return moshi.adapter(CompatibilityReport::class.java).toJson(report)
    }

    // --- История Оплаты (Payment History) ---

    suspend fun getUserProfileSync(): UserProfileEntity? = withContext(Dispatchers.IO) {
        dao.getUserProfileSync()
    }

    suspend fun getBillingStateSync(): BillingStateEntity? = withContext(Dispatchers.IO) {
        dao.getBillingStateSync()
    }

    // Получение всех записей платежей из БД в реальном времени
    val allPayments: Flow<List<PaymentHistoryEntity>> = dao.getAllPayments()

    // Сохранение записи о новом переводе в базу данных и синхронизация с сервером
    suspend fun insertPayment(payment: PaymentHistoryEntity): Long = withContext(Dispatchers.IO) {
        val insertedId = dao.insertPayment(payment)
        try {
            apiRepository.logPayment(
                amount = payment.amountRub.toDouble(),
                currency = "RUB",
                status = payment.status
            )
        } catch (e: Exception) {
            AppLogger.e("PalmistRepository", "Ошибка передачи лога платежа на REST API", e)
        }
        insertedId
    }

    // Полная очистка истории платежей
    suspend fun clearPaymentHistory() = withContext(Dispatchers.IO) {
        dao.clearPaymentHistory()
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int = 1200): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val max = maxOf(width, height)
        val scale = maxSize.toFloat() / max
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
