package com.aistudio.hiromant.kxsrwa.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.aistudio.hiromant.kxsrwa.ui.language.AppLanguage
import com.aistudio.hiromant.kxsrwa.ui.screens.configureTtsVoice
import com.aistudio.hiromant.kxsrwa.ui.screens.prepareTextForTts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Глобальный менеджер озвучки для информационных блоков и ответов на дополнительные вопросы
object GlobalTtsManager {
    // Синтезатор речи TextToSpeech
    private var tts: TextToSpeech? = null
    // Флаг готовности голосового движка
    private var isReady: Boolean = false
    // Отложенное действие озвучивания, если движок TTS ещё инициализируется
    private var pendingSpeakAction: (() -> Unit)? = null
    // Идентификатор текущего воспроизводимого текста
    private val _currentSpeakingId = MutableStateFlow<String?>(null)
    // Публичный поток состояния текущего звучащего блока
    val currentSpeakingId: StateFlow<String?> = _currentSpeakingId

    // Диапазон произносимого слова для синхронной подсветки текста
    private val _currentWordRange = MutableStateFlow<Pair<Int, Int>?>(null)
    val currentWordRange: StateFlow<Pair<Int, Int>?> = _currentWordRange

    // Объект очищенного текста с картой индексов для TTS
    private var activeCleanedTts: com.aistudio.hiromant.kxsrwa.ui.screens.CleanedTtsText? = null

    // Карта смещений фрагментов текста относительно глобального текста для корректной подсветки при сегментации
    private val utteranceOffsets = java.util.concurrent.ConcurrentHashMap<String, Int>()

