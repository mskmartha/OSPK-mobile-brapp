package com.albertsons.acupick.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class GamesPointsDto(
    @Json(name = "gameInfo") val gameInfo: GameInfoDto?,
    @Json(name = "storeNumber") val storeNumber: String?,
    @Json(name = "eventUserId") val eventUserId: String?,
    @Json(name = "totalPoints") val totalPoints: String?,
    @Json(name = "leagueId") val leagueId: String?,
    @Json(name = "playerBaseScoreDetails") val playerBaseScoreDetails: PlayerBaseScoreDetailsDto?,
    @Json(name = "playerOTHScoreDetails") val playerOTHScoreDetails: PlayerOTHScoreDetailsDto?,
    @Json(name = "playerWaitTimeBreakdown") val playerWaitTimeBreakdown: PlayerWaitTimeBreakdownDto?,
    @Json(name = "storeOTHScoreDetails") val storeOTHScoreDetails: StoreOTHScoreDetailsDto?,
    @Json(name = "orderNumbersServicedByPlayer") val orderNumbersServicedByPlayer: List<Long>?,
    @Json(name = "playerTodayTrend") val playerTodayTrend: List<PlayerTodayTrendDto>?,
    @Json(name = "playerPlayerPerformanceSummaryTillDate") val playerPerformanceSummaryTillDate: PlayerPerformanceSummaryDto?,
    @Json(name = "leaderBoardDetails") val leaderBoardDetails: LeaderBoardDetailsDto?,
    @Json(name = "totalPlayersInLeague") val totalPlayersInLeague: String?
) : Dto, Parcelable


@JsonClass(generateAdapter = true)
@Parcelize
data class GameInfoDto(
    @Json(name = "gameName") val gameName: String?,
    @Json(name = "gameType") val gameType: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "recurringFrequency") val recurringFrequency: String?,
    @Json(name = "activeTournament") val activeTournament: String?,
    @Json(name = "startDate") val startDate: String?,
    @Json(name = "endDate") val endDate: String?,
    @Json(name = "modifiedBy") val modifiedBy: String?,
    @Json(name = "modifiedAt") val modifiedAt: String?,
    @Json(name = "createdBy") val createdBy: String?,
    @Json(name = "createdAt") val createdAt: String?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PlayerBaseScoreDetailsDto(
    @Json(name = "baseScore") val baseScore: String?,
    @Json(name = "baseScoreBreakdown") val baseScoreBreakdown: Map<String, ScoreBreakdownDetailDto>?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PlayerOTHScoreDetailsDto(
    @Json(name = "othScore") val othScore: String?,
    @Json(name = "dailyPlayerOTHScore") val dailyPlayerOTHScore: Map<String, PlayerDailyOTHScoreDto>?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class StoreOTHScoreDetailsDto(
    @Json(name = "storeOTHScore") val storeOTHScore: String?,
    @Json(name = "dailyStoreOTHScore") val dailyStoreOTHScore: Map<String, StoreDailyOTHScoreDto>?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PlayerDailyOTHScoreDto(
    @Json(name = "othPercentage") val othPercentage: String?,
    @Json(name = "othScore") val othScore: String?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class StoreDailyOTHScoreDto(
    @Json(name = "storeOTHPercentage") val storeOTHPercentage: String?,
    @Json(name = "storeOTHScore") val storeOTHScore: String?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PlayerTodayTrendDto(
    @Json(name = "trendName") val trendName: String?,
    @Json(name = "trendValue") val trendValue: TrendValueDto?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class TrendValueDto(
    @Json(name = "score") val score: String? = null,
    @Json(name = "percentage") val percentage: String? = null,
    @Json(name = "description") val description: String? = null
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PlayerPerformanceSummaryDto(
    @Json(name = "keyValue") val keyValue: Map<String, String>?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class LeaderBoardDetailsDto(
    @Json(name = "players") val players: List<LeaderBoardPlayerDto>?,
    @Json(name = "lastUpdateInfo") val lastUpdateInfo: String?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class LeaderBoardPlayerDto(
    @Json(name = "playerId") val playerId: String?,
    @Json(name = "pointsMap") val pointsMap: List<String>?,
    @Json(name = "rank") val rank: Int?,
    @Json(name = "totalPoints") val totalPoints: Int?,
    @Json(name = "trend") val trend: String?
) : Dto, Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class ScoreBreakdownDetailDto(
    @Json(name = "description") val description: String?,
    @Json(name = "score") val score: String? // Assuming score is a string in the breakdown
) :Dto, Parcelable


@JsonClass(generateAdapter = true)
@Parcelize
data class PlayerWaitTimeBreakdownDto(
    @Json(name = "bestWaitTimeBreakDown") val bestWaitTimeBreakDown: WaitTimeDto?,
    @Json(name = "averageWaitTimeBreakDown") val averageWaitTimeBreakDown: WaitTimeDto?,
    @Json(name = "bestPlayerAverageWaitTimeBreakDown") val bestPlayerAverageWaitTimeBreakDown: WaitTimeDto?
) : Dto, Parcelable


@JsonClass(generateAdapter = true)
@Parcelize
data class WaitTimeDto(
    @Json(name = "handOffStartTimeDiff") val handOffStartTimeDiff: Double?,
    @Json(name = "deStageTimeSpendDiff") val deStageTimeSpendDiff: Double?,
    @Json(name = "walkoutTimeSpendDiff") val walkoutTimeSpendDiff: Double?,
    @Json(name = "totalTimeDiff") val totalTimeDiff: Double?
) : Dto, Parcelable