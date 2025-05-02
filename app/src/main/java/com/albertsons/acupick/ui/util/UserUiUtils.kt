package com.albertsons.acupick.ui.util

import com.albertsons.acupick.data.model.User

fun User.firstInitialDotLastName(): String {
    val firstInitial = this.firstName.substring(0, 1)
    val lastName = this.lastName
    return "$firstInitial. $lastName"
}

fun User.storeNumberTitle(): String {
    return "Store ${this.selectedStoreId}"
}

fun User.storeNumberTitleForNavMenuItem(): String {
    return "Store #${this.selectedStoreId}"
}

fun User.firstAndLastName(): String {
    return "${this.firstName} ${this.lastName}"
}
