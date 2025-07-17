package com.albertsons.acupick.ui.my_score

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.response.DailyOTHScoreUIModel
import com.albertsons.acupick.data.model.response.GamesPointsDto
import com.albertsons.acupick.data.model.response.LeaderBoardPlayerDto
import com.albertsons.acupick.data.model.response.OthRule
import com.albertsons.acupick.data.model.response.PlayerDailyOTHScoreDto
import com.albertsons.acupick.data.model.response.PlayerTodayTrendDto
import com.albertsons.acupick.data.model.response.StoreDailyOTHScoreDto
import com.albertsons.acupick.data.model.response.toUIMappedList
import com.albertsons.acupick.data.model.response.toUIModelListNew
import com.albertsons.acupick.databinding.AdapterDailyScoresBinding
import com.albertsons.acupick.databinding.AdapterGameInfoBinding
import com.albertsons.acupick.databinding.AdapterLeaderBoardBinding
import com.albertsons.acupick.ui.util.dpToPx
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@BindingAdapter("app:playersCountText")
fun setPlayersCountText(textView: TextView, totalPlayers: String?) {
    textView.text = if (!totalPlayers.isNullOrBlank()) {
        "$totalPlayers Players"
    } else {
        "0 Players"
    }
}


@BindingAdapter("app:endsInText")
fun setEndsInText(textView: TextView, data: GamesPointsDto?) {
    val endDate = data?.gameInfo?.endDate ?: return
    try {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val end = formatter.parse(endDate)
        val now = Date()

        if (end != null) {
            val diffInMillis = end.time - now.time
            val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

            val text = if (days > 0) {
                "Ends in $days days"
            } else {
                "Ended"
            }

            textView.text = text
        } else {
            textView.text = "Ends in N/A"
        }

    } catch (e: Exception) {
        textView.text = "Ends in N/A"
    }
}

@BindingAdapter("app:lastLeaderBoardUpdatedAt")
fun setLeaderBoardLastUpdatedAt(textView: TextView, data: GamesPointsDto?) {
    val leaderBoardDetails = data?.leaderBoardDetails ?: return
    textView.text = if (!leaderBoardDetails.lastUpdateInfo.isNullOrBlank()) {
        "Last Updated ${leaderBoardDetails.lastUpdateInfo}"
    } else {
        ""
    }
}


@BindingAdapter("app:basePointsToday")
fun setBasePointsToday(textView: TextView, data: GamesPointsDto?) {
    val trend = findTrend(data?.playerTodayTrend, "Base Points earned")
    textView.text = trend?.trendValue?.score ?: "0"
}

@BindingAdapter("app:othToday", "app:isDescription", requireAll = false)
fun setOTHToday(textView: TextView, data: GamesPointsDto?, isDescription: Boolean = false) {
    val trend = findTrend(data?.playerTodayTrend, "OTH")
    textView.text = if (isDescription) {
        trend?.trendValue?.description ?: "N/A"
    } else {
        trend?.trendValue?.percentage?.let { "$it%" } ?: "0%"
    }
}

@BindingAdapter("app:storeOTHToday", "app:isDescription", requireAll = false)
fun setStoreOTHToday(textView: TextView, data: GamesPointsDto?, isDescription: Boolean = false) {
    val trend = findTrend(data?.playerTodayTrend, "store OTH")
    textView.text = if (isDescription) {
        trend?.trendValue?.description ?: "N/A"
    } else {
        trend?.trendValue?.percentage?.let { "$it%" } ?: "0%"
    }
}

@BindingAdapter("app:getLabelForStat", "app:performanceType", requireAll = true)
fun bindPerformanceStatLabel(view: TextView, data: GamesPointsDto?, type: Int) {
    val keyValue = data?.playerPerformanceSummaryTillDate?.keyValue ?: return

    val (label, key) = when (type) {
        1 -> "Base" to "Base"
        2 -> "OTH" to "OTH"
        3 -> "Store OTH" to "Store OTH"
        else -> return
    }
    view.text = key
}

@BindingAdapter("app:getValueForStat", "app:performanceType", requireAll = true)
fun bindPerformanceStatValue(view: TextView, data: GamesPointsDto?, type: Int) {
    val keyValue = data?.playerPerformanceSummaryTillDate?.keyValue ?: return

    val (label, key) = when (type) {
        1 -> "Base" to "Base"
        2 -> "OTH" to "OTH"
        3 -> "Store OTH" to "Store OTH"
        else -> return
    }

    val value = keyValue[key] ?: "0"

    view.text = value
}

