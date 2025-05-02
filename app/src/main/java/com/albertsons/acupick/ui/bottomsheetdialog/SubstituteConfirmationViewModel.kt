package com.albertsons.acupick.ui.bottomsheetdialog

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.request.ItemPickCompleteDto
import com.albertsons.acupick.data.model.request.SubstitutedItems
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.substitute.SubstitutionLocalItem
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.time.ZonedDateTime

class SubstituteConfirmationViewModel(
    val app: Application,
) : BaseViewModel(app) {

    private val pickRepository: PickRepository by inject()
    private val userRepo: UserRepository by inject()

    val iaId = MutableLiveData<Long>()
    val messageSid = MutableLiveData<String?>()
    val siteId = MutableLiveData<String>()
    val navigation: LiveData<Pair<CloseAction, Int?>> = LiveEvent()
    val subListItemUi = MutableLiveData<List<SubstitutionLocalItem>>()
    val imageUrl = MutableLiveData<String>()
    val description = MutableLiveData<String>()
    val requestedCount = MutableLiveData<String>()
    val isOrderedByWeight = MutableLiveData<Boolean>()
    val isDisplayType3PW = MutableLiveData<Boolean>()
    val requestedWeightAndUnits = MutableLiveData<String>()
    val itemToRemove = MutableLiveData<SubstitutionLocalItem>()
    val isIssueScanning = MutableLiveData<Boolean>()
    val isBulk = MutableLiveData<Boolean>()
    val sellByType = MutableLiveData<SellByType>()
    val isRequestedWeightToShow = MutableLiveData<Boolean>()
    val isCustomerBagPreference: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    fun showDeleteSubItemDialog(item: SubstitutionLocalItem) {
        itemToRemove.postValue(item)
    }

    fun onConfirmButtonClick() {
        val siteId = userRepo.user.value.also {
            if (it == null) acuPickLogger.w("[loadUpcCodes] user is null - unable to retrieve siteId")
        }?.selectedStoreId.orEmpty()
        viewModelScope.launch {
            val result = isBlockingUi.wrap {
                pickRepository.recordItemPickComplete(
                    ItemPickCompleteDto(
                        iaId = iaId.value,
                        siteId = siteId,
                        messageSid = messageSid.value,
                        substitutedItems = subListItemUi.value?.map {
                            SubstitutedItems(
                                itemId = it.item.itemId,
                                substitutedTime = ZonedDateTime.now(),
                                pickedUpc = it.itemBarcodeType?.getBarcodeToSendToBackend()
                            )
                        }
                    )
                )
            }
            when (result) {
                is ApiResult.Success -> {
                    navigation.postValue(Pair(CloseAction.Positive, null))
                }
                is ApiResult.Failure -> {
                    if (result is ApiResult.Failure.Server) {
                        handleApiError(result, retryAction = { onConfirmButtonClick() })
                    } else {
                        navigation.postValue(Pair(CloseAction.Positive, null))
                    }
                }
            }
        }
    }

    fun onAddAnotherSubstitutionClick() {
        navigation.postValue(Pair(CloseAction.Negative, null))
    }
}
