package com.hora.varisankya.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.hora.varisankya.R
import com.hora.varisankya.PreferenceHelper
import kotlin.math.abs

abstract class SwipeActionCallback(private val context: Context) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_delete_sweep)
    private val pauseIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_pause) // Ensure this exists or use standard
    private val checkIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_check_circle)

    private val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
    private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
    
    // Colors - Resolved Dynamically in onChildDraw or using cached context if theme doesn't change
    // Ideally resolve in Constructor if Context is Activity, but Theme might differ if using different contexts.
    // Safe to resolve lazily or in draw.
    
    // Using simple lazy properties if Context is reliable
    private val deleteColor by lazy { ThemeHelper.getErrorColor(context) }
    private val inactiveColor by lazy { ThemeHelper.getSurfaceContainerHighestColor(context) } 
    private val paidColor by lazy { 
        // Use Tertiary for "Positive" action if fits, or fall back to Primary.
        // M3 doesn't have "Success". Let's use Primary as per Plan, or a custom Green if user had it.
        // Reverting to Primary or Tertiary. Let's use PrimaryContainer for a softer look or just Primary.
        // Actually earlier we used 0xFF5BBd78. Let's try to find a dynamic match.
        // If we want M3E, we should use Semantic colors. 
        // "Marking as Paid" is a primary action.
        ThemeHelper.getPrimaryColor(context)
    }

    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    private val backgroundPaint = Paint().apply { style = Paint.Style.FILL }
    
    // Haptic State Tracking
    private val hapticStateMap = mutableMapOf<Long, HapticState>()
    private data class HapticState(
        var hasTriggeredStart: Boolean = false,
        var hasTriggeredDeep: Boolean = false
    )

    private val DEEP_SWIPE_THRESHOLD = 0.5f 

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        if (viewHolder.itemViewType != 0) return 0 
        // Only allow RIGHT swipe
        return makeMovementFlags(0, ItemTouchHelper.RIGHT)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }
        
        val id = viewHolder.itemId
        val state = hapticStateMap.getOrPut(id) { HapticState() }
        val progress = abs(dX) / itemView.width.toFloat()
        
        if (dX == 0f) {
            state.hasTriggeredStart = false
            state.hasTriggeredDeep = false
        }

        val radius = if (itemView is MaterialCardView) itemView.radius else 28f 

        if (dX > 0) {
            // SWIPE RIGHT -> MARK AS PAID (Positive Action)
            val triggerThreshold = 0.15f
            if (progress > triggerThreshold && !state.hasTriggeredStart) {
                PreferenceHelper.performHaptics(itemView, HapticFeedbackConstants.SEGMENT_TICK)
                state.hasTriggeredStart = true
            }
            
            // Background: Paid Color (Primary or Custom)
            val finalColor = paidColor
            backgroundPaint.color = finalColor
            
            val background = RectF(
                itemView.left.toFloat(),
                itemView.top.toFloat(),
                itemView.left.toFloat() + dX,
                itemView.bottom.toFloat()
            )
            c.drawRoundRect(background, radius, radius, backgroundPaint)

            val iconMargin = (itemHeight - intrinsicHeight) / 2
            val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val iconBottom = iconTop + intrinsicHeight
            val iconLeft = itemView.left + iconMargin
            val iconRight = itemView.left + iconMargin + intrinsicWidth

            // Use Check Icon for Paid
            checkIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            
            // Use OnPrimary for contrast
            checkIcon?.setTint(ThemeHelper.getOnPrimaryColor(context))
            checkIcon?.draw(c)
        }
        
        // No logic for Left Swipe (dX < 0) as it is blocked by makeMovementFlags
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }
    
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
         return 0.4f
    }
    
    // Abstract method to be implemented by activity
    // Only one action now: Mark Paid
    abstract fun onSwipeRight(position: Int)

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        val id = viewHolder.itemId
        
        // Clear state
        hapticStateMap.remove(id)

        if (direction == ItemTouchHelper.RIGHT) {
             onSwipeRight(position)
        }
    }
}
