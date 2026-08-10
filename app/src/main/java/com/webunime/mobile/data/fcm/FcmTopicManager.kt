package com.webunime.mobile.data.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * FCM topic per anime: `anime_<slug>` (karakter non-aman diganti `_`).
 */
object FcmTopicManager {
    private const val TAG = "FcmTopic"

    fun topicForSlug(slug: String): String {
        val safe = slug.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\-_.~%]"), "_")
            .take(80)
            .ifBlank { "unknown" }
        return "anime_$safe"
    }

    suspend fun subscribeSlug(slug: String) {
        val topic = topicForSlug(slug)
        runCatching {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Log.d(TAG, "subscribed $topic")
        }.onFailure { Log.w(TAG, "subscribe failed $topic: ${it.message}") }
    }

    suspend fun unsubscribeSlug(slug: String) {
        val topic = topicForSlug(slug)
        runCatching {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            Log.d(TAG, "unsubscribed $topic")
        }.onFailure { Log.w(TAG, "unsubscribe failed $topic: ${it.message}") }
    }

    /** Sinkronkan topic dengan daftar subscribe saat ini. */
    suspend fun syncTopics(desiredSlugs: Collection<String>, previousSlugs: Collection<String>) {
        val desired = desiredSlugs.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val previous = previousSlugs.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        for (slug in previous - desired) {
            unsubscribeSlug(slug)
        }
        for (slug in desired) {
            subscribeSlug(slug)
        }
    }

    suspend fun clearAll(slugs: Collection<String>) {
        for (slug in slugs) {
            unsubscribeSlug(slug)
        }
    }
}
