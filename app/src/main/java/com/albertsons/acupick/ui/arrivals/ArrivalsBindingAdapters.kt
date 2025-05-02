package com.albertsons.acupick.ui.arrivals

import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.VanStatus
import com.albertsons.acupick.data.model.response.FE_SCREEN_STATUS_STORE_NOTIFIED
import com.albertsons.acupick.ui.bindingadapters.startTimer
import com.albertsons.acupick.ui.bindingadapters.stopTimer
import com.albertsons.acupick.ui.dialog.DialogStyle
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.FulfillmentTypeUI
import com.albertsons.acupick.ui.models.OrderItemUI
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.lifeCycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@BindingAdapter("dialogBodyStyleType")
fun TextView.setDialogBodyStyleType(styleType: DialogStyle?) {
    when (styleType) {
        DialogStyle.PrintShippingLabel -> this.setTextAppearance(R.style.NunitoSansRegular18_Grey600)
        DialogStyle.OfAgeVerification -> this.setTextAppearance(R.style.NunitoSansBold16_verificationRed)
        DialogStyle.DriverIdVerification -> this.setTextAppearance(R.style.NunitoSansBold16_Blue)
        else -> Unit
    }
}

@BindingAdapter("dialogSecondaryBodyStyleType")
fun TextView.setDialogSecondaryBodyStyleType(styleType: DialogStyle?) {
    when (styleType) {
        DialogStyle.OfAgeVerification,
        DialogStyle.DriverIdVerification -> this.setTextAppearance(R.style.NunitoSansRegular16_Grey600)
        DialogStyle.RejectedItems -> this.visibility = View.GONE
        else -> Unit
    }
}

@BindingAdapter(value = ["android:text", "app:section_count"], requireAll = true)
fun TextView.setTextWithStatusCount(status: CustomerArrivalStatusUI?, count: Int) {
    text = when (status) {
        CustomerArrivalStatusUI.ARRIVED -> context.getString(R.string.customer_arrived_format, count)
        CustomerArrivalStatusUI.ARRIVING -> context.getString(R.string.customer_arriving_format, count)
        CustomerArrivalStatusUI.EN_ROUTE -> context.getString(R.string.customer_en_route_format, count)
        CustomerArrivalStatusUI.PICKUP_READY -> context.getString(R.string.customer_pickup_ready_format, count)
        else -> ""
    }
}

@BindingAdapter("app:setRegulatedStyling")
fun AppCompatTextView.setRegulatedStyling(hasRegulatedItems: Boolean) {
    typeface = resources.getFont(
        if (hasRegulatedItems) R.font.nunito_sans_bold else R.font.nunito_sans_semibold
    )
}

// TODO - Change to pass lambda, or named interface, as parameter and move to generic UI binding file
@BindingAdapter(value = ["app:setOnRefresh", "app:isRefreshComplete"], requireAll = false)
fun SwipeRefreshLayout.setOnRefresh(viewModel: ArrivalsViewModel, refreshComplete: Boolean = false) {
    isRefreshing = refreshComplete
    setOnRefreshListener {
        viewModel.loadDataEvent.postValue(true)
    }
}

@BindingAdapter(value = ["app:arrivalStatus", "app:item", "app:is1pl", "app:onStartTimer"])
fun AppCompatTextView.arrivalStatus(
    arrivalStatus: CustomerArrivalStatusUI?,
    item: OrderItemUI,
    is1pl: Boolean,
    onStartTimer: (Job) -> Unit
) {
    stopTimer()
    setTextColor(context.getColor(R.color.grey_700))
    if (is1pl) {
        textSize = 22f
        this.setTextAppearance(R.style.PoppinsMedium22)
        if (item.vanStatus == VanStatus.IN_PROGRESS) {
            this.startTimer(item.vanArrivalTime, onStartTimer)
        } else {
            text = item.formattedArrivalTime.orEmpty()
        }
        return
    }
    if (item.fulfillment == FulfillmentTypeUI.DUG && (arrivalStatus == CustomerArrivalStatusUI.ARRIVED || arrivalStatus == CustomerArrivalStatusUI.ARRIVED_NOT_STARTED) &&
        (item.feScreenStatus == FE_SCREEN_STATUS_STORE_NOTIFIED || item.feScreenStatus == null)
    ) {
        textSize = 22f
        this.setTextAppearance(R.style.PoppinsMedium22)
        this.startTimer(item.customerArrivalTime, onStartTimer)
        return
    }
    when (arrivalStatus) {
        CustomerArrivalStatusUI.EN_ROUTE, CustomerArrivalStatusUI.ARRIVING -> {
            textSize = 22f
            text = item.formattedArrivalTime.orEmpty()
            this.setTextAppearance(R.style.PoppinsMedium22)
        }
        CustomerArrivalStatusUI.PICKUP_READY, null -> {
            if (!item.isYesterdaysOrder) {
                textSize = 22f
                text = context.getString(R.string.pick_window_format, item.formattedWindowStartTime, item.formattedWindowEndTime)
                this.setTextAppearance(R.style.PoppinsMedium22)
            } else {
                text = item.formattedWindowStartTimeWithAMPM.orEmpty()
                this.setTextAppearance(R.style.PoppinsMedium22)
                textSize = 22f
            }
        }
        else -> {
            if (item.fulfillment == FulfillmentTypeUI.THREEPL) {
                textSize = 22f
                this.setTextAppearance(R.style.PoppinsMedium22)
                this.startTimer(item.customerArrivalTime, onStartTimer)
            }
        }
    }
}

