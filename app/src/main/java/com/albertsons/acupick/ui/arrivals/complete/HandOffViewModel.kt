package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.AcuPickConfig
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.duginterjection.DugInterjectionState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isBeginVerificationState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isIdleState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isItemsRemovedState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isRegulatedOrder
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isRemoveRestrictedItemsState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isRxItemsRemovedState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isRxRemoveRestrictedItemsState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isVerificationCtaEnabled
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isVerifiedPickupPersonState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isVerifyCodeState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isVerifyingCodeState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.pickupType
import com.albertsons.acupick.data.logic.HandOffVerificationState
import com.albertsons.acupick.data.logic.HandOffVerificationState.BEGIN_VERIFICATION
import com.albertsons.acupick.data.logic.HandOffVerificationState.CODE_VERIFIED
import com.albertsons.acupick.data.logic.HandOffVerificationState.IDLE
import com.albertsons.acupick.data.logic.HandOffVerificationState.ITEMS_REMOVED
import com.albertsons.acupick.data.logic.HandOffVerificationState.RX_ITEMS_REMOVED
import com.albertsons.acupick.data.logic.HandOffVerificationState.RX_REMOVE_ITEMS
import com.albertsons.acupick.data.logic.HandOffVerificationState.VERIFIED_PICKUP_PERSON
import com.albertsons.acupick.data.logic.HandOffVerificationState.VERIFYING_CODE
import com.albertsons.acupick.data.logic.HandOffVerificationState.VERIFY_CODE
import com.albertsons.acupick.data.model.AuthCodeUnavailableReasonCode
import com.albertsons.acupick.data.model.AuthCodeUnavailableReasonCode.WRONG_CUSTOMER_CODE
import com.albertsons.acupick.data.model.CancelReasonCode
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.request.PickUpUserRequestDto
import com.albertsons.acupick.data.model.request.PickupPersonDto
import com.albertsons.acupick.data.model.request.RemoveItemsReasonCode
import com.albertsons.acupick.data.model.request.RemoveItemsReasonCode.CUSTOMER_NOT_HAVE_VALID_ID
import com.albertsons.acupick.data.model.request.RemoveItemsReasonCode.DRIVER_NOT_HAVE_VALID_ID
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDto
import com.albertsons.acupick.data.model.request.RxDeliveryFailedReason
import com.albertsons.acupick.data.model.request.RxOrder
import com.albertsons.acupick.data.model.request.RxOrderStatus
import com.albertsons.acupick.data.model.response.FE_SCREEN_STATUS_STORE_NOTIFIED
import com.albertsons.acupick.data.model.response.OrderStatus
import com.albertsons.acupick.data.model.response.hasAddOnPrescription
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.IdRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.toDob
import com.albertsons.acupick.infrastructure.utils.toServerFormattedDob
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DRIVER_ID_VERIFICATION_DATA
import com.albertsons.acupick.ui.dialog.DRIVER_ID_VERIFICATION_DATA_DUG
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.ORDER_DETAILS_CANCEL_HANDOFF_ARG_DATA
import com.albertsons.acupick.ui.dialog.RX_ORDER_DETAILS_CANCEL_HANDOFF_ARG_DATA
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.FulfillmentTypeUI
import com.albertsons.acupick.ui.models.IdentificationInfo
import com.albertsons.acupick.ui.models.VerificationInfo
import com.albertsons.acupick.ui.models.isComplete
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.combineWith
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.toFulfillmentTypeUI
import com.albertsons.acupick.ui.util.transform
import com.albertsons.acupick.ui.util.triple
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.io.Serializable
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

const val DEFAULT_MINIMUM_AGE_REQUIRED = 21

