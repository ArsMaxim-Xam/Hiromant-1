package com.aistudio.hiromant.kxsrwa.data.remote

import okhttp3.OkHttpClient // Импорт клиента OkHttp
import okhttp3.logging.HttpLoggingInterceptor // Импорт логгера сетевых запросов
import retrofit2.Retrofit // Импорт основного класса Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory // Импорт конвертера Moshi
import retrofit2.http.Body // Импорт аннотации для передачи тела запроса
import retrofit2.http.POST // Импорт аннотации для HTTP-метода POST
import retrofit2.http.Path // Импорт аннотации для подстановки динамического пути в URL
import retrofit2.http.Query // Импорт аннотации для параметров запроса URL
import java.util.concurrent.TimeUnit // Импорт классов временных интервалов

// Интерфейс Retrofit для выполнения HTTP-запросов к REST API Google Gemini
interface GeminiApiService {
    
    // Вызов POST-запроса для генерации контента с динамическим выбором модели ИИ
    @POST("models/{model}:generateContent") // Запрос к Google Gemini с динамической подстановкой модели
    suspend fun generateContent(
        @Path("model") model: String, // Имя используемой ИИ-модели (например, gemini-1.5-flash)
        @Query("key") apiKey: String, // Передача индивидуального секретного API-ключа Google в URL
        @Body request: GenerateContentRequest // Передача тела запроса с промптом и снимками ладоней
    ): GenerateContentResponse // Возвращает сгенерированный ИИ текстовый контент и метаданные расхода токенов
}
