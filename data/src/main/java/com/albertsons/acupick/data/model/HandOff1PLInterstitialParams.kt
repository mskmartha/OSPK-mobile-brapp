package com.albertsons.acupick.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.request.RemoveItems1PLRequestDto
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class Complete1PLHandoffData(val handOffInterstitialParamsList: HandOff1PLInterstitialParams) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class HandOff1PLInterstitialParams(
    val actId: Long? = null,
    var isHandOffReassigned: Boolean = false,
    val handOffAction: HandOff1PLAction = HandOff1PLAction.COMPLETE,
    val removeItems1PLRequestDto: RemoveItems1PLRequestDto
) : Parcelable

@JsonClass(generateAdapter = false)
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
enum class HandOff1PLAction {
    COMPLETE,
    CANCEL,
}
