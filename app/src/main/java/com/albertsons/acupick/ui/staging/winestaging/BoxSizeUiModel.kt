package com.albertsons.acupick.ui.staging.winestaging

import androidx.lifecycle.MutableLiveData
import com.albertsons.acupick.data.model.BoxType

class BoxSizeUiModel(
    val boxSize: BoxType,
    val totalQuantity: MutableLiveData<Int>
) {
    fun totalQuantity() = totalQuantity.value
}
