package com.webunime.mobile.data.user

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.unlockStore by preferencesDataStore("webunime_episode_unlocks")

class EpisodeUnlockStore(private val context: Context) {
    private val kUnlocks = stringSetPreferencesKey("unlocks")

    val unlocksFlow: Flow<Set<String>> = context.unlockStore.data.map { it[kUnlocks] ?: emptySet() }

    suspend fun isUnlocked(slug: String, episode: Int): Boolean {
        val key = key(slug, episode)
        return (context.unlockStore.data.first()[kUnlocks] ?: emptySet()).contains(key)
    }

    suspend fun markUnlocked(slug: String, episode: Int) {
        val key = key(slug, episode)
        context.unlockStore.edit { prefs ->
            val cur = prefs[kUnlocks]?.toMutableSet() ?: mutableSetOf()
            cur.add(key)
            prefs[kUnlocks] = cur
        }
    }

    companion object {
        fun key(slug: String, episode: Int) = "$slug#$episode"
    }
}
