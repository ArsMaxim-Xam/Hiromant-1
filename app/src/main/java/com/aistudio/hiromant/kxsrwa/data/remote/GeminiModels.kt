package com.aistudio.hiromant.kxsrwa.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Сетевые модели запросов и ответов для REST API Gemini ---

// Модель запроса на генерацию контента (отправляется на сервера Google Gemini)
@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>, // Список элементов содержимого (текст, изображения)
    val generationConfig: GenerationConfig? = null, // Параметры конфигурации генерации текста
    val systemInstruction: Content? = null // Системный промпт (роль и контекст ИИ)
)

// Модель содержимого (обертка для частей запроса)
@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part> // Список текстовых или мультимедийных блоков
)

// Модель части содержимого (может содержать либо текст, либо закодированное изображение)
@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null, // Текстовый промпт
    val inlineData: InlineData? = null // Бинарные данные (например, изображение Base64)
)

// Модель встроенных данных для передачи картинок/файлов в API
@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String, // MIME-тип передаваемого файла (например, "image/jpeg")
    val data: String // Содержимое файла, закодированное в строку Base64
)

// Модель формата ожидаемого ответа от ИИ (по умолчанию JSON)
@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val type: String = "application/json" // Задает тип ответа (обычно JSON)
)

// Модель конфигурации параметров генерации ИИ
@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @field:Json(name = "responseMimeType") val responseMimeType: String? = null, // Ожидаемый MIME-тип ответа ИИ ("application/json")
    val temperature: Float? = null, // Креативность модели (от 0.0 до 2.0)
    @field:Json(name = "responseSchema") val responseSchema: Map<String, Any>? = null // Схема валидации структуры JSON на выходе
)

// Модель ответа от сервера генерации контента Gemini API
@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?, // Список сгенерированных вариантов ответа ИИ
    val usageMetadata: UsageMetadata? = null // Метаданные о количестве израсходованных токенов в запросе и ответе
)

// Модель для хранения детальных сведений о расходе токенов Gemini API
@JsonClass(generateAdapter = true)
data class UsageMetadata(
    val promptTokenCount: Int? = 0, // Количество токенов во входящем промпте (запросе пользователя)
    val candidatesTokenCount: Int? = 0, // Количество токенов в сгенерированном ИИ ответе
    val totalTokenCount: Int? = 0 // Суммарное количество израсходованных токенов (запрос + ответ)
)

// Модель кандидата (варианта ответа) из Gemini API
@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? // Содержимое ответа кандидата
)

// --- Кастомные модели для десериализации структурированного JSON-отчёта Хироманта ---

// Модель анализа конкретной линии на ладони (Жизни, Головы, Сердца и др.)
@JsonClass(generateAdapter = true)
data class PalmLineAnalysis(
    val name: String, // Название линии (например, "Линия Сердца")
    val color: String, // Код цвета линии на карте для визуализации
    val shortDescription: String, // Краткое мистическое резюме линии
    val fullDescription: String, // Детальная трактовка линии хиромантом
    val keyTakeaways: List<String> // Главные выводы из анализа этой линии
)

// Модель анализа планетарного холма ладони (Венеры, Луны, Юпитера и др.)
@JsonClass(generateAdapter = true)
data class PalmMountAnalysis(
    val name: String, // Название холма (например, "Холм Венеры")
    val active: Boolean, // Развит/активен ли холм у пользователя
    val description: String // Индивидуальное толкование холма ИИ
)

// Модель особого знака на ладони (Крест, Звезда, Остров и др.)
@JsonClass(generateAdapter = true)
data class PalmSign(
    val name: String, // Название знака (например, "Мистический крест")
    val description: String, // Толкование знака и его влияние на судьбу
    val location: String // Расположение знака на ладони
)

// Главный структурированный отчёт анализа ладони от Хироманта
@JsonClass(generateAdapter = true)
data class PalmistReport(
    val overallPortrait: String, // Общий психологический и астрологический портрет
    val handType: String, // Тип руки (Земля, Воздух, Огонь, Вода)
    val lines: List<PalmLineAnalysis> = emptyList(), // Список проанализированных линий рук
    val mounts: List<PalmMountAnalysis> = emptyList(), // Список холмов планет на ладони
    val signs: List<PalmSign> = emptyList(), // Список особых знаков и символов
    val leftHand: String = "", // Описание скрытого потенциала по левой руке
    val rightHand: String = "", // Описание реализованного потенциала по правой руке
    val characterQualities: String = "", // Черты характера и сильные стороны личности
    val lifePathEvents: String = "", // Ключевые события на жизненном пути
    val lifeSituationsInfluence: String = "", // Влияние внешних обстоятельств на судьбу
    val marriageChildren: String = "", // Личная жизнь, брак и дети
    val recommendations: String = "", // Советы и духовные предостережения
    val lifeEvents: String? = null, // Важные исторические вехи
    val predictions: String? = null, // Пророчества и предостережения на будущее
    val promptTokens: Int? = null, // Количество токенов, ушедших в промпт запроса к Gemini
    val candidatesTokens: Int? = null, // Количество токенов, вернувшихся в ответе от Gemini
    val totalTokens: Int? = null, // Общая сумма израсходованных токенов за данный сеанс
    val isMock: Boolean = false // Флаг локального демонстрационного отчета
)

// Структурированный отчёт анализа совместимости по ладоням двух людей
@JsonClass(generateAdapter = true)
data class CompatibilityReport(
    val compatibilityPercent: Int, // Процент совместимости партнеров (от 0 до 100)
    val partner1Portrait: String, // Портрет характера первого партнёра по ладони
    val partner2Portrait: String, // Портрет характера второго партнёра по ладони
    val combinedAnalysis: String, // Общий анализ взаимодействия энергетик
    val strongPoints: List<String>, // Сильные стороны союза (точки соприкосновения)
    val weakPoints: List<String>, // Слабые стороны и потенциальные разногласия
    val emotionalCompatibility: String, // Трактовка любовной (эмоциональной) связи
    val intellectualCompatibility: String, // Трактовка ментальной совместимости
    val financialCompatibility: String, // Финансовые и бытовые перспективы союза
    val recommendations: String, // Советы хироманта по укреплению отношений
    val promptTokens: Int? = null, // Количество токенов запроса в совместимости
    val candidatesTokens: Int? = null, // Количество токенов ответа в совместимости
    val totalTokens: Int? = null, // Суммарное количество токенов в совместимости
    val isMock: Boolean = false // Флаг локального демонстрационного отчета
)
