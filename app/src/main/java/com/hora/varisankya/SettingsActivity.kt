package com.hora.varisankya

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class SettingsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        auth = FirebaseAuth.getInstance()

        setupLogoutButton()
        setupThemeToggle()
        setupFontToggle()
        setupNotificationTimeSetting()
        setupNotificationDaysSetting()
        setupHapticsToggle()
        setupPrivacyPolicy()
        setupPaymentViewToggle()
    }

    private fun setupPaymentViewToggle() {
        val paymentViewToggleGroup = findViewById<ChipGroup>(R.id.payment_view_toggle_group)
        val currentDefault = PreferenceHelper.getDefaultPaymentView(this)
        
        if (currentDefault == "chart") {
            paymentViewToggleGroup.check(R.id.view_chart)
        } else {
            paymentViewToggleGroup.check(R.id.view_list)
        }

        paymentViewToggleGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                PreferenceHelper.performHaptics(group, HapticFeedbackConstants.KEYBOARD_TAP)
                val mode = if (checkedIds[0] == R.id.view_chart) "chart" else "list"
                PreferenceHelper.setDefaultPaymentView(this, mode)
            }
        }
    }

    private fun setupLogoutButton() {
        val logoutButton = findViewById<View>(R.id.logout_button)
        logoutButton.setOnClickListener {
            PreferenceHelper.performHaptics(window.decorView, HapticFeedbackConstants.CONFIRM)
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupThemeToggle() {
        val themeToggleGroup = findViewById<ChipGroup>(R.id.theme_toggle_group)
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> themeToggleGroup.check(R.id.theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> themeToggleGroup.check(R.id.theme_dark)
            else -> themeToggleGroup.check(R.id.theme_device)
        }

        themeToggleGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                PreferenceHelper.performHaptics(group, HapticFeedbackConstants.KEYBOARD_TAP)
                when (checkedIds[0]) {
                    R.id.theme_light -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    R.id.theme_dark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    R.id.theme_device -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
    }

    private fun setupFontToggle() {
        val fontToggleGroup = findViewById<ChipGroup>(R.id.font_toggle_group)
        
        if (PreferenceHelper.isGoogleFontEnabled(this)) {
            fontToggleGroup.check(R.id.font_google)
        } else {
            fontToggleGroup.check(R.id.font_system)
        }

        fontToggleGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                PreferenceHelper.performHaptics(group, HapticFeedbackConstants.KEYBOARD_TAP)
                val enableGoogleFont = checkedIds[0] == R.id.font_google
                
                // Only recreate if preference actually changed
                if (enableGoogleFont != PreferenceHelper.isGoogleFontEnabled(this)) {
                    PreferenceHelper.setGoogleFontEnabled(this, enableGoogleFont)
                    
                    // Restart Activity to apply theme
                    val intent = intent
                    finish()
                    startActivity(intent)
                    overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }
        }
    }

    private fun setupNotificationTimeSetting() {
        val timeSettingLayout = findViewById<View>(R.id.notification_time_setting)
        val timeTextView = findViewById<TextView>(R.id.notification_time_text)

        val currentHour = PreferenceHelper.getNotificationHour(this)
        val currentMinute = PreferenceHelper.getNotificationMinute(this)
        
        updateTimeText(timeTextView, currentHour, currentMinute)

        timeSettingLayout.setOnClickListener {
            PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
            
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(currentHour)
                .setMinute(currentMinute)
                .setTitleText("Select Reminder Time")
                .build()

            picker.addOnPositiveButtonClickListener {
                PreferenceHelper.setNotificationTime(this, picker.hour, picker.minute)
                updateTimeText(timeTextView, picker.hour, picker.minute)
                rescheduleNotifications()
            }

            picker.show(supportFragmentManager, "NOTIFICATION_TIME_PICKER")
        }
    }

    private fun setupNotificationDaysSetting() {
        val slider = findViewById<Slider>(R.id.notification_days_slider)
        val label = findViewById<TextView>(R.id.notification_days_label)

        val currentDays = PreferenceHelper.getNotificationDays(this)
        // Ensure slider value is within bounds (0-10)
        slider.value = currentDays.toFloat().coerceIn(0f, 10f)
        label.text = "$currentDays days before"

        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                PreferenceHelper.performHaptics(slider, HapticFeedbackConstants.CLOCK_TICK)
                val days = value.toInt()
                label.text = "$days days before"
            }
        }

        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // No-op
            }

            override fun onStopTrackingTouch(slider: Slider) {
                val days = slider.value.toInt()
                PreferenceHelper.setNotificationDays(this@SettingsActivity, days)
            }
        })
    }

    private fun updateTimeText(textView: TextView, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        
        val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        val hour12 = if (calendar.get(Calendar.HOUR) == 0) 12 else calendar.get(Calendar.HOUR)
        
        textView.text = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm)
    }

    private fun rescheduleNotifications(hour: Int? = null, minute: Int? = null) {
        val targetHour = hour ?: PreferenceHelper.getNotificationHour(this)
        val targetMinute = minute ?: PreferenceHelper.getNotificationMinute(this)

        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<SubscriptionNotificationWorker>(
            24, TimeUnit.HOURS
        )
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "subscription_notifications",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun setupHapticsToggle() {
        val hapticsToggleGroup = findViewById<ChipGroup>(R.id.haptics_toggle_group)
        
        if (PreferenceHelper.isHapticsEnabled(this)) {
            hapticsToggleGroup.check(R.id.haptics_on)
        } else {
            hapticsToggleGroup.check(R.id.haptics_off)
        }
        
        hapticsToggleGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                PreferenceHelper.performHaptics(group, HapticFeedbackConstants.KEYBOARD_TAP)
                val enableHaptics = checkedIds[0] == R.id.haptics_on
                PreferenceHelper.setHapticsEnabled(this, enableHaptics)
            }
        }
    }

    private fun setupPrivacyPolicy() {
        val privacyPolicyLayout = findViewById<View>(R.id.privacy_policy_layout)
        privacyPolicyLayout.setOnClickListener {
            PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
            val policyUrl = "https://github.com/aarshps/varisankya-android/blob/master/PRIVACY.md"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(policyUrl))
            startActivity(browserIntent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        PreferenceHelper.performHaptics(window.decorView, HapticFeedbackConstants.KEYBOARD_TAP)
        finish()
        return true
    }
}