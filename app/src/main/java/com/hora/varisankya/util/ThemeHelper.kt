package com.hora.varisankya.util

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import com.google.android.material.color.MaterialColors

object ThemeHelper {

    private val idCache = mutableMapOf<String, Int>()

    private fun resolveAttrId(context: Context, attrName: String): Int {
        return idCache.getOrPut(attrName) {
            // First try the app's attributes (where Material attributes are merged)
            var id = context.resources.getIdentifier(attrName, "attr", context.packageName)
            if (id == 0) {
                // Then try android system attributes
                id = context.resources.getIdentifier(attrName, "attr", "android")
            }
            id
        }
    }

    private fun getColor(context: Context, attrName: String, fallback: Int): Int {
        val attrId = resolveAttrId(context, attrName)
        return if (attrId != 0) {
            MaterialColors.getColor(context, attrId, fallback)
        } else {
            fallback
        }
    }

    fun getPrimaryColor(context: Context): Int = getColor(context, "colorPrimary", Color.BLACK)
    fun getOnPrimaryColor(context: Context): Int = getColor(context, "colorOnPrimary", Color.WHITE)
    fun getPrimaryContainerColor(context: Context): Int = getColor(context, "colorPrimaryContainer", Color.DKGRAY)
    fun getOnPrimaryContainerColor(context: Context): Int = getColor(context, "colorOnPrimaryContainer", Color.WHITE)
    
    fun getSecondaryColor(context: Context): Int = getColor(context, "colorSecondary", Color.DKGRAY)
    fun getOnSecondaryColor(context: Context): Int = getColor(context, "colorOnSecondary", Color.WHITE)
    fun getSecondaryContainerColor(context: Context): Int = getColor(context, "colorSecondaryContainer", Color.LTGRAY)
    fun getOnSecondaryContainerColor(context: Context): Int = getColor(context, "colorOnSecondaryContainer", Color.BLACK)
    
    fun getTertiaryColor(context: Context): Int = getColor(context, "colorTertiary", Color.DKGRAY)
    fun getOnTertiaryColor(context: Context): Int = getColor(context, "colorOnTertiary", Color.WHITE)
    
    fun getSurfaceColor(context: Context): Int = getColor(context, "colorSurface", Color.WHITE)
    fun getOnSurfaceColor(context: Context): Int = getColor(context, "colorOnSurface", Color.BLACK)
    fun getSurfaceVariantColor(context: Context): Int = getColor(context, "colorSurfaceVariant", Color.LTGRAY)
    fun getOnSurfaceVariantColor(context: Context): Int = getColor(context, "colorOnSurfaceVariant", Color.GRAY)
    
    fun getErrorColor(context: Context): Int = getColor(context, "colorError", Color.RED)
    fun getOnErrorColor(context: Context): Int = getColor(context, "colorOnError", Color.WHITE)
    fun getErrorContainerColor(context: Context): Int = getColor(context, "colorErrorContainer", Color.RED)
    fun getOnErrorContainerColor(context: Context): Int = getColor(context, "colorOnErrorContainer", Color.WHITE)
    
    fun getOutlineVariantColor(context: Context): Int = getColor(context, "colorOutlineVariant", Color.LTGRAY)
    fun getSurfaceContainerHighColor(context: Context): Int = getColor(context, "colorSurfaceContainerHigh", Color.LTGRAY)
    fun getSurfaceContainerColor(context: Context): Int = getColor(context, "colorSurfaceContainer", Color.WHITE)
    fun getSurfaceContainerLowColor(context: Context): Int = getColor(context, "colorSurfaceContainerLow", Color.WHITE)
    fun getSurfaceContainerHighestColor(context: Context): Int = getColor(context, "colorSurfaceContainerHighest", Color.LTGRAY)
    fun getReferenceColor(context: Context, attrId: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.data
    }
}




