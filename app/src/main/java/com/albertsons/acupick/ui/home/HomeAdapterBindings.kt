package com.albertsons.acupick.ui.home

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.albertsons.acupick.R
import com.albertsons.acupick.infrastructure.utils.AM_PM_TIME_FORMATTER
import com.albertsons.acupick.infrastructure.utils.HOUR_MINUTE_TIME_FORMATTER
import com.albertsons.acupick.infrastructure.utils.formattedWith
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.ui.bindingadapters.setVisibilityGoneIfTrue
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.FulfillmentTypeUI
import com.albertsons.acupick.ui.util.getOrEmpty
import com.albertsons.acupick.ui.util.setTopMargin
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@BindingAdapter("app:setOnRefresh", "app:isRefreshComplete", "app:loadingState")
fun SwipeRefreshLayout.setOnRefresh(viewModel: HomeViewModel, refreshComplete: Boolean = false, loadingState: HomeLoadingState? = null) {
    isRefreshing = refreshComplete
    isEnabled = loadingState == HomeLoadingState.End
    if (isEnabled) {
        setOnRefreshListener {
            viewModel.load(isRefresh = true, isSwipeRefresh = true)
        }
    }
}

@BindingAdapter(value = ["app:customerArrivalStatus", "app:customerArrivalTime", "app:shouldShowPastDue", "app:timerStillActive", "app:dueDayLabel", "app:is1Pl"])
fun AppCompatTextView.setPastDueVisible(
    customerArrivalStatusUI: CustomerArrivalStatusUI?,
    customerArrivalTime: Long,
    isFlash: Boolean,
    isTimerActive: Boolean,
    dueDay: Long = 0L,
    is1Pl: Boolean?
) {
    text = context.getString(
        when {
            is1Pl == true -> R.string.remove_unwanted_items_by
            customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED || customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED_NOT_STARTED -> {
                R.string.home_arrived_and_waiting
            }
            customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVING || customerArrivalStatusUI == CustomerArrivalStatusUI.EN_ROUTE -> {
                if (customerArrivalTime <= 0)
                    R.string.home_on_their_way
                else
                    R.string.home_arriving_in
            }
            customerArrivalStatusUI == CustomerArrivalStatusUI.PICKUP_READY -> {
                R.string.pickup_ready_home
            }
            !isTimerActive && isFlash -> {
                R.string.pastDue
            }
            isFlash -> {
                R.string.stage_in
            }
            else -> {
                when {
                    dueDay == 1L -> R.string.due
                    dueDay > 1L -> R.string.due_in
                    else -> R.string.stage_by
                }
            }
        }
    )
}

@BindingAdapter(value = ["app:setFullFillmentType", "app:source", "app:isBatchOrder"])
fun AppCompatTextView.setFullFillmentType(fullFillmentTypes: Set<FulfillmentTypeUI>?, source: String?, isBatchOrder: Boolean?) {
    fullFillmentTypes?.let {
        val fullFillmentText = if (source.isNotNullOrBlank()) source
        else when {
            isBatchOrder == true -> ""
            fullFillmentTypes.contains(FulfillmentTypeUI.DUG) -> context.getString(R.string.fulfillment_dug)
            fullFillmentTypes.contains(FulfillmentTypeUI.ONEPL) -> context.getString(R.string.fulfillment_one_pl)
            fullFillmentTypes.contains(FulfillmentTypeUI.THREEPL) -> context.getString(R.string.fulfillment_three_pl)
            else -> ""
        }
        text = fullFillmentText
    }
}

@BindingAdapter(value = ["app:isReProcess", "app:isPrepNeeded", "app:isPrePick"])
fun AppCompatTextView.setRedBadgeText(isReProcess: Boolean?, isPrepNeeded: Boolean?, isPrePick: Boolean) {
    text = when {
        isReProcess == true -> context.getString(R.string.reshop)
        isPrepNeeded == true -> context.getString(R.string.prep_not_ready)
        isPrePick == true -> context.getString(R.string.pre_pick)
        else -> null
    }
    visibility = when {
        isReProcess == true || isPrepNeeded == true || isPrePick == true -> View.VISIBLE
        else -> View.GONE
    }

    if (isPrePick == true) {
        this.background.setTint(context.getColor(R.color.lightPurple))
    }
}

