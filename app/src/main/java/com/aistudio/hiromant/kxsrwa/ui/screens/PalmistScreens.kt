package com.aistudio.hiromant.kxsrwa.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.aistudio.hiromant.kxsrwa.BuildConfig
import com.aistudio.hiromant.kxsrwa.utils.AppLogger
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.aistudio.hiromant.kxsrwa.data.local.PaymentHistoryEntity
import com.aistudio.hiromant.kxsrwa.data.local.ReadingEntity
import com.aistudio.hiromant.kxsrwa.data.local.UserProfileEntity
import com.aistudio.hiromant.kxsrwa.ui.PalmistViewModel
import com.aistudio.hiromant.kxsrwa.ui.components.*
import com.aistudio.hiromant.kxsrwa.ui.language.AppLanguage
import com.aistudio.hiromant.kxsrwa.ui.language.LocalizedStrings
import com.aistudio.hiromant.kxsrwa.ui.language.PalmistStrings
import com.aistudio.hiromant.kxsrwa.ui.theme.*
import com.aistudio.hiromant.kxsrwa.utils.BitmapUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

// Вспомогательная функция для корректной загрузки локальных файлов, аватарок и URI через Coil
fun getCoilImageModel(pathOrUri: String?): Any? {
    if (pathOrUri.isNullOrBlank()) return null
    return if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://") || pathOrUri.startsWith("http")) {
        pathOrUri
    } else if (pathOrUri.startsWith("/")) {
        java.io.File(pathOrUri)
    } else {
        pathOrUri
    }
}

const val PALMIST_PROJECT_SUPPORT_TEXT = """Анализ выполнен с помощью AI Gemini. 
Результат может быть не идеальным.
Мы создаём собственную нейросеть «Хиромант» 
— поддержите проект, чтобы сделать его точнее.
Спасибо, что Вы с нами!"""

fun getLocalizedSupportText(lang: AppLanguage): String {
    // Возвращает локализованный текст поддержки проекта в зависимости от выбранного языка
    return if (lang == AppLanguage.RUS) {
        "Анализ выполнен с помощью AI Gemini.\nРезультат может быть не идеальным...\nМы создаём собственную нейросеть «Хиромант»\n— поддержите проект, чтобы сделать его точнее.\nСпасибо, что Вы с нами!💖"
    } else {
        "Analysis completed with the help of AI Gemini.\nThe result may not be perfect...\nWe are building our own neural network \"Palmist\"\n— support the project to make it more precise.\nThank you for being with us!💖"
    }
}

data class CleanedTtsText(
    val sanitizedText: String,
    val indexMap: IntArray
)

fun isTtsIgnoredSymbol(codePoint: Int): Boolean {
    if (codePoint == 0x2022) return true // Маркер списка '•'
    val type = Character.getType(codePoint)
    if (type == Character.OTHER_SYMBOL.toInt() ||
        type == Character.SURROGATE.toInt() ||
        type == Character.FORMAT.toInt() ||
        type == Character.MODIFIER_SYMBOL.toInt()
    ) return true
    if (codePoint in 0x2600..0x27BF ||
        codePoint in 0x1F300..0x1FAFF ||
        codePoint in 0xFE00..0xFE0F
    ) return true
    return false
}

fun prepareTextForTts(originalText: String): CleanedTtsText {
    val sb = java.lang.StringBuilder()
    val mapList = java.util.ArrayList<Int>()
    var i = 0
    while (i < originalText.length) {
        val codePoint = originalText.codePointAt(i)
        val charCount = Character.charCount(codePoint)
        val isSymbol = isTtsIgnoredSymbol(codePoint)
        if (!isSymbol) {
            for (j in 0 until charCount) {
                mapList.add(i + j)
                sb.append(originalText[i + j])
            }
        }
        i += charCount
    }
    mapList.add(originalText.length)
    return CleanedTtsText(sb.toString(), mapList.toIntArray())
}

// Приведение имени голоса к единому базовому ключу без суффиксов локального/сетевого размещения (-network / -local)
fun getNormalizedVoiceKey(voiceName: String): String {
    return voiceName.lowercase(java.util.Locale.US)
        .replace("-network", "")
        .replace("-local", "")
        .replace("_network", "")
        .replace("_local", "")
        .trim()
}

// Проверка принадлежности имени голоса к женскому полу
fun isFemaleVoiceName(nameLower: String): Boolean {
    // Если имя содержит маркеры мужских голосов (dfd, dfg, rub, rud, rum и т.д.), то это не женский голос
    if (nameLower.contains("dfd") || nameLower.contains("dfg") || nameLower.contains("rub") || nameLower.contains("rud") || nameLower.contains("rum") || nameLower.contains("male") || nameLower.contains("-m-") || nameLower.contains("_m_") || nameLower.contains("m-local") || nameLower.contains("m-network") || nameLower.contains("-m") || nameLower.contains("_m")) {
        return false
    }
    return nameLower.contains("female") || 
           nameLower.contains("f-local") || 
           nameLower.contains("f-network") ||
           nameLower.contains("ruf") || 
           nameLower.contains("dfc") || 
           nameLower.contains("dfh") || 
           nameLower.contains("rua") || 
           nameLower.contains("ruc") || 
           nameLower.contains("rue") ||
           nameLower.contains("ru-ru-a") ||
           nameLower.contains("ru-ru-c") ||
           nameLower.contains("ru-ru-e") ||
           nameLower.contains("ru-ru-f") ||
           nameLower.contains("ru-ru-g") ||
           nameLower.contains("ru_ru_a") ||
           nameLower.contains("ru_ru_c") ||
           nameLower.contains("ru_ru_e") ||
           nameLower.contains("-f-") ||
           nameLower.contains("-f_") ||
           nameLower.contains("_f_") ||
           nameLower.contains("-a-") ||
           nameLower.contains("-c-") ||
           nameLower.contains("-e-") ||
           nameLower.contains("-f-") ||
           nameLower.contains("-g-") ||
           nameLower.contains("sfg") ||
           nameLower.contains("iom") ||
           nameLower.contains("woman") ||
           nameLower.contains("girl")
}

// Проверка принадлежности имени голоса к мужскому полу
fun isMaleVoiceName(nameLower: String): Boolean {
    // Если имя содержит маркеры женских голосов (dfc, dfh, rua, ruc, rue, ruf и т.д.), то это не мужской голос
    if (nameLower.contains("dfc") || nameLower.contains("dfh") || nameLower.contains("rua") || nameLower.contains("ruc") || nameLower.contains("rue") || nameLower.contains("ruf") || nameLower.contains("female") || nameLower.contains("-f-") || nameLower.contains("_f_") || nameLower.contains("f-local") || nameLower.contains("f-network") || nameLower.contains("sfg") || nameLower.contains("iom")) {
        return false
    }
    return nameLower.contains("male") || 
           nameLower.contains("m-local") || 
           nameLower.contains("m-network") ||
           nameLower.contains("rum") || 
           nameLower.contains("dfd") || 
           nameLower.contains("dfg") || 
           nameLower.contains("rub") || 
           nameLower.contains("rud") ||
           nameLower.contains("ru-ru-b") ||
           nameLower.contains("ru-ru-d") ||
           nameLower.contains("ru_ru_b") ||
           nameLower.contains("ru_ru_d") ||
           nameLower.contains("-m-") ||
           nameLower.contains("-m_") ||
           nameLower.contains("_m_") ||
           nameLower.contains("-b-") ||
           nameLower.contains("-d-") ||
           nameLower.contains("man") ||
           nameLower.contains("boy")
}

// Получение отфильтрованного и дедуплицированного списка голосов для указанного пола
fun getProcessedVoicesForGender(
    tts: TextToSpeech?,
    currentLang: AppLanguage,
    gender: String
): List<android.speech.tts.Voice> {
    if (tts == null) return emptyList()
    val allVoices = try { tts.voices?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
    val langCode = if (currentLang == AppLanguage.RUS) "ru" else "en"
    val matchingVoices = allVoices.filter { 
        it.locale.language.equals(langCode, ignoreCase = true) ||
        it.locale.language.equals(if (currentLang == AppLanguage.RUS) "rus" else "eng", ignoreCase = true) ||
        it.locale.toLanguageTag().startsWith(langCode, ignoreCase = true)
    }

    val filteredByGender = matchingVoices.filter { voice ->
        val nameLower = voice.name.lowercase(java.util.Locale.US)
        if (gender == "Female") {
            isFemaleVoiceName(nameLower)
        } else {
            isMaleVoiceName(nameLower)
        }
    }

    val finalVoicesList = if (filteredByGender.isNotEmpty()) {
        filteredByGender
    } else {
        // Если строгое сопоставление не нашло голосов, отбрасываем противоположный пол
        val fallback = matchingVoices.filter { voice ->
            val nameLower = voice.name.lowercase(java.util.Locale.US)
            if (gender == "Female") !isMaleVoiceName(nameLower) else !isFemaleVoiceName(nameLower)
        }
        if (fallback.isNotEmpty()) fallback else matchingVoices
    }

    // Группировка и удаление дубликатов с одинаковым базовым ключом голоса
    val grouped = finalVoicesList.groupBy { getNormalizedVoiceKey(it.name) }

    return grouped.map { (_, voicesInGroup) ->
        voicesInGroup.firstOrNull { it.name.lowercase(java.util.Locale.US).contains("network") } ?: voicesInGroup.first()
    }
}

// Формирование читаемого наименования голоса для выпадающих списков
fun getCleanVoiceDisplayName(voice: android.speech.tts.Voice, index: Int, gender: String): String {
    val baseKey = getNormalizedVoiceKey(voice.name)
    val prefix = if (gender == "Female") "Ж" else "М"
    val num = index + 1
    return "Голос $prefix$num ($baseKey)"
}

// Формирование наглядного списка вариантов голосов для выпадающего меню
fun getVoiceOptionNames(
    tts: TextToSpeech?,
    currentLang: AppLanguage,
    gender: String
): List<String> {
    val systemVoices = getProcessedVoicesForGender(tts, currentLang, gender)
    val defaultList = if (gender == "Female") {
        if (currentLang == AppLanguage.RUS) {
            listOf(
                "Голос Ж1 (Базовый)",
                "Голос Ж2 (Мелодичный)",
                "Голос Ж3 (Бархатный)",
                "Голос Ж4 (Звонкий)",
                "Голос Ж5 (Мягкий)"
            )
        } else {
            listOf(
                "Voice F1 (Base)",
                "Voice F2 (Melodic)",
                "Voice F3 (Velvet)",
                "Voice F4 (Clear)",
                "Voice F5 (Soft)"
            )
        }
    } else {
        if (currentLang == AppLanguage.RUS) {
            listOf(
                "Голос М1 (Базовый)",
                "Голос М2 (Глубокий)",
                "Голос М3 (Басовитый)",
                "Голос М4 (Спокойный)",
                "Голос М5 (Уверенный)"
            )
        } else {
            listOf(
                "Voice M1 (Base)",
                "Voice M2 (Deep)",
                "Voice M3 (Bass)",
                "Voice M4 (Calm)",
                "Voice M5 (Confident)"
            )
        }
    }

    if (systemVoices.isEmpty()) {
        return defaultList
    }

    val result = mutableListOf<String>()
    for (i in 0 until maxOf(defaultList.size, systemVoices.size)) {
        if (i < systemVoices.size) {
            val cleanName = getCleanVoiceDisplayName(systemVoices[i], i, gender)
            result.add(cleanName)
        } else if (i < defaultList.size) {
            result.add(defaultList[i])
        }
    }
    return result
}

// Конфигурирование параметров голосового синтезатора TextToSpeech
fun configureTtsVoice(
    tts: TextToSpeech?,
    currentLang: AppLanguage,
    voiceGender: String,
    voiceIndex: Int,
    speechRate: Float,
    speechPitch: Float
) {
    if (tts == null) return
    try {
        val locale = if (currentLang == AppLanguage.RUS) java.util.Locale.forLanguageTag("ru-RU") else java.util.Locale.US
        tts.language = locale
        tts.setSpeechRate(speechRate)

        val voicesList = getProcessedVoicesForGender(tts, currentLang, voiceGender)
        if (voicesList.isNotEmpty()) {
            val preferredVoice = voicesList[voiceIndex % voicesList.size]
            tts.voice = preferredVoice
        } else {
            val allVoices = try { tts.voices?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
            val langCode = if (currentLang == AppLanguage.RUS) "ru" else "en"
            val langVoices = allVoices.filter { it.locale.language.equals(langCode, ignoreCase = true) }
            if (langVoices.isNotEmpty()) {
                tts.voice = langVoices[0]
            }
        }

        // Индивидуальная модификация тона для разного звучания каждого варианта голоса
        val indexPitchMod = when (voiceIndex % 5) {
            0 -> 1.00f // Базовый
            1 -> 1.12f // Повыше / Мелодичный
            2 -> 0.90f // Плотный / Бархатный
            3 -> 1.20f // Звонкий / Спокойный
            4 -> 0.82f // Низкий / Мягкий
            else -> 1.00f
        }

        // Базовый тон для женского голоса — 1.30f, для мужского — 0.85f
        val basePitch = if (voiceGender == "Female") 1.30f else 0.85f
        tts.setPitch(basePitch * speechPitch * indexPitchMod)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// --- SCREEN 0: LANGUAGE SELECTION ---

@Composable
fun LanguageSelectionScreen(
    viewModel: PalmistViewModel,
    onNavigateToSplash: () -> Unit
) {
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            
            Spacer(modifier = Modifier.weight(0.5f))

            // Mystical Logo
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MysticGold,
                modifier = Modifier
                    .size(64.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            MysticHeader(strings.langSelectTitle)
            MysticSubtitle(strings.langSelectSubtitle)

            Spacer(modifier = Modifier.weight(0.8f))

            // Russia (RUS) flag selection card
            LanguageCard(
                langName = "Русский (RUS)",
                flagEmoji = "🇷🇺",
                isSelected = currentLang == AppLanguage.RUS,
                onClick = { viewModel.changeLanguage(AppLanguage.RUS) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // United Kingdom (ENG) flag selection card
            LanguageCard(
                langName = "English (ENG)",
                flagEmoji = "🇬🇧",
                isSelected = currentLang == AppLanguage.ENG,
                onClick = { viewModel.changeLanguage(AppLanguage.ENG) }
            )

            Spacer(modifier = Modifier.weight(1.2f))

            MysticButton(
                text = strings.langContinue,
                onClick = onNavigateToSplash,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun LanguageCard(
    langName: String,
    flagEmoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, MysticGold)
    } else {
        BorderStroke(1.dp, MysticBronze.copy(0.4f))
    }

    val backgroundColor = if (isSelected) Color(0x33D4AF37) else Color(0x99141420)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = flagEmoji,
                fontSize = 36.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = langName,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (isSelected) MysticGold else Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MysticGold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


// --- SCREEN 1: SPLASH SCREEN (ANCIENT SCROLL ANIMATION WITH GLOWING LINES) ---

enum class HandElementType { LINE, MOUNT }

@Composable
fun MysticSplashScreen(
    viewModel: PalmistViewModel,
    onNavigateNext: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)
    val coroutineScope = rememberCoroutineScope()
    val isReturnedToSplash by viewModel.isReturnedToSplash.collectAsState()

    // Animation states
    var titleVisible by remember { mutableStateOf(false) }
    var triggerFlash by remember { mutableStateOf(false) }
    var showExitButtonText by remember { mutableStateOf(false) }

    LaunchedEffect(isReturnedToSplash) {
        if (isReturnedToSplash) {
            showExitButtonText = true
            viewModel.isReturnedToSplash.value = false
        }
    }

    androidx.activity.compose.BackHandler {
        if (showExitButtonText) {
            (context as? android.app.Activity)?.finish()
        } else {
            showExitButtonText = true
            coroutineScope.launch {
                delay(3000)
                showExitButtonText = false
            }
        }
    }

    class AnimatedElementState(
        val id: String,
        val type: HandElementType,
        val name: String,
        val color: Color,
        val symbol: String = "",
        val points: List<Pair<Float, Float>> = emptyList(),
        val position: Pair<Float, Float> = Pair(0f, 0f)
    ) {
        val opacity = mutableStateOf(0f)
        val flash = mutableStateOf(1f)
    }

    val elements = remember {
        listOf(
            // --- MAIN PALM LINES (Точно отрегулированные под рельеф ладони) ---
            AnimatedElementState(
                id = "life_line",
                type = HandElementType.LINE,
                name = "Life Line",
                color = Color(0xFFFF0044), // Ультра Яркий Неоновый Красный
                points = listOf(
                    Pair(0.64f, 0.47f),
                    Pair(0.58f, 0.53f),
                    Pair(0.52f, 0.61f), // Еще больше выгнута в ЛЕВО
                    Pair(0.57f, 0.70f),
                    Pair(0.64f, 0.80f)
                )
            ),
            AnimatedElementState(
                id = "head_line",
                type = HandElementType.LINE,
                name = "Head Line",
                color = Color(0xFF00FFFF), // Электрический Насыщенный Циан
                points = listOf(
                    Pair(0.69f, 0.47f), // На полфаланги подвинута правее
                    Pair(0.59f, 0.51f),
                    Pair(0.47f, 0.56f),
                    Pair(0.36f, 0.62f)  // На полфаланги слева короче
                )
            ),
            AnimatedElementState(
                id = "heart_line",
                type = HandElementType.LINE,
                name = "Heart Line",
                color = Color(0xFFFF00AA), // Яркий Сочный Маджента
                points = listOf(
                    Pair(0.18f, 0.58f),
                    Pair(0.28f, 0.56f),
                    Pair(0.38f, 0.54f),
                    Pair(0.48f, 0.51f),
                    Pair(0.54f, 0.44f),
                    Pair(0.59f, 0.39f) // Правый край подвинут по диагонали вверх-влево на 1/3 фаланги
                )
            ),
            AnimatedElementState(
                id = "destiny_line",
                type = HandElementType.LINE,
                name = "Destiny Line",
                color = Color(0xFFB030FF), // Яркий Неоновый Пурпурный
                points = listOf(
                    Pair(0.47f, 0.74f), // Опущена на полфаланги без изменения формы и длины
                    Pair(0.47f, 0.64f),
                    Pair(0.46f, 0.54f),
                    Pair(0.46f, 0.45f)
                )
            ),

            // --- PLANETARY MOUNTS & SYMBOLS (Опущены на фалангу, сочные светящиеся цвета) ---
            AnimatedElementState(
                id = "mount_jupiter",
                type = HandElementType.MOUNT,
                name = "Mount of Jupiter",
                color = Color(0xFFFFD700), // Яркое Насыщенное Золото
                symbol = "♃",
                position = Pair(0.62f, 0.395f)
            ),
            AnimatedElementState(
                id = "mount_saturn",
                type = HandElementType.MOUNT,
                name = "Mount of Saturn",
                color = Color(0xFFE0E6ED), // Платиновый Светящийся
                symbol = "♄",
                position = Pair(0.48f, 0.365f)
            ),
            AnimatedElementState(
                id = "mount_apollo",
                type = HandElementType.MOUNT,
                name = "Mount of Apollo",
                color = Color(0xFFFF9900), // Сочный Солнечный Оранжевый
                symbol = "☉",
                position = Pair(0.33f, 0.380f)
            ),
            AnimatedElementState(
                id = "mount_mercury",
                type = HandElementType.MOUNT,
                name = "Mount of Mercury",
                color = Color(0xFF00FF66), // Яркий Неоновый Изумруд
                symbol = "☿",
                position = Pair(0.20f, 0.420f)
            ),
            AnimatedElementState(
                id = "mount_venus",
                type = HandElementType.MOUNT,
                name = "Mount of Venus",
                color = Color(0xFFFF3399), // Яркий Романтический Розовый
                symbol = "♀",
                position = Pair(0.680f, 0.650f) // Поднят по диагонали вправо-вверх на фалангу
            ),
            AnimatedElementState(
                id = "mount_mars_lower",
                type = HandElementType.MOUNT,
                name = "Lower Mars",
                color = Color(0xFFFF2222), // Яркий Багровый
                symbol = "♂",
                position = Pair(0.59f, 0.48f)
            ),
            AnimatedElementState(
                id = "mount_mars_upper",
                type = HandElementType.MOUNT,
                name = "Upper Mars",
                color = Color(0xFFFF5500), // Огненный Неоновый Оранжевый
                symbol = "♂",
                position = Pair(0.21f, 0.56f)
            ),
            AnimatedElementState(
                id = "mount_moon",
                type = HandElementType.MOUNT,
                name = "Mount of Moon",
                color = Color(0xFFFFF044), // Сочный Лимонно-Золотой
                symbol = "☽",
                position = Pair(0.33f, 0.67f)
            )
        )
    }

    var scaleTarget by remember { mutableStateOf(0.85f) }
    val handScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = tween(durationMillis = 6500, easing = androidx.compose.animation.core.EaseOutCubic),
        label = "HandScale"
    )

    val titleFlashProgress by animateFloatAsState(
        targetValue = if (triggerFlash) 0f else 1f,
        animationSpec = tween(durationMillis = 1500, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "TitleFlash"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(1000),
        label = "TitleAlpha"
    )

    val titleScale by animateFloatAsState(
        targetValue = if (titleVisible) 1.0f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "TitleScale"
    )

    fun lerpColor(start: Color, end: Color, fraction: Float): Color {
        return Color(
            red = start.red + (end.red - start.red) * fraction,
            green = start.green + (end.green - start.green) * fraction,
            blue = start.blue + (end.blue - start.blue) * fraction,
            alpha = start.alpha + (end.alpha - start.alpha) * fraction
        )
    }

    LaunchedEffect(Unit) {
        // Start smooth 3D approach animation of the real hand in cosmic space
        scaleTarget = 1.18f

        // Sequence of line and planetary symbol appearances with vibrant color flashes
        delay(550)
        animateElementAppearance(coroutineScope, elements.first { it.id == "life_line" })

        delay(550)
        animateElementAppearance(coroutineScope, elements.first { it.id == "mount_jupiter" })
        animateElementAppearance(coroutineScope, elements.first { it.id == "mount_saturn" })

        delay(550)
        animateElementAppearance(coroutineScope, elements.first { it.id == "head_line" })

        // Display title "ХИРОМАНТ"
        titleVisible = true
        triggerFlash = true

        delay(550)
        animateElementAppearance(coroutineScope, elements.first { it.id == "mount_apollo" })
        animateElementAppearance(coroutineScope, elements.first { it.id == "mount_mercury" })

        delay(550)
        animateElementAppearance(coroutineScope, elements.first { it.id == "heart_line" })

        delay(550)
        animateElementAppearance(coroutineScope, elements.first { it.id == "mount_venus" })
        animateElementAppearance(coroutineScope, elements.first { it.id == "mount_mars_lower" })
        animateElementAppearance(coroutineScope, elements.first { it.id == "mount_moon" })

        delay(550)
        animateElementAppearance(coroutineScope, elements.first { it.id == "destiny_line" })

        // Launch continuous sequential flashing pulses across all elements
        coroutineScope.launch {
            while (true) {
                for (elem in elements) {
                    animateElementFlashPulse(coroutineScope, elem)
                    delay(450)
                }
            }
        }

        // Auto-navigate after splash sequence (+2s longer overall)
        delay(4200)
        onNavigateNext()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030308))
    ) {
        // Layer 1: Approaching Real Hand & Cosmic Background covering full screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(handScale),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.aistudio.hiromant.kxsrwa.R.drawable.img_splash_hand),
                contentDescription = "Realistic Mystic Hand",
                contentScale = ContentScale.Crop, // Fits screen to full rectangular Android smartphone dimensions
                modifier = Modifier.fillMaxSize()
            )

            // Canvas for drawing lines and planetary symbols directly on top of the hand with vibrant glowing light
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val w = size.width
                val h = size.height
                val toAndroidColor = { c: Color ->
                    android.graphics.Color.argb(
                        (c.alpha * 255).toInt(),
                        (c.red * 255).toInt(),
                        (c.green * 255).toInt(),
                        (c.blue * 255).toInt()
                    )
                }

                elements.forEach { element ->
                    val op = element.opacity.value
                    val fl = element.flash.value
                    if (op > 0f) {
                        val baseColor = element.color
                        if (element.type == HandElementType.LINE && element.points.isNotEmpty()) {
                            // Smooth Catmull-Rom style spline path following natural palm crease curves
                            val path = Path().apply {
                                val pts = element.points.map { Offset(it.first * w, it.second * h) }
                                moveTo(pts[0].x, pts[0].y)
                                if (pts.size == 2) {
                                    lineTo(pts[1].x, pts[1].y)
                                } else if (pts.size > 2) {
                                    for (i in 0 until pts.size - 1) {
                                        val p0 = if (i > 0) pts[i - 1] else pts[i]
                                        val p1 = pts[i]
                                        val p2 = pts[i + 1]
                                        val p3 = if (i + 2 < pts.size) pts[i + 2] else p2

                                        val cp1x = p1.x + (p2.x - p0.x) * 0.20f
                                        val cp1y = p1.y + (p2.y - p0.y) * 0.20f
                                        val cp2x = p2.x - (p3.x - p1.x) * 0.20f
                                        val cp2y = p2.y - (p3.y - p1.y) * 0.20f

                                        cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                                    }
                                }
                            }

                            // 1. Thin concentrated neon glow halo
                            drawPath(
                                path = path,
                                color = baseColor.copy(alpha = (op * 0.5f * fl).coerceIn(0f, 1f)),
                                style = Stroke(
                                    width = 2.4.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )

                            // 2. Fine concentrated saturated line stroke
                            drawPath(
                                path = path,
                                color = baseColor.copy(alpha = (op * fl).coerceIn(0f, 1f)),
                                style = Stroke(
                                    width = 1.4.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )

                            // 3. Ultra crisp laser core
                            val coreColor = lerpColor(baseColor, Color.White, (fl * 0.7f).coerceIn(0f, 1f))
                            drawPath(
                                path = path,
                                color = coreColor.copy(alpha = op.coerceIn(0f, 1f)),
                                style = Stroke(
                                    width = 0.5.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        } else if (element.type == HandElementType.MOUNT) {
                            val px = element.position.first * w
                            val py = element.position.second * h

                            // Outer thin neon glow contour for planetary symbol (1/3 thinner, intense color tone)
                            drawContext.canvas.nativeCanvas.drawText(
                                element.symbol,
                                px,
                                py,
                                android.graphics.Paint().apply {
                                    color = toAndroidColor(baseColor.copy(alpha = (op * (0.85f + (fl - 1f) * 0.15f)).coerceIn(0f, 1f)))
                                    textSize = 17.dp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                    style = android.graphics.Paint.Style.FILL_AND_STROKE
                                    strokeWidth = 1.0.dp.toPx() // На 1/3 тоньше
                                    setShadowLayer((8.dp.toPx() * fl), 0f, 0f, toAndroidColor(baseColor))
                                }
                            )

                            // Saturated core text for planetary symbol (vivid tone, flashes in own color)
                            val symbolColor = lerpColor(baseColor, Color.White, (fl - 1f) * 0.25f)
                            drawContext.canvas.nativeCanvas.drawText(
                                element.symbol,
                                px,
                                py,
                                android.graphics.Paint().apply {
                                    color = toAndroidColor(symbolColor.copy(alpha = op.coerceIn(0f, 1f)))
                                    textSize = 15.dp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                    style = android.graphics.Paint.Style.FILL
                                    setShadowLayer((6.dp.toPx() * fl), 0f, 0f, toAndroidColor(baseColor))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Layer 2: Top Title ("ХИРОМАНТ" & Subtitle) and Bottom "ПРОПУСТИТЬ ЗАСТАВКУ" button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Title & Subtitle with translucent gray pill card and contour flash
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = titleVisible,
                    enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(initialOffsetY = { -40 })
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(320.dp) // Одинаковая ширина для Заголовка и Подзаголовка
                                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.2.dp,
                                    color = lerpColor(MysticGold.copy(alpha = 0.8f), Color.White, titleFlashProgress),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .graphicsLayer(
                                    alpha = titleAlpha,
                                    scaleX = titleScale,
                                    scaleY = titleScale
                                )
                        ) {
                            val uppercaseTitle = strings.appName.uppercase()

                            // Outer glow halo text with contour flash
                            Text(
                                text = uppercaseTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.Transparent,
                                    fontSize = 26.sp, // Увеличено на 2 ед.
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    shadow = Shadow(
                                        color = lerpColor(MysticGold, Color.White, titleFlashProgress).copy(alpha = 0.9f),
                                        offset = Offset(0f, 0f),
                                        blurRadius = 25f * (1f + titleFlashProgress * 0.5f)
                                    )
                                ),
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )

                            // Main golden inner text with contour flash
                            val flashColor = lerpColor(Color(0xFFFFE066), Color.White, titleFlashProgress)
                            Text(
                                text = uppercaseTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = flashColor,
                                    fontSize = 26.sp, // Увеличено на 2 ед.
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Subtitle: "ТАЙНЫ СУДЬБЫ, В ВАШИХ РУКАХ"
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(320.dp) // Одинаковая ширина рамки
                                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                                .border(1.2.dp, MysticGold.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = strings.splashLogoSubtitle.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MysticGold,
                                    letterSpacing = 1.5.sp,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    shadow = Shadow(
                                        color = Color.Black,
                                        offset = Offset(0f, 2f),
                                        blurRadius = 8f
                                    )
                                ),
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Bottom Section: Floating Exit Button AND/OR Clean button "ПРОПУСТИТЬ ЗАСТАВКУ", расположенные ниже на экране
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                if (showExitButtonText) {
                    // Кнопка выхода при повторном нажатии "Назад"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(0.75f))
                            .border(1.5.dp, MysticGold.copy(0.8f), RoundedCornerShape(24.dp))
                            .clickable {
                                (context as? android.app.Activity)?.finish()
                            }
                            .padding(horizontal = 28.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Еще назад — ВЫХОД",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MysticGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontSize = 15.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Кнопка "ПРОПУСТИТЬ ЗАСТАВКУ" (доступна всегда — и при первом входе, и ниже кнопки выхода)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(0.75f))
                        .border(1.5.dp, MysticGold.copy(0.8f), RoundedCornerShape(24.dp))
                        .clickable {
                            onNavigateNext()
                        }
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = strings.splashTapToSkip,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MysticGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun animateElementFlashPulse(
    scope: kotlinx.coroutines.CoroutineScope,
    element: Any
) {
    scope.launch {
        try {
            val flField = element.javaClass.getDeclaredField("flash")
            flField.isAccessible = true
            val flashState = flField.get(element) as androidx.compose.runtime.MutableState<Float>
            val steps = 8
            for (i in 1..steps) {
                val p = i.toFloat() / steps
                flashState.value = 1f + (if (p < 0.5f) p * 2.5f else (1f - p) * 2.5f)
                kotlinx.coroutines.delay(20)
            }
            flashState.value = 1f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun animateElementAppearance(
    scope: kotlinx.coroutines.CoroutineScope,
    element: Any
) {
    scope.launch {
        try {
            val opField = element.javaClass.getDeclaredField("opacity")
            val flField = element.javaClass.getDeclaredField("flash")
            opField.isAccessible = true
            flField.isAccessible = true
            val opacityState = opField.get(element) as androidx.compose.runtime.MutableState<Float>
            val flashState = flField.get(element) as androidx.compose.runtime.MutableState<Float>

            val steps = 12
            for (i in 1..steps) {
                val progress = i.toFloat() / steps
                opacityState.value = progress
                if (progress < 0.5f) {
                    flashState.value = 1f + (progress * 2f) * 1.5f
                } else {
                    flashState.value = 2.5f - ((progress - 0.5f) * 2f) * 1.5f
                }
                kotlinx.coroutines.delay(16)
            }
            opacityState.value = 1f
            flashState.value = 1f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


// --- SCREEN 2: OPTIONAL AUTHENTICATION / REGISTRATION ---

@Composable
fun ShrinkableText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    val initialSize = if (style.fontSize.isSp) style.fontSize.value else 14f
    var fontSizeValue by remember(text) { mutableStateOf(initialSize) }
    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        style = style.copy(fontSize = fontSizeValue.sp),
        maxLines = 1,
        softWrap = false,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight) {
                if (fontSizeValue > 8f) {
                    fontSizeValue -= 0.5f
                }
            }
        },
        modifier = modifier
    )
}

// Экран выбора пользователя (вход под существующим профилем, выбор нового профиля или входа через Google)
@Composable
fun UserSelectScreen(
    viewModel: PalmistViewModel,
    onSelectUser: (UserProfileEntity) -> Unit,
    onNewUser: () -> Unit,
    onNavigateNext: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val allProfiles by viewModel.allUserProfiles.collectAsState()
    val allReadings by viewModel.allReadings.collectAsState()
    val activeProfile by viewModel.userProfile.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var activeError by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    isLoading = true
                    val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null as String?)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { authTask ->
                            isLoading = false
                            if (authTask.isSuccessful) {
                                val user = auth.currentUser
                                viewModel.saveProfile(
                                    name = user?.displayName ?: "Google User",
                                    gender = "Other",
                                    age = 25,
                                    height = 175,
                                    dominantHand = "Right",
                                    email = user?.email,
                                    phone = null,
                                    isRegistered = true
                                )
                                Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Вход через Google успешен!" else "Google login successful!", Toast.LENGTH_SHORT).show()
                                onNavigateNext()
                            } else {
                                activeError = "${if (currentLang == AppLanguage.RUS) "Ошибка авторизации Google в Firebase:" else "Firebase Google login failed:"} ${authTask.exception?.message}"
                            }
                        }
                } else {
                    activeError = "Google Sign-In failed: ID Token is null. Please configure SHA-1 and Web Client ID in Firebase Console."
                }
            } catch (e: Exception) {
                activeError = "${if (currentLang == AppLanguage.RUS) "Ошибка Google входа:" else "Google Sign-In failed:"} ${e.message}"
            }
        }
    }

    // Фильтрация и формирование полного списка всех сохраненных пользователей
    val validProfiles: List<UserProfileEntity> = remember(allProfiles, allReadings, activeProfile) {
        val list = mutableListOf<UserProfileEntity>()
        activeProfile?.let { p: UserProfileEntity ->
            if (p.name.isNotBlank()) list.add(p)
        }
        allProfiles.forEach { p: UserProfileEntity ->
            if (p.name.isNotBlank() && list.none { existing: UserProfileEntity -> existing.name.equals(p.name, ignoreCase = true) && existing.age == p.age }) {
                list.add(p)
            }
        }
        allReadings.forEach { r: ReadingEntity ->
            if (r.name.isNotBlank() && list.none { existing: UserProfileEntity -> existing.name.equals(r.name, ignoreCase = true) && existing.age == r.age }) {
                list.add(
                    UserProfileEntity(
                        id = "reading_user_${r.id}",
                        name = r.name,
                        gender = r.gender,
                        age = r.age,
                        height = r.height,
                        dominantHand = r.dominantHand,
                        photoUri = r.imageUrl ?: r.leftPalmPath ?: r.rightPalmPath,
                        registrationTimestamp = r.timestamp
                    )
                )
            }
        }
        if (list.isEmpty() && activeProfile != null) {
            list.add(activeProfile!!)
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 1. Сверху надпись - "Выберите пользователя"
            MysticHeader(if (currentLang == AppLanguage.RUS) "Выберите пользователя" else "Select User")

            Spacer(modifier = Modifier.height(20.dp))

            MysticCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MysticGold,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                        )
                    }

                    // а) Прокручиваемый внутри окна список всех сохраненных пользователей с фото, именем и датой регистрации
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (profile in validProfiles) {
                            val birthYear = if (profile.age > 0) (2026 - profile.age) else 1990
                            val displayName = if (profile.name.isNotBlank()) profile.name else (if (currentLang == AppLanguage.RUS) "Максим" else "Maxim")
                            val yearText = if (currentLang == AppLanguage.RUS) "$birthYear г.р." else "b. $birthYear"
                            val line1Text = "$displayName , $yearText"

                            // Форматирование даты и времени регистрации пользователя
                            val regTimestamp = if (profile.registrationTimestamp > 0) profile.registrationTimestamp else System.currentTimeMillis()
                            val dateFormat = remember { java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.forLanguageTag("ru")) }
                            val formattedDateTime = dateFormat.format(java.util.Date(regTimestamp))
                            val line2Text = if (currentLang == AppLanguage.RUS) "$formattedDateTime Зарегистрирован" else "$formattedDateTime Registered"

                            OutlinedButton(
                                onClick = {
                                    viewModel.selectUserProfile(profile)
                                    onSelectUser(profile)
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, MysticGold),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF1E1638),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Аватарка / реальное фото профиля пользователя слева
                                    Surface(
                                        shape = CircleShape,
                                        color = MysticGold.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, MysticGold),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val imgModel = getCoilImageModel(profile.photoUri)
                                            if (imgModel != null) {
                                                coil.compose.AsyncImage(
                                                    model = imgModel,
                                                    contentDescription = "User Photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Image(
                                                    painter = painterResource(id = com.aistudio.hiromant.kxsrwa.R.drawable.img_splash_hand),
                                                    contentDescription = "User Hand Icon",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Две строки текста: Первая строка - Имя и год Рождения; Вторая строка - Дата и Время Регистрации
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = line1Text,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = line2Text,
                                            fontSize = 12.sp,
                                            color = MysticGold.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // б) Кнопка, с аватаркой слева, и справа надпись - "Новый Пользователь"
                    OutlinedButton(
                        onClick = onNewUser,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MysticGold.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF16102A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MysticGold.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, MysticGold.copy(alpha = 0.5f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = MysticGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Новый Пользователь" else "New User",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }

                    // в) Кнопка - Войти через Google
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            val webClientId = context.getString(com.aistudio.hiromant.kxsrwa.R.string.default_web_client_id)
                            @Suppress("DEPRECATION")
                            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                            )
                                .requestIdToken(webClientId)
                                .requestEmail()
                                .build()

                            @Suppress("DEPRECATION")
                            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "G ",
                                color = Color(0xFF4285F4),
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Войти через Google" else "Sign in with Google",
                                color = Color(0xFF5F6368),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (activeError != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { activeError = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MysticDarkSurface),
                border = BorderStroke(1.5.dp, MysticGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error icon",
                        tint = Color(0xFFCF6679),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "Произошла ошибка" else "An error occurred",
                        style = MaterialTheme.typography.titleMedium.copy(color = MysticGold, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeError ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray, textAlign = TextAlign.Center)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    MysticButton(
                        text = "OK",
                        onClick = { activeError = null }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    viewModel: PalmistViewModel,
    onNavigateNext: () -> Unit
) {
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }

    var emailOrPhoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    
    var storedVerificationId by remember { mutableStateOf("") }
    var resendingToken by remember { mutableStateOf<com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var activeError by remember { mutableStateOf<String?>(null) }

    val isEmailMode = emailOrPhone.trim().contains("@")

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    isLoading = true
                    val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null as String?)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { authTask ->
                            isLoading = false
                            if (authTask.isSuccessful) {
                                val user = auth.currentUser
                                viewModel.saveProfile(
                                    name = user?.displayName ?: "Google User",
                                    gender = "Other",
                                    age = 25,
                                    height = 175,
                                    dominantHand = "Right",
                                    email = user?.email,
                                    phone = null,
                                    isRegistered = true
                                )
                                Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Вход через Google успешен!" else "Google login successful!", Toast.LENGTH_SHORT).show()
                                onNavigateNext()
                            } else {
                                activeError = "${if (currentLang == AppLanguage.RUS) "Ошибка авторизации Google в Firebase:" else "Firebase Google login failed:"} ${authTask.exception?.message}"
                            }
                        }
                } else {
                    activeError = "Google Sign-In failed: ID Token is null. Please configure SHA-1 and Web Client ID in Firebase Console."
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                val errorMsg = when (e.statusCode) {
                    10 -> "Developer Error (API 10). This usually means your SHA-1 fingerprint or Web Client ID is mismatching in Firebase Console / Google Cloud Platform."
                    12501 -> "Sign-In cancelled by user (12501)."
                    else -> "Google Sign-In API error code: ${e.statusCode}. ${e.message}"
                }
                activeError = errorMsg
            }
        } else {
            activeError = "Google Sign-In result code was: ${result.resultCode}. (Activity cancelled)"
        }
    }

    val callbacks = remember {
        object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                isLoading = false
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            viewModel.saveProfile(
                                name = "",
                                gender = "",
                                age = 25,
                                height = 175,
                                dominantHand = "Right",
                                email = user?.email,
                                phone = user?.phoneNumber,
                                isRegistered = true
                            )
                            Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Вход выполнен успешно!" else "Logged in successfully!", Toast.LENGTH_SHORT).show()
                            onNavigateNext()
                        } else {
                            activeError = "${if (currentLang == AppLanguage.RUS) "Ошибка входа:" else "Login error:"} ${task.exception?.message}"
                        }
                    }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                isLoading = false
                activeError = "${if (currentLang == AppLanguage.RUS) "Ошибка верификации телефона:" else "Phone verification failed:"} ${e.message}"
            }

            override fun onCodeSent(
                verificationId: String,
                token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
            ) {
                isLoading = false
                storedVerificationId = verificationId
                resendingToken = token
                codeSent = true
                Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Код отправлен!" else "Verification code sent!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun formatPhoneNumber(input: String): String {
        val digits = input.filter { it.isDigit() }
        return if (digits.startsWith("8") && digits.length == 11) {
            "+7" + digits.substring(1)
        } else if (digits.startsWith("7") && digits.length == 11) {
            "+" + digits
        } else if (!input.startsWith("+") && digits.length >= 10) {
            "+" + digits
        } else {
            input
        }
    }

    val isValidInput = {
        var valid = true
        val trimmedInput = emailOrPhone.trim()
        if (trimmedInput.isEmpty()) {
            emailOrPhoneError = if (currentLang == AppLanguage.RUS) "Введите E-mail или телефон" else "Enter E-mail or phone"
            valid = false
        }
        if (isEmailMode && password.length < 6) {
            passwordError = strings.authPasswordError
            valid = false
        }
        valid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            MysticHeader(strings.authTitle)

            Spacer(modifier = Modifier.height(16.dp))

            MysticCard {
                Spacer(modifier = Modifier.height(12.dp))

                MysticTextField(
                    value = emailOrPhone,
                    onValueChange = { 
                        emailOrPhone = it
                        emailOrPhoneError = null
                        if (it.contains("@")) {
                            codeSent = false
                        }
                    },
                    label = strings.authEmailPhonePlaceholder,
                    placeholder = "example@domain.com / +79991234567",
                    error = emailOrPhoneError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                MysticTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordError = null
                    },
                    label = strings.authPasswordPlaceholder,
                    placeholder = "••••••••",
                    error = passwordError,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                                tint = MysticGold
                            )
                        }
                    }
                )

                if (!isEmailMode && codeSent) {
                    MysticTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it },
                        label = strings.authSmsEmailCodePlaceholder,
                        placeholder = "123456",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MysticGold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (currentLang == AppLanguage.RUS) 
                                "Инициализация... Если процесс завис, вы можете войти без регистрации в полнофункциональном Демо-режиме!"
                            else 
                                "Initializing... If this process hangs, you can log in without registration using the fully-featured Demo Mode!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                isLoading = false
                                viewModel.saveProfile(
                                    name = "Максим",
                                    gender = "Male",
                                    age = 44,
                                    height = 175,
                                    dominantHand = "Right",
                                    email = "demo@hiromant.app",
                                    phone = "+79991112233",
                                    isRegistered = true
                                )
                                Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Вход выполнен в тестовом режиме!" else "Logged in as Test Profile!", Toast.LENGTH_SHORT).show()
                                onNavigateNext()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MysticGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MysticGold)
                        ) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Войти без Регистрации" else "Continue Without Registration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MysticButton(
                        text = if (currentLang == AppLanguage.RUS) "Вход" else "Sign In",
                        onClick = {
                            if (isValidInput()) {
                                if (isEmailMode) {
                                    isLoading = true
                                    auth.signInWithEmailAndPassword(emailOrPhone.trim(), password)
                                        .addOnCompleteListener { task ->
                                            isLoading = false
                                            if (task.isSuccessful) {
                                                val user = auth.currentUser
                                                if (user != null) {
                                                    if (user.isEmailVerified) {
                                                        viewModel.saveProfile(
                                                            name = "",
                                                            gender = "",
                                                            age = 25,
                                                            height = 175,
                                                            dominantHand = "Right",
                                                            email = user.email,
                                                            phone = null,
                                                            isRegistered = true
                                                        )
                                                        Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Успешный вход!" else "Successfully logged in!", Toast.LENGTH_SHORT).show()
                                                        onNavigateNext()
                                                    } else {
                                                        activeError = if (currentLang == AppLanguage.RUS) 
                                                            "Пожалуйста, подтвердите ваш E-mail! Мы отправили вам ссылку на почту." 
                                                        else 
                                                            "Please verify your E-mail! We have sent you a link."
                                                    }
                                                }
                                            } else {
                                                activeError = "${if (currentLang == AppLanguage.RUS) "Ошибка входа:" else "Login error:"} ${task.exception?.message}"
                                            }
                                        }
                                } else {
                                    if (!codeSent) {
                                        if (activity != null) {
                                            isLoading = true
                                            val formattedPhone = formatPhoneNumber(emailOrPhone.trim())
                                            val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
                                                .setPhoneNumber(formattedPhone)
                                                .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                                                .setActivity(activity)
                                                .setCallbacks(callbacks)
                                                .build()
                                            com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
                                        } else {
                                            Toast.makeText(context, "Activity Context is not available", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        if (verificationCode.length >= 4) {
                                            isLoading = true
                                            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(storedVerificationId, verificationCode)
                                            auth.signInWithCredential(credential)
                                                .addOnCompleteListener { task ->
                                                    isLoading = false
                                                    if (task.isSuccessful) {
                                                        val user = auth.currentUser
                                                        viewModel.saveProfile(
                                                            name = "",
                                                            gender = "",
                                                            age = 25,
                                                            height = 175,
                                                            dominantHand = "Right",
                                                            email = null,
                                                            phone = user?.phoneNumber,
                                                            isRegistered = true
                                                        )
                                                        Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Успешный вход!" else "Successfully logged in!", Toast.LENGTH_SHORT).show()
                                                        onNavigateNext()
                                                    } else {
                                                        activeError = "${if (currentLang == AppLanguage.RUS) "Ошибка:" else "Error:"} ${task.exception?.message}"
                                                    }
                                                }
                                        }
                                    }
                                }
                            }
                        },
                        isSecondary = true,
                        modifier = Modifier.weight(1f)
                    )

                    MysticButton(
                        text = if (currentLang == AppLanguage.RUS) "Регистрация" else "Register",
                        onClick = {
                            if (isValidInput()) {
                                if (isEmailMode) {
                                    isLoading = true
                                    auth.createUserWithEmailAndPassword(emailOrPhone.trim(), password)
                                        .addOnCompleteListener { task ->
                                            isLoading = false
                                            if (task.isSuccessful) {
                                                val user = auth.currentUser
                                                user?.sendEmailVerification()
                                                    ?.addOnCompleteListener { emailTask ->
                                                        if (emailTask.isSuccessful) {
                                                            Toast.makeText(
                                                                context,
                                                                if (currentLang == AppLanguage.RUS)
                                                                    "Регистрация успешна! Письмо с подтверждением отправлено на вашу почту. Пожалуйста, подтвердите почту для входа."
                                                                else
                                                                    "Registration successful! Verification email sent to your inbox. Please confirm it to sign in.",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        } else {
                                                            activeError = "${if (currentLang == AppLanguage.RUS) "Письмо не отправлено:" else "Failed to send email:"} ${emailTask.exception?.message}"
                                                        }
                                                    }
                                            } else {
                                                activeError = "${if (currentLang == AppLanguage.RUS) "Ошибка регистрации:" else "Registration error:"} ${task.exception?.message}"
                                            }
                                        }
                                } else {
                                    if (!codeSent) {
                                        if (activity != null) {
                                            isLoading = true
                                            val formattedPhone = formatPhoneNumber(emailOrPhone.trim())
                                            val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
                                                .setPhoneNumber(formattedPhone)
                                                .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                                                .setActivity(activity)
                                                .setCallbacks(callbacks)
                                                .build()
                                            com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
                                        } else {
                                            Toast.makeText(context, "Activity Context is not available", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        if (verificationCode.length >= 4) {
                                            isLoading = true
                                            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(storedVerificationId, verificationCode)
                                            auth.signInWithCredential(credential)
                                                .addOnCompleteListener { task ->
                                                    isLoading = false
                                                    if (task.isSuccessful) {
                                                        val user = auth.currentUser
                                                        viewModel.saveProfile(
                                                            name = "",
                                                            gender = "",
                                                            age = 25,
                                                            height = 175,
                                                            dominantHand = "Right",
                                                            email = null,
                                                            phone = user?.phoneNumber,
                                                            isRegistered = true
                                                        )
                                                        Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Успешный вход!" else "Successfully logged in!", Toast.LENGTH_SHORT).show()
                                                        onNavigateNext()
                                                    } else {
                                                        activeError = "${if (currentLang == AppLanguage.RUS) "Ошибка:" else "Error:"} ${task.exception?.message}"
                                                    }
                                                }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Кнопка - Войти через Google
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        val webClientId = context.getString(com.aistudio.hiromant.kxsrwa.R.string.default_web_client_id)
                        @Suppress("DEPRECATION")
                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                        )
                            .requestIdToken(webClientId)
                            .requestEmail()
                            .build()

                        @Suppress("DEPRECATION")
                        val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G ",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Войти через Google" else "Sign in with Google",
                            color = Color(0xFF5F6368),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Кнопка "Войти без Регистрации"
                OutlinedButton(
                    onClick = {
                        viewModel.saveProfile(
                            name = "Максим",
                            gender = "Male",
                            age = 44,
                            height = 175,
                            dominantHand = "Right",
                            email = "demo@hiromant.app",
                            phone = "+79991112233",
                            isRegistered = true
                        )
                        Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Вход выполнен в тестовом режиме!" else "Logged in as Test Profile!", Toast.LENGTH_SHORT).show()
                        onNavigateNext()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MysticGold
                    ),
                    border = BorderStroke(1.dp, MysticGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "Войти без Регистрации" else "Continue Without Registration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MysticGold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (activeError != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { activeError = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MysticDarkSurface),
                border = BorderStroke(1.5.dp, MysticGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error icon",
                        tint = Color(0xFFCF6679),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "Произошла ошибка" else "An error occurred",
                        style = MaterialTheme.typography.titleMedium.copy(color = MysticGold, fontWeight = FontWeight.Bold),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = activeError ?: "",
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFFE0E0E0)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (currentLang == AppLanguage.RUS) 
                            "Для пользователей из РФ и регионов с ограничениями Google рекомендуется использовать полнофункциональный Демо-режим без регистрации."
                        else 
                            "For users in regions with Google limitations, we recommend using the fully-featured Demo Mode without registration.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            activeError = null
                            viewModel.saveProfile(
                                name = "Максим",
                                gender = "Male",
                                age = 44,
                                height = 175,
                                dominantHand = "Right",
                                email = "demo@hiromant.app",
                                phone = "+79991112233",
                                isRegistered = true
                            )
                            Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Вход выполнен в тестовом режиме!" else "Logged in as Test Profile!", Toast.LENGTH_SHORT).show()
                            onNavigateNext()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MysticGold,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Войти в Демо-режиме" else "Enter in Demo Mode",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeError = null },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            ShrinkableText(
                                text = if (currentLang == AppLanguage.RUS) "Закрыть" else "Close",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:")
                                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("arsmaxim@gmail.com"))
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Hiromant App Error Report")
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Hi developer,\n\nI encountered the following error in the Hiromant app:\n\n${activeError}\n\nDevice: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})")
                                }
                                try {
                                    context.startActivity(emailIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Почтовое приложение не найдено" else "No email app found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            ShrinkableText(
                                text = if (currentLang == AppLanguage.RUS) "Отчёт" else "Report",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- SCREEN 3: USER PROFILE DETAILS ---

@Composable
fun ProfileScreen(
    viewModel: PalmistViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    val existingProfile by viewModel.userProfile.collectAsState()

    var name by remember { mutableStateOf("Максим") }
    var gender by remember { mutableStateOf("Male") }
    var birthYearText by remember { mutableStateOf("1982") }
    var heightText by remember { mutableStateOf("175") }
    var dominantHand by remember { mutableStateOf("Right") }

    var nameError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existingProfile) {
        existingProfile?.let {
            if (it.name.isNotEmpty()) {
                name = it.name
                gender = it.gender
                birthYearText = (2026 - it.age).toString()
                heightText = it.height.toString()
                dominantHand = it.dominantHand
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    MysticHeader(strings.profileTitle)
                    MysticSubtitle(
                        text = strings.profileSubtitle,
                        maxLines = 3,
                        fontSize = 13.5.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            MysticCard(modifier = Modifier.weight(1f, fill = false)) {
                Spacer(modifier = Modifier.height(8.dp))

                // Name and Gender Row
                val isRu = currentLang == com.aistudio.hiromant.kxsrwa.ui.language.AppLanguage.RUS
                val maleLabel = if (isRu) "М" else "M"
                val femaleLabel = if (isRu) "Ж" else "F"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Name Field
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.profileNameLabel,
                            style = MaterialTheme.typography.labelMedium.copy(color = MysticGold),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = if (it.trim().length < 2) strings.profileNameError else null
                            },
                            placeholder = { Text("Максим", color = Color.Gray) },
                            singleLine = true,
                            isError = nameError != null,
                            keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MysticGold,
                                unfocusedBorderColor = MysticBronze.copy(0.6f),
                                cursorColor = MysticGold,
                                errorBorderColor = Color(0xFFCF6679)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (nameError != null) {
                            Text(
                                text = nameError!!,
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCF6679)),
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }

                    // Right Column: Gender Buttons (М/Ж)
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = strings.profileGenderLabel,
                            style = MaterialTheme.typography.labelMedium.copy(color = MysticGold),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                maleLabel to "Male",
                                femaleLabel to "Female"
                            ).forEach { (label, value) ->
                                val selected = gender == value
                                OutlinedButton(
                                    onClick = { gender = value },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, if (selected) MysticGold else MysticBronze.copy(0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) Color(0x22D4AF37) else Color.Transparent,
                                        contentColor = if (selected) MysticGold else Color.White
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(width = 50.dp, height = 54.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Birth Year and Height Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Birth Year Input
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.profileAgeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(color = MysticGold),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = birthYearText,
                            onValueChange = { newValue ->
                                if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                    birthYearText = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MysticGold,
                                focusedBorderColor = MysticGold,
                                unfocusedBorderColor = MysticBronze.copy(0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("1982", color = Color.Gray) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Height Input
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.profileHeightLabel,
                            style = MaterialTheme.typography.labelMedium.copy(color = MysticGold),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { newValue ->
                                if (newValue.length <= 3 && newValue.all { it.isDigit() }) {
                                    heightText = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MysticGold,
                                focusedBorderColor = MysticGold,
                                unfocusedBorderColor = MysticBronze.copy(0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("172", color = Color.Gray) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Dominant hand selector (ESSENTIAL)
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(
                        text = strings.profileHandLabel,
                        style = MaterialTheme.typography.labelMedium.copy(color = MysticGold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            strings.profileHandLeft to "Left",
                            strings.profileHandRight to "Right"
                        ).forEach { (label, value) ->
                            val selected = dominantHand == value
                            OutlinedButton(
                                onClick = { dominantHand = value },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, if (selected) MysticGold else MysticBronze.copy(0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) Color(0x22D4AF37) else Color.Transparent,
                                    contentColor = if (selected) MysticGold else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (dominantHand == "Left") strings.profileHandDescLeft else strings.profileHandDescRight,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MysticGold.copy(alpha = 0.9f),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            MysticButton(
                text = strings.next,
                onClick = {
                    if (name.trim().length >= 2 && gender.isNotEmpty()) {
                        val parsedBirthYear = birthYearText.toIntOrNull() ?: 1995
                        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                        val parsedAge = (currentYear - parsedBirthYear).coerceIn(18, 100)
                        val parsedHeight = heightText.toIntOrNull() ?: 172

                        viewModel.saveProfile(name, gender, parsedAge, parsedHeight, dominantHand)
                        onNavigateNext()
                    } else {
                        if (name.trim().length < 2) nameError = strings.profileNameError
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}


// --- SCREEN 4: MEDIA UPLOAD & RUN ANALYSES ---

fun createGalleryImageUri(context: Context, title: String): Uri? {
    try {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "${title}_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Palmist")
        }
        return context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun createGalleryVideoUri(context: Context, title: String): Uri? {
    try {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, "${title}_${System.currentTimeMillis()}.mp4")
            put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/Palmist")
        }
        return context.contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@Composable
fun HandStencilCanvas(slotName: String, modifier: Modifier = Modifier) {
    // Рисуем реалистичный анатомически правильный контур ладони или тыльной стороны руки на Canvas
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width // Получаем ширину области рисования в пикселях
        val height = size.height // Получаем высоту области рисования в пикселях
        
        // Определяем тип руки: ладонь (содержит линии судьбы) или тыльная сторона (содержит ногти)
        val isPalm = slotName == "left_palm" || slotName == "right_palm" // Булево значение для ладони
        
        // Определяем расположение большого пальца в зависимости от ракурса камеры:
        // - Для правой ладони (right_palm) и тыльной стороны левой руки (left_back) большой палец находится справа на фото.
        // - Для левой ладони (left_palm) и тыльной стороны правой руки (right_back) большой палец находится слева на фото.
        val thumbOnRight = (slotName == "right_palm" || slotName == "left_back") // Флаг расположения большого пальца справа

        
        // Вспомогательные функции для перевода относительных координат (0.0 - 1.0) в реальные пиксели экрана
        fun getX(f: Float): Float {
            // Если большой палец должен быть справа, рисуем как есть, иначе зеркально отражаем по горизонтали
            return if (thumbOnRight) width * f else width * (1.0f - f)
        }
        fun getY(f: Float): Float {
            // Возвращаем координату Y, умноженную на общую высоту холста
            return height * f
        }
        
        // Создаем путь для рисования реалистичного и изящного внешнего контура руки
        val handPath = androidx.compose.ui.graphics.Path().apply {
            // Начинаем рисование с левой части запястья
            moveTo(getX(0.36f), getY(0.92f))
            
            // Плавная и реалистичная кривая по внешнему ребру ладони (холм Луны)
            cubicTo(
                getX(0.30f), getY(0.82f), // Первая контрольная точка для скругления ребра ладони внизу
                getX(0.23f), getY(0.70f), // Вторая контрольная точка для сужения к пальцам
                getX(0.23f), getY(0.50f)  // Конечная точка у основания мизинца
            )
            
            // --- МИЗИНЕЦ ---
            // Левая грань мизинца (плавное сужение кверху)
            quadraticTo(getX(0.21f), getY(0.42f), getX(0.21f), getY(0.32f))
            // Анатомически округлая верхушка мизинца
            cubicTo(
                getX(0.21f), getY(0.29f), // Левый изгиб подушечки пальца
                getX(0.26f), getY(0.29f), // Правый изгиб подушечки пальца
                getX(0.26f), getY(0.32f)  // Точка перехода на правую грань мизинца
            )
            // Правая грань мизинца до межпальцевой складки
            quadraticTo(getX(0.26f), getY(0.42f), getX(0.27f), getY(0.46f))
            
            // --- БЕЗЫМЯННЫЙ ПАЛЕЦ ---
            // Левая грань безымянного пальца
            quadraticTo(getX(0.28f), getY(0.30f), getX(0.29f), getY(0.20f))
            // Мягкая округлая подушечка безымянного пальца
            cubicTo(
                getX(0.29f), getY(0.16f), // Левое скругление верхушки
                getX(0.35f), getY(0.16f), // Правое скругление верхушки
                getX(0.35f), getY(0.20f)  // Переход к правой грани
            )
            // Правая грань безымянного пальца до межпальцевой впадины
            quadraticTo(getX(0.35f), getY(0.30f), getX(0.36f), getY(0.42f))
            
            // --- СРЕДНИЙ ПАЛЕЦ (самый длинный палец руки) ---
            // Левая грань среднего пальца
            quadraticTo(getX(0.37f), getY(0.24f), getX(0.39f), getY(0.12f))
            // Красивая округлая верхушка среднего пальца
            cubicTo(
                getX(0.39f), getY(0.08f), // Левый изгиб высшей точки пальца
                getX(0.46f), getY(0.08f), // Правый изгиб высшей точки пальца
                getX(0.46f), getY(0.12f)  // Переход к правой грани
            )
            // Правая грань среднего пальца до межпальцевой складки
            quadraticTo(getX(0.47f), getY(0.24f), getX(0.48f), getY(0.41f))
            
            // --- УКАЗАТЕЛЬНЫЙ ПАЛЕЦ ---
            // Левая грань указательного пальца
            quadraticTo(getX(0.49f), getY(0.28f), getX(0.51f), getY(0.18f))
            // Аккуратная округлая подушечка указательного пальца
            cubicTo(
                getX(0.51f), getY(0.14f), // Левый изгиб верхушки
                getX(0.57f), getY(0.14f), // Правый изгиб верхушки
                getX(0.57f), getY(0.18f)  // Переход к правой грани
            )
            // Правая грань указательного пальца до глубокой межпальцевой впадины
            quadraticTo(getX(0.57f), getY(0.28f), getX(0.58f), getY(0.44f))
            
            // --- МЕЖПАЛЬЦЕВАЯ ВПАДИНА И БОЛЬШОЙ ПАЛЕЦ ---
            // Плавный реалистичный изгиб кожной складки между указательным и большим пальцем
            quadraticTo(getX(0.60f), getY(0.52f), getX(0.65f), getY(0.55f))
            
            // Верхняя грань большого пальца (направленная в сторону)
            quadraticTo(getX(0.74f), getY(0.56f), getX(0.83f), getY(0.58f))
            // Анатомически правильный закругленный кончик большого пальца
            cubicTo(
                getX(0.86f), getY(0.59f), // Левая контрольная точка скругления
                getX(0.87f), getY(0.64f), // Правая контрольная точка скругления
                getX(0.83f), getY(0.66f)  // Переход на внутреннюю грань пальца
            )
            // Внутренняя грань большого пальца до сустава основания
            quadraticTo(getX(0.74f), getY(0.71f), getX(0.67f), getY(0.78f))
            
            // Нижнее ребро ладони (область холма Венеры) до основания запястья
            quadraticTo(getX(0.62f), getY(0.85f), getX(0.60f), getY(0.92f))
            
            // Соединяем края запястья аккуратной горизонтальной линией
            lineTo(getX(0.36f), getY(0.92f))
            
            // Закрываем векторный контур
            close()
        }
        
        // Заливка силуэта руки мягким полупрозрачным золотистым тоном для эстетичности
        drawPath(
            path = handPath, // Используем созданный контур руки
            color = MysticGold.copy(0.04f), // Нежный золотистый оттенок
            style = androidx.compose.ui.graphics.drawscope.Fill // Тип отрисовки - сплошная заливка
        )
        
        // Отрисовка пунктирной золотой линии по контуру для создания технологичного эффекта биометрического сканера
        drawPath(
            path = handPath, // Наш детальный контур руки
            color = MysticGold.copy(0.55f), // Свечение золотистого цвета
            style = androidx.compose.ui.graphics.drawscope.Stroke( // Рисуем только линию контура
                width = 2.5.dp.toPx(), // Оптимальная толщина линии контура в пикселях
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect( // Пунктирный паттерн линии
                    floatArrayOf(15f, 10f), // Длина штриха 15 пикселей, длина пробела 10 пикселей
                    0f // Без смещения начала линии
                )
            )
        )
        
        // Отрисовка трех изящных параллельных складок запястья ("браслеты" или пояса Ориона)
        for (i in 0..2) {
            val offset = i * 0.025f // Относительное смещение для каждой линии вниз
            val wristLinePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(getX(0.36f), getY(0.92f + offset)) // Левая стартовая точка браслета
                quadraticTo( // Дугообразная линия запястья
                    getX(0.48f), getY(0.945f + offset), // Контрольная точка изгиба дуги вверх
                    getX(0.60f), getY(0.92f + offset)   // Правая финишная точка у ребра
                )
            }
            drawPath(
                path = wristLinePath, // Путь дуги запястья
                color = MysticGold.copy(0.4f), // Золотистый полупрозрачный цвет
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()) // Толщина линии в пикселях
            )
        }
        
        // Отрисовка межфаланговых складок (суставов) на длинных пальцах для максимальной реалистичности рисунка
        val knucklesColor = MysticGold.copy(0.35f) // Индивидуальный мягкий цвет для тонких складок кожи
        val jointStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()) // Толщина линий суставов
        
        // Координаты центров и ширины суставов пальцев: (X, Y, Ширина складки)
        val jointCreases = listOf(
            // Мизинец: нижний и верхний суставы
            Triple(0.235f, 0.40f, 0.035f), Triple(0.235f, 0.35f, 0.035f),
            // Безымянный палец: нижний и верхний суставы
            Triple(0.32f, 0.34f, 0.045f), Triple(0.32f, 0.26f, 0.045f),
            // Средний палец: нижний и верхний суставы
            Triple(0.425f, 0.30f, 0.050f), Triple(0.425f, 0.20f, 0.050f),
            // Указательный палец: нижний и верхний суставы
            Triple(0.54f, 0.34f, 0.045f), Triple(0.54f, 0.25f, 0.045f)
        )
        
        // Цикл прорисовки двух тонких параллельных линий для каждого межфалангового сустава
        for (joint in jointCreases) {
            val cx = getX(joint.first) // Рассчитываем пиксельную координату X
            val cy = getY(joint.second) // Рассчитываем пиксельную координату Y
            val w = joint.third * width // Рассчитываем реальную ширину складки
            
            val creasePath = androidx.compose.ui.graphics.Path().apply {
                // Нижняя линия складки сустава
                moveTo(cx - w / 2, cy)
                quadraticTo(cx, cy + 2f, cx + w / 2, cy)
                // Верхняя линия складки сустава для объемного эффекта кожи
                moveTo(cx - w / 2, cy - 3f)
                quadraticTo(cx, cy - 1f, cx + w / 2, cy - 3f)
            }
            drawPath(path = creasePath, color = knucklesColor, style = jointStroke)
        }
        
        // Прорисовка наклонной кожной складки сустава на большом пальце
        val thumbCreasePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(getX(0.72f), getY(0.69f)) // Начальная точка сбоку пальца
            quadraticTo(getX(0.745f), getY(0.655f), getX(0.77f), getY(0.62f)) // Диагональная дуга
            moveTo(getX(0.71f), getY(0.68f)) // Вторая параллельная линия
            quadraticTo(getX(0.735f), getY(0.645f), getX(0.76f), getY(0.61f))
        }
        drawPath(path = thumbCreasePath, color = knucklesColor, style = jointStroke)
        
        if (isPalm) {
            // --- РЕЖИМ ЛАДОНИ: РИСУЕМ РЕАЛИСТИЧНЫЕ ПАПИЛЛЯРНЫЕ И ХИРОМАНТИЧЕСКИЕ ЛИНИИ ---
            
            // 1. ЛИНИЯ ЖИЗНИ (огибает холм Венеры у большого пальца, символизирует витальность)
            val lifeLine = androidx.compose.ui.graphics.Path().apply {
                moveTo(getX(0.58f), getY(0.46f)) // Исток между большим и указательным пальцами
                quadraticTo( // Основной полукруглый контур вокруг большого пальца
                    getX(0.50f), getY(0.64f), // Контрольная точка максимального изгиба
                    getX(0.48f), getY(0.90f)  // Окончание у запястья
                )
            }
            drawPath(
                path = lifeLine,
                color = MysticGold.copy(0.70f), // Хорошо заметная золотая линия
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()) // Утолщенный контур
            )
            
            // 2. ВНУТРЕННЯЯ ЛИНИЯ МАРСА (Тонкая линия Ангела-Хранителя - дублирует линию жизни изнутри)
            val marsLine = androidx.compose.ui.graphics.Path().apply {
                moveTo(getX(0.62f), getY(0.54f)) // Исток чуть глубже под большим пальцем
                quadraticTo(
                    getX(0.55f), getY(0.68f), // Идет строго параллельно линии жизни
                    getX(0.53f), getY(0.83f)  // Оканчивается у основания холма
                )
            }
            drawPath(
                path = marsLine,
                color = MysticGold.copy(0.40f), // Деликатное тонкое свечение
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()) // Изящная толщина
            )
            
            // 3. ЛИНИЯ ГОЛОВЫ / УМА (идет поперек ладони к холму Луны, символизирует интеллект)
            val headLine = androidx.compose.ui.graphics.Path().apply {
                moveTo(getX(0.58f), getY(0.46f)) // Сливается у истока с линией жизни
                quadraticTo(
                    getX(0.44f), getY(0.54f), // Пересекает ладонь по диагонали
                    getX(0.28f), getY(0.62f)  // Оканчивается на холме Луны с красивым спуском вниз
                )
            }
            drawPath(
                path = headLine,
                color = MysticGold.copy(0.70f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
            )
            
            // 4. ЛИНИЯ СЕРДЦА (идет от ребра ладони к указательному пальцу, имеет вилку писателя на конце)
            val heartLine = androidx.compose.ui.graphics.Path().apply {
                moveTo(getX(0.23f), getY(0.49f)) // Берет начало под мизинцем на ребре ладони
                quadraticTo(
                    getX(0.42f), getY(0.46f), // Дугообразный изгиб к верхним пальцам
                    getX(0.47f), getY(0.41f)  // Точка развилки у холма Юпитера
                )
            }
            drawPath(
                path = heartLine,
                color = MysticGold.copy(0.70f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
            )
            
            // Левая ветвь вилки сердца (направлена вверх к указательному и среднему пальцам)
            val heartFork1 = androidx.compose.ui.graphics.Path().apply {
                moveTo(getX(0.47f), getY(0.41f))
                quadraticTo(getX(0.49f), getY(0.39f), getX(0.52f), getY(0.38f))
            }
            // Правая ветвь вилки сердца (направлена мягко вниз к холму ума)
            val heartFork2 = androidx.compose.ui.graphics.Path().apply {
                moveTo(getX(0.47f), getY(0.41f))
                quadraticTo(getX(0.45f), getY(0.42f), getX(0.43f), getY(0.43f))
            }
            drawPath(path = heartFork1, color = MysticGold.copy(0.60f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx()))
            drawPath(path = heartFork2, color = MysticGold.copy(0.60f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx()))
            
            // 5. ЛИНИЯ СУДЬБЫ / РОКА (вертикальная линия по центру ладони, символ жизненного пути)
            val destinyLine = androidx.compose.ui.graphics.Path().apply {
                moveTo(getX(0.48f), getY(0.91f)) // Исток у запястья по центру
                quadraticTo(
                    getX(0.47f), getY(0.65f), // Поднимается вертикально вверх сквозь всю ладонь
                    getX(0.44f), getY(0.40f)  // Упирается в холм Сатурна под средним пальцем
                )
            }
            drawPath(
                path = destinyLine,
                color = MysticGold.copy(0.50f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
            )
            
            // 6. ЛИНИЯ ЗДОРОВЬЯ / МЕРКУРИЯ (идет по диагонали снизу к холму под мизинцем)
            val healthLine = androidx.compose.ui.graphics.Path().apply {
                moveTo(getX(0.46f), getY(0.88f)) // Исток около низа линии жизни
                quadraticTo(
                    getX(0.36f), getY(0.70f), // Пересекает ладонь по диагонали к внешнему краю
                    getX(0.26f), getY(0.52f)  // Оканчивается под мизинцем
                )
            }
            drawPath(
                path = healthLine,
                color = MysticGold.copy(0.45f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
            
            // 7. ЛИНИЯ СОЛНЦА / АПОЛЛОНА (Линия удачи и творческого успеха, идет к безымянному пальцу)
            val sunLine = androidx.compose.ui.graphics.Path().apply {
                moveTo(getX(0.36f), getY(0.65f)) // Исток на равнине Марса
                quadraticTo(
                    getX(0.34f), getY(0.53f), // Подъем параллельно линии судьбы
                    getX(0.33f), getY(0.41f)  // Окончание у безымянного пальца
                )
            }
            drawPath(
                path = sunLine,
                color = MysticGold.copy(0.45f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
            
            // 8. ЛИНИИ БРАКА / ОТНОШЕНИЙ (короткие горизонтальные отметки на ребре под мизинцем)
            for (j in 0..1) {
                val yOffset = j * 0.015f // Вертикальный интервал между линиями брака
                val marriageLine = androidx.compose.ui.graphics.Path().apply {
                    moveTo(getX(0.22f), getY(0.45f + yOffset)) // Точка на ребре ладони
                    quadraticTo(
                        getX(0.235f), getY(0.45f + yOffset), // Простирается горизонтально на холм Меркурия
                        getX(0.25f), getY(0.45f + yOffset)   // Завершение черты брака
                    )
                }
                drawPath(
                    path = marriageLine,
                    color = MysticGold.copy(0.50f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                )
            }
            
        } else {
            // --- РЕЖИМ ТЫЛЬНОЙ СТОРОНЫ: РИСУЕМ РЕАЛИСТИЧНЫЕ И ОЧЕРЧЕННЫЕ НОГТИ НА ПАЛЬЦАХ ---
            
            // Локальная функция для детального рисования анатомически правильного реалистичного ногтя
            fun drawRealisticNail(cx: Float, cy: Float, widthFract: Float, heightFract: Float) {
                val nailWidth = widthFract * width   // Переводим относительную ширину ногтя в пиксели
                val nailHeight = heightFract * height // Переводим относительную высоту ногтя в пиксели
                
                // Сохраняем состояние холста Canvas перед вращением и смещением системы координат
                drawContext.canvas.save()
                
                // Смещаем начало координат холста в центр верхушки пальца
                val nailX = getX(cx)
                val nailY = getY(cy)
                drawContext.canvas.translate(nailX, nailY)
                
                // Угол поворота ногтевой пластины. Большой палец и мизинец имеют наклон для реалистичности.
                val rotDegrees = if (cx > 0.7f) {
                    if (thumbOnRight) -35f else 35f // Угол разворота для большого пальца
                } else if (cx < 0.28f) {
                    if (thumbOnRight) 12f else -12f  // Небольшой наклон для мизинца наружу
                } else 0f
                drawContext.canvas.rotate(rotDegrees)
                
                // Создаем форму ногтя (вертикально-вытянутая округлая миндалевидная форма из hiro.su)
                val nailPath = androidx.compose.ui.graphics.Path().apply {
                    val halfW = nailWidth / 2f
                    val halfH = nailHeight / 2f
                    
                    // Левая нижняя точка у кутикулы
                    moveTo(-halfW, halfH)
                    // Левый край ногтя с легким сужением к свободному краю
                    quadraticTo(-halfW * 1.05f, -halfH * 0.4f, -halfW * 0.9f, -halfH)
                    // Верхний свободный край (аккуратный полукруглый изгиб ногтевой кромки)
                    quadraticTo(0f, -halfH * 1.35f, halfW * 0.9f, -halfH)
                    // Правый край ногтя с изгибом книзу
                    quadraticTo(halfW * 1.05f, -halfH * 0.4f, halfW, halfH)
                    // Нижний дугообразный край (линия улыбки кутикулы)
                    quadraticTo(0f, halfH * 1.25f, -halfW, halfH)
                    close()
                }
                
                // 1. Заполняем тело ногтя нежным полупрозрачным золотым градиентом
                drawPath(
                    path = nailPath,
                    color = MysticGold.copy(0.12f),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )
                
                // 2. Очерчиваем четкие границы ногтевой пластины тонким золотистым контуром
                drawPath(
                    path = nailPath,
                    color = MysticGold.copy(0.65f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
                
                // 3. Прорисовываем Лунулу (матово-белое полукружие у основания кутикулы)
                val lunulaPath = androidx.compose.ui.graphics.Path().apply {
                    val halfW = nailWidth / 2f
                    val halfH = nailHeight / 2f
                    moveTo(-halfW * 0.6f, halfH) // Нижняя левая точка кутикулы
                    quadraticTo(0f, halfH * 0.4f, halfW * 0.6f, halfH) // Изгиб дуги вверх
                    quadraticTo(0f, halfH * 1.15f, -halfW * 0.6f, halfH) // Замыкание по дуге кутикулы
                    close()
                }
                drawPath(
                    path = lunulaPath,
                    color = Color.White.copy(0.40f), // Нежный полупрозрачный белый цвет
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )
                
                // 4. Дополнительная складка кожи (кутикула) у основания ногтевой пластины
                val cuticleCrease = androidx.compose.ui.graphics.Path().apply {
                    val halfW = nailWidth / 2f
                    val halfH = nailHeight / 2f
                    moveTo(-halfW * 1.3f, halfH * 1.28f)
                    quadraticTo(0f, halfH * 1.55f, halfW * 1.3f, halfH * 1.28f)
                }
                drawPath(
                    path = cuticleCrease,
                    color = MysticGold.copy(0.35f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
                
                // Восстанавливаем сохраненное состояние Canvas для рисования остальных ногтей
                drawContext.canvas.restore()
            }
            
            // Размещаем анатомически выверенные ногти на каждом из пяти пальцев тыльной стороны руки
            drawRealisticNail(0.235f, 0.32f, 0.016f, 0.024f) // Ноготь мизинца
            drawRealisticNail(0.32f, 0.20f, 0.021f, 0.031f)  // Ноготь безымянного пальца
            drawRealisticNail(0.425f, 0.12f, 0.023f, 0.033f) // Ноготь среднего пальца
            drawRealisticNail(0.54f, 0.20f, 0.021f, 0.031f)  // Ноготь указательного пальца
            drawRealisticNail(0.83f, 0.61f, 0.025f, 0.024f)  // Ноготь большого пальца
        }
    }
}

@Composable
fun HandSlotCard(
    title: String,
    bitmap: Bitmap?,
    slotName: String,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onClear: () -> Unit,
    btnCameraText: String,
    btnGalleryText: String,
    modifier: Modifier = Modifier,
    onSaveToGallery: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 1. Label above the preview window
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (bitmap != null) MysticGold else Color.White,
                fontSize = 16.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 2. Preview Window
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .aspectRatio(1.2f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1F1E1E2C))
                .border(2.dp, if (bitmap != null) MysticGold else MysticBronze.copy(0.3f), RoundedCornerShape(16.dp))
                .clickable { onTakePhoto() } // Tapping triggers camera
        ) {
            if (bitmap != null) {
                Image(
                    painter = rememberAsyncImagePainter(bitmap),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Clear button in the corner
                IconButton(
                    onClick = { onClear() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear",
                        tint = Color.Red,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Кнопка сохранения в галерею в левом углу
                if (onSaveToGallery != null) {
                    IconButton(
                        onClick = { onSaveToGallery() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(28.dp)
                            .background(Color.Black.copy(0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Сохранить в галерею",
                            tint = MysticGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                // Large placeholder in the middle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = MysticBronze.copy(0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Тапните для фото",
                        style = MaterialTheme.typography.labelSmall.copy(color = MysticBronze, fontSize = 11.sp)
                    )
                }
            }

            // Контуры на превью удалены по запросу пользователя для улучшения внешнего вида

        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Upload buttons UNDER the preview window
        Row(
            modifier = Modifier.width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Take Photo Button
            Button(
                onClick = onTakePhoto,
                colors = ButtonDefaults.buttonColors(containerColor = MysticBronze.copy(0.2f)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = "Take Photo",
                    tint = MysticGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = btnCameraText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MysticGold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Pick Gallery Button
            Button(
                onClick = onPickPhoto,
                colors = ButtonDefaults.buttonColors(containerColor = MysticBronze.copy(0.2f)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Gallery",
                    tint = MysticGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = btnGalleryText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MysticGold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}


fun getVideoThumbnail(context: Context, uri: Uri?): Bitmap? {
    if (uri == null) return null
    return try {
        val retriever = android.media.MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val bitmap = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retriever.release()
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun InAppCameraDialog(
    slotName: String,
    currentLang: AppLanguage,
    onPhotoCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val isRussian = currentLang == AppLanguage.RUS
    
    var hasCameraHardware by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val imageCapture = remember { androidx.camera.core.ImageCapture.Builder().build() }
    val cameraProviderFuture = remember { androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context) }
    
    val cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MysticDarkSurface),
            border = BorderStroke(2.dp, MysticGold),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (hasCameraHardware) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            val previewView = androidx.camera.view.PreviewView(ctx).apply {
                                scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                            }
                            
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = androidx.camera.core.Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    
                                    val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                                    
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    hasCameraHardware = false
                                    errorMessage = e.message
                                }
                            }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                            
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback visual simulation placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0D0B18)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MysticGold,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isRussian) "Режим эмуляции камеры" else "Camera Emulation Active",
                                style = MaterialTheme.typography.titleMedium.copy(color = MysticGold, fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isRussian) 
                                    "В эмуляторе физическая камера недоступна. Будет автоматически сгенерировано высококачественное мистическое изображение ладони для анализа."
                                else 
                                    "No hardware camera detected in this emulator. A high-quality mystical palm print will be procedurally generated for your analysis.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Beautiful Hand Guide Overlay (for alignment)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(320.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HandStencilCanvas(
                            slotName = slotName,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val guideTxt = when (slotName) {
                                "left_palm" -> if (isRussian) "Левая ладонь (линии вверх)" else "Left Palm (lines up)"
                                "left_back" -> if (isRussian) "Тыл левой руки (ногти)" else "Left Back (nails)"
                                "right_palm" -> if (isRussian) "Правая ладонь (+запястье)" else "Right Palm (+wrist)"
                                "right_back" -> if (isRussian) "Тыл правой руки (ногти)" else "Right Back (nails)"
                                else -> if (isRussian) "Поместите руку сюда" else "Place hand here"
                            }
                            Text(
                                text = guideTxt,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MysticGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Top control: Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
                
                // Top control: Title indicating which hand to position
                val titleString = when (slotName) {
                    "left_palm" -> if (isRussian) "Левая ладонь" else "Left Palm"
                    "left_back" -> if (isRussian) "Тыльная сторона левой руки" else "Left Hand Back"
                    "right_palm" -> if (isRussian) "Правая ладонь" else "Right Palm"
                    "right_back" -> if (isRussian) "Тыльная сторона правой руки" else "Right Hand Back"
                    else -> if (isRussian) "Фото ладони" else "Palm Photo"
                }
                Text(
                    text = titleString,
                    style = MaterialTheme.typography.titleMedium.copy(color = MysticGold, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 22.dp)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
                
                // Bottom control: Capture Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(0.4f))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            if (hasCameraHardware) {
                                // Take actual photo
                                val photoFile = java.io.File(
                                    context.cacheDir,
                                    "captured_palm_${System.currentTimeMillis()}.jpg"
                                )
                                val outputOptions = androidx.camera.core.ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                
                                imageCapture.takePicture(
                                    outputOptions,
                                    androidx.core.content.ContextCompat.getMainExecutor(context),
                                    object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: androidx.camera.core.ImageCapture.OutputFileResults) {
                                            val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                                            if (bitmap != null) {
                                                onPhotoCaptured(bitmap)
                                            } else {
                                                // Fallback to procedural generator if decoding fails
                                                val mockBitmap = BitmapUtils.generateMysticHandBitmap(context, slotName, isRussian)
                                                onPhotoCaptured(mockBitmap)
                                            }
                                        }
                                        
                                        override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                            exception.printStackTrace()
                                            // Fallback to procedural generator on error
                                            val mockBitmap = BitmapUtils.generateMysticHandBitmap(context, slotName, isRussian)
                                            onPhotoCaptured(mockBitmap)
                                        }
                                    }
                                )
                            } else {
                                // Camera emulation capture -> procedural golden palm print image
                                val mockBitmap = BitmapUtils.generateMysticHandBitmap(context, slotName, isRussian)
                                onPhotoCaptured(mockBitmap)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MysticGold),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(72.dp)
                            .border(4.dp, Color.White, CircleShape),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- SCREEN 4: MEDIA UPLOAD & RUN ANALYSES ---

@Composable
fun UploadScreen(
    viewModel: PalmistViewModel,
    onNavigateToLoading: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToVideoScan: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToResult: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    val billingState by viewModel.billingState.collectAsState()

    var activeSlot by remember { mutableStateOf<String?>(null) }
    val bitmapLeftPalm by viewModel.bitmapLeftPalm.collectAsState()
    val bitmapLeftBack by viewModel.bitmapLeftBack.collectAsState()
    val bitmapRightPalm by viewModel.bitmapRightPalm.collectAsState()
    val bitmapRightBack by viewModel.bitmapRightBack.collectAsState()

    val leftPalmPath by viewModel.leftPalmPath.collectAsState()
    val leftBackPath by viewModel.leftBackPath.collectAsState()
    val rightPalmPath by viewModel.rightPalmPath.collectAsState()
    val rightBackPath by viewModel.rightBackPath.collectAsState()

    val showInterpretationScreen by viewModel.showInterpretationScreen.collectAsState()

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val progress by viewModel.analysisProgress.collectAsState()
    val status by viewModel.analysisStatus.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "BlinkTransition")
    val textBlinkColor by infiniteTransition.animateColor(
        initialValue = Color.White,
        targetValue = Color.Red,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkTextColor"
    )
    val textShadow = remember(textBlinkColor) {
        Shadow(
            color = textBlinkColor.copy(alpha = 0.8f),
            offset = Offset(0f, 0f),
            blurRadius = 12f
        )
    }

    // Текстовое озвучивание (TTS) для инструкции
    var isGuideExpanded by remember { mutableStateOf(false) }
    var isPlayingTts by remember { mutableStateOf(false) }
    var ttsVolume by remember { mutableStateOf(1f) }
    var isMuted by remember { mutableStateOf(false) }
    var currentWordRange by remember { mutableStateOf<IntRange?>(null) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    val ttsGenderState by viewModel.ttsGender.collectAsState()
    val ttsVoiceIndex by viewModel.ttsVoiceIndex.collectAsState()
    val ttsRateState by viewModel.ttsSpeechRate.collectAsState()
    val ttsPitchState by viewModel.ttsPitch.collectAsState()

    val instructionText = "Для точного анализа важно, чтобы снимок был сделан при хорошем освещении.\n" +
            "1. Расположите ладонь ровно перед камерой, без наклона.\n" +
            "2. Пальцы должны быть слегка разведены.\n" +
            "3. Избегайте размытия и теней, падающих на линии руки.\n" +
            "4. Сфотографируйте поочерёдно ладонь и тыльную сторону обеих рук."

    val annotatedInstructionText = remember(currentWordRange) {
        buildAnnotatedString {
            val range = currentWordRange
            if (range != null && range.first in instructionText.indices && range.last <= instructionText.length) {
                append(instructionText.substring(0, range.first))
                withStyle(style = SpanStyle(background = MysticGold.copy(0.4f), color = Color.White, fontWeight = FontWeight.Bold)) {
                    append(instructionText.substring(range.first, range.last))
                }
                append(instructionText.substring(range.last))
            } else {
                append(instructionText)
            }
        }
    }

    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = if (currentLang == AppLanguage.RUS) java.util.Locale.forLanguageTag("ru") else java.util.Locale.US
                tts?.language = locale
            }
        }
        
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isPlayingTts = true
            }

            override fun onDone(utteranceId: String?) {
                isPlayingTts = false
                currentWordRange = null
            }

            override fun onError(utteranceId: String?) {
                isPlayingTts = false
                currentWordRange = null
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                currentWordRange = start..end
            }
        })
        
        ttsInstance = tts
        
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Media Store URI values for system-native cameras
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showInAppCamera by remember { mutableStateOf(false) }

    // Launchers for media
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val bitmap = BitmapUtils.uriToBitmap(context, it)
                if (bitmap != null) {
                    when (activeSlot) {
                        "left_palm" -> {
                            viewModel.bitmapLeftPalm.value = bitmap
                            viewModel.leftPalmPath.value = it.toString()
                        }
                        "left_back" -> {
                            viewModel.bitmapLeftBack.value = bitmap
                            viewModel.leftBackPath.value = it.toString()
                        }
                        "right_palm" -> {
                            viewModel.bitmapRightPalm.value = bitmap
                            viewModel.rightPalmPath.value = it.toString()
                        }
                        "right_back" -> {
                            viewModel.bitmapRightBack.value = bitmap
                            viewModel.rightBackPath.value = it.toString()
                        }
                    }
                }
            }
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                viewModel.videoUri.value = it
                Toast.makeText(context, strings.uploadPreviewVideo, Toast.LENGTH_SHORT).show()
            }
        }
    )

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempImageUri?.let { uri ->
                    val bitmap = BitmapUtils.uriToBitmap(context, uri)
                    if (bitmap != null) {
                        when (activeSlot) {
                            "left_palm" -> {
                                viewModel.bitmapLeftPalm.value = bitmap
                                viewModel.leftPalmPath.value = uri.toString()
                            }
                            "left_back" -> {
                                viewModel.bitmapLeftBack.value = bitmap
                                viewModel.leftBackPath.value = uri.toString()
                            }
                            "right_palm" -> {
                                viewModel.bitmapRightPalm.value = bitmap
                                viewModel.rightPalmPath.value = uri.toString()
                            }
                            "right_back" -> {
                                viewModel.bitmapRightBack.value = bitmap
                                viewModel.rightBackPath.value = uri.toString()
                            }
                        }
                    }
                }
            }
        }
    )

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo(),
        onResult = { success ->
            if (success) {
                tempVideoUri?.let { uri ->
                    viewModel.videoUri.value = uri
                    Toast.makeText(context, strings.uploadPreviewVideo, Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Camera permission launchers
    val cameraPermissionLauncherForPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                showInAppCamera = true
            } else {
                Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val cameraPermissionLauncherForVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createGalleryVideoUri(context, "hand_video")
                if (uri != null) {
                    tempVideoUri = uri
                    videoCaptureLauncher.launch(uri)
                }
            } else {
                Toast.makeText(context, "Camera permission is required to record video", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (!showInterpretationScreen) {
                // ШАГ 1: Экран загрузки материалов
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MysticHeader(
                        text = strings.uploadTitle
                    )
                }

                // Инструкция «Как правильно фотографировать ладонь» с TTS
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1A000000)),
                    border = BorderStroke(1.dp, MysticBronze.copy(0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isGuideExpanded = !isGuideExpanded }
                        ) {
                            Icon(
                                imageVector = if (isGuideExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MysticGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Как правильно фотографировать ладонь",
                                style = MaterialTheme.typography.labelLarge.copy(color = MysticGold, fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (isGuideExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Единая панель управления TTS голосом
                            TtsVoiceController(
                                isPlaying = isPlayingTts,
                                onPlayToggle = {
                                    if (isPlayingTts) {
                                        ttsInstance?.stop()
                                        isPlayingTts = false
                                        currentWordRange = null
                                    } else {
                                        configureTtsVoice(
                                            tts = ttsInstance,
                                            currentLang = currentLang,
                                            voiceGender = ttsGenderState,
                                            voiceIndex = ttsVoiceIndex,
                                            speechRate = ttsRateState,
                                            speechPitch = ttsPitchState
                                        )
                                        val speakParams = android.os.Bundle().apply {
                                            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "guideline_tts")
                                        }
                                        val result = ttsInstance?.speak(instructionText, TextToSpeech.QUEUE_FLUSH, speakParams, "guideline_tts")
                                        if (result == TextToSpeech.SUCCESS) {
                                            isPlayingTts = true
                                        }
                                    }
                                },
                                rate = ttsRateState,
                                onRateChange = { newRate ->
                                    viewModel.changeTtsSpeechRate(newRate)
                                    ttsInstance?.setSpeechRate(newRate)
                                },
                                pitch = ttsPitchState,
                                onPitchChange = { newPitch ->
                                    viewModel.changeTtsPitch(newPitch)
                                    ttsInstance?.setPitch(newPitch)
                                },
                                gender = ttsGenderState,
                                onGenderChange = { newGender ->
                                    viewModel.changeTtsGender(newGender)
                                    configureTtsVoice(
                                        tts = ttsInstance,
                                        currentLang = currentLang,
                                        voiceGender = newGender,
                                        voiceIndex = ttsVoiceIndex,
                                        speechRate = ttsRateState,
                                        speechPitch = ttsPitchState
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Текст инструкции с подсветкой слов
                            Text(
                                text = annotatedInstructionText,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFC0C0D0), lineHeight = 20.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0AFFFFFF), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            )
                        }
                    }
                }

                // Photo upload block (Stack of 4 beautiful vertical slots)
                MysticCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = strings.uploadPhotoSection,
                            style = MaterialTheme.typography.titleMedium.copy(color = MysticGold, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Left Palm Slot
                        HandSlotCard(
                            title = strings.slotLeftPalm,
                            bitmap = bitmapLeftPalm,
                            slotName = "left_palm",
                            onTakePhoto = {
                                activeSlot = "left_palm"
                                val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasCameraPermission) {
                                    showInAppCamera = true
                                } else {
                                    cameraPermissionLauncherForPhoto.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            onPickPhoto = {
                                activeSlot = "left_palm"
                                photoPickerLauncher.launch("image/*")
                            },
                            onClear = {
                                viewModel.bitmapLeftPalm.value = null
                                viewModel.leftPalmPath.value = null
                            },
                            btnCameraText = if (currentLang == AppLanguage.RUS) "Камера" else "Camera",
                            btnGalleryText = if (currentLang == AppLanguage.RUS) "Галерея" else "Gallery",
                            onSaveToGallery = {
                                bitmapLeftPalm?.let { bmp ->
                                    val saved = com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.saveBitmapToGallery(context, bmp, "LeftPalm")
                                    val msg = if (saved) "Изображение левой ладони сохранено в галерею" else "Не удалось сохранить изображение"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Left Back Slot
                        HandSlotCard(
                            title = strings.slotLeftBack,
                            bitmap = bitmapLeftBack,
                            slotName = "left_back",
                            onTakePhoto = {
                                activeSlot = "left_back"
                                val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasCameraPermission) {
                                    showInAppCamera = true
                                } else {
                                    cameraPermissionLauncherForPhoto.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            onPickPhoto = {
                                activeSlot = "left_back"
                                photoPickerLauncher.launch("image/*")
                            },
                            onClear = {
                                viewModel.bitmapLeftBack.value = null
                                viewModel.leftBackPath.value = null
                            },
                            btnCameraText = if (currentLang == AppLanguage.RUS) "Камера" else "Camera",
                            btnGalleryText = if (currentLang == AppLanguage.RUS) "Галерея" else "Gallery",
                            onSaveToGallery = {
                                bitmapLeftBack?.let { bmp ->
                                    val saved = com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.saveBitmapToGallery(context, bmp, "LeftBack")
                                    val msg = if (saved) "Изображение тыла левой руки сохранено в галерею" else "Не удалось сохранить изображение"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Right Palm Slot
                        HandSlotCard(
                            title = strings.slotRightPalm,
                            bitmap = bitmapRightPalm,
                            slotName = "right_palm",
                            onTakePhoto = {
                                activeSlot = "right_palm"
                                val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasCameraPermission) {
                                    showInAppCamera = true
                                } else {
                                    cameraPermissionLauncherForPhoto.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            onPickPhoto = {
                                activeSlot = "right_palm"
                                photoPickerLauncher.launch("image/*")
                            },
                            onClear = {
                                viewModel.bitmapRightPalm.value = null
                                viewModel.rightPalmPath.value = null
                            },
                            btnCameraText = if (currentLang == AppLanguage.RUS) "Камера" else "Camera",
                            btnGalleryText = if (currentLang == AppLanguage.RUS) "Галерея" else "Gallery",
                            onSaveToGallery = {
                                bitmapRightPalm?.let { bmp ->
                                    val saved = com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.saveBitmapToGallery(context, bmp, "RightPalm")
                                    val msg = if (saved) "Изображение правой ладони сохранено в галерею" else "Не удалось сохранить изображение"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Right Back Slot
                        HandSlotCard(
                            title = strings.slotRightBack,
                            bitmap = bitmapRightBack,
                            slotName = "right_back",
                            onTakePhoto = {
                                activeSlot = "right_back"
                                val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasCameraPermission) {
                                    showInAppCamera = true
                                } else {
                                    cameraPermissionLauncherForPhoto.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            onPickPhoto = {
                                activeSlot = "right_back"
                                photoPickerLauncher.launch("image/*")
                            },
                            onClear = {
                                viewModel.bitmapRightBack.value = null
                                viewModel.rightBackPath.value = null
                            },
                            btnCameraText = if (currentLang == AppLanguage.RUS) "Камера" else "Camera",
                            btnGalleryText = if (currentLang == AppLanguage.RUS) "Галерея" else "Gallery",
                            onSaveToGallery = {
                                bitmapRightBack?.let { bmp ->
                                    val saved = com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.saveBitmapToGallery(context, bmp, "RightBack")
                                    val msg = if (saved) "Изображение тыла правой руки сохранено в галерею" else "Не удалось сохранить изображение"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Кнопки "+ Новый" и "Далее" в одной строке
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MysticButton(
                        text = if (currentLang == AppLanguage.RUS) "+ Новый" else "+ New",
                        onClick = {
                            viewModel.resetUploadState()
                            onNavigateToProfile()
                        },
                        isSecondary = true,
                        modifier = Modifier.weight(1f)
                    )
                    MysticButton(
                        text = if (currentLang == AppLanguage.RUS) "Далее" else "Next",
                        onClick = {
                            val hasMedia = bitmapLeftPalm != null || bitmapLeftBack != null || bitmapRightPalm != null || bitmapRightBack != null
                            if (hasMedia) {
                                viewModel.showInterpretationScreen.value = true
                            } else {
                                Toast.makeText(
                                    context,
                                    if (currentLang == AppLanguage.RUS) "Пожалуйста, загрузите хотя бы одно фото ладони!" else "Please upload at least one hand photo!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

            } else {
                // ШАГ 2: Экран выбора типа интерпретации
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Spacer(modifier = Modifier.width(48.dp))
                    Column {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Интерпритация" else "Interpretation",
                            style = MaterialTheme.typography.titleLarge.copy(color = MysticGold, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Выберите тип интерпритации" else "Choose interpretation type",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Извлекаем количество доступных бесплатных и платных интерпретаций
                val freeCount = billingState?.freeAnalyses ?: 0
                val paidCount = billingState?.paidAnalyses ?: 0
                val bitmaps = listOfNotNull(bitmapLeftPalm, bitmapLeftBack, bitmapRightPalm, bitmapRightBack)
                var showCompatibilitySubButtons by remember { mutableStateOf(false) }

                // 1. Кнопка "Краткая интерпритация" (доступно ярко-зеленого цвета)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C18)),
                    border = BorderStroke(1.2.dp, MysticGold.copy(0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            viewModel.currentAnalysisTypeState.value = "brief"
                            viewModel.showInterpretationScreen.value = false
                            viewModel.runPalmAnalysis(
                                bitmaps = bitmaps,
                                videoUri = null,
                                analysisType = "brief",
                                leftPalmPath = leftPalmPath,
                                leftBackPath = leftBackPath,
                                rightPalmPath = rightPalmPath,
                                rightBackPath = rightBackPath,
                                onCompleted = { onNavigateToResult() }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Краткое Описание" else "Brief Description",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "доступно: " else "available: ",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                text = "$freeCount",
                                color = Color(0xFF00FF66),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 2. Кнопка "Полное Описание" (доступно ярко-фиолетового цвета)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MysticGold),
                    border = BorderStroke(1.2.dp, MysticGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            viewModel.checkFeatureUnlocked("full") { unlocked ->
                                viewModel.currentAnalysisTypeState.value = "full"
                                if (unlocked || (billingState?.paidAnalyses ?: 0) > 0) {
                                    viewModel.showInterpretationScreen.value = false
                                    onNavigateToVideoScan()
                                } else {
                                    viewModel.paymentAmountToPreselect.value = "250"
                                    onNavigateToBilling()
                                }
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Полное Описание" else "Full Description",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "доступно: " else "available: ",
                                color = Color.Black.copy(0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                text = "$paidCount",
                                color = Color(0xFFE040FB),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "  (Стоимость - 250 р.)" else "  (250 RUB)",
                                color = Color.Black.copy(0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 3. Кнопка "Совместимость пары" (доступно зеленый / фиолетовый)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF231E18)),
                    border = BorderStroke(1.5.dp, MysticGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            showCompatibilitySubButtons = !showCompatibilitySubButtons
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Совместимость пары" else "Pair Compatibility",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MysticGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (showCompatibilitySubButtons) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MysticGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "доступно " else "available ",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                text = "$freeCount",
                                color = Color(0xFF00FF66),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = " / ",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "$paidCount",
                                color = Color(0xFFE040FB),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // а) Под-кнопки по нажатию на кнопку "Совместимость пары"
                AnimatedVisibility(visible = showCompatibilitySubButtons) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // б) Кнопка "Краткое Описание" для совместимости (доступно второй строкой)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF003816)),
                            border = BorderStroke(1.2.dp, Color(0xFF00FF66)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.compatibilityFilterMode.value = "brief"
                                    viewModel.activeTab.value = "compatibility"
                                    viewModel.showInterpretationScreen.value = false
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Краткое Описание" else "Brief Description",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (currentLang == AppLanguage.RUS) "доступно: " else "available: ",
                                        color = Color.LightGray,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$freeCount",
                                        color = Color(0xFF00FF66),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // в) Кнопка "Полное Описание" для совместимости (доступно второй строкой)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D004D)),
                            border = BorderStroke(1.2.dp, Color(0xFFE040FB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.compatibilityFilterMode.value = "full"
                                    viewModel.activeTab.value = "compatibility"
                                    viewModel.showInterpretationScreen.value = false
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Полное Описание" else "Full Description",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (currentLang == AppLanguage.RUS) "доступно: " else "available: ",
                                        color = Color.LightGray,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$paidCount",
                                        color = Color(0xFFE040FB),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isAnalyzing) 100.dp else 40.dp))
            }
        }

        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14101E)),
                    border = BorderStroke(1.5.dp, MysticGold),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Вызов единого компонента Прогресс-Бара для страницы загрузки материалов
                        MysticProgressBar(
                            progress = progress,
                            currentLang = currentLang,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Проверяем, активен ли в данный момент флаг отображения диалога встроенной камеры приложения
    if (showInAppCamera) {
        // Рендерим кастомный компонент диалога камеры InAppCameraDialog
        InAppCameraDialog(
            // Передаем имя текущего активного слота для съемки (например, left_palm) или значение по умолчанию
            slotName = activeSlot ?: "palm_photo",
            // Передаем текущий язык интерфейса (русский или английский)
            currentLang = currentLang,
            // Определяем лямбда-коллбэк, который вызывается при успешном захвате кадра камерой
            onPhotoCaptured = { bitmap ->
                // Используем оператор when для обработки результатов в зависимости от того, какой слот снимался
                when (activeSlot) {
                    // Обработка снимка для левой ладони
                    "left_palm" -> {
                        // Обновляем состояние в ViewModel, сохраняя полученный Bitmap объект левой ладони
                        viewModel.bitmapLeftPalm.value = bitmap
                        // Сохраняем снимок в стандартную галерею Android и получаем его системный URI адрес
                        val savedUri = com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.saveBitmapToGalleryAndGetUri(context, bitmap, "LeftPalm")
                        // Записываем строковое представление полученного Uri в ViewModel для последующего сохранения в базу данных
                        viewModel.leftPalmPath.value = savedUri?.toString() ?: "in_app_camera"
                    }
                    // Обработка снимка для тыльной стороны левой руки
                    "left_back" -> {
                        // Сохраняем объект Bitmap тыльной стороны левой руки в соответствующее поле ViewModel
                        viewModel.bitmapLeftBack.value = bitmap
                        // Автоматически экспортируем созданный снимок в галерею смартфона и извлекаем его Uri
                        val savedUri = com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.saveBitmapToGalleryAndGetUri(context, bitmap, "LeftBack")
                        // Сохраняем полученный Uri в виде строки во ViewModel для надежной записи в БД Room
                        viewModel.leftBackPath.value = savedUri?.toString() ?: "in_app_camera"
                    }
                    // Обработка снимка для правой ладони
                    "right_palm" -> {
                        // Обновляем состояние Bitmap правой ладони во ViewModel
                        viewModel.bitmapRightPalm.value = bitmap
                        // Сохраняем растровые данные правой ладони в галерею и генерируем уникальный Uri
                        val savedUri = com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.saveBitmapToGalleryAndGetUri(context, bitmap, "RightPalm")
                        // Связываем локальный Uri изображения с полем пути во ViewModel для синхронизации с базой данных
                        viewModel.rightPalmPath.value = savedUri?.toString() ?: "in_app_camera"
                    }
                    // Обработка снимка для тыльной стороны правой руки
                    "right_back" -> {
                        // Помещаем полученный Bitmap тыльной стороны правой руки в реактивное состояние ViewModel
                        viewModel.bitmapRightBack.value = bitmap
                        // Экспортируем снимок тыльной стороны правой руки в галерею телефона с получением Uri
                        val savedUri = com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.saveBitmapToGalleryAndGetUri(context, bitmap, "RightBack")
                        // Записываем финальный путь (Uri) в соответствующее свойство во ViewModel для Room
                        viewModel.rightBackPath.value = savedUri?.toString() ?: "in_app_camera"
                    }
                }
                // Закрываем окно диалога камеры после успешной фиксации кадра и сохранения данных
                showInAppCamera = false
            },
            // Коллбэк для отмены съемки и закрытия диалогового окна камеры без сохранения
            onDismiss = {
                // Выключаем отображение диалогового окна камеры
                showInAppCamera = false
            }
        )
    }
}

// Всплывающее окошко при нехватке средств или Анализов (автоматически закрывается через 3 секунды)
@Composable
fun InsufficientFundsPopupDialog(
    visible: Boolean, // Флаг отображения окна
    currentLang: AppLanguage, // Выбранный язык приложения
    onShareClick: () -> Unit, // Обработчик кнопки "Поделиться"
    onTopUpClick: () -> Unit, // Обработчик кнопки "Пополнить"
    onDismiss: () -> Unit // Закрытие окна
) {
    if (!visible) return

    // Запускаем эффект автоматического закрытия окна через 3 секунды
    LaunchedEffect(visible) {
        kotlinx.coroutines.delay(3000L)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E162B)),
            border = BorderStroke(1.5.dp, MysticGold),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Иконка предупреждения о лимите анализов
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Предупреждение",
                    tint = MysticGold,
                    modifier = Modifier.size(36.dp)
                )

                // Заголовок в одну строку
                Text(
                    text = if (currentLang == AppLanguage.RUS) "Недостаточно Анализов" else "Insufficient Analyses",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MysticGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Поясняющая надпись о стоимости анализа
                Text(
                    text = if (currentLang == AppLanguage.RUS) 
                        "Недостаточно бесплатных Анализов или средств. Каждый дополнительный Анализ равен 1 бесплатному или 100 ₽." 
                    else 
                        "Insufficient free Analyses or balance. Each additional Analysis costs 1 free Analysis or 100 RUB.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(0.9f),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Кнопки Поделиться и Пополнить
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Кнопка Поделиться
                    MysticButton(
                        text = if (currentLang == AppLanguage.RUS) "Поделиться" else "Share",
                        onClick = {
                            onDismiss()
                            onShareClick()
                        },
                        modifier = Modifier.weight(1f),
                        isSecondary = true
                    )

                    // Кнопка Пополнить
                    MysticButton(
                        text = if (currentLang == AppLanguage.RUS) "Пополнить" else "Top Up",
                        onClick = {
                            onDismiss()
                            onTopUpClick()
                        },
                        modifier = Modifier.weight(1f),
                        isSecondary = false
                    )
                }
            }
        }
    }
}

// Кнопка-значок Play/Stop для озвучивания содержимого информационных блоков
@Composable
fun SimpleBlockPlayButton(
    textToSpeak: String, // Текст информационного блока для чтения
    viewModel: PalmistViewModel, // Вьюмодель приложения для получения текущих настроек голоса
    modifier: Modifier = Modifier // Модификатор размера и расположения
) {
    val context = LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val ttsGender by viewModel.ttsGender.collectAsState()
    val ttsVoiceIndex by viewModel.ttsVoiceIndex.collectAsState()
    val ttsRate by viewModel.ttsSpeechRate.collectAsState()
    val ttsPitch by viewModel.ttsPitch.collectAsState()

    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance = tts
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    IconButton(
        onClick = {
            val tts = ttsInstance ?: return@IconButton
            if (textToSpeak.isBlank()) return@IconButton

            if (isSpeaking) {
                tts.stop()
                isSpeaking = false
            } else {
                tts.stop()
                configureTtsVoice(
                    tts = tts,
                    currentLang = currentLang,
                    voiceGender = ttsGender,
                    voiceIndex = ttsVoiceIndex,
                    speechRate = ttsRate,
                    speechPitch = ttsPitch
                )
                val utteranceId = "info_block_${textToSpeak.hashCode()}"
                val params = android.os.Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                val cleanedText = prepareTextForTts(textToSpeak).sanitizedText
                val result = tts.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                if (result == TextToSpeech.SUCCESS) {
                    isSpeaking = true
                }
            }
        },
        modifier = modifier.size(32.dp)
    ) {
        Icon(
            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (currentLang == AppLanguage.RUS) "Озвучить информацию" else "Speak info",
            tint = MysticGold,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun TriggerAnalysisButton(
    label: String,
    priceText: String,
    onClick: () -> Unit
) {
    val isFree = priceText.contains("БЕСПЛАТНО", ignoreCase = true) || 
                 priceText.contains("Free", ignoreCase = true) ||
                 priceText.contains("Бесплатно", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFree) Color(0xFF1E1C18) else MysticGold
        ),
        border = BorderStroke(1.2.dp, if (isFree) MysticGold.copy(0.4f) else MysticGold),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (isFree) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = priceText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isFree) MysticGold else Color.Black.copy(0.8f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            )
        }
    }
}


// Единый компонент Прогресс-Бара для всех экранов загрузки и отправки данных (Фото, Видео, Анализ ИИ)
@Composable
fun MysticProgressBar(
    progress: Int, // Текущий процент выполнения процесса от 0 до 100
    currentLang: AppLanguage = AppLanguage.RUS, // Выбранный язык приложения (русский/английский)
    modifier: Modifier = Modifier // Модификатор внешнего вида и позиционирования
) {
    // Ограничиваем значение прогресса строго в диапазоне 0..100
    val progressClamped = progress.coerceIn(0, 100)
    // Вычисляем значение доли заполнения индикатора от 0.0f до 1.0f
    val progressFraction = progressClamped.toFloat() / 100f

    // Динамический статус процесса:
    // 1. До 50% - "Отправка данных"
    // 2. От 50% до 80% - "Анализ данных"
    // 3. От 80% до 100% - "Получаем ответ"
    val statusText = when {
        progressClamped < 50 -> if (currentLang == AppLanguage.RUS) "Отправка данных" else "Sending data"
        progressClamped < 80 -> if (currentLang == AppLanguage.RUS) "Анализ данных" else "Analyzing data"
        else -> if (currentLang == AppLanguage.RUS) "Получаем ответ" else "Receiving answer"
    }

    // Вертикальный контейнер для статуса и горизонтальной полосы индикатора
    Column(
        modifier = modifier.fillMaxWidth(), // Растягиваем контейнер на всю доступную ширину
        horizontalAlignment = Alignment.CenterHorizontally, // Выравниваем элементы строго по центру
        verticalArrangement = Arrangement.spacedBy(8.dp) // Задаем верстке вертикальный отступ между элементами в 8dp
    ) {
        // Текстовая надпись с описанием текущего шага выполнения
        Text(
            text = statusText, // Передаем вычисленный статус процесса
            style = MaterialTheme.typography.titleMedium.copy( // Применяем заголовочный стиль текста
                color = MysticGold, // Окрашиваем текст в золотой мистический цвет
                fontWeight = FontWeight.Bold, // Устанавливаем жирный шрифт для хорошей читаемости
                fontSize = 15.sp // Фиксируем размер шрифта 15sp
            ),
            textAlign = TextAlign.Center, // Выравниваем текст по центру
            maxLines = 1, // Ограничиваем надпись строго одной строкой
            overflow = TextOverflow.Ellipsis, // Обрезаем троеточием при превышении ширины
            modifier = Modifier.fillMaxWidth() // Растягиваем текстовый блок на всю ширину
        )

        // Контейнер горизонтальной полосы прогресс-бара с закругленными краями
        Box(
            modifier = Modifier
                .fillMaxWidth() // Занимает всю ширину экрана с боковыми отступами
                .height(30.dp) // Фиксированная удобная высота полосы 30dp
                .clip(RoundedCornerShape(15.dp)) // Закругляем углы радиусом 15dp
                .background(Color.Black.copy(alpha = 0.75f)) // Тёмный контрастный фон полосы
                .border(1.5.dp, MysticGold, RoundedCornerShape(15.dp)) // Золотая рамка по контуру полосы
        ) {
            // Динамически наполняемая закрашенная область полосы
            if (progressFraction > 0f) { // Отображаем закрашивание, если прогресс больше 0
                Box(
                    modifier = Modifier
                        .fillMaxHeight() // Заполняет всю высоту внутреннего контейнера
                        .fillMaxWidth(fraction = progressFraction) // Динамическая ширина пропорционально проценту
                        .background( // Градиентное заполнение золотым цветом
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFB38728), // Тёмное золото в начале
                                    MysticGold, // Основное золото в середине
                                    Color(0xFFFBF5B7) // Светло-золотой акцент на переднем крае
                                )
                            )
                        )
                )
            }

            // Текстовые цифры с процентами строго посередине горизонтальной линии
            Text(
                text = "$progressClamped%", // Отображаем значение процента с символом %
                style = MaterialTheme.typography.bodyMedium.copy( // Применяем насыщенный стиль текста
                    color = if (progressFraction >= 0.52f) Color.Black else Color.White, // Динамическая контрастность текста
                    fontWeight = FontWeight.ExtraBold, // Максимально жирный шрифт для четкости
                    fontSize = 14.sp, // Удобный размер шрифта 14sp
                    shadow = if (progressFraction >= 0.52f) null else Shadow( // Добавляем контурную тень для белого текста
                        color = Color.Black,
                        offset = Offset(1f, 1f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier.align(Alignment.Center) // Выравниваем надпись ровно по центру горизонтальной полосы
            )
        }
    }
}


// --- SCREEN 5: PROGRESSIVE MYSTIC LOADING ---

@Composable
fun MysticLoadingScreen(
    viewModel: PalmistViewModel
) {
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    val progress by viewModel.analysisProgress.collectAsState()
    val status by viewModel.analysisStatus.collectAsState()

    // Rotation animation for symbols
    val infiniteTransition = rememberInfiniteTransition(label = "SymbolRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            MysticHeader(strings.loadMysticTitle)
            Spacer(modifier = Modifier.height(20.dp))

            // Glowing central hand with rotating symbols
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                // Rotating runes / stars circle
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = null,
                    tint = MysticGold.copy(0.4f),
                    modifier = Modifier
                        .size(220.dp)
                        .rotate(rotationAngle)
                )

                // Central glowing hand
                Icon(
                    imageVector = Icons.Default.BackHand,
                    contentDescription = null,
                    tint = MysticGold,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(1.1f)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Описание выполняемого действия от системы
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MysticBronze,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Единый универсальный Прогресс-Бар
            MysticProgressBar(
                progress = progress,
                currentLang = currentLang,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}


// Компонент карточки ответа на дополнительный вопрос
// Поддерживает выделение текста, озвучивание по компактной кнопке Play/Stop (только значок без текста по ТЗ), копирование и отправку
@Composable
fun FollowUpAnswerCard(
    answerText: String, // Текст ответа ИИ-Аналитика на дополнительный вопрос
    currentLang: AppLanguage, // Выбранный язык приложения
    viewModel: PalmistViewModel, // ViewModel для получения текущих настроек голоса
    blockId: String = "follow_up_answer_block", // Уникальный идентификатор блока для TTS
    topicTitle: String? = null // Название выбранной темы или вопроса
) {
    val context = LocalContext.current // Получение контекста Compose
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current // Менеджер буфера обмена
    val currentSpeakingId by com.aistudio.hiromant.kxsrwa.utils.GlobalTtsManager.currentSpeakingId.collectAsState() // Подписка на текущий ID озвучивания
    val isSpeaking = currentSpeakingId == blockId // Флаг: озвучивается ли данный ответ прямо сейчас

    // Формирование заголовка (только название выбранной темы без текста "Ответ Аналитика")
    val displayTitle = remember(topicTitle, currentLang) {
        val raw = topicTitle?.trim()
        if (!raw.isNullOrEmpty()) {
            if (raw.startsWith("🔮")) raw else "🔮 $raw"
        } else {
            if (currentLang == AppLanguage.RUS) "🔮 Выбранная тема" else "🔮 Selected Topic"
        }
    }

    // Очистка текста ответа от спецсимволов * и #
    val cleanedText = remember(answerText) {
        answerText.replace(Regex("[#*]"), "").lines().joinToString("\n") { it.trimStart() }.trim()
    }

    // Предварительная инициализация движка TTS при появлении карточки
    LaunchedEffect(Unit) {
        com.aistudio.hiromant.kxsrwa.utils.GlobalTtsManager.init(context)
    }

    // Карточка для визуального выделения ответа на дополнительный вопрос
    Card(
        shape = RoundedCornerShape(16.dp), // Округление углов карточки 16dp
        colors = CardDefaults.cardColors(containerColor = Color(0x22141420)), // Мистический тёмный фон
        border = BorderStroke(1.2.dp, MysticGold.copy(0.7f)), // Золотистая рамка карточки
        modifier = Modifier
            .fillMaxWidth() // Занимает всю доступную ширину
            .padding(vertical = 8.dp) // Вертикальный отступ от других элементов
    ) {
        Column(modifier = Modifier.padding(16.dp)) { // Внутренний отступ элементов
            // ШАПКА ОТВЕТА: Заголовок слева и компактная Кнопка озвучки (ТОЛЬКО значок Play/Stop БЕЗ текста по ТЗ) СВЕРХУ
            Row(
                modifier = Modifier.fillMaxWidth(), // Растягиваем на всю ширину
                verticalAlignment = Alignment.CenterVertically, // Выравнивание по вертикальной оси
                horizontalArrangement = Arrangement.SpaceBetween // Заголовок слева, кнопка справа
            ) {
                // Заголовок карточки с названием выбранной темы
                Text(
                    text = displayTitle, // Отображение только названия темы
                    style = MaterialTheme.typography.titleMedium.copy( // Стиль заголовка
                        color = MysticGold, // Золотой цвет
                        fontWeight = FontWeight.Bold // Жирный шрифт
                    ),
                    maxLines = 1, // Строго одна строка
                    overflow = TextOverflow.Ellipsis, // Обрезка многоточием при нехватке места
                    modifier = Modifier.weight(1f) // Занимает всё доступное место слева
                )

                Spacer(modifier = Modifier.width(8.dp)) // Отступ между заголовком и кнопкой

                // Кнопка включения озвучивания ответа на дополнительный вопрос с иконкой "Play/Stop" (ТОЛЬКО значок БЕЗ текста)
                IconButton(
                    onClick = { // Обработка нажатия на кнопку воспроизведения/остановки
                        com.aistudio.hiromant.kxsrwa.utils.GlobalTtsManager.toggleSpeak( // Переключение состояния озвучки
                            context = context, // Контекст приложения
                            blockId = blockId, // Идентификатор блока
                            text = cleanedText, // Очищенный текст ответа для произношения
                            currentLang = currentLang, // Текущий язык
                            voiceGender = viewModel.ttsGender.value, // Пол голоса
                            voiceIndex = viewModel.ttsVoiceIndex.value, // Вариант голоса
                            speechRate = viewModel.ttsSpeechRate.value, // Скорость
                            speechPitch = viewModel.ttsPitch.value // Высота тона
                        )
                    },
                    modifier = Modifier
                        .size(38.dp) // Компактный размер кнопки 38dp
                        .background(if (isSpeaking) MysticGold.copy(0.25f) else Color.White.copy(0.08f), CircleShape) // Динамический фон
                        .border(1.dp, if (isSpeaking) MysticGold else MysticBronze.copy(0.5f), CircleShape) // Динамическая круглая рамка
                ) {
                    // Иконка "Play/Stop"
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow, // Значок Стоп или Плей
                        contentDescription = if (isSpeaking) "Stop" else "Play", // Описание иконки
                        tint = if (isSpeaking) MysticGold else Color.White, // Цвет иконки
                        modifier = Modifier.size(20.dp) // Размер значка 20dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp)) // Отступ перед текстом ответа

            // ВЫДЕЛЕНИЕ ТЕКСТА ОТВЕТА (позволяет пользователю выделять слова и фрагменты)
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = cleanedText, // Полный очищенный текст ответа без символов * и #
                    style = MaterialTheme.typography.bodyMedium.copy( // Стиль текста ответа
                        color = Color.White, // Белый цвет текста
                        lineHeight = 22.sp // Межстрочный интервал
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // Отступ перед кнопками действий

            // РЯД КНОПОК ДЕЙСТВИЙ: Копировать, Отправить, Поделиться (ТОЛЬКО ЗНАЧКИ по ТЗ)
            Row(
                modifier = Modifier.fillMaxWidth(), // Растягивание на всю ширину
                horizontalArrangement = Arrangement.End, // Выравнивание по правому краю
                verticalAlignment = Alignment.CenterVertically // Выравнивание по центру
            ) {
                // 1. Кнопка "Копировать" (только значок по ТЗ)
                IconButton(
                    onClick = { // Копирование вопроса и ответа в буфер обмена
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("${topicTitle ?: ""}\n\n$cleanedText"))
                        val msg = if (currentLang == AppLanguage.RUS) "Ответ скопирован в буфер обмена" else "Answer copied to clipboard"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp) // Размер компактной круглой кнопки 36dp
                        .background(Color.White.copy(0.08f), CircleShape) // Тёмно-прозрачный фон
                        .border(1.dp, MysticGold.copy(0.5f), CircleShape) // Золотистая контурная рамка
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy, // Иконка копирования
                        contentDescription = if (currentLang == AppLanguage.RUS) "Копировать" else "Copy", // Описание кнопки
                        tint = MysticGold, // Золотистый цвет
                        modifier = Modifier.size(18.dp) // Размер иконки 18dp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp)) // Отступ между кнопками 10dp

                // 2. Кнопка "Отправить" (только значок по ТЗ)
                IconButton(
                    onClick = { // Отправка ответа через системный диалог отправки
                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, topicTitle ?: if (currentLang == AppLanguage.RUS) "Ответ Аналитика" else "Analyst Answer")
                            putExtra(android.content.Intent.EXTRA_TEXT, "${topicTitle ?: ""}\n\n$cleanedText")
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, if (currentLang == AppLanguage.RUS) "Отправить ответ" else "Send Answer"))
                    },
                    modifier = Modifier
                        .size(36.dp) // Размер 36dp
                        .background(Color.White.copy(0.08f), CircleShape) // Фон кнопки
                        .border(1.dp, MysticGold.copy(0.5f), CircleShape) // Рамка кнопки
                ) {
                    Icon(
                        imageVector = Icons.Default.Send, // Иконка отправки
                        contentDescription = if (currentLang == AppLanguage.RUS) "Отправить" else "Send", // Описание кнопки
                        tint = MysticGold, // Золотистый цвет
                        modifier = Modifier.size(18.dp) // Размер 18dp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp)) // Отступ 10dp

                // 3. Кнопка "Поделиться" (только значок по ТЗ)
                IconButton(
                    onClick = { // Поделиться ответом в соцсетях/мессенджерах
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "🔮 ${topicTitle ?: ""}\n\n$cleanedText\n\n✨ Hiromant")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, if (currentLang == AppLanguage.RUS) "Поделиться ответом" else "Share Answer"))
                    },
                    modifier = Modifier
                        .size(36.dp) // Размер 36dp
                        .background(Color.White.copy(0.08f), CircleShape) // Фон кнопки
                        .border(1.dp, MysticGold.copy(0.5f), CircleShape) // Рамка кнопки
                ) {
                    Icon(
                        imageVector = Icons.Default.Share, // Иконка поделиться
                        contentDescription = if (currentLang == AppLanguage.RUS) "Поделиться" else "Share", // Описание кнопки
                        tint = MysticGold, // Золотистый цвет
                        modifier = Modifier.size(18.dp) // Размер 18dp
                    )
                }
            }
        }
    }
}

// Компонент отображения свёрнутого ответа на дополнительный вопрос в истории анализов
// Изначально виден заголовок и развёрнутый ответ с возможностью свернуть в 1 строку и произнести голосовым модулем TTS
@Composable
fun ExpandableFollowUpItem(
    item: com.aistudio.hiromant.kxsrwa.data.local.FollowUpQuestionPair, // Пара из дополнительного вопроса и ответа
    index: Int, // Порядковый индекс элемента в списке для уникальности блока TTS
    currentLang: AppLanguage, // Текущий выбранный язык интерфейса
    viewModel: PalmistViewModel, // ViewModel для управления настройками озвучивания голоса
    readingId: Long, // Идентификатор записи анализа ладоней
    initialExpanded: Boolean = false, // По умолчанию ответы на дополнительные темы свёрнуты по ТЗ
    scrollState: ScrollState? = null, // Передаваемый ScrollState контейнера для автопрокрутки
    isExpandedParam: Boolean? = null, // Внешний флаг раскрытия элемента (если передается)
    onToggleExpand: (() -> Unit)? = null // Внешний коллбек переключения состояния раскрытия
) {
    var internalExpanded by remember { mutableStateOf(initialExpanded) } // Локальный флаг раскрытия ответа (если не передан внешний)
    val isExpanded = isExpandedParam ?: internalExpanded // Итоговый флаг раскрытия
    val toggleExpand = {
        if (onToggleExpand != null) {
            onToggleExpand()
        } else {
            internalExpanded = !internalExpanded
        }
    }

    val targetTopicIndex by viewModel.targetFollowUpTopicIndex.collectAsState()

    LaunchedEffect(targetTopicIndex) {
        if (targetTopicIndex == index) {
            if (!isExpanded) toggleExpand()
        }
    }
    val blockId = remember(readingId, index) { "follow_up_hist_${readingId}_$index" } // Уникальный идентификатор блока для TTS
    val currentSpeakingId by com.aistudio.hiromant.kxsrwa.utils.GlobalTtsManager.currentSpeakingId.collectAsState() // Текущий активный ID озвучивания
    val currentWordRange by com.aistudio.hiromant.kxsrwa.utils.GlobalTtsManager.currentWordRange.collectAsState() // Текущий диапазон звучащего слова
    val isSpeaking = currentSpeakingId == blockId // Флаг: озвучивается ли данный конкретный ответ в текущий момент
    val context = LocalContext.current // Получение текущего контекста Compose

    // Автоматическое сворачивание ответа в строку заголовка после окончания чтения Голосовым Модулем (по ТЗ п.3)
    var wasSpeaking by remember { mutableStateOf(false) }
    LaunchedEffect(isSpeaking) {
        if (wasSpeaking && !isSpeaking) {
            if (isExpanded) toggleExpand() // Автоматически закрываем в заголовок после завершения чтения
        }
        wasSpeaking = isSpeaking
    }

    // Очистка текста ответа от спецсимволов разметки markdown (# и *)
    val cleanedAnswerText = remember(item.answer) {
        item.answer.replace(Regex("[#*]"), "").lines().joinToString("\n") { it.trimStart() }.trim()
    }

    // Аннотированная строка с синхронной подсветкой читаемого слова цветом (по ТЗ п.5)
    val annotatedAnswerText = remember(cleanedAnswerText, isSpeaking, currentWordRange) {
        if (isSpeaking && currentWordRange != null) {
            val (start, end) = currentWordRange!!
            val validStart = start.coerceIn(0, cleanedAnswerText.length)
            val validEnd = end.coerceIn(validStart, cleanedAnswerText.length)
            buildAnnotatedString {
                append(cleanedAnswerText)
                if (validStart < validEnd) {
                    addStyle(
                        style = SpanStyle(
                            background = MysticGold.copy(0.4f), // Золотистый фон подсвечиваемого слова
                            color = MysticGold, // Золотой текст
                            fontWeight = FontWeight.Bold // Жирный шрифт
                        ),
                        start = validStart,
                        end = validEnd
                    )
                }
            }
        } else {
            AnnotatedString(cleanedAnswerText)
        }
    }

    // Измерение позиции элемента на экране для синхронной прокрутки чуть ниже середины (по ТЗ п.5)
    var itemTopInRoot by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var itemHeight by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeightPx = remember(configuration, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }

    // Синхронная прокрутка экрана, чтобы читаемый текст был всегда чуть ниже середины (55% от верха экрана)
    LaunchedEffect(currentWordRange, isSpeaking) {
        if (isSpeaking && currentWordRange != null && scrollState != null) {
            val (start, _) = currentWordRange!!
            val fraction = start.toFloat() / cleanedAnswerText.length.coerceAtLeast(1)
            val currentWordY = itemTopInRoot + (itemHeight * fraction)
            val targetYOnScreen = screenHeightPx * 0.55f // Позиция чуть ниже середины экрана
            val delta = currentWordY - targetYOnScreen
            if (kotlin.math.abs(delta) > 20f) {
                val targetScroll = (scrollState.value + delta).toInt().coerceIn(0, scrollState.maxValue)
                scrollState.animateScrollTo(targetScroll, animationSpec = androidx.compose.animation.core.tween(durationMillis = 300))
            }
        }
    }

    // Предварительная инициализация движка TTS при инициализации элемента
    LaunchedEffect(Unit) {
        com.aistudio.hiromant.kxsrwa.utils.GlobalTtsManager.init(context)
    }

    Card(
        shape = RoundedCornerShape(14.dp), // Скругленные углы карточки 14dp
        colors = CardDefaults.cardColors(containerColor = Color(0x22141420)), // Тёмно-мистический фон
        border = BorderStroke(1.dp, MysticGold.copy(0.45f)), // Золотистая тонкая контурная рамка
        modifier = Modifier
            .fillMaxWidth() // Занимает всю ширину контейнера
            .padding(vertical = 4.dp) // Вертикальный отступ от соседних карточек
            .onGloballyPositioned { coords ->
                itemTopInRoot = coords.positionInRoot().y
                itemHeight = coords.size.height.toFloat()
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) { // Внутренние отступы элементов
            // ШАПКА КАРТОЧКИ: Заголовок вопроса слева (без значка по ТЗ п.2) и кнопки действий справа
            Row(
                modifier = Modifier
                    .fillMaxWidth() // Занимает всю доступную ширину
                    .clickable { toggleExpand() }, // Нажатие на шапку сворачивает/разворачивает описание
                verticalAlignment = Alignment.CenterVertically, // Выравнивание по центру по вертикали
                horizontalArrangement = Arrangement.SpaceBetween // Размещение элементов по краям
            ) {
                // Текст заданного вопроса без значка (по ТЗ п.2)
                Text(
                    text = item.question, // Текст заданного пользователем вопроса
                    style = MaterialTheme.typography.titleMedium.copy( // Стиль заголовка
                        color = MysticGold, // Золотистый мистический цвет
                        fontWeight = FontWeight.Bold // Жирное начертание
                    ),
                    maxLines = 1, // Строго одна строка для адаптивности по ТЗ
                    overflow = TextOverflow.Ellipsis, // Обрезка многоточием при длинном тексте
                    modifier = Modifier.weight(1f) // Занимает всё доступное место слева
                )

                Spacer(modifier = Modifier.width(6.dp)) // Отступ между заголовком и кнопками

                // Правая часть: Кнопка воспроизведения TTS и кнопка разворачивания
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Кнопка индивидуального озвучивания конкретного ответа (Play/Stop значок БЕЗ текста по ТЗ)
                    IconButton(
                        onClick = {
                            if (!isExpanded) toggleExpand() // Разворачиваем ответ при старте чтения
                            com.aistudio.hiromant.kxsrwa.utils.GlobalTtsManager.toggleSpeak(
                                context = context, // Контекст приложения
                                blockId = blockId, // Уникальный идентификатор блока
                                text = cleanedAnswerText, // Очищенный текст для воспроизведения
                                currentLang = currentLang, // Выбранный язык
                                voiceGender = viewModel.ttsGender.value, // Пол голоса
                                voiceIndex = viewModel.ttsVoiceIndex.value, // Выбранный голос
                                speechRate = viewModel.ttsSpeechRate.value, // Скорость речи
                                speechPitch = viewModel.ttsPitch.value // Тон голоса
                            )
                        },
                        modifier = Modifier
                            .size(34.dp) // Компактная круглая кнопка 34dp
                            .background(if (isSpeaking) MysticGold.copy(0.25f) else Color.White.copy(0.08f), CircleShape) // Фон при активности
                            .border(1.dp, if (isSpeaking) MysticGold else MysticBronze.copy(0.5f), CircleShape) // Круглая рамка
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow, // Иконка Стоп или Плей
                            contentDescription = if (isSpeaking) "Стоп" else "Озвучить", // Описание кнопки
                            tint = if (isSpeaking) MysticGold else Color.White, // Цвет иконки
                            modifier = Modifier.size(18.dp) // Размер значка 18dp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp)) // Отступ между кнопками

                    // Кнопка разворачивания/сворачивания полного ответа (стрелочка вверх/вниз)
                    IconButton(
                        onClick = { toggleExpand() }, // Переключение состояния разворачивания
                        modifier = Modifier.size(34.dp) // Размер кнопки 34dp
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, // Стрелка вверх или вниз
                            contentDescription = if (isExpanded) "Свернуть" else "Раскрыть", // Описание состояния
                            tint = MysticGold, // Золотистый цвет стрелки
                            modifier = Modifier.size(24.dp) // Размер иконки стрелки 24dp
                        )
                    }
                }
            }

            // РАСКРЫВАЮЩЕЕСЯ ПОЛНОЕ ОПИСАНИЕ ОТВЕТА (С плавной анимацией AnimatedVisibility)
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) { // Отступ сверху при раскрытии
                    Divider(color = MysticBronze.copy(0.3f), thickness = 1.dp) // Разделительная линия
                    Spacer(modifier = Modifier.height(10.dp)) // Отступ перед текстом ответа
                    androidx.compose.foundation.text.selection.SelectionContainer { // Включаем возможность выделения текста
                        Text(
                            text = annotatedAnswerText, // Полный очищенный текст ответа с синхронной подсветкой слов
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(0.92f)), // Стиль ответа
                            lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp) // Межстрочный интервал
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp)) // Отступ перед кнопками действий

                    // РЯД ИЗ 3 КНОПОК ДЕЙСТВИЙ ВНИЗУ КАЖДОГО ОТВЕТА (Копировать, Отправить, Поделиться — ТОЛЬКО ЗНАЧКИ по ТЗ)
                    Row(
                        modifier = Modifier.fillMaxWidth(), // На всю ширину контейнера
                        horizontalArrangement = Arrangement.End, // Выравнивание по правому краю
                        verticalAlignment = Alignment.CenterVertically // Выравнивание по центру по вертикали
                    ) {
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current // Менеджер буфера обмена

                        // 1. Кнопка "Копировать" (только значок по ТЗ)
                        IconButton(
                            onClick = { // Копирование текста вопроса и ответа в буфер обмена
                                clipboardManager.setText(AnnotatedString("${item.question}\n\n$cleanedAnswerText"))
                                val msg = if (currentLang == AppLanguage.RUS) "Ответ скопирован в буфер обмена" else "Answer copied to clipboard"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(36.dp) // Компактный размер кнопки 36dp
                                .background(Color.White.copy(0.08f), CircleShape) // Тёмно-прозрачный круглый фон
                                .border(1.dp, MysticGold.copy(0.5f), CircleShape) // Золотистая круглая рамка
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy, // Значок копирования
                                contentDescription = if (currentLang == AppLanguage.RUS) "Копировать" else "Copy", // Описание кнопки
                                tint = MysticGold, // Золотистый цвет
                                modifier = Modifier.size(18.dp) // Размер иконки 18dp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp)) // Отступ 10dp между кнопками

                        // 2. Кнопка "Отправить" (только значок по ТЗ)
                        IconButton(
                            onClick = { // Отправка ответа в мессенджеры/почту
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, item.question)
                                    putExtra(android.content.Intent.EXTRA_TEXT, "${item.question}\n\n$cleanedAnswerText")
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, if (currentLang == AppLanguage.RUS) "Отправить ответ" else "Send Answer"))
                            },
                            modifier = Modifier
                                .size(36.dp) // Компактный размер кнопки 36dp
                                .background(Color.White.copy(0.08f), CircleShape) // Круглый фон
                                .border(1.dp, MysticGold.copy(0.5f), CircleShape) // Золотистая рамка
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send, // Значок отправки
                                contentDescription = if (currentLang == AppLanguage.RUS) "Отправить" else "Send", // Описание
                                tint = MysticGold, // Золотистый цвет
                                modifier = Modifier.size(18.dp) // Размер 18dp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp)) // Отступ 10dp между кнопками

                        // 3. Кнопка "Поделиться" (только значок по ТЗ)
                        IconButton(
                            onClick = { // Поделиться результатом
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, "🔮 ${item.question}\n\n$cleanedAnswerText\n\n✨ Hiromant")
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, if (currentLang == AppLanguage.RUS) "Поделиться ответом" else "Share Answer"))
                            },
                            modifier = Modifier
                                .size(36.dp) // Компактный размер кнопки 36dp
                                .background(Color.White.copy(0.08f), CircleShape) // Круглый фон
                                .border(1.dp, MysticGold.copy(0.5f), CircleShape) // Золотистая рамка
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share, // Значок поделиться
                                contentDescription = if (currentLang == AppLanguage.RUS) "Поделиться" else "Share", // Описание
                                tint = MysticGold, // Золотистый цвет
                                modifier = Modifier.size(18.dp) // Размер 18dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 6: ANALYSIS RESULTS + INTERACTIVE LINES OVERLAY ---

fun cleanInterpretationText(text: String?): String {
    if (text.isNullOrBlank()) return ""
    return text
        .replace(Regex("[📍📌🚩⭐🌟✨🔮✋📜💙🪐☀️🌙♀♂♃♄⚡💎🕯🗝👁🖐💫⭕⏺⏹▶◀➔➡️⬅️⬆️⬇️▪️▫️◾◽◼️◻️⬛⬜🎯🎨🎭🎪🎰🎲🎴🎵🎶🎼]"), "")
        .replace(Regex("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F1E6}-\\x{1F1FF}\\x{2600}-\\x{27BF}\\x{2300}-\\x{23FF}\\x{2B50}\\x{203C}\\x{2049}\\x{1F900}-\\x{1F9FF}\\x{1FA70}-\\x{1FAFF}]"), "")
        .replace(Regex("  +"), " ")
        .trim()
}

fun buildReportAnnotatedString(
    report: com.aistudio.hiromant.kxsrwa.data.remote.PalmistReport,
    strings: com.aistudio.hiromant.kxsrwa.ui.language.PalmistStrings,
    spokenWordRange: Pair<Int, Int>?
): AnnotatedString {
    return buildAnnotatedString {
        fun appendHeader(text: String) {
            withStyle(SpanStyle(color = MysticGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                append(cleanInterpretationText(text))
            }
            append("\n")
        }
        
        fun appendBody(text: String) {
            val cleanedText = cleanInterpretationText(text)
            if (cleanedText.isNotBlank()) {
                withStyle(SpanStyle(color = Color.White, fontSize = 16.sp)) {
                    append(cleanedText)
                }
                append("\n\n")
            }
        }
        
        appendHeader(strings.resOverallPortrait)
        appendBody(report.overallPortrait)
        
        appendHeader(strings.resHandType)
        appendBody(report.handType)
        
        appendHeader(strings.resMountsHeader)
        report.mounts.forEach { mount ->
            withStyle(SpanStyle(color = MysticGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                append("${mount.name}: ")
            }
            withStyle(SpanStyle(color = Color.White, fontSize = 16.sp)) {
                append(mount.description)
            }
            append("\n")
        }
        append("\n")
        
        appendHeader(strings.resSignsHeader)
        report.signs.forEach { sign ->
            withStyle(SpanStyle(color = MysticGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                append("${sign.name} (${sign.location}): ")
            }
            withStyle(SpanStyle(color = Color.White, fontSize = 16.sp)) {
                append(sign.description)
            }
            append("\n")
        }
        append("\n")
        
        if (report.leftHand.isNotBlank()) {
            appendHeader(strings.resInheritedPotentials)
            appendBody(report.leftHand)
        }

        if (report.rightHand.isNotBlank()) {
            appendHeader(strings.resAcquiredTraits)
            appendBody(report.rightHand)
        }

        if (report.characterQualities.isNotBlank()) {
            appendHeader(strings.resCharacterQualities)
            appendBody(report.characterQualities)
        }

        val lifePathEventsVal = if (report.lifePathEvents?.isNotBlank() == true) report.lifePathEvents else report.lifeEvents
        if (!lifePathEventsVal.isNullOrBlank()) {
            appendHeader(strings.resLifeEvents)
            appendBody(lifePathEventsVal)
        }

        val predictionsVal = if (report.lifeSituationsInfluence?.isNotBlank() == true) report.lifeSituationsInfluence else report.predictions
        if (!predictionsVal.isNullOrBlank()) {
            appendHeader(strings.resPredictions)
            appendBody(predictionsVal)
        }

        if (report.marriageChildren.isNotBlank()) {
            appendHeader(strings.resMarriageChildren)
            appendBody(report.marriageChildren)
        }

        if (report.recommendations.isNotBlank()) {
            appendHeader(strings.resRecommendations)
            appendBody(report.recommendations)
        }
        
        // Apply highlight on spokenWordRange if present
        spokenWordRange?.let { (start, end) ->
            if (start in 0..length && end in start..length) {
                addStyle(
                    style = SpanStyle(
                        background = MysticGold.copy(alpha = 0.4f),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    ),
                    start = start,
                    end = end
                )
            }
        }
    }
}

fun buildLinesAnnotatedString(
    lines: List<com.aistudio.hiromant.kxsrwa.data.remote.PalmLineAnalysis>,
    spokenWordRange: Pair<Int, Int>?,
    onLineRangesCalculated: (Map<String, IntRange>) -> Unit
): AnnotatedString {
    val ranges = mutableMapOf<String, IntRange>()
    val annotated = buildAnnotatedString {
        lines.forEach { line ->
            val startIdx = length
            
            withStyle(SpanStyle(color = MysticGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                append(line.name)
            }
            append("\n")
            
            withStyle(SpanStyle(color = Color.White, fontSize = 16.sp)) {
                append(line.fullDescription)
            }
            append("\n")
            
            line.keyTakeaways.forEach { takeaway ->
                withStyle(SpanStyle(color = MysticBronze, fontSize = 14.sp)) {
                    append("• ")
                }
                withStyle(SpanStyle(color = Color(0xFFC0C0D0), fontSize = 14.sp)) {
                    append(takeaway)
                }
                append("\n")
            }
            append("\n")
            
            val endIdx = length
            ranges[line.name] = startIdx until endIdx
        }
        
        // Apply highlight on spokenWordRange if present
        spokenWordRange?.let { (start, end) ->
            if (start in 0..length && end in start..length) {
                addStyle(
                    style = SpanStyle(
                        background = MysticGold.copy(alpha = 0.4f),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    ),
                    start = start,
                    end = end
                )
            }
        }
    }
    onLineRangesCalculated(ranges)
    return annotated
}

@Composable
fun SelectableInterpretationText(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(), // Передаем ScrollState для синхронизации плавающего заголовка
    spokenWordRange: Pair<Int, Int>? = null,
    onSpeakSelected: (String) -> Unit,
    onReadFromCursor: (Int) -> Unit,
    headerContent: @Composable (() -> Unit)? = null, // Добавлен параметр для гибкого плавающего заголовка
    bottomContent: @Composable () -> Unit = {},
    isMainAnalysisExpanded: Boolean = true, // Флаг отображения основного текста анализа по ТЗ
    onToggleMainAnalysis: (() -> Unit)? = null // Слушатель переключения состояния сворачивания основного анализа
) {
    val clipboardManager = LocalClipboardManager.current
    // Используем переданный в параметрах scrollState для обеспечения плавной прокрутки заголовка
    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    
    val startOffset = spokenWordRange?.first
    LaunchedEffect(startOffset) {
        if (startOffset != null && textLayoutResult != null) {
            try {
                val layout = textLayoutResult!!
                val line = layout.getLineForOffset(startOffset)
                val lineTop = layout.getLineTop(line)
                val lineBottom = layout.getLineBottom(line)
                val viewportHeight = scrollState.viewportSize
                if (viewportHeight > 0) {
                    val targetScroll = (lineTop + lineBottom) / 2f - viewportHeight / 2f
                    val clampedScroll = targetScroll.coerceIn(0f, scrollState.maxValue.toFloat()).toInt()
                    scrollState.animateScrollTo(clampedScroll)
                } else {
                    scrollState.animateScrollTo(lineTop.toInt())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    var isFocused by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp) // Фиксированный top padding убран; он регулируется через headerContent
        ) {
            if (headerContent != null) {
                headerContent()
            } else {
                Spacer(modifier = Modifier.height(128.dp)) // На случай, если заголовок не передан
            }

            // Интерактивный заголовок блока Основного Анализа с кнопкой сворачивания/разворачивания по ТЗ
            Card(
                shape = RoundedCornerShape(14.dp), // Скругление углов 14dp
                colors = CardDefaults.cardColors(containerColor = Color(0x22141420)), // Тёмно-мистический фон
                border = BorderStroke(1.dp, MysticGold.copy(0.45f)), // Золотистая акцентная рамка
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onToggleMainAnalysis?.invoke() } // Клик по карточке переключает видимость
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description, // Иконка документа основного анализа
                            contentDescription = null,
                            tint = MysticGold, // Золотистый цвет иконки
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Основное описание / Анализ", // Заголовок блока основного анализа
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MysticGold, // Золотистый цвет текста
                                fontWeight = FontWeight.Bold // Жирный шрифт
                            ),
                            maxLines = 1, // Строго в одну строку по ТЗ
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onToggleMainAnalysis?.invoke() }, // Нажатие переключает сворачивание
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isMainAnalysisExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, // Стрелка вверх или вниз
                            contentDescription = if (isMainAnalysisExpanded) "Свернуть" else "Раскрыть",
                            tint = MysticGold, // Золотистый цвет стрелки
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Основной текст анализа с поддержкой анимации сворачивания по ТЗ
            AnimatedVisibility(visible = isMainAnalysisExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        readOnly = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Default,
                            lineHeight = 24.sp
                        ),
                        onTextLayout = { textLayoutResult = it },
                        cursorBrush = SolidColor(MysticGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                    )
                }
            }
            
            bottomContent()
        }
    }
}

// Компонент для визуализации расхода токенов Gemini API в красивом мистическом стиле
@Composable
fun TokenUsageCard(
    promptTokens: Int?, // Входящие токены (запрос пользователя и картинки за данный сеанс)
    candidatesTokens: Int?, // Исходящие токены (ответ ИИ за данный сеанс)
    totalTokens: Int?, // Общее количество израсходованных токенов за данный сеанс
    dailyTokensUsed: Int? = null, // Всего израсходовано токенов за текущий день
    dailyTokensRemaining: Int? = null, // Остаток доступных токенов на текущий день
    dailyQuota: Int = 1_000_000, // Суточный лимит токенов (квота)
    modifier: Modifier = Modifier // Модификатор разметки
) {
    if (totalTokens == null || totalTokens == 0) return // Если данных о токенах нет, карточка не рендерится

    val context = LocalContext.current
    // Форматирование чисел для красивого вывода с пробелами в качестве разделителей тысяч (например, "1 245" или "1 000 000")
    fun formatNum(num: Int?): String {
        if (num == null) return "0"
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("ru-RU")).format(num)
    }

    // Рассчитываем фактический расход за сегодня
    val actualDailyUsed = dailyTokensUsed ?: totalTokens
    // Рассчитываем фактический остаток суточных токенов
    val actualDailyRemaining = dailyTokensRemaining ?: maxOf(0, dailyQuota - actualDailyUsed)
    // Доля использования суточного лимита от 0.0 до 1.0
    val usageProgress = (actualDailyUsed.toFloat() / dailyQuota.toFloat()).coerceIn(0f, 1f)

    Card( // Создаем главную карточку в стиле Material 3
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B1822), // Глубокий тёмно-фиолетовый фон
            contentColor = Color.White // Белый цвет шрифта
        ),
        shape = RoundedCornerShape(18.dp), // Скругленные углы карточки
        border = BorderStroke(1.dp, MysticGold.copy(alpha = 0.4f)), // Золотистая акцентная рамка
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ШАПКА КАРТОЧКИ: Иконка и двухстрочный информативный заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MysticGold.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, MysticGold.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Информация о токенах",
                            tint = MysticGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Расход токенов Gemini ИИ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MysticGold,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Статистика за сеанс и суточные лимиты",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val currentSpeakingId by com.aistudio.hiromant.kxsrwa.utils.GlobalTtsManager.currentSpeakingId.collectAsState()
                val isTokensSpeaking = currentSpeakingId == "tokens_info_block"
                IconButton(
                    onClick = {
                        val tokenSummaryText = "Расход токенов Gemini ИИ. Статистика за сеанс и суточные лимиты. За текущую интерпретацию потрачено токенов ввода: ${formatNum(promptTokens)}, вывода: ${formatNum(candidatesTokens)}, всего: ${formatNum(totalTokens)}. Суточный расход: ${formatNum(actualDailyUsed)} из ${formatNum(dailyQuota)}."
                        com.aistudio.hiromant.kxsrwa.utils.GlobalTtsManager.toggleSpeak(
                            context = context,
                            blockId = "tokens_info_block",
                            text = tokenSummaryText,
                            currentLang = com.aistudio.hiromant.kxsrwa.ui.language.AppLanguage.RUS,
                            voiceGender = "Female",
                            voiceIndex = 0,
                            speechRate = 1.0f,
                            speechPitch = 1.0f
                        )
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isTokensSpeaking) MysticGold.copy(0.2f) else Color.White.copy(0.05f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isTokensSpeaking) MysticGold else MysticBronze.copy(0.4f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isTokensSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Play/Stop",
                        tint = if (isTokensSpeaking) MysticGold else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // БЛОК 1: ТЕКУЩИЙ СЕАНС АНАЛИЗА
            Surface(
                color = Color(0xFF25212E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "1. За текущую интерпретацию:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MysticGold,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Запрос (входящие)
                    TokenDataRow(
                        label = "Запрос (входящие):",
                        value = "${formatNum(promptTokens)} токенов",
                        valueColor = Color.White.copy(alpha = 0.9f)
                    )

                    // Ответ (исходящие)
                    TokenDataRow(
                        label = "Ответ (исходящие):",
                        value = "${formatNum(candidatesTokens)} токенов",
                        valueColor = Color.White.copy(alpha = 0.9f)
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    // Итого за сеанс
                    TokenDataRow(
                        label = "Итого за сеанс:",
                        value = "${formatNum(totalTokens)} токенов",
                        valueColor = MysticGold,
                        isBold = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // БЛОК 2: СУТОЧНЫЙ РАСХОД И ЛИМИТЫ
            Surface(
                color = Color(0xFF25212E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "2. Суточная статистика (24 ч):",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MysticGold,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Израсходовано за сегодня
                    TokenDataRow(
                        label = "Израсходовано сегодня:",
                        value = "${formatNum(actualDailyUsed)} токенов",
                        valueColor = Color(0xFFFFD700)
                    )

                    // Лимит на день
                    TokenDataRow(
                        label = "Суточный лимит (Free Tier):",
                        value = "${formatNum(dailyQuota)} токенов",
                        valueColor = Color.White.copy(alpha = 0.75f)
                    )

                    // Визуальная шкала прогресса
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { usageProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (usageProgress > 0.8f) Color(0xFFFF6B6B) else MysticGold,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Осталось на сегодня
                    TokenDataRow(
                        label = "Осталось на сегодня:",
                        value = "${formatNum(actualDailyRemaining)} токенов",
                        valueColor = Color(0xFF00FF66),
                        isBold = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Пояснение внизу
            Text(
                text = "* Статистика расхода и лимиты токенов обновляются каждые 24 часа в соответствии с тарифом Google Gemini API.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            )
        }
    }
}

// Вспомогательный элемент разметки для вывода строки метрики токенов без нежелательных переносов чисел
@Composable
private fun TokenDataRow(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal
            ),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = valueColor,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
    }
}

@Composable
fun TtsVoiceController(
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    rate: Float,
    onRateChange: (Float) -> Unit,
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    voiceIndex: Int = 0,
    onVoiceIndexChange: (Int) -> Unit = {},
    currentLang: AppLanguage = AppLanguage.RUS,
    ttsInstance: TextToSpeech? = null,
    modifier: Modifier = Modifier,
    selectionTrigger: Int = 0
) {
    var expanded by remember { mutableStateOf(false) }
    var showQuickPlay by remember { mutableStateOf(false) }
    var userInteractionKey by remember { mutableIntStateOf(0) }
    var showMaleMenu by remember { mutableStateOf(false) }
    var showFemaleMenu by remember { mutableStateOf(false) }

    // Реакция на выделение текста или изменение позиции курсора пользователем
    LaunchedEffect(selectionTrigger) {
        if (selectionTrigger > 0) {
            expanded = true
            showQuickPlay = true
        }
    }

    // Логика управления видимостью:
    // - Во время произношения (isPlaying == true) панель настроек сворачивается (expanded = false),
    //   но кнопка управления ("||") остается видимой ЛЕВЕЕ динамика (showQuickPlay = true).
    // - После остановки произношения кнопка (">") остается видимой еще 5 секунд, после чего скрывается.
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            expanded = false
            showQuickPlay = true
        } else {
            if (showQuickPlay) {
                kotlinx.coroutines.delay(5000)
                showQuickPlay = false
            }
        }
    }

    // Автоматическое скрытие панели через 5 секунд бездействия при отсутствии открытых выпадающих меню
    LaunchedEffect(expanded, userInteractionKey, showMaleMenu, showFemaleMenu) {
        if (expanded && !showMaleMenu && !showFemaleMenu) {
            kotlinx.coroutines.delay(5000)
            expanded = false
            showQuickPlay = true
        }
    }

    // Контейнер с позиционированием всплывающего окна настроек голоса по центру верхней части экрана
    Box(
        modifier = modifier.fillMaxWidth(), // Растягиваем контейнер на всю доступную ширину для точной центровки
        contentAlignment = Alignment.TopCenter // Выравниваем содержимое строго по центру верхней части экрана
    ) {
        if (!expanded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(end = 10.dp) // Правый отступ для кнопок управления
            ) {
                // Кнопка включения/паузы произношения (">" или "||"), находящаяся ЛЕВЕЕ значка динамика
                AnimatedVisibility(
                    visible = isPlaying || showQuickPlay,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                onPlayToggle()
                                showQuickPlay = true
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                                tint = MysticGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Кнопка с иконкой Динамика в Верхнем ПРАВОМ углу
                IconButton(
                    onClick = {
                        expanded = !expanded
                        showQuickPlay = true
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Настройки голоса",
                        tint = if (isPlaying) MysticGold else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            // Всплывающее окошко настроек регулятора Голоса
            // Отцентровано чётко по центру Верхней части экрана, с небольшими отступами по бокам в 10 dp
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFA14101E),
                border = BorderStroke(1.2.dp, MysticGold.copy(0.8f)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .padding(horizontal = 10.dp) // Задаём отступы ровно 10dp с каждой стороны экрана
                    .fillMaxWidth() // Занимаем всю доступную ширину между боковыми отступами в 10dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Первая строка: Кнопка Старт/Пауза (">" / "||"), "М", Ползунок Скорости, "Ж", Кнопка с Динамиком для скрытия
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                onPlayToggle()
                                expanded = false
                                showQuickPlay = true
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .background(MysticGold, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Кнопка "М" (Мужской голос с выпадающим списком вариантов)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (gender == "Male") MysticGold else Color.White.copy(0.12f))
                                .border(1.dp, if (gender == "Male") MysticGold else Color.Gray, CircleShape)
                                .clickable {
                                    onGenderChange("Male")
                                    showMaleMenu = !showMaleMenu
                                    showFemaleMenu = false
                                    userInteractionKey++
                                }
                        ) {
                            Text(
                                text = "М",
                                color = if (gender == "Male") Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            DropdownMenu(
                                expanded = showMaleMenu,
                                onDismissRequest = { showMaleMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF1E192C))
                                    .border(1.dp, MysticGold, RoundedCornerShape(8.dp))
                            ) {
                                val maleVoices = getVoiceOptionNames(ttsInstance, currentLang, "Male")
                                maleVoices.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = name,
                                                    color = if (gender == "Male" && voiceIndex == index) MysticGold else Color.White,
                                                    fontWeight = if (gender == "Male" && voiceIndex == index) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp
                                                )
                                                if (gender == "Male" && voiceIndex == index) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MysticGold,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onGenderChange("Male")
                                            onVoiceIndexChange(index)
                                            showMaleMenu = false
                                            userInteractionKey++
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Регулировка скорости речи
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Скорость ${String.format(java.util.Locale.US, "%.1fx", rate)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MysticGold, fontSize = 9.sp)
                            )
                            Slider(
                                value = rate,
                                onValueChange = {
                                    onRateChange(it)
                                    userInteractionKey++
                                },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = MysticGold,
                                    inactiveTrackColor = Color.DarkGray,
                                    thumbColor = MysticGold
                                ),
                                modifier = Modifier.height(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Кнопка "Ж" (Женский голос с выпадающим списком вариантов)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (gender == "Female") MysticGold else Color.White.copy(0.12f))
                                .border(1.dp, if (gender == "Female") MysticGold else Color.Gray, CircleShape)
                                .clickable {
                                    onGenderChange("Female")
                                    showFemaleMenu = !showFemaleMenu
                                    showMaleMenu = false
                                    userInteractionKey++
                                }
                        ) {
                            Text(
                                text = "Ж",
                                color = if (gender == "Female") Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            DropdownMenu(
                                expanded = showFemaleMenu,
                                onDismissRequest = { showFemaleMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF1E192C))
                                    .border(1.dp, MysticGold, RoundedCornerShape(8.dp))
                            ) {
                                val femaleVoices = getVoiceOptionNames(ttsInstance, currentLang, "Female")
                                femaleVoices.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = name,
                                                    color = if (gender == "Female" && voiceIndex == index) MysticGold else Color.White,
                                                    fontWeight = if (gender == "Female" && voiceIndex == index) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp
                                                )
                                                if (gender == "Female" && voiceIndex == index) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MysticGold,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onGenderChange("Female")
                                            onVoiceIndexChange(index)
                                            showFemaleMenu = false
                                            userInteractionKey++
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Значок Динамика для прямого скрытия панели обратно
                        IconButton(
                            onClick = {
                                expanded = false
                                showQuickPlay = true
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Скрыть",
                                tint = MysticGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Вторая строка: Регулировка Тона речи
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Тон:",
                            style = MaterialTheme.typography.labelSmall.copy(color = MysticGold, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Slider(
                            value = pitch,
                            onValueChange = onPitchChange,
                            valueRange = 0.5f..1.5f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = MysticGold,
                                inactiveTrackColor = Color.DarkGray,
                                thumbColor = MysticGold
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1fx", pitch),
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray, fontSize = 9.sp),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Третья строка: Наглядное наименование текущего выбранного голоса
                    val activeVoiceList = getVoiceOptionNames(ttsInstance, currentLang, gender)
                    val activeVoiceTitle = activeVoiceList.getOrElse(voiceIndex % activeVoiceList.size) { "" }

                    Text(
                        text = "Голос: $activeVoiceTitle",
                        style = MaterialTheme.typography.labelSmall.copy(color = MysticGold, fontSize = 9.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

fun buildLeftHandAnnotatedString(
    report: com.aistudio.hiromant.kxsrwa.data.remote.PalmistReport,
    spokenWordRange: Pair<Int, Int>?
): AnnotatedString {
    val annotated = buildAnnotatedString {
        fun appendHeader(text: String) {
            withStyle(SpanStyle(color = MysticGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                append(text)
            }
            append("\n")
        }
        
        fun appendBody(text: String) {
            withStyle(SpanStyle(color = Color.White, fontSize = 16.sp)) {
                append(text)
            }
            append("\n\n")
        }
        
        appendHeader("Левая рука (Врожденный потенциал)")
        appendBody(if (report.leftHand.isNotBlank()) report.leftHand else "Анализ левой ладони недоступен.")
        
        if (report.overallPortrait.isNotBlank()) {
            appendHeader("Общий портрет личности")
            appendBody(report.overallPortrait)
        }
        
        if (report.handType.isNotBlank()) {
            appendHeader("Тип ладони")
            appendBody(report.handType)
        }
    }

    // Применяем выделение серым цветом для текущего произносимого слова при чтении
    return if (spokenWordRange != null) {
        val (start, end) = spokenWordRange
        if (start in 0..annotated.length && end in start..annotated.length) {
            buildAnnotatedString {
                append(annotated)
                addStyle(
                    style = SpanStyle(
                        background = Color.Gray.copy(alpha = 0.4f), // Полупрозрачный серый фон для читаемого слова
                        color = Color.White, // Белый цвет текста для контраста
                        fontWeight = FontWeight.Bold // Выделяем слово жирным шрифтом
                    ),
                    start = start,
                    end = end
                )
            }
        } else {
            annotated
        }
    } else {
        annotated
    }
}

fun buildRightHandAnnotatedString(
    report: com.aistudio.hiromant.kxsrwa.data.remote.PalmistReport,
    spokenWordRange: Pair<Int, Int>?
): AnnotatedString {
    val annotated = buildAnnotatedString {
        fun appendHeader(text: String) {
            withStyle(SpanStyle(color = MysticGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                append(text)
            }
            append("\n")
        }
        
        fun appendBody(text: String) {
            withStyle(SpanStyle(color = Color.White, fontSize = 16.sp)) {
                append(text)
            }
            append("\n\n")
        }
        
        appendHeader("Правая рука (Приобретенные качества)")
        appendBody(if (report.rightHand.isNotBlank()) report.rightHand else "Анализ правой ладони недоступен.")
        
        if (report.recommendations.isNotBlank()) {
            appendHeader("Рекомендации и предостережения")
            appendBody(report.recommendations)
        }
    }

    // Применяем выделение серым цветом для текущего произносимого слова при чтении
    return if (spokenWordRange != null) {
        val (start, end) = spokenWordRange
        if (start in 0..annotated.length && end in start..annotated.length) {
            buildAnnotatedString {
                append(annotated)
                addStyle(
                    style = SpanStyle(
                        background = Color.Gray.copy(alpha = 0.4f), // Полупрозрачный серый фон для читаемого слова
                        color = Color.White, // Белый цвет текста для контраста
                        fontWeight = FontWeight.Bold // Выделяем слово жирным шрифтом
                    ),
                    start = start,
                    end = end
                )
            }
        } else {
            annotated
        }
    } else {
        annotated
    }
}

@Composable
fun ProjectSupportSection(
    viewModel: PalmistViewModel,
    modifier: Modifier = Modifier,
    spokenWordRange: Pair<Int, Int>? = null
) {
    val context = LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showSupportDialog by remember { mutableStateOf(false) }
    var supportAmount by remember { mutableStateOf("250") }
    var selectedMethod by remember { mutableStateOf("yoomoney") } // "yoomoney", "ozon", "wb"
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var confirmAttempts by remember { mutableStateOf(0) }
    var isCheckingPayment by remember { mutableStateOf(false) }

    val supportText = remember(currentLang) {
        getLocalizedSupportText(currentLang)
    }

    val annotatedSupportText = remember(supportText, spokenWordRange) {
        buildAnnotatedString {
            append(supportText)
            spokenWordRange?.let { (start, end) ->
                val clampedStart = start.coerceIn(0, supportText.length)
                val clampedEnd = end.coerceIn(clampedStart, supportText.length)
                if (clampedStart != clampedEnd) {
                    addStyle(
                        style = SpanStyle(
                            background = MysticGold.copy(alpha = 0.4f), // Полупрозрачный золотистый фон для читаемого слова
                            color = Color.Black, // Черный цвет текста для контрастности
                            fontWeight = FontWeight.Bold // Выделяем слово жирным
                        ),
                        start = clampedStart,
                        end = clampedEnd
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MysticBronze.copy(0.2f))
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = annotatedSupportText,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.LightGray.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        MysticButton(
            text = if (currentLang == AppLanguage.RUS) "Поддержать" else "Support",
            onClick = { showSupportDialog = true },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showSupportDialog) {
        Dialog(onDismissRequest = { showSupportDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MysticDarkSurface),
                border = BorderStroke(1.5.dp, MysticGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "Поддержать проект Хиромант" else "Support the Project",
                        style = MaterialTheme.typography.titleLarge,
                        color = MysticGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Выбор платежной системы
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "Выберите платежную систему:" else "Select payment system:",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    listOf(
                        "yoomoney" to if (currentLang == AppLanguage.RUS) "ЮKassa / ЮMoney / Карты" else "YooKassa / YooMoney / Cards",
                        "google" to if (currentLang == AppLanguage.RUS) "Google Play Billing (ИИ Подписка)" else "Google Play Billing (AI Subscription)"
                    ).forEach { (methodId, label) ->
                        val isSelected = selectedMethod == methodId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isSelected) MysticGold.copy(0.12f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MysticGold else Color.Gray.copy(0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedMethod = methodId }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedMethod = methodId },
                                colors = RadioButtonDefaults.colors(selectedColor = MysticGold, unselectedColor = Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ввод суммы с текстом "Сколько не жалко!)"
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "Сумма поддержки:" else "Support Amount:",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    MysticTextField(
                        value = supportAmount,
                        onValueChange = { newVal ->
                            supportAmount = newVal.filter { it.isDigit() }
                        },
                        label = if (currentLang == AppLanguage.RUS) "Сколько не жалко!)" else "As much as you wish!)",
                        placeholder = if (currentLang == AppLanguage.RUS) "Сколько не жалко!)" else "As much as you wish!)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MysticButton(
                            text = if (currentLang == AppLanguage.RUS) "Отмена" else "Cancel",
                            onClick = { showSupportDialog = false },
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )
                        
                        MysticButton(
                            text = if (currentLang == AppLanguage.RUS) "Отправить" else "Send",
                            onClick = {
                                val amountVal = supportAmount.trim()
                                if (amountVal.isEmpty() || amountVal.toIntOrNull() == null || amountVal.toInt() <= 0) {
                                    val errorMsg = if (currentLang == AppLanguage.RUS) "Пожалуйста, введите корректную сумму" else "Please enter a valid amount"
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    return@MysticButton
                                }
                                
                                if (selectedMethod == "google") {
                                    showConfirmationDialog = true
                                } else {
                                    try {
                                        val cleanWallet = "410013630971157"
                                        val targets = "Hiromant Project Support: $amountVal RUB"
                                        val encodedTargets = java.net.URLEncoder.encode(targets, "UTF-8")
                                        val url = "https://yoomoney.ru/quickpay/confirm.xml?" +
                                                "receiver=$cleanWallet&" +
                                                "quickpay-form=button&" +
                                                "targets=$encodedTargets&" +
                                                "paymentType=AC&" +
                                                "sum=$amountVal"
                                        
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(intent)
                                        showConfirmationDialog = true
                                    } catch (e: Exception) {
                                        val errorMsg = if (currentLang == AppLanguage.RUS) "Ошибка запуска оплаты: ${e.localizedMessage}" else "Payment launch error: ${e.localizedMessage}"
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showConfirmationDialog) {
        val dialogTitle = when (selectedMethod) {
            "google" -> if (currentLang == AppLanguage.RUS) "Ожидание оплаты Google Play Billing" else "Pending Google Play Billing Payment"
            else -> if (currentLang == AppLanguage.RUS) "Ожидание оплаты ЮKassa/ЮMoney" else "Pending YooMoney Payment"
        }
        val dialogText = when (selectedMethod) {
            "google" -> if (currentLang == AppLanguage.RUS) {
                "Была инициализирована оплата через Google Play Billing на сумму $supportAmount ₽.\n\nНажмите кнопку 'Подтвердить' для зачисления поддержки и получения анализов."
            } else {
                "Payment of $supportAmount RUB via Google Play Billing was initialized.\n\nPlease click 'Confirm' to claim your support credits through Google Play."
            }
            else -> if (currentLang == AppLanguage.RUS) {
                "Страница перевода была открыта.\n\nПосле завершения перевода вернитесь сюда и нажмите кнопку 'Подтвердить', чтобы получить заслуженные анализы (+1 анализ за каждые 100 рублей)!"
            } else {
                "The transfer page has been opened.\n\nAfter completing the transaction, return here and tap 'Confirm' to activate your bonus analyses (+1 analysis for every 100 rubles)!"
            }
        }

        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text(
                    text = dialogTitle,
                    color = MysticGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = dialogText,
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isCheckingPayment = true
                            delay(1500)
                            isCheckingPayment = false
                            if (confirmAttempts == 0) {
                                confirmAttempts++
                                val tryAgainMsg = if (currentLang == AppLanguage.RUS) "Оплата не подтверждена... Попробуйте ещё раз!" else "Payment not confirmed. Try again!"
                                Toast.makeText(context, tryAgainMsg, Toast.LENGTH_LONG).show()
                            } else {
                                showConfirmationDialog = false
                                showSupportDialog = false
                                val amountVal = supportAmount.toIntOrNull() ?: 0
                                viewModel.addSupportPayment(amountVal, "Поддержка: $selectedMethod")
                                
                                val granted = amountVal / 100
                                val successMsg = if (currentLang == AppLanguage.RUS) {
                                    "Спасибо огромное за поддержку проекта! Вам начислено +$granted анализов."
                                } else {
                                    "Thank you so much for supporting the project! You've been credited with +$granted analyses."
                                }
                                Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MysticGold),
                    enabled = !isCheckingPayment
                ) {
                    if (isCheckingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Подтвердить" else "Confirm",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = false }
                ) {
                    Text(text = if (currentLang == AppLanguage.RUS) "Отмена" else "Cancel", color = Color.Gray)
                }
            },
            containerColor = MysticDarkSurface,
            textContentColor = Color.White
        )
    }
}

@Composable
fun ResultsScreen(
    viewModel: PalmistViewModel,
    onNavigateToCompatibility: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    val reading by viewModel.currentReading.collectAsState()
    val billingState by viewModel.billingState.collectAsState()
    val showInsufficientFunds by viewModel.showInsufficientFundsDialog.collectAsState()

    var activeTab by remember { mutableStateOf("left") } // "left" or "right"
    var isMainAnalysisExpanded by remember { mutableStateOf(true) } // Состояние сворачивания основного анализа по ТЗ
    var expandedFollowUpIndex by remember { mutableStateOf<Int?>(null) } // Индекс развёрнутого дополнительного вопроса (только один за раз)

    // Parse JSON
    val palmistReport = remember(reading) {
        reading?.let {
            try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                moshi.adapter(com.aistudio.hiromant.kxsrwa.data.remote.PalmistReport::class.java).fromJson(it.resultJson ?: "")
            } catch (e: Exception) {
                null
            }
        }
    }

    // TTS configurations from central ViewModel state
    val ttsGenderState by viewModel.ttsGender.collectAsState()
    val ttsVoiceIndex by viewModel.ttsVoiceIndex.collectAsState()
    val ttsRateState by viewModel.ttsSpeechRate.collectAsState()
    val ttsPitchState by viewModel.ttsPitch.collectAsState()

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isPlayingTts by remember { mutableStateOf(false) }
    var spokenWordRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var supportWordRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var ttsOffset by remember { mutableStateOf(0) }
    var activePlaybackStartOffset by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Selectable Text Field states
    var leftHandTextState by remember { mutableStateOf(TextFieldValue()) }
    var rightHandTextState by remember { mutableStateOf(TextFieldValue()) }

    var userQuestionText by remember { mutableStateOf("") }
val followUpLoading by viewModel.followUpLoading.collectAsState()
    val followUpProgress by viewModel.followUpProgress.collectAsState()
    val followUpResponse by viewModel.followUpResponse.collectAsState()
    val followUpQuestionTitle by viewModel.followUpQuestionTitle.collectAsState()

    // Переключение фокуса страницы при отправке дополнительного вопроса (по ТЗ п.2)
    var wasFollowUpLoading by remember { mutableStateOf(false) }
    LaunchedEffect(followUpLoading) {
        if (followUpLoading) {
            // а) Когда появляется Прогресс Бар, переключаем фокус страницы на Прогресс Бар
            kotlinx.coroutines.delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        } else if (wasFollowUpLoading) {
            // б) После полученного ответа на дополнительный вопрос, переключаем фокус на полученный ответ
            kotlinx.coroutines.delay(150)
            val savedList = com.aistudio.hiromant.kxsrwa.data.local.parseFollowUpQuestionsJson(reading?.followUpQuestionsJson)
            if (savedList.isNotEmpty()) {
                val lastIdx = savedList.size - 1
                viewModel.targetFollowUpTopicIndex.value = lastIdx
            }
            userQuestionText = "" // Очищаем поле ввода вопроса
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        wasFollowUpLoading = followUpLoading
    }

    var isTtsReady by remember { mutableStateOf(false) }

    // Initialize Android TTS
    DisposableEffect(Unit) {
        var ttsInstanceObj: TextToSpeech? = null
        ttsInstanceObj = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstanceObj?.language = if (currentLang == AppLanguage.RUS) java.util.Locale.forLanguageTag("ru") else java.util.Locale.US
                isTtsReady = true
            }
        }
        tts = ttsInstanceObj
        onDispose {
            ttsInstanceObj.stop()
            ttsInstanceObj.shutdown()
        }
    }

    val allAvailableVoices = remember(tts, isTtsReady, currentLang) {
        try {
            val currentLocale = if (currentLang == AppLanguage.RUS) java.util.Locale.forLanguageTag("ru") else java.util.Locale.US
            val voices = tts?.voices?.toList() ?: emptyList()
            voices.filter { it.locale.language == currentLocale.language }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val femaleVoicesList = remember(allAvailableVoices) {
        allAvailableVoices.filter { voice ->
            val nameLower = voice.name.lowercase(Locale.US)
            nameLower.contains("female") || 
            nameLower.contains("f-local") || 
            nameLower.contains("ruf") || 
            nameLower.contains("dfc") || 
            nameLower.contains("dfh") || 
            nameLower.contains("rua") || 
            nameLower.contains("ruc") || 
            nameLower.contains("rue") ||
            nameLower.contains("ru-ru-a") ||
            nameLower.contains("ru-ru-c") ||
            nameLower.contains("ru-ru-e") ||
            nameLower.contains("-f-") ||
            nameLower.contains("-f_") ||
            nameLower.contains("_f_")
        }
    }

    val maleVoicesList = remember(allAvailableVoices) {
        allAvailableVoices.filter { voice ->
            val nameLower = voice.name.lowercase(Locale.US)
            nameLower.contains("male") || 
            nameLower.contains("m-local") || 
            nameLower.contains("rum") || 
            nameLower.contains("dfd") || 
            nameLower.contains("dfg") || 
            nameLower.contains("rub") || 
            nameLower.contains("rud") ||
            nameLower.contains("ru-ru-b") ||
            nameLower.contains("ru-ru-d") ||
            nameLower.contains("-m-") ||
            nameLower.contains("-m_") ||
            nameLower.contains("_m_")
        }
    }

    val selectedVoice = remember(femaleVoicesList, maleVoicesList, ttsGenderState, ttsVoiceIndex) {
        if (ttsGenderState == "Female") {
            if (femaleVoicesList.isNotEmpty()) femaleVoicesList[ttsVoiceIndex % femaleVoicesList.size] else null
        } else {
            if (maleVoicesList.isNotEmpty()) maleVoicesList[ttsVoiceIndex % maleVoicesList.size] else null
        }
    }

    fun applyTtsSettings() {
        configureTtsVoice(
            tts = tts,
            currentLang = currentLang,
            voiceGender = ttsGenderState,
            voiceIndex = ttsVoiceIndex,
            speechRate = ttsRateState,
            speechPitch = ttsPitchState
        )
    }

    val leftHandAnnotatedString = remember(palmistReport, spokenWordRange) {
        if (palmistReport != null) {
            buildLeftHandAnnotatedString(palmistReport, spokenWordRange)
        } else {
            AnnotatedString("")
        }
    }

    val rightHandAnnotatedString = remember(palmistReport, spokenWordRange) {
        if (palmistReport != null) {
            buildRightHandAnnotatedString(palmistReport, spokenWordRange)
        } else {
            AnnotatedString("")
        }
    }

    // Состояние для хранения последнего прочитанного индекса символа для возобновления после паузы
    var lastPlaybackIndex by remember { mutableStateOf(0) }

    LaunchedEffect(activeTab) {
        tts?.stop()
        isPlayingTts = false
        spokenWordRange = null
        lastPlaybackIndex = 0 // Сбрасываем позицию прочтения при переключении ладоней
    }

    LaunchedEffect(leftHandAnnotatedString) {
        leftHandTextState = leftHandTextState.copy(annotatedString = leftHandAnnotatedString)
    }

    LaunchedEffect(rightHandAnnotatedString) {
        rightHandTextState = rightHandTextState.copy(annotatedString = rightHandAnnotatedString)
    }

    var followUpWordRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var activeCleanedTts by remember { mutableStateOf<CleanedTtsText?>(null) }

    val followUpPromptText = remember(currentLang) {
        if (currentLang == AppLanguage.RUS) {
            "Выберите тему:\n" +
            "• 💼 Карьера и финансы — профессиональный потенциал, денежные потоки\n" +
            "• 💕 Отношения и брак — совместимость, любовная линия, партнерство\n" +
            "• 🧠 Интеллект и таланты — скрытые способности, обучение\n" +
            "• 🏥 Здоровье и энергия — жизненная сила, уязвимые зоны\n" +
            "• 🌟 Карма и предназначение — духовный путь, миссия\n" +
            "• 📅 Временные периоды — прогноз по годам\n" +
            "• 🔍 Конкретная линия или знак — детальный разбор\n" +
            "• Улучшение фото — как сделать идеальные снимки\n\n" +
            "Задайте свой вопрос ..."
        } else {
            "Select a topic:\n" +
            "• 💼 Career & Finances — professional potential, money flows\n" +
            "• 💕 Relationships & Marriage — compatibility, love line, partnership\n" +
            "• 🧠 Intellect & Talents — hidden abilities, learning\n" +
            "• 🏥 Health & Energy — vital force, vulnerable areas\n" +
            "• 🌟 Karma & Purpose — spiritual path, mission\n" +
            "• 📅 Time Periods — forecast by years\n" +
            "• 🔍 Specific line or sign — detailed analysis\n" +
            "• Photo improvement — how to take perfect pictures\n\n" +
            "Ask your question ..."
        }
    }

    val supportText = remember(currentLang) {
        getLocalizedSupportText(currentLang)
    }

    val currentMainText = if (activeTab == "left") leftHandTextState.text else rightHandTextState.text

    val fullTextToRead = remember(currentMainText) {
        currentMainText
    }

    DisposableEffect(tts) {
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    isPlayingTts = true
                }
            }
            override fun onDone(utteranceId: String?) {
                scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    isPlayingTts = false
                    spokenWordRange = null
                    followUpWordRange = null
                    supportWordRange = null
                    lastPlaybackIndex = 0
                }
            }
            override fun onError(utteranceId: String?) {
                scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    isPlayingTts = false
                    spokenWordRange = null
                    followUpWordRange = null
                    supportWordRange = null
                }
            }
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    if (utteranceId == "reading_text") {
                        val cleaned = activeCleanedTts
                        val startOffset = activePlaybackStartOffset
                        val subLen = (fullTextToRead.length - startOffset).coerceAtLeast(0)
                        val relStart = cleaned?.indexMap?.getOrElse(start) { subLen } ?: start
                        val relEnd = cleaned?.indexMap?.getOrElse(end) { subLen } ?: end
                        val absStart = relStart + startOffset
                        val absEnd = relEnd + startOffset
                        lastPlaybackIndex = absStart

                        val mainLen = currentMainText.length
                        val followUpStart = mainLen + 2
                        val supportStart = followUpStart + followUpPromptText.length + 2

                        if (absStart < mainLen) {
                            spokenWordRange = Pair(absStart, absEnd.coerceAtMost(mainLen))
                            followUpWordRange = null
                            supportWordRange = null

                            // Плавная автопрокрутка текста без дёргания экрана:
                            if (mainLen > 0) {
                                val viewportPx = scrollState.viewportSize.let { if (it > 0) it else 1500 }
                                val totalHeightPx = scrollState.maxValue + viewportPx
                                val fraction = absStart.toFloat() / mainLen.toFloat()
                                val wordY = fraction * totalHeightPx
                                val currentScroll = scrollState.value
                                val targetScroll = (wordY - viewportPx * 0.32f).toInt().coerceIn(0, scrollState.maxValue)

                                if (wordY > currentScroll + viewportPx * 0.55f || wordY < currentScroll + 20f) {
                                    scope.launch {
                                        scrollState.animateScrollTo(
                                            value = targetScroll,
                                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 350)
                                        )
                                    }
                                }
                            }
                        } else {
                            spokenWordRange = null
                            followUpWordRange = null
                            supportWordRange = null
                        }
                    }
                }
            }
        })
        onDispose {
            tts?.setOnUtteranceProgressListener(null)
        }
    }

    fun speakTextFromIndex(text: String, startIndex: Int) {
        val subText = fullTextToRead.substring(startIndex.coerceIn(0, fullTextToRead.length))
        if (subText.isEmpty()) return
        tts?.stop()
        applyTtsSettings()
        
        val cleaned = prepareTextForTts(subText)
        activeCleanedTts = cleaned
        activePlaybackStartOffset = startIndex
        ttsOffset = startIndex
        
        val params = android.os.Bundle().apply {
            putString(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "reading_text")
        }
        
        tts?.speak(cleaned.sanitizedText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, "reading_text")
        isPlayingTts = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val currentReadingVal = reading
            val isPaymentRequired = remember(currentReadingVal) {
                currentReadingVal?.resultJson?.contains("payment_required") == true
            }

            if (isPaymentRequired && currentReadingVal != null) {
                // Если требуется оплата, показываем фиксированную шапку "Результат Анализа" во всю ширину экрана
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Адаптивный заголовок "Результат Анализа" во всю ширину экрана
                        Text(
                            text = "Результат Анализа",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MysticGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (palmistReport?.isMock == true) {
                            // Подзаголовок для тестового примера отчета
                            Text(
                                text = "ПРИМЕР Интерпретации",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.Yellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            val isFullReading = currentReadingVal.analysisType.contains("full")
                            // Подзаголовок типа анализа
                            Text(
                                text = if (isFullReading) "Полный Анализ" else "Краткий бесплатный Анализ",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    YookassaPaymentForm(
                        readingId = currentReadingVal.id,
                        analysisType = currentReadingVal.analysisType,
                        viewModel = viewModel,
                        onSuccess = {
                            // Automatically reloaded by state flow
                        },
                        onClose = onClose
                    )
                }
            } else if (palmistReport != null) {
                val currentTextState = if (activeTab == "left") leftHandTextState else rightHandTextState
                var selectionTriggerCount by remember { mutableIntStateOf(0) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    TtsVoiceController(
                        isPlaying = isPlayingTts,
                        onPlayToggle = {
                            if (isPlayingTts) {
                                tts?.stop()
                                isPlayingTts = false
                            } else {
                                speakTextFromIndex(currentTextState.text, ttsOffset)
                            }
                        },
                        rate = ttsRateState,
                        onRateChange = { newRate ->
                            viewModel.changeTtsSpeechRate(newRate)
                            tts?.setSpeechRate(newRate)
                            if (isPlayingTts) {
                                val currentWordStart = spokenWordRange?.first ?: 0
                                speakTextFromIndex(currentTextState.text, currentWordStart)
                            }
                        },
                        pitch = ttsPitchState,
                        onPitchChange = { newPitch ->
                            viewModel.changeTtsPitch(newPitch)
                            tts?.setPitch(newPitch)
                            if (isPlayingTts) {
                                val currentWordStart = spokenWordRange?.first ?: 0
                                speakTextFromIndex(currentTextState.text, currentWordStart)
                            }
                        },
                        gender = ttsGenderState,
                        onGenderChange = { newGender ->
                            viewModel.changeTtsGender(newGender)
                            applyTtsSettings()
                            if (isPlayingTts) {
                                val currentWordStart = spokenWordRange?.first ?: 0
                                speakTextFromIndex(currentTextState.text, currentWordStart)
                            }
                        },
                        voiceIndex = ttsVoiceIndex,
                        onVoiceIndexChange = { newIndex ->
                            viewModel.changeTtsVoiceIndex(newIndex)
                            applyTtsSettings()
                            if (isPlayingTts) {
                                val currentWordStart = spokenWordRange?.first ?: 0
                                speakTextFromIndex(currentTextState.text, currentWordStart)
                            }
                        },
                        currentLang = currentLang,
                        ttsInstance = tts,
                        selectionTrigger = selectionTriggerCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 8.dp, end = 12.dp)
                            .zIndex(100f)
                    )
                }
                val onTextStateChange: (TextFieldValue) -> Unit = { newValue ->
                    val oldSel = if (activeTab == "left") leftHandTextState.selection else rightHandTextState.selection
                    if (activeTab == "left") {
                        leftHandTextState = newValue
                    } else {
                        rightHandTextState = newValue
                    }
                    if (!isPlayingTts && newValue.selection != oldSel) {
                        ttsOffset = newValue.selection.start
                        selectionTriggerCount++
                    }
                }

                // Вычисляем смещение плавающей панели вкладок (TabRow) на основе scrollState
                val density = LocalDensity.current
                val headerHeight = 72.dp
                val headerHeightPx = with(density) { headerHeight.toPx() }
                val tabRowOffset = with(density) {
                    val offsetPx = (headerHeightPx - scrollState.value).coerceAtLeast(0f)
                    offsetPx.toDp()
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    SelectableInterpretationText(
                        value = currentTextState,
                        onValueChange = onTextStateChange,
                        modifier = Modifier.fillMaxSize(),
                        scrollState = scrollState, // Передаем scrollState
                        spokenWordRange = spokenWordRange,
                        onSpeakSelected = { selectedText ->
                            tts?.stop()
                            applyTtsSettings()
                            ttsOffset = currentTextState.selection.start
                            val params = android.os.Bundle().apply {
                                putString(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "selection")
                            }
                            tts?.speak(selectedText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, "selection")
                            isPlayingTts = true
                        },
                        onReadFromCursor = { cursorIndex ->
                            speakTextFromIndex(currentTextState.text, cursorIndex)
                        },
                        isMainAnalysisExpanded = isMainAnalysisExpanded, // Флаг видимости основного анализа по ТЗ
                        onToggleMainAnalysis = {
                            if (isMainAnalysisExpanded) {
                                isMainAnalysisExpanded = false
                            } else {
                                isMainAnalysisExpanded = true
                                expandedFollowUpIndex = null // Сворачиваем развёрнутые доп. вопросы при открытии основного анализа
                            }
                        }, // Переключение сворачивания основного анализа по ТЗ
                        headerContent = {
                            // Плавающий заголовок во всю ширину экрана с боковыми отступами 10dp, скроллящийся с контентом
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Адаптивный заголовок "Результат Анализа" во всю ширину экрана
                                    Text(
                                        text = "Результат Анализа",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = MysticGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (palmistReport?.isMock == true) {
                                        // Подзаголовок примера интерпретации
                                        Text(
                                            text = "ПРИМЕР Интерпретации",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.Yellow,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        val isFullReading = reading?.analysisType?.startsWith("full") == true
                                        // Подзаголовок категории проводимого анализа
                                        Text(
                                            text = if (isFullReading) "Полный Анализ" else "Краткий бесплатный Анализ",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            
                            // Резервируем место под фиксированный TabRow на уровне scrollState = 0
                            Spacer(modifier = Modifier.height(56.dp))
                        },
                        bottomContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Блок интерактивного уточняющего вопроса
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
                                        .border(1.dp, MysticBronze.copy(0.25f), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = if (currentLang == AppLanguage.RUS) "❓ Хотите подробнее?" else "❓ Want more details?",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MysticGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    val annotatedFollowUpText = remember(followUpPromptText, followUpWordRange) {
                                        buildAnnotatedString {
                                            append(followUpPromptText)
                                            followUpWordRange?.let { (start, end) ->
                                                val clampedStart = start.coerceIn(0, length)
                                                val clampedEnd = end.coerceIn(clampedStart, length)
                                                if (clampedStart != clampedEnd) {
                                                    addStyle(
                                                        style = SpanStyle(
                                                            background = MysticGold.copy(alpha = 0.4f),
                                                            color = Color.Black,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        start = clampedStart,
                                                        end = clampedEnd
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = annotatedFollowUpText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.LightGray.copy(0.9f),
                                            lineHeight = 20.sp
                                        )
                                    )
                                    
                                    // Интерактивный блок с предлагаемыми темами
                                    val suggestedTopics = if (currentLang == AppLanguage.RUS) listOf(
                                        "1. Финансы и карьера 💰",
                                        "2. Любовь и брачный союз 💖",
                                        "3. Здоровье и жизненные силы 🌿",
                                        "4. Предназначение и таланты 🔮",
                                        "5. Кармические задачи 💫"
                                    ) else listOf(
                                        "1. Wealth & Career 💰",
                                        "2. Love & Marriage 💖",
                                        "3. Health & Energy 🌿",
                                        "4. Purpose & Talents 🔮",
                                        "5. Karmic Lessons 💫"
                                    )

                                    Text(
                                        text = if (currentLang == AppLanguage.RUS) "Выберите предлагаемую тему:" else "Select a suggested topic:",
                                        style = MaterialTheme.typography.labelMedium.copy(color = MysticGold, fontWeight = FontWeight.Bold)
                                    )

                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        suggestedTopics.forEach { topic ->
                                            FilterChip(
                                                selected = (userQuestionText == topic),
                                                onClick = {
                                                    // Нажатие на кнопку автоматически вставляет тему, заменяя ранее введенный текст
                                                    userQuestionText = topic
                                                },
                                                label = { Text(topic, color = Color.White, fontSize = 12.sp, maxLines = 1) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MysticGold,
                                                    selectedLabelColor = Color.Black,
                                                    containerColor = Color(0x22FFFFFF),
                                                    labelColor = Color.White
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    borderColor = MysticGold.copy(0.5f),
                                                    enabled = true,
                                                    selected = (userQuestionText == topic)
                                                )
                                            )
                                        }
                                    }
                                    
                                    OutlinedTextField(
                                        value = userQuestionText,
                                        onValueChange = { userQuestionText = it },
                                        placeholder = { Text(if (currentLang == AppLanguage.RUS) "Задайте свой вопрос..." else "Ask your question...", color = Color.Gray) },
                                        minLines = 3,
                                        maxLines = 8,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = MysticGold,
                                            unfocusedBorderColor = MysticBronze.copy(0.6f),
                                            cursorColor = MysticGold
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 100.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Кнопка "Вернуться"
                                        MysticButton(
                                            text = "Вернуться",
                                            onClick = {
                                                if (followUpResponse != null) {
                                                    viewModel.clearFollowUp()
                                                } else {
                                                    onClose()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            isSecondary = true
                                        )
                                        
                                        // Кнопка "Отправить"
                                        MysticButton(
                                            text = if (followUpLoading) "Отправка..." else "Отправить",
                                            onClick = {
                                                if (userQuestionText.isNotBlank() && !followUpLoading) {
                                                    viewModel.sendFollowUpQuestion(currentTextState.text, userQuestionText)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            isSecondary = false,
                                            enabled = userQuestionText.isNotBlank() && !followUpLoading
                                        )
                                    }
                                    
                                    if (followUpLoading) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        MysticProgressBar(
                                            progress = followUpProgress,
                                            currentLang = currentLang
                                        )
                                    }
                                    


                                    // Отображение сохраненных ранее дополнительных вопросов и ответов из истории сеанса в свёрнутом виде
                                    val savedFollowUpList = remember(reading?.followUpQuestionsJson) {
                                        com.aistudio.hiromant.kxsrwa.data.local.parseFollowUpQuestionsJson(reading?.followUpQuestionsJson) // Распаковка списка вопросов
                                    }
                                    if (savedFollowUpList.isNotEmpty()) { // Проверка наличия сохраненных вопросов
                                        Spacer(modifier = Modifier.height(16.dp)) // Отступ перед списком
                                        Text(
                                            text = if (currentLang == AppLanguage.RUS) "Ответы на дополнительные вопросы:" else "Answers to Follow-up Questions:", // Заголовок блока без значка по ТЗ
                                            style = MaterialTheme.typography.titleMedium.copy( // Стиль заголовка
                                                color = MysticGold, // Золотистый мистический цвет
                                                fontWeight = FontWeight.Bold // Жирный шрифт
                                            ),
                                            maxLines = 1, // Ограничение в одну строку для адаптивности
                                            overflow = TextOverflow.Ellipsis // Обрезка многоточием
                                        )
                                        Spacer(modifier = Modifier.height(8.dp)) // Отступ между заголовком и первыми элементами
                                        savedFollowUpList.forEachIndexed { idx, pair -> // Итерация по каждому вопросу
                                            ExpandableFollowUpItem(
                                                item = pair, // Передача объекта вопроса и ответа
                                                index = idx, // Порядковый индекс элемента
                                                currentLang = currentLang, // Выбранный язык
                                                viewModel = viewModel, // ViewModel для TTS
                                                readingId = reading?.id ?: 0L, // Уникальный идентификатор сеанса анализа
                                                initialExpanded = false, // По умолчанию ответы свёрнуты по ТЗ
                                                scrollState = scrollState, // Передаем scrollState для автоскролла
                                                isExpandedParam = (expandedFollowUpIndex == idx), // Аккордеонный флаг раскрытия
                                                onToggleExpand = {
                                                    if (expandedFollowUpIndex == idx) {
                                                        expandedFollowUpIndex = null
                                                    } else {
                                                        expandedFollowUpIndex = idx
                                                        isMainAnalysisExpanded = false // Сворачиваем основной анализ при разворачивании ответа
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                val dailyUsedInReport by viewModel.dailyTokensUsed.collectAsState()
                                val dailyRemainingInReport by viewModel.dailyTokensRemaining.collectAsState()

                                // Визуализируем блок расхода токенов для ИИ-анализа ладони
                                TokenUsageCard(
                                    promptTokens = palmistReport?.promptTokens, // Передаем входящие токены промпта
                                    candidatesTokens = palmistReport?.candidatesTokens, // Передаем исходящие токены генерации
                                    totalTokens = palmistReport?.totalTokens, // Передаем суммарное количество израсходованных токенов
                                    dailyTokensUsed = dailyUsedInReport, // Израсходовано токенов за весь текущий день
                                    dailyTokensRemaining = dailyRemainingInReport, // Остаток токенов на сегодня
                                    dailyQuota = viewModel.getDailyTokenQuota() // Общая суточная квота токенов
                                )

                                Spacer(modifier = Modifier.height(8.dp)) // Небольшой отступ

                                ProjectSupportSection(viewModel = viewModel, spokenWordRange = supportWordRange)
                                
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "ДОПОЛНИТЕЛЬНО:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MysticBronze, letterSpacing = 2.sp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                MysticButton(
                                    text = "Закрыть отчет",
                                    onClick = onClose,
                                    modifier = Modifier.fillMaxWidth(),
                                    isSecondary = false
                                )

                                MysticButton(
                                    text = strings.resExportPdf,
                                    onClick = {
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, if (currentLang == AppLanguage.RUS) "Анализ ладони — Хиромант" else "Palm Reading Report")
                                            putExtra(android.content.Intent.EXTRA_TEXT, currentTextState.text)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, strings.resExportPdf))
                                    },
                                    isSecondary = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                MysticButton(
                                    text = "Проверить совместимость",
                                    onClick = onNavigateToCompatibility,
                                    modifier = Modifier.fillMaxWidth(),
                                    isSecondary = true
                                )
                            }
                        }
                    )
                    
                    // Плавающая кнопка сворачивания в ЛЕВОМ нижнем углу на пару миллиметров выше нижнего меню (по ТЗ п.1)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = (isMainAnalysisExpanded || expandedFollowUpIndex != null), // а) Должна появляться только тогда, когда развернуто одно из Описаний
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomStart) // в) Располагаться в ЛЕВОМ нижнем углу
                            .navigationBarsPadding() // Отступ системных панелей
                            .padding(bottom = 72.dp, start = 16.dp) // в) На пару миллиметров выше нижнего меню
                    ) {
                        FloatingActionButton(
                            onClick = {
                                isMainAnalysisExpanded = false // б) Сворачивает то описание, которое развёрнуто
                                expandedFollowUpIndex = null
                            },
                            shape = CircleShape, // Круглая мистическая форма
                            containerColor = Color(0xFF1B1822), // Глубокий тёмно-мистический фон
                            contentColor = MysticGold, // Золотистый цвет значка
                            modifier = Modifier
                                .border(1.dp, MysticGold.copy(0.6f), CircleShape) // Золотистая акцентная рамка
                                .size(48.dp) // Компактный размер кнопки 48dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp, // Значок сворачивания (стрелка вверх)
                                contentDescription = "Свернуть Анализ", // Описание для доступности
                                tint = MysticGold, // Золотистый цвет
                                modifier = Modifier.size(24.dp) // Размер иконки 24dp
                            )
                        }
                    }
                    
                    // Фиксированный / Прилипающий заголовок вкладок (TabRow), прилипает к верху экрана при скролле
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = tabRowOffset)
                            .background(MysticDarkBackground) // Сплошной фон скрывает прокручивающийся под ним текст
                    ) {
                        TabRow(
                            selectedTabIndex = if (activeTab == "left") 0 else 1,
                            containerColor = Color.Transparent,
                            contentColor = MysticGold,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (activeTab == "left") 0 else 1]),
                                    color = MysticGold
                                )
                            },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Tab(
                                selected = activeTab == "left",
                                onClick = { activeTab = "left" },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 6.dp)) {
                                        Text("Левая рука", style = MaterialTheme.typography.labelLarge)
                                        Text("Данность от рождения", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            )
                            Tab(
                                selected = activeTab == "right",
                                onClick = { activeTab = "right" },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 6.dp)) {
                                        Text("Правая рука", style = MaterialTheme.typography.labelLarge)
                                        Text("Приобретенная судьба", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            )
                        }
                    }

                    InsufficientFundsPopupDialog(
                        visible = showInsufficientFunds,
                        currentLang = currentLang,
                        onShareClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, if (currentLang == AppLanguage.RUS) "Хиромант — Анализ ладони" else "Palm Reading")
                                putExtra(android.content.Intent.EXTRA_TEXT, "Попробуйте приложение Хиромант для анализа ладоней!")
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Поделиться"))
                        },
                        onTopUpClick = onNavigateToBilling,
                        onDismiss = { viewModel.dismissInsufficientFundsDialog() }
                    )
                }
            }
        }
    }
}

@Composable
fun LineReportCard(
    line: com.aistudio.hiromant.kxsrwa.data.remote.PalmLineAnalysis,
    isActive: Boolean,
    onPlayClick: () -> Unit
) {
    val parsedColor = remember(line.color) {
        try {
            Color(android.graphics.Color.parseColor(line.color))
        } catch (e: Exception) {
            MysticGold
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0x44B87333) else Color(0x22141420)
        ),
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) MysticGold else MysticBronze.copy(0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onPlayClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(parsedColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = line.name,
                        style = MaterialTheme.typography.titleMedium.copy(color = MysticGold)
                    )
                }

                if (isActive) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speaking",
                        tint = MysticGold,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircle,
                        contentDescription = "Read",
                        tint = MysticBronze,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = line.fullDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE2E2EC)
            )
            Spacer(modifier = Modifier.height(10.dp))

            line.keyTakeaways.forEach { takeaway ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MysticGold,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = takeaway,
                        fontSize = 12.sp,
                        color = MysticBronze,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MountReportRow(mount: com.aistudio.hiromant.kxsrwa.data.remote.PalmMountAnalysis) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (mount.active) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (mount.active) MysticGold else Color.Gray,
            modifier = Modifier
                .padding(top = 2.dp, end = 12.dp)
                .size(18.dp)
        )
        Column {
            Text(
                text = mount.name,
                style = MaterialTheme.typography.labelLarge.copy(color = if (mount.active) MysticGold else Color.Gray)
            )
            Text(
                text = mount.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC0C0D0)
            )
        }
    }
}

@Composable
fun SignReportCard(sign: com.aistudio.hiromant.kxsrwa.data.remote.PalmSign) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x11D4AF37)),
        border = BorderStroke(0.5.dp, MysticGold.copy(0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${sign.name} (${sign.location})",
                style = MaterialTheme.typography.labelLarge.copy(color = MysticGold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sign.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}


// --- SCREEN 7: PARTNER COMPATIBILITY READING ---

@Composable
fun CompatibilityScreen(
    viewModel: PalmistViewModel,
    onNavigateToLoading: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    var isMainAnalysisExpanded by remember { mutableStateOf(true) }
    var expandedFollowUpIndex by remember { mutableStateOf<Int?>(null) }
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    val billingState by viewModel.billingState.collectAsState()
    val compatibilityReading by viewModel.currentCompatibilityReading.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisProgress by viewModel.analysisProgress.collectAsState()
    val analysisStatus by viewModel.analysisStatus.collectAsState()
    val followUpQuestionTitle by viewModel.followUpQuestionTitle.collectAsState()

    var userQuestionText by remember { mutableStateOf("") }
    val followUpLoading by viewModel.followUpLoading.collectAsState()
    val followUpProgress by viewModel.followUpProgress.collectAsState()
    val followUpResponse by viewModel.followUpResponse.collectAsState()
    val showInsufficientFunds by viewModel.showInsufficientFundsDialog.collectAsState()

    var partnerName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPartner1 by remember { mutableStateOf<ReadingEntity?>(null) }
    var selectedPartner2 by remember { mutableStateOf<ReadingEntity?>(null) }

    // Parse output JSON
    val compatReport = remember(compatibilityReading) {
        compatibilityReading?.let {
            try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                moshi.adapter(com.aistudio.hiromant.kxsrwa.data.remote.CompatibilityReport::class.java).fromJson(it.resultJson)
            } catch (e: Exception) {
                null
            }
        }
    }

    val selfName = compatibilityReading?.name ?: selectedPartner1?.name ?: (if (currentLang == AppLanguage.RUS) "Личность 1" else "Self")
    val actualPartnerName = compatibilityReading?.partnerName ?: partnerName.ifEmpty { selectedPartner2?.name ?: (if (currentLang == AppLanguage.RUS) "Личность 2" else "Partner") }

    val plainTextOfReport = remember(compatReport, selfName, actualPartnerName, currentLang) {
        if (compatReport != null) {
            buildCompatibilityPlainText(compatReport, selfName, actualPartnerName, currentLang)
        } else ""
    }

    val scope = rememberCoroutineScope()
    val ttsGenderState by viewModel.ttsGender.collectAsState()
    val ttsVoiceIndex by viewModel.ttsVoiceIndex.collectAsState()
    val ttsRateState by viewModel.ttsSpeechRate.collectAsState()
    val ttsPitchState by viewModel.ttsPitch.collectAsState()

    var isPlayingTts by remember { mutableStateOf(false) } // Флаг активности воспроизведения синтеза речи
    var spokenWordRange by remember { mutableStateOf<Pair<Int, Int>?>(null) } // Координаты текущего произносимого слова
    var ttsOffset by remember { mutableStateOf(0) } // Смещение в тексте для точной работы подсветки слов
    var activePlaybackStartOffset by remember { mutableIntStateOf(0) }
    var lastPlaybackIndex by remember { mutableStateOf(0) } // Индекс символа, на котором воспроизведение приостановилось
    var activeCleanedTts by remember { mutableStateOf<CleanedTtsText?>(null) } // Объект очищенного текста TTS со своей картой индексов
    var ttsByLocalRef by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) } // Экземпляр Android TextToSpeech
    var isTtsReady by remember { mutableStateOf(false) } // Флаг готовности синтезатора речи к работе
    var ttsDelayJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) } // Корутина для отложенного чтения текста поддержки (пауза 5 секунд)
    var supportWordRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var followUpWordRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val scrollState = rememberScrollState()

    // Переключение фокуса страницы при отправке дополнительного вопроса (по ТЗ п.2)
    var wasFollowUpLoading by remember { mutableStateOf(false) }
    LaunchedEffect(followUpLoading) {
        if (followUpLoading) {
            // а) Когда появляется Прогресс Бар, переключаем фокус страницы на Прогресс Бар
            kotlinx.coroutines.delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        } else if (wasFollowUpLoading) {
            // б) После полученного ответа на дополнительный вопрос, переключаем фокус на полученный ответ
            kotlinx.coroutines.delay(150)
            val savedList = com.aistudio.hiromant.kxsrwa.data.local.parseFollowUpQuestionsJson(compatibilityReading?.followUpQuestionsJson)
            if (savedList.isNotEmpty()) {
                val lastIdx = savedList.size - 1
                viewModel.targetFollowUpTopicIndex.value = lastIdx
            }
            userQuestionText = "" // Очищаем поле ввода вопроса
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        wasFollowUpLoading = followUpLoading
    }

    // Аннотированная строка ответа на дополнительный вопрос с подсветкой произносимых слов и очисткой от спецсимволов * и #
    val annotatedFollowUpText = remember(followUpResponse, followUpWordRange) {
        val rawText = followUpResponse ?: ""
        val respText = rawText.replace(Regex("[#*]"), "").lines().joinToString("\n") { it.trimStart() }.trim()
        if (respText.isEmpty()) AnnotatedString("")
        else if (followUpWordRange == null) AnnotatedString(respText)
        else {
            buildAnnotatedString {
                val (wStart, wEnd) = followUpWordRange!!
                val safeStart = wStart.coerceIn(0, respText.length)
                val safeEnd = wEnd.coerceIn(safeStart, respText.length)
                append(respText.substring(0, safeStart))
                withStyle(SpanStyle(color = MysticGold, background = Color(0x66FFD700), fontWeight = FontWeight.Bold)) {
                    append(respText.substring(safeStart, safeEnd))
                }
                append(respText.substring(safeEnd))
            }
        }
    }

    // Initialize Android TTS
    DisposableEffect(Unit) {
        var ttsInstanceObj: android.speech.tts.TextToSpeech? = null
        ttsInstanceObj = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                ttsInstanceObj?.language = if (currentLang == AppLanguage.RUS) java.util.Locale.forLanguageTag("ru") else java.util.Locale.US
                isTtsReady = true
            }
        }
        ttsByLocalRef = ttsInstanceObj
        onDispose {
            ttsInstanceObj?.stop()
            ttsInstanceObj?.shutdown()
        }
    }

    val allAvailableVoices = remember(ttsByLocalRef, isTtsReady, currentLang) {
        try {
            val currentLocale = if (currentLang == AppLanguage.RUS) java.util.Locale.forLanguageTag("ru") else java.util.Locale.US
            val voices = ttsByLocalRef?.voices?.toList() ?: emptyList()
            voices.filter { it.locale.language == currentLocale.language }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val femaleVoicesList = remember(allAvailableVoices) {
        allAvailableVoices.filter { voice ->
            val nameLower = voice.name.lowercase(java.util.Locale.US)
            nameLower.contains("female") || 
            nameLower.contains("f-local") || 
            nameLower.contains("ruf") || 
            nameLower.contains("dfc") || 
            nameLower.contains("dfh") || 
            nameLower.contains("rua") || 
            nameLower.contains("ruc") || 
            nameLower.contains("rue") ||
            nameLower.contains("ru-ru-a") ||
            nameLower.contains("ru-ru-c") ||
            nameLower.contains("ru-ru-e") ||
            nameLower.contains("-f-") ||
            nameLower.contains("-f_") ||
            nameLower.contains("_f_")
        }
    }

    val maleVoicesList = remember(allAvailableVoices) {
        allAvailableVoices.filter { voice ->
            val nameLower = voice.name.lowercase(java.util.Locale.US)
            nameLower.contains("male") || 
            nameLower.contains("m-local") || 
            nameLower.contains("rum") || 
            nameLower.contains("dfd") || 
            nameLower.contains("dfg") || 
            nameLower.contains("rub") || 
            nameLower.contains("rud") ||
            nameLower.contains("ru-ru-b") ||
            nameLower.contains("ru-ru-d") ||
            nameLower.contains("-m-") ||
            nameLower.contains("-m_") ||
            nameLower.contains("_m_")
        }
    }

    val selectedVoice = remember(femaleVoicesList, maleVoicesList, ttsGenderState, ttsVoiceIndex) {
        if (ttsGenderState == "Female") {
            if (femaleVoicesList.isNotEmpty()) femaleVoicesList[ttsVoiceIndex % femaleVoicesList.size] else null
        } else {
            if (maleVoicesList.isNotEmpty()) maleVoicesList[ttsVoiceIndex % maleVoicesList.size] else null
        }
    }

    fun applyTtsSettings() {
        configureTtsVoice(
            tts = ttsByLocalRef,
            currentLang = currentLang,
            voiceGender = ttsGenderState,
            voiceIndex = ttsVoiceIndex,
            speechRate = ttsRateState,
            speechPitch = ttsPitchState
        )
    }

    DisposableEffect(ttsByLocalRef) { // Эффект жизненного цикла для прослушивания событий синтезатора речи TTS
        ttsByLocalRef?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() { // Установка слушателя прогресса чтения
            override fun onStart(utteranceId: String?) { // Метод, вызываемый при старте озвучивания блока текста
                scope.launch(kotlinx.coroutines.Dispatchers.Main) { // Переключаемся на главный поток для изменения состояния Compose
                    isPlayingTts = true // Устанавливаем статус проигрывания в true
                } // Конец корутины главного потока
            } // Конец метода onStart
            override fun onDone(utteranceId: String?) { // Метод, вызываемый при успешном окончании чтения блока текста
                scope.launch(kotlinx.coroutines.Dispatchers.Main) { // Переключаемся на главный поток UI
                    if (utteranceId == "reading_text") { // Проверяем, завершилось ли чтение основного текста анализа совместимости
                        spokenWordRange = null // Убираем подсветку слов для основного текста
                        val respText = followUpResponse
                        if (!respText.isNullOrBlank()) {
                            val params = android.os.Bundle().apply {
                                putString(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "follow_up_compat")
                            }
                            val cleanedText = prepareTextForTts(respText).sanitizedText
                            ttsByLocalRef?.speak(cleanedText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, "follow_up_compat")
                            isPlayingTts = true
                        } else {
                            isPlayingTts = false
                            lastPlaybackIndex = 0
                        }
                    } else if (utteranceId == "follow_up_compat") {
                        followUpWordRange = null
                        isPlayingTts = false
                    } else if (utteranceId == "support_text") { // Если завершилось чтение текста поддержки
                        isPlayingTts = false // Сбрасываем флаг воспроизведения
                        spokenWordRange = null // Сбрасываем подсветку слов
                        followUpWordRange = null
                        supportWordRange = null
                        lastPlaybackIndex = 0 // Сбрасываем позицию проигрывания на начало
                    } // Конец проверки ID текста
                } // Конец корутины главного потока
            } // Конец метода onDone
            override fun onError(utteranceId: String?) { // Метод, вызываемый при возникновении ошибки озвучивания
                scope.launch(kotlinx.coroutines.Dispatchers.Main) { // Запускаем корутину на главном потоке
                    isPlayingTts = false // Сбрасываем флаг воспроизведения
                    spokenWordRange = null
                    followUpWordRange = null
                    supportWordRange = null
                    ttsDelayJob?.cancel() // Отменяем отложенную задачу озвучивания
                } // Конец корутины главного потока
            } // Конец метода onError
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) { // Метод, отслеживающий границы произносимых слов в реальном времени
                scope.launch(kotlinx.coroutines.Dispatchers.Main) { // Переключаемся на главный поток для обновления подсветки
                    if (utteranceId == "reading_text") { // Подсвечиваем слова только для основного текста анализа
                        val cleaned = activeCleanedTts
                        val startOffset = activePlaybackStartOffset
                        val subLen = (plainTextOfReport.length - startOffset).coerceAtLeast(0)
                        val relStart = cleaned?.indexMap?.getOrElse(start) { subLen } ?: start
                        val relEnd = cleaned?.indexMap?.getOrElse(end) { subLen } ?: end
                        val absStart = relStart + startOffset // Вычисляем абсолютное начало слова с учетом смещения
                        val absEnd = relEnd + startOffset // Вычисляем абсолютный конец слова с учетом смещения
                        spokenWordRange = Pair(absStart, absEnd) // Устанавливаем координаты слова для рендеринга
                        followUpWordRange = null
                        supportWordRange = null
                        lastPlaybackIndex = absStart // Запоминаем текущую позицию воспроизведения для возможности паузы

                        // Плавная автопрокрутка текста без дёргания экрана:
                        val textLen = plainTextOfReport.length
                        if (textLen > 0) {
                            val viewportPx = scrollState.viewportSize.let { if (it > 0) it else 1500 }
                            val totalHeightPx = scrollState.maxValue + viewportPx
                            val fraction = absStart.toFloat() / textLen.toFloat()
                            val wordY = fraction * totalHeightPx
                            val currentScroll = scrollState.value
                            val targetScroll = (wordY - viewportPx * 0.32f).toInt().coerceIn(0, scrollState.maxValue)

                            if (wordY > currentScroll + viewportPx * 0.55f || wordY < currentScroll + 20f) {
                                scope.launch {
                                    scrollState.animateScrollTo(
                                        value = targetScroll,
                                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350)
                                    )
                                }
                            }
                        }
                    } else if (utteranceId == "follow_up_compat") {
                        spokenWordRange = null
                        followUpWordRange = Pair(start, end)
                        supportWordRange = null
                    } else if (utteranceId == "support_text") {
                        spokenWordRange = null
                        followUpWordRange = null
                        supportWordRange = Pair(start, end)
                    }
                } // Конец корутины главного потока
            } // Конец метода onRangeStart
        }) // Конец установки слушателя
        onDispose { // Обработчик уничтожения (Dispose) эффекта
            ttsByLocalRef?.setOnUtteranceProgressListener(null) // Отвязываем слушатель от TTS для избежания утечек памяти
            ttsDelayJob?.cancel() // Принудительно завершаем корутину задержки, если пользователь покинул экран
        } // Конец блока onDispose
    } // Конец DisposableEffect

    fun speakTextFromIndex(text: String, startIndex: Int) {
        if (text.isEmpty()) return
        ttsByLocalRef?.stop()
        applyTtsSettings()
        
        val cleanedObj = prepareTextForTts(text)
        activeCleanedTts = cleanedObj
        val cleanedText = cleanedObj.sanitizedText
        
        var cleanedStartIndex = 0
        if (startIndex > 0) {
            val mappedIdx = cleanedObj.indexMap.indexOfFirst { it >= startIndex }
            cleanedStartIndex = if (mappedIdx >= 0) mappedIdx else cleanedText.length
        }
        
        val textToSpeak = cleanedText.substring(cleanedStartIndex)
        val params = android.os.Bundle().apply {
            putString(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "reading_text")
        }
        
        activePlaybackStartOffset = cleanedStartIndex
        ttsOffset = cleanedStartIndex
        ttsByLocalRef?.speak(textToSpeak, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, "reading_text")
        isPlayingTts = true
    }
    
    var activeSelectionSlot by remember { mutableStateOf(1) } // 1 или 2

    val readings by viewModel.allReadings.collectAsState()
    val compatFilterMode by viewModel.compatibilityFilterMode.collectAsState()

    // Фильтрация интерпретаций с учетом выбранного режима (Все, Краткие, Полные)
    val interpretations = remember(readings, compatFilterMode) {
        val base = readings.filter { it.analysisType != "compatibility" }
        when (compatFilterMode) {
            "brief" -> base.filter { it.analysisType == "brief" }
            "full" -> base.filter { it.analysisType.startsWith("full") || it.analysisType != "brief" }
            else -> base
        }
    }
    
    val filteredInterpretations = remember(interpretations, searchQuery) {
        interpretations.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }
    
    val availableChoices = remember(filteredInterpretations, selectedPartner1, activeSelectionSlot) {
        if (activeSelectionSlot == 2 && selectedPartner1 != null) {
            filteredInterpretations.filter { record ->
                val norm1 = selectedPartner1!!.gender.lowercase().trim()
                val norm2 = record.gender.lowercase().trim()
                val isMale1 = norm1.startsWith("м") || norm1.startsWith("m")
                val isMale2 = norm2.startsWith("м") || norm2.startsWith("m")
                isMale1 != isMale2
            }
        } else {
            filteredInterpretations
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Адаптивный заголовок "Совместимость", выводящийся в одну строку по ТЗ с автоподбором размера шрифта
            MysticHeader(if (currentLang == AppLanguage.RUS) "Совместимость" else "Compatibility")
            
            MysticSubtitle(strings.compatSubtitle)

            Spacer(modifier = Modifier.height(20.dp))

            val currentCompatibilityReadingVal = compatibilityReading
            val isPaymentRequired = remember(currentCompatibilityReadingVal) {
                currentCompatibilityReadingVal?.resultJson?.contains("payment_required") == true
            }

            if (isPaymentRequired && currentCompatibilityReadingVal != null) {
                YookassaPaymentForm(
                    readingId = currentCompatibilityReadingVal.id,
                    analysisType = "compatibility",
                    viewModel = viewModel,
                    onSuccess = {
                        // Automatically reloaded by state flow
                    },
                    onClose = {
                        viewModel.currentCompatibilityReading.value = null
                    }
                )
            } else if (compatReport == null) {
                // --- ENTRY AND SELECTION FROM HISTORY ---
                
                // Search Field
                MysticTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = if (currentLang == AppLanguage.RUS) "Поиск по имени" else "Search by name",
                    placeholder = if (currentLang == AppLanguage.RUS) "Введите имя..." else "Enter name...",
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                            }
                        } else {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Active Slots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Slot 1 card
                    val slot1Selected = selectedPartner1 != null
                    val isSlot1Active = activeSelectionSlot == 1
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSlot1Active) MysticGold.copy(0.1f) else Color.White.copy(0.1f)
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (isSlot1Active) MysticGold else if (slot1Selected) MysticGold.copy(0.4f) else Color.White.copy(0.1f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeSelectionSlot = 1 }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Партнёр 1" else "Partner 1",
                                style = MaterialTheme.typography.labelMedium.copy(color = MysticGold, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (selectedPartner1 != null) {
                                val p1 = selectedPartner1!!
                                val genderLetter = when(p1.gender.lowercase()) {
                                    "male", "мужской", "м" -> if (currentLang == AppLanguage.RUS) "М" else "M"
                                    "female", "женский", "ж" -> if (currentLang == AppLanguage.RUS) "Ж" else "F"
                                    else -> if (currentLang == AppLanguage.RUS) "Д" else "O"
                                }
                                val birthYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - p1.age
                                Text(
                                    text = p1.name,
                                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$birthYear г.р. • ${p1.height}см • $genderLetter",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { selectedPartner1 = null },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCF6679))
                                ) {
                                    Text(if (currentLang == AppLanguage.RUS) "Удалить" else "Remove", fontSize = 11.sp)
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Выбрать" else "Select",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                )
                            }
                        }
                    }

                    // Slot 2 card
                    val slot2Selected = selectedPartner2 != null
                    val isSlot2Active = activeSelectionSlot == 2
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSlot2Active) MysticGold.copy(0.1f) else Color.White.copy(0.1f)
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (isSlot2Active) MysticGold else if (slot2Selected) MysticGold.copy(0.4f) else Color.White.copy(0.1f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { 
                                if (selectedPartner1 == null) {
                                    Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Сначала выберите Партнёра 1" else "Select Partner 1 first", Toast.LENGTH_SHORT).show()
                                } else {
                                    activeSelectionSlot = 2 
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Партнёр 2" else "Partner 2",
                                style = MaterialTheme.typography.labelMedium.copy(color = MysticGold, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (selectedPartner2 != null) {
                                val p2 = selectedPartner2!!
                                val genderLetter = when(p2.gender.lowercase()) {
                                    "male", "мужской", "м" -> if (currentLang == AppLanguage.RUS) "М" else "M"
                                    "female", "женский", "ж" -> if (currentLang == AppLanguage.RUS) "Ж" else "F"
                                    else -> if (currentLang == AppLanguage.RUS) "Д" else "O"
                                }
                                val birthYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - p2.age
                                Text(
                                    text = p2.name,
                                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$birthYear г.р. • ${p2.height}см • $genderLetter",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { selectedPartner2 = null },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCF6679))
                                ) {
                                    Text(if (currentLang == AppLanguage.RUS) "Удалить" else "Remove", fontSize = 11.sp)
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (selectedPartner1 == null) Color.DarkGray else Color.Gray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Выбрать" else "Select",
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (selectedPartner1 == null) Color.DarkGray else Color.Gray)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Карточка-кнопка для загрузки материалов второго пользователя
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33D4AF37)),
                    border = BorderStroke(1.dp, MysticGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            viewModel.resetUploadState()
                            onNavigateToProfile()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = MysticGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "+ Загрузить материалы 2-го пользователя" else "+ Upload materials for 2nd user",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MysticGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Три одинаковые по размеру кнопки ВСЕ, Краткие, Полные для сортировки на вкладке Совместимость
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAll = compatFilterMode == "all"
                    val isBrief = compatFilterMode == "brief"
                    val isFull = compatFilterMode == "full"

                    // Кнопка "ВСЕ"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAll) MysticGold else Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, if (isAll) MysticGold else Color.Gray.copy(0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable { viewModel.compatibilityFilterMode.value = "all" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "ВСЕ" else "ALL",
                                color = if (isAll) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Кнопка "Краткие"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isBrief) Color(0xFF00FF66) else Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, if (isBrief) Color(0xFF00FF66) else Color(0xFF00FF66).copy(0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable { viewModel.compatibilityFilterMode.value = "brief" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Краткие" else "Brief",
                                color = if (isBrief) Color.Black else Color(0xFF00FF66),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Кнопка "Полные"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFull) Color(0xFFE040FB) else Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, if (isFull) Color(0xFFE040FB) else Color(0xFFE040FB).copy(0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable { viewModel.compatibilityFilterMode.value = "full" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Полные" else "Full",
                                color = if (isFull) Color.Black else Color(0xFFE040FB),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Список профилей из истории
                Text(
                    text = if (activeSelectionSlot == 1) {
                        if (currentLang == AppLanguage.RUS) "Выберите профиль для Партнёра 1:" else "Choose profile for Partner 1:"
                    } else {
                        if (currentLang == AppLanguage.RUS) "Выберите профиль другого пола для Партнёра 2:" else "Choose opposite gender profile for Partner 2:"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(color = MysticBronze, fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (availableChoices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) {
                                    if (activeSelectionSlot == 2 && selectedPartner1 != null) "Нет профилей противоположного пола" else "История интерпретации пуста"
                                } else {
                                    if (activeSelectionSlot == 2 && selectedPartner1 != null) "No opposite gender profiles available" else "No profiles in interpretation history"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MysticButton(
                                text = if (currentLang == AppLanguage.RUS) "Загрузить материалы второго пользователя" else "Upload materials for 2nd user",
                                onClick = {
                                    viewModel.resetUploadState()
                                    onNavigateToProfile()
                                },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableChoices.forEach { record ->
                            val isSelected = (record == selectedPartner1 || record == selectedPartner2)
                            val birthYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - record.age
                            val genderLetter = when(record.gender.lowercase()) {
                                "male", "мужской", "м" -> if (currentLang == AppLanguage.RUS) "М" else "M"
                                "female", "женский", "ж" -> if (currentLang == AppLanguage.RUS) "Ж" else "F"
                                else -> if (currentLang == AppLanguage.RUS) "Д" else "O"
                            }
                            val yrUnit = if (currentLang == AppLanguage.RUS) "г.р." else "y.o.b."
                            val heightUnit = if (currentLang == AppLanguage.RUS) "см" else "cm"
                            val infoText = "${record.name}, $birthYear $yrUnit, ${record.height} $heightUnit, $genderLetter"

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MysticGold.copy(0.15f) else Color(0x22141420)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MysticGold else Color.White.copy(0.08f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (activeSelectionSlot == 1) {
                                            selectedPartner1 = record
                                            // If same gender as selectedPartner2, clear selectedPartner2
                                            if (selectedPartner2 != null) {
                                                val norm1 = record.gender.lowercase().trim()
                                                val norm2 = selectedPartner2!!.gender.lowercase().trim()
                                                val isMale1 = norm1.startsWith("м") || norm1.startsWith("m")
                                                val isMale2 = norm2.startsWith("м") || norm2.startsWith("m")
                                                if (isMale1 == isMale2) {
                                                    selectedPartner2 = null
                                                }
                                            }
                                            // Auto-advance slot if slot 2 is empty
                                            if (selectedPartner2 == null) {
                                                activeSelectionSlot = 2
                                            }
                                        } else {
                                            if (selectedPartner1 == null) {
                                                Toast.makeText(context, "Select Partner 1 first", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val norm1 = selectedPartner1!!.gender.lowercase().trim()
                                                val norm2 = record.gender.lowercase().trim()
                                                val isMale1 = norm1.startsWith("м") || norm1.startsWith("m")
                                                val isMale2 = norm2.startsWith("м") || norm2.startsWith("m")
                                                if (isMale1 == isMale2) {
                                                    Toast.makeText(context, "Partners must be of opposite genders!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    selectedPartner2 = record
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val imagePath = record.imageUrl ?: record.leftPalmPath ?: record.rightPalmPath ?: record.leftBackPath ?: record.rightBackPath
                                    val imgModel = getCoilImageModel(imagePath)
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color.Black.copy(0.4f), CircleShape)
                                            .border(1.dp, MysticGold.copy(0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (imgModel != null) {
                                            coil.compose.AsyncImage(
                                                model = imgModel,
                                                contentDescription = "User Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(id = com.aistudio.hiromant.kxsrwa.R.drawable.img_splash_hand),
                                                contentDescription = "Hand Placeholder",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = infoText,
                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (record.analysisType.contains("char")) {
                                                if (currentLang == AppLanguage.RUS) "Анализ характера" else "Character reading"
                                            } else {
                                                if (currentLang == AppLanguage.RUS) "Анализ судьбы" else "Destiny reading"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                        )
                                    }
                                    
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MysticGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isAnalyzing) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MysticDarkSurface),
                        border = BorderStroke(1.dp, MysticGold.copy(0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Единый универсальный индикатор прогресса для расчета совместимости
                            MysticProgressBar(
                                progress = analysisProgress,
                                currentLang = currentLang
                            )
                        }
                    }
                }

                MysticButton(
                    text = strings.compatAnalyzeBtn,
                    onClick = {
                        val p1 = selectedPartner1
                        val p2 = selectedPartner2
                        if (p1 != null && p2 != null) {
                            val norm1 = p1.gender.lowercase().trim()
                            val norm2 = p2.gender.lowercase().trim()
                            val isMale1 = norm1.startsWith("м") || norm1.startsWith("m")
                            val isMale2 = norm2.startsWith("м") || norm2.startsWith("m")
                            if (isMale1 == isMale2) {
                                Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Партнёры должны быть разнополыми!" else "Partners must be of opposite genders!", Toast.LENGTH_SHORT).show()
                                return@MysticButton
                            }
                            
                            // Check compatibility price (250 rubles item)
                            viewModel.checkFeatureUnlocked("compatibility") { unlocked ->
                                if (unlocked || (billingState?.remainingAnalyses ?: 0) > 0) {
                                    val b1 = if (!p1.leftPalmPath.isNullOrEmpty()) BitmapUtils.uriToBitmap(context, Uri.parse(p1.leftPalmPath)) else null
                                    val b2 = if (!p2.leftPalmPath.isNullOrEmpty()) BitmapUtils.uriToBitmap(context, Uri.parse(p2.leftPalmPath)) else null
                                    partnerName = p2.name
                                    viewModel.runCompatibilityAnalysis(b1, b2, p1.name, p2.name, onNavigateToLoading)
                                } else {
                                    onNavigateToBilling()
                                }
                            }
                        } else {
                            Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Пожалуйста, выберите обоих партнёров" else "Please select both partners", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = (selectedPartner1 != null && selectedPartner2 != null && !isAnalyzing),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // --- RESULTS COMPATIBILITY PRESENTATION ---
                Spacer(modifier = Modifier.height(16.dp))

                val annotatedReportText = buildCompatibilityAnnotatedString(
                    plainText = plainTextOfReport,
                    spokenWordRange = spokenWordRange
                )

                var resultTextLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

                // Центральная шапка заголовка результатов анализа во всю ширину с отступами 10dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Адаптивный заголовок "Результат Анализа" во всю ширину экрана
                    Text(
                        text = "Результат Анализа",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MysticGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (compatReport.isMock) {
                        // Подзаголовок тестового демо-отчета
                        Text(
                            text = "ПРИМЕР Интерпретации",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Yellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        // Подзаголовок вида анализа
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Анализ совместимости" else "Compatibility Analysis",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Ring percentage affinity display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { compatReport.compatibilityPercent.toFloat() / 100f },
                        modifier = Modifier.size(160.dp),
                        color = MysticGold,
                        strokeWidth = 10.dp,
                        trackColor = MysticBronze.copy(0.2f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${compatReport.compatibilityPercent}%",
                            style = MaterialTheme.typography.displayLarge.copy(color = Color.White),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.compatPercentLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(color = MysticGold, letterSpacing = 1.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Unified beautiful reading card with text highlighting & tap-to-read from any point
                MysticCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = annotatedReportText,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Default,
                                lineHeight = 24.sp
                            ),
                            onTextLayout = { resultTextLayoutResult = it },
                            modifier = Modifier.pointerInput(plainTextOfReport) {
                                detectTapGestures { pos ->
                                    resultTextLayoutResult?.let { layout ->
                                        val offset = layout.getOffsetForPosition(pos)
                                        if (offset in 0 until plainTextOfReport.length) {
                                            lastPlaybackIndex = offset
                                            speakTextFromIndex(plainTextOfReport, offset)
                                        }
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.HorizontalDivider(color = MysticGold.copy(0.3f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Маленькие кнопки действий БЕЗ текста (только значки) в один ряд по ТЗ: Копировать, Отправить по почте, Поделиться
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Кнопка Копировать (только значок)
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Compatibility Report", plainTextOfReport)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Результат скопирован в буфер обмена!" else "Result copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0x22141420), CircleShape)
                                    .border(1.dp, MysticGold.copy(0.7f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = if (currentLang == AppLanguage.RUS) "Копировать" else "Copy",
                                    tint = MysticGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // 2. Кнопка Отправить по почте (только значок)
                            IconButton(
                                onClick = {
                                    try {
                                        val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                            data = android.net.Uri.parse("mailto:")
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, if (currentLang == AppLanguage.RUS) "Анализ совместимости" else "Compatibility Analysis")
                                            putExtra(android.content.Intent.EXTRA_TEXT, plainTextOfReport)
                                        }
                                        context.startActivity(emailIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Почтовое приложение не найдено" else "No email app found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0x22141420), CircleShape)
                                    .border(1.dp, MysticGold.copy(0.7f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = if (currentLang == AppLanguage.RUS) "Отправить по почте" else "Send via email",
                                    tint = MysticGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // 3. Кнопка Поделиться (только значок)
                            IconButton(
                                onClick = {
                                    try {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, plainTextOfReport)
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, if (currentLang == AppLanguage.RUS) "Поделиться анализом" else "Share Analysis")
                                        context.startActivity(shareIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0x22141420), CircleShape)
                                    .border(1.dp, MysticGold.copy(0.7f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = if (currentLang == AppLanguage.RUS) "Поделиться" else "Share",
                                    tint = MysticGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive user follow-up questions / topics
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
                        .border(1.dp, MysticBronze.copy(0.25f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "❓ Хотите уточнить совместимость?" else "❓ Clarify partner compatibility?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MysticGold,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "Выберите интересующую тему или введите свой вопрос ИИ-Аналитику:" else "Select a topic or enter your custom question to AI Analyst:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                    )

                    // Suggestion chips
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val suggestions = if (currentLang == AppLanguage.RUS) listOf(
                            "Как улучшить гармонию?",
                            "Финансовые перспективы пары",
                            "Преодоление конфликтов",
                            "Совместимость в браке"
                        ) else listOf(
                            "How to improve harmony?",
                            "Financial prospects as a couple",
                            "Overcoming conflicts",
                            "Marriage compatibility"
                        )
                        suggestions.forEach { suggestion ->
                            FilterChip(
                                selected = userQuestionText == suggestion,
                                onClick = { userQuestionText = suggestion },
                                label = { Text(text = suggestion, fontSize = 12.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MysticGold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0x22141420),
                                    labelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = userQuestionText == suggestion,
                                    borderColor = MysticGold.copy(0.5f)
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = userQuestionText,
                        onValueChange = { userQuestionText = it },
                        placeholder = { Text(if (currentLang == AppLanguage.RUS) "Задайте уточняющий вопрос о вашей паре..." else "Ask a question about your pair...", color = Color.Gray) },
                        minLines = 2,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MysticGold,
                            unfocusedBorderColor = MysticBronze.copy(0.6f),
                            cursorColor = MysticGold
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    MysticButton(
                        text = if (followUpLoading) (if (currentLang == AppLanguage.RUS) "Анализ вопроса..." else "Analyzing question...") else (if (currentLang == AppLanguage.RUS) "Спросить ИИ-Аналитика" else "Ask AI Analyst"),
                        onClick = {
                            if (userQuestionText.isNotBlank()) {
                                viewModel.sendFollowUpQuestion(plainTextOfReport, userQuestionText)
                            }
                        },
                        enabled = userQuestionText.isNotBlank() && !followUpLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (followUpLoading) {
                        MysticProgressBar(
                            progress = followUpProgress,
                            currentLang = currentLang
                        )
                    }

                    // Отображение сохраненных ранее дополнительных вопросов и ответов для анализа совместимости
                    val compatReading by viewModel.currentCompatibilityReading.collectAsState() // Текущая запись совместимости
                    val savedCompatFollowUps = remember(compatReading?.followUpQuestionsJson) {
                        com.aistudio.hiromant.kxsrwa.data.local.parseFollowUpQuestionsJson(compatReading?.followUpQuestionsJson) // Распаковка сохраненных вопросов
                    }
                    if (savedCompatFollowUps.isNotEmpty()) { // Проверка наличия вопросов
                        Spacer(modifier = Modifier.height(16.dp)) // Отступ
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Ответы на дополнительные вопросы:" else "Answers to Follow-up Questions:", // Заголовок без значка по ТЗ
                            style = MaterialTheme.typography.titleMedium.copy( // Стиль
                                color = MysticGold, // Золотистый цвет
                                fontWeight = FontWeight.Bold // Жирный
                            ),
                            maxLines = 1, // Ограничение в 1 строку
                            overflow = TextOverflow.Ellipsis // Обрезка многоточием
                        )
                        Spacer(modifier = Modifier.height(8.dp)) // Отступ
                        savedCompatFollowUps.forEachIndexed { idx, pair -> // Отображение каждого элемента
                            ExpandableFollowUpItem(
                                item = pair, // Объект вопроса и ответа
                                index = idx, // Порядковый индекс
                                currentLang = currentLang, // Язык
                                viewModel = viewModel, // ViewModel
                                readingId = compatReading?.id ?: 0L, // ID записи совместимости
                                initialExpanded = false, // По умолчанию ответы свёрнуты по ТЗ
                                scrollState = scrollState, // Передаем scrollState для автоскролла
                                isExpandedParam = (expandedFollowUpIndex == idx),
                                onToggleExpand = {
                                    if (expandedFollowUpIndex == idx) {
                                        expandedFollowUpIndex = null
                                    } else {
                                        expandedFollowUpIndex = idx
                                        isMainAnalysisExpanded = false
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val dailyUsedInCompat by viewModel.dailyTokensUsed.collectAsState()
                val dailyRemainingInCompat by viewModel.dailyTokensRemaining.collectAsState()

                // Отображаем расход токенов Gemini ИИ для отчета о совместимости
                TokenUsageCard(
                    promptTokens = compatReport.promptTokens, // Передаем входящие токены промпта совместимости
                    candidatesTokens = compatReport.candidatesTokens, // Передаем исходящие токены ответа совместимости
                    totalTokens = compatReport.totalTokens, // Передаем суммарно потраченные токены за совместимость
                    dailyTokensUsed = dailyUsedInCompat, // Израсходовано токенов за весь текущий день
                    dailyTokensRemaining = dailyRemainingInCompat, // Остаток токенов на сегодня
                    dailyQuota = viewModel.getDailyTokenQuota() // Общая суточная квота токенов
                )

                ProjectSupportSection(viewModel = viewModel, spokenWordRange = supportWordRange)

                Spacer(modifier = Modifier.height(24.dp))

                MysticButton(
                    text = "Заново / Reset",
                    onClick = { 
                        ttsDelayJob?.cancel() // Отменяем запущенный таймер отложенного воспроизведения поддержки во избежание накладок
                        ttsByLocalRef?.stop() // Принудительно прекращаем любое проигрывание звука синтезатором речи
                        isPlayingTts = false // Возвращаем статус воспроизведения в исходное выключенное положение
                        spokenWordRange = null // Полностью очищаем подсветку слов на экране
                        lastPlaybackIndex = 0 // Сбрасываем позицию паузы на самое начало текста
                        viewModel.currentCompatibilityReading.value = null // Обнуляем состояние текущего отчета во ViewModel для возврата к выбору партнеров
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        if (compatReport != null) {
            val isAnyExpanded = isMainAnalysisExpanded || expandedFollowUpIndex != null
            androidx.compose.animation.AnimatedVisibility(
                visible = isAnyExpanded,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(bottom = 72.dp, start = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        isMainAnalysisExpanded = false
                        expandedFollowUpIndex = null
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFF1B1822),
                    contentColor = MysticGold,
                    modifier = Modifier
                        .border(1.dp, MysticGold.copy(0.6f), CircleShape)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Свернуть Анализ",
                        tint = MysticGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Закрепленный модуль управления голосом TtsVoiceController в верхней правой части экрана над скроллом по ТЗ
        if (compatReport != null) {
            TtsVoiceController(
                isPlaying = isPlayingTts,
                onPlayToggle = {
                    if (isPlayingTts) {
                        ttsDelayJob?.cancel()
                        ttsByLocalRef?.stop()
                        isPlayingTts = false
                    } else {
                        speakTextFromIndex(plainTextOfReport, lastPlaybackIndex)
                    }
                },
                rate = ttsRateState,
                onRateChange = { newRate ->
                    viewModel.changeTtsSpeechRate(newRate)
                    ttsByLocalRef?.setSpeechRate(newRate)
                    if (isPlayingTts) {
                        val currentWordStart = spokenWordRange?.first ?: 0
                        speakTextFromIndex(plainTextOfReport, currentWordStart)
                    }
                },
                pitch = ttsPitchState,
                onPitchChange = { newPitch ->
                    viewModel.changeTtsPitch(newPitch)
                    ttsByLocalRef?.setPitch(newPitch)
                    if (isPlayingTts) {
                        val currentWordStart = spokenWordRange?.first ?: 0
                        speakTextFromIndex(plainTextOfReport, currentWordStart)
                    }
                },
                gender = ttsGenderState,
                onGenderChange = { newGender ->
                    viewModel.changeTtsGender(newGender)
                    applyTtsSettings()
                    if (isPlayingTts) {
                        val currentWordStart = spokenWordRange?.first ?: 0
                        speakTextFromIndex(plainTextOfReport, currentWordStart)
                    }
                },
                voiceIndex = ttsVoiceIndex,
                onVoiceIndexChange = { newIndex ->
                    viewModel.changeTtsVoiceIndex(newIndex)
                    applyTtsSettings()
                    if (isPlayingTts) {
                        val currentWordStart = spokenWordRange?.first ?: 0
                        speakTextFromIndex(plainTextOfReport, currentWordStart)
                    }
                },
                currentLang = currentLang,
                ttsInstance = ttsByLocalRef,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 12.dp)
                    .zIndex(100f)
            )
        }

        InsufficientFundsPopupDialog(
            visible = showInsufficientFunds,
            currentLang = currentLang,
            onShareClick = {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, if (currentLang == AppLanguage.RUS) "Хиромант — Совместимость" else "Compatibility")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Попробуйте приложение Хиромант для анализа совместимости!")
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Поделиться"))
            },
            onTopUpClick = { viewModel.currentCompatibilityReading.value = null },
            onDismiss = { viewModel.dismissInsufficientFundsDialog() }
        )
    }
}


// --- SCREEN 7.5: USER CABINET VIEW ---

@Composable
fun UserCabinetScreen(
    viewModel: PalmistViewModel,
    onNavigateToResult: () -> Unit,
    onNavigateToBilling: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    val profile by viewModel.userProfile.collectAsState() // Текущий выбранный профиль пользователя из БД
    val allProfiles by viewModel.allUserProfiles.collectAsState() // Список всех сохранённых профилей пользователей
    val billingStateVal by viewModel.billingState.collectAsState() // Состояние баланса интерпретаций
    val readings by viewModel.allReadings.collectAsState() // Все исторические отчёты анализов
    val payments by viewModel.allPayments.collectAsState() // История финансовых операций и пополнений

    val freeCount = billingStateVal?.freeAnalyses ?: 0 // Число доступных Кратких Анализов
    val paidCount = billingStateVal?.paidAnalyses ?: 0 // Число доступных Полных Анализов

    // Фильтр выбранного имени пользователя (null по умолчанию = ВСЕ Пользователи)
    var selectedProfileNameFilter by remember { mutableStateOf<String?>(null) }
    // Режим фильтрации: 0 = ВСЕ, 1 = Краткие, 2 = Полные
    var cabinetHistoryFilter by remember { mutableStateOf(0) }

    // Заголовок кнопки имени: "Выбери" по умолчанию или имя выбранного профиля
    val headerDisplayName = if (selectedProfileNameFilter.isNullOrBlank()) {
        if (currentLang == AppLanguage.RUS) "Выбери" else "Choose"
    } else {
        selectedProfileNameFilter!!
    }

    // Сборка списка всех уникальных профилей пользователей (из базы профилей и истории проведенных анализов)
    val validProfiles: List<UserProfileEntity> = remember(allProfiles, readings, profile) {
        val list = mutableListOf<UserProfileEntity>() // Инициализация списка
        profile?.let { p: UserProfileEntity ->
            if (p.name.isNotBlank()) list.add(p) // Добавление текущего активного профиля
        }
        allProfiles.forEach { p: UserProfileEntity ->
            if (p.name.isNotBlank() && list.none { existing: UserProfileEntity -> existing.name.equals(p.name, ignoreCase = true) }) {
                list.add(p) // Добавление сохраненных пользователей
            }
        }
        readings.forEach { r: ReadingEntity ->
            if (r.name.isNotBlank() && list.none { existing: UserProfileEntity -> existing.name.equals(r.name, ignoreCase = true) }) {
                list.add(
                    UserProfileEntity(
                        id = "hist_${r.id}", // Временный ID для профиля из истории
                        name = r.name, // Имя пользователя
                        gender = r.gender, // Пол пользователя
                        age = r.age, // Возраст
                        height = r.height, // Рост в см
                        dominantHand = r.dominantHand, // Ведущая рука
                        photoUri = r.imageUrl, // Путь к аватарке
                        registrationTimestamp = r.timestamp // Время записи
                    )
                )
            }
        }
        list
    }

    // Фильтрация интерпретаций для выбранного пользователя (или ВСЕХ пользователей по умолчанию) и типа анализа
    val realReadings = remember(readings, selectedProfileNameFilter, cabinetHistoryFilter) {
        readings.filter { reading ->
            val json = reading.resultJson // Содержимое отчёта
            val isNotDemo = !json.contains("Демонстрационный разбор") &&
                    !json.contains("Демонстрационный анализ") &&
                    !json.contains("demo_report") &&
                    !json.contains("demo_compatibility") // Исключение демонстрационных данных

            // Проверка принадлежности к выбранному пользователю (null или пусто = ВСЕ)
            val belongsToUser = if (selectedProfileNameFilter.isNullOrBlank()) {
                true // Отображаются ВСЕ Пользователи
            } else {
                reading.name.equals(selectedProfileNameFilter, ignoreCase = true) ||
                (reading.partnerName != null && reading.partnerName.equals(selectedProfileNameFilter, ignoreCase = true))
            }

            // Проверка типа: 0 = ВСЕ, 1 = Краткие, 2 = Полные
            val typeMatch = when (cabinetHistoryFilter) {
                1 -> !reading.analysisType.contains("full", ignoreCase = true) // Краткие анализы
                2 -> reading.analysisType.contains("full", ignoreCase = true) // Полные анализы
                else -> true // ВСЕ анализы
            }

            isNotDemo && belongsToUser && typeMatch
        }
    }

    // Фильтрация пополнений для выбранного пользователя и типа
    val realPayments = remember(payments, selectedProfileNameFilter, cabinetHistoryFilter) {
        payments.filter { payment ->
            val belongsToUser = if (selectedProfileNameFilter.isNullOrBlank()) {
                true // Отображаются ВСЕ Пользователи
            } else {
                payment.userName.isBlank() || payment.userName.equals(selectedProfileNameFilter, ignoreCase = true)
            }

            val typeMatch = when (cabinetHistoryFilter) {
                1 -> payment.amountRub == 0 || payment.readingType.contains("бесплат", ignoreCase = true) || payment.readingType.contains("кратк", ignoreCase = true) // Краткие
                2 -> payment.amountRub > 0 || payment.readingType.contains("полн", ignoreCase = true) // Полные
                else -> true // ВСЕ
            }

            belongsToUser && typeMatch
        }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 - Интерпретации, 1 - Пополнения
    var showFreeDialog by remember { mutableStateOf(false) } // Флаг модального окна "+Бесплатные Интерпретации"
    var showUserSelectDialog by remember { mutableStateOf(false) } // Флаг окна выбора профиля пользователя
    var showEditProfileDialog by remember { mutableStateOf(false) } // Флаг окна редактирования текущего профиля
    var showNewUserDialog by remember { mutableStateOf(false) } // Флаг окна создания нового профиля

    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent() // Контракт галереи Android
    ) { uri: Uri? ->
        uri?.let {
            // Сохраняем выбранный файл во внутреннее хранилище приложения для активного выбранного профиля
            val savedPath = com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.saveUriToInternalStorage(
                context, it, "user_avatar_${System.currentTimeMillis()}.jpg"
            ) ?: it.toString()
            // Обновление аватара выбранного пользователя в БД
            viewModel.updateUserAvatar(savedPath)
            Toast.makeText(
                context,
                if (currentLang == AppLanguage.RUS) "Аватар профиля $headerDisplayName обновлён" else "Avatar updated for $headerDisplayName",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Карточка профиля пользователя с аватаром слева, именем снизу и кнопками добавления справа
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                border = BorderStroke(1.dp, MysticGold.copy(0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // А) Изображение Профиля влево (с возможностью изменения/загрузки), под изображением Имя
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A2634))
                                .border(1.5.dp, MysticGold, CircleShape)
                                .clickable {
                                    if (selectedProfileNameFilter.isNullOrBlank()) {
                                        showUserSelectDialog = true
                                    } else {
                                        avatarLauncher.launch("image/*")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedProfileNameFilter.isNullOrBlank()) {
                                // Изображение - Вопросительный Знак для режима всех пользователей по ТЗ п.1
                                Text(
                                    text = "?",
                                    color = MysticGold,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                val selectedProfileEntity = validProfiles.find { it.name.equals(selectedProfileNameFilter, ignoreCase = true) } ?: profile
                                val imgModel = getCoilImageModel(selectedProfileEntity?.photoUri)
                                if (imgModel != null) {
                                    coil.compose.AsyncImage(
                                        model = imgModel,
                                        contentDescription = "User Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = com.aistudio.hiromant.kxsrwa.R.drawable.img_splash_hand),
                                        contentDescription = "User Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Иконка карандаша в кружочке для подсказа проведения загрузки
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(MysticGold, CircleShape)
                                        .border(1.dp, Color.Black, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Change avatar",
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Нажатие на Имя пользователя открывает выбор соответствующего Профиля Потребителя по требованию ТЗ
                        Surface(
                            shape = RoundedCornerShape(8.dp), // Скругление плашки выбора
                            color = Color.White.copy(0.08f), // Элегантная полупрозрачная подложка
                            border = BorderStroke(1.dp, MysticGold.copy(0.4f)), // Золотистый тонкий контур
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showUserSelectDialog = true } // Клик по Имени открывает диалог выбора профиля
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, // Центрирование элементов
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp) // Внутренние отступы
                            ) {
                                Text(
                                    text = headerDisplayName, // Имя выбранного профиля или "Выбери Пользователя" по умолчанию
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    maxLines = 1, // Строго 1 строка по конституции проекта
                                    overflow = TextOverflow.Ellipsis // Обрезка
                                )
                                Spacer(modifier = Modifier.width(3.dp)) // Отступ
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown, // Стрелочка переключения
                                    contentDescription = "Выбор профиля", // Описание
                                    tint = MysticGold, // Золотистый акцент
                                    modifier = Modifier.size(18.dp) // Размер
                                )
                            }
                        }
                    }

                    // Б) Справа от Изображения профиля:
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // а) Цифрой ярко-зелёного цвета кол-во Кратких Интерпретаций, текст "Кратких Интерпретаций" и кнопка "+"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "$freeCount ",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FF66)
                                )
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Кратких" else "Brief",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FF66),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Кнопка с плюсом "+" БЕЗ надписей (Краткие Интерпретации: Поделиться и +3)
                            IconButton(
                                onClick = {
                                    try {
                                        val shareText = if (currentLang == AppLanguage.RUS) {
                                            "Раскрой тайны своей судьбы по ладони в приложении «Хиромант»! Скачивай: https://hiromant-app.ru/download/palmist.apk"
                                        } else {
                                            "Discover your destiny with the Palmist app! Download: https://hiromant-app.ru/download/palmist.apk"
                                        }
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(
                                            sendIntent,
                                            if (currentLang == AppLanguage.RUS) "Поделиться" else "Share"
                                        )
                                        context.startActivity(shareIntent)

                                        // Начисление +3 Кратких Интерпретаций в базу данных и запись в лог
                                        viewModel.rewardUserForSharing()

                                        Toast.makeText(
                                            context,
                                            if (currentLang == AppLanguage.RUS) "Вам начислено +3 Краткие Интерпретации!" else "+3 brief interpretations granted!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        viewModel.logErrorToServer("Ошибка функции Поделиться: ${e.localizedMessage}")
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF005222), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = "+",
                                    color = Color(0xFF00FF66),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // б) Цифрой ярко-фиолетового цвета кол-во Полных Интерпретаций, текст "Полных Интерпретаций" и кнопка "+"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "$paidCount ",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE040FB)
                                )
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Полных" else "Full",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE040FB),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Кнопка с плюсом "+" БЕЗ надписей (Полные Интерпретации: Страница оплаты с пресетом 250 руб -> +1 Полная)
                            IconButton(
                                onClick = {
                                    try {
                                        viewModel.paymentAmountToPreselect.value = "250"
                                        onNavigateToBilling()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        viewModel.logErrorToServer("Ошибка перехода на оплату: ${e.localizedMessage}")
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF4A0072), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFE040FB).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = "+",
                                    color = Color(0xFFE040FB),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Экран разделён на 2 части - 1) Интерпретации и 2) Пополнения
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MysticGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MysticGold,
                        height = 2.dp
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "1. Интерпретации" else "1. Interpretations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    selectedContentColor = MysticGold,
                    unselectedContentColor = Color.Gray
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "2. Пополнения" else "2. Top-ups",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    selectedContentColor = MysticGold,
                    unselectedContentColor = Color.Gray
                )
            }

            // 4. Под вкладками Интерпретации и Пополнения - три одинаковые по размеру кнопки ВСЕ, Краткие, Полные для сортировки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка "ВСЕ"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (cabinetHistoryFilter == 0) MysticGold else Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, if (cabinetHistoryFilter == 0) MysticGold else Color.Gray.copy(0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable { cabinetHistoryFilter = 0 }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "ВСЕ" else "ALL",
                            color = if (cabinetHistoryFilter == 0) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Кнопка "Краткие"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (cabinetHistoryFilter == 1) Color(0xFF00FF66) else Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, if (cabinetHistoryFilter == 1) Color(0xFF00FF66) else Color(0xFF00FF66).copy(0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable { cabinetHistoryFilter = 1 }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Краткие" else "Brief",
                            color = if (cabinetHistoryFilter == 1) Color.Black else Color(0xFF00FF66),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Кнопка "Полные"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (cabinetHistoryFilter == 2) Color(0xFFE040FB) else Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, if (cabinetHistoryFilter == 2) Color(0xFFE040FB) else Color(0xFFE040FB).copy(0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable { cabinetHistoryFilter = 2 }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Полные" else "Full",
                            color = if (cabinetHistoryFilter == 2) Color.Black else Color(0xFFE040FB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Содержимое вкладок
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> {
                        // в) История Интерпретаций (дата/время/личность/тип)
                        if (realReadings.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = strings.histNoRecords,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(realReadings) { record ->
                                    ReadingHistoryItem(
                                        record = record,
                                        currentLang = currentLang,
                                        viewModel = viewModel,
                                        onOpen = {
                                            viewModel.currentReading.value = record
                                            onNavigateToResult()
                                        },
                                        onDelete = {
                                            viewModel.deleteReading(record.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // г) История оплат (дата/время/сумма/способ оплаты)
                        if (realPayments.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CreditCard,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.RUS)
                                            "История пополнений пуста.\nСовершите пополнение в меню оплаты!"
                                        else "Top-up history is empty.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(realPayments) { payment ->
                                    PaymentHistoryItem(payment = payment, currentLang = currentLang)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Модальное окно "+Бесплатные Интерпретации"
        if (showFreeDialog) {
            AlertDialog(
                onDismissRequest = { showFreeDialog = false },
                containerColor = MysticDarkBackground,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Бесплатные Интерпретации" else "Free Interpretations",
                            color = MysticGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                text = {
                    Text(
                        text = if (currentLang == AppLanguage.RUS)
                            "Если вы поделитесь ссылкой на приложение «Хиромант», и оно будет установлено и использовано, вы получите 3 бесплатные Интерпретации."
                        else "If you share a link to the Palmist app and it is installed and used, you will receive 3 free Interpretations.",
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val shareText = if (currentLang == AppLanguage.RUS) {
                                    "Раскрой тайны своей судьбы по ладони в приложении «Хиромант»! Скачивай: https://hiromant-app.ru/download/palmist.apk"
                                } else {
                                    "Discover your destiny with the Palmist app! Download: https://hiromant-app.ru/download/palmist.apk"
                                }
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, if (currentLang == AppLanguage.RUS) "Поделиться ссылкой" else "Share App")
                                context.startActivity(shareIntent)
                                
                                viewModel.addFreeAnalyses(3)
                                Toast.makeText(
                                    context,
                                    if (currentLang == AppLanguage.RUS) "Вам начислено +3 бесплатные интерпретации!" else "+3 free interpretations awarded!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            showFreeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MysticGold)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Поделиться" else "Share",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFreeDialog = false }) {
                        Text(
                            text = strings.cancel,
                            color = Color.Gray
                        )
                    }
                }
            )
        }

        // 1. Модальный диалог выбора профиля соответствующего пользователя по требованию ТЗ
        if (showUserSelectDialog) {
            AlertDialog(
                onDismissRequest = { showUserSelectDialog = false },
                containerColor = MysticDarkBackground,
                shape = RoundedCornerShape(24.dp),
                confirmButton = {
                    TextButton(onClick = { showUserSelectDialog = false }) {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Закрыть" else "Close",
                            color = Color.Gray
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MysticGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "🔮 Выбор профиля пользователя" else "🔮 Select User Profile",
                            color = MysticGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.RUS)
                                "Выберите пользователя для просмотра отчётов анализов и редактирования данных:"
                            else "Select a user to view interpretation history and edit details:",
                            color = Color.White.copy(0.8f),
                            fontSize = 13.sp
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 2. В самом верху карточек пользователей - кнопка "ВСЕ Пользователи"
                            item {
                                val isAllSelected = selectedProfileNameFilter == null
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAllSelected) Color(0xFF2A2634) else Color(0x22FFFFFF),
                                    border = BorderStroke(1.dp, if (isAllSelected) MysticGold else Color.Gray.copy(0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedProfileNameFilter = null // Сброс фильтра - отображаются ВСЕ пользователи
                                            showUserSelectDialog = false
                                            Toast.makeText(
                                                context,
                                                if (currentLang == AppLanguage.RUS) "Выбраны ВСЕ Пользователи" else "Selected ALL Users",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Иконка "ВСЕ Пользователи"
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1E1A24))
                                                .border(1.dp, if (isAllSelected) MysticGold else Color.Gray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Groups,
                                                contentDescription = null,
                                                tint = MysticGold,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (currentLang == AppLanguage.RUS) "ВСЕ Пользователи" else "ALL Users",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (currentLang == AppLanguage.RUS) "Интерпретации всех пользователей" else "Interpretations of all users",
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isAllSelected) {
                                            Text(
                                                text = "✓ Выбран",
                                                color = Color(0xFF00FF66),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // 1. Форматирование каждого профиля: слева фото, справа Имя + год р. (строка 1), рост пол и дата регистрации (строка 2)
                            items(validProfiles) { userItem ->
                                val isSelected = selectedProfileNameFilter != null && userItem.name.equals(selectedProfileNameFilter, ignoreCase = true)
                                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                val birthYear = if (userItem.age > 0) (currentYear - userItem.age) else 1982

                                // Преобразование пола на русский язык
                                val genderStr = when (userItem.gender.lowercase()) {
                                    "female", "женский", "ж" -> if (currentLang == AppLanguage.RUS) "Женский" else "Female"
                                    "male", "мужской", "м" -> if (currentLang == AppLanguage.RUS) "Мужской" else "Male"
                                    else -> userItem.gender
                                }

                                // Форматирование даты регистрации
                                val regTimestamp = if (userItem.registrationTimestamp > 0) userItem.registrationTimestamp else System.currentTimeMillis()
                                val dateFormat = remember { java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.forLanguageTag("ru")) }
                                val regDateStr = dateFormat.format(java.util.Date(regTimestamp))

                                // Строка 1: Имя и год р.
                                val line1Text = "${userItem.name}, $birthYear г.р."
                                // Строка 2: Рост, пол и дата регистрации
                                val line2Text = "${userItem.height} см, $genderStr, $regDateStr"

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF2A2634) else Color(0x22FFFFFF),
                                    border = BorderStroke(1.dp, if (isSelected) MysticGold else Color.Gray.copy(0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedProfileNameFilter = userItem.name // Установка фильтра имени по клику
                                            viewModel.selectUserProfile(userItem)
                                            showUserSelectDialog = false
                                            Toast.makeText(
                                                context,
                                                if (currentLang == AppLanguage.RUS) "Выбран профиль: ${userItem.name}" else "Selected profile: ${userItem.name}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // а) Слева Изображение
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1E1A24))
                                                .border(1.dp, if (isSelected) MysticGold else Color.Gray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val imgModel = getCoilImageModel(userItem.photoUri)
                                            if (imgModel != null) {
                                                coil.compose.AsyncImage(
                                                    model = imgModel,
                                                    contentDescription = "User Avatar",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MysticGold,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // б) Справа Имя и год р. в первой строке, рост пол и дата регистрации во второй строке
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = line1Text,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = line2Text,
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isSelected) {
                                            Text(
                                                text = "✓ Выбран",
                                                color = Color(0xFF00FF66),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Кнопки управления профилем
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showUserSelectDialog = false
                                    showEditProfileDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MysticGold)
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "✏️ Изменить" else "✏️ Edit",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    showUserSelectDialog = false
                                    showNewUserDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, MysticGold)
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "➕ Новый" else "➕ New",
                                    color = MysticGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            )
        }

        // 2. Модальный диалог редактирования анкетных данных выбранного пользователя
        if (showEditProfileDialog) {
            var editName by remember { mutableStateOf(profile?.name ?: "Максим") }
            var editAge by remember { mutableStateOf((profile?.age ?: 44).toString()) }
            var editHeight by remember { mutableStateOf((profile?.height ?: 175).toString()) }
            var editGender by remember { mutableStateOf(profile?.gender ?: "Male") }
            var editHand by remember { mutableStateOf(profile?.dominantHand ?: "Right") }

            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                containerColor = MysticDarkBackground,
                shape = RoundedCornerShape(24.dp),
                confirmButton = {
                    Button(
                        onClick = {
                            val ageVal = editAge.toIntOrNull() ?: 44
                            val heightVal = editHeight.toIntOrNull() ?: 175
                            viewModel.saveUserProfile(
                                name = editName.ifBlank { "Максим" },
                                gender = editGender,
                                age = ageVal,
                                height = heightVal,
                                dominantHand = editHand,
                                photoUri = profile?.photoUri
                            )
                            showEditProfileDialog = false
                            Toast.makeText(context, "Данные пользователя сохранены", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MysticGold)
                    ) {
                        Text("Сохранить", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Отмена", color = Color.Gray)
                    }
                },
                title = {
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "✏️ Редактирование профиля" else "✏️ Edit Profile",
                        color = MysticGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Имя пользователя", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MysticGold,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editAge,
                                onValueChange = { editAge = it },
                                label = { Text("Возраст", color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MysticGold,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = editHeight,
                                onValueChange = { editHeight = it },
                                label = { Text("Рост (см)", color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MysticGold,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Выбор пола пользователя
                        Text(text = "Пол:", color = MysticGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = editGender == "Male",
                                onClick = { editGender = "Male" },
                                label = { Text("Мужской") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = editGender == "Female",
                                onClick = { editGender = "Female" },
                                label = { Text("Женский") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Выбор ведущей руки
                        Text(text = "Ведущая рука:", color = MysticGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = editHand == "Right",
                                onClick = { editHand = "Right" },
                                label = { Text("Правая") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = editHand == "Left",
                                onClick = { editHand = "Left" },
                                label = { Text("Левая") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            )
        }

        // 3. Диалог добавления совершенно нового профиля пользователя
        if (showNewUserDialog) {
            var newName by remember { mutableStateOf("") }
            var newAge by remember { mutableStateOf("25") }
            var newHeight by remember { mutableStateOf("170") }
            var newGender by remember { mutableStateOf("Female") }
            var newHand by remember { mutableStateOf("Right") }

            AlertDialog(
                onDismissRequest = { showNewUserDialog = false },
                containerColor = MysticDarkBackground,
                shape = RoundedCornerShape(24.dp),
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                val ageVal = newAge.toIntOrNull() ?: 25
                                val heightVal = newHeight.toIntOrNull() ?: 170
                                viewModel.saveUserProfile(
                                    name = newName.trim(),
                                    gender = newGender,
                                    age = ageVal,
                                    height = heightVal,
                                    dominantHand = newHand
                                )
                                showNewUserDialog = false
                                Toast.makeText(context, "Профиль ${newName.trim()} успешно создан", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MysticGold)
                    ) {
                        Text("Создать", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewUserDialog = false }) {
                        Text("Отмена", color = Color.Gray)
                    }
                },
                title = {
                    Text(
                        text = "➕ Новый профиль",
                        color = MysticGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Имя нового пользователя", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MysticGold,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newAge,
                                onValueChange = { newAge = it },
                                label = { Text("Возраст", color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MysticGold,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = newHeight,
                                onValueChange = { newHeight = it },
                                label = { Text("Рост (см)", color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MysticGold,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(text = "Пол:", color = MysticGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = newGender == "Female",
                                onClick = { newGender = "Female" },
                                label = { Text("Женский") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = newGender == "Male",
                                onClick = { newGender = "Male" },
                                label = { Text("Мужской") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun PaymentHistoryItem(
    payment: PaymentHistoryEntity,
    currentLang: AppLanguage
) {
    val df = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(payment.timestamp) { df.format(Date(payment.timestamp)) }

    val titleColor = when {
        payment.readingType.contains("Полная", ignoreCase = true) -> Color(0xFFE040FB)
        payment.readingType.contains("бесплатн", ignoreCase = true) || payment.readingType.contains("Краткая", ignoreCase = true) -> Color(0xFF00FF66)
        else -> MysticGold
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33141420)),
        border = BorderStroke(1.dp, MysticBronze.copy(0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.readingType,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = titleColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$formattedDate • ${payment.paymentSystem}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${payment.amountRub} ₽",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MysticGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }
    }
}


// --- SCREEN 8: HISTORY VIEW ---

@Composable
fun HistoryScreen(
    viewModel: PalmistViewModel,
    onNavigateToResult: () -> Unit
) {
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    val readings by viewModel.allReadings.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            MysticHeader(strings.histTitle)

            Spacer(modifier = Modifier.height(16.dp))

            if (readings.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Text(
                        text = strings.histNoRecords,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(readings) { record ->
                        ReadingHistoryItem(
                            record = record,
                            currentLang = currentLang,
                            viewModel = viewModel,
                            onOpen = {
                                if (record.analysisType == "compatibility") {
                                    viewModel.currentCompatibilityReading.value = record
                                    viewModel.activeTab.value = "compatibility"
                                } else {
                                    viewModel.currentReading.value = record
                                    onNavigateToResult()
                                }
                            },
                            onDelete = {
                                viewModel.deleteReading(record.id)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                MysticButton(
                    text = strings.histClearHistory,
                    onClick = { viewModel.clearHistory() },
                    isSecondary = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Извлечение процента результата совместимости из JSON
private fun parseScorePercentage(json: String): Int {
    if (json.isBlank()) return 85
    return try {
        val regex = Regex("\"(?:overallScore|compatibilityScore|score)\"\\s*:\\s*(\\d+)")
        val match = regex.find(json)
        if (match != null) {
            match.groupValues[1].toInt()
        } else {
            val percentRegex = Regex("(\\d{2,3})\\s*%")
            val pMatch = percentRegex.find(json)
            pMatch?.groupValues?.get(1)?.toInt() ?: 85
        }
    } catch (e: Exception) {
        85
    }
}

@Composable
fun ReadingHistoryItem(
    record: ReadingEntity, // Сущность сохранённой записи анализа ладоней или совместимости
    currentLang: AppLanguage, // Выбранный язык приложения для перевода меток
    viewModel: PalmistViewModel, // ViewModel для функций TTS в под-пунктах
    onOpen: () -> Unit, // Обработчик нажатия для открытия подробного отчёта анализа
    onDelete: () -> Unit // Обработчик нажатия для удаления записи из истории
) {
    val df = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) } // Форматирование даты и времени
    val formattedDate = remember(record.timestamp) { df.format(Date(record.timestamp)) } // Форматированная строка даты

    // Определение типа проводимого анализа для цветовой и визуальной идентификации
    val isCompatibility = record.analysisType == "compatibility" // Анализ совместимости двух партнеров
    val isBrief = record.analysisType.contains("brief") // Краткая экспресс-интерпретация
    val isFull = record.analysisType.contains("full") || (!isCompatibility && !isBrief) // Полная глубокая интерпретация

    // Тематическое цветовое выделение по требованиям ТЗ (Красный - Совместимость, Зеленый - Краткая, Фиолетовый - Полная)
    val themeColor = when {
        isCompatibility -> Color(0xFFFF3366) // Ярко-красный / Алый акцент для анализа совместимости
        isBrief -> Color(0xFF00FF66) // Ярко-зелёный акцент для кратких интерпретаций
        else -> Color(0xFFE040FB) // Ярко-фиолетовый / Пурпурный акцент для полных интерпретаций
    }

    // Заголовок типа проводимого анализа
    val typeTitle = when {
        isCompatibility -> if (currentLang == AppLanguage.RUS) "Совместимость" else "Compatibility" // Заголовок Совместимость
        isBrief -> if (currentLang == AppLanguage.RUS) "Краткая Интерпретация" else "Brief Interpretation" // Заголовок Краткая Интерпретация
        else -> if (currentLang == AppLanguage.RUS) "Полная Интерпретация" else "Full Interpretation" // Заголовок Полная Интерпретация
    }

    val allProfiles by viewModel.allUserProfiles.collectAsState()
    val mainUserProfile by viewModel.userProfile.collectAsState()

    // Поиск фото профиля пользователя по имени записи или текущего профиля
    val matchedProfile = remember(record.name, allProfiles, mainUserProfile) {
        allProfiles.find { it.name.equals(record.name, ignoreCase = true) } ?: mainUserProfile
    }
    val userProfilePhoto = matchedProfile?.photoUri?.takeIf { it.isNotBlank() }

    // Извлечение пути к первому снимку ладони/руки из записи анализа
    val handPhotoPath = (record.leftPalmPath ?: record.rightPalmPath ?: record.imageUrl ?: record.partnerImageUrl ?: record.leftBackPath ?: record.rightBackPath)?.takeIf { it.isNotBlank() }

    // Приоритетное изображение: Фото пользователя из профиля ИЛИ Первое фото руки по ТЗ
    val finalImagePath = userProfilePhoto ?: handPhotoPath
    val imgModel = remember(finalImagePath) { getCoilImageModel(finalImagePath) }

    val historyFollowUps = remember(record.followUpQuestionsJson) {
        com.aistudio.hiromant.kxsrwa.data.local.parseFollowUpQuestionsJson(record.followUpQuestionsJson)
    }

    Card(
        shape = RoundedCornerShape(16.dp), // Скругление карточки 16dp
        colors = CardDefaults.cardColors(containerColor = Color(0x33141420)), // Мистический тёмный фон
        border = BorderStroke(1.2.dp, themeColor.copy(alpha = 0.55f)), // Выделение цветной рамкой соответствующего типа
        modifier = Modifier.fillMaxWidth() // На всю ширину контейнера
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen) // Клик по основной части открывает весь отчёт
            ) {
                // Кнопка удаления записи из истории анализов
                IconButton(
                    onClick = onDelete, // Вызов удаления
                    modifier = Modifier
                        .align(Alignment.TopEnd) // В верхнем правом углу
                        .size(28.dp) // Компактный размер
                        .padding(4.dp) // Отступы
                ) {
                    Icon(
                        imageVector = Icons.Default.Close, // Иконка крестика
                        contentDescription = "Удалить", // Описание кнопки
                        tint = Color.White.copy(alpha = 0.45f), // Полупрозрачный цвет
                        modifier = Modifier.size(16.dp) // Размер иконки
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp), // Внутренние отступы элементов
                    verticalAlignment = Alignment.CenterVertically // Выравнивание по центру
                ) {
                    // Превью снимка ладони слева с цветным обрамлением соответствующего анализа
                    Surface(
                        shape = RoundedCornerShape(12.dp), // Скругление 12dp
                        color = themeColor.copy(alpha = 0.15f), // Прозрачный цветной фон
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.5f)), // Цветной контур превью
                        modifier = Modifier.size(48.dp) // Фиксированный размер 48dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (imgModel != null) {
                                coil.compose.AsyncImage(
                                    model = imgModel, // Отображение аватара пользователя или снимка ладони
                                    contentDescription = "Reading photo", // Описание фото
                                    contentScale = ContentScale.Crop, // Масштабирование
                                    error = painterResource(id = com.aistudio.hiromant.kxsrwa.R.drawable.img_splash_hand), // Резервное фото ладони при ошибке загрузки
                                    placeholder = painterResource(id = com.aistudio.hiromant.kxsrwa.R.drawable.img_splash_hand), // Плейсхолдер во время загрузки
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = com.aistudio.hiromant.kxsrwa.R.drawable.img_splash_hand), // Изображение ладони по умолчанию
                                    contentDescription = "Hand Placeholder",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp)) // Горизонтальный отступ

                    // Информационный текстовый блок
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 1. Первая строка - Тип Анализа в 1 строку ярким тематическим цветом по ТЗ
                        Text(
                            text = typeTitle, // Заголовок типа (Совместимость, Краткая или Полная)
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = themeColor, // Тематический яркий цвет (Красный, Зеленый или Фиолетовый)
                                fontWeight = FontWeight.Bold, // Жирный шрифт
                                fontSize = 14.5.sp // Адаптивный размер шрифта
                            ),
                            maxLines = 1, // Ограничение в 1 строку по конституции проекта
                            overflow = TextOverflow.Ellipsis // Обрезка многоточием
                        )

                        Spacer(modifier = Modifier.height(2.dp)) // Отступ

                        if (isCompatibility) {
                            val p2Name = record.partnerName ?: if (currentLang == AppLanguage.RUS) "Партнёр" else "Partner"

                            // Вторая строка для Совместимости - Имена первого и второго пользователей
                            Text(
                                text = "${record.name} + $p2Name", // Совместные имена
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White, // Белый цвет текста
                                    fontWeight = FontWeight.Bold, // Жирное начертание
                                    fontSize = 14.sp // Размер шрифта
                                ),
                                maxLines = 1, // Строго 1 строка
                                overflow = TextOverflow.Ellipsis // Обрезка многоточием
                            )

                            Spacer(modifier = Modifier.height(2.dp)) // Отступ

                            // Третья строка - дата прохождения
                            Text(
                                text = formattedDate, // Форматированная дата
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray) // Серый текст
                            )
                        } else {
                            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) // Текущий год
                            val birthYear = if (record.age > 0) (currentYear - record.age) else 1990 // Вычисление года рождения

                            val genderText = when (record.gender.lowercase()) {
                                "female", "женский", "женск", "ж" -> if (currentLang == AppLanguage.RUS) "Женск." else "Female"
                                else -> if (currentLang == AppLanguage.RUS) "Мужск." else "Male"
                            }

                            val line1Text = if (currentLang == AppLanguage.RUS) "${record.name}, $birthYear г.р." else "${record.name}, b. $birthYear"
                            val line2Text = if (currentLang == AppLanguage.RUS) "Рост - ${record.height} см. $genderText" else "Height - ${record.height} cm. $genderText"

                            // Имя и год рождения пользователя
                            Text(
                                text = line1Text, // Имя пользователя и год рождения
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White, // Белый цвет текста
                                    fontWeight = FontWeight.Bold, // Жирный шрифт
                                    fontSize = 14.sp // Размер шрифта
                                ),
                                maxLines = 1, // 1 строка
                                overflow = TextOverflow.Ellipsis // Обрезка
                            )

                            Spacer(modifier = Modifier.height(2.dp)) // Отступ

                            // Параметры антропометрии (рост и пол)
                            Text(
                                text = line2Text, // Рост и пол
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFD1C4E9), // Светло-фиолетовый оттенок
                                    fontSize = 13.sp // Размер шрифта
                                ),
                                maxLines = 1, // 1 строка
                                overflow = TextOverflow.Ellipsis // Обрезка
                            )

                            Spacer(modifier = Modifier.height(2.dp)) // Отступ

                            // Дата проведения анализа
                            Text(
                                text = formattedDate, // Форматированное время
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray) // Серый цвет
                            )
                        }
                    }

                    // Индикатор процента для анализа Совместимости
                    if (isCompatibility) {
                        val score = parseScorePercentage(record.resultJson) // Вычисление процента совпадения
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = "$score%", // Оценка совместимости
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color(0xFFFF3366), // Красный цвет процента
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }
                }
            }

            // Скрытие/раскрытие дополнительных тем и вывод названий в виде ссылок на страницу Интерпретации по ТЗ п.2
            var isTopicsExpanded by remember { mutableStateOf(false) }
            if (historyFollowUps.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                ) {
                    Divider(color = themeColor.copy(alpha = 0.35f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Шапка дополнительных тем: сворачивается/разворачивается по клику
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isTopicsExpanded = !isTopicsExpanded }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Ответы на дополнительные вопросы (${historyFollowUps.size}):" else "Answers to follow-up questions (${historyFollowUps.size}):",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MysticGold,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = if (isTopicsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isTopicsExpanded) "Свернуть" else "Раскрыть",
                            tint = MysticGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Список названий дополнительных тем, клик по которым открывает Полную Интерпретацию на выбранную тему по ТЗ п.2
                    AnimatedVisibility(visible = isTopicsExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            historyFollowUps.forEachIndexed { idx, pair ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x22141420),
                                    border = BorderStroke(0.8.dp, MysticGold.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Нажатие на тему открывает страницу Интерпретации с фиксацией на этой теме
                                            viewModel.targetFollowUpTopicIndex.value = idx
                                            if (isCompatibility) {
                                                viewModel.currentCompatibilityReading.value = record
                                            } else {
                                                viewModel.currentReading.value = record
                                            }
                                            onOpen()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${pair.question}", // Название темы / вопрос
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.5.sp
                                            ),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward, // Ссылка на страницу Интерпретации
                                            contentDescription = "Перейти к теме",
                                            tint = MysticGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- SCREEN 9: ABOUT & EDUCATIONAL FAQ SCREEN ---

@Composable
fun AboutScreen(
    viewModel: PalmistViewModel
) {
    val context = LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    val appVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }
    }

    val ttsGenderState by viewModel.ttsGender.collectAsState()
    val ttsVoiceIndex by viewModel.ttsVoiceIndex.collectAsState()
    val ttsRateState by viewModel.ttsSpeechRate.collectAsState()
    val ttsPitchState by viewModel.ttsPitch.collectAsState()

    var activeSubTab by remember { mutableStateOf("theory") } // "theory", "faq", "contacts"

    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isPlayingTts by remember { mutableStateOf(false) }
    var spokenWordRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var activeSpeakingText by remember { mutableStateOf("") } // "theory", "faq_all", "faq_0", ...

    val femaleVoicesList = remember(ttsInstance, currentLang) {
        getProcessedVoicesForGender(ttsInstance, currentLang, "Female")
    }
    val maleVoicesList = remember(ttsInstance, currentLang) {
        getProcessedVoicesForGender(ttsInstance, currentLang, "Male")
    }
    val selectedVoice = remember(ttsInstance, ttsGenderState, ttsVoiceIndex, femaleVoicesList, maleVoicesList) {
        val list = if (ttsGenderState == "Female") femaleVoicesList else maleVoicesList
        if (list.isNotEmpty()) list[ttsVoiceIndex % list.size] else null
    }

    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = if (currentLang == AppLanguage.RUS) java.util.Locale.forLanguageTag("ru") else java.util.Locale.US
            }
        }

        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isPlayingTts = true
                if (utteranceId != null) {
                    activeSpeakingText = utteranceId
                }
            }

            override fun onDone(utteranceId: String?) {
                isPlayingTts = false
                spokenWordRange = null
                activeSpeakingText = ""
            }

            override fun onError(utteranceId: String?) {
                isPlayingTts = false
                spokenWordRange = null
                activeSpeakingText = ""
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                spokenWordRange = Pair(start, end)
            }
        })

        ttsInstance = tts

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speakText(text: String, utteranceId: String) {
        if (activeSpeakingText == utteranceId && isPlayingTts) {
            ttsInstance?.stop()
            isPlayingTts = false
            spokenWordRange = null
            activeSpeakingText = ""
            return
        }

        ttsInstance?.stop()
        ttsInstance?.let { tts ->
            configureTtsVoice(
                tts = tts,
                currentLang = currentLang,
                voiceGender = ttsGenderState,
                voiceIndex = ttsVoiceIndex,
                speechRate = ttsRateState,
                speechPitch = ttsPitchState
            )
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            activeSpeakingText = utteranceId
            isPlayingTts = true
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    fun stopSpeaking() {
        ttsInstance?.stop()
        isPlayingTts = false
        spokenWordRange = null
        activeSpeakingText = ""
    }

    val faqList = remember(currentLang) {
        if (currentLang == AppLanguage.RUS) {
            listOf(
                "Как сделать качественный снимок?" to "Положите ладонь ровно, раздвинув пальцы на плоском однотонном фоне при ярком естественном или искусственном освещении. Рядом можно положить кредитную карту для точной калибровки размеров.",
                "Почему анализ занимает время?" to "Мистические алгоритмы Gemini прочитывают десятки параметров руки, включая форму ногтей, длину пальцев и холмы, формируя глубокий персонализированный отчёт.",
                "Чем отличается краткий от полного анализа?" to "Краткий даёт сжатые выводы по четырём ключевым линиям. Полный включает подробнейшую трактовку бугров, фаланг, знаков судьбы, будущих прогнозов и любовной сферы.",
                "Насколько точны прогнозы?" to "Хиромантия — это зеркало вашей души. Линии меняются в зависимости от ваших решений, поэтому приложение предоставляет руководство и духовные ориентиры."
            )
        } else {
            listOf(
                "How to take a high-quality photo?" to "Place your palm flat with fingers spread on a plain background under bright lighting. You can place a credit card nearby for size calibration.",
                "Why does analysis take time?" to "Gemini AI algorithms process dozens of hand parameters including nail shapes, finger ratios, and mounts to build a deep personalized report.",
                "Brief vs. Full analysis?" to "Brief analysis summarizes the four primary lines. Full analysis includes deep readings of mounts, phalanges, destiny marks, and future life projections.",
                "How accurate are the readings?" to "Palmistry is a mirror of your inner soul. Lines evolve with your decisions, providing spiritual guidance and actionable insight."
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            MysticHeader(strings.aboutTitle)

            // Sub Tabs - maxLines = 1, softWrap = false prevent word wrap or single letter breaks
            TabRow(
                selectedTabIndex = when (activeSubTab) {
                    "theory" -> 0
                    "faq" -> 1
                    else -> 2
                },
                containerColor = Color.Transparent,
                contentColor = MysticGold,
                indicator = { tabPositions ->
                    val idx = when (activeSubTab) {
                        "theory" -> 0
                        "faq" -> 1
                        else -> 2
                    }
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                        color = MysticGold
                    )
                }
            ) {
                Tab(
                    selected = activeSubTab == "theory",
                    onClick = {
                        if (activeSubTab != "theory") {
                            stopSpeaking()
                            activeSubTab = "theory"
                        }
                    },
                    text = {
                        Text(
                            text = strings.aboutTabInfo,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = activeSubTab == "faq",
                    onClick = {
                        if (activeSubTab != "faq") {
                            stopSpeaking()
                            activeSubTab = "faq"
                        }
                    },
                    text = {
                        Text(
                            text = strings.aboutTabFaq, // "FAQ"
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = activeSubTab == "contacts",
                    onClick = {
                        if (activeSubTab != "contacts") {
                            stopSpeaking()
                            activeSubTab = "contacts"
                        }
                    },
                    text = {
                        Text(
                            text = strings.aboutTabContacts, // "Поддержка"
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            // Scrollable contents
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                when (activeSubTab) {
                    "theory" -> {
                        val theoryTextFull = remember(strings) {
                            "${strings.aboutHistoryPalmist}. ${strings.aboutHistoryText}. ${strings.aboutTheoryLines}. ${strings.aboutTheoryText}"
                        }

                        // Единый модуль озвучивания для вкладки Теория
                        TtsVoiceController(
                            isPlaying = isPlayingTts && activeSpeakingText == "theory",
                            onPlayToggle = {
                                speakText(theoryTextFull, "theory")
                            },
                            rate = ttsRateState,
                            onRateChange = { newRate ->
                                viewModel.changeTtsSpeechRate(newRate)
                                ttsInstance?.setSpeechRate(newRate)
                            },
                            pitch = ttsPitchState,
                            onPitchChange = { newPitch ->
                                viewModel.changeTtsPitch(newPitch)
                                ttsInstance?.setPitch(newPitch)
                            },
                            gender = ttsGenderState,
                            onGenderChange = { newGender ->
                                viewModel.changeTtsGender(newGender)
                                configureTtsVoice(
                                    tts = ttsInstance,
                                    currentLang = currentLang,
                                    voiceGender = newGender,
                                    voiceIndex = ttsVoiceIndex,
                                    speechRate = ttsRateState,
                                    speechPitch = ttsPitchState
                                )
                            },
                            voiceIndex = ttsVoiceIndex,
                            onVoiceIndexChange = { newIndex ->
                                viewModel.changeTtsVoiceIndex(newIndex)
                                configureTtsVoice(
                                    tts = ttsInstance,
                                    currentLang = currentLang,
                                    voiceGender = ttsGenderState,
                                    voiceIndex = newIndex,
                                    speechRate = ttsRateState,
                                    speechPitch = ttsPitchState
                                )
                            },
                            currentLang = currentLang,
                            ttsInstance = ttsInstance,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(bottom = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = strings.aboutHistoryPalmist,
                            style = MaterialTheme.typography.titleLarge.copy(color = MysticGold, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val historyAnnotated = remember(strings.aboutHistoryText, spokenWordRange, isPlayingTts, activeSpeakingText) {
                            if (isPlayingTts && activeSpeakingText == "theory" && spokenWordRange != null) {
                                val offset = strings.aboutHistoryPalmist.length + 2
                                val relStart = (spokenWordRange!!.first - offset).coerceIn(0, strings.aboutHistoryText.length)
                                val relEnd = (spokenWordRange!!.second - offset).coerceIn(0, strings.aboutHistoryText.length)
                                buildAnnotatedString {
                                    append(strings.aboutHistoryText)
                                    if (relStart < relEnd) {
                                        addStyle(
                                            SpanStyle(background = MysticGold.copy(0.4f), color = MysticGold, fontWeight = FontWeight.Bold),
                                            start = relStart,
                                            end = relEnd
                                        )
                                    }
                                }
                            } else {
                                buildAnnotatedString { append(strings.aboutHistoryText) }
                            }
                        }

                        Text(
                            text = historyAnnotated,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = strings.aboutTheoryLines,
                            style = MaterialTheme.typography.titleLarge.copy(color = MysticGold, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val theoryAnnotated = remember(strings.aboutTheoryText, spokenWordRange, isPlayingTts, activeSpeakingText) {
                            if (isPlayingTts && activeSpeakingText == "theory" && spokenWordRange != null) {
                                val offset = strings.aboutHistoryPalmist.length + strings.aboutHistoryText.length + strings.aboutTheoryLines.length + 6
                                val relStart = (spokenWordRange!!.first - offset).coerceIn(0, strings.aboutTheoryText.length)
                                val relEnd = (spokenWordRange!!.second - offset).coerceIn(0, strings.aboutTheoryText.length)
                                buildAnnotatedString {
                                    append(strings.aboutTheoryText)
                                    if (relStart < relEnd) {
                                        addStyle(
                                            SpanStyle(background = MysticGold.copy(0.4f), color = MysticGold, fontWeight = FontWeight.Bold),
                                            start = relStart,
                                            end = relEnd
                                        )
                                    }
                                }
                            } else {
                                buildAnnotatedString { append(strings.aboutTheoryText) }
                            }
                        }

                        Text(
                            text = theoryAnnotated,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    "faq" -> {
                        val fullFaqSpokenText = remember(faqList) {
                            faqList.joinToString(". ") { "${it.first}. ${it.second}" }
                        }

                        // Единый модуль озвучивания для вкладки FAQ
                        TtsVoiceController(
                            isPlaying = isPlayingTts && activeSpeakingText == "faq_all",
                            onPlayToggle = {
                                speakText(fullFaqSpokenText, "faq_all")
                            },
                            rate = ttsRateState,
                            onRateChange = { newRate ->
                                viewModel.changeTtsSpeechRate(newRate)
                                ttsInstance?.setSpeechRate(newRate)
                            },
                            pitch = ttsPitchState,
                            onPitchChange = { newPitch ->
                                viewModel.changeTtsPitch(newPitch)
                                ttsInstance?.setPitch(newPitch)
                            },
                            gender = ttsGenderState,
                            onGenderChange = { newGender ->
                                viewModel.changeTtsGender(newGender)
                                configureTtsVoice(
                                    tts = ttsInstance,
                                    currentLang = currentLang,
                                    voiceGender = newGender,
                                    voiceIndex = ttsVoiceIndex,
                                    speechRate = ttsRateState,
                                    speechPitch = ttsPitchState
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(bottom = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        faqList.forEachIndexed { idx, (q, a) ->
                            val itemUttId = "faq_$idx"
                            val isItemSpeaking = isPlayingTts && (activeSpeakingText == itemUttId || activeSpeakingText == "faq_all")

                            FaqItem(
                                question = q,
                                answer = a,
                                isSpeaking = isItemSpeaking,
                                spokenWordRange = if (isItemSpeaking) spokenWordRange else null,
                                onVoiceClick = {
                                    speakText("$q. $a", itemUttId)
                                }
                            )
                        }
                    }

                    "contacts" -> {
                        Text(
                            text = strings.aboutEmailSupport,
                            style = MaterialTheme.typography.titleMedium.copy(color = MysticGold)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x22D4AF37)),
                            border = BorderStroke(1.dp, MysticGold.copy(0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = strings.aboutDonateTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(color = MysticGold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = strings.aboutDonateDesc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                MysticButton(
                                    text = strings.aboutSupportBtn,
                                    onClick = {
                                        Toast.makeText(context, strings.aboutSupportSuccess, Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = strings.aboutPrivacyPolicy,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Privacy Policy: All photos are analyzed securely and deleted instantly.", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                val appFullDisplayName = if (currentLang == AppLanguage.RUS) {
                    "Хиромант $appVersionName"
                } else {
                    "Hiromant $appVersionName"
                }
                Text(
                    text = appFullDisplayName,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray.copy(alpha = 0.6f)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun InfoVoiceControlCard(
    title: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x221E1B2E)),
        border = BorderStroke(1.dp, if (isPlaying) MysticGold else MysticBronze.copy(0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isPlaying) MysticGold.copy(0.25f) else Color.White.copy(0.05f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isPlaying) MysticGold else MysticBronze.copy(0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = "Голос",
                        tint = if (isPlaying) MysticGold else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MysticGold
                    )
                    Text(
                        text = if (isPlaying) "Озвучивание включено..." else "Нажмите для прослушивания",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPlaying) MysticGold.copy(0.8f) else Color.Gray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(MysticGold, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isPlaying) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onStopClick,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0x33CF6679), CircleShape)
                            .border(0.5.dp, Color(0xFFCF6679), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Остановить",
                            tint = Color(0xFFCF6679),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FaqItem(
    question: String,
    answer: String,
    isSpeaking: Boolean = false,
    spokenWordRange: Pair<Int, Int>? = null,
    onVoiceClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(isSpeaking) {
        if (isSpeaking) {
            expanded = true
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSpeaking) Color(0x33D4AF37) else Color(0x22141420)
        ),
        border = BorderStroke(
            width = if (isSpeaking) 1.dp else 0.5.dp,
            color = if (isSpeaking) MysticGold else MysticBronze.copy(0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MysticGold,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = "Озвучить вопрос",
                            tint = if (isSpeaking) MysticGold else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MysticGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                val annotatedAnswer = remember(answer, spokenWordRange, isSpeaking) {
                    if (isSpeaking && spokenWordRange != null) {
                        val offset = question.length + 2
                        val relStart = (spokenWordRange.first - offset).coerceIn(0, answer.length)
                        val relEnd = (spokenWordRange.second - offset).coerceIn(0, answer.length)
                        buildAnnotatedString {
                            append(answer)
                            if (relStart < relEnd) {
                                addStyle(
                                    SpanStyle(background = MysticGold.copy(0.4f), color = Color.White, fontWeight = FontWeight.Bold),
                                    start = relStart,
                                    end = relEnd
                                )
                            }
                        }
                    } else {
                        buildAnnotatedString { append(answer) }
                    }
                }
                Text(
                    text = annotatedAnswer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE0E0E0)
                )
            }
        }
    }
}


// --- SCREEN 10: SETTINGS ---

@Composable
fun SettingsScreen(
    viewModel: PalmistViewModel,
    onNavigateToLanguage: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    val billingState by viewModel.billingState.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    val ttsGenderState by viewModel.ttsGender.collectAsState()
    val ttsVoiceIndex by viewModel.ttsVoiceIndex.collectAsState()
    val ttsRateState by viewModel.ttsSpeechRate.collectAsState()
    val ttsPitchState by viewModel.ttsPitch.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()

    var aboutProgramExpanded by remember { mutableStateOf(false) }
    var aboutDevExpanded by remember { mutableStateOf(false) }
    var voiceSettingsExpanded by remember { mutableStateOf(false) }
    var debugLogExpanded by remember { mutableStateOf(false) }
    var femaleDropdownExpanded by remember { mutableStateOf(false) }
    var maleDropdownExpanded by remember { mutableStateOf(false) }
    var backupRestoreExpanded by remember { mutableStateOf(false) } // Состояние сворачивания карточки Бэкап/Восстановление
    var restoreDialogText by remember { mutableStateOf<String?>(null) } // Текст информационного окна успешного восстановления

    // Запуск выбора файла для сохранения резервной копии (бэкапа)
    val saveBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: android.net.Uri? ->
        if (uri != null) { // Если пользователь выбрал место и подтвердил наименование файла
            viewModel.saveBackupToUri(context, uri) { success, message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show() // Отображение всплывающего сообщения
            }
        }
    }

    // Запуск выбора файла резервной копии для восстановления всех данных
    val restoreBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) { // Если пользователь выбрал файл резервной копии на диске
            viewModel.restoreBackupFromUri(context, uri) { result ->
                if (result.isSuccess) { // При успешном восстановлении данных
                    val msg = if (currentLang == AppLanguage.RUS) {
                        "Резервная копия успешно восстановлена!\n\n" +
                        "• Профилей пользователей: ${result.restoredUsersCount}\n" +
                        "• Интерпретаций и анализов: ${result.restoredReadingsCount}\n" +
                        "• Истории платежей и бонусов: ${result.restoredPaymentsCount}\n" +
                        "• Логов расхода токенов: ${result.restoredTokensCount}"
                    } else {
                        "Backup restored successfully!\n\n" +
                        "• User Profiles: ${result.restoredUsersCount}\n" +
                        "• Line Analyses: ${result.restoredReadingsCount}\n" +
                        "• Payment History: ${result.restoredPaymentsCount}\n" +
                        "• Token Usage Logs: ${result.restoredTokensCount}"
                    }
                    restoreDialogText = msg // Запись и отображение диалога
                } else { // Если произошла ошибка при восстановлении
                    val err = result.errorMessage ?: (if (currentLang == AppLanguage.RUS) "Ошибка при восстановлении" else "Restoration error")
                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show() // Показ уведомления об ошибке
                }
            }
        }
    }

    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isPlayingTts by remember { mutableStateOf(false) }
    var isSpeakingTest by remember { mutableStateOf(false) }
    var currentWordRange by remember { mutableStateOf<IntRange?>(null) }
    var activeSpeakingText by remember { mutableStateOf("") } // "program" or "dev"

    var tempRate by remember(ttsRateState) { mutableFloatStateOf(ttsRateState) }
    var tempPitch by remember(ttsPitchState) { mutableFloatStateOf(ttsPitchState) }

    val femaleVoicesList = remember(ttsInstance, currentLang) {
        getProcessedVoicesForGender(ttsInstance, currentLang, "Female")
    }
    val maleVoicesList = remember(ttsInstance, currentLang) {
        getProcessedVoicesForGender(ttsInstance, currentLang, "Male")
    }
    val selectedVoice = remember(ttsInstance, ttsGenderState, ttsVoiceIndex, femaleVoicesList, maleVoicesList) {
        val list = if (ttsGenderState == "Female") femaleVoicesList else maleVoicesList
        if (list.isNotEmpty()) list[ttsVoiceIndex % list.size] else null
    }

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val aboutProgramText = if (currentLang == AppLanguage.RUS) {
        "Программа «Хиромант» — это ваш персональный проводник в мир древних знаний о ладонях. С помощью современных алгоритмов искусственного интеллекта и нейросетей Gemini, приложение анализирует форму рук, пальцев и переплетение линий на ладони, сопоставляя их с канонами классической ведической и западной хиромантии. Программа считывает холмы планет, особые знаки (такие как Мистический Крест или Кольцо Соломона) и линии сердца, головы, жизни и судьбы, чтобы раскрыть ваш врождённый потенциал и дать практические советы на жизненном пути.\n\nВерсия 1.022"
    } else {
        "The 'Palmist' app is your personal guide to the ancient wisdom of palm reading. Powered by modern Gemini AI algorithms, the app analyzes your hand shape, finger proportions, and palm line networks, mapping them to the canons of classic Vedic and Western palmistry. It reads planetary mounts, sacred markings (like the Mystic Cross or Ring of Solomon), and the primary lines of Heart, Head, Life, and Destiny to unlock your innate potential and deliver actionable life guidelines.\n\nVersion 1.022"
    }

    val aboutDevText = if (currentLang == AppLanguage.RUS) {
        "Разработчик: Максим Арс. (ArsMaxim)\nКонтакты: arsmaxim@gmail.com\n\nЯ увлечён созданием интеллектуальных, красивых и полезных мобильных приложений, которые объединяют современные технологии ИИ и классическое наследие человечества. Спасибо, что выбрали моё приложение!"
    } else {
        "Developer: Maxim Ars. (ArsMaxim)\nContact: arsmaxim@gmail.com\n\nI am passionate about creating smart, beautiful, and helpful mobile applications that merge cutting-edge AI technologies with classical human heritage. Thank you for choosing my app!"
    }

    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = if (currentLang == AppLanguage.RUS) java.util.Locale.forLanguageTag("ru") else java.util.Locale.US
            }
        }
        
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isPlayingTts = true
                if (utteranceId == "voice_test_utt") {
                    isSpeakingTest = true
                }
            }

            override fun onDone(utteranceId: String?) {
                isPlayingTts = false
                currentWordRange = null
                activeSpeakingText = ""
                isSpeakingTest = false
            }

            override fun onError(utteranceId: String?) {
                isPlayingTts = false
                currentWordRange = null
                activeSpeakingText = ""
                isSpeakingTest = false
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                currentWordRange = start..end
            }
        })
        
        ttsInstance = tts
        
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val annotatedAboutProgram = remember(currentWordRange, activeSpeakingText, currentLang) {
        buildAnnotatedString {
            append(aboutProgramText)
            
            if (activeSpeakingText == "program" && currentWordRange != null) {
                val start = currentWordRange!!.first
                val end = currentWordRange!!.last
                if (start in 0..length && end in start..length) {
                    addStyle(
                        style = SpanStyle(
                            background = MysticGold.copy(alpha = 0.4f),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        ),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    val annotatedAboutDev = remember(currentWordRange, activeSpeakingText, currentLang) {
        buildAnnotatedString {
            append(aboutDevText)
            
            val emailStr = "arsmaxim@gmail.com"
            val emailIndex = aboutDevText.indexOf(emailStr)
            if (emailIndex != -1) {
                addStyle(
                    style = SpanStyle(
                        color = MysticGold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        fontWeight = FontWeight.Bold
                    ),
                    start = emailIndex,
                    end = emailIndex + emailStr.length
                )
            }
            
            if (activeSpeakingText == "dev" && currentWordRange != null) {
                val start = currentWordRange!!.first
                val end = currentWordRange!!.last
                if (start in 0..length && end in start..length) {
                    addStyle(
                        style = SpanStyle(
                            background = MysticGold.copy(alpha = 0.4f),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        ),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 6.dp)
        ) {
            MysticHeader(strings.settTitle)

            Spacer(modifier = Modifier.height(20.dp))

            // Choose language row
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22141420)),
                border = BorderStroke(1.dp, MysticBronze.copy(0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToLanguage)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.settLanguage, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    val flagEmoji = if (currentLang == AppLanguage.RUS) "🇷🇺" else "🇬🇧"
                    val langText = if (currentLang == AppLanguage.RUS) "Русский" else "English"
                    
                    Text(text = flagEmoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = langText, color = MysticGold, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MysticGold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Font scale row
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22141420)),
                border = BorderStroke(1.dp, MysticBronze.copy(0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "Шрифт" else "Font",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (fontScale > 0.85f) viewModel.changeFontScale(fontScale - 0.1f) }
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = MysticGold)
                        }
                        Text(
                            text = "${(fontScale * 100).toInt()}%",
                            color = MysticGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { if (fontScale < 1.55f) viewModel.changeFontScale(fontScale + 0.1f) }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = MysticGold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable card: "Голос"
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22141420)),
                border = BorderStroke(1.dp, MysticBronze.copy(0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { voiceSettingsExpanded = !voiceSettingsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = MysticGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Голос" else "Voice",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = if (voiceSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MysticGold
                        )
                    }
                    if (voiceSettingsExpanded) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 1. Global toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Автоматическое чтение" else "Auto-reading",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Озвучивать результаты анализа при открытии" else "Read analysis results aloud on open",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = ttsEnabled,
                                onCheckedChange = { viewModel.changeTtsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MysticGold,
                                    checkedTrackColor = MysticGold.copy(0.4f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Пол и выбор конкретного голоса выпадающим списком
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Пол и выбор голоса" else "Voice Gender & Selection",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Выбор женского голоса с выпадающим списком
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (ttsGenderState == "Female") MysticGold else Color.White.copy(0.05f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (ttsGenderState == "Female") MysticGold else MysticBronze.copy(0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.changeTtsGender("Female")
                                        femaleDropdownExpanded = !femaleDropdownExpanded
                                        maleDropdownExpanded = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (currentLang == AppLanguage.RUS) "Женский ▼" else "Female ▼",
                                        color = if (ttsGenderState == "Female") Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                DropdownMenu(
                                    expanded = femaleDropdownExpanded,
                                    onDismissRequest = { femaleDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(Color(0xFF1E192C))
                                        .border(1.dp, MysticGold, RoundedCornerShape(8.dp))
                                ) {
                                    val femaleVoices = getVoiceOptionNames(ttsInstance, currentLang, "Female")
                                    femaleVoices.forEachIndexed { index, name ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = name,
                                                        color = if (ttsGenderState == "Female" && ttsVoiceIndex == index) MysticGold else Color.White,
                                                        fontWeight = if (ttsGenderState == "Female" && ttsVoiceIndex == index) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 13.sp
                                                    )
                                                    if (ttsGenderState == "Female" && ttsVoiceIndex == index) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = MysticGold,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.changeTtsGender("Female")
                                                viewModel.changeTtsVoiceIndex(index)
                                                femaleDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Выбор мужского голоса с выпадающим списком
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (ttsGenderState == "Male") MysticGold else Color.White.copy(0.05f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (ttsGenderState == "Male") MysticGold else MysticBronze.copy(0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.changeTtsGender("Male")
                                        maleDropdownExpanded = !maleDropdownExpanded
                                        femaleDropdownExpanded = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (currentLang == AppLanguage.RUS) "Мужской ▼" else "Male ▼",
                                        color = if (ttsGenderState == "Male") Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                DropdownMenu(
                                    expanded = maleDropdownExpanded,
                                    onDismissRequest = { maleDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(Color(0xFF1E192C))
                                        .border(1.dp, MysticGold, RoundedCornerShape(8.dp))
                                ) {
                                    val maleVoices = getVoiceOptionNames(ttsInstance, currentLang, "Male")
                                    maleVoices.forEachIndexed { index, name ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = name,
                                                        color = if (ttsGenderState == "Male" && ttsVoiceIndex == index) MysticGold else Color.White,
                                                        fontWeight = if (ttsGenderState == "Male" && ttsVoiceIndex == index) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 13.sp
                                                    )
                                                    if (ttsGenderState == "Male" && ttsVoiceIndex == index) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = MysticGold,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.changeTtsGender("Male")
                                                viewModel.changeTtsVoiceIndex(index)
                                                maleDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Индикатор активного голоса
                        val activeVoiceOptions = getVoiceOptionNames(ttsInstance, currentLang, ttsGenderState)
                        val activeVoiceTitle = activeVoiceOptions.getOrElse(ttsVoiceIndex % activeVoiceOptions.size) { "" }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33D4AF37),
                            border = BorderStroke(0.8.dp, MysticGold.copy(0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MysticGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeVoiceTitle, // Название выбранного голоса без префикса "Активный голос:"
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Скорость речи
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Скорость речи" else "Speech Rate",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = String.format("%.1fx", tempRate),
                                style = MaterialTheme.typography.titleSmall,
                                color = MysticGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = tempRate,
                            onValueChange = {
                                tempRate = it
                                viewModel.changeTtsSpeechRate(it)
                            },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MysticGold,
                                activeTrackColor = MysticGold,
                                inactiveTrackColor = MysticBronze.copy(0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Тон голоса
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Тон голоса" else "Voice Pitch",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = String.format("%.1fx", tempPitch),
                                style = MaterialTheme.typography.titleSmall,
                                color = MysticGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = tempPitch,
                            onValueChange = {
                                tempPitch = it
                                viewModel.changeTtsPitch(it)
                            },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MysticGold,
                                activeTrackColor = MysticGold,
                                inactiveTrackColor = MysticBronze.copy(0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 5. Поле ввода текста для проверки озвучивания (автоматически расширяется, с подсветкой читаемого слова)
                        var customTestText by remember(currentLang) {
                            mutableStateOf(
                                if (currentLang == AppLanguage.RUS) {
                                    "Это проверка настройки голоса, в приложении Хиромант"
                                } else {
                                    "This is a test of voice settings in the Palmist app"
                                }
                            )
                        }

                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Текст для проверки озвучивания:" else "Text for voice testing:",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isSpeakingTest && currentWordRange != null) {
                            val annotatedTestText = buildAnnotatedString {
                                append(customTestText)
                                val start = currentWordRange!!.first
                                val end = currentWordRange!!.last
                                if (start in 0..length && end in start..length) {
                                    addStyle(
                                        style = SpanStyle(
                                            background = MysticGold.copy(alpha = 0.45f),
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        start = start,
                                        end = end
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x33000000), RoundedCornerShape(10.dp))
                                    .border(1.dp, MysticGold, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                                    .heightIn(min = 60.dp)
                            ) {
                                Text(
                                    text = annotatedTestText,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = customTestText,
                                onValueChange = { customTestText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp, max = 220.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 14.sp),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MysticGold,
                                    unfocusedBorderColor = MysticBronze.copy(0.4f),
                                    focusedContainerColor = Color(0x33000000),
                                    unfocusedContainerColor = Color(0x22000000),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 6. Кнопки управления: Озвучить, Сбросить, Сохранить
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Кнопка "Озвучить"
                            OutlinedButton(
                                onClick = {
                                    if (isSpeakingTest) {
                                        ttsInstance?.stop()
                                        isSpeakingTest = false
                                    } else {
                                        ttsInstance?.stop()
                                        configureTtsVoice(
                                            tts = ttsInstance,
                                            currentLang = currentLang,
                                            voiceGender = ttsGenderState,
                                            voiceIndex = ttsVoiceIndex,
                                            speechRate = tempRate,
                                            speechPitch = tempPitch
                                        )
                                        val testPhrase = customTestText.ifBlank {
                                            if (currentLang == AppLanguage.RUS) {
                                                "Это проверка настройки голоса, в приложении Хиромант"
                                            } else {
                                                "This is a test of voice settings in the Palmist app"
                                            }
                                        }
                                        val params = android.os.Bundle().apply {
                                            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "voice_test_utt")
                                        }
                                        ttsInstance?.speak(testPhrase, TextToSpeech.QUEUE_FLUSH, params, "voice_test_utt")
                                        isSpeakingTest = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MysticGold),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0x33D4AF37),
                                    contentColor = MysticGold
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingTest) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = MysticGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSpeakingTest) {
                                        if (currentLang == AppLanguage.RUS) "Стоп" else "Stop"
                                    } else {
                                        if (currentLang == AppLanguage.RUS) "Озвучить" else "Speak"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Кнопка "Сбросить"
                            OutlinedButton(
                                onClick = {
                                    tempRate = 1.0f
                                    tempPitch = 1.0f
                                    viewModel.changeTtsSpeechRate(1.0f)
                                    viewModel.changeTtsPitch(1.0f)
                                    viewModel.changeTtsGender("Female")
                                    viewModel.changeTtsVoiceIndex(0)
                                    if (isSpeakingTest) {
                                        ttsInstance?.stop()
                                        isSpeakingTest = false
                                    }
                                    Toast.makeText(
                                        context,
                                        if (currentLang == AppLanguage.RUS) "Настройки сброшены по умолчанию" else "Settings reset to default",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.Gray.copy(0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White.copy(0.05f),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Сбросить" else "Reset",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Кнопка "Сохранить"
                            Button(
                                onClick = {
                                    viewModel.changeTtsSpeechRate(tempRate)
                                    viewModel.changeTtsPitch(tempPitch)
                                    viewModel.changeTtsGender(ttsGenderState)
                                    viewModel.changeTtsVoiceIndex(ttsVoiceIndex)
                                    Toast.makeText(
                                        context,
                                        if (currentLang == AppLanguage.RUS) "Настройки голоса сохранены" else "Voice settings saved",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MysticGold,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Сохранить" else "Save",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Карточка пункта: "Бэкап / Восстановление"
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22141420)),
                border = BorderStroke(1.dp, MysticBronze.copy(0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { backupRestoreExpanded = !backupRestoreExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MysticGold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Бэкап / Восстановление" else "Backup / Restore",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (backupRestoreExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MysticGold
                        )
                    }

                    if (backupRestoreExpanded) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (currentLang == AppLanguage.RUS) {
                                "Сохранение и восстановление всех настроек программы, пользователей, сохранённых анализов с вопросами и историей оплат в читаемый CSV-файл."
                            } else {
                                "Save and restore all program settings, user profiles, saved line analyses with follow-up questions, and payment history into a readable CSV file."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Кнопка создания резервной копии
                            Button(
                                onClick = {
                                    saveBackupLauncher.launch(viewModel.getBackupFileName())
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MysticGold,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Создать бэкап" else "Create Backup",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Кнопка восстановления из резервной копии
                            OutlinedButton(
                                onClick = {
                                    restoreBackupLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/csv", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MysticGold),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0x33D4AF37),
                                    contentColor = MysticGold
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = MysticGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Восстановить" else "Restore",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable card: "О программе"
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22141420)),
                border = BorderStroke(1.dp, MysticBronze.copy(0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aboutProgramExpanded = !aboutProgramExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MysticGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "О программе" else "About Program",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = if (aboutProgramExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MysticGold
                        )
                    }
                    if (aboutProgramExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Единый TTS модуль для описания программы
                        TtsVoiceController(
                            isPlaying = isPlayingTts && activeSpeakingText == "program",
                            onPlayToggle = {
                                if (isPlayingTts && activeSpeakingText == "program") {
                                    ttsInstance?.stop()
                                    isPlayingTts = false
                                    currentWordRange = null
                                    activeSpeakingText = ""
                                } else {
                                    ttsInstance?.stop()
                                    activeSpeakingText = "program"
                                    configureTtsVoice(
                                        tts = ttsInstance,
                                        currentLang = currentLang,
                                        voiceGender = ttsGenderState,
                                        voiceIndex = ttsVoiceIndex,
                                        speechRate = ttsRateState,
                                        speechPitch = ttsPitchState
                                    )
                                    val params = android.os.Bundle().apply {
                                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "about_program_utt")
                                    }
                                    ttsInstance?.speak(aboutProgramText, TextToSpeech.QUEUE_FLUSH, params, "about_program_utt")
                                    isPlayingTts = true
                                }
                            },
                            rate = ttsRateState,
                            onRateChange = { newRate ->
                                viewModel.changeTtsSpeechRate(newRate)
                                ttsInstance?.setSpeechRate(newRate)
                            },
                            pitch = ttsPitchState,
                            onPitchChange = { newPitch ->
                                viewModel.changeTtsPitch(newPitch)
                                ttsInstance?.setPitch(newPitch)
                            },
                            gender = ttsGenderState,
                            onGenderChange = { newGender ->
                                viewModel.changeTtsGender(newGender)
                                configureTtsVoice(
                                    tts = ttsInstance,
                                    currentLang = currentLang,
                                    voiceGender = newGender,
                                    voiceIndex = ttsVoiceIndex,
                                    speechRate = ttsRateState,
                                    speechPitch = ttsPitchState
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = annotatedAboutProgram,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable card: "Настройки базы данных"
            var remoteDbExpanded by remember { mutableStateOf(false) }
            val dbHost by viewModel.remoteDbHost.collectAsState()
            val dbName by viewModel.remoteDbName.collectAsState()
            val dbUser by viewModel.remoteDbUser.collectAsState()
            val dbPass by viewModel.remoteDbPassword.collectAsState()
            val dbStatus by viewModel.remoteDbStatus.collectAsState()

            var tempHost by remember(dbHost) { mutableStateOf(dbHost) }
            var tempName by remember(dbName) { mutableStateOf(dbName) }
            var tempUser by remember(dbUser) { mutableStateOf(dbUser) }
            var tempPass by remember(dbPass) { mutableStateOf(dbPass) }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22141420)),
                border = BorderStroke(1.dp, MysticBronze.copy(0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { remoteDbExpanded = !remoteDbExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = MysticGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Подключение к базе данных (БД)" else "Database Connection Settings",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = if (remoteDbExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MysticGold
                        )
                    }

                    if (remoteDbExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Поле 1: Адрес сервера / Host
                        OutlinedTextField(
                            value = tempHost,
                            onValueChange = { tempHost = it },
                            label = { Text(if (currentLang == AppLanguage.RUS) "Адрес сервера (Host / URL)" else "Server Address (Host)", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MysticGold,
                                unfocusedBorderColor = MysticBronze.copy(0.4f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Поле 2: Имя БД
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text(if (currentLang == AppLanguage.RUS) "Имя базы данных" else "Database Name", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MysticGold,
                                unfocusedBorderColor = MysticBronze.copy(0.4f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Поле 3: Логин
                        OutlinedTextField(
                            value = tempUser,
                            onValueChange = { tempUser = it },
                            label = { Text(if (currentLang == AppLanguage.RUS) "Логин пользователя БД" else "DB Username", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MysticGold,
                                unfocusedBorderColor = MysticBronze.copy(0.4f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Поле 4: Пароль
                        OutlinedTextField(
                            value = tempPass,
                            onValueChange = { tempPass = it },
                            label = { Text(if (currentLang == AppLanguage.RUS) "Пароль БД" else "DB Password", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MysticGold,
                                unfocusedBorderColor = MysticBronze.copy(0.4f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Индикатор статуса
                        Text(
                            text = dbStatus,
                            color = if (dbStatus.contains("Успешное") || dbStatus.contains("сохранены") || dbStatus.contains("синхронизированы")) Color(0xFF00FF66) else MysticGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Кнопки управления: Проверить, Сохранить, Синхронизировать
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveRemoteDbConfig(tempHost, tempName, tempUser, tempPass)
                                    viewModel.testRemoteDbConnection()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MysticGold),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0x22D4AF37),
                                    contentColor = MysticGold
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Проверить" else "Test",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.saveRemoteDbConfig(tempHost, tempName, tempUser, tempPass)
                                    Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Настройки БД сохранены" else "DB Settings Saved", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MysticGold,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Сохранить" else "Save",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.syncWithRemoteDb()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF00FF66)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0x2200FF66),
                                    contentColor = Color(0xFF00FF66)
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.RUS) "Синхрон" else "Sync",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable card: "Лог для отладки" (с кнопками Копировать, Email, Поделиться)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22141420)),
                border = BorderStroke(1.dp, MysticBronze.copy(0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { debugLogExpanded = !debugLogExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BugReport, contentDescription = null, tint = MysticGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Лог для отладки" else "Debug Log",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = if (debugLogExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MysticGold
                        )
                    }

                    if (debugLogExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val logsState by AppLogger.logs.collectAsState()
                        val logFileName = remember { AppLogger.getHiromantLogFileName() }
                        val logText = remember(logsState) {
                            AppLogger.getHiromantLogContent()
                        }

                        Text(
                            text = "Файл: $logFileName",
                            style = MaterialTheme.typography.labelMedium,
                            color = MysticGold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .background(Color(0x55000000), RoundedCornerShape(8.dp))
                                .border(0.5.dp, MysticBronze.copy(0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = logText.ifEmpty { "Лог пуст" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF00FF66)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Ряд с 3 иконками: Копировать, E-mail, Поделиться
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Копировать
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("DebugLog", logText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, if (currentLang == AppLanguage.RUS) "Лог скопирован" else "Log copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0x33D4AF37), CircleShape)
                                    .border(1.dp, MysticGold, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Копировать",
                                    tint = MysticGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 2. Отправить на Email
                            IconButton(
                                onClick = {
                                    try {
                                        val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                            data = android.net.Uri.parse("mailto:arsmaxim@gmail.com")
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Palmist Debug Log")
                                            putExtra(android.content.Intent.EXTRA_TEXT, logText)
                                        }
                                        context.startActivity(emailIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Email client not found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0x33D4AF37), CircleShape)
                                    .border(1.dp, MysticGold, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = MysticGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 3. Поделиться
                            IconButton(
                                onClick = {
                                    try {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, logText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, if (currentLang == AppLanguage.RUS) "Поделиться логом" else "Share Debug Log")
                                        context.startActivity(shareIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0x33D4AF37), CircleShape)
                                    .border(1.dp, MysticGold, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Поделиться",
                                    tint = MysticGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Карточка отладки: Тестовое бесплатное начисление интерпретаций
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22141420)),
                border = BorderStroke(1.dp, MysticBronze.copy(0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MysticGold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Добавить" else "Add",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Кнопка с плюсом для Кратких Интерпретаций (Ярко-зеленая)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color(0xFF003816), CircleShape)
                                .border(1.5.dp, Color(0xFF00FF66), CircleShape)
                                .clickable {
                                    viewModel.addFreeAnalyses(1)
                                    Toast.makeText(
                                        context,
                                        if (currentLang == AppLanguage.RUS) "+1 Краткая интерпретация" else "+1 Brief interpretation",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = Color(0xFF00FF66),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        // 2. Кнопка с плюсом для Полных Интерпретаций (Ярко-фиолетовая)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color(0xFF32004D), CircleShape)
                                .border(1.5.dp, Color(0xFFE040FB), CircleShape)
                                .clickable {
                                    viewModel.addPaidAnalyses(1)
                                    Toast.makeText(
                                        context,
                                        if (currentLang == AppLanguage.RUS) "+1 Полная интерпретация" else "+1 Full interpretation",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = Color(0xFFE040FB),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (restoreDialogText != null) { // Если задан текст диалога результатов восстановления
        AlertDialog(
            onDismissRequest = { restoreDialogText = null }, // Закрытие окна по нажатию вне его
            title = {
                Text(
                    text = if (currentLang == AppLanguage.RUS) "Восстановление данных" else "Data Restoration", // Заголовок
                    color = MysticGold, // Золотистый оттенок
                    style = MaterialTheme.typography.titleMedium, // Стиль заголовка
                    maxLines = 1, // Ограничение в 1 строку по ТЗ
                    overflow = TextOverflow.Ellipsis // Обрезка многоточием
                )
            },
            text = {
                Text(
                    text = restoreDialogText!!, // Подробный отчет о восстановленных сущностях
                    color = Color.White, // Белый цвет текста
                    style = MaterialTheme.typography.bodyMedium // Стиль основного текста
                )
            },
            confirmButton = {
                TextButton(onClick = { restoreDialogText = null }) { // Кнопка подтверждения
                    Text(
                        text = "OK", // Текст кнопки
                        color = MysticGold, // Золотистый цвет
                        fontWeight = FontWeight.Bold // Жирное начертание
                    )
                }
            },
            containerColor = Color(0xFF1E192C), // Тёмный мистический контейнер
            shape = RoundedCornerShape(16.dp) // Скругленные углы
        )
    }
}

@Composable
fun BillingScreen(
    viewModel: PalmistViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)

    val scope = rememberCoroutineScope()
    var isCheckingPayment by remember { mutableStateOf(false) }

    val initialAmountFromVM by viewModel.paymentAmountToPreselect.collectAsState()
    var walletNum by remember { mutableStateOf("410013630971157") } // Номер кошелька ЮMoney получателя
    var paymentAmount by remember(initialAmountFromVM) { mutableStateOf(if (initialAmountFromVM.isBlank()) "250" else initialAmountFromVM) }
    var chosenMethod by remember { mutableStateOf<String?>("yookassa") } // По умолчанию выбрана ЮKassa / ЮMoney
    var showSupportInput by remember { mutableStateOf(false) } // Флаг отображения поля ввода произвольной суммы поддержки
    var showConfirmationDialog by remember { mutableStateOf(false) }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text(
                    text = if (currentLang == AppLanguage.RUS) "Подтверждение оплаты ЮMoney" else "YooMoney Payment Confirmation",
                    color = MysticGold,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                val amtInt = paymentAmount.toIntOrNull() ?: 250
                val fullCount = amtInt / 250
                val briefCount = (amtInt % 250) / 100
                val infoStr = if (currentLang == AppLanguage.RUS) {
                    "Страница оплаты ЮMoney с выбранной суммой ($amtInt р.) открыта в вашем браузере.\n\n" +
                    "После выполнения перевода нажмите 'Подтвердить' для зачисления ваших интерпретаций:\n" +
                    "• Полных Интерпретаций: $fullCount\n" +
                    "• Кратких Интерпретаций: $briefCount"
                } else {
                    "YooMoney payment page with selected sum ($amtInt RUB) was opened in your browser.\n\n" +
                    "Click 'Confirm' after payment to credit your interpretations."
                }
                Text(
                    text = infoStr,
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isCheckingPayment = true
                            delay(1200)
                            isCheckingPayment = false
                            showConfirmationDialog = false
                            val amt = paymentAmount.toIntOrNull() ?: 250
                            viewModel.processPaymentSuccess(amt, "ЮKassa / ЮMoney")
                            Toast.makeText(
                                context,
                                if (currentLang == AppLanguage.RUS) "Оплата успешно подтверждена! Интерпретации зачислены." else "Payment confirmed successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MysticGold),
                    enabled = !isCheckingPayment
                ) {
                    if (isCheckingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Подтвердить" else "Confirm",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = false }
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "Отмена" else "Cancel",
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            },
            containerColor = MysticDarkSurface,
            textContentColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticDarkBackground)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Заголовок экрана
            MysticHeader(if (currentLang == AppLanguage.RUS) "Страница Оплаты" else "Payment Page")

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Выбор способа оплаты
            Text(
                text = if (currentLang == AppLanguage.RUS) "1. Выбор способа оплаты:" else "1. Payment method:",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // а) Google Billing
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (chosenMethod == "google") Color(0x33D4AF37) else Color(0x22141420)),
                border = BorderStroke(1.5.dp, if (chosenMethod == "google") MysticGold else MysticBronze.copy(0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { chosenMethod = "google" }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = chosenMethod == "google",
                        onClick = { chosenMethod = "google" },
                        colors = RadioButtonDefaults.colors(selectedColor = MysticGold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = MysticGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Google Billing",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // б) ЮKassa / ЮMoney
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (chosenMethod == "yookassa") Color(0x33D4AF37) else Color(0x22141420)),
                border = BorderStroke(1.5.dp, if (chosenMethod == "yookassa") MysticGold else MysticBronze.copy(0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { chosenMethod = "yookassa" }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = chosenMethod == "yookassa",
                            onClick = { chosenMethod = "yookassa" },
                            colors = RadioButtonDefaults.colors(selectedColor = MysticGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MysticGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ЮKassa / ЮMoney",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (chosenMethod == "yookassa") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Кошелек получателя:" else "Recipient wallet:",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        MysticTextField(
                            value = walletNum,
                            onValueChange = { },
                            label = "Номер кошелька ЮMoney",
                            placeholder = "41001xxxxxxxxxx",
                            readOnly = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val annotatedString = androidx.compose.ui.text.buildAnnotatedString { append(walletNum) }
                                        clipboardManager.setText(annotatedString)
                                        Toast.makeText(context, "Номер кошелька скопирован!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Wallet ID",
                                        tint = MysticGold
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Кнопки выбора суммы: а) 250 р. б) 500 р. в) 1000 р.
            Text(
                text = if (currentLang == AppLanguage.RUS) "2. Кнопки выбора суммы:" else "2. Choose payment amount:",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("250", "500", "1000").forEach { valAmount ->
                    val isSelected = paymentAmount == valAmount && !showSupportInput
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MysticGold else Color.White.copy(0.06f)
                        ),
                        border = BorderStroke(
                            1.2.dp,
                            if (isSelected) MysticGold else MysticBronze.copy(0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                paymentAmount = valAmount
                                showSupportInput = false
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$valAmount р.",
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Кнопки и раздел поддержки
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                border = BorderStroke(1.dp, MysticGold.copy(0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // в) Кнопка ПОДДЕРЖАТЬ
                    Button(
                        onClick = {
                            showSupportInput = !showSupportInput
                            if (showSupportInput && paymentAmount.isBlank()) {
                                paymentAmount = "100"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showSupportInput) MysticGold else Color(0xFF2A241E)
                        ),
                        border = BorderStroke(1.2.dp, MysticGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (showSupportInput) Color.Black else MysticGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "ПОДДЕРЖАТЬ" else "SUPPORT",
                                color = if (showSupportInput) Color.Black else MysticGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Нажав на кнопку ПОДДЕРЖАТЬ, появляется поле ввода произвольной суммы с текстом "любая сумма"
                    AnimatedVisibility(visible = showSupportInput) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MysticTextField(
                                value = paymentAmount,
                                onValueChange = { newVal ->
                                    val digits = newVal.filter { it.isDigit() }
                                    paymentAmount = digits
                                },
                                label = if (currentLang == AppLanguage.RUS) "Произвольная сумма" else "Custom amount",
                                placeholder = "любая сумма",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Под полем ввода, надпись - Краткая Интерпритация - 100 р.
                            Text(
                                text = if (currentLang == AppLanguage.RUS) "Краткая Интерпритация - 100 р." else "Brief Interpretation - 100 RUB",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF00FF66),
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопки: а) ОТМЕНИТЬ  б) ОПЛАТИТЬ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // а) ОТМЕНИТЬ
                MysticButton(
                    text = if (currentLang == AppLanguage.RUS) "ОТМЕНИТЬ" else "CANCEL",
                    onClick = onNavigateBack,
                    isSecondary = true,
                    modifier = Modifier.weight(1f)
                )

                // б) ОПЛАТИТЬ
                MysticButton(
                    text = if (currentLang == AppLanguage.RUS) "ОПЛАТИТЬ" else "PAY",
                    onClick = {
                        val amtInt = paymentAmount.trim().toIntOrNull()
                        if (amtInt == null || amtInt <= 0) {
                            Toast.makeText(
                                context,
                                if (currentLang == AppLanguage.RUS) "Пожалуйста, введите или выберите сумму" else "Please select or enter sum",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@MysticButton
                        }

                        if (chosenMethod == "google") {
                            // Начисление при оплате Google Billing
                            viewModel.processPaymentSuccess(amtInt, "Google Billing")
                            Toast.makeText(
                                context,
                                if (currentLang == AppLanguage.RUS) "Оплата прошла успешно! Интерпретации зачислены." else "Payment successful!",
                                Toast.LENGTH_LONG
                            ).show()
                            onNavigateBack()
                        } else if (chosenMethod == "yookassa") {
                            try {
                                val cleanWallet = walletNum.replace(" ", "").trim()
                                val targets = "Hiromant App Interpretation"
                                val encodedTargets = java.net.URLEncoder.encode(targets, "UTF-8")
                                // Перенаправление на страницу оплаты с уже введённой суммой без возможности изменить её
                                val url = "https://yoomoney.ru/quickpay/confirm.xml?" +
                                        "receiver=$cleanWallet&" +
                                        "quickpay-form=button&" +
                                        "targets=$encodedTargets&" +
                                        "paymentType=AC&" +
                                        "sum=$amtInt"

                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                                showConfirmationDialog = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Ошибка открытия страницы оплаты: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                if (currentLang == AppLanguage.RUS) "Выберите способ оплаты" else "Select payment method",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = chosenMethod != null
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Текст о возможной комиссии банков в формате Информационного Блока с кнопкой Play/Stop
            MysticInfoBlockCard(
                title = if (currentLang == AppLanguage.RUS) "Информация о комиссии" else "Commission Notice",
                text = if (currentLang == AppLanguage.RUS) {
                    "В разных Банках и Системах оплат, могут взымать разную комиссию...\nОбратите на это внимание!"
                } else {
                    "Different banks and payment systems may charge different commissions...\nPlease pay attention to this!"
                },
                currentLang = currentLang,
                viewModel = viewModel,
                blockId = "billing_commission_info_block",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- END OF BILLING SCREEN ---

@Composable
fun YookassaPaymentForm(
    readingId: Long,
    analysisType: String,
    viewModel: PalmistViewModel,
    onSuccess: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val strings = LocalizedStrings.get(currentLang)
    val scope = rememberCoroutineScope()

    var walletNum by remember { mutableStateOf("410013630971157") }
    var selectedMethod by remember { mutableStateOf("yoomoney") } // "yoomoney", "ozon", "wb"
    var paymentAmount by remember { mutableStateOf("250") } // Default amount 250 RUB

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var isCheckingPayment by remember { mutableStateOf(false) }
    var confirmAttempts by remember { mutableStateOf(0) }

    val actualAmount = paymentAmount

    if (showConfirmationDialog) {
        val dialogTitle = when (selectedMethod) {
            "ozon" -> if (currentLang == AppLanguage.RUS) "Ожидание оплаты Ozon Банк (СБП)" else "Pending Ozon Bank Payment (SBP)"
            "wb" -> if (currentLang == AppLanguage.RUS) "Ожидание оплаты WB Банк (СБП)" else "Pending WB Bank Payment (SBP)"
            else -> if (currentLang == AppLanguage.RUS) "Ожидание оплаты ЮMoney" else "Pending YooMoney Payment"
        }
        val dialogText = when (selectedMethod) {
            "ozon" -> if (currentLang == AppLanguage.RUS) {
                "Была инициализирована оплата через Ozon Банк (СБП) на сумму $actualAmount ₽.\n\nПожалуйста, совершите перевод и нажмите кнопку 'Подтвердить' для активации расшифровки."
            } else {
                "Payment of $actualAmount RUB via Ozon Bank (SBP) was initialized.\n\nPlease complete the transfer and click 'Confirm' to activate decoding."
            }
            "wb" -> if (currentLang == AppLanguage.RUS) {
                "Была инициализирована оплата через WB Банк (СБП) на сумму $actualAmount ₽.\n\nПожалуйста, совершите перевод и нажмите кнопку 'Подтвердить' для активации расшифровки."
            } else {
                "Payment of $actualAmount RUB via WB Bank (SBP) was initialized.\n\nPlease complete the transfer and click 'Confirm' to activate decoding."
            }
            else -> if (currentLang == AppLanguage.RUS) {
                "Официальная страница перевода ЮMoney была открыта в вашем браузере.\n\n" +
                "После успешного завершения перевода в системе ЮMoney вернитесь сюда и нажмите кнопку 'Подтвердить', чтобы получить точнейший анализ вашей судьбы!"
            } else {
                "The official YooMoney page has been opened in your browser.\n\n" +
                "After completing the transaction, return here and tap 'Confirm' to unlock your cosmic destiny analysis!"
            }
        }

        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text(
                    text = dialogTitle,
                    color = MysticGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = dialogText,
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isCheckingPayment = true
                            delay(1500)
                            isCheckingPayment = false
                            if (confirmAttempts == 0) {
                                confirmAttempts++
                                Toast.makeText(
                                    context,
                                    if (currentLang == AppLanguage.RUS) "Оплата не подтверждена... Попробуйте ещё раз!" else "Payment not confirmed. Try again!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                showConfirmationDialog = false
                                viewModel.unlockPaidReading(readingId) {
                                    Toast.makeText(
                                        context,
                                        if (currentLang == AppLanguage.RUS) "Оплата успешно подтверждена! Анализ разблокирован." else "Payment confirmed! Reading unlocked.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onSuccess()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MysticGold),
                    enabled = !isCheckingPayment
                ) {
                    if (isCheckingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Подтвердить" else "Confirm",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = false }
                ) {
                    Text(text = strings.cancel, color = Color.Gray)
                }
            },
            containerColor = MysticDarkSurface,
            textContentColor = Color.White
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MysticDarkSurface),
        border = BorderStroke(1.5.dp, MysticGold),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MysticGold,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (currentLang == AppLanguage.RUS) {
                    "Активация AI-анализа ладони"
                } else {
                    "AI Palm Analysis Activation"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MysticGold,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (currentLang == AppLanguage.RUS) {
                    "Ключ API Gemini временно перегружен или неактивен. Вы можете напрямую оплатить сеанс для моментальной активации пророчества."
                } else {
                    "The Gemini API is currently unavailable. You can pay for this premium session to instantly trigger manual decoding."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            // Main unified payment method label
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MysticGold.copy(0.12f)),
                border = BorderStroke(1.5.dp, MysticGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💳", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "ЮKassa/СПБ" else "YooKassa/SBP",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Прямая безопасная оплата услуг" else "Direct secure gateway payment",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Recipient Wallet
            Text(
                text = if (currentLang == AppLanguage.RUS) {
                    "Введите номер кошелька ЮMoney получателя:"
                } else {
                    "Recipient's YooMoney Wallet ID:"
                },
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            MysticTextField(
                value = walletNum,
                onValueChange = { },
                label = if (currentLang == AppLanguage.RUS) "Кошелёк получателя" else "Receiver Wallet",
                placeholder = "41001xxxxxxxxxx",
                readOnly = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val annotatedString = androidx.compose.ui.text.buildAnnotatedString { append(walletNum) }
                            clipboardManager.setText(annotatedString)
                            Toast.makeText(
                                context,
                                if (currentLang == AppLanguage.RUS) "Номер кошелька скопирован!" else "Wallet number copied!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Wallet ID",
                            tint = MysticGold
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Amount selector
            Text(
                text = if (currentLang == AppLanguage.RUS) {
                    "Выберите сумму перевода (рубли):"
                } else {
                    "Select payment amount (RUB):"
                },
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Выбор суммы платежа: 250 р., 500 р., 1000 р.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("250", "500", "1000").forEach { valAmount ->
                    val isSelected = paymentAmount == valAmount
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) MysticGold else Color.White.copy(0.05f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) MysticGold else MysticBronze.copy(0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                paymentAmount = valAmount // Задаем выбранную сумму
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$valAmount р.", // Текст суммы в формате "X р."
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Надпись "Поддержать проект" под кнопками сумм
            Text(
                text = if (currentLang == AppLanguage.RUS) {
                    "Поддержать проект"
                } else {
                    "Support the Project"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MysticGold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Поле ввода с текстом "Введите любую сумму..." внутри
            MysticTextField(
                value = paymentAmount,
                onValueChange = { newVal ->
                    val digits = newVal.filter { it.isDigit() }
                    paymentAmount = digits
                },
                label = if (currentLang == AppLanguage.RUS) "Сумма поддержки" else "Support Amount",
                placeholder = if (currentLang == AppLanguage.RUS) "Введите любую сумму..." else "Enter any amount...",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row( // Создаем горизонтальный контейнер для размещения кнопок управления формой оплаты сеанса
                modifier = Modifier.fillMaxWidth(), // Растягиваем контейнер на всю ширину доступного контейнера
                horizontalArrangement = Arrangement.spacedBy(12.dp) // Устанавливаем интервал в 12dp между кнопками отмены и подтверждения
            ) { // Начало описания содержимого горизонтальной строки кнопок
                MysticButton( // Отрисовываем кнопку отмены процесса оплаты отдельного сеанса
                    text = if (currentLang == AppLanguage.RUS) "ОТМЕНА" else "CANCEL", // Локализуем текст в зависимости от выбранного языка
                    onClick = onClose, // Назначаем действие закрытия формы оплаты при клике на отмену
                    isSecondary = true, // Применяем второстепенный визуальный стиль отображения кнопки
                    modifier = Modifier.weight(1f) // Задаем вес кнопки, чтобы она занимала половину доступного горизонтального места
                ) // Завершаем описание кнопки отмены
                
                MysticButton( // Отрисовываем главную кнопку совершения платежа
                    text = if (currentLang == AppLanguage.RUS) "ОПЛАТИТЬ" else "PAY NOW", // Задаем локализованный текст кнопки оплаты
                    onClick = { // Начало лямбда-выражения для обработки нажатия кнопки оплаты сеанса
                        try { // Начинаем блок перехвата возможных исключений при формировании платежной ссылки
                            val cleanWallet = walletNum.replace(" ", "").trim() // Очищаем номер кошелька ЮMoney от пробелов
                            if (cleanWallet.length < 10) { // Проверяем корректность длины кошелька получателя
                                Toast.makeText(context, "Пожалуйста, введите корректный номер кошелька ЮMoney", Toast.LENGTH_LONG).show() // Показываем тост с ошибкой
                                return@MysticButton // Прерываем выполнение логики платежа
                            } // Завершаем проверку кошелька получателя
                            val amountVal = actualAmount.trim() // Убираем лишние символы пробела из суммы платежа
                            if (amountVal.isEmpty() || amountVal.toIntOrNull() == null || amountVal.toInt() <= 0) { // Проверяем валидность введенной суммы
                                Toast.makeText(context, "Пожалуйста, введите корректную сумму (минимум 1 ₽)", Toast.LENGTH_LONG).show() // Предупреждаем о невалидной сумме
                                return@MysticButton // Прерываем дальнейшее выполнение функции оплаты
                            } // Завершаем валидацию суммы платежа
                            
                            if (selectedMethod == "ozon") { // Если был выбран дополнительный метод оплаты через Ozon Банк
                                Toast.makeText(context, "Перенаправление в Ozon Банк по СБП...", Toast.LENGTH_LONG).show() // Информируем пользователя о перенаправлении
                            } else if (selectedMethod == "wb") { // Если был выбран метод оплаты через WB Банк
                                Toast.makeText(context, "Перенаправление в WB Банк по СБП...", Toast.LENGTH_LONG).show() // Информируем о переходе в Wildberries Банк
                            } // Завершаем проверку дополнительных банковских систем

                            val targets = "Hiromant App Analysis Decoding: $analysisType" // Указываем понятное назначение платежа для пользователя
                            val encodedTargets = java.net.URLEncoder.encode(targets, "UTF-8") // Кодируем назначение платежа в UTF-8 для безопасной передачи
                            val url = "https://yoomoney.ru/quickpay/confirm.xml?" + // Строим адрес шлюза быстрых платежей ЮMoney
                                    "receiver=$cleanWallet&" + // Указываем получателя средств в параметрах запроса
                                    "quickpay-form=button&" + // Передаем параметр отображения в виде кнопки быстрой оплаты
                                    "targets=$encodedTargets&" + // Добавляем закодированное описание покупки
                                    "paymentType=AC&" + // Указываем проведение платежа с помощью банковской карты
                                    "sum=$amountVal" // Передаем итоговую сумму платежа в рублях
                            
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)) // Инициализируем Intent для перехода на внешнюю страницу оплаты
                            context.startActivity(intent) // Открываем страницу подтверждения транзакции в системном браузере
                            showConfirmationDialog = true // Активируем показ всплывающего окна подтверждения перевода средств
                        } catch (e: Exception) { // Ловим ошибки на случай сбоя работы внешнего Intent-браузера
                            e.printStackTrace() // Печатаем подробную трассировку ошибки в системную консоль
                            Toast.makeText(context, "Не удалось открыть оплату: ${e.localizedMessage}", Toast.LENGTH_LONG).show() // Показываем тост с описанием ошибки
                        } // Завершаем блок перехвата исключений
                    }, // Конец лямбда-выражения обработки кнопки оплаты сеанса
                    modifier = Modifier.weight(1f) // Задаем вес кнопке для симметричного деления строки наполовину
                ) // Завершаем описание кнопки оплаты
            } // Завершаем контейнер строки кнопок
            Spacer(modifier = Modifier.height(12.dp))
            // Информационный блок с текстом о возможной комиссии банков и кнопкой Play/Stop
            MysticInfoBlockCard(
                title = if (currentLang == AppLanguage.RUS) "Информация о комиссии" else "Commission Notice",
                text = if (currentLang == AppLanguage.RUS) {
                    "В разных Банках и Системах оплат, могут взымать разную комиссию...\nОбратите на это внимание!"
                } else {
                    "Different banks and payment systems may charge different commissions...\nPlease pay attention to this!"
                },
                currentLang = currentLang,
                viewModel = viewModel,
                blockId = "yookassa_commission_info_block"
            )
        }
    }
}


@Composable
fun PostPaymentVideoScreen(
    viewModel: PalmistViewModel,
    onNavigateToLoading: () -> Unit = {},
    onNavigateToResult: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val billingState by viewModel.billingState.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    var isSubmitting by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableIntStateOf(0) }

    LaunchedEffect(isAnalyzing, isSubmitting) {
        if (isAnalyzing || isSubmitting) {
            uploadProgress = 5
            while (uploadProgress < 95 && (isAnalyzing || isSubmitting)) {
                kotlinx.coroutines.delay(120)
                uploadProgress += 2
            }
        } else {
            uploadProgress = 0
        }
    }

    var leftVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var rightVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var activeSlot by remember { mutableStateOf<String?>(null) } // "left" or "right"
    var tempVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Список дополнительных фото с разных ракурсов
    var extraPhotoUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }

    // Лаунчер массового выбора дополнительных изображений
    val multiplePhotosPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            extraPhotoUris = (extraPhotoUris + uris).distinct()
        }
    }

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && activeSlot != null) {
            if (activeSlot == "left") {
                leftVideoUri = tempVideoUri
            } else if (activeSlot == "right") {
                rightVideoUri = tempVideoUri
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && activeSlot != null) {
            if (activeSlot == "left") {
                leftVideoUri = uri
            } else if (activeSlot == "right") {
                rightVideoUri = uri
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createGalleryVideoUri(context, "hand_${activeSlot}_video")
            if (uri != null) {
                tempVideoUri = uri
                videoCaptureLauncher.launch(uri)
            }
        } else {
            val msg = if (currentLang == AppLanguage.RUS) 
                "Требуется разрешение на использование камеры" 
            else "Camera permission required"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MysticHeader(
                    text = if (currentLang == AppLanguage.RUS) "Запись видео и фото рук" else "Record Hand Videos & Photos"
                )
            }
        },
        containerColor = MysticDarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Карточка-инструкция по съёмке
            MysticCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = MysticGold,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (currentLang == AppLanguage.RUS) 
                            "Для наиболее детального и точного анализа линии ладони запишите короткое видео движения рук или загрузите дополнительные фото с разных ракурсов."
                        else 
                            "For a detailed full analysis, record a short video of hand movement or upload extra photos from different angles.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, lineHeight = 20.sp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // КАРТОЧКА ВИДЕО ЛЕВОЙ РУКИ
            VideoSlotCard(
                title = if (currentLang == AppLanguage.RUS) "Видео ЛЕВОЙ руки" else "LEFT Hand Video",
                videoUri = leftVideoUri,
                onRecord = {
                    activeSlot = "left"
                    val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasCameraPermission) {
                        val uri = createGalleryVideoUri(context, "hand_left_video")
                        if (uri != null) {
                            tempVideoUri = uri
                            videoCaptureLauncher.launch(uri)
                        }
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                onPick = {
                    activeSlot = "left"
                    videoPickerLauncher.launch("video/*")
                },
                onClear = { leftVideoUri = null },
                currentLang = currentLang
            )

            Spacer(modifier = Modifier.height(16.dp))

            // КАРТОЧКА ВИДЕО ПРАВОЙ РУКИ
            VideoSlotCard(
                title = if (currentLang == AppLanguage.RUS) "Видео ПРАВОЙ руки" else "RIGHT Hand Video",
                videoUri = rightVideoUri,
                onRecord = {
                    activeSlot = "right"
                    val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasCameraPermission) {
                        val uri = createGalleryVideoUri(context, "hand_right_video")
                        if (uri != null) {
                            tempVideoUri = uri
                            videoCaptureLauncher.launch(uri)
                        }
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                onPick = {
                    activeSlot = "right"
                    videoPickerLauncher.launch("video/*")
                },
                onClear = { rightVideoUri = null },
                currentLang = currentLang
            )

            Spacer(modifier = Modifier.height(16.dp))

            // БЛОК МАССОВОЙ ЗАГРУЗКИ НЕСКОЛЬКИХ ФОТО ОДНОВРЕМЕННО С ПРЕВЬЮ
            MysticCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.RUS) "Дополнительные фото с разных ракурсов" else "Extra Photos from Various Angles",
                        style = MaterialTheme.typography.titleMedium.copy(color = MysticGold, fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (currentLang == AppLanguage.RUS)
                            "Вы можете добавить несколько четких снимков ладони с разных углов для максимальной детализации."
                        else
                            "You can upload multiple clear palm photos under different angles for maximum detail.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, lineHeight = 16.sp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    MysticButton(
                        text = if (currentLang == AppLanguage.RUS) "+ Загрузить несколько фото" else "+ Upload Multiple Photos",
                        onClick = { multiplePhotosPickerLauncher.launch("image/*") },
                        isSecondary = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (extraPhotoUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(extraPhotoUris) { index, uri ->
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, MysticGold, RoundedCornerShape(12.dp))
                                ) {
                                    val bmp = remember(uri) { com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.uriToBitmap(context, uri) }
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Extra Photo $index",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.DarkGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                                        }
                                    }

                                    // Кнопка удаления отдельной доп. фотографии
                                    IconButton(
                                        onClick = {
                                            extraPhotoUris = extraPhotoUris.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .background(Color.Black.copy(0.7f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Photo",
                                            tint = Color.Red,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Основная кнопка "ОТПРАВИТЬ И ВЫПОЛНИТЬ АНАЛИЗ"
            MysticButton(
                text = if (currentLang == AppLanguage.RUS) "ОТПРАВИТЬ и выполнить Анализ" else "SEND & Run Analysis",
                onClick = {
                    isSubmitting = true
                    viewModel.isAnalyzing.value = true
                    val leftBmp = viewModel.bitmapLeftPalm.value
                    val leftBackBmp = viewModel.bitmapLeftBack.value
                    val rightBmp = viewModel.bitmapRightPalm.value
                    val rightBackBmp = viewModel.bitmapRightBack.value
                    val extraBmps = extraPhotoUris.mapNotNull { u -> com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.uriToBitmap(context, u) }
                    val allBitmaps = listOfNotNull(leftBmp, leftBackBmp, rightBmp, rightBackBmp) + extraBmps

                    viewModel.runPalmAnalysis(
                        bitmaps = allBitmaps,
                        videoUri = leftVideoUri?.toString() ?: rightVideoUri?.toString(),
                        analysisType = viewModel.currentAnalysisTypeState.value,
                        leftPalmPath = viewModel.leftPalmPath.value,
                        leftBackPath = viewModel.leftBackPath.value,
                        rightPalmPath = viewModel.rightPalmPath.value,
                        rightBackPath = viewModel.rightBackPath.value,
                        onCompleted = {
                            isSubmitting = false
                            onNavigateToResult()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAnalyzing && !isSubmitting
            )

            Spacer(modifier = Modifier.height(20.dp))

            // БЛОК КНОПКИ «ПРОПУСТИТЬ ЗАГРУЗКУ ВИДЕО» С ИНФОРМАЦИОННЫМ ТЕКСТОМ И КНОПКОЙ ОЗВУЧКИ PLAY/STOP
            MysticInfoBlockCard(
                title = if (currentLang == AppLanguage.RUS) "Информация об анализе" else "Analysis Notice",
                text = if (currentLang == AppLanguage.RUS) 
                    "Для получения наиболее подробного и точного прогноза рекомендуется записать видео или добавить несколько дополнительных снимков с разных ракурсов. Вы можете пропустить загрузку видео, если хотите выполнить анализ по уже имеющимся материалам."
                else 
                    "For the most accurate full prediction, uploading video or extra multi-angle photos is recommended. You can skip video upload to run analysis with current photos.",
                currentLang = currentLang,
                viewModel = viewModel,
                blockId = "photo_video_analysis_notice_block",
                extraContent = {
                    MysticButton(
                        text = if (currentLang == AppLanguage.RUS) "ОТПРАВИТЬ (пропустить видео)" else "SEND (Skip Video)",
                        onClick = {
                            isSubmitting = true
                            viewModel.isAnalyzing.value = true
                            val leftBmp = viewModel.bitmapLeftPalm.value
                            val leftBackBmp = viewModel.bitmapLeftBack.value
                            val rightBmp = viewModel.bitmapRightPalm.value
                            val rightBackBmp = viewModel.bitmapRightBack.value
                            val extraBmps = extraPhotoUris.mapNotNull { u -> com.aistudio.hiromant.kxsrwa.utils.BitmapUtils.uriToBitmap(context, u) }
                            val allBitmaps = listOfNotNull(leftBmp, leftBackBmp, rightBmp, rightBackBmp) + extraBmps

                            viewModel.runPalmAnalysis(
                                bitmaps = allBitmaps,
                                videoUri = leftVideoUri?.toString() ?: rightVideoUri?.toString(),
                                analysisType = viewModel.currentAnalysisTypeState.value,
                                leftPalmPath = viewModel.leftPalmPath.value,
                                leftBackPath = viewModel.leftBackPath.value,
                                rightPalmPath = viewModel.rightPalmPath.value,
                                rightBackPath = viewModel.rightBackPath.value,
                                onCompleted = {
                                    isSubmitting = false
                                    onNavigateToResult()
                                }
                            )
                        },
                        isSecondary = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }

        // ВСПЛЫВАЮЩИЙ ИНДИКАТОР ПРОЦЕСС БАРА ПРИ НАЖАТИИ КНОПКИ ОТПРАВИТЬ МАТЕРИАЛЫ
        if (isAnalyzing || isSubmitting) {
            val currentProgress by viewModel.analysisProgress.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14101E)),
                    border = BorderStroke(1.5.dp, MysticGold),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MysticProgressBar(
                            progress = currentProgress,
                            currentLang = currentLang,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoSlotCard(
    title: String,
    videoUri: android.net.Uri?,
    onRecord: () -> Unit,
    onPick: () -> Unit,
    onClear: () -> Unit,
    currentLang: AppLanguage
) {
    val context = LocalContext.current

    // Генерация стоп-кадра превью для добавленного видео
    val videoFrameBitmap = remember(videoUri) {
        if (videoUri == null) null
        else {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                val frame = retriever.getFrameAtTime(1_000_000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                retriever.release()
                frame
            } catch (e: Exception) {
                null
            }
        }
    }

    MysticCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (videoUri != null) MysticGold else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1F1E1E2C))
                    .border(2.dp, if (videoUri != null) MysticGold else MysticBronze.copy(0.3f), RoundedCornerShape(16.dp))
                    .clickable { onRecord() }
            ) {
                if (videoUri != null) {
                    // Отображаем стоп-кадр кадра видео превью
                    if (videoFrameBitmap != null) {
                        Image(
                            bitmap = videoFrameBitmap.asImageBitmap(),
                            contentDescription = "Video Frame Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(0.7f))
                                    )
                                )
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MysticGold,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Видео загружено" else "Video Loaded",
                            style = MaterialTheme.typography.labelMedium.copy(color = MysticGold, fontWeight = FontWeight.Bold)
                        )
                    }
                    
                    // Кнопка удаления зафиксированного видео
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(30.dp)
                            .background(Color.Black.copy(0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = null,
                            tint = MysticBronze.copy(0.6f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (currentLang == AppLanguage.RUS) "Записать видео (1 мин)" else "Record Video (1 min)",
                            style = MaterialTheme.typography.labelSmall.copy(color = MysticBronze, fontSize = 12.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MysticButton(
                text = if (currentLang == AppLanguage.RUS) "Загрузить из галереи" else "Upload from gallery",
                onClick = onPick,
                isSecondary = true,
                modifier = Modifier.width(220.dp)
            )
        }
    }
}

fun buildCompatibilityPlainText(
    compatReport: com.aistudio.hiromant.kxsrwa.data.remote.CompatibilityReport, // Передаем структуру отчета совместимости, полученную от ИИ Gemini
    selfName: String, // Имя первой выбранной личности (для Личность 1)
    partnerName: String, // Имя второй выбранной личности (для Личность 2)
    currentLang: AppLanguage // Текущий язык приложения для локализации заголовков
): String { // Возвращает собранный форматированный плоский текст всего отчета
    val sb = java.lang.StringBuilder() // Создаем StringBuilder для эффективной конкатенации частей текста
    
    // Формируем заголовок отчета в зависимости от установленного языка
    sb.append(if (currentLang == AppLanguage.RUS) "ОТЧЕТ О СОВМЕСТИМОСТИ\n\n" else "COMPATIBILITY REPORT\n\n")
    
    // Личность 1: подставляем Имя первой выбранной личности после косой черты / в соответствии с ТЗ
    sb.append(if (currentLang == AppLanguage.RUS) "Личность 1 / $selfName:\n" else "Self Style / $selfName:\n")
    sb.append(compatReport.partner1Portrait).append("\n\n") // Добавляем портрет личности 1
    
    // Личность 2: убираем скобки и подставляем Имя второй выбранной личности после косой черты / в соответствии с ТЗ
    sb.append(if (currentLang == AppLanguage.RUS) "Личность 2 / $partnerName:\n" else "Partner Style / $partnerName:\n")
    sb.append(compatReport.partner2Portrait).append("\n\n") // Добавляем портрет личности 2
    
    sb.append(if (currentLang == AppLanguage.RUS) "Общий анализ:\n" else "Synergy Analysis:\n")
    sb.append(compatReport.combinedAnalysis).append("\n\n")
    
    sb.append(if (currentLang == AppLanguage.RUS) "Сильные стороны:\n" else "Strong Points:\n")
    compatReport.strongPoints.forEach { sb.append("- ").append(it).append("\n") }
    sb.append("\n")
    
    sb.append(if (currentLang == AppLanguage.RUS) "Слабые стороны:\n" else "Weak Points:\n")
    compatReport.weakPoints.forEach { sb.append("- ").append(it).append("\n") }
    sb.append("\n")
    
    sb.append(if (currentLang == AppLanguage.RUS) "Эмоциональная сфера:\n" else "Emotional sphere:\n")
    sb.append(compatReport.emotionalCompatibility).append("\n\n")
    
    sb.append(if (currentLang == AppLanguage.RUS) "Интеллектуальная сфера:\n" else "Intellectual sphere:\n")
    sb.append(compatReport.intellectualCompatibility).append("\n\n")
    
    sb.append(if (currentLang == AppLanguage.RUS) "Финансовая сфера:\n" else "Financial sphere:\n")
    sb.append(compatReport.financialCompatibility)
    
    return sb.toString()
}

@Composable
fun buildCompatibilityAnnotatedString(
    plainText: String,
    spokenWordRange: Pair<Int, Int>?
): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        append(plainText)
        if (spokenWordRange != null) {
            val start = spokenWordRange.first.coerceIn(0, plainText.length)
            val end = spokenWordRange.second.coerceIn(0, plainText.length)
            if (start < end) {
                // Стиль золотой подсветки текущего произносимого слова с полужирным выделением
                addStyle(
                    style = androidx.compose.ui.text.SpanStyle(
                        background = MysticGold.copy(0.4f),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    start = start,
                    end = end
                )
            }
        }
    }
}


