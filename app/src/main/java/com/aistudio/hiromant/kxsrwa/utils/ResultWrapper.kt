package com.aistudio.hiromant.kxsrwa.utils

// Изолированный класс (sealed class) для безопасной обработки состояний сетевых и локальных операций
sealed class ResultWrapper<out T> {
    // Состояние успешного выполнения с полученными данными
    data class Success<out T>(val value: T) : ResultWrapper<T>()
    
    // Состояние ошибки с текстом сообщения и необязательным кодом HTTP / исключением
    data class Error(
        val message: String,
        val code: Int? = null,
        val exception: Throwable? = null
    ) : ResultWrapper<Nothing>()
    
    // Состояние ожидания / индикации загрузки
    object Loading : ResultWrapper<Nothing>()
}
