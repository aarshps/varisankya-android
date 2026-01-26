package com.hora.varisankya

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import com.google.android.material.color.DynamicColors

open class BaseActivity : AppCompatActivity() {
    
    private var currentFontEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        
        // Cache the current preference value used to create this activity
        currentFontEnabled = PreferenceHelper.isGoogleFontEnabled(this)
        
        // 1. Apply the Base Theme (decides the font)
        if (!currentFontEnabled) {
            setTheme(R.style.Theme_Varisankya_SystemFont)
        } else {
            setTheme(R.style.Theme_Varisankya)
        }
        
        // VITAL: Re-apply Dynamic Colors because the manual setTheme() above overrides 
        // the global Application callback (which happens in onActivityPreCreated).
        DynamicColors.applyToActivityIfAvailable(this)
        
        super.onCreate(savedInstanceState)


    }

    override fun onResume() {
        super.onResume()
        // Check if the preference has changed since this activity was created/last resumed.
        // If it has changed (e.g., changed in SettingsActivity and we are now returning to MainActivity),
        // we recreate this activity to apply the new theme.
        if (currentFontEnabled != PreferenceHelper.isGoogleFontEnabled(this)) {
            recreate()
        }
    }
}