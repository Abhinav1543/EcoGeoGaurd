package com.example.ecogeoguard.ui.disaster.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.ecogeoguard.R

class SensorHealthChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Data
    private data class SensorData(
        val label: String,
        var value: Float,
        val color: Int
    )

    private var sensors = mutableListOf<SensorData>()
    private var totalValue = 0f

    // Colors
    private val textColor = Color.parseColor("#6C757D")
    private val backgroundColor = Color.parseColor("#F5F5F5")

    // Paints
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 32f
        textAlign = Paint.Align.LEFT
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 36f
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.DEFAULT_BOLD
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }

    private val barBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.FILL
    }

    // Animation
    private var animationProgress = 1f
    private var animator: ValueAnimator? = null

    init {
        // Set default data
        setData(
            listOf(
                Triple("Vibration", 8f, Color.parseColor("#FF6B6B")),
                Triple("Tilt", 12f, Color.parseColor("#FFA726")),
                Triple("Moisture", 15f, Color.parseColor("#42A5F5")),
                Triple("Rainfall", 5f, Color.parseColor("#66BB6A")),
                Triple("Temperature", 10f, Color.parseColor("#AB47BC"))
            )
        )
    }

    fun setData(data: List<Triple<String, Float, Int>>) {
        sensors.clear()
        data.forEach { (label, value, color) ->
            sensors.add(SensorData(label, value, color))
        }
        totalValue = sensors.sumOf { it.value.toDouble() }.toFloat()
        invalidate()
    }

    fun updateValue(index: Int, newValue: Float) {
        if (index in sensors.indices) {
            val oldValue = sensors[index].value

            animator?.cancel()
            animator = ValueAnimator.ofFloat(oldValue, newValue).apply {
                duration = 800
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    sensors[index].value = animation.animatedValue as Float
                    totalValue = sensors.sumOf { it.value.toDouble() }.toFloat()
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (sensors.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 40f
        val barHeight = 60f
        val spacing = 20f

        var currentY = padding

        // Draw title
        textPaint.textSize = 40f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Sensor Health Distribution", padding, currentY, textPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 32f

        currentY += 60f

        // Draw total
        valuePaint.textSize = 36f
        canvas.drawText("Total: ${totalValue.toInt()} sensors", width - padding, currentY, valuePaint)

        currentY += 40f

        // Draw bars for each sensor
        sensors.forEach { sensor ->
            val barWidth = (sensor.value / totalValue) * (width - 2 * padding)

            // Label
            canvas.drawText(sensor.label, padding, currentY + 35f, textPaint)

            // Value
            valuePaint.textSize = 32f
            valuePaint.color = sensor.color
            canvas.drawText("${sensor.value.toInt()}", width - padding, currentY + 35f, valuePaint)

            currentY += 40f

            // Bar background
            canvas.drawRoundRect(
                padding, currentY, width - padding, currentY + barHeight,
                20f, 20f, barBackgroundPaint
            )

            // Colored bar
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sensor.color
                style = Paint.Style.FILL
            }

            canvas.drawRoundRect(
                padding, currentY, padding + barWidth, currentY + barHeight,
                20f, 20f, barPaint
            )

            // Percentage
            val percentage = (sensor.value / totalValue * 100).toInt()
            textPaint.color = sensor.color
            textPaint.textSize = 28f
            canvas.drawText("$percentage%", padding + barWidth + 10, currentY + 35f, textPaint)

            currentY += barHeight + spacing + 20f
        }
    }

    fun setBarColors(vararg colors: Int) {
        colors.forEachIndexed { index, color ->
            if (index < sensors.size) {
                sensors[index] = sensors[index].copy(color = color)
            }
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}