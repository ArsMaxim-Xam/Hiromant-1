package com.aistudio.hiromant.kxsrwa.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// === МОДЕЛИ ЗАПРОСОВ И ОТВЕТОВ К REST API СЕРВЕРУ FASTAPI ===

// 1. Модель запроса для регистрации нового пользователя в базе данных PostgreSQL
@JsonClass(generateAdapter = true)
data class RegisterUserRequest(
    @field:Json(name = "username") val username: String, // Имя пользователя
    @field:Json(name = "email") val email: String? = null, // Электронная почта пользователя (необязательно)
    @field:Json(name = "phone_number") val phoneNumber: String? = null, // Номер телефона пользователя (необязательно)
    @field:Json(name = "avatar_url") val avatarUrl: String? = null, // URL-ссылка на аватар пользователя (необязательно)
    @field:Json(name = "birth_year") val birthYear: Int? = null, // Год рождения пользователя (необязательно)
    @field:Json(name = "height") val height: Int? = null, // Рост пользователя в см (необязательно)
    @field:Json(name = "hand_photos") val handPhotos: List<String> = emptyList(), // Список URL или путей фотографий рук
    @field:Json(name = "video_url") val videoUrl: String? = null, // Ссылка на видеозапись анализа (необязательно)
    @field:Json(name = "android_version") val androidVersion: String, // Версия ОС Android на устройстве
    @field:Json(name = "phone_model") val phoneModel: String, // Модель смартфона (например, Samsung Galaxy)
    @field:Json(name = "screen_resolution") val screenResolution: String, // Разрешение экрана (ширина x высота)
    @field:Json(name = "install_source") val installSource: String = "Google Play", // Источник установки приложения
    @field:Json(name = "install_timestamp") val installTimestamp: String // Время установки в ISO 8601
)

// Модель ответа при успешной регистрации пользователя
@JsonClass(generateAdapter = true)
data class RegisterUserResponse(
    @field:Json(name = "status") val status: String, // Статус выполнения ("success")
    @field:Json(name = "user_id") val userId: Long // Назначенный сервером уникальный числовой идентификатор пользователя
)

// 2. Модель лога записи совершения платежа
@JsonClass(generateAdapter = true)
data class PaymentLogRequest(
    @field:Json(name = "user_id") val userId: Long, // Уникальный ID пользователя на сервере
    @field:Json(name = "amount") val amount: Double, // Сумма проведенного платежа
    @field:Json(name = "currency") val currency: String = "RUB", // Валюта проведения транзакции ("RUB")
    @field:Json(name = "status") val status: String = "success" // Статус транзакции
)

// 3. Модель лога выполнения сеанса анализа (интерпретации)
@JsonClass(generateAdapter = true)
data class InterpretationLogRequest(
    @field:Json(name = "user_id") val userId: Long, // Уникальный ID пользователя
    @field:Json(name = "interp_type") val interpType: String, // Тип анализа: "free", "paid_full", "paid_add", "paid_compat"
    @field:Json(name = "action") val action: String // Действие: "start" (начало) или "end" (завершение)
)

// 4. Модель запроса обновления времени использования приложения
@JsonClass(generateAdapter = true)
data class UpdateUsageTimeRequest(
    @field:Json(name = "user_id") val userId: Long, // Уникальный ID пользователя
    @field:Json(name = "seconds_to_add") val secondsToAdd: Int // Количество добавляемых секунд активности
)

// 5. Модель логирования суточного расхода токенов Gemini ИИ
@JsonClass(generateAdapter = true)
data class TokenUsageLogRequest(
    @field:Json(name = "user_id") val userId: Long, // Уникальный ID пользователя на сервере
    @field:Json(name = "usage_date") val usageDate: String, // Дата расхода в формате "YYYY-MM-DD"
    @field:Json(name = "tokens_sent") val tokensSent: Int, // Входящие токены (запрос пользователя и картинка)
    @field:Json(name = "tokens_received") val tokensReceived: Int // Исходящие токены (сгенерированный ответ ИИ)
)

// 6. Модель отправки журнала логов сбоев и ошибок на сервер
@JsonClass(generateAdapter = true)
data class ErrorLogRequest(
    @field:Json(name = "user_id") val userId: Long, // Уникальный ID пользователя на сервере
    @field:Json(name = "log_content") val logContent: String // Подробный стек ошибок или текст сбоя
)

// Универсальная модель базового ответа сервера со статусом
@JsonClass(generateAdapter = true)
data class SimpleApiResponse(
    @field:Json(name = "status") val status: String, // Результат выполнения запроса ("success" / "error")
    @field:Json(name = "message") val message: String? = null // Описание ошибки или детали ответа при наличии
)

// Псевдонимы типов для обеспечения обратной совместимости со старыми модулями
typealias SimpleResponse = SimpleApiResponse
typealias UsageTimeUpdateRequest = UpdateUsageTimeRequest
