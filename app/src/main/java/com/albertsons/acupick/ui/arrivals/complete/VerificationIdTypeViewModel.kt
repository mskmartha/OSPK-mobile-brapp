package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.repository.IdRepository
import com.albertsons.acupick.data.repository.IdentificationInfoPto
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.models.IdentificationType
import com.albertsons.acupick.ui.models.toIdentificationInfoPto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class VerificationIdTypeViewModel(
    app: Application,
) : BaseViewModel(app) {

    private val idRepository: IdRepository by inject()
    val orderNumber = MutableStateFlow<String?>(null)
    val isDugOrder = MutableStateFlow(false)
    val radioChecked = MutableStateFlow(-1)
    val selection = radioChecked.map { radioChecked ->
        try {
            IdentificationType.values().find { it.layoutId == radioChecked }
        } catch (e: Exception) {
            null
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = null)
    val radioButtons = IdentificationType.values().map { it.label.getString(app.baseContext) }.toList()

    // Events
    val onIdUnavailableClickedEvent = MutableSharedFlow<String>()
    val onContinueClickedEvent = MutableSharedFlow<Pair<String, IdentificationType>>()

    fun onContinueClicked() {
        orderNumber.value?.let { orderNumber ->
            selection.value?.let { selection ->
                idRepository.saveCompleteHandoff(
                    orderNumber,
                    IdentificationInfoPto(
                        identificationType = selection.toIdentificationInfoPto(),
                        name = null,
                        dateOfBirth = null,
                        identificationNumber = null,
                        pickupPersonSignature = null
                    )
                )

                viewModelScope.launch {
                    onContinueClickedEvent.emit(Pair(orderNumber, selection))
                }
            }
        }
    }
    fun onIdUnavailableClicked() {
        viewModelScope.launch {
            // remove the id info for the order number if id unavailable is clicked
            orderNumber.value?.let {
                idRepository.removeCompleteHandoff(it)
            }
            onIdUnavailableClickedEvent.emit(orderNumber.value)
        }
    }
}
