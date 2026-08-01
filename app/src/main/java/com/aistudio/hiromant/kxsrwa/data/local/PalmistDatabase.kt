package com.aistudio.hiromant.kxsrwa.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// Кастомный абстрактный класс базы данных Room, объединяющий все сущности и предоставляющий DAO
@Database(
    entities = [
        ReadingEntity::class, // Таблица результатов сеансов анализа
        UserProfileEntity::class, // Таблица настроек и параметров профиля искателя
        BillingStateEntity::class, // Таблица баланса анализов и подписок
        PaymentHistoryEntity::class, // Таблица истории транзакций и бонусов
        TokenUsageEntity::class // Таблица расхода токенов Gemini ИИ
    ],
    version = 9, // Обновленная версия схемы БД для поддержки хранения списка дополнительных вопросов и ответов в анализах ладоней
    exportSchema = false // Отключение экспорта схемы БД в json-файл при компиляции
)
abstract class PalmistDatabase : RoomDatabase() {
    // Метод получения интерфейса DAO доступа к таблицам
    abstract fun palmistDao(): PalmistDao
}
