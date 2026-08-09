package com.webunime.mobile.data

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()
        }

    var displayName: String
        get() = prefs.getString(KEY_NAME, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_NAME, value).apply()
        }

    fun loginAs(name: String) {
        displayName = name
        isLoggedIn = true
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "weeboonime_session"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_NAME = "display_name"
    }
}
