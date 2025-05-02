package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.request.BoxTypeCount

data class BoxCountPerOrder(val referenceEntityId: String?, val boxTypeCount: List<BoxTypeCount>?)
