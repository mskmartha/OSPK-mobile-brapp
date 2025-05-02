package com.albertsons.acupick.data.model

import java.io.Serializable

sealed class FulfilledQuantityResult {
    object DefaultQuantity : FulfilledQuantityResult(), Serializable

    data class QuantityPicker(
        val quantity: Int
    ) : FulfilledQuantityResult(), Serializable

    data class ConfirmNetWeightResult(
        val netWeight: Double,
        val quantity: Int,
        val itemType: SellByType
    ) : FulfilledQuantityResult(), Serializable

    fun toQuantity(): Int = when (this) {
        is DefaultQuantity -> 1
        is QuantityPicker -> quantity
        is ConfirmNetWeightResult -> quantity
    }

    fun toWeight(): Double = when (this) {
        is DefaultQuantity -> 0.0
        is QuantityPicker -> 0.0
        is ConfirmNetWeightResult -> netWeight
    }
}
