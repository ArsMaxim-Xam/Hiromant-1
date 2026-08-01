package com.aistudio.hiromant.kxsrwa.utils

import android.content.Context
import android.content.SharedPreferences

// Класс-менеджер SharedPreferences для безопасного хранения системных идентификаторов и локального состояния
class SharedPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "hirromant_fastapi_prefs"
        private const val KEY_USER_ID = "remote_user_id"
        private const val KEY_USERNAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_IS_REGISTERED = "is_registered_on_server"
        private const val KEY_ACCUMULATED_USAGE_SECONDS = "accumulated_usage_seconds"
        private const val KEY_FREE_INTERPRETATIONS = "free_interpretations_count"
    }

    // Сохранение ID зарегистрированного пользователя с REST API сервера PostgreSQL
    fun saveUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
        setIsRegistered(true)
    }

    // Получение ID пользователя на сервере (возвращает -1L, если еще не зарегистрирован)
    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1L)
    }

    // Сохранение флага успешной регистрации на сервере
    fun setIsRegistered(isRegistered: Boolean) {
        prefs.edit().putBoolean(KEY_IS_REGISTERED, isRegistered).apply()
    }

    // Проверка, зарегистрирован ли текущий профиль на сервере
    fun isRegistered(): Boolean {
        return prefs.getBoolean(KEY_IS_REGISTERED, false) && getUserId() > 0
    }

    // Сохранение контактных данных пользователя
    fun saveUserData(name: String, email: String? = null, phone: String? = null) {
        prefs.edit()
            .putString(KEY_USERNAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PHONE, phone)
            .apply()
    }

    fun getUsername(): String {
        // Возвращает имя пользователя из настроек приложения (по умолчанию Максим)
        return prefs.getString(KEY_USERNAME, "Максим") ?: "Максим"
    }

    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    fun getPhone(): String? {
        return prefs.getString(KEY_PHONE, null)
    }

    // Сохранение накопленного времени активности в секундах
    fun addUsageSeconds(seconds: Int) {
        val current = getAccumulatedUsageSeconds()
        prefs.edit().putInt(KEY_ACCUMULATED_USAGE_SECONDS, current + seconds).apply()
    }

    fun getAccumulatedUsageSeconds(): Int {
        return prefs.getInt(KEY_ACCUMULATED_USAGE_SECONDS, 0)
    }

    // Сохранение количества доступных бесплатных интерпретаций
    fun setFreeInterpretationsCount(count: Int) {
        prefs.edit().putInt(KEY_FREE_INTERPRETATIONS, count).apply()
    }

    fun getFreeInterpretationsCount(): Int {
        return prefs.getInt(KEY_FREE_INTERPRETATIONS, 3)
    }
}
