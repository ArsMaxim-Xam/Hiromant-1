package com.aistudio.hiromant.kxsrwa.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Класс данных для сохранения пары дополнительного вопроса и ответа в историю анализа
data class FollowUpQuestionPair(
    val question: String, // Заголовок или текст заданного дополнительного вопроса
    val answer: String, // Полный текст ответа ИИ-Аналитика
    val timestamp: Long = System.currentTimeMillis() // Время сохранения вопроса
)

// Функция парсинга JSON строки дополнительных вопросов в список объектов FollowUpQuestionPair
fun parseFollowUpQuestionsJson(json: String?): List<FollowUpQuestionPair> {
    if (json.isNullOrBlank()) return emptyList() // Если строка пустая, возвращаем пустой список
    return try { // Безопасный блок парсинга JSON
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build() // Создаем адаптер Moshi
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, FollowUpQuestionPair::class.java)
        val adapter = moshi.adapter<List<FollowUpQuestionPair>>(type)
        adapter.fromJson(json) ?: emptyList() // Распаковываем список или пустой список при ошибке
    } catch (e: Exception) {
        emptyList() // Возврат пустого списка в случае исключения
    }
}

// Функция упаковки списка дополнительных вопросов FollowUpQuestionPair в JSON-строку
fun formatFollowUpQuestionsJson(list: List<FollowUpQuestionPair>): String {
    return try { // Безопасная сериализация списка вопросов в JSON
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build() // Создаем конфигурацию Moshi
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, FollowUpQuestionPair::class.java)
        val adapter = moshi.adapter<List<FollowUpQuestionPair>>(type)
        adapter.toJson(list) // Преобразование списка объектов в JSON-строку
    } catch (e: Exception) {
        "" // Возврат пустой строки при возникновении сбоя
    }
}

// Сущность (таблица) в базе данных Room, хранящая результаты проведенных анализов ладоней и совместимости
@Entity(tableName = "readings")
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Уникальный идентификатор записи с автогенерацией
    val timestamp: Long = System.currentTimeMillis(), // Системное время прохождения анализа ладоней
    val name: String, // Имя пользователя, прошедшего сеанс
    val gender: String, // Пол ("Мужской", "Женский")
    val age: Int, // Возраст пользователя
    val height: Int, // Рост пользователя
    val dominantHand: String, // Ведущая рука ("Правая", "Левая")
    val analysisType: String, // Тип анализа: "brief_char" (краткий характер), "full_char" (полный характер), "brief_path" (краткий путь), "full_path" (полный путь), "compatibility" (совместимость)
    val resultJson: String, // Текстовый результат анализа, сгенерированный ИИ Gemini в формате JSON
    val imageUrl: String? = null, // Путь к фотографии ладони первого человека
    val partnerName: String? = null, // Имя партнёра (заполняется только для совместимости)
    val partnerImageUrl: String? = null, // Путь к фотографии ладони партнёра
    val leftPalmPath: String? = null, // Локальный путь к снимку левой ладони
    val leftBackPath: String? = null, // Локальный путь к снимку левой тыльной стороны
    val rightPalmPath: String? = null, // Локальный путь к снимку правой ладони
    val rightBackPath: String? = null, // Локальный путь к снимку правой тыльной стороны
    val videoPath: String? = null, // Локальный путь к видеозаписи рук (если была сделана)
    val followUpQuestionsJson: String? = null // Хранение списка дополнительных вопросов и ответов в JSON-формате
)

