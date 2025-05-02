package com.albertsons.acupick.data.model.request

import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AddParticipantDto(
    val conversationSid: String,
    val userId: String
) : Dto
