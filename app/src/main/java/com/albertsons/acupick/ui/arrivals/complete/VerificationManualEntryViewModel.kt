package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.repository.IdRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.getAge
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.infrastructure.utils.toDob
import com.albertsons.acupick.infrastructure.utils.toFormattedDob
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.INVALID_DOB_DATA
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.dialog.getObtainSignatureDialogDialog
import com.albertsons.acupick.ui.models.IdentificationInfo
import com.albertsons.acupick.ui.models.IdentificationType
import com.albertsons.acupick.ui.util.DrawableIdHelper
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.toFormatHelper
import com.albertsons.acupick.ui.util.toIdHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class VerificationManualEntryViewModel(
    app: Application,
) : BaseViewModel(app) {

    // DI
    private val dispatcher: DispatcherProvider by inject()
    private val siteRepo: SiteRepository by inject()
    private val idRepository: IdRepository by inject()

    // Input
    val showId = siteRepo.digitizeAgeFlags.id
    val orderNumber = MutableStateFlow<String?>(null)
    val isDugOrder = MutableStateFlow(false)
    val idType = MutableStateFlow(IdentificationType.DriversLicense)
    val minimumAgeRequired = MutableStateFlow(DEFAULT_MINIMUM_AGE_REQUIRED)

    // UI
    val idTypeText = idType.map {
        it.label.getString(app)
    }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = "")

    val title = isDugOrder.combine(idTypeText) { isDug, idType ->
        (if (isDug) R.string.verification_instructions else R.string.verification_driver)
            .toFormatHelper(idType).getString(app)
    }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = "")

    val verifyCtaText = isDugOrder.map { isDugOrder ->
        (if (isDugOrder) R.string.verification_pickup_person_cta else R.string.verification_driver_cta)
            .toIdHelper().getString(app)
    }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = "")

    val nameEntry = MutableLiveData("")
    val dobEntry = MutableLiveData("")
    val idNumberEntry = MutableLiveData("")

    val dobEntryError = dobEntry.map {
        it.isNotEmpty() && !it.isEntryPartiallyValid()
    }

    val dobEntryErrorRes = dobEntryError.map { isError ->
        if (isError) R.string.dob_error else null
    }

    val isVerifyCtaEnabled = combine(nameEntry.asFlow(), dobEntry.asFlow(), dobEntryError.asFlow(), idNumberEntry.asFlow()) { name, dob, dobError, idNumber ->
        name.isNotNullOrBlank() && dob.isNotNullOrBlank() && !dobError && if (showId.orFalse()) idNumber.isNotNullOrBlank() else true
    }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = false)

    // Events
    val onReportAndRemoveClicked = MutableSharedFlow<Unit>()
    val storeIdentificationInfoEvent = MutableLiveData<IdentificationInfo>()
    val ageVerificationComplete = MutableSharedFlow<Boolean>()

    init {
        viewModelScope.launch(dispatcher.IO) {
            toolbarRightFirstImageEvent.asFlow().collect {
                storeIdInfoToSharedVM()
                navigateToIdentificationBarcodeScanFragment()
            }
        }

        registerCloseAction(INVALID_DOB_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    viewModelScope.launch(dispatcher.IO) {
                        // remove the id info for the order number if id is invalid and user clicks to remove the items
                        orderNumber.value?.let {
                            idRepository.removeCompleteHandoff(it)
                        }
                        onReportAndRemoveClicked.emit(Unit)
                    }
                },
            )
        }

        registerCloseAction(OBTAIN_SIGNATURE_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    storeIdInfoToSharedVM()
                    navigateToSignatureCapturePage()
                },
            )
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // UI Callbacks
    // /////////////////////////////////////////////////////////////////////////
    fun onVerifyCtaClicked() {
        if (isVerifyCtaEnabled.value) {
            if (getAge(dobEntry.value?.toDob()) >= (minimumAgeRequired.value)) {

                if (siteRepo.digitizeAgeFlags.signature.orFalse()) {
                    showObtainSignatureDialog()
                } else {
                    storeIdInfoToSharedVM()
                    viewModelScope.launch(dispatcher.IO) {
                        ageVerificationComplete.emit(true)
                    }
                }
            } else {
                inlineDialogEvent.postValue(CustomDialogArgDataAndTag(INVALID_DOB_DATA, INVALID_DOB_DIALOG_TAG))
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Helpers
    // /////////////////////////////////////////////////////////////////////////
    fun showScanCta() {
        changeToolbarRightFirstExtraImageEvent.postValue(
            DrawableIdHelper.Id(R.drawable.ic_scan)
        )
    }

    fun getIdInfoFromSharedVM(idInfo: IdentificationInfo?) {
        nameEntry.value = idInfo?.name.orEmpty()
        idType.value = idInfo?.identificationType ?: IdentificationType.Other
        idNumberEntry.value = idInfo?.identificationNumber.orEmpty()
        dobEntry.value = idInfo?.dateOfBirth?.toFormattedDob().orEmpty()
    }

    private fun storeIdInfoToSharedVM() {
        viewModelScope.launch(dispatcher.Main) {
            orderNumber.value?.let {
                val idInfo = idRepository.loadCompleteHandoff(it)?.copy(
                    name = nameEntry.value,
                    dateOfBirth = dobEntry.value,
                    identificationNumber = idNumberEntry.value,
                    pickupPersonSignature = null
                )
                if (idInfo != null) {
                    idRepository.saveCompleteHandoff(it, idInfo).also {
                        if (idInfo.name == null || idInfo.dateOfBirth == null)
                            acuPickLogger.i("storeIdInfoToSharedVM: Id info object is not null but does not has name or dob ${orderNumber.value}")
                    }
                } else {
                    acuPickLogger.i("storeIdInfoToSharedVM: Id info is null ${orderNumber.value}")
                }
            }

            storeIdentificationInfoEvent.set(
                IdentificationInfo(
                    identificationType = idType.value,
                    name = nameEntry.value,
                    dateOfBirth = dobEntry.value?.toDob(),
                    identificationNumber = idNumberEntry.value,
                    pickupPersonSignature = null
                )
            )
        }
    }

    private fun showObtainSignatureDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                getObtainSignatureDialogDialog(isDugOrder = isDugOrder.value),
                tag = OBTAIN_SIGNATURE_DIALOG_TAG
            )
        )
    }

    private fun navigateToIdentificationBarcodeScanFragment() {
        orderNumber.value?.let { orderNumber ->
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    VerificationManualEntryFragmentDirections.actionToIdentificationBarcodeScanFragment(orderNumber)
                )
            )
        }
    }

    private fun navigateToSignatureCapturePage() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                VerificationManualEntryFragmentDirections.actionToCustomerSignatureFragment(orderNumber = orderNumber.value ?: "")
            )
        )
    }

    companion object {
        private val INVALID_DOB_DIALOG_TAG = "invalidDobDialogTag${this.hashCode()}"
        private val OBTAIN_SIGNATURE_DIALOG_TAG = "obtainSignatureDialogTag${this.hashCode()}"
    }
}
