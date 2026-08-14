package com.webunime.mobile.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.webunime.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = false)
data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String = "",
    val changelog: String? = null,
)

/**
 * Self-update tanpa Play Store.
 *
 * 1) Baca update/version.json (GitHub API + fallback)
 * 2) Tolak manifest yang tidak HTTPS / bukan release repo ini / tanpa SHA-256
 * 3) Unduh APK, verifikasi hash + package + signature, lalu install
 */
class AppUpdateChecker(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addNetworkInterceptor { chain ->
            val url = chain.request().url
            if (url.scheme != "https") {
                throw IOException("OTA hanya HTTPS")
            }
            if (url.host.lowercase() !in ALLOWED_HOSTS) {
                throw IOException("Host OTA tidak diizinkan")
            }
            chain.proceed(chain.request())
        }
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(AppUpdateInfo::class.java)

    suspend fun fetchAvailableUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val bust = System.currentTimeMillis()
        val urls = listOf(
            VERSION_JSON_GITHUB_API,
            "$VERSION_JSON_JSDELIVR?t=$bust",
            "$VERSION_JSON_URL?t=$bust",
        )
        var info: AppUpdateInfo? = null
        for (url in urls) {
            info = if (url == VERSION_JSON_GITHUB_API) {
                fetchVersionJsonFromGithubApi()
            } else {
                fetchVersionJson(url)
            }
            if (info != null) break
        }
        val resolved = info ?: return@withContext null
        if (resolved.versionCode <= BuildConfig.VERSION_CODE) {
            Log.i(TAG, "Up to date: installed=${BuildConfig.VERSION_CODE} remote=${resolved.versionCode}")
            return@withContext null
        }
        if (!isTrustedManifest(resolved)) {
            Log.w(TAG, "Manifest OTA ditolak (URL/hash tidak sah)")
            return@withContext null
        }
        Log.i(TAG, "Update available: ${resolved.versionName} (${resolved.versionCode})")
        resolved
    }

    private fun fetchVersionJsonFromGithubApi(): AppUpdateInfo? {
        return runCatching {
            val request = Request.Builder()
                .url(VERSION_JSON_GITHUB_API)
                .header("User-Agent", "Weeboonime-Mobile/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/vnd.github.raw+json")
                .header("Cache-Control", "no-cache")
                .get()
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "GitHub API HTTP ${response.code}")
                    return null
                }
                response.body?.string().orEmpty()
            }
            parseUpdateInfo(body)
        }.onFailure {
            Log.w(TAG, "GitHub API version.json failed: ${it.message}")
        }.getOrNull()
    }

    private fun fetchVersionJson(url: String): AppUpdateInfo? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Weeboonime-Mobile/${BuildConfig.VERSION_NAME}")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .get()
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "version.json HTTP ${response.code} for $url")
                    return null
                }
                response.body?.string().orEmpty()
            }
            if (body.isBlank()) return null
            parseUpdateInfo(body)
        }.onFailure {
            Log.w(TAG, "version.json fetch failed: $url — ${it.message}")
        }.getOrNull()
    }

    private fun parseUpdateInfo(body: String): AppUpdateInfo? {
        val start = body.indexOf('{')
        if (start < 0) return null
        val clean = body.substring(start).trim().removePrefix("\uFEFF")
        runCatching { adapter.fromJson(clean) }.getOrNull()?.let { return it }
        return runCatching {
            val o = JSONObject(clean)
            AppUpdateInfo(
                versionCode = o.getInt("versionCode"),
                versionName = o.getString("versionName"),
                apkUrl = o.getString("apkUrl"),
                sha256 = o.optString("sha256"),
                changelog = o.optString("changelog").takeIf { it.isNotBlank() },
            )
        }.getOrNull()
    }

    suspend fun downloadApk(
        info: AppUpdateInfo,
        onProgress: ((percent: Int) -> Unit)? = null,
    ): File = withContext(Dispatchers.IO) {
        if (!isTrustedManifest(info)) error("Manifest update tidak sah")

        val dir = File(context.cacheDir, "updates").also { it.mkdirs() }
        val out = File(dir, "WEBUNIME-Mobile-update.apk")
        if (out.exists()) out.delete()

        val expectedHash = hexToBytes(normalizeSha256(info.sha256))
        val digest = MessageDigest.getInstance("SHA-256")

        val request = Request.Builder()
            .url(info.apkUrl)
            .header("User-Agent", "WEBUNIME-Mobile/${BuildConfig.VERSION_NAME}")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val finalHost = response.request.url.host.lowercase()
                if (finalHost !in ALLOWED_HOSTS) error("Host unduhan tidak diizinkan")
                val body = response.body ?: error("Empty body")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    DigestOutputStream(out.outputStream(), digest).use { output ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        var done = 0L
                        var lastPct = -1
                        while (input.read(buf).also { read = it } >= 0) {
                            output.write(buf, 0, read)
                            done += read
                            if (total > 0) {
                                val pct = ((done * 100) / total).toInt().coerceIn(0, 100)
                                if (pct != lastPct && pct % 5 == 0) {
                                    lastPct = pct
                                    onProgress?.invoke(pct)
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }
            if (out.length() < 100_000L) error("APK terlalu kecil")
            if (!MessageDigest.isEqual(digest.digest(), expectedHash)) {
                Log.w(TAG, "SHA-256 APK tidak cocok")
                error("File update tidak sah")
            }
            verifyApkIdentity(out, info)
        } catch (t: Throwable) {
            out.delete()
            throw t
        }
        onProgress?.invoke(100)
        out
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    fun installApk(activity: Activity, apkFile: File, info: AppUpdateInfo) {
        verifyDownloadedApk(apkFile, info)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    private fun verifyDownloadedApk(apkFile: File, info: AppUpdateInfo) {
        if (!apkFile.exists()) error("File update hilang")
        if (!isTrustedManifest(info)) error("Manifest update tidak sah")
        val actual = sha256(apkFile)
        val expected = hexToBytes(normalizeSha256(info.sha256))
        if (!MessageDigest.isEqual(actual, expected)) {
            Log.w(TAG, "SHA-256 berubah sebelum install")
            apkFile.delete()
            error("File update tidak sah")
        }
        verifyApkIdentity(apkFile, info)
    }

    private fun verifyApkIdentity(apkFile: File, info: AppUpdateInfo) {
        val apkPkg = packageInfoFromApk(apkFile) ?: error("APK tidak bisa dibaca")
        if (apkPkg.packageName != context.packageName) {
            Log.w(TAG, "Package APK ${apkPkg.packageName} != ${context.packageName}")
            error("File update tidak sah")
        }
        val apkCode = apkVersionCode(apkPkg)
        if (apkCode != info.versionCode.toLong()) {
            Log.w(TAG, "versionCode APK $apkCode != manifest ${info.versionCode}")
            error("File update tidak sah")
        }
        val installed = installedPackageInfo()
        val apkCerts = signingCerts(apkPkg)
        val installedCerts = signingCerts(installed)
        val sameSigner = apkCerts.any { apkCert ->
            installedCerts.any { MessageDigest.isEqual(apkCert, it) }
        }
        if (!sameSigner) {
            Log.w(TAG, "Signature APK tidak sama dengan app terpasang")
            error("File update tidak sah")
        }
    }

    private fun packageInfoFromApk(apk: File): PackageInfo? {
        val pm = context.packageManager
        val flags = signingFlags()
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apk.absolutePath, flags)
        } ?: return null
        info.applicationInfo?.apply {
            sourceDir = apk.absolutePath
            publicSourceDir = apk.absolutePath
        }
        return info
    }

    private fun installedPackageInfo(): PackageInfo {
        val pm = context.packageManager
        val flags = signingFlags()
        return if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, flags)
        }
    }

    private fun signingFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
    }

    @Suppress("DEPRECATION")
    private fun signingCerts(info: PackageInfo): List<ByteArray> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val si = info.signingInfo ?: return emptyList()
            val signers = if (si.hasMultipleSigners()) {
                si.apkContentsSigners
            } else {
                si.signingCertificateHistory
            }
            return signers.orEmpty().map { it.toByteArray() }
        }
        return info.signatures.orEmpty().map { it.toByteArray() }
    }

    private fun apkVersionCode(info: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    companion object {
        private const val TAG = "AppUpdate"

        const val VERSION_JSON_URL =
            "https://raw.githubusercontent.com/gitgitmiko/app_weeboonime/main/update/version.json"

        const val VERSION_JSON_JSDELIVR =
            "https://cdn.jsdelivr.net/gh/gitgitmiko/app_weeboonime@main/update/version.json"

        /** Sumber utama — jarang kena CDN stale seperti raw.githubusercontent. */
        const val VERSION_JSON_GITHUB_API =
            "https://api.github.com/repos/gitgitmiko/app_weeboonime/contents/update/version.json?ref=main"

        private val ALLOWED_HOSTS = setOf(
            "api.github.com",
            "raw.githubusercontent.com",
            "cdn.jsdelivr.net",
            "github.com",
            "objects.githubusercontent.com",
            "github-releases.githubusercontent.com",
            "release-assets.githubusercontent.com",
        )

        private val TRUSTED_APK_URL = Regex(
            "^https://github\\.com/gitgitmiko/app_weeboonime/releases/download/[^/]+/[^/]+\\.apk$",
            RegexOption.IGNORE_CASE,
        )

        fun isTrustedManifest(info: AppUpdateInfo): Boolean {
            if (info.versionCode <= 0) return false
            if (info.versionName.isBlank()) return false
            if (!TRUSTED_APK_URL.matches(info.apkUrl.trim())) return false
            return normalizeSha256(info.sha256).matches(Regex("^[a-f0-9]{64}$"))
        }

        fun normalizeSha256(value: String): String =
            value.trim().lowercase().replace(" ", "")

        fun hexToBytes(hex: String): ByteArray {
            require(hex.length == 64) { "SHA-256 tidak valid" }
            return ByteArray(32) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }

        fun sha256(file: File): ByteArray {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(64 * 1024)
                var n: Int
                while (input.read(buf).also { n = it } >= 0) {
                    md.update(buf, 0, n)
                }
            }
            return md.digest()
        }
    }
}
