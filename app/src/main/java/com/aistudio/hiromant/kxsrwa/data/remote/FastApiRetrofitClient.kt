package com.aistudio.hiromant.kxsrwa.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

// Одиночный объект (Синглтон) клиенто-строения Retrofit для связи с REST API FastAPI сервером
object FastApiRetrofitClient {

    // Перехватчик HTTP-запросов для автоматической подстановки заголовок авторизации Bearer-токена и параметров ключа
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val newUrl = originalUrl.newBuilder()
            .addQueryParameter("token", ApiConfig.API_KEY)
            .addQueryParameter("api_key", ApiConfig.API_KEY)
            .build()
        val requestWithAuth = originalRequest.newBuilder()
            .url(newUrl)
            .header("Authorization", "Bearer ${ApiConfig.API_KEY}")
            .header("x-api-key", ApiConfig.API_KEY)
            .header("token", ApiConfig.API_KEY)
            .header("api-key", ApiConfig.API_KEY)
            .build()
        chain.proceed(requestWithAuth)
    }

    // Перехватчик для детального логирования сетевых запросов и ответов
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Построение и конфигурация экземпляра OkHttpClient с таймаутом в 30 секунд
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // Создание конвертера Moshi JSON
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    // Создание экземпляра сервиса FastApiService
    val service: FastApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FastApiService::class.java)
    }
}
