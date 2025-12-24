package com.hora.varisankya

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()

        setupThemeToggle()
        setupNotificationTimeSetting()
        setupHapticsToggle()
        setupPrivacyPolicy()
        setupLogoutButton()
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
                rescheduleNotifications(picker.hour, picker.minute)
            }

            picker.show(supportFragmentManager, "NOTIFICATION_TIME_PICKER")
        }
    }

    private fun updateTimeText(textView: TextView, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        
        val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        val hour12 = if (calendar.get(Calendar.HOUR) == 0) 12 else calendar.get(Calendar.HOUR)
        
        textView.text = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm)
    }

    private fun rescheduleNotifications(hour: Int, minute: Int) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
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
        val hapticsSwitch = findViewById<MaterialSwitch>(R.id.haptics_switch)
        hapticsSwitch.isChecked = PreferenceHelper.isHapticsEnabled(this)
        
        hapticsSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferenceHelper.setHapticsEnabled(this, isChecked)
            if (isChecked) {
                hapticsSwitch.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
        }
    }

    private fun setupPrivacyPolicy() {
        val privacyPolicyLayout = findViewById<View>(R.id.privacy_policy_layout)
        privacyPolicyLayout.setOnClickListener {
            PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
            // Updated to the URL provided by the user
            val policyUrl = "https://github.com/aarshps/varisankya-android/blob/master/PRIVACY.md"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(policyUrl))
            startActivity(browserIntent)
        }
    }

    private fun setupLogoutButton() {
        val logoutButton = findViewById<Button>(R.id.logout_button)
        logoutButton.setOnClickListener { view ->
            PreferenceHelper.performHaptics(view, HapticFeedbackConstants.CONFIRM)

            // Sign out of Firebase
            auth.signOut()

            // Return to the main activity, which will now show the login screen
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        PreferenceHelper.performHaptics(window.decorView, HapticFeedbackConstants.KEYBOARD_TAP)
        finish()
        return true
    }
}