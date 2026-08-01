package com.aistudio.hiromant.kxsrwa.ui.language

// Перечисление доступных языков интерфейса приложения «Хиромант»
enum class AppLanguage(val code: String, val label: String) {
    RUS("RU", "🇷🇺 Русский (RUS)"), // Русский язык
    ENG("EN", "🇬🇧 English (ENG)")  // Английский язык
}

// Класс данных, содержащий все локализуемые строки для многоязычного интерфейса приложения
data class PalmistStrings(
    // Кнопки нижней навигационной панели (Bottom Navigation)
    val navScan: String,     // Кнопка перехода к сканированию ладони
    val navCompat: String,   // Кнопка перехода к расчету совместимости партнеров
    val navHistory: String,  // Кнопка перехода к архиву прошлых сеансов
    val navAbout: String,    // Кнопка перехода к справочной информации

    // Общесистемные строковые ресурсы приложения
    val appName: String,     // Название приложения на экране
    val skip: String,        // Кнопка «Пропустить»
    val next: String,        // Кнопка «Далее»
    val cancel: String,      // Кнопка «Отмена»
    val save: String,        // Кнопка «Сохранить»
    val loading: String,     // Индикатор загрузки данных
    val empty: String,       // Пустое состояние
    val priceRub: String,    // Валютный символ рубля или доллара

    // Экран выбора языка (Language Screen)
    val langSelectTitle: String,     // Заголовок экрана выбора языка
    val langSelectSubtitle: String,  // Подзаголовок с разъяснением системного языка
    val langContinue: String,        // Кнопка продолжения после выбора языка

    // Приветственный экран заставки (Splash Screen)
    val splashMysticScroll: String,  // Текст анимации разворачивания свитка
    val splashTapToSkip: String,     // Подсказка для пропуска заставки по тапу
    val splashLogoSubtitle: String,  // Мистический слоган под логотипом

    // Экран авторизации и регистрации пользователя (Auth Screen)
    val authTitle: String,                  // Заголовок формы входа/регистрации
    val authSubtitle: String,               // Описание преимуществ личного профиля
    val authEmailPhonePlaceholder: String,  // Поле ввода почты или телефона
    val authPasswordPlaceholder: String,    // Поле ввода пароля аккаунта
    val authSmsEmailCodePlaceholder: String,// Поле ввода проверочного кода из SMS/почты
    val authRegisterBtn: String,            // Кнопка запуска регистрации
    val authSkipBtn: String,                // Кнопка гостевого входа без регистрации
    val authSendCodeBtn: String,            // Кнопка отправки кода подтверждения
    val authErrorInvalid: String,           // Текст ошибки некорректных учетных данных
    val authEmailPhoneError: String,        // Ошибка валидации формата почты/телефона
    val authPasswordError: String,          // Ошибка валидации минимальной длины пароля

    // Экран ввода физиологического профиля (Profile Screen)
    val profileTitle: String,         // Заголовок экрана ввода персональных данных
    val profileSubtitle: String,      // Подзаголовок о значении возраста и пола в хиромантии
    val profileNameLabel: String,     // Название поля ввода имени
    val profileNameError: String,     // Ошибка валидации длины имени
    val profileGenderLabel: String,   // Лейбл выбора гендера
    val profileGenderMale: String,    // Вариант «Мужской»
    val profileGenderFemale: String,  // Вариант «Женский»
    val profileGenderNone: String,    // Вариант «Не указывать»
    val profileAgeLabel: String,      // Поле выбора года рождения
    val profileHeightLabel: String,   // Поле ввода роста в сантиметрах
    val profileHandLabel: String,     // Лейбл выбора ведущей руки
    val profileHandLeft: String,      // Вариант ведущей руки «Левша»
    val profileHandRight: String,     // Вариант ведущей руки «Правша»
    val profileHandDescLeft: String,  // Подробное разъяснение для левшей
    val profileHandDescRight: String, // Подробное разъяснение для правшей

    // Экран фотографирования и загрузки материалов (Upload Screen)
    val uploadTitle: String,              // Заголовок экрана загрузки файлов ладони
    val uploadGuideHeader: String,        // Заголовок памятки для качественной съемки
    val uploadGuideText: String,          // Подробные правила (освещение, фон, пальцы)
    val uploadPhotoSection: String,       // Секция загрузки фотоснимков ладони
    val uploadVideoSection: String,       // Секция записи короткого видео руки
    val uploadTakePhoto: String,          // Кнопка «Сделать фото» через камеру
    val uploadGallery: String,            // Кнопка загрузки фото из галереи устройства
    val uploadRecordVideo: String,        // Кнопка включения видеозаписи
    val uploadLoadVideo: String,         // Кнопка выбора готового видео из памяти
    val uploadVideoHint: String,          // Подсказка с инструкцией по съемке видео
    val uploadPreviewPhoto: String,       // Превью успешно загруженного снимка
    val uploadPreviewVideo: String,       // Превью записанного видеофайла
    val uploadChooseAnalysisType: String, // Заголовок выбора тарифа интерпретации

    // Кнопки запуска различных видов анализа в меню тарифов (Analysis Buttons)
    val btnBriefChar: String,  // Кнопка «Краткий анализ характера»
    val btnFullChar: String,   // Кнопка «Полный анализ характера»
    val btnBriefPath: String,  // Кнопка «Краткий анализ жизненного пути»
    val btnFullPath: String,   // Кнопка «Полный анализ жизненного пути»
    val freeLabel: String,     // Метка «Бесплатно»
    val btnClose: String,      // Кнопка закрытия модального окна выбора тарифа

    // Экран прогресса анализа и генерации результатов (Loading / Processing Screen)
    val loadAnalyzeLines: String,   // Шаг разметки линий на ладони
    val loadStudyMounts: String,    // Шаг вычисления планетарных бугров
    val loadReadSigns: String,      // Шаг поиска скрытых знаков
    val loadGenPredictions: String, // Шаг составления ИИ предсказаний
    val loadMysticTitle: String,    // Верхний заголовок экрана медитации-загрузки
    val loadProgressText: String,   // Медитативный текст прогресса

    // Экран просмотра отчетов анализа ладони (Results Screen)
    val resTitle: String,              // Заголовок экрана результатов анализа
    val resTabReport: String,          // Вкладка «Текстовый отчет»
    val resTabLinesMap: String,        // Вкладка «Карта линий ладони»
    val resOverallPortrait: String,    // Раздел общего психологического портрета
    val resHandType: String,           // Тип архетипа ладони (Земля, Воздух, Вода, Огонь)
    val resLinesHeader: String,        // Подзаголовок основных линий
    val resMountsHeader: String,       // Подзаголовок планетарных холмов
    val resSignsHeader: String,        // Подзаголовок сакральных символов ладони
    val resMarriageChildren: String,   // Раздел анализа любви, брака и детей (Пункт 6)
    val resLifeEvents: String,         // Раздел анализа ключевых вех судьбы (Пункт 4)
    val resPredictions: String,        // Раздел анализа внешних сил и будущего (Пункт 5)
    val resRecommendations: String,    // Раздел духовных рекомендаций хироманта
    val resInheritedPotentials: String,// Раздел потенциалов левой руки (Пункт 1)
    val resAcquiredTraits: String,     // Раздел достижений правой руки (Пункт 2)
    val resCharacterQualities: String, // Раздел качеств характера человека (Пункт 3)
    val resAudioTitle: String,         // Текст озвучки отчета синтезатором речи (TTS)
    val resVoiceMale: String,          // Мужской голос синтеза речи
    val resVoiceFemale: String,        // Женский голос синтеза речи
    val resVoiceSpeed: String,         // Регулировка скорости чтения отчета
    val resExportPdf: String,          // Кнопка экспорта отчета в PDF-документ
    val resExportSuccess: String,      // Уведомление об успешном создании PDF
    val resBtnBuy10: String,           // Предложение покупки пакета анализов
    val resBtnBuyCompat: String,       // Предложение покупки совместимости с партнером

    // Экран вычисления синастрии и совместимости (Compatibility Screen)
    val compatTitle: String,          // Заголовок экрана совместимости
    val compatSubtitle: String,       // Подзаголовок о сравнении планетарных холмов и сердечных линий
    val compatUploadSelf: String,     // Слот для загрузки своей руки
    val compatUploadPartner: String,  // Слот для загрузки руки возлюбленного/партнера
    val compatAnalyzeBtn: String,     // Кнопка «Начать расчет»
    val compatPercentLabel: String,    // Отображение процента взаимного притяжения
    val compatCombinedTitle: String,  // Общий анализ отношений
    val compatStrongTitle: String,    // Раздел гармонии и сильных качеств пары
    val compatWeakTitle: String,      // Раздел трений, барьеров и духовных разногласий
    val compatEmotional: String,      // Степень эмоциональной гармонии
    val compatIntellectual: String,   // Степень интеллектуального единства
    val compatFinancial: String,      // Степень финансового благополучия союза

    // Экран архива и истории проведенных сканирований (History Screen)
    val histTitle: String,        // Заголовок списка истории анализов
    val histSubtitle: String,     // Подзаголовок списка истории
    val histNoRecords: String,    // Заглушка, если история сканирований пуста
    val histClearHistory: String, // Кнопка полной очистки базы данных истории

    // Экран информации о хиромантии и поддержки разработчиков (About & FAQ Screen)
    val aboutTitle: String,           // Заголовок информационного экрана
    val aboutTabInfo: String,        // Вкладка с теорией хиромантии
    val aboutTabFaq: String,          // Вкладка с частыми вопросами (FAQ)
    val aboutTabContacts: String,     // Вкладка с контактами поддержки
    val aboutHistoryPalmist: String,  // Заголовок исторической справки
    val aboutHistoryText: String,     // Текст об истории развития науки хиромантии
    val aboutTheoryLines: String,     // Заголовок интерактивной карты руки
    val aboutTheoryText: String,      // Разъяснение значений каждой из линий
    val aboutSupportBtn: String,      // Кнопка поддержки проекта донатом
    val aboutDonateTitle: String,    // Заголовок всплывающего окна доната
    val aboutDonateDesc: String,     // Описание для взноса в пользу развития ИИ
    val aboutEmailSupport: String,    // Адрес электронной почты разработчиков
    val aboutPrivacyPolicy: String,   // Ссылка на соглашение о конфиденциальности данных
    val aboutSupportSuccess: String,  // Текст благодарности при поддержке
    val appVersionLabel: String,      // Текстовая метка версии сборки

    // Экран общих настроек приложения (Settings Screen)
    val settTitle: String,        // Заголовок экрана настроек
    val settLanguage: String,     // Меню переключения локализации приложения
    val settSubStatus: String,    // Отображение уровня подписки пользователя
    val settSubActive: String,    // Индикатор активной премиум подписки
    val settSubInactive: String,  // Индикатор базового бесплатного доступа
    val settResetApp: String,     // Кнопка очистки всех кэш-файлов и сброса
    val settDeleteAcc: String,    // Кнопка полного удаления личной учетной записи

    // Окна оплаты премиум контента и подписок (Billing Dialogs)
    val billDialogTitle: String,      // Заголовок платежного окна
    val billDialogChoosePay: String,  // Подсказка выбора провайдера платежей
    val billDialogYooKassa: String,  // Кнопка оплаты через ЮKassa
    val billDialogGooglePlay: String, // Кнопка оплаты через Google Billing
    val billDialogCardNum: String,    // Поле ввода номера банковской карты
    val billDialogSuccess: String,    // Оповещение об успешном платеже
    val billDialogFail: String,       // Оповещение об отказе транзакции

    // Названия физиологических слотов ладоней (Hand Slots)
    val slotLeftPalm: String,   // Название слота «Левая ладонь»
    val slotLeftBack: String,   // Название слота «Тыл левой руки»
    val slotRightPalm: String,  // Название слота «Правая ладонь»
    val slotRightBack: String   // Название слота «Тыл правой руки»
)

