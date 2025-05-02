package com.albertsons.acupick.ui.util

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.jay.widget.StickyHeaders
import com.jay.widget.StickyHeadersLinearLayoutManager
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.GroupieViewHolder

// /////////////////////////////////////////////////////////////////////////
// Helper classes for tracker and diff utils with Groupie
// /////////////////////////////////////////////////////////////////////////
class GroupieSelectionLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {
    override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? =
        recyclerView.findChildViewUnder(e.x, e.y)?.let { childView ->
            (recyclerView.getChildViewHolder(childView) as? GroupieViewHolder)?.getItemDetails()
        }
}

//  This extension allows easy creation of ItemDetailsLookup from GroupieViewHolder
fun GroupieViewHolder.getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
    object : ItemDetailsLookup.ItemDetails<Long>() {

        override fun getSelectionKey() = item.id
        override fun getPosition() = adapterPosition
    }

// Convenience property
val RecyclerView.groupieAdapter get() = adapter as? GroupieAdapter

// GroupieItems can also implement this interface, and return true, if they want to be sticky
interface StickyItem {
    val isSticky: Boolean
}

//  Simple adapter to wrap GroupieAdapter with StickyHeader interface
class StickyGroupieAdapter : GroupieAdapter(), StickyHeaders {
    override fun isStickyHeader(position: Int) = (getItem(position) as? StickyItem)?.isSticky ?: false

    // /////////////////////////////////////////////////////////////////////////
    // Workaround for Sticky Header issue with diff utils:
    //   https://github.com/ShamylZakariya/StickyHeaders/issues/26
    // /////////////////////////////////////////////////////////////////////////
    private var stickyHeadersObserver: RecyclerView.AdapterDataObserver? = null

    // FIXME - Might use this code when fixing bug where item view becomes sticky
    //   /////////////////////////////////////////////////////////////////////////

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        if (observer::class.java.enclosingClass == StickyHeadersLinearLayoutManager::class.java) {
            stickyHeadersObserver = observer
        } else {
            super.registerAdapterDataObserver(observer)
        }
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        if (observer == stickyHeadersObserver) {
            stickyHeadersObserver = null
        } else {
            super.unregisterAdapterDataObserver(observer)
        }
    }

    // override fun onItemMoved(group: Group, fromPosition: Int, toPosition: Int) {
    //     super.onItemMoved(group, fromPosition, toPosition)
    //     // stickyHeadersObserver?.onItemRangeMoved(fromPosition, toPosition, 1)
    //     stickyHeadersObserver?.onChanged()
    //
    // }
    //
    //
    // override fun onItemRemoved(group: Group, position: Int) {
    //     super.onItemRemoved(group, position)
    //     // stickyHeadersObserver?.onItemRangeRemoved(position, 1)
    //     stickyHeadersObserver?.onChanged()
    //
    // }
    //
    // override fun onItemChanged(group: Group, position: Int) {
    //     super.onItemChanged(group, position)
    //     // stickyHeadersObserver?.onItemRangeChanged(position, 1)
    //     stickyHeadersObserver?.onChanged()
    //
    // }
    //
    // override fun onItemChanged(group: Group, position: Int, payload: Any?) {
    //     super.onItemChanged(group, position, payload)
    //     // stickyHeadersObserver?.onItemRangeChanged(position, 1, payload)
    //     stickyHeadersObserver?.onChanged()
    //
    // }
    //
    // override fun onItemRangeChanged(group: Group, positionStart: Int, itemCount: Int, payload: Any?) {
    //     super.onItemRangeChanged(group, positionStart, itemCount, payload)
    //     // stickyHeadersObserver?.onItemRangeChanged(positionStart, itemCount, payload)
    //     stickyHeadersObserver?.onChanged()
    //
    // }
    //
    // override fun onItemRangeChanged(group: Group, positionStart: Int, itemCount: Int) {
    //     super.onItemRangeChanged(group, positionStart, itemCount)
    //     // stickyHeadersObserver?.onItemRangeChanged(positionStart, itemCount)
    //     stickyHeadersObserver?.onChanged()
    //
    // }
    //
    // override fun onItemRangeInserted(group: Group, positionStart: Int, itemCount: Int) {
    //     super.onItemRangeInserted(group, positionStart, itemCount)
    //     // stickyHeadersObserver?.onItemRangeInserted(positionStart, itemCount)
    //     stickyHeadersObserver?.onChanged()
    //
    // }
    //
    // override fun onItemRangeRemoved(group: Group, positionStart: Int, itemCount: Int) {
    //     super.onItemRangeRemoved(group, positionStart, itemCount)
    //     // stickyHeadersObserver?.onItemRangeRemoved(positionStart, itemCount)
    //     stickyHeadersObserver?.onChanged()
    //
    // }
    //
    // override fun onItemInserted(group: Group, position: Int) {
    //     super.onItemInserted(group, position)
    //     // stickyHeadersObserver?.onItemRangeInserted(position, 1)
    //     stickyHeadersObserver?.onChanged()
    //
    // }

    override fun onChanged(group: Group) {
        super.onChanged(group)
        stickyHeadersObserver?.onChanged()
    }
}
