// Настройка управления плагинами сборки (Plugin Management)
pluginManagement {
  // Определение репозиториев, откуда будут загружаться плагины Gradle
  repositories {
    // Подключение официального репозитория Google для Android-плагинов
    google {
      // Ограничение контента: запрашивать в репозитории Google только подходящие по маске группы
      content {
        includeGroupByRegex("com\\.android.*") // Загрузка плагинов от Android (AGP)
        includeGroupByRegex("com\\.google.*")  // Загрузка плагинов от компании Google
        includeGroupByRegex("androidx.*")    // Загрузка плагинов семейства AndroidX
      }
    }
    // Подключение центрального Maven-репозитория общего назначения
    mavenCentral()
    // Подключение официального портала плагинов Gradle
    gradlePluginPortal()
  }
}

// Подключение системного плагина Foojay для автоматического разрешения версий JDK в тулчейнах
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

// Настройка управления разрешением зависимостей проекта (Dependency Resolution Management)
dependencyResolutionManagement {
  // Настройка строгого режима: запрет на объявление локальных репозиториев внутри модулей build.gradle
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  // Объявление централизованных репозиториев для поиска библиотек и зависимостей модулей
  repositories {
    google()       // Использование репозитория Google для библиотек Jetpack, Compose и др.
    mavenCentral() // Использование Maven Central для сторонних библиотек и утилит
  }
}

// Чтение файла версии проекта для динамического назначения имени проекта
val versionFile = file("version.txt")
// Получение строки версии: если файл существует, считываем ее, иначе используем версию по умолчанию 1.001
val currentVersion = if (versionFile.exists()) versionFile.readText().trim() else "1.001"
// Назначение имени корневого проекта в точной кодировке по просьбе пользователя (без замены точек на подчеркивания)
rootProject.name = "Хиромант_${currentVersion}"

// Подключение модуля ":app" в структуру сборки проекта
include(":app")

