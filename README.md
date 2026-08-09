# WEBUNIME Mobile

App Android HP (Jetpack Compose) untuk katalog WEBUNIME.

Lokasi project (sama isinya):

- Workspace: `Weeboonime/mobile`
- Build path: `Documents/Project/App WEBUNIME Mobile`

## API

Base URL: https://webunime-catalog-api.vercel.app

## Build

```powershell
cd "C:\Users\sjatm\OneDrive\Documents\Project\App WEBUNIME Mobile"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

APK debug: `app\build\outputs\apk\debug\app-debug.apk`

Atau buka folder di Android Studio → Run di emulator/HP.

## Fitur MVP (Fase 0b)

- Home: Anime Terbaru, Jadwal hari ini, Anime Movie
- Cari (debounce)
- Jadwal rilis Senin–Minggu
- Detail + daftar episode
- Player: ExoPlayer (URL media langsung) / WebView (embed)
- Update in-app (OTA) seperti app TV

## Update in-app (development)

App mengecek `update/version.json` di repo ini (GitHub raw + jsDelivr).
Jika `versionCode` remote > yang terpasang, user bisa unduh & install APK langsung dari app.

### Rilis update baru

1. Naikkan `versionCode` / `versionName` di `app/build.gradle.kts`
2. Build release/debug APK, rename misalnya `WEBUNIME-Mobile-v0.1.1.apk`
3. Upload APK ke GitHub Releases: https://github.com/gitgitmiko/app_weeboonime/releases
4. Update `update/version.json`:

```json
{
  "versionCode": 2,
  "versionName": "0.1.1",
  "apkUrl": "https://github.com/gitgitmiko/app_weeboonime/releases/download/v0.1.1/WEBUNIME-Mobile-v0.1.1.apk",
  "changelog": "Perbaikan …"
}
```

5. Commit + push `version.json` ke `main`

HP yang sudah pasang versi lama akan mendapat dialog **Update tersedia** saat buka app.
