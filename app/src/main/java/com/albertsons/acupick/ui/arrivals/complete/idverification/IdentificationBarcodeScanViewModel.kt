package com.albertsons.acupick.ui.arrivals.complete.idverification

import android.app.Application
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.infrastructure.utils.identificationName
import com.albertsons.acupick.infrastructure.utils.toLocalDateDob
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.models.IdentificationInfo
import com.albertsons.acupick.ui.models.IdentificationType.DriversLicense
import com.albertsons.acupick.ui.util.UserFeedback
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import timber.log.Timber

const val SUCCESSFUL_BARCODE_SCAN_DELAY: Long = 2000

class IdentificationBarcodeScanViewModel(val app: Application) : BaseViewModel(app) {

    val userFeedback: UserFeedback by inject()

    val orderNumber = MutableStateFlow<String?>(null)
    val scannedDriversLicense = MutableStateFlow<IdentificationInfo?>(null)
    val navigateBackEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val scanSuccessful = MutableLiveData(false)

    fun onManualEntryClicked() {
        navigateToVerificationManualEntryFragment()
    }

    fun onCloseButtonClicked() {
        viewModelScope.launch {
            navigateBackEvent.emit(Unit)
        }
    }

    @ExperimentalGetImage
    fun processImage(imageProxy: ImageProxy, barcodeScannerClient: BarcodeScanner) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            barcodeScannerClient.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        when (barcode.valueType) {
                            Barcode.TYPE_DRIVER_LICENSE -> {
                                viewModelScope.launch {
                                    acceptSuccessfulDriversLicense(barcode)
                                }
                            }
                        }
                    }
                }.addOnFailureListener {
                    // Task failed with an exception
                    userFeedback.setFailureScannedSoundAndHaptic()
                    Timber.d("[BarcodeScan] - Barcode scan failed $it")
                }.addOnCompleteListener {
                    imageProxy.image?.close()
                    imageProxy.close()
                }
        }
    }

    private suspend fun acceptSuccessfulDriversLicense(barcode: Barcode) {
        val driverLicense = barcode.driverLicense
        scannedDriversLicense.value =
            IdentificationInfo(
                identificationType = DriversLicense,
                name = driverLicense?.identificationName(),
                dateOfBirth = driverLicense?.birthDate?.toLocalDateDob(),
                identificationNumber = driverLicense?.licenseNumber,
                pickupPersonSignature = null,
            )
        scanSuccessful.value = true
        viewModelScope.launch {
            delay(SUCCESSFUL_BARCODE_SCAN_DELAY)
            // Navigate to the next page
            navigateToVerificationManualEntryFragment()
        }
    }

    private fun navigateToVerificationManualEntryFragment() {
        orderNumber.value?.let { orderNumber ->
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    IdentificationBarcodeScanFragmentDirections.actionToVerificationManualEntryFragment(orderNumber)
                )
            )
        }
    }
}
