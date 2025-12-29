package com.hora.varisankya

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions

class VarisankyaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic colors to all activities if available
        val optionsBuilder = DynamicColorsOptions.Builder()
        val overlayId = resources.getIdentifier("ThemeOverlay_Material3_DynamicColors_Expressive", "style", packageName)
        if (overlayId != 0) {
             optionsBuilder.setThemeOverlay(overlayId)
        }
        DynamicColors.applyToActivitiesIfAvailable(this, optionsBuilder.build())
    }
}