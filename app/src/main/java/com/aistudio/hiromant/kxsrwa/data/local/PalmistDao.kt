package com.aistudio.hiromant.kxsrwa.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Интерфейс доступа к данным (DAO) Room для выполнения SQL-запросов к таблицам приложения
@Dao
interface PalmistDao {
    
    // Получение всех сохраненных сеансов анализа, отсортированных по времени от новых к старым
    @Query("SELECT * FROM readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<ReadingEntity>>

    // Синхронное получение всех сеансов анализа ладоней из базы данных Room
    @Query("SELECT * FROM readings ORDER BY timestamp DESC")
    suspend fun getAllReadingsSync(): List<ReadingEntity>

    // Поиск конкретной записи сеанса анализа по его уникальному ID
    @Query("SELECT * FROM readings WHERE id = :id LIMIT 1")
    suspend fun getReadingById(id: Long): ReadingEntity?

    // Вставка нового или замена существующего сеанса анализа ладоней
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: ReadingEntity): Long

    // Удаление записи анализа из истории по ее уникальному идентификатору
    @Query("DELETE FROM readings WHERE id = :id")
    suspend fun deleteReadingById(id: Long)

    // Полная очистка всей сохраненной истории анализов
    @Query("DELETE FROM readings")
    suspend fun clearHistory()

    // Наблюдение за изменениями профиля текущего пользователя в реальном времени
    @Query("SELECT * FROM user_profile WHERE id = 'current_user' LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    // Получение списка всех сохраненных профилей пользователей
    @Query("SELECT * FROM user_profile")
    fun getAllUserProfiles(): Flow<List<UserProfileEntity>>

    // Синхронное получение данных профиля текущего пользователя (блокирующий/фоновый вызов)
    @Query("SELECT * FROM user_profile WHERE id = 'current_user' LIMIT 1")
    suspend fun getUserProfileSync(): UserProfileEntity?

    @Query("SELECT * FROM user_profile")
    suspend fun getAllUserProfilesSync(): List<UserProfileEntity>

    // Создание или обновление информации о пользователе в таблице профиля
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    // Сброс и удаление профиля пользователя из базы данных
    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    // Асинхронное отслеживание изменений лимитов и состояния покупок
    @Query("SELECT * FROM app_billing_state WHERE id = 1 LIMIT 1")
    fun getBillingState(): Flow<BillingStateEntity?>

    // Синхронное получение текущего баланса сеансов и статуса подписки
    @Query("SELECT * FROM app_billing_state WHERE id = 1 LIMIT 1")
    suspend fun getBillingStateSync(): BillingStateEntity?

    // Вставка или обновление данных биллинга (начисление сеансов или покупка подписки)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillingState(state: BillingStateEntity)

    // --- Методы для работы с единой историей транзакций и оплат ---

    // Получение полного списка платежей и бонусов пользователя от новых к старым
    @Query("SELECT * FROM payment_history ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentHistoryEntity>>

    // Синхронное получение списка всех оплат для бэкапа
    @Query("SELECT * FROM payment_history ORDER BY timestamp DESC")
    suspend fun getAllPaymentsSync(): List<PaymentHistoryEntity>

    // Сохранение транзакции платежа или начисления реферального/шеринг-бонуса в БД
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentHistoryEntity): Long

    // Очистка списка истории платежей и бонусов
    @Query("DELETE FROM payment_history")
    suspend fun clearPaymentHistory()

    // --- Методы для работы с логированием расхода токенов по каждому пользователю ---

    // Вставка новой записи об использовании токенов в базу данных Room
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokenUsage(usage: TokenUsageEntity): Long

    // Получение истории использования токенов для конкретного пользователя по имени
    @Query("SELECT * FROM token_usage WHERE userName = :userName ORDER BY timestamp DESC")
    fun getTokenUsageForUser(userName: String): Flow<List<TokenUsageEntity>>

    // Подсчет всех токенов, потраченных конкретным пользователем за все время
    @Query("SELECT SUM(totalTokens) FROM token_usage WHERE userName = :userName")
    fun getTotalTokensUsedByUser(userName: String): Flow<Int?>

    // Подсчет токенов, потраченных конкретным пользователем за сегодня
    @Query("SELECT SUM(totalTokens) FROM token_usage WHERE userName = :userName AND dateString = :dateString")
    suspend fun getTodayTokensUsedByUserSync(userName: String, dateString: String): Int?

    // Подсчет сумарного расхода токенов всеми пользователями за указанную дату
    @Query("SELECT SUM(totalTokens) FROM token_usage WHERE dateString = :dateString")
    suspend fun getTodayTotalTokensSync(dateString: String): Int?

    // Получение агрегированного отчета по расходу токенов для каждого пользователя отдельно
    @Query("SELECT userName, SUM(totalTokens) as totalTokens, COUNT(*) as usageCount FROM token_usage GROUP BY userName ORDER BY totalTokens DESC")
    fun getUserTokenSummaries(): Flow<List<UserTokenSummary>>

    // Получение всех логов расхода токенов
    @Query("SELECT * FROM token_usage ORDER BY timestamp DESC")
    fun getAllTokenUsages(): Flow<List<TokenUsageEntity>>

    // Синхронное получение всех логов расхода токенов для бэкапа
    @Query("SELECT * FROM token_usage ORDER BY timestamp DESC")
    suspend fun getAllTokenUsagesSync(): List<TokenUsageEntity>

    // Полное удаление логов использования токенов
    @Query("DELETE FROM token_usage")
    suspend fun clearTokenUsageHistory()
}

