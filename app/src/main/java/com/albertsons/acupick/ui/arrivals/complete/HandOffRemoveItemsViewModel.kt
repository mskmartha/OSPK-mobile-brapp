package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HandOffRemoveItemsViewModel(
    val app: Application,
) : BaseViewModel(app) {

    private val _items = MutableStateFlow<List<RestrictedItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _isRxOrder = MutableStateFlow(false)
    val isRxOrder = _isRxOrder.asStateFlow()

    private val _removeItemsConfirmedEvent = MutableSharedFlow<Unit>()
    val removeItemsConfirmedEvent = _removeItemsConfirmedEvent.asSharedFlow()

    private val _removeItemsCancelledEvent = MutableSharedFlow<Unit>()
    val removeItemsCancelledEvent = _removeItemsCancelledEvent.asSharedFlow()

    fun setItems(isRx: Boolean, items: List<RestrictedItem>) {
        _items.value = items
        _isRxOrder.value = isRx
    }

    fun onRestrictedItemCheckedChange(item: RestrictedItem, isChecked: Boolean) {
        val updatedItems = _items.value.map {
            if (it == item) it.copy(isChecked = isChecked) else it
        }
        _items.value = updatedItems
    }

    fun areAllItemsChecked(): Boolean {
        return _items.value.all { it.isChecked } ?: false
    }

    fun onRemoveItemsConfirmed() {
        if (areAllItemsChecked()) {
            viewModelScope.launch {
                _removeItemsConfirmedEvent.emit(Unit)
            }
        }
    }

    fun onRemoveItemsCancelled() {
        viewModelScope.launch {
            _removeItemsCancelledEvent.emit(Unit)
        }
    }
}

fun <T> List<T>.duplicateItems(): List<T> {
    return this + this
}

data class RestrictedItem(val imageUrl: String? = null, val description: String, val qty: Int? = null, val isChecked: Boolean)

fun HandOffRegulatedItem.toRestrictedItem() = RestrictedItem(imageUrl, description, totalQty.toInt(), isChecked = false)
