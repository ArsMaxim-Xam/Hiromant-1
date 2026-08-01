package com.aistudio.hiromant.kxsrwa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Индивидуальная темная цветовая схема Material Design 3 для создания аутентичной мистической атмосферы
private val MysticColorScheme = darkColorScheme(
    primary = MysticGold, // Акцентный золотой цвет для кнопок, обводок и важных элементов
    onPrimary = Color.Black, // Черный цвет текста поверх золотых элементов для высокой контрастности
    secondary = MysticBronze, // Бронзовый цвет для второстепенных действий и декораций
    onSecondary = Color.White, // Белый цвет текста поверх бронзовых элементов
    tertiary = MysticSecondary, // Серебристый оттенок для неактивных/третьих по важности элементов
    onTertiary = Color.White, // Белый цвет поверх третичных элементов
    background = MysticDarkBackground, // Основной глубокий темный цвет фона всего приложения
    onBackground = Color(0xFFE2E2EC), // Светло-серый цвет для основного текста поверх фона
    surface = MysticDarkSurface, // Темно-коричневый цвет поверхностей (карточек, диалогов)
    onSurface = Color(0xFFE2E2EC), // Светло-серый цвет текста поверх карточек
    surfaceVariant = MysticDarkSurfaceVariant, // Альтернативный цвет поверхностей
    onSurfaceVariant = Color(0xFFC8C8DB), // Светлый оттенок текста для подписей внутри карточек
    outline = MysticGold, // Контурные рамки окрашиваются золотом
    error = Color(0xFFCF6679), // Нежно-красный цвет для индикации ошибок и предупреждений сбоя
    onError = Color.Black // Черный цвет текста поверх предупреждений об ошибках
)

// Главная Compose-функция темы приложения, оборачивающая все экраны
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit // Дочерние компоненты (весь UI приложения)
) {
    MaterialTheme(
        colorScheme = MysticColorScheme, // Применение мистической темной цветовой схемы
        typography = Typography, // Назначение настроек типографики и шрифтов
        content = content // Отрисовка внутреннего содержимого экрана
    )
}
