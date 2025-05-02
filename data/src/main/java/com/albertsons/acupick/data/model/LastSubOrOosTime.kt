package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class LastSubOrOosTime(val lastSubOrOosTime: ZonedDateTime) : Parcelable
