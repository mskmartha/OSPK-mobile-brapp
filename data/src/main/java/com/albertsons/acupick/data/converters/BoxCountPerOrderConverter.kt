package com.albertsons.acupick.data.converters

import com.albertsons.acupick.data.model.BoxCountPerOrder
import com.albertsons.acupick.data.model.BoxCountPerOrderDto
import com.albertsons.acupick.data.model.BoxTypeCountDto
import com.albertsons.acupick.data.model.BoxTypeDto

fun BoxCountPerOrder.toDto() = BoxCountPerOrderDto(
    referenceEntityId = this.referenceEntityId.orEmpty(),
    this.boxTypeCount?.map { boxTypeCount ->
        BoxTypeCountDto(BoxTypeDto.valueOf(boxTypeCount.boxType?.name.orEmpty()), boxTypeCount.count)
    }
)

fun List<BoxCountPerOrder>?.toDtos() = this?.map {
    it.toDto()
}
