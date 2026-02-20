package com.hora.varisankya.util

import android.view.MotionEvent
import android.view.View
import android.view.animation.Interpolator
import androidx.core.view.animation.PathInterpolatorCompat
import com.hora.varisankya.Constants

object AnimationHelper {

    // M3 Standard Emphasized Easing (The "Google Feel")
    // Used for most UI movement (Dialogs, Shared Axis, Navigation)
    val EMPHASIZED: Interpolator = PathInterpolatorCompat.create(0.2f, 0.0f, 0.0f, 1.0f)

    // M3 Emphasized Decelerate (Incoming objects)
    // Used for entrances, lists loading in
    val EMPHASIZED_DECELERATE: Interpolator = PathInterpolatorCompat.create(0.05f, 0.7f, 0.1f, 1.0f)

    // M3 Emphasized Accelerate (Outgoing objects)
    val EMPHASIZED_ACCELERATE: Interpolator = PathInterpolatorCompat.create(0.3f, 0.0f, 0.8f, 0.15f)



    /**
     * Staggered entrance animation for RecyclerView items.
     * Call this in onBindViewHolder.
     * Uses M3 Emphasized Decelerate for natural entry.
     */
    fun animateEntrance(view: View, position: Int) {
        view.alpha = 0f
        view.translationY = 50f // Reduced travel distance for elegance
        view.scaleX = 0.85f // M3E Depth Entry Scale
        view.scaleY = 0.85f
        
        // Stagger based on position (capped at 300ms delay max)
        val delay = (position * Constants.ANIM_STAGGER_BASE_DELAY).coerceAtMost(400)
        
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(Constants.ANIM_DURATION_LONG)
            .setStartDelay(delay)
            .setInterpolator(EMPHASIZED_DECELERATE)
            .start()
    }

    /**
     * Smooth reveal for containers
     */
    fun animateReveal(view: View) {
        view.alpha = 0f
        view.scaleX = 0.92f
        view.scaleY = 0.92f
        
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(Constants.ANIM_DURATION_LONG)
            .setInterpolator(EMPHASIZED)
            .start()
    }

    /**
     * Animates a TextView's number from 0 to target value.
     * Preserves prefix/suffix if passed.
     * Uses LTR embedding to handle RTL currency symbols properly.
     */
    fun animateTextCountUp(
        textView: android.widget.TextView, 
        targetValue: Double, 
        prefix: String = "", 
        suffix: String = ""
    ) {
        val animator = android.animation.ValueAnimator.ofFloat(0f, targetValue.toFloat())
        animator.duration = Constants.ANIM_DURATION_EXTRA_LONG
        animator.interpolator = EMPHASIZED
        
        // LTR embedding marks to handle RTL currency symbols
        val ltrMark = "\u200E"
        
        // Check if target has decimals to keep formatting stable during animation
        val hasDecimals = targetValue % 1.0 >= 0.01
        
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            
            // Stabilize formatting to prevent width jumps during animation
            val formattedNoSymbol = if (hasDecimals) {
                String.format(java.util.Locale.US, "%.2f", value)
            } else {
                String.format(java.util.Locale.US, "%.0f", value)
            }
            
            if (prefix.isNotEmpty() && prefix.endsWith(" ")) {
                val symbol = prefix.trim()
                val fullText = "$ltrMark$symbol $formattedNoSymbol$suffix"
                val spannable = android.text.SpannableStringBuilder(fullText)
                
                // Find symbol start and end in the full text
                val symbolStart = fullText.indexOf(symbol)
                if (symbolStart != -1) {
                    spannable.setSpan(
                        android.text.style.RelativeSizeSpan(0.5f),
                        symbolStart,
                        symbolStart + symbol.length,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                textView.text = spannable
            } else {
                textView.text = "$ltrMark$prefix$formattedNoSymbol$suffix"
            }
        }
        
        animator.start()
    }
}
