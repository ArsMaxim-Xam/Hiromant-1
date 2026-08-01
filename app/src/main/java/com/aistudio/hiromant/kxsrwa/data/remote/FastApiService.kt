package com.aistudio.hiromant.kxsrwa.data.remote

import com.aistudio.hiromant.kxsrwa.data.model.ErrorLogRequest
import com.aistudio.hiromant.kxsrwa.data.model.InterpretationLogRequest
import com.aistudio.hiromant.kxsrwa.data.model.PaymentLogRequest
import com.aistudio.hiromant.kxsrwa.data.model.RegisterUserRequest
import com.aistudio.hiromant.kxsrwa.data.model.RegisterUserResponse
import com.aistudio.hiromant.kxsrwa.data.model.SimpleApiResponse
import com.aistudio.hiromant.kxsrwa.data.model.TokenUsageLogRequest
import com.aistudio.hiromant.kxsrwa.data.model.UpdateUsageTimeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// Интерфейс эндпоинтов REST API для взаимодействия с сервером FastAPI
interface FastApiService {

    // 1. Регистрация пользователя
    @Headers("Content-Type: application/json")
    @POST("api/user/register")
    suspend fun registerUser(
        @Body request: RegisterUserRequest
    ): Response<RegisterUserResponse>

    // 2. Лог оплаты
    @Headers("Content-Type: application/json")
    @POST("api/log/payment")
    suspend fun logPayment(
        @Body request: PaymentLogRequest
    ): Response<SimpleApiResponse>

    // 3. Лог интерпретации (начала и завершения анализа)
    @Headers("Content-Type: application/json")
    @POST("api/log/interpretation")
    suspend fun logInterpretation(
        @Body request: InterpretationLogRequest
    ): Response<SimpleApiResponse>

    // 4. Обновление времени использования приложения пользователем
    @Headers("Content-Type: application/json")
    @POST("api/user/update_usage_time")
    suspend fun updateUsageTime(
        @Body request: UpdateUsageTimeRequest
    ): Response<SimpleApiResponse>

    // 5. Лог использования токенов Gemini ИИ пользователем
    @Headers("Content-Type: application/json")
    @POST("api/log/token_usage")
    suspend fun logTokenUsage(
        @Body request: TokenUsageLogRequest
    ): Response<SimpleApiResponse>

    // 6. Отправка отчета о возникшей ошибке или фатальном сбое
    @Headers("Content-Type: application/json")
    @POST("api/log/error")
    suspend fun logError(
        @Body request: ErrorLogRequest
    ): Response<SimpleApiResponse>
}