@BindingAdapter(value = ["app:isOrderReadyToPickUp", "app:orderNumber"])
fun AppCompatTextView.setOrderNumber(isOrderReadyToPickUp: Boolean, orderNumber: String?) {
    text = context.getString(R.string.order_number_value, orderNumber)
    val isShow = isOrderReadyToPickUp && !orderNumber.isNullOrEmpty()
    setVisibilityGoneIfTrue(!isShow)
}

/** If the current store is a micro fullfillment center, use the word 'totes' instead of 'bags' */
@BindingAdapter("app:setItems")
fun AppCompatTextView.setItems(homeCardData: HomeCardData?) {
    text = if (homeCardData?.is1Pl == true) {
        if (homeCardData.rejectedItemsCount != null) context.resources.getString(R.string.number_unwanted_items, homeCardData.rejectedItemsCount.getOrEmpty()) else null
    } else if (homeCardData?.isOrderReadyToPickUp == false) {
        context.resources.getQuantityString(R.plurals.cap_items_plural, homeCardData.itemQty?.toInt() ?: 0, homeCardData.itemQty?.toInt() ?: 0)
    } else {
        null
    }
}

@BindingAdapter("app:customerNameOrOrderCount", "app:isBatchOrder", "app:formattedOrderCountText")
fun AppCompatTextView.setcustomerNameOrOrderCount(cardData: HomeCardData?, isBatchOrder: Boolean?, count: Int?) {
    text = if (isBatchOrder == true && cardData?.isOrderReadyToPickUp == false) {
        context.resources.getQuantityString(R.plurals.number_of_order_plural, count ?: 1, count ?: 1)
    } else {
        cardData?.contactName.orEmpty()
    }
}

@BindingAdapter(value = ["app:isOrderReadyToPickUp", "app:isActive"])
fun AppCompatImageView.setZoneVisibility(isOrderReadyToPickUp: Boolean, isActive: Boolean) {
    visibility = when {
        !isOrderReadyToPickUp && isActive -> View.VISIBLE
        else -> View.GONE
    }
}

@BindingAdapter("app:isOrderReadyToPickUp", "app:pickingButtonText")
fun AppCompatTextView.setPickingButtonText(isOrderReadyToPickUp: Boolean, pickingButtonText: String?) {
    text = if (isOrderReadyToPickUp) {
        context.getString(R.string.home_begin_handoff)
    } else {
        pickingButtonText
    }
}

@BindingAdapter("app:imageSrc")
fun AppCompatImageView.setPickListIllustration(illustration: Int) {
    setImageResource(illustration)
}

@BindingAdapter("app:cardData", "app:setHandoffDuration", "app:shouldShowPastDue", "app:hideTimer", "app:timerStillActive", "app:is1pl", "app:hidePastDue")
fun AppCompatTextView.setFormattedDate(
    homeCardData: HomeCardData?,
    durationSeconds: Long,
    isFlash: Boolean,
    hideTimer: Boolean,
    timerActive: Boolean,
    is1pl: Boolean?,
    hidePastDue: Boolean?
) {
    if (is1pl == true) {
        if (hidePastDue == false) {
            text = processHomeScreenTimerText(context, "0m 0s")
            setTextColor(ContextCompat.getColor(context, R.color.semiDarkRed))
        } else {
            text = homeCardData?.vanDepartureTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(HOUR_MINUTE_TIME_FORMATTER)
            setTextColor(ContextCompat.getColor(context, R.color.grey_700))
        }
        textSize = 42.0f
        setTopMargin(-20)
    } else if (homeCardData?.isOrderReadyToPickUp == true) {
        if ((durationSeconds == HomeViewModel.ARRIVING_SOON_WAIT_TIME_PLACEHOLDER || durationSeconds == 0L) && homeCardData.customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVING) {
            text = context.getString(R.string.arriving_soon)
            textSize = 22.0f
            setTopMargin(0)
        } else {
            text = if (durationSeconds == 0L) null
            else processHomeScreenTimerText(context, context.getString(R.string.timer_format, durationSeconds.div(60), durationSeconds.rem(60)))
            textSize = 42.0f
            setTopMargin(-20)
        }
        if (homeCardData.customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED || homeCardData.customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED_NOT_STARTED) {
            setTextColor(
                ContextCompat.getColor(
                    context,
                    when {
                        durationSeconds < 120 -> R.color.grey_700
                        durationSeconds < 300 -> R.color.semiLightOrange
                        else -> R.color.semiDarkRed
                    }
                )
            )
        } else if (homeCardData.customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVING) {
            setTextColor(
                ContextCompat.getColor(
                    context,
                    when {
                        durationSeconds > 120 || durationSeconds == HomeViewModel.ARRIVING_SOON_WAIT_TIME_PLACEHOLDER || durationSeconds == 0L -> R.color.grey_700
                        else -> R.color.semiLightOrange
                    }
                )
            )
        }
    } else {
        text = homeCardData?.expectedEndTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(HOUR_MINUTE_TIME_FORMATTER)
        setTextColor(
            ContextCompat.getColor(
                context,
                if (isFlash) R.color.semiDarkRed else R.color.black
            )
        )
    }
    // setVisibilityGoneIfTrue(timerActive || hideTimer || durationSeconds == HomeViewModel.ARRIVING_SOON_WAIT_TIME_PLACEHOLDER)
    setVisibilityGoneIfTrue(timerActive || hideTimer)
}