object LocalizedStrings {
    private val ruStrings = PalmistStrings(
        navScan = "Хиромантия",
        navCompat = "Совместимость",
        navHistory = "История",
        navAbout = "Инфо",

        appName = "Хиромант",
        skip = "Пропустить",
        next = "Далее",
        cancel = "Отмена",
        save = "Сохранить",
        loading = "Загрузка...",
        empty = "Пусто",
        priceRub = "₽",

        langSelectTitle = "Выберите язык",
        langSelectSubtitle = "Мы определили ваш системный язык, но вы можете изменить его",
        langContinue = "Продолжить",

        splashMysticScroll = "Древний свиток разворачивается...",
        splashTapToSkip = "ПРОПУСТИТЬ ЗАСТАВКУ",
        splashLogoSubtitle = "ТАЙНЫ СУДЬБЫ, В ВАШИХ РУКАХ",

        authTitle = "Вход/Регистрация",
        authSubtitle = "Создайте профиль для сохранения истории ваших сеансов",
        authEmailPhonePlaceholder = "Email или Телефон",
        authPasswordPlaceholder = "Пароль",
        authSmsEmailCodePlaceholder = "Код подтверждения (из SMS/Email)",
        authRegisterBtn = "Зарегистрироваться",
        authSkipBtn = "Продолжить без регистрации",
        authSendCodeBtn = "Получить код",
        authErrorInvalid = "Пожалуйста, введите корректные данные",
        authEmailPhoneError = "Введите корректный E-mail или номер телефона (от 10 цифр)",
        authPasswordError = "Пароль должен быть не менее 6 символов",

        profileTitle = "Введите данные",
        profileSubtitle = "Хиромантия учитывает возраст, пол и физиологические пропорции",
        profileNameLabel = "Ваше имя",
        profileNameError = "Имя должно содержать от 2 символов",
        profileGenderLabel = "Пол",
        profileGenderMale = "Мужской",
        profileGenderFemale = "Женский",
        profileGenderNone = "Не указывать",
        profileAgeLabel = "Год рождения",
        profileHeightLabel = "Рост (см)",
        profileHandLabel = "Ведущая рука",
        profileHandLeft = "Левша",
        profileHandRight = "Правша",
        profileHandDescLeft = "У левшей, левая рука отображает активную судьбу, правая - врождённый потенциал, и наоборот.",
        profileHandDescRight = "У правшей, правая рука отображает активную судьбу, левая - врождённый потенциал, и наоборот.",

        uploadTitle = "Загрузка материалов",
        uploadGuideHeader = "ВАЖНО ДЛЯ ТОЧНОЙ ИНТЕРПРИТАЦИИ:",
        uploadGuideText = "• Снимайте на фоне кредитной карты (для определения масштаба)\n• Раздвиньте пальцы максимально широко\n• Рука расслаблена\n• При видео — медленно поворачивайте руку ладонью и тыльной стороной\n• Хорошее освещение без резких теней\n• В хиромантии важно всё: форма ногтей, пропорции пальцев, длина ладони",
        uploadPhotoSection = "📸 Фото ладони",
        uploadVideoSection = "🎥 Видео ладони (до 60 секунд)",
        uploadTakePhoto = "Сделать фото",
        uploadGallery = "Из галереи",
        uploadRecordVideo = "Записать видео",
        uploadLoadVideo = "Загрузить видео",
        uploadVideoHint = "Подсказка: Медленно поворачивайте руки ладонью и тыльной стороной, раздвиньте пальцы. Для масштаба положите рядом кредитную карту.",
        uploadPreviewPhoto = "Фото загружено",
        uploadPreviewVideo = "Видео готово (60 сек лимит)",
        uploadChooseAnalysisType = "Выберите тип интерпритации:",

        btnBriefChar = "7. Краткая интерпритация Характера и Качеств (Бесплатно)",
        btnFullChar = "8. Полная интерпритация Характера и Качеств (150 ₽)",
        btnBriefPath = "9. Краткая интерпритация Жизненного пути (Бесплатно)",
        btnFullPath = "10. Полная интерпритация Жизненного пути (150 ₽)",
        freeLabel = "Бесплатно",
        btnClose = "Закрыть",

        loadAnalyzeLines = "Интерпритация линий...",
        loadStudyMounts = "Изучение бугров...",
        loadReadSigns = "Чтение знаков...",
        loadGenPredictions = "Формирование прогноза...",
        loadMysticTitle = "Мистическая Интерпритация",
        loadProgressText = "Проявление энергетических потоков на вашей ладони...",

        resTitle = "Интерпритация",
        resTabReport = "Отчёт",
        resTabLinesMap = "Карта линий",
        resOverallPortrait = "Общий портрет личности",
        resHandType = "Тип ладони",
        resLinesHeader = "Основные линии судьбы",
        resMountsHeader = "Планетарные бугры",
        resSignsHeader = "Особые мистические знаки",
        resMarriageChildren = "6. Интерпритация Отношений Семьи, Брака, Дети и Спутники жизни",
        resLifeEvents = "4. Интерпритация Жизненного пути и событий",
        resPredictions = "5. Интерпритация Жизненных ситуаций и внешнего влияния на жизнь человека",
        resRecommendations = "Мистические рекомендации",
        resInheritedPotentials = "1. Интерпритация Левой руки (то, что заложено в человеке)",
        resAcquiredTraits = "2. Интерпритация Правой руки (то, как человек живёт и реализуется)",
        resCharacterQualities = "3. Интерпритация Характера и Качеств человека",
        resAudioTitle = "Прочитать описание (TTS)",
        resVoiceMale = "Мужской",
        resVoiceFemale = "Женский",
        resVoiceSpeed = "Скорость речи",
        resExportPdf = "Поделиться",
        resExportSuccess = "Ссылка и текст отчета готовы к отправке!",
        resBtnBuy10 = "Получить 10 полных интерпритаций за 1000 ₽",
        resBtnBuyCompat = "Узнать совместимость с партнёром за 250 ₽",

        compatTitle = "Совместимость с партнёром",
        compatSubtitle = "Сравните линии сердца, брака и планетарные бугры для оценки вашей связи",
        compatUploadSelf = "Ваше фото руки",
        compatUploadPartner = "Фото руки партнёра",
        compatAnalyzeBtn = "Рассчитать совместимость",
        compatPercentLabel = "Процент слияния душ",
        compatCombinedTitle = "Интерпритация союза",
        compatStrongTitle = "Сильные стороны связи",
        compatWeakTitle = "Слабые стороны и барьеры",
        compatEmotional = "Эмоциональная гармония",
        compatIntellectual = "Интеллектуальное единство",
        compatFinancial = "Финансовая стабильность",

        histTitle = "История Интерпритаций",
        histSubtitle = "",
        histNoRecords = "Вы ещё не проводили интерпритацию ладоней",
        histClearHistory = "Очистить историю",

        aboutTitle = "О приложении",
        aboutTabInfo = "Теория",
        aboutTabFaq = "FAQ",
        aboutTabContacts = "Поддержка",
        aboutHistoryPalmist = "История Хиромантии",
        aboutHistoryText = "Хиромантия (от греч. cheir — рука и manteia — анализ) — одна из древнейших систем предсказания, зародившаяся в Древней Индии, Китае и Египте. Великие умы древности, включая Аристотеля и Гиппократа, изучали линии рук для оценки характера и здоровья человека.",
        aboutTheoryLines = "Карта вашей ладони",
        aboutTheoryText = "Линия Жизни отражает витальность, силу и энергию.\nЛиния Сердца раскрывает эмоциональность и способность любить.\nЛиния Головы символизирует интеллект и тип мышления.\nЛиния Судьбы указывает на карьерный путь и внешние влияния.",
        aboutSupportBtn = "Поддержать разработку",
        aboutDonateTitle = "Пожертвование проекту",
        aboutDonateDesc = "Если вам нравится приложение, вы можете внести любой взнос на его поддержку и развитие.",
        aboutEmailSupport = "Связаться с поддержкой: support@palmist-mystic.com",
        aboutPrivacyPolicy = "Политика конфиденциальности и защиты данных",
        aboutSupportSuccess = "Спасибо за вашу щедрость и поддержку!",
        appVersionLabel = "Версия приложения",

        settTitle = "Настройки",
        settLanguage = "Язык",
        settSubStatus = "Статус подписки",
        settSubActive = "Премиум активен (10 сеансов)",
        settSubInactive = "Бесплатная базовая версия",
        settResetApp = "Сбросить данные приложения",
        settDeleteAcc = "Удалить аккаунт",

        billDialogTitle = "Мистическая оплата",
        billDialogChoosePay = "Выберите способ проведения платежа:",
        billDialogYooKassa = "Оплата через ЮKassa (Карта/СБП)",
        billDialogGooglePlay = "Google Play Billing",
        billDialogCardNum = "Номер карты (для ЮKassa)",
        billDialogSuccess = "Платеж прошёл успешно! Доступ разблокирован.",
        billDialogFail = "Не удалось завершить транзакцию.",

        slotLeftPalm = "Левая ладонь",
        slotLeftBack = "Тыл левой руки",
        slotRightPalm = "Правая ладонь",
        slotRightBack = "Тыл правой руки"
    )

