package com.albertsons.acupick.ui.picklists

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class PickListPagerViewModel(app: Application) : BaseViewModel(app) {
    val dispatcherProvider: DispatcherProvider by inject()
    val activityViewModel: MainActivityViewModel by inject()
    val conversationsRepository: ConversationsRepository by inject()
    val pickRepo: PickRepository by inject()
    val showUnreadMessages = MutableLiveData<Boolean>(false)

    fun onChatClicked(orderNumber: String) {
        viewModelScope.launch {
            pickRepo.pickList.value?.actId?.let {
                _navigationEvent.postValue(
                    NavigationEvent.Directions(
                        NavGraphDirections.actionPicklistPagerFragmentToPickListItemsFragment(
                            activityId = it.toString(),
                            orderNumber = orderNumber,
                            navigateToChat = true
                        )
                    )
                )
            }
        }
    }
}
