package com.albertsons.acupick.ui.arrivals.destage

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.getFulfillmentTypeDescriptions
import com.albertsons.acupick.ui.arrivals.complete.HandOffUI.Companion.regulatedItemsList
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.convertArrivalStatus
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.orTrue
import kotlinx.coroutines.Job
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Parcelize
@Keep
class DetailsHeaderUi(
    val activityId: Long?,
    val shortOrderNumber: String?,
    val contactName: String?,
    val customerOrderNumber: String?,
    val expectedCount: Int?,
    val showRegulatedError: Boolean?,
    val startTime: ZonedDateTime?,
    val customerArrivalStatusUI: CustomerArrivalStatusUI?,
    val isCustomerBagPreference: Boolean,
    val isGift: Boolean,
    val onStartTimer: (Job) -> Unit
) : Parcelable {

    constructor(orderDetails: ActivityDto, onStartTimer: (Job) -> Unit) : this(
        activityId = orderDetails.actId,
        shortOrderNumber = orderDetails.getFulfillmentTypeDescriptions(),
        contactName = orderDetails.fullContactName(),
        customerOrderNumber = orderDetails.customerOrderNumber,
        expectedCount = orderDetails.expectedCount,
        showRegulatedError = regulatedItemsList(orderDetails).isNotEmpty(),
        startTime = orderDetails.nextActExpStartTime,
        customerArrivalStatusUI = convertArrivalStatus(orderDetails.subStatus),
        isCustomerBagPreference = orderDetails.isCustomerBagPreference.orTrue(),
        isGift = orderDetails.isGift.orFalse(),
        onStartTimer = onStartTimer
    )
}
