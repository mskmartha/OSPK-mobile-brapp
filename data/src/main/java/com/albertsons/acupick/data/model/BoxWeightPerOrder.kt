package com.albertsons.acupick.data.model

data class BoxDetails(
    val referenceEntityId: String?,
    val weight: String?,
    val label: String?,
    val type: BoxType?,
    val boxNumber: String?,
)

fun BoxDetails.toDto() = BoxDetailsDto(
    referenceEntityId = referenceEntityId,
    weight = weight?.toFloat(),
    label = label,
    type = type?.toDto(),
    boxNumber = boxNumber
)

private fun BoxType.toDto() = when (this) {
    BoxType.XS -> BoxTypeDto.XS
    BoxType.SS -> BoxTypeDto.SS
    BoxType.MM -> BoxTypeDto.MM
    BoxType.LL -> BoxTypeDto.LL
    BoxType.XL -> BoxTypeDto.XL
}