// Сущность (таблица), содержащая информацию о профиле текущего пользователя приложения
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "current_user", // Фиксированный ключ для сохранения единственного профиля пользователя
    val email: String? = null, // Электронная почта
    val phone: String? = null, // Мобильный телефон
    val isRegistered: Boolean = false, // Флаг пройденной авторизации/регистрации
    val name: String = "Максим", // Имя пользователя по умолчанию (Максим)
    val gender: String = "Male", // Пол пользователя по умолчанию (Мужской / М)
    val age: Int = 44, // Возраст пользователя по умолчанию (44 года, 1982 г.р.)
    val height: Int = 175, // Рост пользователя в см по умолчанию (175 см)
    val dominantHand: String = "Right", // Ведущая рука (по умолчанию Правая)
    val photoUri: String? = null, // Локальный путь/URI на фото или аватар пользователя
    val registrationTimestamp: Long = System.currentTimeMillis() // Системное время/дата проведения регистрации пользователя
)

// Сущность (таблица) состояния биллинга, баланса сеансов и наличия премиум-подписок
@Entity(tableName = "app_billing_state")
data class BillingStateEntity(
    @PrimaryKey val id: Int = 1, // Идентификатор записи состояния биллинга (всегда 1)
    val freeAnalyses: Int = 3, // Количество доступных бесплатных интерпретаций
    val paidAnalyses: Int = 0, // Количество доступных полных/платных интерпретаций
    val remainingAnalyses: Int = 3, // Количество доступных общее (для обратной совместимости)
    val isPremiumSubscribed: Boolean = false, // Активен ли режим полной безлимитной премиум-подписки
    val purchasedItemIds: String = "" // Идентификаторы приобретенных пакетов товаров через запятую
)

// Сущность (таблица) единой истории совершенных транзакций, платежей, донатов и начислений в приложении
@Entity(tableName = "payment_history")
data class PaymentHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Автогенерируемый ключ записи платежа
    val timestamp: Long = System.currentTimeMillis(), // Системное время совершения операции/транзакции
    val amountRub: Int, // Сумма платежа в рублях (0 для бесплатных бонусов)
    val paymentSystem: String, // Название платежной системы (например, "ЮKassa (СБП)", "Google Billing")
    val status: String, // Результат транзакции (например, "Успешно", "В процессе", "Ошибка")
    val readingType: String, // Описание назначения платежа (например, "Разблокировка: Анализ ладони", "Донат")
    val userName: String = "", // Имя пользователя на момент платежа
    val userAge: Int = 18, // Возраст пользователя на момент платежа
    val leftPalmPath: String? = null, // Путь к фото левой ладони при проведении оплаты
    val leftBackPath: String? = null, // Путь к фото левой тыльной стороны при проведении оплаты
    val rightPalmPath: String? = null, // Путь к фото правой ладони при проведении оплаты
    val rightBackPath: String? = null, // Путь к фото правой тыльной стороны при проведении оплаты
    val grantedAnalyses: Int = 0, // Количество начисленных анализов за эту транзакцию
    val remainingAnalysesAfterPayment: Int = 0 // Баланс анализов пользователя сразу после совершения этого платежа
)

// Сущность (таблица) в базе данных Room для отдельного учета расхода токенов Gemini API каждым пользователем
@Entity(tableName = "token_usage")
data class TokenUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Уникальный идентификатор записи лога токенов
    val timestamp: Long = System.currentTimeMillis(), // Время совершения запроса к ИИ Gemini
    val userName: String, // Имя конкретного пользователя, прошедшего анализ
    val userEmail: String? = null, // Электронная почта пользователя (при наличии)
    val analysisType: String, // Название операции или типа проведенного анализа
    val promptTokens: Int, // Входящие токены (запрос и изображения)
    val candidatesTokens: Int, // Исходящие токены (сгенерированный ответ ИИ)
    val totalTokens: Int, // Общее количество потраченных токенов за сеанс
    val dateString: String // Дата выполнения операции в формате "yyyy-MM-dd" для суточной статистики
)

// Класс для представления агрегированных данных об использовании токенов в разрезе каждого пользователя
data class UserTokenSummary(
    val userName: String, // Имя пользователя
    val totalTokens: Int, // Суммарное число использованных токенов за весь период
    val usageCount: Int // Количество выполненных запросов к ИИ
)

