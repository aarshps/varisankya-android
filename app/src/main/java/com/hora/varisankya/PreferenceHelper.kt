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
    private const val KEY_USE_GOOGLE_FONT = "use_google_font"
    private const val KEY_NOTIF_DAYS = "notification_days"
    private const val KEY_PAYMENT_VIEW_MODE = "payment_view_mode" // "list" or "chart" (current session)
    private const val KEY_DEFAULT_PAYMENT_VIEW = "default_payment_view" // "list" or "chart" (user setting)

    fun customPreference(context: Context, name: String): android.content.SharedPreferences {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

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

    fun isGoogleFontEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_USE_GOOGLE_FONT, true)
    }

    fun setGoogleFontEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USE_GOOGLE_FONT, enabled).apply()
    }

    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    fun isBiometricEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
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

    fun getNotificationDays(context: Context): Int {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NOTIF_DAYS, 7) // Default 7 days
    }

    fun setNotificationDays(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NOTIF_DAYS, days).apply()
    }

    fun getDefaultPaymentView(context: Context): String {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEFAULT_PAYMENT_VIEW, "chart") ?: "chart"
    }

    fun setDefaultPaymentView(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEFAULT_PAYMENT_VIEW, mode).apply()
    }

    fun getPaymentViewMode(context: Context): String {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        // If no session override exists, return the user's default setting
        return prefs.getString(KEY_PAYMENT_VIEW_MODE, null) ?: getDefaultPaymentView(context)
    }

    fun setPaymentViewMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PAYMENT_VIEW_MODE, mode).apply()
    }

    fun performHaptics(view: View, feedbackConstant: Int) {
        if (isHapticsEnabled(view.context)) {
            view.performHapticFeedback(feedbackConstant)
        }
    }

    /**
     * Performs a strong "Success" or "Confirm" haptic.
     * Uses CONFIRM on Android R+, else CONTEXT_CLICK.
     */
    fun performSuccessHaptic(view: View) {
        if (!isHapticsEnabled(view.context)) return
        
        val constant = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }
        view.performHapticFeedback(constant)
    }

    /**
     * Performs a light "Click" or "Tick" haptic.
     * Uses CLOCK_TICK or KEYBOARD_TAP.
     */
    fun performClickHaptic(view: View) {
        if (!isHapticsEnabled(view.context)) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Performs an "Error" or "Reject" haptic.
     */
    fun performErrorHaptic(view: View) {
         if (!isHapticsEnabled(view.context)) return
         
         val constant = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS // Fallback heavy vibration
        }
        view.performHapticFeedback(constant)
    }

    /**
     * Attaches a subtle scroll haptic engine to a RecyclerView.
     * Triggers a light TICK every time the user scrolls past a distance equivalent to ~1 item (80dp).
     * Creates a premium "mechanical wheel" feel.
     */
    fun attachScrollHaptics(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        if (!isHapticsEnabled(recyclerView.context)) return

        recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            private var accumulatedDy = 0
            private val HAPTIC_THRESHOLD_PX = (80 * recyclerView.context.resources.displayMetrics.density).toInt() // Typical item height

            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                accumulatedDy += dy

                if (Math.abs(accumulatedDy) >= HAPTIC_THRESHOLD_PX) {
                    // Trigger subtle tick
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        recyclerView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    } else {
                        recyclerView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    // Reset accumulator, keeping remainder for precision
                    accumulatedDy %= HAPTIC_THRESHOLD_PX
                }
            }
        })
    }
}