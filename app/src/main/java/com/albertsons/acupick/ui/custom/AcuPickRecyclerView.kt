package com.albertsons.acupick.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import androidx.annotation.Nullable
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R

class AcuPickRecyclerView : RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        @Nullable attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    // By default, only show both scroll indicators
    private var showTopScrollIndicator = true
    private var showBottomScrollIndicator = true

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)

        val rect = Rect().also {
            it.left = scrollX
            it.right = scrollX + right - left
            it.top = scrollY
            it.bottom = scrollY + bottom - top
        }

        if (showTopScrollIndicator) {
            val indicatorDrawable = AppCompatResources.getDrawable(context, R.drawable.scroll_indicator_top)
            indicatorDrawable?.let { drawable ->
                val h = indicatorDrawable.intrinsicHeight
                val canScrollUp = canScrollVertically(-1)
                if (canScrollUp) {
                    drawable.setBounds(rect.left, rect.top, rect.right, rect.top + h)
                    canvas?.let {
                        drawable.draw(it)
                    }
                }
            }
        }

        if (showBottomScrollIndicator) {
            val indicatorDrawable = AppCompatResources.getDrawable(context, R.drawable.scroll_indicator_bottom)
            indicatorDrawable?.let { drawable ->
                val h = indicatorDrawable.intrinsicHeight
                val canScrollDown = canScrollVertically(1)
                if (canScrollDown) {
                    drawable.setBounds(rect.left, rect.bottom - h, rect.right, rect.bottom)
                    canvas?.let {
                        drawable.draw(it)
                    }
                }
            }
        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.AcuPickRecyclerView, 0, 0).apply {
            setShowScrollIndicator(getInteger(R.styleable.AcuPickRecyclerView_showScrollIndicator, 0))
        }
    }

    private fun setShowScrollIndicator(which: Int) {
        showTopScrollIndicator = which == 1 || which == 2
        showBottomScrollIndicator = which == 0 || which == 2
    }
}
