package com.aistudio.hiromant.kxsrwa

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.aistudio.hiromant.kxsrwa.data.local.PalmistDatabase
import com.aistudio.hiromant.kxsrwa.data.repository.PalmistRepository
import com.aistudio.hiromant.kxsrwa.utils.AppLogger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter

class PalmistApplication : Application() {

    // Свойство базы данных Room с отложенной инициализацией
    lateinit var database: PalmistDatabase
        private set // Закрытый сеттер извне класса

    // Свойство репозитория для доступа к БД и ИИ-сервису
    lateinit var repository: PalmistRepository
        private set // Закрытый сеттер извне класса

    override fun onCreate() {
        super.onCreate() // Вызов родительского метода инициализации
        
        // Инициализация кастомного регистратора логов AppLogger на старте приложения
        AppLogger.init(this)
        AppLogger.i("PalmistApplication", "Запуск onCreate приложения. Загружены настройки компиляции.")
        
        // Настройка глобального обработчика непредвиденных сбоев (сбои/краши приложения)
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter() // Буфер для записи стека ошибки
                val pw = PrintWriter(sw) // Поток записи
                throwable.printStackTrace(pw) // Сброс трейса ошибки в буфер
                val stackTrace = sw.toString() // Преобразование в строку
                
                // Запись лога фатального сбоя в AppLogger
                AppLogger.e("UncaughtCrash", "Сбой в потоке ${thread.name}: ${throwable.message}", throwable)
                
                // Синхронное сохранение лога сбоя в SharedPreferences перед падением процесса
                val sharedPrefs = getSharedPreferences("palmist_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putString("last_crash_log", stackTrace).commit()
                
                // Запись отчета о падении во внешний файл crash.log для оффлайн анализа разработчиками
                val logDir = getExternalFilesDir(null)
                if (logDir != null && (logDir.exists() || logDir.mkdirs())) {
                    val logFile = File(logDir, "crash.log")
                    val writer = FileWriter(logFile, true) // Открытие файла в режиме добавления строк
                    writer.write("\n--- Фатальный сбой на ${System.currentTimeMillis()} ---\n")
                    writer.write(stackTrace)
                    writer.write("\n--- Конец трейса сбоя ---\n\n")
                    writer.flush() // Сброс буфера в файл
                    writer.close() // Закрытие писателя
                }
            } catch (e: Exception) {
                e.printStackTrace() // Печать внутренней ошибки обработки краша в стандартный поток
            }
            
            // Передача управления стандартному системному обработчику сбоев ОС Android
            oldHandler?.uncaughtException(thread, throwable)
        }

        // Объект миграции со 2 на 3 версию базы данных Room для добавления таблицы истории платежей (сохраняет старые данные)
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Создаем таблицу истории платежей, если она еще не создана
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payment_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `amountRub` INTEGER NOT NULL,
                        `paymentSystem` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `readingType` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Объект миграции с 8 на 9 версию базы данных Room для добавления колонки сохранения дополнительных вопросов и ответов
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Выполнение SQL-запроса на добавление колонки followUpQuestionsJson в таблицу readings
                db.execSQL("ALTER TABLE `readings` ADD COLUMN `followUpQuestionsJson` TEXT DEFAULT NULL")
            }
        }

        try {
            AppLogger.i("PalmistApplication", "Инициализация экземпляра PalmistDatabase...")
            // Сборка инстанса базы данных Room с поддержкой миграции и откатом к деструктивной схеме
            @Suppress("DEPRECATION")
            database = Room.databaseBuilder(
                applicationContext,
                PalmistDatabase::class.java,
                "palmist_database"
            )
            .addMigrations(MIGRATION_2_3, MIGRATION_8_9) // Регистрация объектов миграции БД
            .fallbackToDestructiveMigration(true) // Резервное восстановление с удалением таблиц при изменении схемы БД
            .build()
            AppLogger.i("PalmistApplication", "База данных PalmistDatabase успешно собрана.")

            AppLogger.i("PalmistApplication", "Инициализация экземпляра PalmistRepository...")
            // Создание единственного экземпляра репозитория
            repository = PalmistRepository(applicationContext, database.palmistDao())
            AppLogger.i("PalmistApplication", "Репозиторий PalmistRepository успешно собран.")
        } catch (e: Exception) {
            AppLogger.e("PalmistApplication", "Ошибка при сборке БД или Репозитория", e)
            e.printStackTrace()
        }

        // Инициализация начального состояния биллинга в фоне (запуск первого набора бесплатных сеансов)
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch { // Использование глобальной области для независимой инициализации
            try {
                if (::repository.isInitialized) { // Проверка готовности репозитория
                    AppLogger.i("PalmistApplication", "Проверка и наполнение биллинга по умолчанию...")
                    repository.initializeBillingStateIfEmpty() // Начисление стартовых лимитов при первом старте
                    AppLogger.i("PalmistApplication", "Состояние биллинга успешно проинициализировано.")
                }
            } catch (e: Throwable) {
                AppLogger.e("PalmistApplication", "Инициализация состояния биллинга завершилась сбоем", e)
                e.printStackTrace()
            }
        }
    }
}
