# Buat keystore production (sekali saja). File hasil JANGAN di-commit.
# Backup .jks + password di tempat aman (password manager / offline).
# Setelah ini dipakai, user yang sudah install APK debug-sign HARUS uninstall dulu.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root "keystore"
$jks = Join-Path $outDir "weeboonime-release.jks"
$props = Join-Path $root "keystore.properties"

if (Test-Path $jks) {
    Write-Error "Keystore sudah ada: $jks"
}

$keytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
if (-not (Test-Path $keytool)) {
    $keytool = "keytool"
}

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$pass = -join ((48..57 + 65..90 + 97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ })

& $keytool -genkeypair -v `
    -keystore $jks `
    -alias weeboonime `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass $pass `
    -keypass $pass `
    -dname "CN=Weeboonime, OU=Mobile, O=Weeboonime, L=Jakarta, ST=Jakarta, C=ID"

@"
storeFile=keystore/weeboonime-release.jks
storePassword=$pass
keyAlias=weeboonime
keyPassword=$pass
"@ | Set-Content -Path $props -Encoding ASCII

Write-Host "Keystore: $jks"
Write-Host "Properties: $props"
Write-Host "BACKUP kedua file ini sekarang. Password tidak akan ditampilkan lagi."