@BindingAdapter(value = ["app:item", "app:inProgess", "app:firstNotificationETATime", "app:secondNotificationETATime", "is1pl", "app:onStartTimer"])
fun AppCompatTextView.arrivalIn(
    item: OrderItemUI,
    inProgress: Boolean?,
    firstNotificationETATime: Int,
    secondNotificationETATime: Int,
    is1pl: Boolean,
    onStartTimer: (Job) -> Unit
) {
    if (inProgress == true || is1pl) {
        visibility = View.GONE
        return
    }
    visibility = View.GONE

    this.setTextAppearance(R.style.RoundedPill_LightAqua)
    when (item.customerArrivalStatus) {
        CustomerArrivalStatusUI.ARRIVED,
        CustomerArrivalStatusUI.ARRIVED_NOT_STARTED,
        -> {
            text = context.getString(R.string.arrival_status_arrived)
            background.setTint(ContextCompat.getColor(context, R.color.picklist_stageByTime_pastDue))
            visibility = View.VISIBLE
        }
        CustomerArrivalStatusUI.EN_ROUTE -> {
            item.customerArrivalTime?.let {
                startTimer(item, firstNotificationETATime, secondNotificationETATime, onStartTimer)
            } ?: run {
                text = context.getString(R.string.arrival_status_on_their_way)
                background.setTint(ContextCompat.getColor(context, R.color.semiLightGreen))
                visibility = View.VISIBLE
            }
        }
        CustomerArrivalStatusUI.ARRIVING -> startTimer(item, firstNotificationETATime, secondNotificationETATime, onStartTimer)
        else -> if (item.isYesterdaysOrder) {
            text = context.getString(R.string.yesterdays_order)
            this.setTextAppearance(R.style.NunitoSansRegular14)
            textSize = 14f
            background.setTint(ContextCompat.getColor(context, android.R.color.transparent))
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }
}

fun TextView.startTimer(item: OrderItemUI, firstNotificationETATime: Int, secondNotificationETATime: Int, onStartTimer: (Job) -> Unit) {
    item.customerArrivalTime?.let {
        stopTimer()
        if (ChronoUnit.MINUTES.between(ZonedDateTime.now(), it) <= 0) {
            text = context.getString(R.string.arriving_soon)
            background.setTint(ContextCompat.getColor(context, R.color.lightYellow))
            visibility = View.VISIBLE
        } else {
            tag = context.lifeCycleScope?.launch {
                flow {
                    do {
                        emit(ChronoUnit.MINUTES.between(ZonedDateTime.now(), it))
                        delay(1000)
                    } while (coroutineContext.isActive)
                }
                    .distinctUntilChanged()
                    .takeWhile { it > 0 }
                    .map {
                        val text = if (it > firstNotificationETATime) StringIdHelper.Id(R.string.arrival_status_on_their_way)
                        else if (it <= 1L) StringIdHelper.Id(R.string.arriving_soon)
                        else StringIdHelper.Format(R.string.arriving_in_min, it.toString())
                        // TODO: Confirm this logic
                        val bgColor = if (it > secondNotificationETATime) R.color.semiLightGreen else R.color.lightYellow
                        text to bgColor
                    }.collect {
                        text = it.first.getString(context)
                        background.setTint(ContextCompat.getColor(context, it.second))
                        visibility = View.VISIBLE
                    }
            }
            this.tag?.let { tag -> onStartTimer.invoke(tag as Job) }
        }
    } ?: run {
        text = context.getString(R.string.arriving_soon)
        background.setTint(ContextCompat.getColor(context, R.color.lightYellow))
        visibility = View.VISIBLE
    }
}

@BindingAdapter(value = ["app:partnerName", "app:fulfillmentResId", "app:is1pl"])
fun AppCompatTextView.setPartnerNameFullFillmentType(source: String?, fulfillmentResId: Int?, is1pl: Boolean) {
    text = if (is1pl)
        context.getString(R.string.one_pl_van_number, source)
    else
        source ?: fulfillmentResId?.let(context::getString)
}

@BindingAdapter(value = ["app:isSelectedCheckBox", "app:isDisabled"])
fun ImageView.setCheckBoxResource(isSelectedCheckBox: Boolean, isDisabled: Boolean) {
    val resource = if (isSelectedCheckBox) R.drawable.ic_checkbox_checked else if (isDisabled) R.drawable.ic_checkbox_disabled else R.drawable.ic_checkbox_unchecked
    setImageDrawable(AppCompatResources.getDrawable(context, resource))
}

@BindingAdapter("app:ellipsisColor")
fun ImageView.showEllipsisIcon(isSelected: Boolean) {
    setColorFilter(ContextCompat.getColor(context, if (isSelected) R.color.grey_550 else R.color.semiLightBlue), PorterDuff.Mode.SRC_IN)
}

@BindingAdapter("app:isInProgress", "app:showNoOrdersReadyUi", "app:showNoOrdersReadyInProgressUi")
fun View.showEmptyState(isInProgress: Boolean?, showNoOrdersReadyUi: Boolean?, showNoOrdersReadyInProgressUi: Boolean?) {
    visibility = if ((isInProgress == true && showNoOrdersReadyInProgressUi == true) || (isInProgress == false && showNoOrdersReadyUi == true)) View.VISIBLE else View.GONE
}
