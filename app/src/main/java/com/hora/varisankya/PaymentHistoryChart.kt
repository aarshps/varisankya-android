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
import kotlin.math.max

class PaymentHistoryChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

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

    private val dataPoints = mutableListOf<Pair<Date, Double>>()

    // Dimensions
    private val barWidth = 100f // "Fat" bars
    private val barSpacing = 80f // Spacing between bars
    private val chartPaddingTop = 100f // Increased to fit labels
    private val chartPaddingBottom = 100f // Increased for chip dates
    private val labelPadding = 16f
    private val cornerRadius = 50f // Fully rounded top/bottom

    fun setPaymentData(payments: List<PaymentRecord>) {
        dataPoints.clear()
        dataPoints.addAll(payments.sortedBy { it.date }.map { (it.date ?: Date()) to (it.amount ?: 0.0) })
        requestLayout() // Trigger onMeasure to resize view width
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        // Calculate dynamic width based on data points
        val minWidth = MeasureSpec.getSize(widthMeasureSpec)
        val calculatedWidth = if (dataPoints.isNotEmpty()) {
            (dataPoints.size * (barWidth + barSpacing) + barSpacing).toInt()
        } else {
            minWidth
        }

        // Use calculated width if it exceeds screen width, otherwise default (match parent equivalent)
        // Ensure proper scrolling by setting measured dimension
        setMeasuredDimension(max(minWidth, calculatedWidth), heightSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        
        val availableHeight = height - chartPaddingTop - chartPaddingBottom

        // Resolve Colors
        val colorPrimary = MaterialColors.getColor(context, android.R.attr.colorPrimary, Color.BLUE)
        val colorOnSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
        
        // M3 Chip Colors: Secondary Container
        val colorSecondaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, Color.LTGRAY)
        val colorOnSecondaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.BLACK)
        
        barPaint.color = colorPrimary
        
        // Date Text (Recycled M3 colors for consistency)
        datePaint.color = colorOnSecondaryContainer
        datePaint.alpha = 255
        
        // Label Background (Chip)
        labelBgPaint.color = colorSecondaryContainer
        labelBgPaint.alpha = 255 // Opaque for solid M3 look
        
        // Label Text
        textPaint.color = colorOnSecondaryContainer

        // Scaling
        val maxAmount = dataPoints.maxOf { it.second }.toFloat()
        val rangeY = max(maxAmount, 100f) // Avoid div by zero, min range 100

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        dataPoints.forEachIndexed { index, pair ->
            val date = pair.first
            val amount = pair.second.toFloat()

            // Calculate Position
            // Start with some left padding
            val xCenter = barSpacing + (index * (barWidth + barSpacing)) + (barWidth / 2)
            
            // Height logic
            val barHeight = (amount / rangeY) * availableHeight
            // Ensure visualization even for small amounts
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

            // --- Draw Date Label (X-Axis) with Chip ---
            val dateText = dateFormat.format(date)
            val dateTextWidth = datePaint.measureText(dateText)
            val dateBgRect = RectF(
                xCenter - dateTextWidth / 2 - labelPadding,
                height - 60f - labelPadding, // Positioned near bottom
                xCenter + dateTextWidth / 2 + labelPadding,
                height - 60f + 30f + labelPadding
            )
            canvas.drawRoundRect(dateBgRect, 20f, 20f, labelBgPaint)
            // Center date text
            val dateMetrics = datePaint.fontMetrics
            val dateBaseline = dateBgRect.centerY() - (dateMetrics.bottom + dateMetrics.top) / 2
            canvas.drawText(dateText, xCenter, dateBaseline, datePaint)


            // --- Draw Amount Label (Top) with Chip ---
            val labelText = String.format("%.0f", amount)
            val textWidth = textPaint.measureText(labelText)
            val bgRect = RectF(
                xCenter - textWidth / 2 - labelPadding,
                barTop - 50f - labelPadding, // Floating above bar
                xCenter + textWidth / 2 + labelPadding,
                barTop - 50f + 30f + labelPadding // Approximate font height
            )

            // Draw Label Background
            canvas.drawRoundRect(bgRect, 20f, 20f, labelBgPaint)
            
            // Draw Label Text
            // Centered vertically in rect
            val fontMetrics = textPaint.fontMetrics
            val baseline = bgRect.centerY() - (fontMetrics.bottom + fontMetrics.top) / 2
            canvas.drawText(labelText, xCenter, baseline, textPaint)
        }
    }
}
