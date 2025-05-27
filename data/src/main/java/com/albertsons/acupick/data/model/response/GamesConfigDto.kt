package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize


@JsonClass(generateAdapter = true)
@Parcelize
data class GameConfigDto(
    @Json(name = "gameId") val gameId: String?,
    @Json(name = "tournamentId") val tournamentId: String?,
    @Json(name = "tournamentStartDate") val tournamentStartDate: String?,
    @Json(name = "tournamentEndDate") val tournamentEndDate: String?,
    @Json(name = "basePointsRules") val basePointsRules: List<OthRule>?,
    @Json(name = "userOTH") val userOTH: List<OthRule>?,
    @Json(name = "storeOTH") val storeOTH: List<OthRule>?
) :Dto, Parcelable


@JsonClass(generateAdapter = true)
@Parcelize
data class OthRule(
    @Json(name = "ruleName") val ruleName: String?,
    @Json(name = "expression") val expression: String?,
    @Json(name = "score") val score: Int?,
    @Json(name = "displayOrder") val displayOrder: Int?
) : Dto, Parcelable