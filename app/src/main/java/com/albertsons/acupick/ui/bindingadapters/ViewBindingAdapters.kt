package com.albertsons.acupick.ui.bindingadapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface.BOLD
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.BindingAdapter
import androidx.viewpager2.widget.ViewPager2
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.BoxType
import com.albertsons.acupick.data.model.CustomerType
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.arrivals.complete.HandOffUI
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.handoff.stepsprogress.ProgressViewConfig
import com.albertsons.acupick.ui.handoff.stepsprogress.StepProgressBarView
import com.albertsons.acupick.ui.handoff.stepsprogress.setUp
import com.albertsons.acupick.ui.models.QuantityParams
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.picklistitems.PickListType
import com.albertsons.acupick.ui.util.BounceInterpolator
import com.albertsons.acupick.ui.util.lifeCycleScope
import com.albertsons.acupick.ui.util.lifecycleOwner
import com.albertsons.acupick.ui.util.orFalse
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Basically [View.GONE] when [value] is false. If true, set to [View.VISIBLE]. */
@BindingAdapter(value = ["visibilityGoneIfFalse", "visibilityGoneIfFalseMaster"], requireAll = false)
fun View.setVisibilityGoneIfFalse(value: Boolean, masterValue: Boolean? = false) {
    visibility = if (!value || masterValue == false) View.GONE else View.VISIBLE
}

/** Basically [View.GONE] when [value] is true. If false, set to [View.VISIBLE]. */
@BindingAdapter("visibilityGoneIfTrue")
fun View.setVisibilityGoneIfTrue(value: Boolean) {
    visibility = if (value) View.GONE else View.VISIBLE
}

/** Basically [View.GONE] when [value] is null. If present (not-null), set to [View.VISIBLE]. Opposite effect of [setVisibilityVisibleIfNull] */
@BindingAdapter("visibilityGoneIfNull")
fun View.setVisibilityGoneIfNull(value: Any?) {
    visibility = if (value == null) View.GONE else View.VISIBLE
}

/** Basically [View.GONE] when [value] is not null. If present (null), set to [View.VISIBLE]. Opposite effect of [setVisibilityVisibleIfNull] */
@BindingAdapter("visibilityGoneIfNotNull")
fun View.setVisibilityGoneIfNotNull(value: Any?) {
    visibility = if (value != null) View.GONE else View.VISIBLE
}

/** Basically [View.INVISIBLE] when [value] is false. If true, set to [View.VISIBLE]. */
@BindingAdapter("visibilityInvisibleIfFalse")
fun View.setVisibilityInvisibleIfFalse(value: Boolean) {
    visibility = if (!value) View.INVISIBLE else View.VISIBLE
}

/** Basically [View.INVISIBLE] when [value] is true. If false, set to [View.VISIBLE]. */
@BindingAdapter("visibilityInvisibleIfTrue")
fun View.setVisibilityInvisibleIfTrue(value: Boolean) {
    visibility = if (value) View.INVISIBLE else View.VISIBLE
}

/** Basically [View.INVISIBLE] when [value] is true. If false, set to [View.VISIBLE]. */
@BindingAdapter("visibilityGoneIfTrueOrNull")
fun View.setVisibilityGoneIfTrueOrNull(value: Boolean?) {
    visibility = if (value == null || value) View.GONE else View.VISIBLE
}

/** Basically [View.VISIBLE] when [value] is null. If present (not-null), set to [View.GONE]. Opposite effect of [setVisibilityGoneIfNull] */
@BindingAdapter("visibilityVisibleIfNull")
fun View.setVisibilityVisibleIfNull(value: Any?) {
    visibility = if (value == null) View.VISIBLE else View.GONE
}

/** Basically [View.GONE] when the String [value] `isNullOrEmpty` is true. */
@BindingAdapter("visibilityGoneIfNullOrEmpty")
fun View.setVisibilityGoneIfNullOrEmpty(value: String?) {
    visibility = if (value.isNullOrEmpty()) View.GONE else View.VISIBLE
}

/** Basically [View.GONE] when the String [value] `isNullOrEmpty` is true. */
@BindingAdapter("visibilityGoneIfNullOrBlank")
fun View.setVisibilityGoneIfNullOrBlank(value: String?) {
    visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
}

