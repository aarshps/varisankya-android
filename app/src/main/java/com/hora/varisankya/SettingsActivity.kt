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
import androidx.core.widget.NestedScrollView
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Math.abs
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.slider.Slider
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

import androidx.biometric.BiometricManager
import com.google.android.material.materialswitch.MaterialSwitch
import com.hora.varisankya.util.BiometricAuthManager

class SettingsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        auth = FirebaseAuth.getInstance()

        setupLogoutButton()
        setupThemeToggle()
        setupFontToggle()
        setupNotificationTimeSetting()
        setupNotificationDaysSetting()
        setupHapticsToggle()
        setupPrivacyPolicy()
        setupScrollHaptics()
        setupBiometricToggle()
        setupCardGrouping()
    }
    
    private fun setupBiometricToggle() {
        val biometricSwitch = findViewById<MaterialSwitch>(R.id.switch_biometric)
        val context = this
        
        // Initial state
        biometricSwitch.isChecked = PreferenceHelper.isBiometricEnabled(context)
        
        // Listen for changes
        // Use setOnClickListener instead of setOnCheckedChangeListener to intercept the UX
        biometricSwitch.setOnClickListener {
            val isTurningOn = biometricSwitch.isChecked
            
            PreferenceHelper.performHaptics(biometricSwitch, HapticFeedbackConstants.CLOCK_TICK)
            
            if (isTurningOn) {
                // If turning ON, must verify compatibility and then authenticate to confirm
                if (BiometricAuthManager.isBiometricAvailable(context)) {
                    // Try to authenticate to prove they can use it
                    BiometricAuthManager.authenticate(this,
                        onSuccess = {
                            // Success: Actually enable it
                            PreferenceHelper.setBiometricEnabled(context, true)
                        },
                        onError = {
                            // Fail: Revert switch
                            biometricSwitch.isChecked = false
                        }
                    )
                } else {
                    // Biometrics not available
                    biometricSwitch.isChecked = false
                }
            } else {
                // Turning OFF: Just disable it (or could auth here too, but usually simple off is okay)
                PreferenceHelper.setBiometricEnabled(context, false)
            }
        }
    }

    private fun setupScrollHaptics() {
        val scrollView = findViewById<NestedScrollView>(R.id.settings_scroll_view)
        var lastScrollY = 0

        scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (abs(scrollY - lastScrollY) > 150) {
                PreferenceHelper.performHaptics(v, HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                lastScrollY = scrollY
            }
        })
    }

    private fun setupCardGrouping() {
        val cards = listOf(
            findViewById<MaterialCardView>(R.id.card_security),
            findViewById<MaterialCardView>(R.id.card_appearance),
            findViewById<MaterialCardView>(R.id.card_typography),
            findViewById<MaterialCardView>(R.id.card_notifications),
            findViewById<MaterialCardView>(R.id.card_haptics),
            findViewById<MaterialCardView>(R.id.card_legal)
        )

        cards.forEachIndexed { index, card ->
            val isFirst = index == 0
            val isLast = index == cards.size - 1
            val isSingle = cards.size == 1

            val styleRes = when {
                isSingle -> R.style.ShapeAppearance_App_SingleItem
                isFirst -> R.style.ShapeAppearance_App_FirstItem
                isLast -> R.style.ShapeAppearance_App_LastItem
                else -> R.style.ShapeAppearance_App_MiddleItem
            }
            card.shapeAppearanceModel = ShapeAppearanceModel.builder(this, styleRes, 0).build()
        }
    }



    private fun updateChipShape(chip: com.google.android.material.chip.Chip) {
        val r = resources.displayMetrics.density
        val selectedRadius = 12f * r
        val unselectedRadius = 100f * r
        
        chip.shapeAppearanceModel = chip.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(if (chip.isChecked) selectedRadius else unselectedRadius)
            .build()
    }

    private fun updateGroupShapes(group: ChipGroup) {
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? com.google.android.material.chip.Chip
            chip?.let { updateChipShape(it) }
        }
    }

    private fun setupLogoutButton() {
        val logoutButton = findViewById<View>(R.id.logout_button)
        logoutButton.setOnClickListener {
            PreferenceHelper.performHaptics(window.decorView, HapticFeedbackConstants.CONFIRM)
            
            // Fire and forget: Clear system credential state in background
            // We use a detached scope because the activity will die immediately
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val credentialManager = CredentialManager.create(applicationContext)
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Instant navigation
            auth.signOut()
            val intent = Intent(this@SettingsActivity, MainActivity::class.java)
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
        updateGroupShapes(themeToggleGroup)

        themeToggleGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                PreferenceHelper.performHaptics(group, HapticFeedbackConstants.KEYBOARD_TAP)
                when (checkedIds[0]) {
                    R.id.theme_light -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    R.id.theme_dark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    R.id.theme_device -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                updateGroupShapes(group)
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
        updateGroupShapes(fontToggleGroup)

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
                updateGroupShapes(group)
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
        updateGroupShapes(hapticsToggleGroup)
        
        hapticsToggleGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                PreferenceHelper.performHaptics(group, HapticFeedbackConstants.KEYBOARD_TAP)
                val enableHaptics = checkedIds[0] == R.id.haptics_on
                PreferenceHelper.setHapticsEnabled(this, enableHaptics)
                updateGroupShapes(group)
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