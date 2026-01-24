package com.hora.varisankya.util

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors

object ThemeHelper {
    
    // Contrast reduction factor (20% = 0.20f blend towards surface)
    private const val CONTRAST_REDUCTION = 0.20f

    fun getPrimaryColor(context: Context): Int {
        val raw = resolveColor(context, "colorPrimary", Color.BLUE)
        return blendTowardsSurface(context, raw, CONTRAST_REDUCTION)
    }

    fun getTertiaryColor(context: Context): Int {
        val raw = resolveColor(context, "colorTertiary", Color.MAGENTA)
        return blendTowardsSurface(context, raw, CONTRAST_REDUCTION)
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
    
    fun getOnPrimaryColor(context: Context): Int {
        val raw = resolveColor(context, "colorOnPrimary", Color.WHITE)
        return blendTowardsSurface(context, raw, CONTRAST_REDUCTION)
    }
    
    fun getOnTertiaryColor(context: Context): Int {
        val raw = resolveColor(context, "colorOnTertiary", Color.WHITE)
        return blendTowardsSurface(context, raw, CONTRAST_REDUCTION)
    }
    
    fun getSurfaceVariantColor(context: Context): Int {
        return resolveColor(context, "colorSurfaceVariant", Color.LTGRAY)
    }
    
    fun getOnSurfaceVariantColor(context: Context): Int {
        return resolveColor(context, "colorOnSurfaceVariant", Color.DKGRAY)
    }
    
    fun getOnSurfaceColor(context: Context): Int {
        return resolveColor(context, "colorOnSurface", Color.BLACK)
    }
    
    fun getOutlineVariantColor(context: Context): Int {
        return resolveColor(context, "colorOutlineVariant", Color.LTGRAY)
    }
    
    /**
     * Blends a color towards the surface color by the given ratio.
     * ratio = 0.0 means no change, ratio = 1.0 means fully surface color.
     */
    private fun blendTowardsSurface(context: Context, color: Int, ratio: Float): Int {
        val surface = getSurfaceColor(context)
        return ColorUtils.blendARGB(color, surface, ratio)
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
