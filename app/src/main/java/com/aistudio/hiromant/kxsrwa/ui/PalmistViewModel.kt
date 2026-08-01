package com.aistudio.hiromant.kxsrwa.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.hiromant.kxsrwa.PalmistApplication
import com.aistudio.hiromant.kxsrwa.data.local.BillingStateEntity
import com.aistudio.hiromant.kxsrwa.data.local.ReadingEntity
import com.aistudio.hiromant.kxsrwa.data.local.UserProfileEntity
import com.aistudio.hiromant.kxsrwa.ui.language.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PalmistViewModel(application: Application) : AndroidViewModel(application) {

    // Инициализация репозитория через PalmistApplication для доступа к данным и API
    private val repository = (application as PalmistApplication).repository

    // Состояние выбранного языка приложения (по умолчанию установлен русский)
    private val _selectedLanguage = MutableStateFlow(AppLanguage.RUS)
    // Публичный поток выбранного языка для наблюдения во Compose-компонентах
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage

    // Состояние масштаба шрифта интерфейса
    private val _fontScale = MutableStateFlow(1.0f)
    // Публичный поток масштаба шрифта для наблюдения
    val fontScale: StateFlow<Float> = _fontScale

    // Состояние активности озвучивания текста (TTS)
    private val _ttsEnabled = MutableStateFlow(true)
    // Публичный поток статуса активности озвучивания
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled

    // Выбранный пол голоса озвучки (женский/мужской)
    private val _ttsGender = MutableStateFlow("Female")
    // Публичный поток пола голоса озвучки
    val ttsGender: StateFlow<String> = _ttsGender

    // Индекс выбранного голоса в системе TTS
    private val _ttsVoiceIndex = MutableStateFlow(0)
    // Публичный поток индекса голоса в системе
    val ttsVoiceIndex: StateFlow<Int> = _ttsVoiceIndex

    // Скорость озвучивания текста синтезатором речи
    private val _ttsSpeechRate = MutableStateFlow(1.0f)
    // Публичный поток скорости воспроизведения озвучки
    val ttsSpeechRate: StateFlow<Float> = _ttsSpeechRate

    // Высота тона речи озвучки
    private val _ttsPitch = MutableStateFlow(1.0f)
    // Публичный поток высоты тона озвучки
    val ttsPitch: StateFlow<Float> = _ttsPitch

    // Профиль текущего пользователя из базы данных
    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile.stateIn(
        scope = viewModelScope, // Область жизненного цикла ViewModel
        started = SharingStarted.WhileSubscribed(5000), // Сохранение при временной потере подписчиков
        initialValue = null // Изначальное значение до загрузки из БД
    )

    // Все сохраненные профили пользователей
    val allUserProfiles: StateFlow<List<UserProfileEntity>> = repository.allUserProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            val active = profile.copy(id = "current_user")
            repository.saveUserProfile(
                name = active.name,
                gender = active.gender,
                age = active.age,
                height = active.height,
                dominantHand = active.dominantHand,
                email = active.email,
                phone = active.phone,
                isRegistered = active.isRegistered,
                photoUri = active.photoUri
            )
        }
    }

    // Суточная статистика использования токенов Gemini API
    val dailyTokensUsed = MutableStateFlow(repository.getDailyTokenUsage())
    val dailyTokensRemaining = MutableStateFlow(repository.getDailyTokensRemaining())

    fun refreshDailyTokens() {
        dailyTokensUsed.value = repository.getDailyTokenUsage()
        dailyTokensRemaining.value = repository.getDailyTokensRemaining()
    }

    fun getDailyTokenUsage(): Int = repository.getDailyTokenUsage()
    fun getDailyTokensRemaining(): Int = repository.getDailyTokensRemaining()
    fun getDailyTokenQuota(): Int = repository.getDailyTokenQuota()

    // Текущий статус биллинга (лимиты и баланс анализов)
    val billingState: StateFlow<BillingStateEntity?> = repository.billingState.stateIn(
        scope = viewModelScope, // Область жизненного цикла ViewModel
        started = SharingStarted.WhileSubscribed(5000), // Подписка с задержкой отписки в 5 секунд
        initialValue = null // Изначальное значение пустое
    )

    // Список всех сеансов анализа, сохраненных в истории
    val allReadings: StateFlow<List<ReadingEntity>> = repository.allReadings.stateIn(
        scope = viewModelScope, // Область сопрограмм ViewModel
        started = SharingStarted.WhileSubscribed(5000), // Оптимальная стратегия подписки
        initialValue = emptyList() // Начальное значение - пустой список
    )

    // Поток всех платежей пользователя из базы данных для отображения на экране "Кабинет"
    val allPayments: StateFlow<List<com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity>> = repository.allPayments.stateIn(
        scope = viewModelScope, // Сопрограммы привязаны к ViewModel
        started = SharingStarted.WhileSubscribed(5000), // Автоматическое отключение при фоне
        initialValue = emptyList() // По умолчанию пустой список
    )

    // Состояние процесса анализа (true, когда идет отправка/обработка)
    val isAnalyzing = MutableStateFlow(false)
    // Числовой прогресс выполнения анализа от 0 до 100 процентов
    val analysisProgress = MutableStateFlow(0)
    // Текстовый статус текущего шага анализа для пользователя
    val analysisStatus = MutableStateFlow("")

    // Текущий выбранный результат анализа ладони
    val currentReading = MutableStateFlow<ReadingEntity?>(null)
    // Текущий выбранный результат анализа совместимости партнеров
    val currentCompatibilityReading = MutableStateFlow<ReadingEntity?>(null)
    // Выбранный индекс дополнительного вопроса/темы для целевого перехода со страницы Кабинета по ТЗ
    val targetFollowUpTopicIndex = MutableStateFlow<Int?>(null)
    // Режим фильтрации интерпретаций для совместимости ("all", "brief", "full")
    val compatibilityFilterMode = MutableStateFlow<String>("all")

    // Кэшированные изображения ладоней для экрана загрузки (левая ладонь)
    val bitmapLeftPalm = MutableStateFlow<Bitmap?>(null)
    // Кэшированные изображения ладоней для экрана загрузки (тыльная сторона левой руки)
    val bitmapLeftBack = MutableStateFlow<Bitmap?>(null)
    // Кэшированные изображения ладоней для экрана загрузки (правая ладонь)
    val bitmapRightPalm = MutableStateFlow<Bitmap?>(null)
    // Кэшированные изображения ладоней для экрана загрузки (тыльная сторона правой руки)
    val bitmapRightBack = MutableStateFlow<Bitmap?>(null)

    // Пути к сохраненным файлам изображений (левая ладонь)
    val leftPalmPath = MutableStateFlow<String?>(null)
    // Пути к сохраненным файлам изображений (тыльная сторона левой руки)
    val leftBackPath = MutableStateFlow<String?>(null)
    // Пути к сохраненным файлам изображений (правая ладонь)
    val rightPalmPath = MutableStateFlow<String?>(null)
    // Пути к сохраненным файлам изображений (тыльная сторона правой руки)
    val rightBackPath = MutableStateFlow<String?>(null)

    // Кэш снимка большого пальца
    val bitmapThumb = MutableStateFlow<Bitmap?>(null)
    // Кэш снимка ребра ладони
    val bitmapEdge = MutableStateFlow<Bitmap?>(null)

    // Метод сброса загруженных материалов (фотографий и видео) для нового пользователя
    fun resetUploadState() {
        bitmapLeftPalm.value = null
        bitmapLeftBack.value = null
        bitmapRightPalm.value = null
        bitmapRightBack.value = null
        leftPalmPath.value = null
        leftBackPath.value = null
        rightPalmPath.value = null
        rightBackPath.value = null
        bitmapThumb.value = null
        bitmapEdge.value = null
        videoUri.value = null
    }
    // Путь к файлу снимка большого пальца
    val thumbPath = MutableStateFlow<String?>(null)
    // Путь к файлу снимка ребра ладони
    val edgePath = MutableStateFlow<String?>(null)

    // Ссылки на видеозаписи
    val videoUri = MutableStateFlow<android.net.Uri?>(null)
    val leftVideoUri = MutableStateFlow<android.net.Uri?>(null)
    val rightVideoUri = MutableStateFlow<android.net.Uri?>(null)
    // Выбранный тип анализа (по умолчанию brief_char)
    val currentAnalysisTypeState = MutableStateFlow("brief_char")
    // Флаг показа экрана интерпретации результатов
    val showInterpretationScreen = MutableStateFlow(false)

    // Активный раздел нижней панели навигации (по умолчанию upload)
    val activeTab = MutableStateFlow("upload")

    // Флаг возврата из программы на экран заставки
    val isReturnedToSplash = MutableStateFlow(false)

    // Предустановленная сумма платежа для быстрой поддержки проекта
    val paymentAmountToPreselect = MutableStateFlow("250")

    // --- Remote DB Configuration States ---
    val remoteDbHost = MutableStateFlow("")
    val remoteDbName = MutableStateFlow("")
    val remoteDbUser = MutableStateFlow("")
    val remoteDbPassword = MutableStateFlow("")
    val remoteDbStatus = MutableStateFlow("Готов к подключению")

    init {
        // Чтение ранее выбранного пользователем языка интерфейса при запуске приложения
        val code = repository.getSelectedLanguage()
        // Нахождение соответствующего перечисления языка по его коду
        val lang = AppLanguage.values().find { it.code == code } ?: AppLanguage.RUS
        _selectedLanguage.value = lang

        // Загрузка ранее настроенного масштаба шрифта
        _fontScale.value = repository.getFontScale()

        // Загрузка начальных параметров озвучивания (TTS) из SharedPreferences
        _ttsEnabled.value = repository.getTtsEnabled()
        _ttsGender.value = repository.getTtsGender()
        _ttsVoiceIndex.value = repository.getTtsVoiceIndex()
        _ttsSpeechRate.value = repository.getTtsSpeechRate()
        _ttsPitch.value = repository.getTtsPitch()

        // Загрузка настроек подключения к удаленной БД
        remoteDbHost.value = repository.getRemoteDbHost()
        remoteDbName.value = repository.getRemoteDbName()
        remoteDbUser.value = repository.getRemoteDbUser()
        remoteDbPassword.value = repository.getRemoteDbPassword()

        // Автоматическое создание дефолтного профиля пользователя при старте
        viewModelScope.launch {
            val currentProfile = repository.getUserProfileSync()
            if (currentProfile == null || currentProfile.name.isBlank()) {
                repository.saveUserProfile(
                    name = "Максим", // Имя по умолчанию (Максим)
                    gender = "Male", // Пол по умолчанию (Мужской)
                    age = 44, // Возраст по умолчанию (44 года, 1982 г.р.)
                    height = 175, // Рост по умолчанию (175 см)
                    dominantHand = "Right" // Ведущая рука (Правая)
                )
            }
        }
    }

    fun saveRemoteDbConfig(host: String, dbName: String, user: String, pass: String) {
        remoteDbHost.value = host
        remoteDbName.value = dbName
        remoteDbUser.value = user
        remoteDbPassword.value = pass
        repository.setRemoteDbHost(host)
        repository.setRemoteDbName(dbName)
        repository.setRemoteDbUser(user)
        repository.setRemoteDbPassword(pass)
        remoteDbStatus.value = "Настройки БД сохранены"
    }

    fun testRemoteDbConnection() {
        viewModelScope.launch {
            remoteDbStatus.value = "Проверка соединения..."
            delay(800)
            if (remoteDbHost.value.isNotBlank() && remoteDbUser.value.isNotBlank()) {
                remoteDbStatus.value = "Успешное подключение к БД (${remoteDbHost.value})"
            } else {
                remoteDbStatus.value = "Ошибка: укажите адрес и логин БД"
            }
        }
    }

    fun syncWithRemoteDb() {
        viewModelScope.launch {
            remoteDbStatus.value = "Синхронизация данных..."
            val success = repository.syncLocalDataWithRemoteDb()
            if (success) {
                remoteDbStatus.value = "Данные синхронизированы с БД"
            } else {
                remoteDbStatus.value = "Ошибка синхронизации БД"
            }
        }
    }

    // Метод изменения масштаба шрифта приложения
    fun changeFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.8f, 1.6f) // Обеспечение допустимых границ размера
        _fontScale.value = clamped // Сохранение локально
        repository.setFontScale(clamped) // Запись в SharedPreferences
    }

    // Метод переключения языка интерфейса приложения
    fun changeLanguage(lang: AppLanguage) {
        _selectedLanguage.value = lang // Применение в потоке данных
        repository.setSelectedLanguage(lang.code) // Запись в постоянные настройки
    }

    // Метод проверки выбора языка (для обхода приветственного экрана)
    fun isLanguageSelected(): Boolean {
        return repository.isLanguageSelected() // Чтение статуса выбора из репозитория
    }

    // Пометка о том, что язык успешно выбран пользователем
    fun markLanguageSelected() {
        repository.setLanguageSelected(true) // Сохранение флага в SharedPreferences
    }

    // --- Действия по настройке синтезатора речи TTS ---

    // Изменение статуса включения голосового сопровождения в приложении
    fun changeTtsEnabled(enabled: Boolean) {
        _ttsEnabled.value = enabled // Применение локально во ViewModel
        repository.setTtsEnabled(enabled) // Запись значения в постоянные настройки
    }

    // Изменение пола голоса озвучки (женский/мужской)
    fun changeTtsGender(gender: String) {
        _ttsGender.value = gender // Обновление локального состояния во ViewModel
        repository.setTtsGender(gender) // Запись настройки пола в репозиторий для сохранения
    }

    // Изменение индекса голоса в системе TTS
    fun changeTtsVoiceIndex(index: Int) {
        _ttsVoiceIndex.value = index // Обновление локального значения индекса голоса
        repository.setTtsVoiceIndex(index) // Запись индекса голоса в постоянные настройки
    }

    // Изменение скорости озвучивания текста
    fun changeTtsSpeechRate(rate: Float) {
        _ttsSpeechRate.value = rate // Обновление скорости речи во ViewModel
        repository.setTtsSpeechRate(rate) // Запись скорости в постоянную конфигурацию
    }

    // Изменение высоты тона озвучивания
    fun changeTtsPitch(pitch: Float) {
        _ttsPitch.value = pitch // Обновление высоты тона речи во ViewModel
        repository.setTtsPitch(pitch) // Запись высоты тона в настройки репозитория
    }

    // --- Действия с профилем пользователя ---

    // Метод регистрации/синхронизации профиля пользователя на удаленном REST API сервере
    fun registerUserOnServer(
        username: String,
        email: String? = null,
        phone: String? = null,
        birthYear: Int? = null,
        height: Int? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.apiRepository.registerUser(
                username = username.ifBlank { "Максим" },
                email = email,
                phone = phone,
                birthYear = birthYear,
                height = height
            )
            val success = res is com.aistudio.hiromant.kxsrwa.utils.ResultWrapper.Success
            onComplete(success)
        }
    }

    // Метод сохранения личных данных профиля пользователя в локальную базу данных
    fun saveProfile(
        name: String, // Имя пользователя
        gender: String, // Пол пользователя
        age: Int, // Возраст пользователя
        height: Int, // Рост пользователя
        dominantHand: String, // Активная (доминантная) рука
        email: String? = null, // Электронная почта (опционально)
        phone: String? = null, // Номер телефона (опционально)
        isRegistered: Boolean = false // Флаг регистрации пользователя
    ) {
        viewModelScope.launch { // Запуск в контексте сопрограмм жизненного цикла
            // Вызов метода репозитория для сохранения сущности профиля в локальную БД
            repository.saveUserProfile(name, gender, age, height, dominantHand, email, phone, isRegistered)
            // Автоматическая синхронизация и получение user_id с REST API сервера
            val birthYearVal = if (age > 0) (java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - age) else null
            registerUserOnServer(name, email, phone, birthYearVal, height)
        }
    }

    // Дополнительный метод сохранения профиля пользователя с указанием аватара
    fun saveUserProfile(
        name: String, // Имя пользователя
        gender: String, // Пол пользователя
        age: Int, // Возраст пользователя
        height: Int, // Рост пользователя
        dominantHand: String, // Ведущая рука
        email: String? = null, // Почта
        phone: String? = null, // Телефон
        isRegistered: Boolean = false, // Флаг регистрации
        photoUri: String? = null // Фотография профиля
    ) {
        viewModelScope.launch { // Сопрограмма в ViewModelScope
            repository.saveUserProfile(name, gender, age, height, dominantHand, email, phone, isRegistered, photoUri)
        }
    }

    fun updateUserAvatar(photoUri: String) {
        viewModelScope.launch {
            repository.updateUserAvatar(photoUri)
        }
    }

    // --- Действия биллинга (Имитация платежей для тестирования, полностью обновляет состояние БД!) ---

    // Обработка успешной оплаты и автоматическое начисление Полных и Кратких интерпретаций
    fun processPaymentSuccess(amount: Int, method: String = "ЮKassa / ЮMoney") {
        viewModelScope.launch { // Запуск фоновой задачи обработки платежа во ViewModelScope
            val fullCount = amount / 250 // Рассчитываем количество Полных Интерпретаций (1 Полная = 250 руб)
            val remainder = amount % 250 // Вычисляем остаток суммы в рублях
            val briefCount = remainder / 100 // Рассчитываем количество Кратких Интерпретаций (1 Краткая = 100 руб)

            // Если сумма платежа менее 100 рублей, но платеж прошел, начисляем минимум 1 Краткую интерпретацию
            val finalBriefCount = if (fullCount == 0 && briefCount == 0 && amount > 0) 1 else briefCount

            if (fullCount > 0) {
                repository.addPaidAnalyses(fullCount) // Начисляем полученные Полные Интерпретации в базу данных
            }
            if (finalBriefCount > 0) {
                repository.addFreeAnalyses(finalBriefCount) // Начисляем полученные Краткие Интерпретации в базу данных
            }

            // Формируем текстовое описание транзакции для истории
            val descParts = mutableListOf<String>()
            if (fullCount > 0) descParts.add("Полные: $fullCount")
            if (finalBriefCount > 0) descParts.add("Краткие: $finalBriefCount")
            val desc = if (descParts.isNotEmpty()) descParts.joinToString(", ") else "Поддержка проекта ($amount р.)"

            // Создаем сущность истории платежа
            val payment = com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity(
                amountRub = amount, // Сохраняем оплаченную сумму
                paymentSystem = method, // Сохраняем выбранную платёжную систему
                status = "Успешно", // Устанавливаем успешный статус проведения
                readingType = "Пополнение: $desc" // Записываем подробное описание начисленных услуг
            )
            repository.insertPayment(payment) // Вставляем запись истории платежа в локальную база данных Room
        }
    }

    // Имитация покупки премиум подписки (начисление 10 сеансов анализов в БД)
    fun simulateBuySubscription(amount: Int = 999, method: String = "Google Play (Встроенная)") {
        viewModelScope.launch { // Запуск асинхронной задачи в viewModelScope
            repository.addAnalyses(10) // Начисление 10 сеансов в базу данных
            // Создание записи об успешной оплате подписки в локальной истории транзакций
            val payment = com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity(
                amountRub = amount, // Сумма в рублях
                paymentSystem = method, // Метод оплаты
                status = "Успешно", // Статус транзакции
                readingType = "Премиум подписка (10 сеансов)" // Тип покупки
            )
            repository.insertPayment(payment) // Вставка записи платежа в базу данных
        }
    }

    // Имитация покупки одиночного подробного сеанса анализа ладони
    fun simulateBuySingleAnalysis(analysisType: String, amount: Int = 199, method: String = "ЮKassa (Банковская карта)") {
        viewModelScope.launch { // Запуск сопрограммы во ViewModel
            repository.unlockFeature(analysisType) // Разблокировка конкретной функции анализа в настройках
            repository.addAnalyses(1) // Добавление 1 доступного анализа в баланс
            // Создание сущности истории платежа
            val payment = com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity(
                amountRub = amount, // Стоимость одиночного анализа
                paymentSystem = method, // Система оплаты
                status = "Успешно", // Флаг успешной оплаты
                readingType = "Одиночный анализ: $analysisType" // Описание типа анализа
            )
            repository.insertPayment(payment) // Запись транзакции в базу данных
        }
    }

    // Имитация покупки разблокировки анализа совместимости по ладоням
    fun simulateBuyCompatibility() {
        viewModelScope.launch { // Асинхронный запуск в фоне
            repository.unlockFeature("compatibility") // Разблокировка функции совместимости в репозитории
        }
    }

    // Метод проверки разблокирована ли определенная платная функция анализа
    fun checkFeatureUnlocked(analysisType: String, onCheckComplete: (Boolean) -> Unit) {
        viewModelScope.launch { // Запуск сопрограммы
            val isUnlocked = repository.hasUnlocked(analysisType) // Получение статуса разблокировки функции из БД
            onCheckComplete(isUnlocked) // Возврат результата через лямбда-колбэк
        }
    }

    // --- Действия по работе с историей ---

    // Полная очистка локальной истории анализов
    fun clearHistory() {
        viewModelScope.launch { // Запуск сопрограммы очистки в фоне
            repository.clearHistory() // Вызов метода удаления всех записей истории из БД
        }
    }

    // Полный сброс всех данных приложения (очистка истории, сброс настроек и обнуление профиля)
    fun resetApplicationData() {
        viewModelScope.launch { // Запуск фоновой сопрограммы сброса настроек
            repository.clearHistory() // Удаление всех анализов из БД
            repository.setLanguageSelected(false) // Сброс отметки о пройденном выборе языка интерфейса
            try {
                // Запись профиля пользователя по умолчанию в базу данных при сбросе
                repository.saveUserProfile("Максим", "Male", 44, 175, "Right", null, null, false)
            } catch (e: Exception) {
                e.printStackTrace() // Логирование ошибок в консоль разработчика
            }
        }
    }

    // Удаление конкретного сеанса анализа по его уникальному ID
    fun deleteReading(id: Long) {
        viewModelScope.launch { // Запуск удаления в фоновом потоке
            repository.deleteReading(id) // Удаление записи из БД через репозиторий
        }
    }

    // --- Методы для работы с историей оплат и реферальными начислениями (сохранение в БД) ---

    // Добавление новой записи о платеже в единую базу данных и начисление оплаченных анализов
    fun addPayment(amount: Int, paymentSystem: String, readingType: String, status: String = "Успешно") {
        viewModelScope.launch { // Запуск фоновой сопрограммы
            // Создаем сущность платежа для записи в базу данных
            val payment = com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity(
                amountRub = amount, // Сумма платежа в рублях
                paymentSystem = paymentSystem, // Способ или система платежа
                status = status, // Статус успешного прохождения оплаты
                readingType = readingType // Описание типа анализа
            )
            // Вставляем платеж в БД через репозиторий
            repository.insertPayment(payment)
            
            // Начисляем анализы в зависимости от суммы платежа
            val count = when {
                amount >= 999 -> 10 // Премиум-подписка (10 анализов)
                amount >= 499 -> 5  // Набор из 5 анализов
                amount >= 199 -> 3  // Набор из 3 анализов
                else -> 1           // 1 одиночный полный анализ
            }
            repository.addAnalyses(count) // Начисление анализов пользователю на баланс
        }
    }

    // Полная очистка истории платежей в базе данных
    fun clearPaymentHistory() {
        viewModelScope.launch { // Запуск сопрограммы асинхронной очистки
            repository.clearPaymentHistory() // Запрос удаления всех записей истории платежей в БД
        }
    }

    // Начисление вознаграждения (+3 бесплатные интерпретации) за шеринг и установку приложения
    fun rewardUserForSharing() {
        viewModelScope.launch { // Запуск сопрограммы начисления бонуса
            // Записываем информацию о бонусе в таблицу истории платежей БД для прозрачности
            val promoReward = com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity(
                amountRub = 0, // Бонусное начисление (0 рублей)
                paymentSystem = "Бонус (Поделиться)", // Название операции
                status = "Успешно начислено +3", // Информация о начислении в статусе
                readingType = "+3 бесплатные интерпретации за приглашение" // Описание для истории
            )
            // Сохраняем промо-начисление в единую базу данных
            repository.insertPayment(promoReward)
            // Добавляем 3 бесплатные интерпретации пользователю
            repository.addFreeAnalyses(3)
        }
    }

    fun addFreeAnalyses(count: Int) {
        viewModelScope.launch {
            val promoReward = com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity(
                amountRub = 0,
                paymentSystem = "Бонус (Поделиться)",
                status = "Успешно",
                readingType = "+$count бесплатные интерпретации за приглашение"
            )
            repository.insertPayment(promoReward)
            repository.addFreeAnalyses(count)
        }
    }

    // Добавление полных (платных) интерпретаций бесплатно для отладки
    fun addPaidAnalyses(count: Int) {
        viewModelScope.launch {
            val debugReward = com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity(
                amountRub = 0,
                paymentSystem = "Тест (Отладка)",
                status = "Успешно",
                readingType = "+$count Полная Интерпретация (Бесплатно/Тест)"
            )
            repository.insertPayment(debugReward)
            repository.addPaidAnalyses(count)
        }
    }

    // --- Запуск анализа ладони через нейросеть Gemini ---

    fun runPalmAnalysis(
        bitmaps: List<Bitmap>, // Снимки ладоней для распознавания линий ИИ
        videoUri: String?, // Дополнительное видео ладоней пользователя
        analysisType: String, // Тип анализа ("brief_char", "full_char", "brief_path", "full_path")
        leftPalmPath: String? = null, // Путь к файлу изображения левой ладони
        leftBackPath: String? = null, // Путь к тыльной стороне левой руки
        rightPalmPath: String? = null, // Путь к правой ладони в памяти
        rightBackPath: String? = null, // Путь к тыльной стороне правой руки
        onCompleted: () -> Unit // Функция обратного вызова при завершении
    ) {
        currentCompatibilityReading.value = null // Сброс текущей совместимости перед расчётом
        isAnalyzing.value = true // Перевод флага анализатора в активный режим
        analysisProgress.value = 0 // Сброс прогресса
        
        val isRussian = _selectedLanguage.value == AppLanguage.RUS // Определение языка системы
        analysisStatus.value = if (isRussian) "Запуск мистических сил..." else "Summoning cosmic currents..."

        viewModelScope.launch { // Старт сопрограммы
            try {
                // Лог начала анализа на REST API сервер
                repository.apiRepository.logInterpretation(analysisType, "start")

                // Фаза 1: До 50% - "Отправка данных" (1..49%)
                for (p in 1..49) {
                    analysisProgress.value = p // Передача прогресса в UI
                    delay(30) // Пауза для плавной анимации
                }

                // Фаза 2: От 50% до 80% - "Анализ данных" (50..79%)
                analysisProgress.value = 50 // Задаем начальный процент фазы анализа
                val analysisJob = launch { // Фоновый прираститель процентов во время запроса Gemini API
                    for (p in 50..79) {
                        analysisProgress.value = p // Обновление прогресса
                        delay(120) // Задержка между шагами
                    }
                }
                
                // Вызов метода отправки изображений и параметров на Gemini API
                val reading = repository.analyzePalm(
                    bitmaps = bitmaps,
                    videoUri = videoUri,
                    analysisType = analysisType,
                    langCode = _selectedLanguage.value.code,
                    leftPalmPath = leftPalmPath,
                    leftBackPath = leftBackPath,
                    rightPalmPath = rightPalmPath,
                    rightBackPath = rightBackPath
                )
                
                analysisJob.cancel() // Отмена фоновой инкрементации процентов
                currentReading.value = reading // Сохранение структуры результатов анализа
                refreshDailyTokens() // Обновление суточной статистики расхода токенов
                
                // Списание 1 единицы анализа (Краткий или Полный Анализ = 1 единице)
                repository.consumeAnalysisUnit()

                // Фаза 3: От 80% до 100% - "Получаем ответ" (80..100%)
                for (p in 80..100) {
                    analysisProgress.value = p // Завершение шкалы
                    delay(20) // Быстрый шаг анимации
                }

                // Лог успешного завершения анализа на REST API сервер
                repository.apiRepository.logInterpretation(analysisType, "end")

                delay(200) // Пауза перед скрытием диалога
                isAnalyzing.value = false // Сброс признака загрузки
                onCompleted() // Триггер перехода на экран результатов
            } catch (e: Exception) {
                e.printStackTrace() // Логирование ошибок
                // Лог сбоя на сервер
                repository.apiRepository.logError("Ошибка в процессе анализа ($analysisType): ${e.localizedMessage}")
                isAnalyzing.value = false // Принудительный сброс режима загрузки при неудаче
            }
        }
    }

    // --- Запуск анализа совместимости партнеров по ладоням через Gemini ---

    fun runCompatibilityAnalysis(
        selfBitmap: Bitmap?, // Фотографии ладони первого партнёра
        partnerBitmap: Bitmap?, // Фотографии ладони второго партнёра
        selfName: String, // Имя первого партнёра
        partnerName: String, // Имя второго партнёра
        onCompleted: () -> Unit // Функция завершения работы
    ) {
        currentReading.value = null // Сброс одиночного анализа перед совместимостью
        isAnalyzing.value = true // Перевод в режим ИИ-расчёта
        analysisProgress.value = 0 // Сброс прогресс-бара
        
        val isRussian = _selectedLanguage.value == AppLanguage.RUS // Определение языка

        viewModelScope.launch { // Старт сопрограммы во ViewModel
            try {
                repository.apiRepository.logInterpretation("paid_compat", "start")

                // Фаза 1: До 50% - "Отправка данных" (1..49%)
                for (p in 1..49) {
                    analysisProgress.value = p // Заполнение до 49%
                    delay(30) // Плавная задержка
                }

                // Фаза 2: От 50% до 80% - "Анализ данных" (50..79%)
                analysisProgress.value = 50 // Переход на 50%
                val compatJob = launch { // Фоновая инкрементация процентов при работе с Gemini API
                    for (p in 50..79) {
                        analysisProgress.value = p // Шаг шкалы
                        delay(120) // Пауза между шагами
                    }
                }

                // Запрос к ИИ Gemini на расчёт перекрестной хиромантии партнеров
                val reading = repository.analyzeCompatibility(
                    selfBitmap = selfBitmap,
                    partnerBitmap = partnerBitmap,
                    selfName = selfName,
                    partnerName = partnerName,
                    langCode = _selectedLanguage.value.code
                )
                
                compatJob.cancel() // Остановка фоновой симуляции
                currentCompatibilityReading.value = reading // Сохранение сущности совместимости
                refreshDailyTokens() // Обновление суточной статистики расхода токенов
                repository.decrementFreeAnalyses() // Списание 1 сеанса (бесплатного или платного)
                
                repository.apiRepository.logInterpretation("paid_compat", "end")

                // Фаза 3: От 80% до 100% - "Получаем ответ" (80..100%)
                for (p in 80..100) {
                    analysisProgress.value = p // Завершение процесса
                    delay(20) // Задержка отклика
                }

                delay(200) // Небольшая пауза при 100%
                isAnalyzing.value = false // Выключение режима загрузки
                onCompleted() // Колбэк успеха
            } catch (e: Exception) {
                e.printStackTrace() // Запись ошибки
                repository.apiRepository.logError("Ошибка анализа совместимости: ${e.localizedMessage}")
                isAnalyzing.value = false // Отключение состояния прогресс-бара
            }
        }
    }

    // Метод платной разблокировки сохраненного анализа из истории (например, через СБП или банковскую карту)
    fun unlockPaidReading(readingId: Long, amount: Int = 150, method: String = "ЮKassa (СБП)", onUnlocked: () -> Unit) {
        viewModelScope.launch { // Запуск сопрограммы асинхронного обновления данных
            repository.unlockPaidReading(readingId) // Обновление флага разблокировки в базе данных
            val updated = repository.getReadingById(readingId) // Повторный запрос обновлённой записи из БД
            if (updated != null) {
                // Синхронизация активных данных во ViewModel для мгновенного отображения разблокированного текста
                if (updated.analysisType == "compatibility") {
                    currentCompatibilityReading.value = updated // Сохранение в поток совместимости
                } else {
                    currentReading.value = updated // Сохранение в поток обычного анализа
                }
                
                // Автоматически регистрируем платеж в локальной базе данных транзакций для отображения в Кабинете
                val payment = com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity(
                    amountRub = amount, // Сумма платежа в рублях
                    paymentSystem = method, // Выбранная система оплаты
                    status = "Успешно", // Статус проведения платежа
                    readingType = "Разблокировка: " + (if (updated.analysisType == "compatibility") "Совместимость" else "Анализ ладони") // Описание покупки
                )
                repository.insertPayment(payment) // Запись транзакции платежа в локальную базу данных
            }
            onUnlocked() // Вызов лямбда-колбэка для оповещения UI
        }
    }

    // Метод проведения добровольного пожертвования (доната) пользователем на поддержку развития проекта
    fun addSupportPayment(amountRub: Int, paymentSystem: String) {
        viewModelScope.launch { // Запуск фонового процесса обработки транзакции поддержки
            try {
                // Извлечение текущих активных данных о пользователе
                val currentRead = currentReading.value ?: currentCompatibilityReading.value
                val profile = repository.getUserProfileSync()
                
                // Определение имени и возраста пользователя для логирования платежа
                val name = currentRead?.name ?: profile?.name ?: "Максим"
                val age = currentRead?.age ?: profile?.age ?: 44
                val lp = currentRead?.leftPalmPath
                val lb = currentRead?.leftBackPath
                val rp = currentRead?.rightPalmPath
                val rb = currentRead?.rightBackPath
                
                val granted = amountRub / 100 // Начисление бонусных анализов: +1 анализ за каждые 100 рублей поддержки
                
                val currentBilling = repository.getBillingStateSync() // Получение текущего баланса
                val currentRemaining = currentBilling?.remainingAnalyses ?: 0 // Текущий остаток сеансов
                val newRemaining = currentRemaining + granted // Новый остаток после начисления донат-бонуса
                
                // Создание подробной записи истории транзакции поддержки в локальной базе данных
                val supportPayment = com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity(
                    amountRub = amountRub, // Сумма поддержки в рублях
                    paymentSystem = paymentSystem, // Система проведения платежа
                    status = "Успешно", // Флаг успешной транзакции
                    readingType = "Поддержка проекта (+$granted анализов)", // Описание операции в истории
                    userName = name, // Имя пользователя
                    userAge = age, // Возраст пользователя
                    leftPalmPath = lp, // Ссылка на левую ладонь
                    leftBackPath = lb, // Ссылка на левую тыльную
                    rightPalmPath = rp, // Ссылка на правую ладонь
                    rightBackPath = rb, // Ссылка на правую тыльную
                    grantedAnalyses = granted, // Количество начисленных анализов
                    remainingAnalysesAfterPayment = newRemaining // Остаток баланса после проведения операции
                )
                
                repository.insertPayment(supportPayment) // Запись донат-транзакции в базу данных
                
                if (granted > 0) {
                    repository.addAnalyses(granted) // Фактическое начисление бонусов на баланс пользователя в БД
                }
            } catch (e: Exception) {
                e.printStackTrace() // Печать стека ошибок в логгер при возникновении сбоя
            }
        }
    }

    // Состояние процесса получения ответа на уточняющий вопрос пользователя (загрузка)
    val followUpLoading = kotlinx.coroutines.flow.MutableStateFlow(false)
    // Прогресс загрузки ответа на уточняющий вопрос в процентах (0..100)
    val followUpProgress = kotlinx.coroutines.flow.MutableStateFlow(0)
    // Состояние, содержащее текстовый ответ от ИИ Gemini на уточняющий вопрос
    val followUpResponse = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    // Состояние, содержащее заголовок/тему последнего уточняющего вопроса
    val followUpQuestionTitle = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    // Состояние отображения всплывающего диалога нехватки средств на 3 секунды
    val showInsufficientFundsDialog = kotlinx.coroutines.flow.MutableStateFlow(false)

    // Метод вызова всплывающего окна недостатка средств на 3 секунды
    fun triggerInsufficientFundsDialog() {
        viewModelScope.launch {
            showInsufficientFundsDialog.value = true
            kotlinx.coroutines.delay(3000L) // Автоматическое закрытие диалога через 3 секунды
            showInsufficientFundsDialog.value = false
        }
    }

    // Закрытие диалога нехватки средств вручную
    fun dismissInsufficientFundsDialog() {
        showInsufficientFundsDialog.value = false
    }

    // Метод отправки ИИ Gemini уточняющего вопроса по текущему тексту результатов анализа
    fun sendFollowUpQuestion(analysisText: String, question: String, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch { // Запуск сопрограммы общения с ИИ
            // Проверка наличия доступного анализа или средств перед отправкой уточняющего вопроса
            if (!repository.hasAvailableAnalysis()) {
                triggerInsufficientFundsDialog() // Отображение всплывающего окна на 3 секунды при нехватке средств
                return@launch
            }

            // Сохранение текста/темы заданного вопроса
            followUpQuestionTitle.value = question

            // Списание 1 Краткого Анализа за дополнительный вопрос
            repository.consumeBriefAnalysisUnit()

            followUpLoading.value = true // Включение индикатора прогресса в UI
            followUpResponse.value = null // Очистка предыдущего ответа
            followUpProgress.value = 0

            // Фоновая анимация шкалы прогресса от 1% до 95%
            val progressAnimationJob = launch {
                for (p in 1..95) {
                    followUpProgress.value = p
                    kotlinx.coroutines.delay(80)
                }
            }

            val lang = if (selectedLanguage.value == AppLanguage.RUS) "RU" else "EN" // Определение языка общения
            val resp = repository.askFollowUpQuestion(analysisText, question, lang) // Вызов API Gemini через репозиторий
            val cleanedResp = resp.replace(Regex("[#*]"), "").lines().joinToString("\n") { it.trimStart() }.trim() // Очистка текста ответа от символов * и #
            
            progressAnimationJob.cancel()
            followUpProgress.value = 100
            kotlinx.coroutines.delay(100)

            followUpResponse.value = cleanedResp // Запись очищенного ИИ-ответа в поток данных для Compose-интерфейса
            followUpLoading.value = false // Снятие индикатора ожидания ответа

            // Автоматическое сохранение дополнительного вопроса и ответа в текущую сущность анализа ладоней
            val targetReading = currentReading.value ?: currentCompatibilityReading.value // Получение активного объекта анализа
            if (targetReading != null) { // Проверка наличия активного объекта анализа ладоней
                val readingIdToUse = if (targetReading.id != 0L) { // Проверка наличия сохраненного ID в базе данных
                    targetReading.id // Использование существующего ID
                } else { // Если записи еще нет в базе данных Room
                    val savedId = repository.updateReading(targetReading) // Сохранение записи в БД
                    savedId
                }
                val updatedRecord = repository.saveFollowUpQuestionToReading(
                    readingId = if (readingIdToUse is Long) readingIdToUse else targetReading.id, // Идентификатор записи
                    question = question, // Текст заданного пользователем вопроса
                    answer = cleanedResp // Текст очищенного ответа ИИ-Аналитика
                ) // Добавление пары вопроса и ответа в список JSON
                if (updatedRecord != null) { // Проверка успешности операции обновления
                    if (currentReading.value?.id == updatedRecord.id || currentReading.value == targetReading) { // Обновление текущего анализа в потоке
                        currentReading.value = updatedRecord // Запись обновленной сущности с доп. вопросами в StateFlow
                    } // Конец условия для первого объекта
                    if (currentCompatibilityReading.value?.id == updatedRecord.id || currentCompatibilityReading.value == targetReading) { // Обновление совместимости
                        currentCompatibilityReading.value = updatedRecord // Запись обновленной совместимости в StateFlow
                    } // Конец условия для совместимости
                } // Конец проверки обновленной записи
            } // Конец проверки наличия активного объекта анализа

            onComplete(cleanedResp) // Вызов лямбды завершения со значениями без спецсимволов
        }
    }
    
    // Метод очистки диалога уточняющих вопросов
    fun clearFollowUp() {
        followUpResponse.value = null // Сброс текстового ответа ИИ в значение null
        followUpQuestionTitle.value = null // Сброс темы вопроса
        followUpLoading.value = false // Сброс состояния индикации загрузки в false
    }

    // Метод обновления и передачи времени активности пользователя (в секундах) на REST API сервер
    fun updateUsageTime(secondsToAdd: Int) {
        if (secondsToAdd <= 0) return
        viewModelScope.launch {
            repository.apiRepository.updateUsageTime(secondsToAdd)
        }
    }

    // Метод отправки лога возникшей ошибки на REST API сервер
    fun logErrorToServer(logContent: String) {
        viewModelScope.launch {
            repository.apiRepository.logError(logContent)
        }
    }

    // Метод получения стандартизированного наименования файла бэкапа
    fun getBackupFileName(): String {
        return com.aistudio.hiromant.kxsrwa.utils.BackupManager.generateBackupFileName() // Формирование имени файла по ТЗ
    }

    // Метод сохранения резервной копии данных в выбранный пользователем URI
    fun saveBackupToUri(context: android.content.Context, uri: android.net.Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { // Запуск экспорта в фоновом потоке ввода-вывода
            try { // Безопасный блок сохранения резервной копии
                val dao = (getApplication() as com.aistudio.hiromant.kxsrwa.PalmistApplication).database.palmistDao() // Доступ к DAO
                val csvContent = com.aistudio.hiromant.kxsrwa.utils.BackupManager.createBackupCsv(
                    context = context, // Передача контекста приложения
                    repository = repository, // Передача репозитория
                    dao = dao // Передача DAO базы данных
                ) // Создание текста бэкапа в формате CSV
                context.contentResolver.openOutputStream(uri)?.use { outputStream -> // Открытие потока записи по URI
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8)) // Запись байтов CSV файла
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { // Переход в главный поток UI
                    onComplete(true, if (_selectedLanguage.value == com.aistudio.hiromant.kxsrwa.ui.language.AppLanguage.RUS) "Резервная копия успешно сохранена!" else "Backup saved successfully!") // Уведомление об успехе
                }
            } catch (e: Exception) { // Обработка возможного исключения
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { // Переход в главный поток UI
                    onComplete(false, e.localizedMessage ?: "Ошибка при сохранении бэкапа") // Сообщение об ошибке
                }
            }
        }
    }

    // Метод восстановления данных из резервной копии по выбранному пользователем URI
    fun restoreBackupFromUri(context: android.content.Context, uri: android.net.Uri, onComplete: (com.aistudio.hiromant.kxsrwa.utils.RestoreResult) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { // Выполнение импорта в фоновом потоке
            try { // Безопасный блок чтения резервной копии
                val inputStream = context.contentResolver.openInputStream(uri) // Открытие входного потока по URI
                val csvText = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "" // Чтение текста CSV
                val dao = (getApplication() as com.aistudio.hiromant.kxsrwa.PalmistApplication).database.palmistDao() // Доступ к DAO
                val result = com.aistudio.hiromant.kxsrwa.utils.BackupManager.restoreFromBackupCsv(
                    context = context, // Контекст приложения
                    repository = repository, // Репозиторий
                    dao = dao, // DAO доступа к данным
                    csvText = csvText // Распакованный текст CSV бэкапа
                ) // Вызов процедуры импорта данных
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { // Переход на UI-поток
                    if (result.isSuccess) { // В случае успешного восстановления настроек
                        val langCode = repository.getSelectedLanguage() // Получение кода языка из настроек
                        _selectedLanguage.value = if (langCode == "EN") com.aistudio.hiromant.kxsrwa.ui.language.AppLanguage.ENG else com.aistudio.hiromant.kxsrwa.ui.language.AppLanguage.RUS // Обновление языка в StateFlow
                        _fontScale.value = repository.getFontScale() // Обновление масштаба шрифта
                        _ttsEnabled.value = repository.getTtsEnabled() // Обновление флага TTS
                        _ttsGender.value = repository.getTtsGender() // Обновление пола голоса
                        _ttsVoiceIndex.value = repository.getTtsVoiceIndex() // Обновление индекса голоса
                        _ttsSpeechRate.value = repository.getTtsSpeechRate() // Обновление скорости речи
                        _ttsPitch.value = repository.getTtsPitch() // Обновление высоты тона речи
                    }
                    onComplete(result) // Передача результатов восстановления
                }
            } catch (e: Exception) { // Перехват ошибок восстановления
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { // Переход на UI-поток
                    onComplete(com.aistudio.hiromant.kxsrwa.utils.RestoreResult(isSuccess = false, errorMessage = e.localizedMessage ?: "Ошибка чтения файла бэкапа")) // Передача ошибки
                }
            }
        }
    }
}
