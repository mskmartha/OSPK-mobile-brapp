package com.albertsons.acupick.data.test.pick

import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.ErItemDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.PickedItemUpcDto
import com.albertsons.acupick.data.model.response.ShortedItemUpcDto

/** Different types of offline tests have different comparison criteria (ex: recordPick won't set any db ids since they must come from the backend api call response */
internal enum class OfflineTestType {
    /** Offline tests that are additive will not have access to the online success response that includes db ids. Don't factor in these db ids when comparing offline results with expected output */
    Additive,
    /** Offline tests that are subtractive will have access to the baseline response data that includes db ids. Factor these db ids in when comparing offline results with expected output */
    Subtractive
}

/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
internal fun ActivityDto.removeUnnecessaryOfflineData(offlineTestType: OfflineTestType): ActivityDto {
    return copy(
        containerActivities = containerActivities?.map { it.removeUnnecessaryOfflineData(offlineTestType) },
        itemActivities = itemActivities?.map { it.removeUnnecessaryOfflineData(offlineTestType) },
    )
}

/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
private fun ContainerActivityDto.removeUnnecessaryOfflineData(offlineTestType: OfflineTestType): ContainerActivityDto {
    return this.copy(
        containerItems = containerItems?.map { it.removeUnnecessaryOfflineData(offlineTestType) },
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
private fun ErItemDto.removeUnnecessaryOfflineData(offlineTestType: OfflineTestType): ErItemDto {
    return this.copy(
        regulated = null, // unable to derive this info from online responses and while offline
    )
}

/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
private fun ItemActivityDto.removeUnnecessaryOfflineData(offlineTestType: OfflineTestType): ItemActivityDto {
    return this.copy(
        attemptToRemove = null,
        pickedUpcCodes = pickedUpcCodes?.map { it.removeUnnecessaryOfflineData(offlineTestType) },
        shortedItemUpc = shortedItemUpc?.map { it.removeUnnecessaryOfflineData(offlineTestType) },
    )
}
/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
private fun PickedItemUpcDto.removeUnnecessaryOfflineData(offlineTestType: OfflineTestType): PickedItemUpcDto {
    return this.copy(
        pickedTime = pickedTime, // know this when offline
        upcId = when (offlineTestType) {
            OfflineTestType.Additive -> null // unable to know this when offline
            OfflineTestType.Subtractive -> upcId
        },
    )
}
/** Null out certain values that 1) cannot be retrieved from online response or while offline as well as 2) some values are unnecessary for use in the picking flow of the app */
private fun ShortedItemUpcDto.removeUnnecessaryOfflineData(offlineTestType: OfflineTestType): ShortedItemUpcDto {
    return this.copy(
        shortedTime = shortedTime, // know this when offline
        shortedId = when (offlineTestType) {
            OfflineTestType.Additive -> null // unable to know this when offline
            OfflineTestType.Subtractive -> shortedId
        },
    )
}
