package com.example.ecogeoguard.ui.government.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.ecogeoguard.R

class DistrictOverviewChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Data
    private data class DistrictData(
        val name: String,
        var value: Float,
        val color: Int
    )

    private var data = mutableListOf<DistrictData>()
    private var totalValue = 0f

    // Animation
    private var animationProgress = 0f
    private var animator: ValueAnimator? = null

    // Colors
    private val textColor = Color.parseColor("#6C757D")
    private val gridColor = Color.parseColor("#E0E0E0")

    // Paints
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        textAlign = Paint.Align.LEFT
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.DEFAULT_BOLD
    }

    private val barBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.FILL
    }

    init {
        // Default data
        setData(
            listOf(
                Triple("Agriculture", 65f, Color.parseColor("#4CAF50")),
                Triple("Livestock", 72f, Color.parseColor("#2196F3")),
                Triple("Disaster", 58f, Color.parseColor("#FF9800")),
                Triple("Infrastructure", 45f, Color.parseColor("#9C27B0")),
                Triple("Healthcare", 82f, Color.parseColor("#00BCD4"))
            )
        )
    }

    fun setData(newData: List<Triple<String, Float, Int>>) {
        data.clear()
        newData.forEach { (name, value, color) ->
            data.add(DistrictData(name, value, color))
        }
        totalValue = data.sumOf { it.value.toDouble() }.toFloat()
        invalidate()
    }

    fun animateTo(newData: List<Triple<String, Float, Int>>, duration: Long = 1000) {
        animator?.cancel()

        val startValues = data.map { it.value }
        val targetValues = newData.map { it.second}

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                data.forEachIndexed { index, district ->
                    district.value = startValues[index] + (targetValues[index] - startValues[index]) * progress
                }
                totalValue = data.sumOf { it.value.toDouble() }.toFloat()
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 80f

        var currentY = padding

        // Draw title
        textPaint.textSize = 40f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.color = Color.parseColor("#212529")
        canvas.drawText("District Performance Overview", padding, currentY, textPaint)

        currentY += 60f

        // Draw grid line
        val gridPaint = Paint().apply {
            color = gridColor
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(padding, currentY, width - padding, currentY, gridPaint)

        currentY += 20f

        // Draw bars for each metric
        data.forEach { metric ->
            val barWidth = (metric.value / 100f) * (width - 2 * padding)

            // Metric name
            textPaint.textSize = 28f
            textPaint.typeface = Typeface.DEFAULT
            textPaint.color = textColor
            canvas.drawText(metric.name, padding, currentY + 30f, textPaint)

            // Value
            valuePaint.color = metric.color
            canvas.drawText("${metric.value.toInt()}%", width - padding, currentY + 30f, valuePaint)

            currentY += 40f

            // Bar background
            canvas.drawRoundRect(
                padding, currentY, width - padding, currentY + 32f,
                16f, 16f, barBackgroundPaint
            )

            // Colored bar
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = metric.color
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                padding, currentY, padding + barWidth, currentY + 32f,
                16f, 16f, barPaint
            )

            currentY += 48f
        }

        // Draw average line
        val avgValue = totalValue / data.size
        val avgX = padding + (avgValue / 100f) * (width - 2 * padding)
        val avgPaint = Paint().apply {
            color = Color.parseColor("#FF6B6B")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
        canvas.drawLine(avgX, 120f, avgX, currentY - 20f, avgPaint)

        // Average label
        textPaint.textSize = 24f
        textPaint.color = Color.parseColor("#FF6B6B")
        canvas.drawText("District Average: ${"%.1f".format(avgValue)}%", avgX + 10f, 110f, textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}