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
        versionCode = 24
        versionName = "0.3.3"

        buildConfigField(
            "String",
            "CATALOG_API_BASE",
            "\"https://webunime-catalog-api.vercel.app\"",
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
        // Samakan dengan APK lama (Android Debug) supaya OTA/update tidak "package invalid".
        // Ganti ke keystore production sendiri sebelum publish Play Store.
        create("release") {
            val debugKs = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storeFile = debugKs
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
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
