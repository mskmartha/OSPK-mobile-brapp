package com.albertsons.acupick.ui.chatImagePreview

import android.app.Application
import androidx.lifecycle.LiveData
import com.albertsons.acupick.ui.BaseViewModel
import com.hadilq.liveevent.LiveEvent

class ChatImagePreviewViewModel(private val app: Application) : BaseViewModel(app) {
    var imageUrl = ""

    private val _dismissEvent = LiveEvent<Unit>()
    val dismissEvent: LiveData<Unit> = _dismissEvent

    fun onClose() = _dismissEvent.postValue(Unit)
}
