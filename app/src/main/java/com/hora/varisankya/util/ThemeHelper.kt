package com.hora.varisankya.util

import android.content.Context
import android.graphics.Color
import com.google.android.material.color.MaterialColors



object ThemeHelper {

    fun getPrimaryColor(context: Context): Int {
        return resolveColor(context, "colorPrimary", Color.BLUE)
    }

    fun getTertiaryColor(context: Context): Int {
        return resolveColor(context, "colorTertiary", Color.MAGENTA)
    }

    fun getErrorColor(context: Context): Int {
        return resolveColor(context, "colorError", Color.RED)
    }

    fun getSecondaryContainerColor(context: Context): Int {
        return resolveColor(context, "colorSecondaryContainer", Color.LTGRAY)
    }

    fun getOnSecondaryContainerColor(context: Context): Int {
        return resolveColor(context, "colorOnSecondaryContainer", Color.BLACK)
    }
    
    fun getSurfaceColor(context: Context): Int {
        return resolveColor(context, "colorSurface", Color.WHITE)
    }

    private fun resolveColor(context: Context, attrName: String, defaultColor: Int): Int {
        // Try finding the attribute ID in the app's package (merged attributes)
        var attrId = context.resources.getIdentifier(attrName, "attr", context.packageName)
        if (attrId == 0) {
            // Fallback to android package
            attrId = context.resources.getIdentifier(attrName, "attr", "android")
        }
        if (attrId == 0) {
             // Basic fallback for material strict names if needed, but usually in app package
        }
        
        return if (attrId != 0) {
            MaterialColors.getColor(context, attrId, defaultColor)
        } else {
            defaultColor
        }
    }
}
