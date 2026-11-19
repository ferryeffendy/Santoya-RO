import java.time.Duration

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.santoyaro.kxmpzq"
    minSdk = 24
    targetSdk = 36
    versionCode = 5
    versionName = "1.4"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      if (file("${rootDir}/debug.keystore").exists()) {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    isCoreLibraryDesugaringEnabled = true
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  sourceSets {
    getByName("debug").assets.directories.add("$projectDir/schemas")
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.paging)
  implementation(libs.androidx.paging.runtime)
  implementation(libs.androidx.paging.compose)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.crashlytics)
  implementation(libs.firebase.analytics.ktx)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.playintegrity)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  coreLibraryDesugaring(libs.desugar.jdk.libs)
}

tasks.withType<Test> {
  failFast = true
  timeout.set(Duration.ofMinutes(2))
  testLogging {
    events("passed", "failed", "skipped")
  }
}

val defaultGoogleServicesJson = """
{
  "project_info": {
    "project_number": "735932389286",
    "project_id": "gen-lang-client-0963336772",
    "storage_bucket": "gen-lang-client-0963336772.firebasestorage.app"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:735932389286:android:61dbdf4ddd701fbaea2167",
        "android_client_info": {
          "package_name": "com.aistudio.santoyaro.kxmpzq"
        }
      },
      "oauth_client": [
        {
          "client_id": "735932389286-50ngi7dhdsqr87acd5d7ji3sm2au3mhg.apps.googleusercontent.com",
          "client_type": 1,
          "android_info": {
            "package_name": "com.aistudio.santoyaro.kxmpzq",
            "certificate_hash": "da39a3ee5e6b4b0d3255bfef95601890afd80709"
          }
        },
        {
          "client_id": "735932389286-qisb53lbk4kd44ts0d4ajqb24leaiv2i.apps.googleusercontent.com",
          "client_type": 1,
          "android_info": {
            "package_name": "com.aistudio.santoyaro.kxmpzq",
            "certificate_hash": "0be9cd661f222f05700dbdd8ba3af6b97e3fb1e4"
          }
        },
        {
          "client_id": "735932389286-jk7m0tq00b60mdaa4l6rcprv885l3s61.apps.googleusercontent.com",
          "client_type": 3
        }
      ],
      "api_key": [
        {
          "current_key": "AIzaSyAPuKJ_zZ933sCEspQsk2bliZMKQQ34omk"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": [
            {
              "client_id": "735932389286-jk7m0tq00b60mdaa4l6rcprv885l3s61.apps.googleusercontent.com",
              "client_type": 3
            }
          ]
        }
      }
    }
  ],
  "configuration_version": "1"
}
""".trimIndent()

val googleServicesFile = layout.projectDirectory.file("google-services.json").asFile
val googleServicesBackup = rootProject.file("gradle/firebase-google-services.json")

// Track file existence in Gradle Configuration Cache so deleting google-services.json
// automatically invalidates the cache and triggers self-healing before any task runs.
val trackedGoogleServicesContent =
  providers.fileContents(layout.projectDirectory.file("google-services.json")).asText.orNull

if (trackedGoogleServicesContent.isNullOrBlank() || !googleServicesFile.exists()) {
  if (googleServicesBackup.exists()) {
    googleServicesBackup.copyTo(googleServicesFile, overwrite = true)
  } else {
    googleServicesFile.writeText(defaultGoogleServicesJson)
  }
}
if (!googleServicesBackup.exists()) {
  googleServicesBackup.parentFile?.mkdirs()
  googleServicesBackup.writeText(defaultGoogleServicesJson)
}

val ensureGoogleServicesJson = tasks.register("ensureGoogleServicesJson") {
  val targetFile = layout.projectDirectory.file("google-services.json").asFile
  val backupFile = rootProject.file("gradle/firebase-google-services.json")
  val fallbackContent = defaultGoogleServicesJson
  outputs.upToDateWhen { targetFile.exists() && targetFile.length() > 10L }
  doLast {
    if (!targetFile.exists() || targetFile.length() <= 10L) {
      if (backupFile.exists() && backupFile.length() > 10L) {
        backupFile.copyTo(targetFile, overwrite = true)
      } else {
        targetFile.writeText(fallbackContent)
      }
    }
  }
}

tasks.matching { it.name == "preBuild" || it.name.contains("GoogleServices") }.configureEach {
  if (name != "ensureGoogleServicesJson") {
    dependsOn(ensureGoogleServicesJson)
  }
}

