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
                        .scaleX(0.95f) // Subtle squish (was 0.92)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .setInterpolator(EMPHASIZED_DECELERATE)
                        .start()
                    // Allow click events to propagate
                    false 
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(400) // Longer settle time for premium feel
                        .setInterpolator(EMPHASIZED) // Smooth settle, no wobble
                        .start()
                    false
                }
                else -> false
            }
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
        
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            // Smart formatting: Remove decimal if .00
            val formatted = if(value % 1.0 < 0.01) String.format("%.0f", value) else String.format("%.2f", value)
            textView.text = "$prefix$formatted$suffix"
        }
        
        animator.start()
    }
}