@BindingAdapter("app:totalPointsText")
fun setTotalPointsText(textView: TextView, data: GamesPointsDto?) {
    val total = data
        ?.playerPerformanceSummaryTillDate
        ?.keyValue
        ?.get("Your Earned Total")
        ?: "0"

    textView.text = "$total"
}

@BindingAdapter("app:todayAsDateRange")
fun setTodayAsDateRange(textView: TextView, show: Boolean) {
    if (!show) {
        textView.text = ""
        return
    }

    val today = Calendar.getInstance()
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val dayFormat = SimpleDateFormat("d", Locale.getDefault())

    val month = monthFormat.format(today.time)
    val day = dayFormat.format(today.time)

    textView.text = "$month $day - Today"
}


@BindingAdapter("app:handOffCompletedPoints")
fun setStatIconFromType(textView: TextView, data: GamesPointsDto?) {
    val basePoints = data?.playerBaseScoreDetails ?: return
    textView.text = if (!basePoints.baseScore.isNullOrBlank()) {
        "Base points: ${basePoints.baseScore}"
    } else {
        "Base points: 0"
    }
}

@BindingAdapter("app:handOffCompletedPointsValue")
fun setHandOffCompletedPointsValue(textView: TextView, data: GamesPointsDto?) {
    val handOff =
        data?.playerBaseScoreDetails?.baseScoreBreakdown?.get("HandOff Complete") ?: return
    if (handOff.score.isNullOrBlank()) {
        textView.text = ""
    } else {
        textView.text = handOff.score
    }
}


@BindingAdapter("app:handOffCompletedPointsDesc")
fun setHandOffCompletedPointsDesc(textView: TextView, data: GamesPointsDto?) {
    val handOff =
        data?.playerBaseScoreDetails?.baseScoreBreakdown?.get("HandOff Complete") ?: return
    if (handOff.score.isNullOrBlank()) {
        textView.text = ""
    } else {
        textView.text = handOff.description
    }
}

@BindingAdapter("app:authCodeCompletedPointsValue")
fun setAuthCodeCompletedPointsValue(textView: TextView, data: GamesPointsDto?) {
    val handOff =
        data?.playerBaseScoreDetails?.baseScoreBreakdown?.get("Auth Code Verified") ?: return
    if (handOff.score.isNullOrBlank()) {
        textView.text = ""
    } else {
        textView.text = handOff.score
    }
}


@BindingAdapter("app:authCodeCompletedPointsDesc")
fun setAuthCodeCompletedPointsDesc(textView: TextView, data: GamesPointsDto?) {
    val handOff =
        data?.playerBaseScoreDetails?.baseScoreBreakdown?.get("Auth Code Verified") ?: return
    if (handOff.score.isNullOrBlank()) {
        textView.text = ""
    } else {
        textView.text = handOff.description
    }
}

@BindingAdapter("app:collectedCompletedPointsValue")
fun setCollectedCCompletedPointsValue(textView: TextView, data: GamesPointsDto?) {
    val handOff =
        data?.playerBaseScoreDetails?.baseScoreBreakdown?.get("Collected All Items") ?: return
    if (handOff.score.isNullOrBlank()) {
        textView.text = ""
    } else {
        textView.text = handOff.score
    }
}


@BindingAdapter("app:collectedCompletedPointsDesc")
fun setCollectedCompletedPointsDesc(textView: TextView, data: GamesPointsDto?) {
    val handOff =
        data?.playerBaseScoreDetails?.baseScoreBreakdown?.get("Collected All Items") ?: return
    if (handOff.score.isNullOrBlank()) {
        textView.text = ""
    } else {
        textView.text = handOff.description
    }
}

@BindingAdapter("app:authCodeProgressData")
fun bindAuthCodeCompleteProgress(progressBar: ProgressBar, data: GamesPointsDto?) {
    val breakdown = data?.playerBaseScoreDetails?.baseScoreBreakdown ?: return

    val authScore = breakdown["Auth Code Verified"]?.score?.toIntOrNull() ?: 0
    val handOffScore = breakdown["HandOff Complete"]?.score?.toIntOrNull() ?: 0

    progressBar.max = handOffScore
    progressBar.progress = authScore
}


@BindingAdapter("app:collectAllItemsProgressData")
fun bindCollectAllItemsProgress(progressBar: ProgressBar, data: GamesPointsDto?) {
    val breakdown = data?.playerBaseScoreDetails?.baseScoreBreakdown ?: return

    val authScore = breakdown["Collected All Items"]?.score?.toIntOrNull() ?: 0
    val handOffScore = breakdown["HandOff Complete"]?.score?.toIntOrNull() ?: 0

    progressBar.max = handOffScore
    progressBar.progress = authScore
}


