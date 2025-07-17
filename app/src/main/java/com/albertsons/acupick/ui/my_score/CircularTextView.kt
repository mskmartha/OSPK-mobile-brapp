package com.albertsons.acupick.ui.my_score

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView

class CircularTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bgColor: Int = getRandomColor()

    init {
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas) {
        val radius = width.coerceAtMost(height) / 2f
        paint.color = bgColor
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
        super.onDraw(canvas)
    }

    fun setInitial(name: String) {
        text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
        bgColor = getRandomColor()
        invalidate()
    }

    private fun getRandomColor(): Int {
        val colors = listOf(
            Color.parseColor("#FF6F61"),
            Color.parseColor("#6B5B95"),
            Color.parseColor("#88B04B"),
            Color.parseColor("#F7CAC9"),
            Color.parseColor("#92A8D1"),
            Color.parseColor("#955251")
        )
        return colors.random()
    }
}
