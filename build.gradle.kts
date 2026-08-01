// Конфигурационный файл верхнего уровня (Top-level build file)
// Здесь настраиваются общие плагины, используемые во всех дочерних модулях проекта.
plugins {
  // Подключение плагина сборки Android Application (AGP) без автоматического применения на корневом уровне
  alias(libs.plugins.android.application) apply false
  // Подключение плагина поддержки Jetpack Compose в языке Kotlin
  alias(libs.plugins.kotlin.compose) apply false
  // Подключение плагина KSP (Kotlin Symbol Processing) для эффективной кодогенерации
  alias(libs.plugins.google.devtools.ksp) apply false
  // Подключение плагина Roborazzi для проведения автоматизированного скриншот-тестирования
  alias(libs.plugins.roborazzi) apply false
  // Подключение плагина Secrets Gradle Plugin для безопасного хранения и инъекции API-ключей
  alias(libs.plugins.secrets) apply false
  // Подключение плагина сервисов Google Play (Firebase, Analytics и др.)
  alias(libs.plugins.google.services) apply false
}

// Регистрация кастомного таска Gradle для инкрементирования версии приложения при сборке
tasks.register("incrementVersion") {
  // Обозначение несовместимости таска с кэшем конфигурации Gradle, так как он записывает внешние файлы на диск
  notCompatibleWithConfigurationCache("Writes external files")
  // Определение логики, которая выполнится на этапе выполнения таска (Execution Phase)
  doLast {
    // Получение ссылки на файл с текущей версией проекта (version.txt)
    val versionFile = file("version.txt")
    // Чтение номера текущей версии или использование дефолтного значения 1.001 при отсутствии файла
    val currentVersion = if (versionFile.exists()) versionFile.readText().trim() else "1.001"
    // Преобразование строкового представления версии в дробное число Double
    val currentDouble = currentVersion.toDoubleOrNull() ?: 1.001
    // Вычисление следующей версии приложения путем добавления шага 0.001
    val nextDouble = currentDouble + 0.001
    // Форматирование нового значения версии обратно в строковый формат с тремя знаками после запятой
    val nextVersion = String.format(java.util.Locale.US, "%.3f", nextDouble)
    // Запись обновленной версии обратно в текстовый файл конфигурации проекта
    versionFile.writeText(nextVersion)

    // Обновление имени приложения с новой версией в файле метаданных AI Studio (metadata.json)
    val metadataFile = file("metadata.json")
    // Проверка физического существования файла метаданных на диске
    if (metadataFile.exists()) {
      // Чтение текстового содержимого файла метаданных
      val content = metadataFile.readText()
      // Замена поля "name" в JSON на новое имя с инкрементированным номером версии
      val updated = content.replace(Regex("\"name\"\\s*:\\s*\".*?\""), "\"name\": \"Хиромант $nextVersion\"")
      // Сохранение обновленного JSON-кода в файл metadata.json
      metadataFile.writeText(updated)
    }

    // Вывод диагностического сообщения в консоль сборщика о завершении операции инкремента версии
    println("SUCCESS: Version incremented from $currentVersion to $nextVersion")
  }
}

