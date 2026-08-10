package com.webunime.mobile.data.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.math.absoluteValue

class WeeboonimeMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Topic-based: token refresh tidak perlu di-upload untuk Fase 2
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val slug = data["slug"]
        val episode = data["episode"]?.toIntOrNull()
        val title = message.notification?.title
            ?: data["title"]
            ?: "Episode baru"
        val body = message.notification?.body
            ?: data["body"]
            ?: buildString {
                append(data["animeTitle"] ?: slug ?: "Anime")
                if (episode != null) append(" — Episode $episode")
            }
        val id = (slug ?: title).hashCode().absoluteValue
        EpisodeNotify.show(
            context = this,
            title = title,
            body = body,
            slug = slug,
            episode = episode,
            notificationId = id,
        )
    }
}
