package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.asFirstInitialDotLastString
import com.albertsons.acupick.data.model.response.isReshop
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Parcelize
@Keep
data class BagLabel(
    val customerOrderNumber: String?,
    val fulfillmentOrderNumber: String?,
    val customerNameFirstInitialLast: String?,
    val zoneType: StorageType?,
    val zoneId: String?,
    val labelId: String?,
    var isScanned: Boolean = false,
    var containerScanTime: ZonedDateTime? = null,
    var isLoose: Boolean? = null,
    val isReshop: Boolean,
    val isMultiSourceOrder: Boolean? = false,
    val isCustomerBagPreference: Boolean?,
    val bagCount: Int?
) : Parcelable {

    constructor(containerActivityDto: ContainerActivityDto, activityDto: ActivityDto) : this(
        customerOrderNumber = containerActivityDto.customerOrderNumber,
        fulfillmentOrderNumber = containerActivityDto.reference?.entityId,
        customerNameFirstInitialLast = containerActivityDto.asFirstInitialDotLastString(),
        zoneType = containerActivityDto.containerType,
        zoneId = containerActivityDto.location,
        labelId = containerActivityDto.containerId,
        isLoose = containerActivityDto.type == ContainerType.LOOSE_ITEM,
        isReshop = containerActivityDto.isReshop(),
        isMultiSourceOrder = activityDto.isMultiSource,
        isCustomerBagPreference = activityDto.isCustomerBagPreference,
        bagCount = containerActivityDto.bagCount
    )

    fun getShortBagLabel() = if (labelId?.contains('-') == true) labelId.split('-').last() else labelId
    // Todo when adding dialog to ManualEntry Mfc, this will be needed same as above
    fun getShortToteLabel() = if (labelId?.contains('-') == true) labelId.split('-').first() else labelId
}
