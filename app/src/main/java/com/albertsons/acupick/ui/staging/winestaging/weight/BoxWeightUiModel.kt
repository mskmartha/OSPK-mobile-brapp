package com.albertsons.acupick.ui.staging.winestaging.weight

import androidx.lifecycle.MutableLiveData
import com.albertsons.acupick.data.model.BoxType

class BoxWeightUiModel(
    val boxType: BoxType?,
    val label: String,
    val boxNumber: String,
    val totalWeight: MutableLiveData<String>,
    val onClick: ((boxWeightUiModel: BoxWeightUiModel) -> Unit)? = null
) {
    private val splitLabel = label.split("-")
    val formattedBoxLabel = "${splitLabel.getOrNull(3)}-${splitLabel.getOrNull(4)}"
    fun onItemClicked() {
        onClick?.invoke(this)
    }
}
