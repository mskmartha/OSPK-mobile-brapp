package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
@JsonClass(generateAdapter = true)
data class ErrorMessage(
    val event: Event,
    val data: ErrorData,
) : Parcelable, Dto

@Parcelize
@JsonClass(generateAdapter = true)
data class Event(
    val id: String,
    val desc: String,
    val eventTs: ZonedDateTime,
) : Parcelable, Dto

@Parcelize
@JsonClass(generateAdapter = true)
data class ErrorData(
    val storeNumber: String?,
    val userId: String?,
    val orderNumber: String? = null,
    val conversationSid: String? = null,
    val stagingBlockedTime: Int? = null,
    val conversationSids: List<String>? = null,
    val orderNumbers: List<String>? = null,
) : Parcelable, Dto
