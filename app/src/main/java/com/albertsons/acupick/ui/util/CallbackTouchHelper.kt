package com.albertsons.acupick.ui.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import kotlinx.coroutines.launch

// /////////////////////////////////////////////////////////////////////////
// Swipe util for RecyclerView
// /////////////////////////////////////////////////////////////////////////
/**
 * Helper class to handle RecyclerView item swipe.
 *  Drag events could be added later as well.
 *
 * Icons, Text, and background color can be be defined directly via constructor or provided at runtime via
 * listener interface.  This allows for dynamic swipe backdrops and actions that depend on content.
 *
 *
 */
class CallbackTouchHelper(
    private val adapter: RecyclerView.Adapter<*>,
    private val defaultIcon: Drawable?,
    private val defaultText: String?,
    private val defaultBackgroundColor: Int?,
    private val swipeCallback: CallbackSwipeListener?,
    private val shouldCompleteSwipeCallback: () -> Boolean?,
    private val cancelSwipeCallback: () -> Unit,
    private val lastIdCallback: (id: Long) -> Unit,
) : ItemTouchHelper.Callback() {

    // enable swipe
    override fun isLongPressDragEnabled() = false
    override fun isItemViewSwipeEnabled() = true

    // onDraw helpers
    private val background = ColorDrawable().apply {
        color = defaultBackgroundColor ?: 0
    }
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    private val intrinsicWidth = defaultIcon?.intrinsicWidth ?: 0
    private val intrinsicHeight = defaultIcon?.intrinsicHeight ?: 0

    //  Values loaded from context
    private var textSize: Float = 0f
    private var textMargin: Float = 0f
    private var fontSet = false

    // Declared at top level to avoid unneeded GC during draw loop
    private lateinit var itemView: View
    private var isCanceled: Boolean = false

    // Values provided thru callback interface
    private var providedDrawable: Drawable? = null
    private var providedText: String? = null
    private var providedBackgroundColor: Int? = null

    // Limit swipe direction
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) = makeMovementFlags(
        0,
        if (swipeCallback?.canSwipe(adapter.getItemId(viewHolder.adapterPosition)) == true) {
            ItemTouchHelper.START
        } else {
            ItemTouchHelper.ACTION_STATE_IDLE
        }
    )

    // No-op on moves
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false

    //  Relay swipe action to callback
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        if (shouldCompleteSwipeCallback.invoke() == true) {
            swipeCallback?.itemSwiped(adapter.getItemId(viewHolder.adapterPosition), direction)
        } else {
            cancelSwipeCallback.invoke()
            // Get the last item that failed to swipe
            lastIdCallback.invoke(adapter.getItemId(viewHolder.adapterPosition))
            adapter.notifyItemChanged(viewHolder.adapterPosition)
        }
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        val id = adapter.getItemId(viewHolder?.adapterPosition ?: 0)

        // Notify interface of selection change
        swipeCallback?.onSelectedChanged(id)

        // Notify super
        super.onSelectedChanged(viewHolder, actionState)

        viewHolder?.itemView?.context?.let { context ->
            context.lifeCycleScope?.launch {
                // Update dynamic values using interface
                providedBackgroundColor = swipeCallback?.provideBackgroundColor(id)?.let { context.getColor(it) }
                providedText = swipeCallback?.provideText(id)?.getString(context)
                providedDrawable = swipeCallback?.provideIcon(id)

                // Only need to set these using context once
                if (textSize == 0f) textSize = context.resources.getDimension(R.dimen.swipe_text_size)
                if (textMargin == 0f) textMargin = context.resources.getDimension(R.dimen.swipe_text_margin)
                if (!fontSet) {
                    textPaint.typeface = ResourcesCompat.getFont(context, R.font.nunito_sans_bold)
                    fontSet = true
                }
            }
        }
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        itemView = viewHolder.itemView
        isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(canvas, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Set background color dynamically
        background.color = providedBackgroundColor ?: defaultBackgroundColor ?: 0

        // Set bounds to cover distance translated from right.  dx is negative
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )

        // Draw the background
        background.draw(canvas)

        // Draw text on canvas, if provided
        (providedText ?: defaultText)?.let { drawText(canvas, itemView, it, textSize, textMargin, textPaint.measureText(it)) }

        // Draw icon on canvas, if provided
        (providedDrawable ?: defaultIcon)?.let { drawIcon(canvas, itemView, it) }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private fun drawText(canvas: Canvas, itemView: View, text: String, textSize: Float, textMargin: Float, textWidth: Float) {
        textPaint.textSize = textSize

        canvas.drawText(
            text,
            itemView.right.toFloat() - textWidth - textMargin,
            (itemView.top.toFloat() + itemView.bottom.toFloat() + textSize) / 2,
            textPaint
        )
    }

    private fun drawIcon(canvas: Canvas, itemView: View, icon: Drawable) {
        // Calculate position of icon
        val itemHeight = itemView.bottom - itemView.top
        val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
        val iconMargin = (itemHeight - intrinsicHeight) / 2
        val iconLeft = itemView.right - iconMargin - intrinsicWidth
        val iconRight = itemView.right - iconMargin
        val iconBottom = iconTop + intrinsicHeight

        // Draw the delete icon
        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        icon.draw(canvas)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }
}

/**
 * Interface used to connect VM to touch helper
 *
 * [itemSwiped] is called when a swipe has been completed.
 * [canSwipe] returns true for items that can be swiped
 * [onSelectedChanged] is called with id when a swipe begins or 0 when one ends
 * [provideIcon] provides the drawable for a specific item
 * [provideText] provides the text for a specific item
 * [provideBackgroundColor] provides the background color for a specific item
 */
interface CallbackSwipeListener {
    fun itemSwiped(id: Long, direction: Int)
    fun canSwipe(id: Long): Boolean
    fun onSelectedChanged(id: Long)
    fun provideIcon(id: Long): Drawable? = null
    fun provideText(id: Long): StringIdHelper? = null
    fun provideBackgroundColor(id: Long): Int? = null
}
