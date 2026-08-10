# Weeboonime (WEBUNIME Mobile)

App Android HP (Jetpack Compose) — katalog anime + ekonomi Fase 1 + OTA self-update.

## API katalog

https://webunime-catalog-api.vercel.app

## Fitur

- Home / Jadwal / History / Subscribed / Timeline (UX ala Wibuku)
- Login Google (Firebase Auth) + sync Firestore
- **3 kunci awal**; 1 episode = 1 kunci (Premium bebas)
- Rewarded AdMob (test ID) → +1 kunci; tukar 10 gem → 1 kunci
- Play Billing product ID: `webunime_premium_1m/3m/6m/12m`
- OTA: baca `update/version.json` di repo ini

## Build release (OTA)

```powershell
cd "c:\Users\sjatm\OneDrive\Documents\Project professional\Weeboonime\mobile"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`

## Setup Firebase

1. Project Firebase + Android app `com.webunime.mobile`
2. `app/google-services.json` (SHA-1 debug sudah terdaftar)
3. Auth → Google + Web client ID di `GOOGLE_WEB_CLIENT_ID`
4. Firestore rules: `firestore.rules`
5. (Opsional) Cloud Functions di `functions/`

## Cara test OTA di HP

1. Buka app yang sudah terpasang (v0.2.1+)
2. Dialog update muncul → unduh & install
3. Atau unduh langsung: [Release terbaru](https://github.com/gitgitmiko/app_weeboonime/releases/latest)
