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
