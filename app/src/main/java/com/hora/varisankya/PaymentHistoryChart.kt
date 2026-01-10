package com.hora.varisankya

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.hora.varisankya.util.ThemeHelper
import kotlin.math.max

class PaymentHistoryChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Data Class for robust handling
    data class ChartItem(
        val label: String,
        val value: Double,
        val symbol: String,
        val payload: Any?
    )

    private val dataPoints = mutableListOf<ChartItem>()
    private var onBarClickListener: ((Any?) -> Unit)? = null

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }


    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#15424242") // fallback
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
        textAlign = Paint.Align.CENTER
        // Set font to sans-serif-medium
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = Color.GRAY
    }

    // Dimensions
    private val barWidth = 120f
    private val barSpacing = 60f 
    private val chartPaddingTop = 120f 
    private val chartPaddingBottom = 160f 
    private val labelPadding = 16f
    private val cornerRadius = 60f 

    fun setChartData(data: List<ChartItem>) {
        animateDataUpdate(data)
    }

    fun setOnBarClickListener(listener: (Any?) -> Unit) {
        this.onBarClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val minWidthView = MeasureSpec.getSize(widthMeasureSpec)

        val contentWidth = if (dataPoints.isNotEmpty()) {
            (dataPoints.size * (barWidth + barSpacing) + barSpacing + 150f).toInt()
        } else {
            0
        }

        val finalWidth = max(minWidthView, contentWidth)
        setMeasuredDimension(finalWidth, heightSize)
    }

    // Animation State
    private var animationProgress = 1f
    private var isAnimating = false
    private var oldDataPoints = listOf<ChartItem>()

    fun animateDataUpdate(newData: List<ChartItem>) {
        oldDataPoints = ArrayList(dataPoints)
        dataPoints.clear()
        dataPoints.addAll(newData)
        
        requestLayout()
        
        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 600
        animator.interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
        animator.addUpdateListener { 
            animationProgress = it.animatedValue as Float
            invalidate()
        }
        isAnimating = true
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        
        val availableHeight = height - chartPaddingTop - chartPaddingBottom

        // Resolve Colors via ThemeHelper
        val colorPrimary = ThemeHelper.getPrimaryColor(context)
        val colorTertiary = ThemeHelper.getTertiaryColor(context)
        val resolvedErrorColor = ThemeHelper.getErrorColor(context)

        // M3 colors
        val colorSecondaryContainer = ThemeHelper.getSecondaryContainerColor(context)
        val colorOnSecondaryContainer = ThemeHelper.getOnSecondaryContainerColor(context)
        
        datePaint.color = colorOnSecondaryContainer
        labelBgPaint.color = colorSecondaryContainer
        textPaint.color = colorOnSecondaryContainer

        // Scaling (safety check for 0)
        val maxAmount = dataPoints.maxOfOrNull { it.value.toFloat() } ?: 100f
        val rangeY = max(maxAmount, 100f)

        // Calculate total content width correctly
        val totalContentWidth = (dataPoints.size * (barWidth + barSpacing)) + barSpacing
        
        // Center content if smaller
        val startOffset = if (totalContentWidth < width) {
            (width - totalContentWidth) / 2f
        } else {
            0f
        }

        dataPoints.forEachIndexed { index, item ->
            val label = item.label
            val rawAmount = item.value.toFloat()
            val symbol = item.symbol
            
            // Use Primary color for all bars
            barPaint.color = colorPrimary
            val amount = if (isAnimating) rawAmount * animationProgress else rawAmount

            // Calculate Position
            val xCenter = startOffset + barSpacing + (index * (barWidth + barSpacing)) + (barWidth / 2)
            
            val barHeight = (amount / rangeY) * availableHeight
            val visualBarHeight = max(barHeight, 20f) 

            val barTop = height - chartPaddingBottom - visualBarHeight
            val barBottom = height - chartPaddingBottom

            // Draw Bar
            val rect = RectF(
                xCenter - barWidth / 2,
                barTop,
                xCenter + barWidth / 2,
                barBottom
            )
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, barPaint)

            // --- Draw X-Axis Label ---
            var labelTextX = label
            if (labelTextX.length > 20) {
                labelTextX = labelTextX.take(18) + "..."
            }
            val dateTextWidth = datePaint.measureText(labelTextX)
            val labelYCenter = height - (chartPaddingBottom / 2)
            
            canvas.save()
            canvas.rotate(-45f, xCenter, labelYCenter)
            
            val dateBgRect = RectF(
                xCenter - dateTextWidth / 2 - labelPadding,
                labelYCenter - 30f, 
                xCenter + dateTextWidth / 2 + labelPadding,
                labelYCenter + 30f
            )
            canvas.drawRoundRect(dateBgRect, 20f, 20f, labelBgPaint)
            val dateMetrics = datePaint.fontMetrics
            val dateBaseline = dateBgRect.centerY() - (dateMetrics.bottom + dateMetrics.top) / 2
            canvas.drawText(labelTextX, xCenter, dateBaseline, datePaint)
            
            canvas.restore()

            // --- Draw Amount Label ---
            if (animationProgress > 0.5f) {
                textPaint.alpha = (255 * (animationProgress - 0.5f) * 2).toInt()
                labelBgPaint.alpha = (255 * (animationProgress - 0.5f) * 2).toInt()
                
                val labelTextY = String.format("%s%.0f", symbol, rawAmount)
                val textWidth = textPaint.measureText(labelTextY)
                val bgRect = RectF(
                    xCenter - textWidth / 2 - labelPadding,
                    barTop - 50f - labelPadding, 
                    xCenter + textWidth / 2 + labelPadding,
                    barTop - 50f + 30f + labelPadding 
                )
                canvas.drawRoundRect(bgRect, 20f, 20f, labelBgPaint)
                val fontMetrics = textPaint.fontMetrics
                val baseline = bgRect.centerY() - (fontMetrics.bottom + fontMetrics.top) / 2
                canvas.drawText(labelTextY, xCenter, baseline, textPaint)
                
                textPaint.alpha = 255
                labelBgPaint.alpha = 255
            }
        }
    }

    private var lastDownX = 0f
    private var lastDownY = 0f
    
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (onBarClickListener == null) return super.onTouchEvent(event)

        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                lastDownX = event.x
                lastDownY = event.y
                return true
            }
            android.view.MotionEvent.ACTION_UP -> {
                if (Math.abs(event.x - lastDownX) < 50 && Math.abs(event.y - lastDownY) < 50) {
                    handleTap(event.x)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTap(x: Float) {
        val minWidth = measuredWidth.toFloat()
        val totalContentWidth = (dataPoints.size * (barWidth + barSpacing)) + barSpacing
        val startOffset = if (totalContentWidth < minWidth) {
            (minWidth - totalContentWidth) / 2f
        } else {
            0f
        }

        val relativeX = x - startOffset
        val widthPerItem = barWidth + barSpacing
        
        val index = ((relativeX - (barSpacing / 2)) / widthPerItem).toInt()
        
        if (index in 0 until dataPoints.size) {
            val item = dataPoints[index]
            PreferenceHelper.performHaptics(this, android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            onBarClickListener?.invoke(item.payload)
        }
    }
}
