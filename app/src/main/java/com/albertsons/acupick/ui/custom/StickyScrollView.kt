package com.albertsons.acupick.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import com.albertsons.acupick.R
import java.util.ArrayList

class StickyScrollView : ScrollView {
    constructor(context: Context) : super(context)
    constructor(context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }
    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs, defStyle)
    }

    private val stickyViews: ArrayList<View> = ArrayList()
    private var currentStickingView: View? = null
    private var stickyViewTopOffset = 0f
    private var stickyViewLeftOffset = 0
    private var redirectTouchesToStickyView = false
    private var clippingToPadding = false
    private var clipToPaddingHasBeenSet = false
    private var shadowHeight: Int = 0
    private var shadowDrawable: Drawable? = null
    private val invalidateRunnable: Runnable = object : Runnable {
        override fun run() {
            if (currentStickingView != null) {
                invalidate()
            }
            postDelayed(this, 16)
        }
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyle: Int = android.R.attr.scrollViewStyle) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.StickyScrollView, defStyle, 0)
        val density = context.resources.displayMetrics.density
        val defaultShadowHeightInPix = (DEFAULT_SHADOW_HEIGHT * density + 0.5f).toInt()
        shadowHeight = attributes.getDimensionPixelSize(R.styleable.StickyScrollView_stuckShadowHeight, defaultShadowHeightInPix)
        val shadowDrawableRes = attributes.getResourceId(R.styleable.StickyScrollView_stuckShadowDrawable, -1)
        if (shadowDrawableRes != -1) {
            shadowDrawable = ContextCompat.getDrawable(context, shadowDrawableRes)
        }
        attributes.recycle()
    }

    private fun getLeftViews(view: View?): Int {
        var v = view
        var left = v!!.left
        while (v!!.parent !== getChildAt(0)) {
            v = v!!.parent as View
            left += v.left
        }
        return left
    }

    private fun getTopViews(view: View?): Int {
        var v = view
        var top = v!!.top
        while (v!!.parent !== getChildAt(0)) {
            v = v!!.parent as View
            top += v.top
        }
        return top
    }

    private fun getRightViews(view: View?): Int {
        var v = view
        var right = v!!.right
        while (v!!.parent !== getChildAt(0)) {
            v = v!!.parent as View
            right += v.right
        }
        return right
    }

    private fun getBottomViews(view: View): Int {
        var v = view
        var bottom = v.bottom
        while (v.parent !== getChildAt(0)) {
            v = v.parent as View
            bottom += v.bottom
        }
        return bottom
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (!clipToPaddingHasBeenSet) {
            clippingToPadding = true
        }
        notifyHierarchyChanged()
    }

    override fun setClipToPadding(clipToPadding: Boolean) {
        super.setClipToPadding(clipToPadding)
        clippingToPadding = clipToPadding
        clipToPaddingHasBeenSet = true
    }

    override fun addView(child: View) {
        super.addView(child)
        findStickyViews(child)
    }

    override fun addView(child: View, index: Int) {
        super.addView(child, index)
        findStickyViews(child)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.addView(child, index, params)
        findStickyViews(child)
    }

    override fun addView(child: View, width: Int, height: Int) {
        super.addView(child, width, height)
        findStickyViews(child)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        super.addView(child, params)
        findStickyViews(child)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (currentStickingView != null) {
            canvas.save()
            canvas.translate((paddingLeft + stickyViewLeftOffset).toFloat(), scrollY + stickyViewTopOffset + if (clippingToPadding) paddingTop else 0)
            canvas.clipRect(
                0f, if (clippingToPadding) -stickyViewTopOffset else 0f,
                (
                    width - stickyViewLeftOffset
                    ).toFloat(),
                (
                    currentStickingView!!.height + shadowHeight + 1
                    ).toFloat()
            )
            if (shadowDrawable != null) {
                val left = 0
                val right = currentStickingView!!.width
                val top = currentStickingView!!.height
                val bottom = currentStickingView!!.height + shadowHeight
                shadowDrawable!!.setBounds(left, top, right, bottom)
                shadowDrawable!!.draw(canvas)
            }
            canvas.clipRect(0f, if (clippingToPadding) -stickyViewTopOffset else 0f, width.toFloat(), currentStickingView!!.height.toFloat())
            if (getViewTag(currentStickingView).contains(FLAG_HAS_TRANSPARENCY)) {
                showView(currentStickingView)
                currentStickingView!!.draw(canvas)
                hideView(currentStickingView)
            } else {
                currentStickingView!!.draw(canvas)
            }
            canvas.restore()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            redirectTouchesToStickyView = true
        }
        if (redirectTouchesToStickyView) {
            redirectTouchesToStickyView = currentStickingView != null
            if (redirectTouchesToStickyView) {
                redirectTouchesToStickyView =
                    ev.y <= currentStickingView!!.height + stickyViewTopOffset && ev.x >= getLeftViews(currentStickingView) && ev.x <= getRightViews(
                    currentStickingView
                )
            }
        } else if (currentStickingView == null) {
            redirectTouchesToStickyView = false
        }
        if (redirectTouchesToStickyView) {
            ev.offsetLocation(0f, -1 * (scrollY + stickyViewTopOffset - getTopViews(currentStickingView)))
        }
        return super.dispatchTouchEvent(ev)
    }

    private var hasNotDoneActionDown = true

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (redirectTouchesToStickyView) {
            ev.offsetLocation(0f, scrollY + stickyViewTopOffset - getTopViews(currentStickingView))
        }
        if (ev.action == MotionEvent.ACTION_DOWN) {
            hasNotDoneActionDown = false
        }
        if (hasNotDoneActionDown) {
            val down = MotionEvent.obtain(ev)
            down.action = MotionEvent.ACTION_DOWN
            super.onTouchEvent(down)
            hasNotDoneActionDown = false
        }
        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            hasNotDoneActionDown = true
        }
        return super.onTouchEvent(ev)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        stickTheViews()
    }

    private fun stickTheViews() {
        var viewThatShouldStick: View? = null
        var approachingView: View? = null
        for (view in stickyViews) {
            val viewTop = getTopViews(view) - scrollY + if (clippingToPadding) 0 else paddingTop
            if (viewTop <= 0) {
                if (viewThatShouldStick == null || viewTop > getTopViews(viewThatShouldStick) - scrollY + if (clippingToPadding) 0 else paddingTop) {
                    viewThatShouldStick = view
                }
            } else {
                if (approachingView == null || viewTop < getTopViews(approachingView) - scrollY + if (clippingToPadding) 0 else paddingTop) {
                    approachingView = view
                }
            }
        }
        if (viewThatShouldStick != null) {
            stickyViewTopOffset =
                if (approachingView == null) 0f else 0.coerceAtMost(getTopViews(approachingView) - scrollY + (if (clippingToPadding) 0 else paddingTop) - viewThatShouldStick.height)
                    .toFloat()
            if (viewThatShouldStick !== currentStickingView) {
                if (currentStickingView != null) {
                    stopStickingView()
                }
                // only compute the left offset when we start sticking.
                stickyViewLeftOffset = getLeftViews(viewThatShouldStick)
                startStickingView(viewThatShouldStick)
            }
        } else if (currentStickingView != null) {
            stopStickingView()
        }
    }

    private fun startStickingView(viewThatShouldStick: View) {
        currentStickingView = viewThatShouldStick
        if (getViewTag(currentStickingView).contains(FLAG_NON_CONSTANT)) {
            hideView(currentStickingView)
        }
        if ((currentStickingView!!.tag as String).contains(FLAG_NON_CONSTANT)) {
            post(invalidateRunnable)
        }
    }

    private fun stopStickingView() {
        if (getViewTag(currentStickingView).contains(FLAG_HAS_TRANSPARENCY)) {
            showView(currentStickingView)
        }
        currentStickingView = null
        removeCallbacks(invalidateRunnable)
    }

    private fun notifyHierarchyChanged() {
        if (currentStickingView != null) {
            stopStickingView()
        }
        stickyViews.clear()
        findStickyViews(getChildAt(0))
        stickTheViews()
        invalidate()
    }

    private fun findStickyViews(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val tag = getViewTag(view.getChildAt(i))
                if (tag.contains(STICKY_TAG)) {
                    stickyViews.add(view.getChildAt(i))
                } else if (view.getChildAt(i) is ViewGroup) {
                    findStickyViews(view.getChildAt(i))
                }
            }
        } else {
            val tag = view.tag as String
            if (tag.contains(STICKY_TAG)) {
                stickyViews.add(view)
            }
        }
    }

    private fun getViewTag(view: View?): String {
        return view?.tag.toString()
    }

    private fun hideView(view: View?) {
        view?.alpha = 0f
    }

    private fun showView(view: View?) {
        view?.alpha = 1f
    }

    companion object {

        // tag for stuck views
        const val STICKY_TAG = "sticky"

        // tag for non constant drawing aka progress bars/buttons
        const val FLAG_NON_CONSTANT = "nonConstant"

        // transparent views
        const val FLAG_HAS_TRANSPARENCY = "hasTransparency"

        private const val DEFAULT_SHADOW_HEIGHT = 10 // dp;
    }
}
