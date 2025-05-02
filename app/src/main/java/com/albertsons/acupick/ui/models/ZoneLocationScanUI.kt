package com.albertsons.acupick.ui.models

import com.albertsons.acupick.data.model.StorageLocationType

data class ZoneLocationScanUI(
    val customerOrderNumber: String,
    val storageTypes: List<StorageLocationType>? = null,
) : UIModel
