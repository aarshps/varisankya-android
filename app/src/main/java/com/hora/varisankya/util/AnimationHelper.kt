package com.hora.varisankya.util

import android.view.MotionEvent
import android.view.View
import android.view.animation.Interpolator
import androidx.core.view.animation.PathInterpolatorCompat

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
     * Applies a tactile "Squish" spring effect when touched.
     * M3E Style: Responsive press (Emphasized Decelerate), fluid release (Emphasized).
     */
    fun applySpringOnTouch(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .setDuration(80)
                        .setInterpolator(EMPHASIZED_DECELERATE)
                        .start()
                    // We must return true to receive ACTION_UP/CANCEL
                    // But if it's a card/button we don't want to block the click
                    // Solution: View must be clickable
                    false 
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(EMPHASIZED)
                        .start()
                    false
                }
                else -> false
            }
        }
        
        // Ensure the view is clickable so it receives the full gesture sequence
        if (!view.isClickable) {
            view.isClickable = true
            view.isFocusable = true
        }
    }

    /**
     * Staggered entrance animation for RecyclerView items.
     * Call this in onBindViewHolder.
     * Uses M3 Emphasized Decelerate for natural entry.
     */
    fun animateEntrance(view: View, position: Int) {
        view.alpha = 0f
        view.translationY = 50f // Reduced travel distance for elegance
        
        // Stagger based on position (capped at 200ms delay max)
        val delay = (position * 25).toLong().coerceAtMost(300)
        
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500) // M3 standard long duration
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
            .setDuration(400)
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
        animator.duration = 1200 // Slower, more deliberate
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
