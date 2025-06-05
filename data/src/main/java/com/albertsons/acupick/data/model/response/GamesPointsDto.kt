package com.albertsons.acupick.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class GamesPointsDto(
    @Json(name = "storeNumber") val storeNumber: String?,
    @Json(name = "eventUserId") val eventUserId: String?,
    @Json(name = "bestOTH5") val bestOTH5: String?,
    @Json(name = "bestWaitTimeBreakDown") val bestWaitTimeBreakDown: String?,
    @Json(name = "totalPoints") val totalPoints: Int?,
    @Json(name = "bestWaitTime") val bestWaitTime: String?,
    @Json(name = "numberOfOTH5EligibleOrder") val numberOfOTH5EligibleOrder: Int?,
    @Json(name = "numberOfTotalOrder") val numberOfTotalOrder: Int?,
    @Json(name = "rules") val rules: List<String>?, // Adjust type if rules is a complex object
    @Json(name = "gameId") val gameId: String?,
    @Json(name = "leagueId") val leagueId: String?,
    @Json(name = "tournamentId") val tournamentId: String?,
    @Json(name = "consumedFromEODJob") val consumedFromEODJob: Boolean?,
    @Json(name = "averageWaitTime") val averageWaitTime: String?,
    @Json(name = "averageWaitTimeBreakDown") val averageWaitTimeBreakDown: String?,
    @Json(name = "currentWaitTime") val currentWaitTime: String?,
    @Json(name = "currentWaitTimeBreakDown") val currentWaitTimeBreakDown: String?,
    @Json(name = "score") val score: Int?,
    @Json(name = "scoreBreakdown") val scoreBreakdown: String?,
    @Json(name = "orderNumbers") val orderNumbers: List<String>?
) : Dto, Parcelable