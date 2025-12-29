package com.hora.varisankya

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions

open class BaseActivity : AppCompatActivity() {
    
    private var currentFontEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Cache the current preference value used to create this activity
        currentFontEnabled = PreferenceHelper.isGoogleFontEnabled(this)
        
        // 1. Apply the Base Theme (decides the font)
        if (!currentFontEnabled) {
            setTheme(R.style.Theme_Varisankya_SystemFont)
        } else {
            setTheme(R.style.Theme_Varisankya)
        }
        
        // 2. Apply Dynamic Colors Overlay ON TOP of the base theme
        val optionsBuilder = DynamicColorsOptions.Builder()
        
        // Try to find the Expressive overlay at runtime to avoid compile-time issues
        // It might be named differently or not exposed in R class in alpha version
        val overlayId = resources.getIdentifier("ThemeOverlay_Material3_DynamicColors_Expressive", "style", packageName)
        if (overlayId != 0) {
            optionsBuilder.setThemeOverlay(overlayId)
        }
        
        DynamicColors.applyToActivityIfAvailable(this, optionsBuilder.build())
        
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