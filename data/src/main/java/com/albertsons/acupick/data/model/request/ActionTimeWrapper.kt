package com.albertsons.acupick.data.model.request

import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime

/** Wraps T and also includes [actionTime] */
@JsonClass(generateAdapter = true)
data class ActionTimeWrapper<T>(val wrapped: T, val actionTime: ZonedDateTime = ZonedDateTime.now())

fun <T> T.wrapActionTime(actionTime: ZonedDateTime = ZonedDateTime.now()): ActionTimeWrapper<T> = ActionTimeWrapper(this, actionTime)