    private val enStrings = PalmistStrings(
        navScan = "Reading",
        navCompat = "Affinity",
        navHistory = "History",
        navAbout = "Info",

        appName = "Palmist",
        skip = "Skip",
        next = "Next",
        cancel = "Cancel",
        save = "Save",
        loading = "Loading...",
        empty = "Empty",
        priceRub = "$",

        langSelectTitle = "Select Language",
        langSelectSubtitle = "We detected your system language, but you can change it here",
        langContinue = "Continue",

        splashMysticScroll = "Ancient scroll unfolding...",
        splashTapToSkip = "Skip splash",
        splashLogoSubtitle = "THE SECRETS OF YOUR DESTINY IN YOUR HAND",

        authTitle = "Sign In / Register",
        authSubtitle = "Create an account to keep track of your reading history",
        authEmailPhonePlaceholder = "Email or Phone Number",
        authPasswordPlaceholder = "Password",
        authSmsEmailCodePlaceholder = "Verification Code (SMS/Email)",
        authRegisterBtn = "Register",
        authSkipBtn = "Continue without registration",
        authSendCodeBtn = "Send Code",
        authErrorInvalid = "Please enter valid credentials",
        authEmailPhoneError = "Enter a valid E-mail or phone number (at least 10 digits)",
        authPasswordError = "Password must be at least 6 characters",

        profileTitle = "Enter Data",
        profileSubtitle = "Palmistry considers age, gender and physical proportions",
        profileNameLabel = "Your Name",
        profileNameError = "Name must be at least 2 characters",
        profileGenderLabel = "Gender",
        profileGenderMale = "Male",
        profileGenderFemale = "Female",
        profileGenderNone = "Prefer not to say",
        profileAgeLabel = "Year of Birth",
        profileHeightLabel = "Height (cm)",
        profileHandLabel = "Dominant Hand",
        profileHandLeft = "Left-handed",
        profileHandRight = "Right-handed",
        profileHandDescLeft = "For lefties, the left hand shows active destiny; the right shows natural potential",
        profileHandDescRight = "For righties, the right hand shows active destiny; the left shows natural potential",

        uploadTitle = "Upload Materials",
        uploadGuideHeader = "IMPORTANT FOR ACCURATE READING:",
        uploadGuideText = "• Place a credit card next to your hand for proper scaling\n• Stretch your fingers out as wide as possible\n• Keep your palm relaxed and fully visible\n• For videos — slowly rotate your hand back and forth\n• Ensure bright, even lighting with no harsh shadows\n• Every detail matters: nail shape, finger ratios, palm length",
        uploadPhotoSection = "📸 Hand Photo",
        uploadVideoSection = "🎥 Hand Video (up to 60 sec)",
        uploadTakePhoto = "Take Photo",
        uploadGallery = "From Gallery",
        uploadRecordVideo = "Record Video",
        uploadLoadVideo = "Upload Video",
        uploadVideoHint = "Hint: Slowly turn your hands palm and back, spread your fingers. Place a credit card next to it for scale.",
        uploadPreviewPhoto = "Photo uploaded",
        uploadPreviewVideo = "Video ready (60 sec max)",
        uploadChooseAnalysisType = "Select analysis type:",

        btnBriefChar = "7. Brief Character & Qualities Analysis (Free)",
        btnFullChar = "8. Full Character & Qualities Analysis (150 ₽)",
        btnBriefPath = "9. Brief Life Path Analysis (Free)",
        btnFullPath = "10. Full Life Path Analysis (150 ₽)",
        freeLabel = "Free",
        btnClose = "Close",

        loadAnalyzeLines = "Analyzing Lines...",
        loadStudyMounts = "Evaluating Mounts...",
        loadReadSigns = "Decoding Sacred Signs...",
        loadGenPredictions = "Formulating Predictions...",
        loadMysticTitle = "Mystical Extraction",
        loadProgressText = "Unveiling the energetic currents on your palm...",

        resTitle = "Interpretation",
        resTabReport = "Report",
        resTabLinesMap = "Lines Map",
        resOverallPortrait = "Overall Personality Portrait",
        resHandType = "Hand Archetype",
        resLinesHeader = "Primary Destiny Lines",
        resMountsHeader = "Planetary Mounts",
        resSignsHeader = "Sacred Signs & Markings",
        resMarriageChildren = "6. Analysis of Relationships: Family, Marriage, Children & Partners",
        resLifeEvents = "4. Analysis of Life Path & Events",
        resPredictions = "5. Analysis of Life Situations & External Influences",
        resRecommendations = "Aura Guidelines",
        resInheritedPotentials = "1. Left Hand Analysis (Innate Potentials)",
        resAcquiredTraits = "2. Right Hand Analysis (Acquired Traits & Realization)",
        resCharacterQualities = "3. Analysis of Character & Personal Qualities",
        resAudioTitle = "Read Description (TTS)",
        resVoiceMale = "Male",
        resVoiceFemale = "Female",
        resVoiceSpeed = "Speech Rate",
        resExportPdf = "Share",
        resExportSuccess = "Report text ready for sharing!",
        resBtnBuy10 = "Unlock 10 Full Readings for 1000 ₽",
        resBtnBuyCompat = "Check Partner Compatibility for 250 ₽",

        compatTitle = "Partner Compatibility",
        compatSubtitle = "Compare heart lines, marriage indicators, and planetary mounts to check your soul link",
        compatUploadSelf = "Your hand photo",
        compatUploadPartner = "Partner's hand photo",
        compatAnalyzeBtn = "Calculate Affinity",
        compatPercentLabel = "Soul Affinity Score",
        compatCombinedTitle = "Synergy Analysis",
        compatStrongTitle = "Aura Harmonics",
        compatWeakTitle = "Friction Areas & Blocks",
        compatEmotional = "Emotional Harmony",
        compatIntellectual = "Intellectual Alignment",
        compatFinancial = "Financial Stability",

        histTitle = "Interpretation History",
        histSubtitle = "",
        histNoRecords = "No palm readings found",
        histClearHistory = "Clear History",

        aboutTitle = "About Palmist",
        aboutTabInfo = "Theory",
        aboutTabFaq = "FAQ",
        aboutTabContacts = "Support",
        aboutHistoryPalmist = "History of Palmistry",
        aboutHistoryText = "Palmistry (from Greek cheir — hand, and manteia — divination) is an ancient predictive science dating back to prehistoric India, China, and Egypt. Historical luminaries like Aristotle and Hippocrates studied hand layouts to diagnose character and path potentials.",
        aboutTheoryLines = "Map of Your Palm",
        aboutTheoryText = "The Life Line is about vitality, core energy, and longevity.\nThe Heart Line determines emotional depth and relationship patterns.\nThe Head Line indicates intellect and psychological framework.\nThe Destiny Line shows career patterns and external life influences.",
        aboutSupportBtn = "Support Development",
        aboutDonateTitle = "Project Donation",
        aboutDonateDesc = "If you appreciate this app, you can support our development with a voluntary contribution of any amount.",
        aboutEmailSupport = "Email us: support@palmist-mystic.com",
        aboutPrivacyPolicy = "Privacy Policy and Data Protection",
        aboutSupportSuccess = "Thank you for your generosity and cosmic support!",
        appVersionLabel = "App Version",

        settTitle = "Settings",
        settLanguage = "Language",
        settSubStatus = "Subscription Tier",
        settSubActive = "Premium active (10 readings)",
        settSubInactive = "Free limited edition",
        settResetApp = "Wipe Cache & Reset",
        settDeleteAcc = "Delete Account",

        billDialogTitle = "Cosmic Checkout",
        billDialogChoosePay = "Select your spiritual payment pathway:",
        billDialogYooKassa = "Pay via YooKassa (Card/SBP)",
        billDialogGooglePlay = "Google Play Billing System",
        billDialogCardNum = "Card number (for YooKassa)",
        billDialogSuccess = "Payment successful! Universal access unlocked.",
        billDialogFail = "Transaction faded into the ether. Try again.",

        slotLeftPalm = "Left Palm",
        slotLeftBack = "Back of Left Hand",
        slotRightPalm = "Right Palm",
        slotRightBack = "Back of Right Hand"
    )

    // Функция для получения набора строк локализации на основе выбранного языка
    fun get(lang: AppLanguage): PalmistStrings {
        return when (lang) {
            AppLanguage.RUS -> ruStrings // Возвращаем русскоязычную локализацию
            AppLanguage.ENG -> enStrings // Возвращаем англоязычную локализацию
        }
    }
}
