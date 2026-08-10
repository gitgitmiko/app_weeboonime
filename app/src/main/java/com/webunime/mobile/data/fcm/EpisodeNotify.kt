package com.webunime.mobile.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.webunime.mobile.MainActivity
import com.webunime.mobile.R

object EpisodeNotify {
    const val CHANNEL_ID = "episode_updates"
    const val EXTRA_OPEN_SLUG = "open_slug"
    const val EXTRA_OPEN_EPISODE = "open_episode"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notify_channel_episodes),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notify_channel_episodes_desc)
        }
        mgr.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        title: String,
        body: String,
        slug: String?,
        episode: Int?,
        notificationId: Int,
    ) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!slug.isNullOrBlank()) putExtra(EXTRA_OPEN_SLUG, slug)
            if (episode != null && episode > 0) putExtra(EXTRA_OPEN_EPISODE, episode)
        }
        val pi = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_app)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notif)
        }
    }
}
