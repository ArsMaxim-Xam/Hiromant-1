package com.aistudio.hiromant.kxsrwa.data.repository

import android.content.Context
import android.os.Build
import com.aistudio.hiromant.kxsrwa.data.model.ErrorLogRequest
import com.aistudio.hiromant.kxsrwa.data.model.InterpretationLogRequest
import com.aistudio.hiromant.kxsrwa.data.model.PaymentLogRequest
import com.aistudio.hiromant.kxsrwa.data.model.RegisterUserRequest
import com.aistudio.hiromant.kxsrwa.data.model.RegisterUserResponse
import com.aistudio.hiromant.kxsrwa.data.model.SimpleApiResponse
import com.aistudio.hiromant.kxsrwa.data.model.TokenUsageLogRequest
import com.aistudio.hiromant.kxsrwa.data.model.UpdateUsageTimeRequest
import com.aistudio.hiromant.kxsrwa.data.remote.FastApiRetrofitClient
import com.aistudio.hiromant.kxsrwa.data.remote.FastApiService
import com.aistudio.hiromant.kxsrwa.utils.AppLogger
import com.aistudio.hiromant.kxsrwa.utils.ResultWrapper
import com.aistudio.hiromant.kxsrwa.utils.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Репозиторий сетевого REST API для выполнения сетевых запросов к серверу FastAPI
class ApiRepository(
    private val context: Context? = null,
    private val apiService: FastApiService = FastApiRetrofitClient.service,
    private val prefsManager: SharedPreferencesManager? = context?.let { SharedPreferencesManager(it) }
) {

    // Метод авто-гарантии наличия зарегистрированного ID пользователя в PostgreSQL базе данных сервера
    suspend fun ensureUserRegistered(): Long = withContext(Dispatchers.IO) {
        val currentUserId = prefsManager?.getUserId() ?: 0L
        if (currentUserId > 0) {
            return@withContext currentUserId
        }

        // Если пользователь ещё не имеет ID на сервере, делаем быструю регистрацию
        val nameToRegister = prefsManager?.getUsername()?.ifBlank { "Максим_${Build.MODEL.take(10)}" } ?: "Максим_${Build.MODEL.take(10)}"
        val emailToRegister = prefsManager?.getEmail()
        val phoneToRegister = prefsManager?.getPhone()

        AppLogger.i("ApiRepository", "Авто-регистрация профиля $nameToRegister на сервере...")
        val res = registerUser(
            username = nameToRegister,
            email = emailToRegister,
            phone = phoneToRegister
        )

        return@withContext when (res) {
            is ResultWrapper.Success -> res.value.userId
            else -> prefsManager?.getUserId() ?: 0L
        }
    }

    // 1. Регистрация нового пользователя на REST API сервере
    suspend fun registerUser(
        username: String,
        email: String? = null,
        phone: String? = null,
        birthYear: Int? = null,
        height: Int? = null,
        handPhotos: List<String> = emptyList()
    ): ResultWrapper<RegisterUserResponse> = withContext(Dispatchers.IO) {
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val installTimestamp = isoFormat.format(Date())

            val screenRes = context?.resources?.displayMetrics?.let { "${it.widthPixels}x${it.heightPixels}" } ?: "1080x2400"
            val request = RegisterUserRequest(
                username = username.ifBlank { "Максим" },
                email = email,
                phoneNumber = phone,
                birthYear = birthYear,
                height = height,
                handPhotos = handPhotos,
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                phoneModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                screenResolution = screenRes,
                installSource = "Google Play",
                installTimestamp = installTimestamp
            )

            AppLogger.i("ApiRepository", "Отправка запроса регистрации пользователя: $username")
            val response = apiService.registerUser(request)

            if (response.isSuccessful && response.body() != null) {
                val registerResponse = response.body()!!
                // Сохраняем полученный от сервера user_id в SharedPreferences
                prefsManager?.saveUserId(registerResponse.userId)
                prefsManager?.saveUserData(username, email, phone)
                AppLogger.i("ApiRepository", "Пользователь успешно зарегистрирован на сервере! User ID: ${registerResponse.userId}")
                ResultWrapper.Success(registerResponse)
            } else {
                val errorMsg = "Ошибка регистрации: ${response.code()} ${response.message()}"
                AppLogger.e("ApiRepository", errorMsg)
                ResultWrapper.Error(message = errorMsg, code = response.code())
            }
        } catch (e: Exception) {
            AppLogger.e("ApiRepository", "Сбой сети при регистрации пользователя", e)
            ResultWrapper.Error(message = e.localizedMessage ?: "Ошибка сети", exception = e)
        }
    }

    suspend fun registerUser(request: RegisterUserRequest): ResultWrapper<RegisterUserResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.registerUser(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                prefsManager?.saveUserId(body.userId)
                ResultWrapper.Success(body)
            } else {
                ResultWrapper.Error("Ошибка регистрации: ${response.code()}")
            }
        } catch (e: Exception) {
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }

    // 2. Отправка лога совершенного платежа на сервер
    suspend fun logPayment(
        amount: Double,
        currency: String = "RUB",
        status: String = "success"
    ): ResultWrapper<SimpleApiResponse> = withContext(Dispatchers.IO) {
        val userId = ensureUserRegistered()
        if (userId <= 0) {
            AppLogger.w("ApiRepository", "Отмена отправки лога платежа: сервер недоступен")
            return@withContext ResultWrapper.Error("Пользователь не зарегистрирован")
        }

        try {
            val request = PaymentLogRequest(
                userId = userId,
                amount = amount,
                currency = currency,
                status = status
            )

            val response = apiService.logPayment(request)
            if (response.isSuccessful && response.body() != null) {
                AppLogger.i("ApiRepository", "Лог платежа на сумму $amount $currency успешно сохранен на сервере.")
                ResultWrapper.Success(response.body()!!)
            } else {
                ResultWrapper.Error("Ошибка записи платежа: ${response.code()}")
            }
        } catch (e: Exception) {
            AppLogger.e("ApiRepository", "Сбой отправки лога платежа", e)
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }

    suspend fun logPayment(request: PaymentLogRequest): ResultWrapper<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.logPayment(request)
            if (response.isSuccessful && response.body() != null) {
                ResultWrapper.Success(response.body()!!)
            } else {
                ResultWrapper.Error("Ошибка записи платежа: ${response.code()}")
            }
        } catch (e: Exception) {
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }


    // 3. Отправка лога анализа ладони (интерпретации) - начало ("start") или завершение ("end")
    suspend fun logInterpretation(
        interpType: String, // "free", "paid_full", "paid_add", "paid_compat"
        action: String // "start" или "end"
    ): ResultWrapper<SimpleApiResponse> = withContext(Dispatchers.IO) {
        val userId = ensureUserRegistered()
        if (userId <= 0) {
            return@withContext ResultWrapper.Error("Пользователь не зарегистрирован")
        }

        try {
            val request = InterpretationLogRequest(
                userId = userId,
                interpType = interpType,
                action = action
            )

            val response = apiService.logInterpretation(request)
            if (response.isSuccessful && response.body() != null) {
                AppLogger.i("ApiRepository", "Лог интерпретации ($interpType, $action) успешно передан.")
                ResultWrapper.Success(response.body()!!)
            } else {
                ResultWrapper.Error("Ошибка лога интерпретации: ${response.code()}")
            }
        } catch (e: Exception) {
            AppLogger.e("ApiRepository", "Сбой отправки лога интерпретации", e)
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }

    suspend fun logInterpretation(request: InterpretationLogRequest): ResultWrapper<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.logInterpretation(request)
            if (response.isSuccessful && response.body() != null) {
                ResultWrapper.Success(response.body()!!)
            } else {
                ResultWrapper.Error("Ошибка лога интерпретации: ${response.code()}")
            }
        } catch (e: Exception) {
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }

    // 4. Отправка обновленного времени активности пользователя в секундах
    suspend fun updateUsageTime(secondsToAdd: Int): ResultWrapper<SimpleApiResponse> = withContext(Dispatchers.IO) {
        val userId = ensureUserRegistered()
        if (userId <= 0 || secondsToAdd <= 0) {
            return@withContext ResultWrapper.Error("Неверный ID или время")
        }

        try {
            val request = UpdateUsageTimeRequest(
                userId = userId,
                secondsToAdd = secondsToAdd
            )

            val response = apiService.updateUsageTime(request)
            if (response.isSuccessful && response.body() != null) {
                prefsManager?.addUsageSeconds(secondsToAdd)
                AppLogger.i("ApiRepository", "Время использования +$secondsToAdd сек. обновлено на сервере.")
                ResultWrapper.Success(response.body()!!)
            } else {
                ResultWrapper.Error("Ошибка обновления времени: ${response.code()}")
            }
        } catch (e: Exception) {
            AppLogger.e("ApiRepository", "Сбой обновления времени использования", e)
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }

    suspend fun updateUsageTime(request: UpdateUsageTimeRequest): ResultWrapper<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateUsageTime(request)
            if (response.isSuccessful && response.body() != null) {
                prefsManager?.addUsageSeconds(request.secondsToAdd)
                ResultWrapper.Success(response.body()!!)
            } else {
                ResultWrapper.Error("Ошибка обновления времени: ${response.code()}")
            }
        } catch (e: Exception) {
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }

    // 5. Логирование суточного расхода токенов Gemini ИИ
    suspend fun logTokenUsage(
        tokensSent: Int,
        tokensReceived: Int,
        usageDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): ResultWrapper<SimpleApiResponse> = withContext(Dispatchers.IO) {
        val userId = ensureUserRegistered()
        if (userId <= 0) {
            return@withContext ResultWrapper.Error("Пользователь не зарегистрирован")
        }

        try {
            val request = TokenUsageLogRequest(
                userId = userId,
                usageDate = usageDate,
                tokensSent = tokensSent,
                tokensReceived = tokensReceived
            )

            val response = apiService.logTokenUsage(request)
            if (response.isSuccessful && response.body() != null) {
                AppLogger.i("ApiRepository", "Лог токенов ($tokensSent вх / $tokensReceived исх) зафиксирован на сервере.")
                ResultWrapper.Success(response.body()!!)
            } else {
                ResultWrapper.Error("Ошибка записи токенов: ${response.code()}")
            }
        } catch (e: Exception) {
            AppLogger.e("ApiRepository", "Сбой отправки лога токенов", e)
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }

    suspend fun logTokenUsage(request: TokenUsageLogRequest): ResultWrapper<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.logTokenUsage(request)
            if (response.isSuccessful && response.body() != null) {
                ResultWrapper.Success(response.body()!!)
            } else {
                ResultWrapper.Error("Ошибка записи токенов: ${response.code()}")
            }
        } catch (e: Exception) {
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }

    // 6. Отправка текстовых логов сбоев и ошибок на сервер
    suspend fun logError(logContent: String): ResultWrapper<SimpleApiResponse> = withContext(Dispatchers.IO) {
        val userId = ensureUserRegistered()
        if (userId <= 0) {
            AppLogger.w("ApiRepository", "Отмена отправки лога ошибки: не удалось получить valid userId")
            return@withContext ResultWrapper.Error("Пользователь не зарегистрирован")
        }

        try {
            val request = ErrorLogRequest(
                userId = userId,
                logContent = logContent
            )

            val response = apiService.logError(request)
            if (response.isSuccessful && response.body() != null) {
                AppLogger.i("ApiRepository", "Лог ошибки передан на сервер для user_id=$userId.")
                ResultWrapper.Success(response.body()!!)
            } else {
                ResultWrapper.Error("Ошибка передачи лога ошибки: ${response.code()}")
            }
        } catch (e: Exception) {
            AppLogger.e("ApiRepository", "Сбой передачи лога ошибки", e)
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }

    suspend fun logError(request: ErrorLogRequest): ResultWrapper<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.logError(request)
            if (response.isSuccessful && response.body() != null) {
                ResultWrapper.Success(response.body()!!)
            } else {
                ResultWrapper.Error("Ошибка передачи лога ошибки: ${response.code()}")
            }
        } catch (e: Exception) {
            ResultWrapper.Error(e.localizedMessage ?: "Сетевая ошибка", exception = e)
        }
    }

}
