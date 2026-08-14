import java.security.MessageDigest
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.webunime.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.webunime.mobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 30
        versionName = "0.3.9"

        buildConfigField(
            "String",
            "CATALOG_API_BASE",
            "\"https://webunime-catalog-api.vercel.app\"",
        )
        buildConfigField(
            "String",
            "REPAIR_API_BASE",
            "\"https://webunime-scrape-proxy.vercel.app\"",
        )
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"347556929487-dvblluel4fml5jj4l65takpqqatf72vp.apps.googleusercontent.com\"",
        )
        buildConfigField(
            "String",
            "ADMOB_REWARDED_UNIT_ID",
            "\"ca-app-pub-3940256099942544/5224354917\"",
        )
        buildConfigField("int", "STARTING_KEYS", "3")
        buildConfigField("int", "XP_PER_EPISODE", "10")
        buildConfigField("int", "GEMS_PER_LEVEL", "5")
        buildConfigField("int", "GEMS_PER_KEY", "10")
    }

    signingConfigs {
        create("release") {
            val keystoreProps = rootProject.file("keystore.properties")
            if (keystoreProps.exists()) {
                val props = Properties().apply {
                    keystoreProps.inputStream().use { load(it) }
                }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            } else {
                // Fallback: debug keystore agar OTA ke user lama tidak "package invalid".
                // Untuk production, buat keystore.properties (lihat keystore.properties.example).
                logger.warn(
                    "keystore.properties tidak ada — APK release masih ditandatangani debug keystore.",
                )
                val debugKs = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storeFile = debugKs
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
}

tasks.register("printReleaseApkSha256") {
    group = "publishing"
    description = "Cetak SHA-256 APK release untuk diisi ke update/version.json"
    doLast {
        val apk = file("build/outputs/apk/release/app-release.apk")
        check(apk.exists()) {
            "APK belum ada. Jalankan assembleRelease dulu: ${apk.absolutePath}"
        }
        val md = MessageDigest.getInstance("SHA-256")
        apk.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            var n: Int
            while (input.read(buf).also { n = it } >= 0) {
                md.update(buf, 0, n)
            }
        }
        val hex = md.digest().joinToString("") { "%02x".format(it) }
        println("file   = ${apk.name}")
        println("size   = ${apk.length()}")
        println("sha256 = $hex")
        println("Salin nilai sha256 ke update/version.json (huruf kecil).")
    }
}