    // Инициализация голосового движка
    fun init(context: Context) {
        if (tts == null) {
            // Создание нового объекта TextToSpeech через контекст приложения
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isReady = true
                    // Установка слушателя прогресса воспроизведения речи
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            // Событие начала озвучивания ответа
                            val cleanId = utteranceId?.substringBefore("_part_")?.substringBefore("_last")
                            _currentSpeakingId.value = cleanId
                        }

                        override fun onDone(utteranceId: String?) {
                            // Окончание чтения текста (проверка завершения последнего сегмента)
                            val cleanId = utteranceId?.substringBefore("_part_")?.substringBefore("_last")
                            if (_currentSpeakingId.value == cleanId && (utteranceId == cleanId || utteranceId?.endsWith("_last") == true)) {
                                _currentSpeakingId.value = null
                                _currentWordRange.value = null
                                utteranceOffsets.clear()
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            // Ошибка озвучивания текста
                            val cleanId = utteranceId?.substringBefore("_part_")?.substringBefore("_last")
                            if (_currentSpeakingId.value == cleanId) {
                                _currentSpeakingId.value = null
                                _currentWordRange.value = null
                                utteranceOffsets.clear()
                            }
                        }

                        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                            // Расчёт глобального смещения текущего фрагмента текста для неразрывной подсветки слов
                            val chunkOffset = if (utteranceId != null) utteranceOffsets[utteranceId] ?: 0 else 0
                            val globalStart = chunkOffset + start
                            val globalEnd = chunkOffset + end

                            val cleaned = activeCleanedTts
                            val map = cleaned?.indexMap
                            if (map != null && map.isNotEmpty()) {
                                val relStart = map.getOrElse(globalStart) { globalStart }
                                val relEnd = map.getOrElse(globalEnd) { globalEnd }
                                _currentWordRange.value = Pair(relStart, relEnd)
                            } else {
                                _currentWordRange.value = Pair(globalStart, globalEnd)
                            }
                        }
                    })
                    // Запуск отложенной задачи озвучивания при наличии
                    val action = pendingSpeakAction
                    pendingSpeakAction = null
                    action?.invoke()
                } else {
                    isReady = false
                    pendingSpeakAction = null
                }
            }
        }
    }

    // Воспроизведение или остановка озвучки выбранного текста
    fun toggleSpeak(
        context: Context,
        blockId: String,
        text: String,
        currentLang: AppLanguage,
        voiceGender: String,
        voiceIndex: Int,
        speechRate: Float,
        speechPitch: Float
    ) {
        // Проверка и инициализация голосового движка при необходимости
        if (tts == null) {
            init(context)
        }

        // Если данный блок уже озвучивается — останавливаем воспроизведение
        if (_currentSpeakingId.value == blockId) {
            stop()
            pendingSpeakAction = null
            return
        }

        // Останавливаем любое активное воспроизведение речи
        stop()

        // Если движок TTS ещё не готов — сохраняем задачу для автозапуска после завершения инициализации
        if (!isReady) {
            pendingSpeakAction = {
                toggleSpeak(
                    context = context,
                    blockId = blockId,
                    text = text,
                    currentLang = currentLang,
                    voiceGender = voiceGender,
                    voiceIndex = voiceIndex,
                    speechRate = speechRate,
                    speechPitch = speechPitch
                )
            }
            return
        }

        val instance = tts ?: return

        // Очистка текста от символов разметки и спецсимволов для чистого звучания
        val cleanedObj = prepareTextForTts(text)
        activeCleanedTts = cleanedObj
        val cleanText = cleanedObj.sanitizedText
        if (cleanText.isEmpty()) return

        // Конфигурирование выбранного пользователем голоса и параметров речи
        configureTtsVoice(
            tts = instance,
            currentLang = currentLang,
            voiceGender = voiceGender,
            voiceIndex = voiceIndex,
            speechRate = speechRate,
            speechPitch = speechPitch
        )

        // Безопасное разделение длинных текстов на части до 2500 символов для гарантированного чтения всех пунктов
        val maxChunkSize = try {
            val limit = TextToSpeech.getMaxSpeechInputLength()
            if (limit in 501..3999) limit - 200 else 2500
        } catch (e: Exception) {
            2500
        }

        val chunks = java.util.ArrayList<String>()
        var tempText = cleanText
        while (tempText.length > maxChunkSize) {
            var splitPos = tempText.lastIndexOf('.', maxChunkSize)
            if (splitPos < 300) splitPos = tempText.lastIndexOf('\n', maxChunkSize)
            if (splitPos < 300) splitPos = tempText.lastIndexOf(' ', maxChunkSize)
            if (splitPos < 300) splitPos = maxChunkSize
            chunks.add(tempText.substring(0, splitPos + 1))
            tempText = tempText.substring(splitPos + 1)
        }
        if (tempText.isNotEmpty()) {
            chunks.add(tempText)
        }

        // Запоминаем текущий ID озвучивания и очищаем предыдущие карты смещений
        _currentSpeakingId.value = blockId
        _currentWordRange.value = null
        utteranceOffsets.clear()

        // Расчёт смещения для каждого фрагмента речи в глобальном очищенном тексте
        var currentOffset = 0
        for (i in chunks.indices) {
            val isLast = i == chunks.size - 1
            val chunkUtteranceId = if (isLast) "${blockId}_last" else "${blockId}_part_$i"
            utteranceOffsets[chunkUtteranceId] = currentOffset
            currentOffset += chunks[i].length

            val params = android.os.Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, chunkUtteranceId)
            }
            val queueMode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val res = instance.speak(chunks[i], queueMode, params, chunkUtteranceId)
            if (res != TextToSpeech.SUCCESS && i == 0) {
                _currentSpeakingId.value = null
                _currentWordRange.value = null
                utteranceOffsets.clear()
                break
            }
        }
    }

    // Полная остановка воспроизведения речи
    fun stop() {
        tts?.stop()
        _currentSpeakingId.value = null
        _currentWordRange.value = null
        utteranceOffsets.clear()
    }

    // Освобождение ресурсов синтезатора
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        pendingSpeakAction = null
        _currentSpeakingId.value = null
        _currentWordRange.value = null
        utteranceOffsets.clear()
    }
}
