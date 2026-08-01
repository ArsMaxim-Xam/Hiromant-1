package com.aistudio.hiromant.kxsrwa.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Одиночный объект (Синглтон) Retrofit-клиента для взаимодействия с сервисами Google Gemini.
 * Включает увеличенные таймауты (120 секунд) во избежание разрывов соединений при обработке 
 * и отправке тяжелых бинарных данных (нескольких фотографий ладоней высокого разрешения).
 * Содержит кастомные перехватчики (Interceptors) для детального ведения сетевого лога.
 */
object RetrofitClient {
    // Базовый эндпоинт Google Developer API для отправки запросов генерации
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    // Конфигурация HTTP-клиента OkHttpClient с ленивой отложенной инициализацией
    private val okHttpClient: OkHttpClient by lazy {
        // Добавление логгера тела HTTP-запросов и ответов для анализа отладки в реальном времени
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Вывод полных заголовков и JSON в Logcat
        }
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS) // Таймаут установки соединения с сервером (120 секунд)
            .readTimeout(120, TimeUnit.SECONDS) // Таймаут чтения ответа от ИИ (120 секунд)
            .writeTimeout(120, TimeUnit.SECONDS) // Таймаут передачи изображений на сервер (120 секунд)
            .addInterceptor(logging) // Регистрация логгера запросов
            .build() // Финальная сборка клиента
    }

    // Ленивая инициализация и регистрация сервиса GeminiApiService
    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // Установка базового адреса Google API
            .client(okHttpClient) // Привязка настроенного OkHttpClient
            .addConverterFactory(MoshiConverterFactory.create()) // Регистрация Moshi для парсинга JSON в Kotlin-объекты
            .build() // Сборка экземпляра Retrofit
            .create(GeminiApiService::class.java) // Генерация исполняемого интерфейса API-сервиса
    }
}
