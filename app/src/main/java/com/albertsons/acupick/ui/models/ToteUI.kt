package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.fullContactName
import kotlinx.android.parcel.Parcelize

@Parcelize
@Keep
data class ToteUI(
    val storageType: StorageType?,
    val toteId: String?,
    val customerOrderNumber: String?,
    val customerName: String,
    var intialBagCount: Int? = null,
    var intialLooseCount: Int? = null,
    var isChecked: Boolean = false
) : UIModel, Parcelable {
    constructor(containerActivity: ContainerActivityDto, intialBagCount: Int?, intialLooseCount: Int?) : this(
        storageType = containerActivity.containerType,
        toteId = containerActivity.containerId,
        customerOrderNumber = containerActivity.customerOrderNumber,
        customerName = containerActivity.fullContactName(),
        intialBagCount = intialBagCount,
        intialLooseCount = intialLooseCount,
    )
    // partial constructor used to interface stagingOne/stagingTwo
    // this constructor and the totelist nav argument can be removed
    // if we more fully utilize the new staging data cache
    constructor(toteId: String, customerOrderNumber: String, storageType: StorageType?) : this(
        storageType = storageType,
        toteId = toteId,
        customerOrderNumber = customerOrderNumber,
        customerName = String(),
        intialBagCount = null,
        intialLooseCount = null,
    )
}