/** Basically [View.INVISIBLE] when the String [value] `isNullOrEmpty` is true. */
@BindingAdapter("visibilityInvisibleIfNullOrEmpty")
fun View.setVisibilityInvisibleIfNullOrEmpty(value: String?) {
    visibility = if (value.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
}

/** Basically [View.GONE] when [value] is null or less than 1 (one). If present (not-null) and above 1, set to [View.VISIBLE]. */
@BindingAdapter("visibilityGoneIfNullOrLessThanOne")
fun View.setVisibilityGoneIfZeroOrNull(value: Int?) {
    visibility = if (value == null || value <= 0) View.GONE else View.VISIBLE
}

@BindingAdapter(value = ["startTimer", "onStartTimer"])
fun TextView.startTimer(startTime: ZonedDateTime? = null, onStartTimer: (Job) -> Unit) {
    if (startTime == null) return
    stopTimer()
    tag = context.lifeCycleScope?.launch {
        flow {
            do {
                emit(ChronoUnit.SECONDS.between(startTime, ZonedDateTime.now()))
                delay(1000)
            } while (coroutineContext.isActive)
        }.collect {
            text = context.getString(R.string.timer_format, it.div(60), it.rem(60))
            setTextColor(
                context.getColor(
                    when {
                        it < 120 -> R.color.darkBlue
                        it < 240 -> R.color.darkestOrange
                        else -> R.color.error
                    }
                )
            )
        }
    }
    onStartTimer.invoke(tag as Job)
}

// Usage only for DUG2.0 Arrival interjection dialog
@BindingAdapter(value = ["startTimerDugArrivalInterjection", "app:isInterjectionForAllUser"], requireAll = false)
fun TextView.startTimerDugArrivalInterjection(startTime: ZonedDateTime? = null, isInterjectionForAllUser: Boolean = false) {
    if (startTime == null) return
    stopTimer()
    tag = context.lifeCycleScope?.launch {
        flow {
            do {
                emit(ChronoUnit.SECONDS.between(startTime, ZonedDateTime.now()))
                delay(1000)
            } while (coroutineContext.isActive)
        }.collect {
            text = if (isInterjectionForAllUser) {
                context.getString(R.string.timer_format, it.div(60), it.rem(60))
            } else {
                context.getString(R.string.dug_arrival_interjection_timer_format, it.div(60), it.rem(60))
            }
        }
    }
}

fun TextView.stopTimer() {
    (tag as? Job)?.cancel()
}

// TODO -Look into making this generic so it can handle all payload types
//   May have issues with binding adapters as they don't like generics much.
@BindingAdapter(value = ["anchorSnackbar", "isNotAnchored", "app:clearEvent"], requireAll = false)
fun View.anchorSnackbarEvent(event: SnackBarEvent<Long>?, isNotAnchored: Boolean, clearEvent: ((SnackBarEvent<Long>?) -> Unit)? = null) {
    if (event == null) return

    try {
        val anchorView = if (isNotAnchored) null else this
        val snackbar = Snackbar.make(
            this,
            context?.let { it -> event.prompt?.getString(it) } ?: "",
            if (event.isIndefinite) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG
        )
            .setMaxInlineActionWidth(1)
            .setAnchorView(anchorView).apply {
                event.cta?.let { cta ->
                    setAction(cta.getString(context)) {
                        event.action?.invoke(event.payload)
                    }
                }
            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, e1: Int) {
                    super.onDismissed(transientBottomBar, e1)
                    clearEvent?.invoke(event)
                }
            })

        snackbar.view.findViewById<TextView>(R.id.snackbar_action).isAllCaps = false
        if (event.isError) {
            snackbar.setBackgroundTint(ContextCompat.getColor(this.context, R.color.snackbarError))
        }
        if (event.isSuccess) {
            snackbar.setBackgroundTint(ContextCompat.getColor(this.context, R.color.statusGreen))
        }

        event.pendingStartJob = context.lifeCycleScope?.launch {
            delay(event.startDelayMs)
            snackbar.show()
        }

        event.callback?.let {
            snackbar.addCallback(it)
        }

        event.dismissLiveEvent?.let { liveEvent ->
            this.lifecycleOwner()?.let { lifeCycleOwner ->
                liveEvent.observe(lifeCycleOwner) { snackbar.dismiss() }
            }
        }
    } catch (e: Exception) {
        // No op
        // TODO - Look into using global scope to inject acuPickLogger here
    }
}

