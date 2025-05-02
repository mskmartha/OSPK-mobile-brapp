package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.repository.IdRepository
import com.albertsons.acupick.ui.BaseViewModel
import com.github.gcacace.signaturepad.views.SignaturePad
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream

class CustomerSignatureViewModel(app: Application) : BaseViewModel(app) {

    val signatureButtonsEnabled = MutableStateFlow(false)
    val signatureString = MutableStateFlow<String?>(null)
    val orderNumber = MutableStateFlow<String?>(null)
    private val idRepository: IdRepository by inject()

    val clearSignatueEvent = MutableSharedFlow<Unit>()
    val saveSignatueEvent = MutableSharedFlow<Unit>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getSignatureFlow(signaturePad: SignaturePad): Flow<Boolean> {
        return callbackFlow {

            val signatureListener = object : SignaturePad.OnSignedListener {
                override fun onStartSigning() {
                    trySend(true)
                }

                override fun onSigned() {
                    // When the screen is rotated, the signature pad triggers
                    // onStartSigning, if this happens, keep buttons disabled
                    if (signaturePad.isEmpty) trySend(false)
                }

                override fun onClear() {
                    trySend(false)
                }
            }

            signaturePad.setOnSignedListener(signatureListener)

            awaitClose {
                signaturePad.setOnSignedListener(null)
            }
        }
    }

    fun onClearButtonClicked() {
        viewModelScope.launch {
            clearSignatueEvent.emit(Unit)
        }
    }

    fun onSaveButtonClicked() {
        viewModelScope.launch {
            saveSignatueEvent.emit(Unit)
        }
    }

    fun encodeSignatureToString(signatureBitmap: Bitmap) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val signature = Base64.encodeToString(byteArray, Base64.DEFAULT)

        orderNumber.value?.let {
            val idInfo = idRepository.loadCompleteHandoff(it)?.copy(
                pickupPersonSignature = signature
            )
            if (idInfo != null) {
                idRepository.saveCompleteHandoff(it, idInfo).also {
                    if (idInfo.name == null || idInfo.dateOfBirth == null)
                        acuPickLogger.i("encodeSignatureToString: Id info object is not null but does not has name or dob ${orderNumber.value}")
                }
            } else {
                acuPickLogger.i("encodeSignatureToString: Id info is null ${orderNumber.value}")
            }
        }
        signatureString.value = signature
    }
}
