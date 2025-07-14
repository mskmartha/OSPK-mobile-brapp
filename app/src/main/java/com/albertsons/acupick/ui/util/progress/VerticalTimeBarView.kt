package com.albertsons.acupick.ui.util.progress

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class VerticalTimeBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var timeBar: TimeBar? = null

    private val barWidth = 40f // width in pixels
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 36f
    }
    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val labelOffset = 20f // space between bar and label

    fun setTimeBar(bar: TimeBar) {
        this.timeBar = bar
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        timeBar?.let { bar ->

            val totalDuration = bar.segments.sumOf { it.durationInSeconds }

            val topPadding = 120f
            val baseLineHeight = 10f
            val extraSpaceAboveBase = 40f
            val bottomPadding = baseLineHeight + extraSpaceAboveBase

            val usableHeight = height - topPadding - bottomPadding

            var currentTop = topPadding

            // === Tooltip background with padding ===
            val totalText = bar.totalLabel
            val textWidth = textPaint.measureText(totalText)
            val textHeight = textPaint.fontMetrics.run { bottom - top }
            val tooltipPaddingHorizontal = 30f
            val tooltipPaddingVertical = 20f

            val tooltipRect = RectF(
                (width / 2 - textWidth / 2 - tooltipPaddingHorizontal),
                topPadding - 100f,
                (width / 2 + textWidth / 2 + tooltipPaddingHorizontal),
                topPadding - 40f
            )

            val tooltipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D47A1")
            }
            canvas.drawRoundRect(tooltipRect, 20f, 20f, tooltipPaint)

            // === Draw downward triangle pointer ===
            val trianglePath = Path()
            val triangleWidth = 20f
            val triangleHeight = 15f

            trianglePath.moveTo(width / 2f - triangleWidth / 2, tooltipRect.bottom)
            trianglePath.lineTo(width / 2f + triangleWidth / 2, tooltipRect.bottom)
            trianglePath.lineTo(width / 2f, tooltipRect.bottom + triangleHeight)
            trianglePath.close()

            canvas.drawPath(trianglePath, tooltipPaint)

            // === Draw total label inside tooltip ===
            textPaint.color = Color.WHITE
            canvas.drawText(
                totalText,
                (width / 2 - textWidth / 2),
                tooltipRect.centerY() + textHeight / 4,
                textPaint
            )
            textPaint.color = Color.BLACK

            // === Draw segments ===
            bar.segments.forEach { segment ->
                val heightFraction = segment.durationInSeconds.toFloat() / totalDuration
                val segmentHeight = heightFraction * usableHeight

                rectPaint.color = segment.color
                canvas.drawRoundRect(
                    (width / 2 - barWidth / 2),
                    currentTop,
                    (width / 2 + barWidth / 2),
                    currentTop + segmentHeight,
                    20f, 20f,
                    rectPaint
                )

                val textY = (currentTop + segmentHeight / 2 + textPaint.textSize / 2)
                    .coerceAtMost(height - bottomPadding - baseLineHeight - 10f) // slightly lift

                canvas.drawText(
                    segment.label,
                    (width / 2 + barWidth / 2 + labelOffset),
                    textY,
                    textPaint
                )

                currentTop += segmentHeight
            }

            // === Draw base full-width line ===
            val sideMargin = 0f

            val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
            }

            canvas.drawRect(
                sideMargin,
                currentTop,
                width - sideMargin,
                currentTop + 2f,
                basePaint
            )
        }
    }




}
