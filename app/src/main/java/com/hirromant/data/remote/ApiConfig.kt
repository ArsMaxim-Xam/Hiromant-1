package com.hirromant.data.remote

/**
 * Конфигурационный файл сетевого взаимодействия с FastAPI REST сервером.
 * Вынесен в отдельный пакет com.hirromant.data.remote согласно требованиям конституции проекта.
 */
object ApiConfig {
    // Базовый URL-адрес развернутого FastAPI сервера
    const val BASE_URL: String = "http://79.137.195.97:8000/"

    // Секретный Bearer-токен авторизации для доступа к API
    const val API_KEY: String = "9e5c4a32b18f0293d8471e62a05f13e9c716b4a2d8e0f145b23a9187c2409d5a"

    // Таймаут выполнения сетевых запросов (в секундах)
    const val TIMEOUT: Long = 30L

    // Имя локальной базы данных
    const val DATABASE_NAME: String = "palmist_database"
}
