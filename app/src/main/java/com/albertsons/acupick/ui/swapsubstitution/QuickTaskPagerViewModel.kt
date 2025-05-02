package com.albertsons.acupick.ui.swapsubstitution

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.ui.BaseViewModel
import org.koin.core.component.inject

class QuickTaskPagerViewModel(app: Application) : BaseViewModel(app) {
    private val siteRepository: SiteRepository by inject()
    val dispatcherProvider: DispatcherProvider by inject()
    val pickRepo: PickRepository by inject()
    val isMasterOrderView = MutableLiveData<Boolean>(siteRepository.twoWayCommsFlags.masterOrderView)
}
