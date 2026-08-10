# Weeboonime (WEBUNIME Mobile)

App Android HP (Jetpack Compose) — katalog anime + ekonomi + subscribe/FCM + OTA self-update.

## API katalog

https://webunime-catalog-api.vercel.app

## Fitur

- Home / Jadwal / History / Subscribed / Timeline (UX ala Wibuku)
- Login Google (Firebase Auth) + sync Firestore (ekonomi + `animeSubs` per UID)
- **3 kunci awal**; 1 episode = 1 kunci (Premium bebas)
- Rewarded AdMob (test ID) → +1 kunci; tukar 10 gem → 1 kunci
- Premium: layar pilih paket + banner shimmer di beranda
- **Subscribe anime** → FCM topic `anime_<slug>` + tab Subscribed
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
4. Firestore rules: `firestore.rules` (`users/{uid}` owner-only; `meta/*` admin-only)
5. Cloud Messaging (FCM) aktif di project (default)

## Deploy Cloud Functions (notifikasi episode)

Butuh **Firebase Blaze** (pay-as-you-go) untuk scheduled function.

```powershell
cd "c:\Users\sjatm\OneDrive\Documents\Project professional\Weeboonime\mobile"
npm install -g firebase-tools   # sekali saja
firebase login
firebase use myproject-fbbb9
cd functions
npm install
cd ..
firebase deploy --only functions,firestore:rules
```

Function `notifyNewEpisodes` jalan tiap 20 menit:
- Baca `GET /v1/anime/latest`
- Bandingkan dengan `meta/episodeNotify`
- Kirim FCM ke topic `anime_<slug>` bila episode baru

Tanpa deploy Functions, subscribe + daftar Subscribed tetap jalan; push otomatis belum terkirim.

## Cara test OTA di HP

1. Buka app yang sudah terpasang (v0.2.1+)
2. Dialog update muncul → unduh & install
3. Atau unduh langsung: [Release terbaru](https://github.com/gitgitmiko/app_weeboonime/releases/latest)
