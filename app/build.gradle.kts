import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.aistudio.hiromant.kxsrwa"
  compileSdk = 36

  val versionFile = rootProject.file("version.txt")
  val currentVersion = if (versionFile.exists()) versionFile.readText().trim() else "1.001"

  defaultConfig {
    applicationId = "com.aistudio.hiromant.kxsrwa"
    minSdk = 29
    targetSdk = 36
    versionCode = 1
    versionName = currentVersion

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    ndk {
      abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    }

    // Поле для API-ключа, который будет подставлен из .env или переменной окружения
    buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: ""}\"")
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
      isUniversalApk = true
    }
  }

  // Register a task to automatically restore a debug.keystore from debug.keystore.base64
  // This avoids violating the Configuration Cache rules since it executes only during the execution phase.
  val generateDebugKeystore = tasks.register("generateDebugKeystore") {
    val debugKeystore = rootProject.file("debug.keystore")
    val base64File = rootProject.file("debug.keystore.base64")
    
    outputs.file(debugKeystore)
    if (base64File.exists()) {
      inputs.file(base64File)
    }

    doLast {
      if (!debugKeystore.exists() && base64File.exists()) {
        try {
          val base64Content = base64File.readText().trim()
          val decodedBytes = Base64.getDecoder().decode(base64Content)
          debugKeystore.writeBytes(decodedBytes)
          logger.lifecycle("Successfully restored debug.keystore from base64")
        } catch (e: Exception) {
          try {
            val decodedBytes = Base64.getMimeDecoder().decode(base64File.readText())
            debugKeystore.writeBytes(decodedBytes)
            logger.lifecycle("Successfully restored debug.keystore via MimeDecoder")
          } catch (e2: Exception) {
            logger.error("Failed to restore debug.keystore from base64: ${e2.message}")
          }
        }
      }
    }
  }

  // Hook this task into the build lifecycle so any signing/assembly task depends on it
  tasks.matching { it.name.startsWith("validateSigning") || it.name.startsWith("package") || it.name.startsWith("assemble") }.configureEach {
    dependsOn(generateDebugKeystore)
  }

  // Ensure only the universal (full) APK is kept in the output directory
  tasks.matching { it.name.startsWith("package") && (it.name.endsWith("Debug") || it.name.endsWith("Release")) }.configureEach {
    doLast {
      val buildDir = layout.buildDirectory.get().asFile
      val apkOutputsDir = file("$buildDir/outputs/apk")
      if (apkOutputsDir.exists()) {
        apkOutputsDir.walkTopDown().forEach { file ->
          if (file.isFile && file.extension == "apk" && !file.name.contains("universal")) {
            if (file.delete()) {
              logger.lifecycle("Removed non-universal split APK: ${file.name}")
            }
          }
        }
      }
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      val keystoreFile = rootProject.file("debug.keystore")
      val base64File = rootProject.file("debug.keystore.base64")
      if (keystoreFile.exists() || base64File.exists()) {
        signingConfig = signingConfigs.getByName("debugConfig")
      } else {
        signingConfig = signingConfigs.getByName("debug")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  
  // Ensure .env.example exists to prevent build crashes in CI environments (like GitHub Actions)
  val envExample = rootProject.file(".env.example")
  if (!envExample.exists()) {
    try {
      envExample.createNewFile()
      envExample.writeText("GEMINI_API_KEY=MY_GEMINI_API_KEY\n")
    } catch (e: Exception) {
      // Safe fallback
    }
  }

  // Automatically propagate environment variables (such as GEMINI_API_KEY from AI Studio Secrets panel)
  // into the local .env file so the Secrets Gradle Plugin can read and inject them into BuildConfig.
  val envFile = rootProject.file(".env")
  val envKeys = listOf("GEMINI_API_KEY")
  val envContent = StringBuilder()
  for (key in envKeys) {
    val value = System.getenv(key)
    if (!value.isNullOrEmpty()) {
      envContent.append("$key=$value\n")
    }
  }
  if (envContent.isNotEmpty()) {
    try {
      envFile.writeText(envContent.toString())
    } catch (e: Exception) {
      // Safe fallback
    }
  }

  defaultPropertiesFileName = ".env.example"
}

googleServices {
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.play.services.auth)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
  // Если нужна аналитика
  implementation("com.google.firebase:firebase-analytics")

  // Если нужна аутентификация
  implementation("com.google.firebase:firebase-auth")

  // Если нужен Firestore (база данных)
  implementation("com.google.firebase:firebase-firestore")

  // Если хотите использовать Firebase AI (Gemini через Firebase)
  implementation("com.google.firebase:firebase-ai")
  // Импортируйте спецификацию материалов Firebase BoM
  implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
  // TODO: Добавьте зависимости для продуктов Firebase, которые вы хотите использовать
  // При использовании BoM не указывайте версии в зависимостях Firebase.
  implementation("com.google.firebase:firebase-analytics")

}
