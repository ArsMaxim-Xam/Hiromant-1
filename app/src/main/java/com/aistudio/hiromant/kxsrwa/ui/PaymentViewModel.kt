package com.aistudio.hiromant.kxsrwa.ui

// Импорты компонентов Android, Coroutines и Jetpack Compose
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.hiromant.kxsrwa.data.model.PaymentLogRequest
import com.aistudio.hiromant.kxsrwa.data.repository.ApiRepository
import com.aistudio.hiromant.kxsrwa.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Состояние проведения оплаты
sealed class PaymentUiState {
    object Idle : PaymentUiState()
    object Processing : PaymentUiState()
    data class Success(val message: String) : PaymentUiState()
    data class Error(val errorMessage: String) : PaymentUiState()
}

// ViewModel для обработки платежей через ЮKassa / СБП / Карты
class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    // Репозиторий сетевого REST API
    private val apiRepository: ApiRepository = ApiRepository(application)

    // Внутренний и публичный StateFlow состояния проведения платежа
    private val _paymentState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val paymentState: StateFlow<PaymentUiState> = _paymentState.asStateFlow()

    // Внутренний и публичный StateFlow выбранного способа оплаты
    private val _selectedPaymentMethod = MutableStateFlow("sbp") // "sbp", "card", "yookassa"
    val selectedPaymentMethod: StateFlow<String> = _selectedPaymentMethod.asStateFlow()

    // Функция выбора метода оплаты
    fun selectPaymentMethod(method: String) {
        _selectedPaymentMethod.value = method
    }

    // Обработка проведения платежа и отправки логов на сервер
    fun processPayment(
        readingId: Long,
        amount: Double,
        currency: String = "RUB",
        methodName: String = "СБП",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _paymentState.value = PaymentUiState.Processing
            AppLogger.i("PaymentViewModel", "Запуск проведения платежа: readingId=$readingId, sum=$amount $currency, method=$methodName")

            try {
                // Симуляция успешного отклика платежного шлюза
                val request = PaymentLogRequest(
                    userId = 0L, // Сервер авто-определит userId
                    amount = amount,
                    currency = currency,
                    status = "success"
                )

                // Отправка лога платежа на сервер через ApiRepository (поддерживает и Double, и PaymentLogRequest)
                apiRepository.logPayment(request)
                apiRepository.logPayment(amount = amount, currency = currency, status = "success")

                AppLogger.i("PaymentViewModel", "Платеж на сумму $amount $currency успешно обработан!")
                _paymentState.value = PaymentUiState.Success("Оплата прошла успешно!")
                onSuccess()
            } catch (e: Exception) {
                AppLogger.e("PaymentViewModel", "Ошибка обработки платежа", e)
                _paymentState.value = PaymentUiState.Error(e.localizedMessage ?: "Сбой при обработке платежа")
            }
        }
    }

    // Сброс состояния оплаты
    fun resetState() {
        _paymentState.value = PaymentUiState.Idle
    }
}
