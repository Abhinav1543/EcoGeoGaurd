package com.example.ecogeoguard.ui.disaster.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.ecogeoguard.R
import kotlin.math.min

class RiskTrendChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Data
    private var dataPoints = mutableListOf<Float>()
    private var labels = mutableListOf<String>()
    private var maxValue = 100f

    // Colors
    private val gridColor = Color.parseColor("#E0E0E0")
    private val textColor = Color.parseColor("#6C757D")
    private val lineColor = Color.parseColor("#FF6B6B")
    private val fillColor = Color.parseColor("#22FF6B6B")
    private val pointColor = Color.parseColor("#FF6B6B")

    // Paints
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridColor
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pointColor
        style = Paint.Style.FILL
    }

    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Animation
    private var animationProgress = 1f
    private var animator: ValueAnimator? = null

    // Paths
    private val linePath = Path()
    private val fillPath = Path()

    init {
        // Set default data
        setData(
            listOf(30f, 45f, 60f, 75f, 70f, 55f, 40f),
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        )
    }

    fun setData(points: List<Float>, labels: List<String>) {
        this.dataPoints = points.toMutableList()
        this.labels = labels.toMutableList()
        this.maxValue = (points.maxOrNull() ?: 100f) * 1.2f
        invalidate()
    }

    fun animateTo(newPoints: List<Float>, duration: Long = 1000) {
        animator?.cancel()

        val startPoints = dataPoints.toList()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                dataPoints = startPoints.mapIndexed { index, start ->
                    val target = newPoints.getOrElse(index) { start }
                    start + (target - start) * progress
                }.toMutableList()
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 60f

        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding - 40f // Extra space for labels

        // Draw grid lines
        drawGrid(canvas, padding, padding, chartWidth, chartHeight)

        // Draw fill area
        drawFill(canvas, padding, padding, chartWidth, chartHeight)

        // Draw line
        drawLine(canvas, padding, padding, chartWidth, chartHeight)

        // Draw points
        drawPoints(canvas, padding, padding, chartWidth, chartHeight)

        // Draw labels
        drawLabels(canvas, padding, padding, chartWidth, chartHeight)
    }

    private fun drawGrid(canvas: Canvas, left: Float, top: Float, width: Float, height: Float) {
        val horizontalLines = 5
        val verticalLines = dataPoints.size

        // Horizontal grid lines
        for (i in 0..horizontalLines) {
            val y = top + height - (i * height / horizontalLines)
            canvas.drawLine(left, y, left + width, y, gridPaint)

            // Value labels
            val value = (i * maxValue / horizontalLines).toInt()
            textPaint.textSize = 32f
            canvas.drawText("$value%", left - 20, y + 10, textPaint)
        }

        // Vertical grid lines
        for (i in 0 until verticalLines) {
            val x = left + (i * width / (verticalLines - 1))
            canvas.drawLine(x, top, x, top + height, gridPaint)
        }
    }

    private fun drawFill(canvas: Canvas, left: Float, top: Float, width: Float, height: Float) {
        if (dataPoints.size < 2) return

        fillPath.reset()

        // Start at bottom left
        fillPath.moveTo(left, top + height)

        // Draw line to first point
        val firstX = left
        val firstY = top + height - (dataPoints[0] / maxValue * height)
        fillPath.lineTo(firstX, firstY)

        // Draw line through all points
        for (i in 1 until dataPoints.size) {
            val x = left + (i * width / (dataPoints.size - 1))
            val y = top + height - (dataPoints[i] / maxValue * height)
            fillPath.lineTo(x, y)
        }

        // Draw line to bottom right and close
        val lastX = left + width
        fillPath.lineTo(lastX, top + height)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
    }

    private fun drawLine(canvas: Canvas, left: Float, top: Float, width: Float, height: Float) {
        if (dataPoints.size < 2) return

        linePath.reset()

        // Move to first point
        val firstX = left
        val firstY = top + height - (dataPoints[0] / maxValue * height)
        linePath.moveTo(firstX, firstY)

        // Draw line through all points
        for (i in 1 until dataPoints.size) {
            val x = left + (i * width / (dataPoints.size - 1))
            val y = top + height - (dataPoints[i] / maxValue * height)
            linePath.lineTo(x, y)
        }

        canvas.drawPath(linePath, linePaint)
    }

    private fun drawPoints(canvas: Canvas, left: Float, top: Float, width: Float, height: Float) {
        val pointRadius = 8f

        for (i in dataPoints.indices) {
            val x = left + (i * width / (dataPoints.size - 1))
            val y = top + height - (dataPoints[i] / maxValue * height)

            canvas.drawCircle(x, y, pointRadius, pointPaint)
            canvas.drawCircle(x, y, pointRadius, pointStrokePaint)
        }
    }

    private fun drawLabels(canvas: Canvas, left: Float, top: Float, width: Float, height: Float) {
        textPaint.textSize = 32f
        textPaint.color = textColor

        for (i in labels.indices) {
            val x = left + (i * width / (labels.size - 1))
            val y = top + height + 40f

            canvas.drawText(labels[i], x, y, textPaint)
        }
    }

    fun setColors(lineColor: Int, fillColor: Int, pointColor: Int) {
        this.linePaint.color = lineColor
        this.fillPaint.color = fillColor
        this.pointPaint.color = pointColor
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}