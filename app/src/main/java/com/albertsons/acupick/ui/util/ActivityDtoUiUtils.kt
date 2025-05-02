package com.albertsons.acupick.ui.util

import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.response.ActivityDto

private const val DELIVERY_DRIVER_NAME_MAX_LENGTH = 17

/** Driver Name needs to be first initial and last name if full name is over 20 characters long,
If that is still over 20 characters, it needs to be trimmed with ellipises.
If no name is in the data, replace with N/A  */
fun ActivityDto.fullDriverName(): String? {
    val (firstName, lastName) = if (fulfillment?.type != FulfillmentType.DELIVERY) contactFirstName to contactLastName
    else driver?.firstName to driver?.lastName
    var name = "${firstName?.take(1)?.let { "$it. " }.orEmpty()}${lastName.orEmpty()}".trim()
    return if (name.isNotEmpty()) {
        if (name.length > DELIVERY_DRIVER_NAME_MAX_LENGTH) {
            name = "${name.take(DELIVERY_DRIVER_NAME_MAX_LENGTH).trimEnd()}..."
        }
        return name
    } else {
        null
    }
}
