package com.example.ecogeoguard.ui.government.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.min

class TrendChart @JvmOverloads constructor(
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
    private val positiveTrendColor = Color.parseColor("#4CAF50")
    private val negativeTrendColor = Color.parseColor("#F44336")

    // Paints
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridColor
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
    private var trendType: TrendType = TrendType.POSITIVE

    enum class TrendType {
        POSITIVE, NEGATIVE, STABLE
    }

    // Paths
    private val linePath = Path()
    private val fillPath = Path()

    init {
        // Default data
        setData(
            listOf(30f, 45f, 60f, 75f, 70f, 55f, 40f),
            listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul")
        )
    }

    fun setData(points: List<Float>, labels: List<String>) {
        this.dataPoints = points.toMutableList()
        this.labels = labels.toMutableList()
        this.maxValue = (points.maxOrNull() ?: 100f) * 1.2f

        // Determine trend
        val first = points.firstOrNull() ?: 0f
        val last = points.lastOrNull() ?: 0f
        trendType = when {
            last > first + 5 -> TrendType.POSITIVE
            last < first - 5 -> TrendType.NEGATIVE
            else -> TrendType.STABLE
        }

        updateLineColor()
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

                // Update trend
                val first = dataPoints.firstOrNull() ?: 0f
                val last = dataPoints.lastOrNull() ?: 0f
                trendType = when {
                    last > first + 5 -> TrendType.POSITIVE
                    last < first - 5 -> TrendType.NEGATIVE
                    else -> TrendType.STABLE
                }
                updateLineColor()
                invalidate()
            }
            start()
        }
    }

    private fun updateLineColor() {
        linePaint.color = when (trendType) {
            TrendType.POSITIVE -> positiveTrendColor
            TrendType.NEGATIVE -> negativeTrendColor
            TrendType.STABLE -> textColor
        }
        fillPaint.color = when (trendType) {
            TrendType.POSITIVE -> Color.parseColor("#224CAF50")
            TrendType.NEGATIVE -> Color.parseColor("#22F44336")
            TrendType.STABLE -> Color.parseColor("#226C757D")
        }
        pointPaint.color = linePaint.color
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 60f

        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding - 60f

        // Draw trend indicator
        drawTrendIndicator(canvas, padding)

        // Draw grid lines
        drawGrid(canvas, padding, padding + 80f, chartWidth, chartHeight)

        // Draw fill area
        drawFill(canvas, padding, padding + 80f, chartWidth, chartHeight)

        // Draw line
        drawLine(canvas, padding, padding + 80f, chartWidth, chartHeight)

        // Draw points
        drawPoints(canvas, padding, padding + 80f, chartWidth, chartHeight)

        // Draw labels
        drawLabels(canvas, padding, padding + 80f, chartWidth, chartHeight)

        // Draw trend line
        drawTrendLine(canvas, padding, padding + 80f, chartWidth, chartHeight)
    }

    private fun drawTrendIndicator(canvas: Canvas, left: Float) {
        val trendText = when (trendType) {
            TrendType.POSITIVE -> "▲ Upward Trend"
            TrendType.NEGATIVE -> "▼ Downward Trend"
            TrendType.STABLE -> "→ Stable Trend"
        }

        val trendColor = when (trendType) {
            TrendType.POSITIVE -> positiveTrendColor
            TrendType.NEGATIVE -> negativeTrendColor
            TrendType.STABLE -> textColor
        }

        textPaint.textSize = 28f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = trendColor
        canvas.drawText(trendText, left, 50f, textPaint)
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
            textPaint.textSize = 28f
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = textColor
            canvas.drawText("$value%", left - 10, y + 8, textPaint)
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
        val pointRadius = 6f

        for (i in dataPoints.indices) {
            val x = left + (i * width / (dataPoints.size - 1))
            val y = top + height - (dataPoints[i] / maxValue * height)

            canvas.drawCircle(x, y, pointRadius, pointPaint)
            canvas.drawCircle(x, y, pointRadius, pointStrokePaint)
        }
    }

    private fun drawLabels(canvas: Canvas, left: Float, top: Float, width: Float, height: Float) {
        textPaint.textSize = 28f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = textColor

        for (i in labels.indices) {
            val x = left + (i * width / (labels.size - 1))
            val y = top + height + 40f

            canvas.drawText(labels[i], x, y, textPaint)
        }
    }

    private fun drawTrendLine(canvas: Canvas, left: Float, top: Float, width: Float, height: Float) {
        if (dataPoints.size < 2) return

        // Calculate linear regression
        val n = dataPoints.size
        val indices = (0 until n).map { it.toFloat() }
        val xMean = indices.average().toFloat()
        val yMean = dataPoints.average().toFloat()

        var numerator = 0f
        var denominator = 0f
        for (i in 0 until n) {
            val x = i.toFloat()
            val y = dataPoints[i]
            numerator += (x - xMean) * (y - yMean)
            denominator += (x - xMean) * (x - xMean)
        }

        val slope = if (denominator != 0f) numerator / denominator else 0f
        val intercept = yMean - slope * xMean

        // Calculate trend line points
        val startX = left
        val startY = top + height - ((intercept + slope * 0f) / maxValue * height)
        val endX = left + width
        val endY = top + height - ((intercept + slope * (n - 1).toFloat()) / maxValue * height)

        val trendLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = linePaint.color
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        canvas.drawLine(startX, startY, endX, endY, trendLinePaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}