package com.albertsons.acupick.ui.itemphoto

import android.app.Application
import androidx.lifecycle.LiveData
import com.albertsons.acupick.ui.BaseViewModel
import com.hadilq.liveevent.LiveEvent

class ItemPhotoViewModel(private val app: Application) : BaseViewModel(app) {
    var imageUrl = ""

    private val _dismissEvent = LiveEvent<Unit>()
    val dismissEvent: LiveData<Unit> = _dismissEvent

    fun onClose() = _dismissEvent.postValue(Unit)
}
