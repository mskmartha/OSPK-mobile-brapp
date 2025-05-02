package com.albertsons.acupick.data.logic

import com.albertsons.acupick.data.model.CustomerType

fun getCustomerType(isSnap: Boolean, isSubscription: Boolean): CustomerType? = when {
    isSnap && isSubscription -> CustomerType.BOTH
    isSnap && !isSubscription -> CustomerType.SNAP
    !isSnap && isSubscription -> CustomerType.SUBSCRIPTION
    else -> null
}

fun shouldShowCustomerType(cattEnabled: Boolean, customerType: CustomerType?): Boolean {
    return when {
        // Freshpass icon shouldn't display for subs when catt is disabled
        cattEnabled && customerType != null -> true
        !cattEnabled && customerType == CustomerType.SNAP ||
            !cattEnabled && customerType == CustomerType.BOTH -> true
        else -> false
    }
}
