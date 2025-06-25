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
    @Json(name = "totalPoints") val totalPoints: Int?,
    @Json(name = "gameId") val gameId: String?,
    @Json(name = "leagueId") val leagueId: String?,
    @Json(name = "tournamentId") val tournamentId: String?,
    @Json(name = "consumedFromEODJob") val consumedFromEODJob: Boolean? = false,
    @Json(name = "bestOTH5") val bestOTH5: String? = null,
    @Json(name = "bestWaitTime") val bestWaitTime: String? = null,
    @Json(name = "bestWaitTimeBreakDown") val bestWaitTimeBreakDown: BestWaitTimeBreakDownDto?,
    @Json(name = "numberOfOTH5EligibleOrder") val numberOfOTH5EligibleOrder: Int? = null,
    @Json(name = "numberOfTotalOrder") val numberOfTotalOrder: Int? = null,
    @Json(name = "rules") val rules: List<String>? = null,
    @Json(name = "averageWaitTime") val averageWaitTime: String? = null,
    @Json(name = "averageWaitTimeBreakDown") val averageWaitTimeBreakDown: AverageWaitTimeBreakDownDto?,
    @Json(name = "currentWaitTime") val currentWaitTime: String? = null,
    @Json(name = "score") val score: Int?,
    @Json(name = "scoreBreakdown") val scoreBreakdown: Map<String, ScoreBreakdownDetailDto>?,
    @Json(name = "orderNumbers") val orderNumbers: List<String>?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class BestWaitTimeBreakDownDto(
    @Json(name = "handOffStartTimeDiff") val handOffStartTimeDiff: Int?,
    @Json(name = "deStageTimeSpendDiff") val deStageTimeSpendDiff: Int?,
    @Json(name = "walkoutTimeSpendDiff") val walkoutTimeSpendDiff: Int?,
    @Json(name = "totalTimeDiff") val totalTimeDiff: Int?
) :Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class AverageWaitTimeBreakDownDto(
    @Json(name = "handOffStartTimeDiff") val handOffStartTimeDiff: Double?,
    @Json(name = "deStageTimeSpendDiff") val deStageTimeSpendDiff: Double?,
    @Json(name = "walkoutTimeSpendDiff") val walkoutTimeSpendDiff: Double?,
    @Json(name = "totalTimeDiff") val totalTimeDiff: Double?
) :Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class ScoreBreakdownDetailDto(
    @Json(name = "description") val description: String?,
    @Json(name = "score") val score: String? // Assuming score is a string in the breakdown
) :Dto, Parcelable