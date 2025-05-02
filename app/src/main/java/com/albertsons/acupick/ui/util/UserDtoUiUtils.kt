package com.albertsons.acupick.ui.util

import com.albertsons.acupick.data.model.request.UserDto
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank

fun UserDto?.asFirstInitialDotLastString(): String? {
    return if (this == null) {
        null
    } else {
        val firstInitial = firstName?.take(1)
        if (firstInitial.isNotNullOrBlank()) {
            "$firstInitial. $lastName"
        } else {
            lastName
        }
    }
}