@BindingAdapter("app:waitTimeSeconds", "app:hideTimer", "app:timerStillActive")
fun AppCompatTextView.setArrivingSoon(waitTimeSeconds: Long, hideTimer: Boolean, timerActive: Boolean) {
    if (waitTimeSeconds == HomeViewModel.ARRIVING_SOON_WAIT_TIME_PLACEHOLDER) {
        visibility = if (timerActive || !hideTimer) {
            View.GONE
        } else {
            View.VISIBLE
        }
        // setVisibilityGoneIfTrue(timerActive || hideTimer)
    } else visibility = View.GONE
}

@BindingAdapter("app:cardDataAmPm", "app:setFormattedDateAmPm", "app:showAmPm", "app:shouldShowPastDue", "app:hideTimer", "app:hidePastDue", "app:isOnePl")
fun AppCompatTextView.setFormattedDateAmPm(
    isOrderReadyToPickUp: Boolean,
    zonedDateTime: ZonedDateTime?,
    timerActive: Boolean?,
    isFlash: Boolean,
    hideTimer: Boolean,
    hidePastDue: Boolean,
    isOnePl: Boolean?
) {
    if (isOnePl == true) {
        text = zonedDateTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(AM_PM_TIME_FORMATTER)
        setVisibilityGoneIfTrue(!hidePastDue)
        return
    }
    if (isFlash) {
        text = zonedDateTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(AM_PM_TIME_FORMATTER)?.lowercase()
        setTextColor(
            ContextCompat.getColor(
                context,
                R.color.semiDarkRed
            )
        )
    } else {
        text = zonedDateTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(AM_PM_TIME_FORMATTER)
        setTextColor(
            ContextCompat.getColor(
                context,
                R.color.black
            )
        )
    }
    setVisibilityGoneIfTrue(hideTimer || timerActive == true || isOrderReadyToPickUp)
}

@BindingAdapter(value = ["app:flashStageDurationMs", "app:timerColor"])
fun AppCompatTextView.setCountDownDuration(countdownDurationMs: Long, timerColor: Int) {
    val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(countdownDurationMs)
    text = context.getString(R.string.timer_format, durationSeconds.div(60), durationSeconds.rem(60))
    setTextColor(
        ContextCompat.getColor(
            context,
            timerColor
        )
    )
}

@BindingAdapter(value = ["app:isActiveCustomerTypeSubIcon"])
fun AppCompatImageView.setCustomerTypSubIconVisibility(isActive: Boolean) {
    visibility = when {
        isActive -> View.VISIBLE
        else -> View.GONE
    }
}

fun processHomeScreenTimerText(context: Context, time: String): CharSequence = with(StringBuilder()) {
    mutableListOf<SpannableString>().let { spannables ->
        val digitTextSize = context.resources.getDimensionPixelSize(R.dimen.home_screen_timer_digit_text)
        val charTextSize = context.resources.getDimensionPixelSize(R.dimen.home_screen_timer_char_text)
        time.forEach {
            if (it.toString().isDigitsOnly()) append(it)
            else {
                if (isNotEmpty()) {
                    spannables.add(
                        SpannableString(toString()).apply {
                            setSpan(AbsoluteSizeSpan(digitTextSize), 0, length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                        }
                    )
                    clear()
                }
                spannables.add(
                    SpannableString(it.toString()).apply {
                        setSpan(AbsoluteSizeSpan(charTextSize), 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    }
                )
            }
        }
        TextUtils.concat(*spannables.toTypedArray())
    }
}
