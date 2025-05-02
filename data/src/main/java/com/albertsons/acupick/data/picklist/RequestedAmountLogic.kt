package com.albertsons.acupick.data.picklist

import com.albertsons.acupick.data.model.ConfirmAmountError
import com.albertsons.acupick.data.model.FulfilledQuantityResult
import com.albertsons.acupick.data.model.RequestedAmount
import com.albertsons.acupick.data.model.SellByType
import java.math.BigDecimal
import java.math.RoundingMode

val RequestedAmount.enteredWeightString: String
    get() = BigDecimal(currentNetWeight).setScale(3, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString()

val RequestedAmount.netWeightString: String
    get() = BigDecimal(totalRequestedNetWeight).setScale(3, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString()

fun RequestedAmount.toConfirmAmountError(enteredAmount: Double, itemType: SellByType): ConfirmAmountError {
    return if (itemType == SellByType.PriceEachTotal) {
        when {
            enteredAmount.toInt() <= 0 -> ConfirmAmountError.InvalidAmountOrQty
            (currentNetWeight + enteredAmount) > totalRequestedNetWeight -> ConfirmAmountError.ExceedsMaxLimit
            else -> ConfirmAmountError.None
        }
    } else {
        when {
            enteredAmount <= minimumPickQty -> ConfirmAmountError.TooLight
            enteredAmount + currentNetWeight > maximumPickQty -> ConfirmAmountError.TooHeavy
            else -> ConfirmAmountError.None
        }
    }
}

private const val MIN_ROUND_UP_DECIMAL = 0.501
val RequestedAmount.minimumPickQty: Double
    get() = baseWeight * MIN_ROUND_UP_DECIMAL

private const val MAX_ROUND_DOWN_DECIMAL = 0.5
val RequestedAmount.maximumPickQty: Double
    get() = totalRequestedNetWeight + (baseWeight * MAX_ROUND_DOWN_DECIMAL)

// TODO Redesign Need to optimize this casting based on item type.
fun RequestedAmount.toConfirmNetWeightResult(confirmedNetWeight: Double, itemType: SellByType) =
    FulfilledQuantityResult.ConfirmNetWeightResult(
        netWeight = if (itemType == SellByType.PriceEachTotal) {
            0.0
        } else {
            BigDecimal(confirmedNetWeight).setScale(3, RoundingMode.HALF_EVEN).toDouble()
        },
        quantity = if (itemType == SellByType.PriceEachTotal) {
            confirmedNetWeight.toInt()
        } else {
            BigDecimal(confirmedNetWeight / baseWeight).setScale(0, RoundingMode.HALF_DOWN).toInt()
        },
        itemType = itemType
    )

val RequestedAmount.isPartialPick: Boolean
    get() = currentNetWeight > 0.0
