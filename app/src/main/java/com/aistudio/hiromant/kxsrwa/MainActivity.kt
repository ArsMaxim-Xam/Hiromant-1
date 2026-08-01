package com.aistudio.hiromant.kxsrwa

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.aistudio.hiromant.kxsrwa.data.local.UserProfileEntity
import com.aistudio.hiromant.kxsrwa.ui.PalmistViewModel
import com.aistudio.hiromant.kxsrwa.ui.language.AppLanguage
import com.aistudio.hiromant.kxsrwa.ui.language.LocalizedStrings
import com.aistudio.hiromant.kxsrwa.ui.screens.*
import com.aistudio.hiromant.kxsrwa.ui.theme.MyApplicationTheme
import com.aistudio.hiromant.kxsrwa.ui.theme.MysticBronze
import com.aistudio.hiromant.kxsrwa.ui.theme.MysticDarkBackground
import com.aistudio.hiromant.kxsrwa.ui.theme.MysticDarkSurface
import com.aistudio.hiromant.kxsrwa.ui.theme.MysticGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter

// Главная активность приложения «Хиромант», управляющая жизненным циклом и навигацией в Compose
class MainActivity : ComponentActivity() {

    // Внедрение общей ViewModel для работы со всеми экранами приложения через делегат viewModels()
    private val viewModel: PalmistViewModel by viewModels()

