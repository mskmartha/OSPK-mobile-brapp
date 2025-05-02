package com.albertsons.acupick.ui.missingItemLocation

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.ui.BaseViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MissingItemLocationViewModel(app: Application) : BaseViewModel(app) {

    private val locationComment = MutableStateFlow("")
    val isErrorShown = MutableStateFlow(false)
    val addLocationEvent = MutableSharedFlow<MissingItemLocationResultParams>()
    val whereToFindLocationEvent = MutableSharedFlow<Unit>()
    val notNowEvent = MutableSharedFlow<Unit>()
    val isLoading = MutableStateFlow(false)

    val aisle = MutableStateFlow("")
    val section = MutableStateFlow("")
    val shelf = MutableStateFlow("")
    private val locationCode = combine(aisle, section, shelf) { aisle, section, shelf ->
        "$aisle$section$shelf"
    }
    val isValidInput = locationCode.combine(locationComment, this::isValidData)

    private fun isValidData(
        locationCode: String?,
        locationComment: String?,
    ) = locationCode?.length == 9 || (locationCode.isNullOrEmpty() && locationComment.isNotNullOrBlank())

    fun onAisleChanged(aisle: String) {
        this.aisle.value = aisle
        if (isErrorShown.value) isErrorShown.value = false
    }

    fun onSectionChanged(section: String) {
        this.section.value = section
        if (isErrorShown.value) isErrorShown.value = false
    }

    fun onShelfChanged(shelf: String) {
        this.shelf.value = shelf
        if (isErrorShown.value) isErrorShown.value = false
    }

    fun onClickAddLocation() {
        viewModelScope.launch {
            val isValid = isValidData(locationCode.first(), locationComment.value).also { isErrorShown.emit(!it) }
            if (isValid) addLocationEvent.emit(MissingItemLocationResultParams(aisle.value, section.value, shelf.value, locationComment.value))
        }
    }

    fun onCommentChanged(comment: String) {
        viewModelScope.launch {
            locationComment.emit(comment)
        }
    }

    fun onClickWhereToFindLocationCode() = viewModelScope.launch {
        whereToFindLocationEvent.emit(Unit)
    }

    fun onClickNotNow() {
        viewModelScope.launch {
            notNowEvent.emit(Unit)
        }
    }

    companion object {
        const val MISSING_ITEM_LOCATION_REQUEST_KEY = "missing_item_location_request_key"
        const val MISSING_ITEM_LOCATION_NOT_NOW_REQUEST_KEY = "missing_item_location_not_now_request_key"
        const val MISSING_ITEM_LOCATION_RESULTS = "missing_item_location_results"
        const val WHERE_TO_FIND_LOCATION_CODE_REQUEST_KEY = "where_to_find_location_code"
        const val DATA_PICKSLIST_SCANNED = "data_picklist_scanned"
    }
}

@Parcelize
data class MissingItemLocationResultParams(
    val aisleLocation: String? = null,
    val section: String? = null,
    val shelf: String? = null,
    val locationComment: String? = null,
) : Parcelable
