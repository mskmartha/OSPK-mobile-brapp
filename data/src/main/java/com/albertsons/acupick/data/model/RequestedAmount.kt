package com.albertsons.acupick.data.model

import java.io.Serializable

data class RequestedAmount(
    val baseWeight: Double,
    val totalRequestedNetWeight: Double,
    val currentNetWeight: Double,
    val weightUOM: String
) : Serializable