// Helper to find a trend by name safely
private fun findTrend(trends: List<PlayerTodayTrendDto>?, key: String): PlayerTodayTrendDto? {
    return trends?.find { it.trendName.equals(key, ignoreCase = true) }
}


class DailyScoreHolder(private val data: DailyOTHScoreUIModel) :
    BindableItem<AdapterDailyScoresBinding>() {

    override fun initializeViewBinding(view: View) = AdapterDailyScoresBinding.bind(view)
    override fun getLayout() = R.layout.adapter_daily_scores

    override fun bind(viewBinding: AdapterDailyScoresBinding, position: Int) = with(viewBinding) {

        viewBinding.labelName = data.date
        viewBinding.gamePercent = data.storeOTHPercentage + "%"
        viewBinding.gamePoints = data.storeOTHScore

        viewBinding.mcvBest.isVisible = data.isBest

        val color = if (data.isBest)
            ContextCompat.getColor(mcvHeading.context, R.color.gold_color)
        else
            ContextCompat.getColor(mcvHeading.context, R.color.transparent)

        mcvHeading.strokeColor = color
        mcvValue.strokeColor = color
    }
}


@BindingAdapter(
    value = ["setItems", "app:itemViewType", "app:setViewModel", "app:fragmentViewLifecycleOwner"],
    requireAll = false
)
fun RecyclerView.setDailyScoreAdapterStore(
    items: Map<String, StoreDailyOTHScoreDto>?, type: String?, vm: MyScoreViewModel?,
    fragmentViewLifecycleOwner: LifecycleOwner? = null,
) {
    if (vm == null || type == null) return

    val list = items?.toUIMappedList() ?: return
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    adapter =
        GroupieAdapter().apply {
            list.forEach {
                add(DailyScoreHolder(it))
            }
        }
}

@BindingAdapter(
    value = ["setItems", "app:itemViewType", "app:setViewModel", "app:fragmentViewLifecycleOwner"],
    requireAll = false
)
fun RecyclerView.setDailyScoreAdapterOTH(
    items: Map<String, PlayerDailyOTHScoreDto>?, type: String?, vm: MyScoreViewModel?,
    fragmentViewLifecycleOwner: LifecycleOwner? = null,
) {
    if (vm == null || type == null) return

    val list = items?.toUIModelListNew() ?: return
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    adapter =
        GroupieAdapter().apply {
            list.forEach {
                add(DailyScoreHolder(it))
            }
        }
}


class LeaderBoard(private val data: LeaderBoardPlayerDto) :
    BindableItem<AdapterLeaderBoardBinding>() {

    override fun initializeViewBinding(view: View) = AdapterLeaderBoardBinding.bind(view)
    override fun getLayout() = R.layout.adapter_leader_board

    override fun bind(viewBinding: AdapterLeaderBoardBinding, position: Int) = with(viewBinding) {

        tvInitial.setInitial(data.playerId ?: "")
        tvRank.text = data.rank.toString()
        tvPoints.text = "${data.totalPoints} pts"


        val color = if (data.playerId.equals("SMAR602", true))
            ContextCompat.getColor(mcvRoot.context, R.color.green)
        else
            ContextCompat.getColor(mcvRoot.context, R.color.score_card)

        mcvRoot.strokeColor = color

        if (data.playerId.equals("SMAR602", true)) {
            tvName.isVisible = true
            ivArrow.visibility = View.VISIBLE
            tvInitial.isVisible = false
            mcvRoot.setCardBackgroundColor(ContextCompat.getColor(mcvRoot.context, R.color.white))
            mcvRoot.strokeWidth = 2
        } else {
            tvName.isVisible = false
            ivArrow.visibility = View.INVISIBLE
            tvInitial.isVisible = true
            mcvRoot.strokeWidth = 2
            mcvRoot.setCardBackgroundColor(ContextCompat.getColor(mcvRoot.context, R.color.score_card))
        }
    }
}


@BindingAdapter(
    value = ["setItems", "app:itemViewType", "app:setViewModel", "app:fragmentViewLifecycleOwner"],
    requireAll = false
)
fun RecyclerView.setLeaderBoard(
    items: List<LeaderBoardPlayerDto>?, type: String?, vm: MyScoreViewModel?,
    fragmentViewLifecycleOwner: LifecycleOwner? = null,
) {
    if (vm == null) return

    val list = items?.sortedBy {
        it.rank ?: -1
    }
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

    adapter =
        GroupieAdapter().apply {
            list?.forEach {
                add(LeaderBoard(it))
            }
        }
}

