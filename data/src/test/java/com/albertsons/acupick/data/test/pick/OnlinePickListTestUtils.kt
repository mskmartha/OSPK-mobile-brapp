package com.albertsons.acupick.data.test.pick

import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.ErItemDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.PickedItemUpcDto
import com.albertsons.acupick.data.model.response.ShortedItemUpcDto

/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
internal fun ActivityDto.removeUnnecessaryOnlineData(): ActivityDto {
    return copy(
        status = null,
        containerActivities = containerActivities?.map { it.removeUnnecessaryOnlineData() },
        itemActivities = itemActivities?.map { it.removeUnnecessaryOnlineData() },
    )
}

/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
private fun ContainerActivityDto.removeUnnecessaryOnlineData(): ContainerActivityDto {
    return this.copy(
        containerItems = containerItems?.map { it.removeUnnecessaryOnlineData() },
        id = null, // unable to derive this info from online responses and while offline
        regulated = null, // unable to derive this info from online responses and while offline
        attemptToRemove = null,
        lastScanTime = null,
        bagCount = null,
        status = null,
        location = null,
        nextDestination = null,
    )
}

/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
private fun ErItemDto.removeUnnecessaryOnlineData(): ErItemDto {
    return this.copy(
        regulated = null, // unable to derive this info from online responses and while offline
    )
}

/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
private fun ItemActivityDto.removeUnnecessaryOnlineData(): ItemActivityDto {
    return this.copy(
        attemptToRemove = null,
        pickedUpcCodes = pickedUpcCodes?.map { it.removeUnnecessaryOnlineData() },
        shortedItemUpc = shortedItemUpc?.map { it.removeUnnecessaryOnlineData() },
    )
}
/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
private fun PickedItemUpcDto.removeUnnecessaryOnlineData(): PickedItemUpcDto {
    return this.copy(
        pickedTime = pickedTime, // known when online
        upcId = upcId, // known when online
    )
}
/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
private fun ShortedItemUpcDto.removeUnnecessaryOnlineData(): ShortedItemUpcDto {
    return this.copy(
        shortedTime = shortedTime, // know this when online
        shortedId = shortedId, // know this when online
    )
}