    // Срабатывает при изменении фокуса окна (например, при сворачивании или возврате в приложение)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus) // Вызов базовой реализации активности
        if (hasFocus) { // Если окно получило фокус ввода
            try {
                // Получение контроллера системных инсетов для принудительного показа статус-бара и навигации
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars()) // Отображение системных панелей
            } catch (e: Exception) {
                // Логирование ошибок при попытке работы с системным интерфейсом
                Log.e("MainActivity", "Не удалось отобразить системные панели при изменении фокуса: ${e.message}")
            }
        }
    }

    // Точка входа в активность при создании экрана
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Вызов базового метода onCreate
        enableEdgeToEdge() // Активация полноэкранного режима отрисовки (Edge-to-Edge)

        try {
            // Показ системных статус-баров и навигационных кнопок устройства
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } catch (e: Exception) {
            // Ошибка при инициализации Edge-to-Edge отображения
            Log.e("MainActivity", "Ошибка при настройке Edge-to-Edge: ${e.message}")
        }

        // Запрос динамического разрешения на отправку уведомлений для Android 13+ (API 33)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS // Выбор нужного системного разрешения
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Запуск системного диалога запроса прав у пользователя
                requestPermissions(arrayOf(permission), 101)
            }
        }

        // Проверка наличия сохраненных логов падения из предыдущей сессии работы приложения
        val sharedPrefs = getSharedPreferences("palmist_prefs", Context.MODE_PRIVATE)
        val lastCrash = sharedPrefs.getString("last_crash_log", null) // Чтение последнего зарегистрированного краша

        if (lastCrash != null) { // Если приложение упало в прошлый раз, открываем защитный экран диагностики
            setContent {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Color(0xFFD4AF37), // Фирменный золотой акцент
                        background = Color(0xFF0C0C14), // Глубокий темный фон
                        surface = Color(0xFF141420) // Темный цвет карточек
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize() // На весь экран
                            .background(Color(0xFF0C0C14)) // Окраска фона
                            .padding(24.dp) // Внутренние отступы безопасности
                            .statusBarsPadding() // Отступ сверху под статус-бар
                            .navigationBarsPadding(), // Отступ снизу под системные кнопки
                        contentAlignment = Alignment.Center // Центрирование контента
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, // Выравнивание по горизонтали по центру
                            verticalArrangement = Arrangement.Center, // Выравнивание по вертикали по центру
                            modifier = Modifier.fillMaxSize() // Заполнение всего пространства колонкой
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning, // Иконка предупреждения об ошибке
                                contentDescription = null, // Описание отсутствует
                                tint = Color(0xFFCF6679), // Красный цвет ошибки
                                modifier = Modifier.size(64.dp) // Размер иконки 64dp
                            )
                            Spacer(modifier = Modifier.height(16.dp)) // Пробел между элементами
                            Text(
                                text = "Произошел сбой приложения", // Заголовок ошибки
                                style = MaterialTheme.typography.titleLarge, // Крупный шрифт
                                color = Color.White, // Белый цвет текста
                                textAlign = TextAlign.Center // Выравнивание по центру
                            )
                            Spacer(modifier = Modifier.height(8.dp)) // Отступ
                            Text(
                                text = "Скопируйте текст ошибки ниже и пришлите его нам для исправления:", // Пояснение для пользователя
                                style = MaterialTheme.typography.bodyMedium, // Средний шрифт текста
                                color = Color.Gray, // Серый цвет текста
                                textAlign = TextAlign.Center // Текст по центру
                            )
                            Spacer(modifier = Modifier.height(16.dp)) // Отступ
                            
                            // Скроллируемая область для вывода полного стектрейса ошибки (crash log)
                            Box(
                                modifier = Modifier
                                    .weight(1f) // Занимает всё доступное вертикальное пространство
                                    .fillMaxWidth() // Растягивается на всю ширину
                                    .background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp)) // Полупрозрачный черный фон со скруглением
                                    .border(1.dp, Color.Gray.copy(0.3f), RoundedCornerShape(8.dp)) // Серая рамка
                                    .padding(12.dp) // Внутренний отступ текста от краев рамки
                                    .verticalScroll(rememberScrollState()) // Добавление вертикального скролла
                            ) {
                                Text(
                                    text = lastCrash, // Вывод текста краш-лога
                                    fontFamily = FontFamily.Monospace, // Моноширинный шрифт для кода ошибки
                                    fontSize = 11.sp, // Небольшой размер шрифта
                                    color = Color(0xFFCF6679) // Выделение текста ошибки красным оттенком
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp)) // Отступ
                            
                            val context = LocalContext.current // Получение текущего Android-контекста
                            
                            // Кнопка для отправки отчета по электронной почте разработчику
                            Button(
                                onClick = {
                                    try {
                                        // Попытка запуска почтового интента
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                            data = android.net.Uri.parse("mailto:") // Схема почты
                                            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("ArsMaxim@gmail.com")) // Адрес разработчика
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Crash Report: Palmist App") // Тема письма
                                            putExtra(android.content.Intent.EXTRA_TEXT, lastCrash) // Вложение текста лога ошибки
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Отправить отчет...")) // Выбор почтового приложения
                                    } catch (ex: Exception) {
                                        try {
                                            // Резервный способ отправки простым текстовым интентом (если ACTION_SENDTO не поддерживается)
                                            val backupIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain" // Текстовый тип
                                                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("ArsMaxim@gmail.com"))
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Crash Report: Palmist App")
                                                putExtra(android.content.Intent.EXTRA_TEXT, lastCrash)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(backupIntent, "Отправить отчет..."))
                                        } catch (e: Exception) {
                                            // Вывод сообщения в случае отсутствия почтовых программ на устройстве
                                            Toast.makeText(context, "Не удалось открыть почтовый клиент", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679)), // Красная кнопка
                                modifier = Modifier
                                    .fillMaxWidth() // Растянуть по ширине
                                    .padding(bottom = 8.dp) // Нижний отступ кнопки
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email, // Иконка письма
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Отправить отчет об ошибке разработчику\n(ArsMaxim@gmail.com)", // Призыв к отправке
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(), // Строка во всю ширину
                                horizontalArrangement = Arrangement.spacedBy(12.dp) // Расстояние между кнопками 12dp
                            ) {
                                // Кнопка быстрого копирования текста лога в буфер обмена
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Crash Log", lastCrash)
                                        clipboard.setPrimaryClip(clip) // Сохранение в буфер
                                        Toast.makeText(context, "Лог скопирован", Toast.LENGTH_SHORT).show() // Подтверждение пользователю
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MysticBronze), // Бронзовая кнопка
                                    modifier = Modifier.weight(1f) // Занимает половину строки
                                ) {
                                    Text("Скопировать")
                                }
                                
                                // Кнопка очистки лога краша и повторного перезапуска приложения
                                Button(
                                    onClick = {
                                        sharedPrefs.edit().remove("last_crash_log").commit() // Удаление флага падения
                                        val intent = intent // Получение текущего интента запуска
                                        finish() // Закрытие текущей активности
                                        startActivity(intent) // Запуск активности заново с чистого листа
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MysticGold), // Золотая кнопка
                                    modifier = Modifier.weight(1f) // Занимает вторую половину строки
                                ) {
                                    Text("Сбросить и запустить", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
            return // Завершение выполнения onCreate во избежание отрисовки основного интерфейса поверх экрана сбоя
        }

        // Штатный путь запуска приложения (обернутый в блок try-catch безопасности)
        try {
            setContent {
                MyApplicationTheme {
                    val navController = rememberNavController() // Создание контроллера навигации Compose
                    val fontScaleValue by viewModel.fontScale.collectAsState() // Наблюдение за пользовательским размером шрифта
                    val density = androidx.compose.ui.platform.LocalDensity.current // Текущая плотность пикселей экрана
                    // Создание кастомного Density для динамического масштабирования шрифтов по ползунку в настройках
                    val customDensity = remember(density, fontScaleValue) {
                        object : androidx.compose.ui.unit.Density by density {
                            override val fontScale: Float
                                get() = density.fontScale * fontScaleValue // Корректировка масштаба системного шрифта
                        }
                    }
                    // Предоставление кастомной плотности шрифтов для всего дерева Compose-интерфейса ниже
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalDensity provides customDensity
                    ) {
                        // Определение стартового экрана навигации: если язык выбран, идем на Сплеш, иначе на экран Языка
                        val startDest = remember {
                            if (viewModel.isLanguageSelected()) "splash" else "language"
                        }

                        val navBackStackEntry by navController.currentBackStackEntryAsState() // Наблюдение за текущим стэком переходов
                        val currentRoute = navBackStackEntry?.destination?.route // Название текущего маршрута экрана

                        // Определение необходимости показа нижнего меню (Bottom Navigation Bar) на разных экранах
                        val showBottomBar = remember(currentRoute) {
                            currentRoute in listOf("main_container", "result", "billing", "settings", "loading")
                        }

                        // Подписка на изменение выбранного языка на верхнем уровне
                        val currentLang by viewModel.selectedLanguage.collectAsState() // Получение текущего языка приложения
                        // Загрузка строковых ресурсов для текущего выбранного языка
                        val strings = LocalizedStrings.get(currentLang) // Загрузка строковых ресурсов для текущего языка
                        // Подписка на изменение активной вкладки нижнего меню на верхнем уровне
                        val activeTab by viewModel.activeTab.collectAsState() // Отслеживание текущей активной вкладки в контейнере
                        // Подписка на состояние биллинга на верхнем уровне для корректной работы бейджей в нижнем меню
                        val billingStateVal by viewModel.billingState.collectAsState() // Получение баланса анализов во избежание сбоев рекомпозиции внутри циклов

                        Scaffold(
                            topBar = {}, // Верхняя панель скрыта согласно требованиям лаконичного дизайна
                            bottomBar = {
                                if (showBottomBar) { // Отрисовка нижнего меню навигации, если флаг равен true
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth() // Во всю ширину экрана
                                            .background(MysticDarkSurface) // Глубокий темный фон панели меню
                                            .border(
                                                width = 1.dp,
                                                color = MysticBronze.copy(0.2f), // Тонкая бронзовая рамка сверху
                                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                            )
                                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) // Скругление верхних углов меню
                                            .navigationBarsPadding() // Отступ снизу под системную панель жестов устройства
                                            .height(60.dp) // Высота нижней панели навигации
                                            .padding(horizontal = 2.dp), // Горизонтальные отступы внутри меню
                                        verticalAlignment = Alignment.CenterVertically, // Выравнивание иконок по центру вертикали
                                        horizontalArrangement = Arrangement.SpaceAround // Равномерное распределение кнопок по ширине
                                    ) {
                                        // Определение списка вкладок: Идентификатор, Текстовая метка, Иконка (активная/неактивная)
                                        val tabs = listOf(
                                            Triple("upload", strings.navScan, if (activeTab == "upload") Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome),
                                            Triple("compatibility", strings.navCompat, if (activeTab == "compatibility") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder),
                                            Triple("user_cabinet", if (currentLang == AppLanguage.RUS) "Кабинет" else "Cabinet", if (activeTab == "user_cabinet") Icons.Filled.Person else Icons.Outlined.Person),
                                            Triple("settings", if (currentLang == AppLanguage.RUS) "Настройки" else "Settings", if (activeTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings),
                                            Triple("billing", if (currentLang == AppLanguage.RUS) "+Р" else "+P", if (activeTab == "billing") Icons.Filled.AddCard else Icons.Outlined.AddCard)
                                        )

                                        tabs.forEach { (tabId, label, icon) ->
                                            // Проверка, является ли данная вкладка выбранной
                                            val isSelected = if (currentRoute == "main_container") activeTab == tabId else {
                                                if (currentRoute == "settings" && tabId == "settings") true
                                                else if (currentRoute == "billing" && tabId == "billing") true
                                                else if (currentRoute == "result" && tabId == "upload") true
                                                else false
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f) // Каждая вкладка занимает равную долю ширины
                                                    .clickable(
                                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                        indication = null, // Отключение стандартного серого эффекта нажатия для мистического вида
                                                        onClick = {
                                                            viewModel.activeTab.value = tabId // Переключение активной вкладки во ViewModel
                                                            if (tabId == "billing") {
                                                                navController.navigate("billing")
                                                            } else if (currentRoute != "main_container") {
                                                                // Возврат на главный экран-контейнер при нажатии на вкладку из других окон
                                                                navController.navigate("main_container") {
                                                                    popUpTo("main_container") { inclusive = false }
                                                                }
                                                            }
                                                        }
                                                    )
                                                    .padding(vertical = 4.dp), // Вертикальные отступы внутри кнопки вкладки
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                // Контейнер для иконки кабинета с крупными цифрами счетчиков без овалов и кругов
                                                if (tabId == "user_cabinet") {
                                                    // Получаем количество доступных бесплатных и платных анализов
                                                    val freeCount = billingStateVal?.freeAnalyses ?: 0
                                                    val paidCount = billingStateVal?.paidAnalyses ?: 0

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        // а) Зеленый счетчик бесплатных анализов (слева) - цифры без кругов, размером с иконку
                                                        Text(
                                                            text = freeCount.toString(),
                                                            color = Color(0xFF00FF66),
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )

                                                        Spacer(modifier = Modifier.width(4.dp))

                                                        // Иконка профиля пользователя
                                                        Icon(
                                                            imageVector = icon,
                                                            contentDescription = null,
                                                            tint = if (isSelected) MysticGold else Color.Gray,
                                                            modifier = Modifier.size(18.dp)
                                                        )

                                                        Spacer(modifier = Modifier.width(4.dp))

                                                        // б) Фиолетовый счетчик полных анализов (справа) - цифры без кругов, размером с иконку
                                                        Text(
                                                            text = paidCount.toString(),
                                                            color = Color(0xFFE040FB),
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) MysticGold else Color.Gray,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp)) // Отступ между иконкой и надписью
                                                Text(
                                                    text = label, // Название пункта меню
                                                    fontSize = 8.sp, // Очень компактный размер текста под иконкой
                                                    color = if (isSelected) MysticGold else Color.Gray, // Золотая подсветка активной подписи
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, // Жирный шрифт для активного пункта
                                                    maxLines = 1, // В одну строку
                                                    overflow = TextOverflow.Ellipsis // Сжатие при переполнении
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            contentWindowInsets = WindowInsets(0, 0, 0, 0), // Устранение лишних дефолтных отступов Scaffold
                            modifier = Modifier.fillMaxSize()
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize() // На весь экран
                                    .background(MysticDarkBackground) // Установка фирменного фона под всеми экранами
                                    .padding(innerPadding) // Уважение безопасных областей Scaffold
                            ) {
                                // Конфигурация NavHost: Хост навигации, управляющий всеми экранами в приложении
                                NavHost(
                                    navController = navController,
                                    startDestination = startDest, // Установка вычисленного стартового маршрута
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // 1. Экран выбора языка интерфейса при первом запуске
                                    composable("language") {
                                        LanguageSelectionScreen(
                                            viewModel = viewModel,
                                            onNavigateToSplash = {
                                                val isAlreadySelected = viewModel.isLanguageSelected()
                                                viewModel.markLanguageSelected() // Сохранение факта выбора языка в настройки
                                                if (isAlreadySelected) {
                                                    navController.popBackStack() // Если меняли в настройках, просто возвращаемся
                                                } else {
                                                    // Если это первый выбор, идем на Сплеш-заставку
                                                    navController.navigate("splash") {
                                                        popUpTo("language") { inclusive = true } // Удаление экрана языка из стэка
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    // 2. Сплеш-экран с анимацией разворачивания пергамента и проявлением логотипа
                                    composable("splash") {
                                        MysticSplashScreen(
                                            viewModel = viewModel,
                                            onNavigateNext = {
                                                val allProfiles = viewModel.allUserProfiles.value
                                                val currentProfile = viewModel.userProfile.value
                                                val hasExistingUsers = (currentProfile != null && currentProfile.name.isNotBlank()) || allProfiles.any { it.name.isNotBlank() }

                                                if (hasExistingUsers) {
                                                    navController.navigate("user_select") {
                                                        popUpTo("splash") { inclusive = true }
                                                    }
                                                } else {
                                                    navController.navigate("auth") {
                                                        popUpTo("splash") { inclusive = true }
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    // 2.2. Экран выбора пользователя (если заходит не в первый раз)
                                    composable("user_select") {
                                        androidx.activity.compose.BackHandler {
                                            navController.navigate("splash") {
                                                popUpTo("user_select") { inclusive = true }
                                            }
                                        }
                                        com.aistudio.hiromant.kxsrwa.ui.screens.UserSelectScreen(
                                            viewModel = viewModel,
                                            onSelectUser = { profile ->
                                                if (profile.name.isNotBlank()) {
                                                    navController.navigate("main_container") {
                                                        popUpTo("user_select") { inclusive = true }
                                                    }
                                                } else {
                                                    navController.navigate("profile") {
                                                        popUpTo("user_select") { inclusive = true }
                                                    }
                                                }
                                            },
                                            onNewUser = {
                                                navController.navigate("auth")
                                            },
                                            onNavigateNext = {
                                                navController.navigate("main_container") {
                                                    popUpTo("user_select") { inclusive = true }
                                                }
                                            }
                                        )
                                    }

                                    // 2.5. Экран авторизации и регистрации пользователя ("Введите данные")
                                    composable("auth") {
                                        androidx.activity.compose.BackHandler {
                                            val allProfiles = viewModel.allUserProfiles.value
                                            if (allProfiles.any { it.name.isNotBlank() }) {
                                                navController.navigate("user_select") {
                                                    popUpTo("auth") { inclusive = true }
                                                }
                                            } else {
                                                navController.navigate("splash") {
                                                    popUpTo("auth") { inclusive = true }
                                                }
                                            }
                                        }
                                        AuthScreen(
                                            viewModel = viewModel,
                                            onNavigateNext = {
                                                val profile = viewModel.userProfile.value
                                                if (profile != null && profile.name.isNotBlank()) {
                                                    navController.navigate("main_container") {
                                                        popUpTo("auth") { inclusive = true }
                                                    }
                                                } else {
                                                    navController.navigate("profile") {
                                                        popUpTo("auth") { inclusive = true }
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    // 3. Экран заполнения анкеты профиля (имя, пол, возраст, рост, доминантная рука)
                                    composable("profile") {
                                        androidx.activity.compose.BackHandler {
                                            navController.navigate("auth") {
                                                popUpTo("profile") { inclusive = true }
                                            }
                                        }
                                        ProfileScreen(
                                            viewModel = viewModel,
                                            onNavigateNext = {
                                                // После заполнения переходим на главный дашборд
                                                navController.navigate("main_container") {
                                                    popUpTo("profile") { inclusive = true } // Удаление экрана ввода анкеты из стэка
                                                }
                                            },
                                            onNavigateBack = {
                                                val pVal = viewModel.userProfile.value
                                                if (pVal != null && pVal.name.isNotBlank()) {
                                                    navController.navigate("main_container") {
                                                        popUpTo("profile") { inclusive = true }
                                                    }
                                                } else {
                                                    navController.navigate("auth") {
                                                        popUpTo("profile") { inclusive = true }
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    // 4. Главный контейнер со вкладками (Сканирование ладоней, Совместимость, Кабинет, Настройки, О приложении)
                                    composable("main_container") {
                                        MainContainerScreen(
                                            viewModel = viewModel,
                                            onNavigateToLoading = { navController.navigate("loading") }, // Переход на экран ИИ-расчетов
                                            onNavigateToResult = { navController.navigate("result") }, // Переход на экран просмотра результата
                                            onNavigateToBilling = { navController.navigate("billing") }, // Переход на экран покупки баланса
                                            onNavigateToLanguage = { navController.navigate("language") }, // Смена языка
                                            onNavigateToVideoScan = { navController.navigate("video_scan") }, // Переход к записи видео ладоней
                                            onNavigateToSplash = {
                                                navController.navigate("splash") {
                                                    popUpTo("main_container") { inclusive = true }
                                                }
                                            },
                                            onNavigateToProfile = {
                                                navController.navigate("profile") // Переход на страницу Ввода данных
                                            }
                                        )
                                    }

                                    // 5. Экран записи короткого видео движения рук (дополнительная разблокировка анализа)
                                    composable("video_scan") {
                                        PostPaymentVideoScreen(
                                            viewModel = viewModel,
                                            onNavigateToLoading = { },
                                            onNavigateToResult = {
                                                navController.navigate("result") {
                                                    popUpTo("video_scan") { inclusive = true }
                                                }
                                            },
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }

                                    // 6. Прогрессивный экран загрузки и отправки промптов с мистическими предсказаниями в реальном времени
                                    composable("loading") {
                                        val isAnalyzing by viewModel.isAnalyzing.collectAsState() // Флаг работы генерации ИИ
                                        
                                        androidx.activity.compose.BackHandler {
                                            viewModel.isAnalyzing.value = false
                                            navController.navigate("profile") {
                                                popUpTo("loading") { inclusive = true }
                                            }
                                        }

                                        LaunchedEffect(isAnalyzing) {
                                            if (!isAnalyzing) { // Как только ИИ закончил анализ ладони
                                                val compReading = viewModel.currentCompatibilityReading.value
                                                if (compReading != null && compReading.analysisType == "compatibility") {
                                                    // Если рассчитывали совместимость, переходим во вкладку Совместимости
                                                    viewModel.activeTab.value = "compatibility"
                                                    navController.navigate("main_container") {
                                                        popUpTo("loading") { inclusive = true } // Закрываем экран загрузки
                                                    }
                                                } else {
                                                    // Если делали обычный анализ ладони, переходим на экран результатов
                                                    navController.navigate("result") {
                                                        popUpTo("loading") { inclusive = true } // Закрываем экран загрузки
                                                    }
                                                }
                                            }
                                        }

                                        // Отображение анимированного мистического лоадера с фазами луны
                                        MysticLoadingScreen(viewModel = viewModel)
                                    }

                                    // 7. Экран результатов ИИ-анализа ладони с подробным интерактивным атласом линий
                                    composable("result") {
                                        ResultsScreen(
                                            viewModel = viewModel,
                                            onNavigateToCompatibility = {
                                                // Навигация на главный контейнер (на вкладку совместимости)
                                                navController.navigate("main_container")
                                            },
                                            onNavigateToBilling = { navController.navigate("billing") }, // Открыть магазин баланса
                                            onClose = {
                                                // Возврат на главный экран сканирования
                                                navController.navigate("main_container") {
                                                    popUpTo("main_container") { inclusive = true }
                                                }
                                            }
                                        )
                                    }

                                    // 8. Экран биллинга / магазина покупок (ЮKassa/ЮMoney СБП, Google Play Billing)
                                    composable("billing") {
                                        BillingScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { navController.popBackStack() } // Закрытие экрана
                                        )
                                    }

                                    // 9. Экран настроек приложения (смена языка, размер шрифта, очистка истории, логирование)
                                    composable("settings") {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            onNavigateToLanguage = { navController.navigate("language") }, // Смена языка
                                            onNavigateBack = { navController.popBackStack() } // Выход из настроек
                                        )
                                    }
                                }

                                // Фиксированная кнопка «Назад» в верхнем ЛЕВОМ углу на ВСЕХ экранах, кроме заставки (splash)
                                if (currentRoute != "splash") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .padding(top = 2.dp, start = 2.dp)
                                            .zIndex(300f)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (currentRoute == "loading") {
                                                    viewModel.isAnalyzing.value = false
                                                    navController.navigate("profile") {
                                                        popUpTo("loading") { inclusive = true }
                                                    }
                                                } else if (currentRoute == "profile") {
                                                    navController.navigate("auth") {
                                                        popUpTo("profile") { inclusive = true }
                                                    }
                                                } else if (currentRoute == "auth") {
                                                    navController.navigate("splash") {
                                                        popUpTo("auth") { inclusive = true }
                                                    }
                                                } else if (currentRoute == "main_container") {
                                                    if (viewModel.showInterpretationScreen.value) {
                                                        viewModel.showInterpretationScreen.value = false
                                                    } else if (activeTab != "upload") {
                                                        viewModel.activeTab.value = "upload" // Возврат на главную вкладку сканирования
                                                    } else {
                                                        viewModel.isReturnedToSplash.value = true
                                                        navController.navigate("splash") {
                                                            popUpTo("main_container") { inclusive = true }
                                                        }
                                                    }
                                                } else if (currentRoute == "result") {
                                                    navController.navigate("main_container") {
                                                        popUpTo("main_container") { inclusive = true }
                                                    }
                                                } else {
                                                    if (navController.previousBackStackEntry != null) {
                                                        navController.popBackStack()
                                                    } else {
                                                        navController.navigate("main_container") {
                                                            popUpTo("main_container") { inclusive = true }
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .offset(x = (-13).dp, y = (-13).dp)
                                                .size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Назад",
                                                tint = MysticGold,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            // Перехват любых критических ошибок на этапе инициализации приложения
            logException(e) // Запись стектрейса ошибки в локальный файл crash.log
            val logFile = File(getExternalFilesDir(null), "crash.log")
            Toast.makeText(
                this,
                "Ошибка запуска: ${e.message}\nЛог записан в ${logFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show() // Показ уведомления пользователю
            throw e // Проброс ошибки дальше в ОС для корректного завершения процесса
        }
    }

    // Вспомогательный метод для подробного логирования сбоев во внешний текстовый файл
    private fun logException(e: Throwable) {
        try {
            val logDir = getExternalFilesDir(null) // Путь к внешней папке файлов приложения
            if (logDir != null && (logDir.exists() || logDir.mkdirs())) {
                val logFile = File(logDir, "crash.log") // Создание или открытие файла crash.log
                val writer = FileWriter(logFile, true) // Открытие потока записи в режиме дозаписи в конец файла
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw) // Чтение трассировки стека ошибки в строку
                writer.write("\n--- Сбой в системе: ${System.currentTimeMillis()} ---\n") // Время возникновения ошибки
                writer.write(sw.toString()) // Текст стектрейса
                writer.write("\n--- Конец блока сбоя ---\n\n")
                writer.flush() // Запись данных из буфера на диск
                writer.close() // Закрытие потока
            }
        } catch (ex: Exception) {
            // Безопасный перехват ошибок записи лога краша во избежание бесконечного цикла падений
            Log.e("MainActivity", "Не удалось записать лог падения в файл", ex)
        }
    }
}


// Компонуемый контейнер, динамически переключающий экраны вкладок в зависимости от выбранной вкладки Bottom Bar
@Composable
fun MainContainerScreen(
    viewModel: PalmistViewModel,
    onNavigateToLoading: () -> Unit,
    onNavigateToResult: () -> Unit = {},
    onNavigateToBilling: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToVideoScan: () -> Unit,
    onNavigateToSplash: () -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val activeTab by viewModel.activeTab.collectAsState() // Подписка на изменение активной вкладки

    // Нажатие системной кнопки возврата "Назад"
    androidx.activity.compose.BackHandler {
        if (viewModel.showInterpretationScreen.value) {
            viewModel.showInterpretationScreen.value = false
        } else if (activeTab != "upload") {
            viewModel.activeTab.value = "upload" // Возврат на главный экран сканирования
        } else {
            onNavigateToProfile() // На странице загрузки материалов кнопка назад возвращает на ввод данных
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize() // На весь экран
            .background(MysticDarkBackground) // Темный мистический фон
    ) {
        // Условный выбор отображаемого экрана в зависимости от выбранного tabId
        when (activeTab) {
            "upload" -> UploadScreen(
                viewModel = viewModel,
                onNavigateToLoading = onNavigateToLoading,
                onNavigateToBilling = onNavigateToBilling,
                onNavigateToVideoScan = onNavigateToVideoScan,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToResult = onNavigateToResult
            )
            "compatibility" -> CompatibilityScreen(
                viewModel = viewModel,
                onNavigateToLoading = onNavigateToLoading,
                onNavigateToBilling = onNavigateToBilling,
                onNavigateToProfile = onNavigateToProfile
            )
            "user_cabinet" -> UserCabinetScreen(
                viewModel = viewModel,
                onNavigateToResult = onNavigateToResult, // Переход к сохраненному результату в кабинете на результат, а не загрузку
                onNavigateToBilling = onNavigateToBilling
            )
            "settings" -> SettingsScreen(
                viewModel = viewModel,
                onNavigateToLanguage = onNavigateToLanguage,
                onNavigateBack = { viewModel.activeTab.value = "upload" } // По умолчанию возвращаемся на сканирование
            )
            "about" -> AboutScreen(viewModel = viewModel) // Описание приложения, методологии и школ хиромантии
        }
    }
}
