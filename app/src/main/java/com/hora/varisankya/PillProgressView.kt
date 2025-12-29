package com.hora.varisankya

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.min

class PillProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val path = Path()
    private val rectF = RectF()
    private val progressRectF = RectF()

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    // Color of the progress fill (foreground)
    var progressColor: Int = Color.BLACK
        set(value) {
            field = value
            invalidate()
        }

    // Color of the track (background)
    var pillBackgroundColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }
        
    // Unused in filled mode, kept for compatibility
    var trackColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }

    var cornerRadiusPx: Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 
        1000f, 
        context.resources.displayMetrics
    )
        set(value) {
            field = value
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePath(w, h)
    }

    private fun updatePath(w: Int, h: Int) {
        path.reset()
        rectF.set(0f, 0f, w.toFloat(), h.toFloat())
        
        // Calculate max possible radius for this size (pill shape)
        val maxRadius = min(rectF.width() / 2f, rectF.height() / 2f)
        val finalRadius = min(cornerRadiusPx, maxRadius)
        
        path.addRoundRect(rectF, finalRadius, finalRadius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        // 1. Draw Background (Track)
        if (pillBackgroundColor != Color.TRANSPARENT) {
            fillPaint.color = pillBackgroundColor
            canvas.drawPath(path, fillPaint)
        }

        // 2. Draw Progress Fill
        if (progress > 0) {
            fillPaint.color = progressColor
            
            // Clip to the pill shape to ensure the left side matches perfectly
            // and to contain any overflow if calculation is slightly off
            canvas.save()
            canvas.clipPath(path)
            
            val progressWidth = width * (progress / 100f)
            val radius = height / 2f
            
            // Draw a Rounded Rect for the progress to give it a soft, rounded leading edge
            progressRectF.set(0f, 0f, progressWidth, height.toFloat())
            canvas.drawRoundRect(progressRectF, radius, radius, fillPaint)
            
            canvas.restore()
        }
    }
}