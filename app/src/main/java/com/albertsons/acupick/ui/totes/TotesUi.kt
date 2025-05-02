package com.albertsons.acupick.ui.totes

import android.graphics.Paint
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.asFirstInitialDotLastString
import com.albertsons.acupick.data.model.response.stageByTimeInSpacedFormat
import com.albertsons.acupick.infrastructure.utils.getFormattedWithAmPm
import com.albertsons.acupick.ui.models.FulfillmentTypeUI
import com.albertsons.acupick.ui.picklistitems.TOTE_UI_COUNT
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.toFulfillmentTypeUI

const val CUSTOMER_NAME_MAX_LENGTH = 160 // text cannot exceed 160dp. It is at 157dp that ellipsize is triggered

data class TotesUi(
    val orderNumber: String,
    val fulfillmentType: FulfillmentTypeUI?,
    val fulfillmentTime: String,
    val numberOfTotes: Int,
    val totesSubUi: List<TotesSubUi>,
    val customerFullName: String,
    val customerShortName: String,
    val pickNumber: String,
    val orderType: OrderType,
    val stageByTime: String,
    val totalItems: String,
    val activityDto: ActivityDto?,
    val isEbt: Boolean
) {

    constructor(itemActivityDto: ItemActivityDto?, activityDto: ActivityDto?) : this(
        orderNumber = itemActivityDto?.customerOrderNumber.orEmpty(),
        fulfillmentType = itemActivityDto?.fulfillment?.toFulfillmentTypeUI(),
        fulfillmentTime = getFormattedWithAmPm(activityDto?.expectedEndTime).orEmpty(),
        numberOfTotes = setNumberOfTotes(getContainerForSubViews(activityDto?.containerActivities, itemActivityDto)),
        totesSubUi = getContainerForSubViews(activityDto?.containerActivities, itemActivityDto),
        customerFullName = itemActivityDto?.getCustomerName().orEmpty(),
        customerShortName = itemActivityDto?.asFirstInitialDotLastString().orEmpty(),
        pickNumber = activityDto?.activityNo.orEmpty(),
        orderType = activityDto?.orderType ?: OrderType.REGULAR,
        stageByTime = activityDto?.stageByTimeInSpacedFormat().orEmpty(),
        totalItems = activityDto?.expectedCount.toString(),
        activityDto = activityDto,
        isEbt = itemActivityDto?.isSnap.orFalse()
    )

    constructor(orderNumber: String?, itemActivityDto: ItemActivityDto?, activityDto: ActivityDto?) : this(
        orderNumber = orderNumber.orEmpty(),
        fulfillmentType = itemActivityDto?.fulfillment?.toFulfillmentTypeUI(),
        fulfillmentTime = getFormattedWithAmPm(activityDto?.expectedEndTime).orEmpty(),
        numberOfTotes = setNumberOfTotes(getContainerForSubViews(activityDto?.containerActivities, itemActivityDto)),
        totesSubUi = getContainerForSubViews(activityDto?.containerActivities, itemActivityDto),
        customerFullName = activityDto?.itemActivities?.find { it.customerOrderNumber == orderNumber }?.getCustomerName().orEmpty(),
        customerShortName = activityDto?.itemActivities?.find { it.customerOrderNumber == orderNumber }?.asFirstInitialDotLastString().orEmpty(),
        pickNumber = activityDto?.activityNo.orEmpty(),
        orderType = activityDto?.orderType ?: OrderType.REGULAR,
        stageByTime = activityDto?.stageByTimeInSpacedFormat().orEmpty(),
        totalItems = activityDto?.expectedCount.toString(),
        activityDto = activityDto,
        isEbt = activityDto?.isSnap.orFalse()
    )

    fun getCustomerName(startMargin: Int): String {
        val fullNameTooLong = (Paint().measureText(customerFullName).toInt() + startMargin) >= CUSTOMER_NAME_MAX_LENGTH
        return when {
            fullNameTooLong -> customerShortName
            else -> customerFullName
        }
    }

    fun getStorageItemTypes(): Map<StorageType?, Double>? {
        val itemActivities = activityDto?.itemActivities?.filter { it.customerOrderNumber == orderNumber }
        return itemActivities?.groupBy { it.storageType }?.mapValues { itemsByStorageType -> itemsByStorageType.value.sumOf { it.qty ?: 0.0 } }
    }

    fun getTotalCount(customerOrderNumber: String?): Int {
        val itemActivities = activityDto?.itemActivities?.filter { it.customerOrderNumber == customerOrderNumber }
        return itemActivities?.sumBy { it.qty?.toInt() ?: 0 } ?: 0
    }

    companion object {

        fun setNumberOfTotes(totesSubUi: List<TotesSubUi>?): Int {
            return totesSubUi?.count() ?: 0
        }

        fun getContainerForSubViews(containerActivityDto: List<ContainerActivityDto>?, itemActivityDto: ItemActivityDto?): List<TotesSubUi> {
            val dbVmList = arrayListOf<TotesSubUi>()
            containerActivityDto?.filter { it.customerOrderNumber == itemActivityDto?.customerOrderNumber }?.map { container ->
                dbVmList.add(
                    TotesSubUi(container)
                )
            }
            return dbVmList
        }
    }
}

data class TotesSubUi(
    val orderNumber: String,
    val storageType: StorageType?,
    val containerId: String,
    val numberOfItemsInTote: Int,
) {
    constructor(containerActivityDto: ContainerActivityDto?) : this(
        orderNumber = containerActivityDto?.customerOrderNumber.orEmpty(),
        storageType = containerActivityDto?.containerType,
        containerId = containerActivityDto?.containerId?.takeLast(TOTE_UI_COUNT).orEmpty(),
        numberOfItemsInTote = containerActivityDto?.containerItems?.sumBy { it.qty?.toInt() ?: 0 } ?: 0,
    )
}
