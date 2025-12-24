package com.hora.varisankya

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View

object PreferenceHelper {

    private const val PREFS_NAME = "DropdownPrefs"
    private const val APP_PREFS = "AppPrefs"
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    private const val KEY_NOTIF_HOUR = "notification_hour"
    private const val KEY_NOTIF_MINUTE = "notification_minute"

    fun recordUsage(context: Context, preferenceKey: String, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("${preferenceKey}_${value}", 0)
        prefs.edit().putInt("${preferenceKey}_${value}", currentCount + 1).apply()
    }

    fun getPersonalizedList(context: Context, preferenceKey: String, defaultList: Array<String>): Array<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val usageCounts = defaultList.map { it to prefs.getInt("${preferenceKey}_${it}", 0) }
        
        val sortedList = usageCounts.sortedByDescending { it.second }.map { it.first }
        
        return sortedList.toTypedArray()
    }

    fun isHapticsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
    }

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    fun getNotificationHour(context: Context): Int {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NOTIF_HOUR, 8) // Default 8 AM
    }

    fun getNotificationMinute(context: Context): Int {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NOTIF_MINUTE, 0) // Default 0 min
    }

    fun setNotificationTime(context: Context, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_NOTIF_HOUR, hour)
            .putInt(KEY_NOTIF_MINUTE, minute)
            .apply()
    }

    fun performHaptics(view: View, feedbackConstant: Int) {
        if (isHapticsEnabled(view.context)) {
            view.performHapticFeedback(feedbackConstant)
        }
    }
}