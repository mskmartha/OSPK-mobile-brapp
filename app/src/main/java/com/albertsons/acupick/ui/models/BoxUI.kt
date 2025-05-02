package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.BoxData
import com.albertsons.acupick.data.model.StorageType
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class BoxUI(
    val zoneType: StorageType,
    val referenceEntityId: String,
    val type: String,
    val orderNumber: String,
    val boxNumber: String,
    val isLoose: Boolean,
    val label: String,
) : UIModel, Parcelable {

    fun toBoxData(): BoxData {
        return BoxData(
            referenceEntityId = referenceEntityId,
            zoneType = zoneType,
            type = type,
            orderNumber = orderNumber,
            boxNumber = boxNumber,
            label = label
        )
    }
}
