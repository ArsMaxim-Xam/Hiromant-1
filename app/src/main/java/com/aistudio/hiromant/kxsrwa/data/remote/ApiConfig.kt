package com.aistudio.hiromant.kxsrwa.data.remote

/**
 * Конфигурационный файл сетевого взаимодействия с FastAPI REST сервером.
 * Все параметр сетевого подключения вынесены в данный файл для удобного редактирования разработчиками.
 */
object ApiConfig {
    // Базовый URL-адрес развернутого FastAPI сервера (с обратным слэшем на конце)
    // Разработчик может изменить этот URL на актуальный IP или домен сервера
    const val BASE_URL: String = "http://79.137.195.97:8000/"

    // Секретный Bearer-токен авторизации для защиты эндпоинтов (сгенерирован через openssl)
    // Измените это значение при обновлении ключа на сервере (/opt/hirromant_api/api_key.txt)
    const val API_KEY: String = "9e5c4a32b18f0293d8471e62a05f13e9c716b4a2d8e0f145b23a9187c2409d5a"

    // Таймаут выполнения сетевых HTTP-запросов к REST API (в секундах)
    const val TIMEOUT_SECONDS: Long = 30L

    // Имя локальной базы данных Room для кэширования
    const val DATABASE_NAME: String = "palmist_database"
}