@BindingAdapter("layoutMarginStart")
fun setLayoutMarginStart(view: View, dimen: Float) {
    val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.marginStart = dimen.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("formattedOrderCountText")
fun TextView.setOrderCountText(count: Int?) {
    this.text = context.resources.getQuantityString(R.plurals.number_of_order_plural, count ?: 1, count ?: 1)
}

@BindingAdapter("formattedSubCountText")
fun TextView.setSubCountText(count: Int?) {
    this.text = context.resources.getQuantityString(R.plurals.substituted_amount_format, count ?: 1, count ?: 1)
}

@BindingAdapter(value = ["notScannedCount", "isMfcSite", "bagBypassEnabled", "hasLooseItem", "isCustomerBagPreference"], requireAll = true)
fun TextView.setNotScannedText(notScannedCount: Int, isMfcSite: Boolean, bagBypassEnabled: Boolean, hasLooseItem: Boolean, isCustomerBagPreference: Boolean) {
    this.text = resources.getString(
        when {
            bagBypassEnabled && !isMfcSite && isCustomerBagPreference -> R.string.bag_scan_skipped
            bagBypassEnabled && !isCustomerBagPreference && hasLooseItem && !isMfcSite -> R.string.tote_loose_item_scan_skipped
            else -> R.string.tote_scan_skipped
        },
        notScannedCount
    )
}

@BindingAdapter(value = ["missingBagLabelMFC"])
fun TextView.missingBagLabelText(isMfcSite: Boolean) {
    this.text = resources.getString(
        if (isMfcSite) R.string.bag_bypass_missing_tote_label_title else
            R.string.bag_bypass_missing_bag_label_title
    )
}

@BindingAdapter(value = ["missingBagMFC"])
fun TextView.missingBagText(isMfcSite: Boolean) {
    this.text = resources.getString(
        if (isMfcSite) R.string.bag_bypass_missing_tote_title else
            R.string.bag_bypass_missing_bag_title
    )
}

@BindingAdapter("quantityHeaderText")
fun TextView.setQuantityHeader(params: QuantityParams?) {
    text = when {
        params?.isWeighted == true -> context.getString(R.string.item_detail_weighted_quantity_header)
        params?.isEaches == true -> context.getString(R.string.item_detail_eaches_quantity_header)
        params?.isTotaled == true -> context.getString(R.string.item_detail_totaled_quantity_header)
        else -> ""
    }
}

@BindingAdapter("app:animateItemCompleteImage")
fun AppCompatImageView.animateItemCompleteImage(bottomSheetType: BottomSheetType) {
    if (bottomSheetType == BottomSheetType.ItemComplete) {
        val anim = AnimationUtils.loadAnimation(this.context, R.anim.fade_in)
        val interpolator = LinearInterpolator()
        anim.interpolator = interpolator
        this.startAnimation(anim)
    }
}

@BindingAdapter("highlightImportantText")
fun TextView.highlightImportantText(string: String) {
    text = SpannableString(string).apply {
        val endIndex = string.indexOf("!") + 1
        setSpan(
            ForegroundColorSpan(context.getColor(R.color.darkestOrange)),
            0, endIndex, 0
        )
        setSpan(StyleSpan(BOLD), 0, endIndex, 0)
    }
}

@BindingAdapter("bulletFormattedText")
fun TextView.setBulletFormattedText(string: String) {
    text = SpannableString(string).apply {
        setSpan(BulletSpan(20), 0, string.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

@BindingAdapter("app:setIconByExpanded")
fun MaterialButton.setIconByExpanded(isExpanded: Boolean?) {
    setIconResource(
        if (isExpanded == true) {
            R.drawable.ic_collapse
        } else {
            R.drawable.ic_expand
        }
    )
}

@BindingAdapter(value = ["app:setCustomerTypeSubIcon", "app:setSubCattEnabled"], requireAll = false)
fun AppCompatImageView.setCustomerTypeSubIcon(customerType: CustomerType?, cattEnabled: Boolean?) {
    if (customerType == CustomerType.SNAP || customerType == CustomerType.BOTH) {
        setImageResource(R.drawable.ic_ebt_sub)
    }
}

@BindingAdapter("setValidIdBySelected")
fun AppCompatImageView.setValidIdBySelected(isSelected: Boolean?) {
    setImageResource(
        if (isSelected == true) {
            R.drawable.ic_id_valid_selected
        } else {
            R.drawable.ic_id_valid_unselected
        }
    )
}

@BindingAdapter("setInvalidIdBySelected")
fun AppCompatImageView.setInvalidIdBySelected(isSelected: Boolean?) {
    setImageResource(
        if (isSelected == true) {
            R.drawable.ic_id_invalid_selected
        } else {
            R.drawable.ic_id_invalid_unselected
        }
    )
}

@BindingAdapter("app:lockViewPager")
fun ViewPager2.lockViewPager(lock: Boolean) {
    this.isUserInputEnabled = !lock
}

@BindingAdapter("app:hideOrShowFab")
fun FloatingActionButton.animateInAndOut(show: Boolean) {
    val anim = AnimationUtils.loadAnimation(this.context, if (show) R.anim.show_fab else R.anim.hide_fab)
    anim.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationEnd(animation: Animation?) {
            visibility = if (show) View.VISIBLE else View.INVISIBLE
        }

        override fun onAnimationRepeat(animation: Animation?) {}
    })
    val interpolator = BounceInterpolator(0.2, 10.0)
    if (show) anim.interpolator = interpolator
    this.startAnimation(anim)
}

@BindingAdapter("app:layoutByTabCount")
fun TabLayout.setLayoutByTabCount(tabCount: Int) {
    this.tabMode = when (tabCount) {
        2 -> TabLayout.MODE_FIXED
        else -> TabLayout.MODE_AUTO
    }
}

@BindingAdapter("customHeight")
fun FrameLayout.setCustomHeight(isDugOrder: Boolean?) {
    isDugOrder?.let {
        this.layoutParams = this.layoutParams.apply {
            this.height = if (it) ViewGroup.LayoutParams.WRAP_CONTENT else resources?.getDimension(R.dimen.id_view_height)!!.toInt()
        }
    }
}

@BindingAdapter("disableRadioButton")
fun AppCompatRadioButton.disableRadioButton(isWineShipping: Boolean?) {
    if (!isWineShipping.orFalse()) {
        this.isEnabled = false
        this.buttonTintList = ContextCompat.getColorStateList(context, R.color.disabledBlue)
        this.setTextColor(ContextCompat.getColorStateList(context, R.color.disabledBlue))
    }
}

@BindingAdapter("app:missingItems")
fun RadioGroup.missingItems(bagLabels: List<String>?) {
    bagLabels?.forEach { label ->
        val radioButton = AppCompatRadioButton(this.context).apply {
            id = View.generateViewId()
            text = label
            textSize = 16f
            val checkedColor = ContextCompat.getColor(context, R.color.cattBlue)
            val uncheckedColor = ContextCompat.getColor(context, R.color.grey_550)
            buttonTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(checkedColor, uncheckedColor)
            )
            val copyTypeface = resources.getFont(R.font.nunito_sans_regular)
            setTextColor(ContextCompat.getColorStateList(this.context, R.color.grey_700))
            typeface = copyTypeface
        }
        this.addView(radioButton)
    }
}

@BindingAdapter("app:boxSize")
fun TextView.setBoxSize(boxSize: BoxType) {
    text = when (boxSize) {
        BoxType.XS -> context.resources.getString(R.string.wine_box_size_xs)
        BoxType.SS -> context.resources.getString(R.string.wine_box_size_ss)
        BoxType.MM -> context.resources.getString(R.string.wine_box_size_mm)
        BoxType.LL -> context.resources.getString(R.string.wine_box_size_ll)
        BoxType.XL -> context.resources.getString(R.string.wine_box_size_xl)
    }
}

@BindingAdapter("app:boxCapacity")
fun TextView.setBoxCapacity(boxSize: BoxType) {
    text = when (boxSize) {
        BoxType.XS -> context.resources.getString(R.string.wine_box_count_xs)
        BoxType.SS -> context.resources.getString(R.string.wine_box_count_ss)
        BoxType.MM -> context.resources.getString(R.string.wine_box_count_mm)
        BoxType.LL -> context.resources.getString(R.string.wine_box_count_ll)
        BoxType.XL -> context.resources.getString(R.string.wine_box_count_xl)
    }
}

@BindingAdapter("app:boxSizeIcon")
fun AppCompatImageView.setBoxSizeIcon(boxSize: BoxType) {
    setImageDrawable(
        when (boxSize) {
            BoxType.XS -> ContextCompat.getDrawable(context, R.drawable.ic_box_xs)
            BoxType.SS -> ContextCompat.getDrawable(context, R.drawable.ic_box_ss)
            BoxType.MM -> ContextCompat.getDrawable(context, R.drawable.ic_box_mm)
            BoxType.LL -> ContextCompat.getDrawable(context, R.drawable.ic_box_ll)
            BoxType.XL -> ContextCompat.getDrawable(context, R.drawable.ic_box_xl)
        }
    )
}

@BindingAdapter(value = ["processedQty", "totalQty"], requireAll = true)
fun AppCompatTextView.setProcessedQty(processedQty: String, totalQty: String) {
    text = context.getString(R.string.purchasedQty_of_totalQty, processedQty, totalQty)
}

@BindingAdapter(value = ["bottomSheetTitle", "highLightText", "bottomSheetType"], requireAll = false)
fun TextView.highlightBottomSheetTitle(bottomSheetTitle: String, highLightText: String?, bottomSheetType: BottomSheetType) {
    text = if (highLightText.isNotNullOrEmpty() && bottomSheetType == BottomSheetType.ItemComplete) {
        val endIndex = highLightText?.length?.plus(1)
        SpannableString(bottomSheetTitle).apply {
            if (endIndex != null) {
                setSpan(
                    ForegroundColorSpan(context.getColor(R.color.semiLightBlue)),
                    0, endIndex, 0
                )
            }
        }
    } else {
        bottomSheetTitle
    }
}

@BindingAdapter(value = ["isScanned", "isVisible"], requireAll = true)
fun TextView.setScanned(isScanned: Boolean, isVisible: Boolean) {
    setTextAppearance(
        if (isScanned && isVisible) R.style.NunitoSansBold20Blue else R.style.NunitoSansRegular20_Grey700
    )
}

@BindingAdapter("substituteSuggestionHeadingBG")
fun AppCompatTextView.setSubstituteSuggestionHeadingBG(suggestionHeading: String?) {
    this.text = suggestionHeading
    when (suggestionHeading) {
        context.getString(R.string.substitute_suggested_header_customer_chosen) -> this.background.setTint(context.getColor(R.color.lightAqua))
        context.getString(R.string.substitute_suggested_header) -> this.background.setTint(context.getColor(R.color.semiLightRed))
    }
}

@BindingAdapter("suggestionCardItemStrokeColor")
fun MaterialCardView.setSuggestionCardItemStrokeColor(suggestionHeading: String?) {
    when (suggestionHeading) {
        context.getString(R.string.substitute_suggested_header_customer_chosen) -> this.strokeColor = context.getColor(R.color.semiLightBlue)
        context.getString(R.string.substitute_suggested_header) -> this.strokeColor = context.getColor(R.color.divider_color)
    }
}

@BindingAdapter("app:textBackgroundTint")
fun TextView.setTextBackgrondTint(tint: Int?) {
    if (tint != null) background.setTint(context.getColor(tint))
}

@BindingAdapter("app:setLabel", "app:isRepickOriginalItemAllowed")
fun AppCompatTextView.setLabel(pickListType: PickListType, isRepickOriginalItemAllowed: Boolean) {
    text = when (pickListType) {
        PickListType.Todo -> context.getString(R.string.cant_find_item)
        PickListType.Picked -> when (isRepickOriginalItemAllowed) {
            true -> context.getString(R.string.repick_original_item_cta)
            else -> context.getString(R.string.un_pick)
        }

        PickListType.Short -> context.getString(R.string.move_to_picklist)
    }
}

@BindingAdapter("setMinHeightForQuamntityPicker")
fun ConstraintLayout.setMinHeightForQuamntityPicker(isIssueScanning: Boolean) {
    this.minHeight = if (isIssueScanning) {
        resources.getDimension(R.dimen.expanded_bottomsheet_peek_height).toInt()
    } else {
        resources.getDimension(R.dimen.default_bottomsheet_peek_height).toInt()
    }
}

@BindingAdapter(value = ["isIssueScanning", "isOrderedByWeight", "requestedQty", "requestedWeightAndUnits", "app:isDisplayType3PW"], requireAll = true)
fun AppCompatTextView.setOrderedQtyInSubstituteConfirmScreen(isIssueScanning: Boolean, isorderedByWeight: Boolean, requestedQty: String, requestedWeightAndUnits: String?, isDisplayType3PW: Boolean) {
    text = when {
        isDisplayType3PW -> requestedQty
        isIssueScanning && isorderedByWeight -> "1"
        isorderedByWeight -> requestedWeightAndUnits
        else -> requestedQty
    }
}

@BindingAdapter("app:setDueDay")
fun TextView.setDueDay(dueDay: Long = 0L) {
    text = when {
        dueDay > 1L -> context.getString(R.string.due_in_days, dueDay)
        dueDay == 1L -> context.getString(R.string.tomorrow)
        else -> ""
    }
}

@BindingAdapter("app:vehicleImageInfo", "app:dugOrder")
fun AppCompatImageView.setCarImageIcon(vehicleImageInfo: Pair<Int, Int>?, dugOrder: Boolean?) {
    if (dugOrder == true && vehicleImageInfo != null) {
        visibility = View.VISIBLE
        val wrapper = ContextThemeWrapper(context, vehicleImageInfo.first)
        setImageDrawable(ResourcesCompat.getDrawable(resources, vehicleImageInfo.second, wrapper.theme))
    } else {
        visibility = View.GONE
    }
}

@BindingAdapter("app:vehicleNameHeader")
fun AppCompatTextView.setVehicleNameHeader(header: HandOffUI?) {
    text = when {
        header?.isDugOrder == true -> context.getString(R.string.vehicle_information)
        else -> ""
    }
    visibility = if (header?.isDugOrder == true && (header.vehicleInformation.isNotNullOrEmpty() || header.vehicleImageInfo != null)) View.VISIBLE else View.GONE
}

@BindingAdapter("app:spannableText", "app:isCustomerBagPreference")
fun AppCompatTextView.setSpannableText(spannableText: String?, isCustomerBagPreference: Boolean) {
    spannableText?.let { spanText ->
        this.text = if (isCustomerBagPreference) {
            spanText
        } else {
            val startIndex = spanText.indexOf(context.getString(R.string.do_not_use_bags))
            SpannableStringBuilder(spanText).apply {
                setSpan(StyleSpan(BOLD), startIndex, spanText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}

@BindingAdapter("withBody", "withBoldBody")
fun TextView.withBoldBody(body: String?, boldBody: String?) {
    this.text =
        if (body != null && boldBody != null) {
            val startIndex = body.indexOf(boldBody)
            if (startIndex == -1) {
                body
            } else {
                SpannableStringBuilder(body).apply {
                    setSpan(StyleSpan(BOLD), startIndex, startIndex + boldBody.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        } else {
            body
        }
}

@BindingAdapter(value = ["app:customerWaitTime", "app:otpCapturedOrBypassTime", "app:isDugOrder"], requireAll = false)
fun TextView.setCustomerWaitTime(
    customerArrivalTime: ZonedDateTime? = null,
    otpCapturedOrByPassTime: ZonedDateTime? = null,
    isDugOrder: Boolean = false,
) {
    customerArrivalTime ?: return
    val endTime = if (isDugOrder) otpCapturedOrByPassTime ?: return else ZonedDateTime.now()
    val totalSeconds = ChronoUnit.SECONDS.between(customerArrivalTime, endTime)
    this.text = context.getString(R.string.timer_format, totalSeconds / 60, totalSeconds % 60)
}

/*
@BindingAdapter(value = ["app:setUpProgressConfig"] , requireAll = false)
fun StepProgressBarView.setUpProgressConfig(
    icon:Boolean ?= true
){
    val config = ProgressViewConfig(
        totalSteps = 5,
        labelSuffix = "m",
        labelTextSizeSp = 14f,
        labelTextColor = Color.BLACK,
        barHeightDp = 10,
        roundRadius = 20,
        thumbDrawable = ContextCompat.getDrawable(this.context, R.drawable.ic_progress_icon),
        barColor = Pair(Color.BLUE, Color.LTGRAY)
    )
    setUp(config) { step ->

    }
}
*/