class HandOffViewModel(
    val app: Application,
    initialUi: HandOffUI,
    private val activityViewModel: MainActivityViewModel,
) : BaseViewModel(app) {

    // DI
    private val apsRepository: ApsRepository by inject()
    private val siteRepository: SiteRepository by inject()
    private val idRepository: IdRepository by inject()
    private val pushNotificationsRepository: PushNotificationsRepository by inject()

    val handOffUI = MutableLiveData<HandOffUI>(initialUi)
    val hasRegulatedItems = MutableLiveData(checkRegualted(initialUi))

    private var isScrolled = false
    val scroller: LiveData<Boolean> = MutableLiveData(false)

    fun checkRegualted(initialUi: HandOffUI): Boolean {
        val rejectedItem = initialUi.rejectedItems.find { it.orderNo == initialUi.orderNumber }
        val nonRemovedList = initialUi.items.map { regulatedItem ->
            rejectedItem?.removedItems?.find { it?.itemId == regulatedItem.itemId && it?.quantity == regulatedItem.totalQty.toInt() } == null
        }
        return nonRemovedList.any { it }
    }

    val restrictedItemCount = initialUi.items.size
    private val removedItemsList = MutableLiveData(initialUi.rejectedItems.toMutableList())
    val isDugOrder = handOffUI.map { it.isDugOrder }
    val is3p = handOffUI.map { it.is3p }
    val isRxDugHandOff = handOffUI.map { it.isRxDug }
    val rxOrderStatus = MutableLiveData(getOrderStatus(handOffUI.value) ?: OrderStatus.READY_FOR_PU)
    val isRxDeliveryReadyForPU = rxOrderStatus.map { it == OrderStatus.READY_FOR_PU }
    private val rxDeliveryFailureReason = MutableLiveData(handOffUI.value?.rxDeliveryFailedReason)
    val isPharmacyNotServicingOrders = handOffUI.value?.let {
        it.isRxDug.orFalse() && (!it.isPharmacyServicingOrders.orFalse() || getOrderStatus(it) != OrderStatus.READY_FOR_PU) ||
            rxDeliveryFailureReason.value.isNotNullOrEmpty()
    }.orFalse()
    val createdOrders = initialUi.createdOrders
    val hasPharmacyServicingOrders = handOffUI.value?.let { it.isPharmacyServicingOrders.orFalse() && it.rxOrderStatus == OrderStatus.READY_FOR_PU }

    val confirmOrderText = StringIdHelper.Format(
        R.string.formatted_verification_code,
        if (initialUi.is3p.orFalse()) initialUi.source else initialUi.provider,
        StringIdHelper.Id(if (initialUi.fulfillmentType?.type == FulfillmentType.DUG) R.string.fulfillment_dug else R.string.fulfillment_three_pl)
            .getString(getApplication())
    ).getString(getApplication())

    val handOffResultFlow = MutableStateFlow<HandOffResultData?>(null)
    val isOverlayVisible = handOffResultFlow.map { it?.markedCompleted == true && !isRxDugHandOff.value.orFalse() }.asLiveData()
    val isCancelled = handOffResultFlow.map { it?.isCancel == true }.asLiveData()

    private var removeItemsReasonCode: RemoveItemsReasonCode? = null
    val isCattEnabled = AcuPickConfig.cattEnabled.value
    val isCctEnabled = siteRepository.isCctEnabled
    val digitizedAgeFlags = siteRepository.digitizeAgeFlags
    val passRemovedItems = MutableLiveData<MutableList<RemoveItemsRequestDto>>(mutableListOf())

    // val bagCount = "${initialUi.bagCount}"
    val toteCount = initialUi.toteCount.toString()
    val bagCount = initialUi.bagCount.toString()
    val looseItemCount = initialUi.looseItemCount.toString()
    val showToteCount = isCattEnabled && initialUi.toteCount > 0
    val showBagCount = isCattEnabled && initialUi.bagCount > 0
    val showLooseItemCount = isCattEnabled && initialUi.looseItemCount > 0

    val verificationCodeEntryText = MutableLiveData("")
    val identificationInfo = MutableStateFlow<IdentificationInfo?>(null)
    val isLoading = MutableLiveData(false)
    val isAuthCodeVerified = MutableLiveData(false)
    val authCodeIssueReported = MutableLiveData(false)
    val isConfirmOrderChecked = MutableLiveData(false)
    val isValidIdClicked = MutableLiveData(false)
    val isInvalidIdClicked = MutableLiveData(false)
    val isIdVerified = MutableStateFlow(false)
    val customerIndex = MutableLiveData(1) // 1-based
    val customerCount = MutableLiveData(1)
    val shownRxRestrictedItems = MutableLiveData(false)
    val isAuthDugEnabled = MutableLiveData(false)

    // adding this extra variables to test whether the otpcaptured or otpbyPass timestamp sent to server correctly
    private var otpCapturedTimeStamp: ZonedDateTime? = null
    private var otpByPassTimeStamp: ZonedDateTime? = null
    val otpCapturedTimestampFlow = MutableStateFlow<ZonedDateTime?>(null)
    val otpByPassTimeStampFlow = MutableStateFlow<ZonedDateTime?>(null)
    val codeVerifiedOrReportLogged = isAuthCodeVerified.combineWith(authCodeIssueReported) { verified, reported ->
        verified == true || reported == true
    }

    val isFromPartialPrescriptionReturn = MutableLiveData(false)

    // private val scannedBags = mutableListOf<String>()
    //
    // fun setPickedBagNumbers(pickedBagNumbers: List<String>) = scannedBags.addAll(pickedBagNumbers)

    // additonally we would show the prescription unavailable header when the rxOrderStatus is DELIVERY_FAILED and isFromPartialPrescriptionReturn is true
    val showPrescriptionUnavailable =
        combine(rxOrderStatus.asFlow(), isFromPartialPrescriptionReturn.asFlow(), isAuthCodeVerified.asFlow(), authCodeIssueReported.asFlow()) {
            rxOrderStatus,
            isPartialPrescriptionReturned,
            verified,
            reported,
            ->
            rxOrderStatus == OrderStatus.DELIVERY_FAILED || isPartialPrescriptionReturned || ((verified == true || reported == true) && isPharmacyNotServicingOrders)
        }.asLiveData()

    private val authCodeUnavailableReasonCodes = AuthCodeUnavailableReasonCode.values()

    // private var authCodeUnavailableReasonCode: AuthCodeUnavailableReasonCode? = null
    val authCodeUnavailableReasonCodeFlow = MutableStateFlow<AuthCodeUnavailableReasonCode?>(null)

    val handOffVerificationState = MutableLiveData(IDLE)
    val showRemoveItemsCta = handOffVerificationState.transform { it.isRemoveRestrictedItemsState() }
    val rxRemoveItemsCta = handOffVerificationState.transform { it.isRxRemoveRestrictedItemsState() || it.isRxItemsRemovedState() }
    val removeItemsCtaEnabled = MutableLiveData(false)

    val verifyCodeVisibility = handOffVerificationState.combineWith(isAuthCodeVerified) { handOffVerificationState, isAuthCodeVerified ->
        handOffVerificationState.isVerifyCodeState() || handOffVerificationState.isVerifyingCodeState() ||
            (isAuthCodeVerified == false || handOffVerificationState.isBeginVerificationState().orFalse())
    }

    val isDugBottomSheetVisible = combine(isOverlayVisible.asFlow(), isDugOrder.asFlow(), isAuthDugEnabled.asFlow(), is3p.asFlow()) { isOverlayVisible, isDugOrder, authDugEnabled, is3p ->
        !isOverlayVisible && isDugOrder.orFalse() && !authDugEnabled && !is3p.orFalse() && !(digitizedAgeVerificationEnabled() && hasRegulatedItems.value.orFalse())
    }.asLiveData()
    val isCompleteEnabled =
        combine(
            isConfirmOrderChecked.asFlow(),
            codeVerifiedOrReportLogged.asFlow(),
            hasRegulatedItems.asFlow(),
            isValidIdClicked.asFlow(),
            isInvalidIdClicked.asFlow(),
        ) { isConfirmChecked, codeVerifiedOrReported, hasRegulatedItems, isValidIdClicked, isInvalidIdClicked ->
            ((isConfirmChecked || codeVerifiedOrReported) && (!hasRegulatedItems || isValidIdClicked || isInvalidIdClicked)) ||
                (isRegulatedOrder(digitizedAgeVerificationEnabled(), hasRegulatedItems) && (if (isAuthDugEnabled.value == false) isConfirmChecked.orFalse() else true))
        }.asLiveData()

    private val isVerifyCodeCtaEnabled = combine(
        verificationCodeEntryText.asFlow(),
        isLoading.asFlow(),
        handOffVerificationState.asFlow()
    ) { enteredCode, isLoading, handOffVerificationState ->
        (enteredCode.length == 4) && !isLoading && !handOffVerificationState.isBeginVerificationState().orFalse()
    }.asLiveData()

    val isVerifyCtaEnabled = isVerifyCodeCtaEnabled.triple(identificationInfo.asLiveData(), handOffVerificationState) { isVerifyCodeCtaEnabled, identificationInfo, handoffState ->
        isVerificationCtaEnabled(handoffState, identificationInfo.isComplete(digitizedAgeFlags.id ?: false), isVerifyCodeCtaEnabled)
    }
    val shouldTriggerBottomSheet =
        combine(
            isConfirmOrderChecked.asFlow(),
            hasRegulatedItems.asFlow(),
            isValidIdClicked.asFlow(),
            is3p.asFlow()
        ) { isConfirmChecked, hasRegulatedItems, isValidIdClicked, is3p ->
            isConfirmChecked && !is3p.orFalse() && (!hasRegulatedItems || isValidIdClicked)
        }.distinctUntilChanged().asLiveData()

    val isCodeUnavailableButtonShown: LiveData<Boolean> = codeVerifiedOrReportLogged.map { !it }
        .combineWith(handOffVerificationState) { isCodeUnavailableButtonShown, showAgeVerificationCta ->
            isCodeUnavailableButtonShown.orFalse() && showAgeVerificationCta.isVerifyCodeState().orFalse()
        }
    val showCompleteCta = combine(
        isDugBottomSheetVisible.asFlow(),
        isAuthDugEnabled.asFlow(),
        codeVerifiedOrReportLogged.asFlow(),
        handOffVerificationState.asFlow(),
        rxRemoveItemsCta.asFlow(),
    ) { isBottomSheetVisible, isAuthDugEnabled, codeVerifiedOrReportLogged, handOffVerificationState, rxRemoveItemsCta ->
        when {
            rxRemoveItemsCta -> false
            handOffVerificationState.isIdleState() &&
                (isAuthDugEnabled.orFalse() && !isDugOrder.value.orFalse()) -> true

            handOffVerificationState.isBeginVerificationState() ||
                handOffVerificationState.isVerifiedPickupPersonState() ||
                handOffVerificationState.isItemsRemovedState() -> true

            handOffVerificationState.isRemoveRestrictedItemsState() -> false
            else -> (!isBottomSheetVisible && (!isAuthDugEnabled || codeVerifiedOrReportLogged))
        }
    }.asLiveData()

    // Events
    val expandBottomSheetAction: LiveData<Unit> = LiveEvent()
    val storeVerificationInfo = MutableSharedFlow<Pair<String, VerificationInfo>>()
    val clearVerificationInfoEvent = MutableSharedFlow<String?>()
    val restrictedItemRemoved = MutableLiveData(false)
    var timerJob: Job? = null

    init {
        registerCloseAction(BOTTOM_SHEET_DIALOG_TAG) {
            closeActionFactory(positive = { onCompleteClicked() })
        }

        registerCloseAction(DRIVER_ID_VERIFICATION_TAG) {
            closeActionFactory(
                positive = { beginIdVerification() },
                negative = {
                    handOffVerificationState.postValue(HandOffVerificationState.REMOVE_ITEMS)
                }
            )
        }

        registerCloseAction(AUTH_CODE_INVALID_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    clearAuthcodeBottomsheet()
                    // otpByPassTimeStamp = null
                    // otpByPassTimeStampFlow.value = null
                    updateOtpByPassTimeStamp(null)
                    authCodeUnavailableReasonCodeFlow.value = null
                    handOffVerificationState.postValue(VERIFY_CODE)
                },
                negative = {
                    authCodeUnavailableReasonCodeFlow.value = WRONG_CUSTOMER_CODE
                    // otpByPassTimeStamp = ZonedDateTime.now()
                    // otpByPassTimeStampFlow.value = ZonedDateTime.now()
                    updateOtpByPassTimeStamp(ZonedDateTime.now())
                    authCodeIssueReported.postValue(true)
                    codeVerificationComplete()
                }
            )
        }

        registerCloseAction(RX_AUTH_CODE_INVALID_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    clearAuthcodeBottomsheet()
                    updateOtpByPassTimeStamp(null)
                    authCodeUnavailableReasonCodeFlow.value = null
                    handOffVerificationState.postValue(VERIFY_CODE)
                },
                negative = {
                    authCodeUnavailableReasonCodeFlow.value = WRONG_CUSTOMER_CODE
                    updateOtpByPassTimeStamp(ZonedDateTime.now())
                    authCodeIssueReported.postValue(true)
                    handOffVerificationState.postValue(RX_REMOVE_ITEMS)
                    removeItemsCtaEnabled.postValue(true)
                    dismissAuthcodeBottomsheet()
                }
            )
        }

        registerCloseAction(RX_CODE_UNAVAILABLE_TAG) {
            closeActionFactory(
                positive = {
                    authCodeUnavailableReasonCodeFlow.value = AuthCodeUnavailableReasonCode.NO_CUSTOMER_CODE
                    // otpByPassTimeStamp = ZonedDateTime.now()
                    // otpByPassTimeStampFlow.value = ZonedDateTime.now()
                    updateOtpByPassTimeStamp(ZonedDateTime.now())
                    authCodeIssueReported.postValue(true)

                    viewModelScope.launch {
                        handOffVerificationState.postValue(RX_REMOVE_ITEMS)
                    }
                    removeItemsCtaEnabled.postValue(true)
                    dismissAuthcodeBottomsheet()
                },
                negative = {
                    // otpByPassTimeStamp = null
                    // otpByPassTimeStampFlow.value = null
                    updateOtpByPassTimeStamp(null)
                    authCodeUnavailableReasonCodeFlow.value = null
                }
            )
        }

        registerCloseAction(RX_CODE_WARNING_TAG) {
            closeActionFactory(
                positive = {
                    showRxWhyCodeNotAvailableModal()
                }
            )
        }
        registerCloseAction(RX_RESTAGE_WARNING_TAG) {
            closeActionFactory(
                positive = {
                    showRestageHandOffDialog()
                }
            )
        }

        registerCloseAction(AUTH_CODE_UNAVAILABLE_DIALOG_TAG) {
            removeItemsReasonCode = if (handOffUI.value?.isDugOrder.orFalse()) CUSTOMER_NOT_HAVE_VALID_ID else DRIVER_NOT_HAVE_VALID_ID
            closeActionFactory(
                positive = { selection ->
                    authCodeUnavailableReasonCodeFlow.value = authCodeUnavailableReasonCodes[selection ?: 0]
                    // otpByPassTimeStamp = ZonedDateTime.now()
                    // otpByPassTimeStampFlow.value = ZonedDateTime.now()
                    updateOtpByPassTimeStamp(ZonedDateTime.now())
                    authCodeIssueReported.postValue(true)
                    handOffVerificationState.postValue(
                        if (digitizedAgeVerificationEnabled() && hasRegulatedItems.value.orFalse()) {
                            BEGIN_VERIFICATION
                        } else {
                            CODE_VERIFIED
                        }
                    )
                    viewModelScope.launch {
                        handOffResultFlow.emit(
                            HandOffResultData(
                                isCancel = false,
                                // authCodeUnavailableReasonCode = authCodeUnavailableReasonCode,
                                authCodeUnavailableReasonCode = authCodeUnavailableReasonCodeFlow.value,
                                // otpByPassTimeStamp = otpByPassTimeStamp,
                                otpByPassTimeStamp = otpByPassTimeStamp,
                                isIdVerified = isIdVerified.value,
                                userInputAuthCode = verificationCodeEntryText.value,
                                completeOrCancelTime = ZonedDateTime.now()
                            )
                        )
                    }
                    codeVerificationComplete()
                }
            )
        }

        registerCloseAction(HANDOFF_RESTAGE_ORDER_DIALOG_TAG) {
            closeActionFactory(
                positive = { selection ->
                    onRestageOrder(CancelReasonCode.values()[selection ?: 0])
                },
            )
        }
    }

    private fun getOrderStatus(handoffUi: HandOffUI?) = apsRepository.cachedPickUpActivityDetails(handoffUi?.erId ?: 0)?.rxDetails?.orderStatus
    private fun getDeliveryFailureReason(app: Application): String? {
        val activityDto = apsRepository.cachedPickUpActivityDetails(handOffUI.value?.erId ?: 0)
        val orderStatus = activityDto?.rxDetails?.orderStatus
        var deliveryFailReason: String? = null
        when {
            handOffUI.value?.isPharmacyServicingOrders == false -> {
                deliveryFailReason = RxDeliveryFailedReason.PHARMACY_CLOSED.value
            }

            (
                orderStatus == OrderStatus.STAGED || orderStatus == OrderStatus.STAGING ||
                    orderStatus == OrderStatus.NEW || orderStatus == OrderStatus.CANCELLED
                ) -> {
                deliveryFailReason = RxDeliveryFailedReason.ORDER_NOT_READY_FOR_PICKUP.value
            }

            rxDeliveryFailureReason.value == app.getString(R.string.rx_dug_pharmacy_issue_dialog_choice_one_value) -> {
                deliveryFailReason = RxDeliveryFailedReason.PHARMACY_STAFF_NOT_PRESENT.value
            }

            rxDeliveryFailureReason.value == app.getString(R.string.rx_dug_pharmacy_issue_dialog_choice_two_value) -> {
                deliveryFailReason = RxDeliveryFailedReason.PHARMACY_SYSTEM_CANNOT_PROCESS.value
            }

            isAuthCodeVerified.value == false || authCodeIssueReported.value == true -> {
                deliveryFailReason = RxDeliveryFailedReason.CUSTOMER_UNABLE_TO_PROVIDE_OTP_CODE.value
            }
        }
        return deliveryFailReason
    }

    private fun recordRemovedCode() {
        if (handOffUI.value?.items.isNotNullOrEmpty()) {
            val updatedRemovedItemsList = removedItemsList.value ?: mutableListOf()
            updatedRemovedItemsList.add(
                RemoveItemsRequestDto(
                    orderNo = handOffUI.value?.orderNumber,
                    siteId = siteRepository.siteDetails.value?.siteId,
                    entityReference = handOffUI.value?.entityReference,
                    timestamp = ZonedDateTime.now(),
                    reasonCode = removeItemsReasonCode,
                    removedItems = handOffUI.value?.items?.map { it.toRemovedItem(true) }
                )
            )
            removedItemsList.postValue(updatedRemovedItemsList)
        }
    }

    fun onValidIdClicked() {
        isValidIdClicked.set(true)
        isInvalidIdClicked.set(false)
    }

    private fun showDriverIdValidationDialog(type: FulfillmentAttributeDto?) {
        val data: CustomDialogArgData = when {
            type?.type == FulfillmentType.DUG -> DRIVER_ID_VERIFICATION_DATA_DUG
            type?.subType == FulfillmentSubType.THREEPL -> DRIVER_ID_VERIFICATION_DATA
            else -> DRIVER_ID_VERIFICATION_DATA
        }
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = data,
                tag = DRIVER_ID_VERIFICATION_TAG
            )
        )
    }

    fun onInvalidIdClicked() {
        isInvalidIdClicked.set(true)
        isValidIdClicked.set(false)
        val updatedRemovedItemsList = removedItemsList.value ?: mutableListOf()
        updatedRemovedItemsList.add(
            RemoveItemsRequestDto(
                orderNo = handOffUI.value?.orderNumber,
                siteId = siteRepository.siteDetails.value?.siteId,
                entityReference = handOffUI.value?.entityReference,
                timestamp = ZonedDateTime.now(),
                reasonCode = removeItemsReasonCode,
                removedItems = handOffUI.value?.items?.map { it.toRemovedItem(true) }
            )
        )
        removedItemsList.postValue(updatedRemovedItemsList)
    }

    fun onBottomSheetHeaderClicked() {
        expandBottomSheetAction.postValue(Unit)
    }

    fun onCompleteClicked() {
        when {
            handOffVerificationState.value.isItemsRemovedState() ||
                handOffVerificationState.value.isVerifiedPickupPersonState() -> completeHandoff()

            handOffVerificationState.value.isVerifyCodeState() ||
                handOffVerificationState.value.isVerifyCodeState() -> onVerifyCodeClicked()

            handOffVerificationState.value.isRxRemoveRestrictedItemsState() -> openHandOffRemovalBottomSheet(isRx = true)
            hasRegulatedItems.value.orFalse() && digitizedAgeVerificationEnabled() -> showDriverIdValidationDialog(handOffUI.value?.fulfillmentType)
            else -> completeHandoff()
        }
    }

    private fun passRemovedItems() {
        removedItemsList.value?.filter { it.orderNo == handOffUI.value?.orderNumber }?.let {
            passRemovedItems.value = passRemovedItems.value?.apply { addAll(it) }
        }
    }

    // remove the id information if available from the shared preferences
    fun cleanUpIdVerification() {
        handOffUI.value?.orderNumber?.let { idRepository.removeCompleteHandoff(it) }
    }

    private fun completeHandoff() {

        viewModelScope.launch {
            if (removedItemsList.value.isNotNullOrEmpty()) {
                passRemovedItems()
            }

            fun getPickUpUserInfoFromDb(): PickUpUserRequestDto? {
                handOffUI.value?.orderNumber?.let {
                    val idInfo = idRepository.loadCompleteHandoff(it)
                    if (idInfo != null && (idInfo.name == null || idInfo.dateOfBirth == null)) {
                        acuPickLogger.i("getPickUpUserInfoFromDb: Id info object is not null but does not has name or dob ${handOffUI.value?.orderNumber}")
                    }
                    return if (idInfo != null) {
                        PickUpUserRequestDto(
                            orderNumber = handOffUI.value?.orderNumber ?: "",
                            siteId = siteRepository.siteDetails.value?.siteId ?: "",
                            pickupTimeStamp = ZonedDateTime.now(),
                            pickupPerson = PickupPersonDto(
                                pickupPersonName = idInfo.name.orEmpty(),
                                pickupPersonDOB = idInfo.dateOfBirth?.toDob()?.toServerFormattedDob().orEmpty(),
                                pickupPersonType = pickupType(handOffUI.value?.fulfillmentType?.type),
                                pickupPersonIDType = app.getString(idInfo.identificationType.serverValue),
                                pickupPersonIDNumber = if (idInfo.identificationNumber?.isNotEmpty().orFalse()) idInfo.identificationNumber else null,
                                pickupPersonSignature = if (idInfo.pickupPersonSignature?.isNotEmpty().orFalse()) idInfo.pickupPersonSignature else null
                            )
                        )
                    } else if (digitizedAgeVerificationEnabled() && hasRegulatedItems.value.orFalse() && !restrictedItemRemoved.value.orFalse()) {
                        acuPickLogger.i("Idinfo is missing ${handOffUI.value?.orderNumber}")
                        null
                    } else if (restrictedItemRemoved.value.orFalse()) {
                        acuPickLogger.i("Restricted item is removed ${handOffUI.value?.orderNumber}")
                        null
                    } else null
                }
                return null
            }

            val rxFailedReason = getDeliveryFailureReason(getApplication())
            val userInfo = getPickUpUserInfoFromDb()

            fun getStatus(): RxOrderStatus {
                val activityDto = apsRepository.cachedPickUpActivityDetails(handOffUI.value?.erId ?: 0)
                val hasAddOnPrescription = activityDto?.hasAddOnPrescription()
                return when {

                    rxFailedReason == RxDeliveryFailedReason.CUSTOMER_UNABLE_TO_PROVIDE_OTP_CODE.value && hasAddOnPrescription.orFalse() -> {
                        RxOrderStatus.DELIVERY_FAILED
                    }

                    !hasAddOnPrescription.orFalse() || rxFailedReason == RxDeliveryFailedReason.PHARMACY_SYSTEM_CANNOT_PROCESS.value -> {
                        RxOrderStatus.DELIVERY_FAILED_NO_PICKUP
                    }

                    rxFailedReason == RxDeliveryFailedReason.PHARMACY_STAFF_NOT_PRESENT.value || rxFailedReason == RxDeliveryFailedReason.PHARMACY_SYSTEM_CANNOT_PROCESS.value -> {
                        RxOrderStatus.DELIVERY_FAILED_NO_PICKUP
                    }

                    else -> {
                        RxOrderStatus.DELIVERY_COMPLETED
                    }
                }
            }

            // fun getDeliveryFailedReasonCode(bagNumber: String?): String {
            //     scannedBags.contains(bagNumber).let {
            //         return if (it) RxDeliveryFailedReason.BAG_PROCESSED_SUCCESSFULLY.value else RxDeliveryFailedReason.BAG_FAILED_TO_PROCESS.value
            //     }
            // }

            // We would send the Rx order details in the completeHandoff API call only if the order is not from partial prescription return
            fun getRxOrders(): List<RxOrder>? {
                return if (isFromPartialPrescriptionReturn.value == false && rxOrderStatus.value != OrderStatus.DELIVERY_FAILED) {
                    handOffUI.value?.rxOrderIds?.map {
                        RxOrder(
                            rxOrderId = it,
                            // TODO will remove after testing
                            rxOrderStatus = /*if (isFromPartialPrescriptionReturn.value == true) RxOrderStatus.DELIVERY_FAILED else*/ getStatus(),
                            deliveryFailReason = /*if (isFromPartialPrescriptionReturn.value == true) getDeliveryFailedReasonCode(it) else*/ rxFailedReason
                        )
                    }
                } else null
            }
            handOffResultFlow.emit(
                HandOffResultData(
                    isCancel = false,
                    markedCompleted = true,
                    // authCodeUnavailableReasonCode = authCodeUnavailableReasonCode,
                    authCodeUnavailableReasonCode = authCodeUnavailableReasonCodeFlow.value,
                    userInputAuthCode = if (authCodeIssueReported.value == true && authCodeUnavailableReasonCodeFlow.value != WRONG_CUSTOMER_CODE) null else
                        verificationCodeEntryText.value,
                    completeOrCancelTime = ZonedDateTime.now(),
                    otpCapturedTimestamp = otpCapturedTimeStamp,
                    otpByPassTimeStamp = otpByPassTimeStamp,
                    // otpCapturedTimestamp = otpCapturedTimestamp.value,
                    // otpByPassTimeStamp = otpByPassTimeStamp.value,
                    rxOrders = getRxOrders(),
                    pickupUserInfoReq = userInfo,
                    isIdVerified = userInfo != null
                )
            )
        }
    }

    fun onToolTipClicked() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToVerificationCodeToolTipFragment()
            )
        )
    }

    // Making this temporary change to test whether the otpcaptured or otpbyPass timestamp sent to server correctly
    fun updateOtpCapturedTimeStamp(newTimestamp: ZonedDateTime?) {
        otpCapturedTimeStamp = newTimestamp
        otpCapturedTimestampFlow.value = newTimestamp
    }

    fun updateOtpByPassTimeStamp(newTimestamp: ZonedDateTime?) {
        otpByPassTimeStamp = newTimestamp
        otpByPassTimeStampFlow.value = newTimestamp
    }

    fun onViewBagsPerViewModelClicked() {
        handOffUI.value?.bagsPerTempZoneParams?.let {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    NavGraphDirections.actionToBagsPerTempZoneFragment(it.copy(startTime = handOffUI.value?.startTime))
                )
            )
        }
    }

    fun onViewOrderSummaryClicked() {
        handOffUI.value?.orderSummary?.let {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    NavGraphDirections.actionHandoffToOrderSummary(
                        OrderSummaryArg(
                            isCas = handOffUI.value?.isCas,
                            orderSummary = it,
                            is3p = handOffUI.value?.is3p,
                            source = handOffUI.value?.source
                        )
                    )
                )
            )
        }
    }

    fun onAuthCodeUnavailableCtaClicked() = when {
        shouldShowRxDialogAuthModal() -> showRxWarningDialog() // Show the RX DUG warning dialog.
        else -> inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.CustomRadioButtons,
                    title = StringIdHelper.Id(R.string.code_unavailable),
                    body = StringIdHelper.Id(R.string.auth_code_unavailable_dialog_title),
                    customData = listOf(
                        StringIdHelper.Id(R.string.auth_code_unavailable_reason_unable),
                        StringIdHelper.Id(R.string.auth_code_unavailable_reason_alt_pickup),
                    ) as Serializable,
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                ),
                tag = AUTH_CODE_UNAVAILABLE_DIALOG_TAG
            )
        )
    }

    private fun onVerifyCodeClicked() {
        if (digitizedAgeVerificationEnabled() && hasRegulatedItems.value.orFalse()) {
            validateAuthCode()
        } else {
            // verifyAuthCodeWithDelay()
            validateAuthCode()
        }
    }

    private fun verifyAuthCodeWithDelay() {
        viewModelScope.launch {
            isLoading.wrap {
                handOffVerificationState.postValue(VERIFYING_CODE)
                delay(VALIDATION_DELAY)
                validateAuthCode()
            }
        }
    }

    private fun validateAuthCode() {
        when {
            handOffUI.value?.authenticatedPin.isNullOrEmpty() -> {
                // authCodeUnavailableReasonCode = AuthCodeUnavailableReasonCode.NO_AUTHENTICATION_CODE_PROVIDED
                authCodeUnavailableReasonCodeFlow.value = AuthCodeUnavailableReasonCode.NO_AUTHENTICATION_CODE_PROVIDED
                authCodeIssueReported.value = true
                updateOtpByPassTimeStamp(ZonedDateTime.now())
                if (isRxDugHandOff.value.orFalse() && !isPharmacyNotServicingOrders.orFalse() && shownRxRestrictedItems.value == false && isFromPartialPrescriptionReturn.value == false) {
                    // to handle the scenario where the order is Rx items but we don't have auth code from the BE and user enters wrong auth code
                    handOffVerificationState.postValue(RX_REMOVE_ITEMS)
                    removeItemsCtaEnabled.postValue(true)
                    dismissAuthcodeBottomsheet()
                } else {
                    shownRxRestrictedItems.postValue(value = true)
                    codeVerificationComplete()
                }
            }

            handOffUI.value?.authenticatedPin == verificationCodeEntryText.value -> {
                isAuthCodeVerified.value = true
                // otpCapturedTimestamp = ZonedDateTime.now()
                // otpCapturedTimestampFlow.value = ZonedDateTime.now()
                updateOtpCapturedTimeStamp(ZonedDateTime.now())
                codeVerificationComplete()
            }

            else -> {
                if (shouldShowRxDialogAuthModal()) {
                    showDoNotHandOffPrescriptionModel()
                } else {
                    showAutCodeErrorModal()
                }
                handOffVerificationState.postValue(VERIFY_CODE)
            }
        }
    }

    private fun shouldShowRxDialogAuthModal(): Boolean {
        return isRxDugHandOff.value.orFalse() && !isPharmacyNotServicingOrders.orFalse() && isFromPartialPrescriptionReturn.value == false
    }

    private fun dismissAuthcodeBottomsheet() {
        activityViewModel.bottomSheetRecordPickArgData.postValue(
            CustomBottomSheetArgData(
                dialogType = BottomSheetType.AuthCodeVerification,
                exit = true,
                title = StringIdHelper.Raw("")
            )
        )
    }

    private fun clearAuthcodeBottomsheet() {
        activityViewModel.bottomSheetRecordPickArgData.postValue(
            CustomBottomSheetArgData(
                dialogType = BottomSheetType.AuthCodeVerification,
                title = StringIdHelper.Raw("")
            )
        )
    }

    private fun codeVerificationComplete() {
        handOffVerificationState.postValue(
            if (digitizedAgeVerificationEnabled() && hasRegulatedItems.value.orFalse()) {
                BEGIN_VERIFICATION
            } else {
                CODE_VERIFIED
            }
        )
        dismissAuthcodeBottomsheet()
    }

    private fun showAutCodeErrorModal() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.AuthCodeError,
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.auth_code_invalid_dialog_title),
                    body = StringIdHelper.Id(R.string.auth_code_invalid_dialog_body),
                    secondaryBody = StringIdHelper.Id(R.string.auth_code_invalid_secondary_body),
                    positiveButtonText = StringIdHelper.Id(R.string.auth_code_invalid_postive_button),
                    negativeButtonText = StringIdHelper.Id(R.string.auth_code_invalid_negative_button),
                ),
                tag = AUTH_CODE_INVALID_DIALOG_TAG
            )
        )
    }

    private fun showRxWhyCodeNotAvailableModal() {
        val dialogArgs =
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.CustomRadioButtons,
                    title = StringIdHelper.Id(R.string.rx_dug_code_unavailable_header),
                    customData = listOf(
                        StringIdHelper.Id(R.string.rx_dug_code_unavailable_choice_one),
                        StringIdHelper.Id(R.string.rx_dug_code_unavailable_choice_two),
                    ) as Serializable,
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.rx_dug_code_cancel),
                    cancelOnTouchOutside = false
                ),
                tag = RX_CODE_UNAVAILABLE_TAG
            )

        inlineDialogEvent.postValue(dialogArgs)
    }

    private fun showDoNotHandOffPrescriptionModel() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.AuthCodeError,
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.rx_dug_code_warning_header),
                    body = StringIdHelper.Id(R.string.auth_code_invalid_dialog_body),
                    secondaryBody = StringIdHelper.Id(R.string.rx_dug_code_invalid_code_warning_body),
                    questionBody = StringIdHelper.Id(R.string.rx_dug_code_warning_question),
                    positiveButtonText = StringIdHelper.Id(R.string.auth_code_invalid_postive_button),
                    negativeButtonText = StringIdHelper.Id(R.string.auth_code_invalid_negative_button),
                ),
                tag = RX_AUTH_CODE_INVALID_DIALOG_TAG
            )
        )
    }

    private fun showRxWarningDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.AuthCodeError,
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.rx_dug_code_warning_header),
                    shouldBoldTitle = true,
                    secondaryBody = StringIdHelper.Id(R.string.rx_dug_code_warning_body),
                    questionBody = StringIdHelper.Id(R.string.rx_dug_code_warning_question),
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.rx_dug_code_cancel)
                ),
                tag = RX_CODE_WARNING_TAG
            )
        )

    private fun showRxRestageWarningDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.pharmacy_return_prescription),
                    shouldBoldTitle = true,
                    body = StringIdHelper.Id(R.string.rx_dug_restage_warning_body),
                    questionBody = StringIdHelper.Id(R.string.rx_dug_code_warning_question),
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel)
                ),
                tag = RX_RESTAGE_WARNING_TAG
            )
        )

    fun shouldHandleRestageOrder(orderNumber: String) = orderNumber == handOffUI.value?.orderNumber

    fun showRestageHandOffDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = ORDER_DETAILS_CANCEL_HANDOFF_ARG_DATA,
                tag = HANDOFF_RESTAGE_ORDER_DIALOG_TAG
            )
        )
    }

    fun showLeavingRxHandoffDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = RX_ORDER_DETAILS_CANCEL_HANDOFF_ARG_DATA,
                tag = RX_RESTAGE_WARNING_TAG
            )
        )

    fun launchRestageDialog() {
        if (handOffUI.value?.isRxDug.orFalse() && !isPharmacyNotServicingOrders.orFalse()) {
            showRxRestageWarningDialog()
        } else {
            showRestageHandOffDialog()
        }
    }

    fun onExitHandOffEvent(orderNumber: String, isFromPartialPrescriptionReturn: Boolean) {
        if (handOffUI.value?.orderNumber == orderNumber) {
            onExitHandoffDialog(isFromPartialPrescriptionReturn)
        }
    }

    private fun onExitHandoffDialog(isFromPartialPrescriptionReturn: Boolean) {
        if (handOffUI.value?.isRxDug.orFalse() && !isPharmacyNotServicingOrders.orFalse() && !isFromPartialPrescriptionReturn) {
            showLeavingRxHandoffDialog()
        } else {
            showRestageHandOffDialog()
        }
    }

    private fun onRestageOrder(cancelReasonCode: CancelReasonCode?) {
        cancelReasonCode?.let { reasonCode ->
            viewModelScope.launch {
                handOffResultFlow.emit(
                    HandOffResultData(
                        isCancel = true,
                        markedCompleted = true,
                        cancelReasonCode = reasonCode,
                        isIdVerified = isIdVerified.value,
                        completeOrCancelTime = ZonedDateTime.now()
                    )
                )
            }
        }
    }

    // Digitized Age Verification
    private fun beginIdVerification() {
        handOffUI.value?.let { handOffUI ->
            viewModelScope.launch {
                storeVerificationInfo.emit(
                    Pair(
                        handOffUI.orderNumber,
                        VerificationInfo(
                            isDugOrder = handOffUI.isDugOrder,
                            minimumAgeRequired = handOffUI.minimumAgeRequired ?: DEFAULT_MINIMUM_AGE_REQUIRED,
                        )
                    )
                )
            }
        }
    }

    fun confirmVerification(idInfo: IdentificationInfo?) {
        identificationInfo.value = idInfo
        isIdVerified.value = true
        handOffVerificationState.postValue(VERIFIED_PICKUP_PERSON)
    }

    fun verifyCtaClicked() {
        openAuthCodeBottomSheet()
    }

    fun removeItemsCanceled() {
        clearVerificationInfo()
        handOffVerificationState.postValue(BEGIN_VERIFICATION)
    }

    fun removeItemsClicked() {
        openHandOffRemovalBottomSheet()
    }

    fun onRemoveItemsConfirmed() {
        showSnackBar(AcupickSnackEvent(message = StringIdHelper.Id(R.string.items_removed), SnackType.SUCCESS, isDismissable = true))
        if (handOffVerificationState.value.isRxRemoveRestrictedItemsState()) {
            handOffVerificationState.postValue(RX_ITEMS_REMOVED)
        } else if (handOffVerificationState.value.isRxItemsRemovedState()) {
            handOffVerificationState.postValue(BEGIN_VERIFICATION)
        } else {
            clearVerificationInfo()
            recordRemovedCode()
            restrictedItemRemoved.value = true
            handOffVerificationState.postValue(ITEMS_REMOVED)
        }
        dismissRemoveItemsBottomsheet()
    }

    fun onItemsRemoveCancelled() {
        dismissRemoveItemsBottomsheet()
    }

    fun dismissRemoveItemsBottomsheet() {
        activityViewModel.bottomSheetRecordPickArgData.postValue(
            CustomBottomSheetArgData(
                dialogType = BottomSheetType.HandOffRemoveItems,
                exit = true,
                title = StringIdHelper.Raw("")
            )
        )
    }

    private fun clearVerificationInfo() {
        viewModelScope.launch {
            clearVerificationInfoEvent.emit(handOffUI.value?.orderNumber)
        }
    }

    fun setHandOffUi(currentHandOffUI: HandOffUI?) {
        handOffUI.value = currentHandOffUI
        if (handOffVerificationState.value.isIdleState()) {
            handOffVerificationState.value = when {
                currentHandOffUI?.handshakeRequired.orFalse() && currentHandOffUI?.isDugOrder.orFalse() -> VERIFY_CODE
                siteRepository.isDigitizeAgeVerificationEnabled && currentHandOffUI?.isRegulated.orFalse() -> BEGIN_VERIFICATION
                else -> IDLE
            }
        }
    }

    fun digitizedAgeVerificationEnabled() = siteRepository.isDigitizeAgeVerificationEnabled

    // DUG interjection to set dug interjection state in case of IN_HANDOFF FAILURE
    fun setDugInterjectionState(dugInterjectionState: DugInterjectionState) = pushNotificationsRepository.setDugInterjectionState(dugInterjectionState)

    fun setUpHeader() {
        if (isShowTimer()) {
            handOffUI.value?.startTime?.let {
                timerJob = viewModelScope.launch {
                    flow {
                        do {
                            emit(ChronoUnit.SECONDS.between(it, ZonedDateTime.now()))
                            delay(1000)
                        } while (coroutineContext.isActive)
                    }.collect {
                        // reset if there's any existing header text
                        changeToolbarTitleEvent.postValue("")
                        changeToolbarSmallTitleEvent.postValue(app.getString(R.string.wait_time_countdown, it.div(60), it.rem(60)))
                        val titleBackground = ContextCompat.getDrawable(app.applicationContext, R.drawable.rounded_corner_picklist_status_button) as GradientDrawable
                        titleBackground.setColor(ContextCompat.getColor(app.applicationContext, R.color.picklist_stageByTime_pastDue))
                        titleBackground.alpha = 170
                        changeToolbarTitleBackgroundImageEvent.postValue(titleBackground)
                    }
                }
                return
            }
        }

        // reset any existing timer text
        timerJob?.cancel()
        changeToolbarSmallTitleEvent.postValue("")
        changeToolbarTitleBackgroundImageEvent.postValue(null)

        changeToolbarTitleEvent.postValue(app.getString(R.string.confirm_hand_off_title))
    }

    private fun isShowTimer() = handOffUI.value?.run {
        if (fulfillmentType?.toFulfillmentTypeUI() == FulfillmentTypeUI.DUG)
            (customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED || customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED_NOT_STARTED) &&
                ((feScreenStatus == FE_SCREEN_STATUS_STORE_NOTIFIED || feScreenStatus == null)) && !codeVerifiedOrReportLogged.value.orFalse()
        else customerArrivalStatusUI != CustomerArrivalStatusUI.ARRIVING && customerArrivalStatusUI != CustomerArrivalStatusUI.EN_ROUTE
    }.orFalse()

    fun onChangeActiveOrder(activeOrderNumber: String?) {
        timerJob?.cancel()
        val isCurrent = activeOrderNumber == handOffUI.value?.orderNumber
        if (isCurrent) {
            setUpHeader()
            scrollOnce()
        }
    }

    private fun scrollOnce() {
        if (!isScrolled) {
            scroller.postValue(true)
            isScrolled = true
        } else {
            scroller.postValue(false)
        }
    }

    private fun openAuthCodeBottomSheet() {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.AuthCodeVerification,
                    title = StringIdHelper.Raw(""),
                    customDataParcel = HandOffAuthInfo(handOffUI.value?.authenticatedPin, handOffUI.value?.name),
                    peekHeight = R.dimen.authcode_bottomsheet_peek_height,
                ),
                tag = AUTH_CODE_VERIFICATION_TAG
            )
        )
    }

    private fun openHandOffRemovalBottomSheet(isRx: Boolean = false) {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.HandOffRemoveItems,
                    title = StringIdHelper.Raw(""),
                    customDataParcel = HandOffRemovalParams(isRx, handOffUI.value),
                    isFullScreen = true,
                    draggable = false,
                    peekHeight = R.dimen.expanded_bottomsheet_peek_height,
                ),
                tag = HANDOFF_REMOVAL_ITEMS_TAG
            )
        )
    }

    companion object {
        const val BOTTOM_SHEET_DIALOG_TAG = "bottomSheetDialogTag"
        const val VALIDATION_DELAY = 3000L
        const val AUTH_CODE_UNAVAILABLE_DIALOG_TAG = "authCodeUnavailableDialogTag"
        const val AUTH_CODE_INVALID_DIALOG_TAG = "authCodeInvalidDialogTag"
        const val RX_AUTH_CODE_INVALID_DIALOG_TAG = "rxAuthCodeInvalidDialogTag"
        const val HANDOFF_RESTAGE_ORDER_DIALOG_TAG = "handoffRestageOrderDialogTag"
        const val DRIVER_ID_VERIFICATION_TAG = "driverIdVerificationTag"
        const val RX_CODE_UNAVAILABLE_TAG = "rxCodeUnavailableTag"
        const val RX_CODE_WARNING_TAG = "rxCodeWarningTag"
        const val RX_RESTAGE_WARNING_TAG = "rxRestageWarningTag"
        const val AUTH_CODE_VERIFICATION_TAG = "authCodeVerificationTag"
        const val HANDOFF_AUTH_CODE_REQUEST_KEY = "handoffAuthCodeRequestKey"
        const val AUTH_CODE_VALIDATION_RESULTS = "authCodeValidationResults"
        const val AUTH_CODE_UNAVAILABLE = "authCodeUnavailable"
        const val HANDOFF_REMOVAL_ITEMS_TAG = "handoffRemovalItemsTag"
        const val HANDOFF_REMOVE_ITEMS_REQUEST_KEY = "handoffRemoveItemsRequestKey"
        const val HANDOFF_REMOVED_ITEMS = "handoffRemovedItems"
    }
}
