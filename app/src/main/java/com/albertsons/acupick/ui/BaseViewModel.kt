package com.albertsons.acupick.ui

import android.app.Application
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.shouldShowCountdownTimer
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.toSameZoneInstantLocalDate
import com.albertsons.acupick.infrastructure.utils.toSpacedHourFormat
import com.albertsons.acupick.infrastructure.utils.toZoneTime
import com.albertsons.acupick.navigation.ExternalNavigationEvent
import com.albertsons.acupick.navigation.ExternalNavigationObserver
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.navigation.NavigationObserver
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.dialog.ALREADY_ASSIGNED_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CANCELED_BATCH_HANDOFF_ARG_DATA
import com.albertsons.acupick.ui.dialog.CANCELED_BATCH_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CANCELED_BATCH_STAGING_ARG_DATA
import com.albertsons.acupick.ui.dialog.CANCELED_SINGLE_HANDOFF_ARG_DATA
import com.albertsons.acupick.ui.dialog.CANCELED_SINGLE_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CANCELED_SINGLE_STAGING_ARG_DATA
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.dialog.CloseActionListener
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.HAND_OFF_ALREADY_ASSIGNED_ARG_DATA
import com.albertsons.acupick.ui.dialog.HAND_OFF_BATCH_ALREADY_ASSIGNED_ARG_DATA
import com.albertsons.acupick.ui.dialog.REASSIGNED_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.util.DrawableIdHelper
import com.albertsons.acupick.ui.util.equalsIgnoreCase
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Provides [LiveEvent]s for both navigation and external navigation and helper functionality to observe the [LiveData] from a [Fragment] */
abstract class BaseViewModel(private val app: Application) : AndroidViewModel(app), KoinComponent {
    // DI
    val acuPickLogger: AcuPickLoggerInterface by inject()

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        dialogTagCloseActionListenerMap.clear()
    }

    /** Use to send [NavigationEvent]s (from subclasses) */
    protected val _navigationEvent = LiveEvent<NavigationEvent>()

    /** Note: You probably don't need to be using this, as [observeNavigationEvents] is likely handling the observer setup for you. Available if necessary. */
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    /** Use to send [ExternalNavigationEvent]s (from subclasses) */
    protected val _externalNavigationEvent = LiveEvent<ExternalNavigationEvent>()

    /** Note: You probably don't need to be using this, as [observeNavigationEvents] is likely handling the observer setup for you. Available if necessary. */
    val externalNavigationEvent: LiveData<ExternalNavigationEvent> = _externalNavigationEvent

    /** BaseFragment observes this and sets UI-blocking loading state in MainActivity */
    val isBlockingUi: LiveData<Boolean> = MutableLiveData()

    /** True is fragment view exists. Necessary to gate updating common UI (toolbar) when the fragment's view associated with this viewmodel is destroyed */
    protected var isUiActive: Boolean = false

    private var timerJob: Job? = null

    /** Call from associated fragment's onCreateView and onDestroyView functions to update UI active state */
    @CallSuper
    open fun updateUiLifecycle(active: Boolean) {
        isUiActive = active
    }

    /** Setup observations for both [navigationEvent] and [externalNavigationEvent] from [fragment] */
    fun observeNavigationEvents(fragment: Fragment) {
        navigationEvent.observe(fragment.viewLifecycleOwner, NavigationObserver(fragment.findNavController()))
        externalNavigationEvent.observe(fragment.viewLifecycleOwner, ExternalNavigationObserver(fragment.requireActivity()))
    }

    //  TODO - look at just using LiveDateHelper interface: DRY
    /** Helper function to avoid needing downcast declarations for public MutableLiveData */
    protected fun <T> LiveData<T>.set(value: T?) = (this as? MutableLiveData<T>)?.setValue(value) ?: run { acuPickLogger.w("[post] unable to setValue for $this") }

    /** Helper function to avoid needing downcast declarations for public MutableLiveData */
    protected fun <T> LiveData<T>.postValue(value: T?) = (this as? MutableLiveData<T>)?.postValue(value) ?: run { acuPickLogger.w("[post] unable to postValue for $this") }

    protected fun <T> MutableLiveData<T>.notifyObservers() {
        this.postValue(this.value)
    }

    /** Helper function to avoid needing downcast declarations for public MutableSharedFlow */
    protected suspend fun <T> SharedFlow<T>.emit(value: T) = (this as? MutableSharedFlow<T>)?.emit(value) ?: run { acuPickLogger.w("[post] unable to emit for $this") }

    // Wrapping a live data with this function will cause further chains to reprocess when update event is triggered
    //  This is useful when updating lower values in object hierarchy that don't trigger observable updates
    fun <T, R> LiveEvent<Unit>.updating(source: LiveData<T>, block: (T) -> R): LiveData<R> =
        asFlow().combine(source.asFlow()) { _, it -> block(it) }.asLiveData()

    fun <T, R> LiveEvent<Unit>.updating(source: Flow<T>, block: (T) -> R): LiveData<R> =
        asFlow().combine(source) { _, it -> block(it) }.asLiveData()

    /**
     * Toolbar events
     *
     *   These live data will act as events to drive the actual toolbar implementation.
     * BaseFragment will observe and relay to implementation, removing view model dependencies on toolbar implementation
     */
    // Outgoing events to toolbar
    val changeToolbarTitleEvent: LiveData<String> = MutableLiveData()
    val changeToolbarTitleBackgroundImageEvent: LiveData<Drawable> = MutableLiveData()
    val changeToolbarSmallTitleEvent: LiveData<String> = MutableLiveData()
    val changeToolbarLeftExtraEvent: LiveData<String> = MutableLiveData()
    val changeToolbarLeftExtraImageEvent: LiveData<Drawable> = MutableLiveData()
    val changeToolbarRightExtraTopEvent: LiveData<String> = MutableLiveData()
    val changeToolbarRightExtraBottomEvent: LiveData<String> = MutableLiveData()
    val changeToolbarExtraRightEvent: LiveData<String> = MutableLiveData()
    val changeToolbarExtraRightCtaEvent: LiveData<Pair<String, () -> Unit>?> = MutableLiveData(null)
    val changeToolbarRightSecondExtraImageEvent: LiveData<DrawableIdHelper> = MutableLiveData()
    val changeToolbarRightFirstExtraImageEvent: LiveData<DrawableIdHelper> = MutableLiveData()
    val clearToolbarEvent: LiveData<Unit> = LiveEvent<Unit>()

    //  Incoming event from toolbar
    val navigationButtonEvent = LiveEvent<Unit?>()
    val triggerHomeButtonEvent = LiveEvent<Unit?>()
    val toolbarRightSecondImageEvent = LiveEvent<Unit>()
    val toolbarRightFirstImageEvent = LiveEvent<Unit>()
    val toolbarTitleTextEvent = LiveEvent<Unit>()

    // /////////////////////////////////////////////////////////////////////////
    // Framework to allow for dialogs to be fully coded inside of ViewModel and still maintain connection across process death.
    // /////////////////////////////////////////////////////////////////////////
    /**
     * Map is used to store listeners for lookup by BaseFragment
     */
    val dialogTagCloseActionListenerMap = HashMap<String, CloseActionListener>()

    /**
     * BaseFragment observes this event and creates a dialog based on args and tag
     */
    val inlineDialogEvent: LiveData<CustomDialogArgDataAndTag> = LiveEvent()

    /**
     * BaseFragment observes this event and creates a bottom sheet based on args and tag
     */
    val inlineBottomSheetEvent: LiveData<BottomSheetArgDataAndTag> = LiveEvent()

    /**
     * BaseFragment observes this event and posts to MainActivity.snackBarEvent with throttling
     */
    val snackBarLiveEvent: LiveData<SnackBarEvent<Long>?> = LiveEvent()

    /**
     * List of snack bar events (including pending ones) to allow clearing them all at once
     */
    val snackBarEventList = mutableListOf<SnackBarEvent<Long>>()

    /**
     * Flag that turns on when snack bar is showing
     */
    val isDisplayingSnackbar = MutableLiveData(false)

    /**
     * This function registers a listener and should be called during VM init {}
     */
    protected fun registerCloseAction(tag: String, block: () -> CloseActionListener) {
        dialogTagCloseActionListenerMap[tag] = block()
    }

    /**
     * A generic listener for errors
     */
    val serverErrorListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            Timber.v("[onCloseAction] closeAction=$closeAction")
            when (closeAction) {
                CloseAction.Positive,
                CloseAction.Negative,
                CloseAction.Dismiss,
                -> Any()
            }.exhaustive
        }
    }

    private var timeelapsedSinceOrderReleasedMs: Long = 0L
    /**
     * Pass thru keyboard event
     */
    val keyboardActiveEvent: LiveData<Boolean> = LiveEvent()

    /**
     * event to handle chat navigation on popup dialogues from other fragments
     */
    val handleChatNavigationEvent: LiveData<Boolean> = LiveEvent()

    /**
     * View model error handling
     *
     *  Going to use this to route error events thru BaseFragment to avoid coupling UI view models
     * directly to higher level view models and error handling logic.
     *
     * Empty values of tag will be caught on other side and given correct default value for handling code
     */
    val apiErrorEvent: LiveData<Pair<String, ApiResult.Failure>> = LiveEvent()
    var retryActionEvent: LiveEvent<(() -> Unit)> = LiveEvent()
    fun handleApiError(errorType: ApiResult.Failure, tag: String = "", retryAction: (() -> Unit)? = null) {
        retryAction?.let { retryActionEvent.postValue(it) }
        apiErrorEvent.postValue(Pair(tag, errorType))
    }

    protected fun navigateToHome() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(NavGraphDirections.actionToHomeFragment())
        )
    }

    /**
     *   Convenience functions for replacing:
     *
     *   loading.postValue(true)
     *   //do a thing
     *   loading.postValue(false)
     *
     *  This functions is suspending so it may work with API cals.
     *
     * */
    protected suspend fun <T> LiveData<Boolean>.wrap(block: suspend () -> T): T {
        this.postValue(true)
        val result = block()
        this.postValue(false)
        return result
    }

    /**
     * Helper function to emit [SnackBarEvent]
     */
    fun showSnackBar(event: SnackBarEvent<Long>) {
        isDisplayingSnackbar.postValue(true)
        snackBarLiveEvent.postValue(event)
    }

    /**
     * Helper function to emit [AcupickSnackEvent]
     */
    fun showSnackBar(event: AcupickSnackEvent) {
        acupickSnackEvent.postValue(event)
    }

    /**
     * Helper function to emit [AcupickSnackEvent]
     */
    fun showAnchoredSnackBar(event: AcupickSnackEvent) {
        acupickSnackEventAnchored.postValue(event)
    }

    /**
     * Clear currently displayed and pending snack bar (events)
     */
    fun clearSnackBarEvents() {
        snackBarEventList.forEach {
            // Cancel pending snackbars
            it.pendingStartJob?.cancel()

            // Dismiss current snackbars - I guess there could only be 1 of these
            it.dismissLiveEvent?.postValue(Unit)
        }
        acupickSnackEvent.value?.action
    }

    /**
     * BaseFragment/Fragment(s) observes this event to show Snackbar
     */
    val acupickSnackEvent = LiveEvent<AcupickSnackEvent>()

    /**
     * Child Fragment(s) observes this event to show snackbar
     */
    val acupickSnackEventAnchored = LiveEvent<AcupickSnackEvent>()

    init {
        registerCloseAction(SINGLE_ORDER_ERROR_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    _navigationEvent.postValue(NavigationEvent.Directions(NavGraphDirections.actionToHomeFragment()))
                }
            )
        }
    }

    fun serverErrorCannotAssignUser(cannotAssignToOrderDialogTypes: CannotAssignToOrderDialogTypes, isBatch: Boolean, isStarted: Boolean = false) {
        inlineDialogEvent.postValue(
            when (cannotAssignToOrderDialogTypes) {
                CannotAssignToOrderDialogTypes.REGULAR -> {
                    val data = if (isStarted) REASSIGNED_PICKLIST_ARG_DATA else ALREADY_ASSIGNED_PICKLIST_ARG_DATA
                    CustomDialogArgDataAndTag(data = data, tag = SINGLE_ORDER_ERROR_DIALOG_TAG)
                }
                CannotAssignToOrderDialogTypes.PICKLIST -> {
                    val data = if (isBatch) CANCELED_BATCH_PICKLIST_ARG_DATA else CANCELED_SINGLE_PICKLIST_ARG_DATA
                    val tag = if (isBatch) BATCH_ORDER_ERROR_DIALOG_TAG else SINGLE_ORDER_ERROR_DIALOG_TAG
                    CustomDialogArgDataAndTag(data = data, tag = tag)
                }
                CannotAssignToOrderDialogTypes.HANDOFF -> {
                    val data = if (isBatch) CANCELED_BATCH_HANDOFF_ARG_DATA else CANCELED_SINGLE_HANDOFF_ARG_DATA
                    val tag = if (isBatch) BATCH_ORDER_ERROR_DIALOG_TAG else SINGLE_ORDER_ERROR_DIALOG_TAG
                    CustomDialogArgDataAndTag(data = data, tag = tag)
                }
                CannotAssignToOrderDialogTypes.HANDOFF_REASSIGN -> {
                    val data = if (isBatch) HAND_OFF_BATCH_ALREADY_ASSIGNED_ARG_DATA else HAND_OFF_ALREADY_ASSIGNED_ARG_DATA
                    val tag = if (isBatch) BATCH_ORDER_ERROR_DIALOG_TAG else SINGLE_ORDER_ERROR_DIALOG_TAG
                    CustomDialogArgDataAndTag(data = data, tag = tag)
                }
                CannotAssignToOrderDialogTypes.STAGING -> {
                    val data = if (isBatch) CANCELED_BATCH_STAGING_ARG_DATA else CANCELED_SINGLE_STAGING_ARG_DATA
                    val tag = if (isBatch) BATCH_ORDER_ERROR_DIALOG_TAG else SINGLE_ORDER_ERROR_DIALOG_TAG
                    CustomDialogArgDataAndTag(data = data, tag = tag)
                }
            }
        )
    }

    fun showStagingTimeOnTitle(
        formatedStageTime: String?,
        orderType: OrderType?,
        concernTimeMs: Long = 0,
        warningTimeMs: Long = 0,
        releaseTime: ZonedDateTime? = null,
        zonedDateTime: ZonedDateTime? = null,
        isPrePickOrAdvancePick: Boolean = false
    ) {
        val stagingTime = formatedStageTime?.toZoneTime() ?: ZonedDateTime.now()
        if (isUiActive) {
            if (orderType.shouldShowCountdownTimer()) {
                val titleBackground = ContextCompat.getDrawable(app.applicationContext, R.drawable.rounded_corner_picklist_status_button) as GradientDrawable
                timerJob?.cancel()
                timerJob = viewModelScope.launch {
                    flow {
                        while (stagingTime > ZonedDateTime.now()) {
                            emit(ChronoUnit.MILLIS.between(stagingTime, ZonedDateTime.now()))
                            delay(1000)
                            timeelapsedSinceOrderReleasedMs = System.currentTimeMillis().minus(releaseTime?.toInstant()?.toEpochMilli() ?: 0)
                        }
                        changeToolbarSmallTitleEvent.postValue(app.getString(R.string.toolbar_staging_past_due_format, stagingTime.toSpacedHourFormat()))

                        titleBackground.setColor(ContextCompat.getColor(app.applicationContext, R.color.picklist_stageByTime_pastDue))
                        changeToolbarTitleBackgroundImageEvent.postValue(titleBackground)
                    }.collect {
                        val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(it)
                        changeToolbarSmallTitleEvent.postValue(
                            app.getString(
                                R.string.toolbar_stage_in_format,
                                app.getString(R.string.timer_format, abs(durationSeconds.div(60)), abs(durationSeconds.rem(60)))
                            )
                        )

                        // TODO: Need to change the background color if due time is less than 12 mins. This will be updated once 'releasedEventDateTime'
                        //  value provided through activity APi
                        val backgroundColor = when {
                            (abs(it)) < warningTimeMs -> R.color.picklist_stageByTime_pastDue
                            timeelapsedSinceOrderReleasedMs > concernTimeMs -> R.color.semiLightOrange
                            else -> R.color.semiLightOrange
                        }

                        titleBackground.setColor(ContextCompat.getColor(app.applicationContext, backgroundColor /*R.color.picklist_stageByTime_dueSoon*/))
                        titleBackground.alpha = 170
                        changeToolbarTitleBackgroundImageEvent.postValue(titleBackground)
                    }
                }
            } else {
                changeToolbarTitleBackgroundImageEvent.postValue(null)
                val dueDay = if (isPrePickOrAdvancePick) ChronoUnit.DAYS.between(
                    ZonedDateTime.now().toLocalDate(), (zonedDateTime ?: ZonedDateTime.now()).toSameZoneInstantLocalDate()
                ) else 0L
                when {
                    dueDay == 1L -> changeToolbarSmallTitleEvent.postValue(app.getString(R.string.toolbar_due_tomorrow))
                    dueDay > 1L -> changeToolbarSmallTitleEvent.postValue(app.getString(R.string.toolbar_due_in_days, dueDay))
                    else -> changeToolbarSmallTitleEvent.postValue(app.getString(R.string.toolbar_stage_by_staging_format, stagingTime.toSpacedHourFormat()))
                }
            }
        }
    }

    // Ties flow to viewModelScope to give StateFlow.
    fun <T> Flow<T>.groundState(initialValue: T) =
        stateIn(viewModelScope, SharingStarted.Lazily, initialValue)

    fun <T> Flow<T?>.groundState() =
        stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun String?.logError(value: String) {
        if (this.isNullOrEmpty() || this.equalsIgnoreCase("null") || this.equalsIgnoreCase("0")) acuPickLogger.e(value)
    }

    companion object {
        const val SINGLE_ORDER_ERROR_DIALOG_TAG = "singleOrderErrorDialogTag"
        const val BATCH_ORDER_ERROR_DIALOG_TAG = "batchOrderErrorDialogTag"
        const val GENERIC_RELOAD_DIALOG = "genericReloadDialog"
    }
}
