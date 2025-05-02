package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class CompleteHandoffData(val handOffInterstitialParamsList: HandOffInterstitialParamsList) : Parcelable, Dto
