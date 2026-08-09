package com.webunime.mobile.ui.update

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.webunime.mobile.R
import com.webunime.mobile.data.AppUpdateChecker
import com.webunime.mobile.data.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cek OTA saat startup + dialog unduh/install (pola app TV).
 */
@Composable
fun AppUpdateHost(activity: ComponentActivity) {
    val checker = remember(activity) { AppUpdateChecker(activity) }
    val scope = rememberCoroutineScope()
    var available by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var dialogVisible by remember { mutableStateOf(false) }
    var pendingApk by remember { mutableStateOf<File?>(null) }
    var downloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Tunggu splash selesai supaya dialog update terlihat.
        delay(2000)
        val info = runCatching { checker.fetchAvailableUpdate() }.getOrNull()
        if (info != null) {
            available = info
            dialogVisible = true
        }
    }

    DisposableEffect(activity, checker) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            val apk = pendingApk
            if (apk != null && apk.exists() && checker.canInstallPackages()) {
                pendingApk = null
                Toast.makeText(activity, R.string.update_installing, Toast.LENGTH_SHORT).show()
                runCatching { checker.installApk(activity, apk) }
                    .onFailure {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.update_failed, it.message ?: "install"),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    val info = available
    if (dialogVisible && info != null && !downloading) {
        val notes = info.changelog?.takeIf { it.isNotBlank() }.orEmpty()
        AlertDialog(
            onDismissRequest = { dialogVisible = false },
            title = { Text(activity.getString(R.string.update_title)) },
            text = {
                Text(activity.getString(R.string.update_message, info.versionName, notes))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogVisible = false
                        downloading = true
                        scope.launch {
                            startDownload(activity, checker, info) { apk ->
                                pendingApk = apk
                            }
                            downloading = false
                        }
                    },
                ) {
                    Text(activity.getString(R.string.update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogVisible = false }) {
                    Text(activity.getString(R.string.update_later))
                }
            },
        )
    }
}

private suspend fun startDownload(
    activity: ComponentActivity,
    checker: AppUpdateChecker,
    info: AppUpdateInfo,
    onNeedPermission: (File) -> Unit,
) {
    val latest = runCatching { checker.fetchAvailableUpdate() }.getOrNull()
    val toInstall = when {
        latest == null -> info
        latest.versionCode >= info.versionCode -> latest
        else -> info
    }

    val progressToast = Toast.makeText(activity, "", Toast.LENGTH_SHORT)
    val apk = runCatching {
        checker.downloadApk(toInstall) { pct ->
            activity.runOnUiThread {
                progressToast.setText(activity.getString(R.string.update_downloading, pct))
                progressToast.show()
            }
        }
    }.getOrElse { err ->
        withContext(Dispatchers.Main) {
            Toast.makeText(
                activity,
                activity.getString(R.string.update_failed, err.message ?: "download"),
                Toast.LENGTH_LONG,
            ).show()
        }
        return
    }

    if (activity.isFinishing) return
    if (!checker.canInstallPackages()) {
        onNeedPermission(apk)
        withContext(Dispatchers.Main) {
            Toast.makeText(activity, R.string.update_need_permission, Toast.LENGTH_LONG).show()
            checker.openInstallPermissionSettings(activity)
        }
        return
    }

    withContext(Dispatchers.Main) {
        Toast.makeText(activity, R.string.update_installing, Toast.LENGTH_SHORT).show()
        runCatching { checker.installApk(activity, apk) }
            .onFailure {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.update_failed, it.message ?: "install"),
                    Toast.LENGTH_LONG,
                ).show()
            }
    }
}
