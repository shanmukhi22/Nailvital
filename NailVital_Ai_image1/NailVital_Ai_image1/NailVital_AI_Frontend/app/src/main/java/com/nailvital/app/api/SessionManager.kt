package com.nailvital.app.api

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nailvital_prefs", Context.MODE_PRIVATE)

    companion object {
        const val USER_TOKEN = "user_token"
        const val USER_NAME = "user_name"
        const val USER_EMAIL = "user_email"
        const val REMINDER_ENABLED = "reminder_enabled"
    }

    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    fun isReminderEnabled(): Boolean {
        return prefs.getBoolean(REMINDER_ENABLED, false)
    }

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(REMINDER_ENABLED, enabled).apply()
    }

    fun saveUserDetails(name: String, email: String) {
        val editor = prefs.edit()
        editor.putString(USER_NAME, name)
        editor.putString(USER_EMAIL, email)
        editor.apply()
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
