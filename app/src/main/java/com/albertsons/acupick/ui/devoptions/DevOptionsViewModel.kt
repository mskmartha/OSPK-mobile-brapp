package com.albertsons.acupick.ui.devoptions

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.AcuPickConfig
import com.albertsons.acupick.configureLeakCanary
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider
import com.albertsons.acupick.data.crashreporting.ForceCrashLogic
import com.albertsons.acupick.data.environment.EnvironmentRepository
import com.albertsons.acupick.data.repository.DevOptionsRepositoryWriter
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.ui.BaseViewModel
import com.hadilq.liveevent.LiveEvent
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.launch
import timber.log.Timber

class DevOptionsViewModel(
    private val app: Application,
    private val forceCrashLogicImpl: ForceCrashLogic,
    private val environmentRepository: EnvironmentRepository,
    private val userRepository: UserRepository,
    private val devOptionsRepositoryWriter: DevOptionsRepositoryWriter,
    buildConfigProvider: BuildConfigProvider,
    private val dispatcherProvider: DispatcherProvider
) : BaseViewModel(app) {

    // ////////////////// ENVIRONMENT SECTION ////////////////// //
    val apsEnvironmentNames: LiveData<List<String>> = MutableLiveData(environmentRepository.apsEnvironments.map { it.apsEnvironmentType.shortName })
    var apsEnvironmentSpinnerPosition = environmentRepository.apsEnvironments.indexOf(environmentRepository.selectedConfig.apsEnvironmentConfig)
        private set

    val osccEnvironmentNames: LiveData<List<String>> = MutableLiveData(environmentRepository.osccEnvironments.map { it.osccEnvironmentType.shortName })
    var osccEnvironmentSpinnerPosition = environmentRepository.osccEnvironments.indexOf(environmentRepository.selectedConfig.osccEnvironmentConfig)
        private set

    val authEnvironmentNames: LiveData<List<String>> = MutableLiveData(environmentRepository.authEnvironments.map { it.authEnvironmentType.shortName })
    var authEnvironmentSpinnerPosition = environmentRepository.authEnvironments.indexOf(environmentRepository.selectedConfig.authEnvironmentConfig)
        private set

    val configEnvironmentNames: LiveData<List<String>> = MutableLiveData(environmentRepository.configEnvironments.map { it.configEnvironmentType.shortName })
    var configEnvironmentSpinnerPosition = environmentRepository.configEnvironments.indexOf(environmentRepository.selectedConfig.configEnvironmentConfig)
        private set

    val itemProcessorEnvironmentNames: LiveData<List<String>> = MutableLiveData(environmentRepository.itemProcessorEnvironments.map { it.itemProcessorEnvironmentType.shortName })
    var itemProcessorEnvironmentSpinnerPosition = environmentRepository.itemProcessorEnvironments.indexOf(environmentRepository.selectedConfig.itemProcessorEnvironmentConfig)
        private set

    private val _baseApsUrl = MutableLiveData<String>()
    val baseApsUrl: LiveData<String> = _baseApsUrl
    private val _baseAuthUrl = MutableLiveData<String>()
    val baseAuthUrl: LiveData<String> = _baseAuthUrl
    private val _baseConfigUrl = MutableLiveData<String>()
    val baseConfigUrl: LiveData<String> = _baseConfigUrl
    private val _baseItemProcessorUrl = MutableLiveData<String>()
    val baseItemProcessorUrl: LiveData<String> = _baseItemProcessorUrl

    // ////////////////// FEATURE FLAG SECTION ////////////////// //
    // add project specific things here

    // ////////////////// APP INFO SECTION ////////////////// //
    val appVersionName: LiveData<String>
    val appVersionCode: LiveData<String>
    val appId: LiveData<String>
    val buildIdentifier: LiveData<String>

    // ////////////////// MISCELLANEOUS ////////////////// //
    val useOnlineInMemoryPickListState = MutableLiveData(devOptionsRepositoryWriter.useOnlineInMemoryPickListState)
    val useLeakCanary = MutableLiveData(devOptionsRepositoryWriter.useLeakCanary)
    val rxFeatureToggle = MutableLiveData(AcuPickConfig.isRxDugEnabledAsFlow().value)
    val cattFeatureToggle = MutableLiveData(AcuPickConfig.cattEnabled.value)
    val bagBypassFeatureToggle = MutableLiveData(AcuPickConfig.bagBypassEnabled.value)
    val autoChooseLastSite = MutableLiveData(devOptionsRepositoryWriter.autoChooseLastSite)
    private val _messageToUser = LiveEvent<String>()
    val messageToUser: LiveData<String> = _messageToUser
    private val _apsEnvironmentDropdownDismissed = LiveEvent<Unit>()
    val apsEnvironmentDropdownDismissed: LiveData<Unit> = _apsEnvironmentDropdownDismissed
    private val _authEnvironmentDropdownDismissed = LiveEvent<Unit>()
    val authEnvironmentDropdownDismissed: LiveData<Unit> = _authEnvironmentDropdownDismissed
    private val _configEnvironmentDropdownDismissed = LiveEvent<Unit>()
    val configEnvironmentDropdownDismissed: LiveData<Unit> = _configEnvironmentDropdownDismissed
    private val _osccEnvironmentDropdownDismissed = LiveEvent<Unit>()
    val osccEnvironmentDropdownDismissed: LiveData<Unit> = _configEnvironmentDropdownDismissed
    private val _itemProcessorEnvironmentDropdownDismissed = LiveEvent<Unit>()
    val itemProcessorEnvironmentDropdownDismissed: LiveData<Unit> = _itemProcessorEnvironmentDropdownDismissed

    private val autoLogoutList: List<Long> = listOf(1, 3, 6, 15, 60)
    val autoLogoutTimes: LiveData<List<Long>> = MutableLiveData(autoLogoutList)
    val defaultAutoLogoutPosition = autoLogoutList.indexOf(devOptionsRepositoryWriter.autoLogoutTime)
    private val _autoLogoutDropdownDismissed = LiveEvent<Unit>()
    val autoLogoutDropdownDismissed: LiveData<Unit> = _autoLogoutDropdownDismissed

    private var restartRequired = false

    init {
        updateEnvironmentInfo()
        appVersionName = MutableLiveData(app.packageManager!!.getPackageInfo(app.packageName, 0).versionName)
        appVersionCode = MutableLiveData(app.packageManager!!.getPackageInfo(app.packageName, 0).versionCode.toString())
        appId = MutableLiveData(app.packageName)
        buildIdentifier = MutableLiveData(buildConfigProvider.buildIdentifier)
    }

    fun onApsEnvironmentChanged(newEnvironmentIndex: Int) {
        val newEnvironment = environmentRepository.apsEnvironments[newEnvironmentIndex]
        val oldEnvironment = environmentRepository.selectedConfig.apsEnvironmentConfig
        Timber.v("[onApsEnvironmentChanged] newEnvironment=$newEnvironment, oldEnvironment=$oldEnvironment")
        if (newEnvironment != oldEnvironment) {
            restartRequired = true
            apsEnvironmentSpinnerPosition = newEnvironmentIndex
            environmentRepository.changeApsEnvironment(environmentRepository.apsEnvironments[newEnvironmentIndex].apsEnvironmentType)
            updateEnvironmentInfo()
            _messageToUser.value = "!!! Restart required !!!"
            viewModelScope.launch(dispatcherProvider.IO) {
                userRepository.logout()
            }
        } else {
            Timber.v("[onApsEnvironmentChanged] no changes needed as the same environment has been selected")
        }
    }

    fun onOsccEnvironmentChanged(newEnvironmentIndex: Int) {
        val newEnvironment = environmentRepository.osccEnvironments[newEnvironmentIndex]
        val oldEnvironment = environmentRepository.selectedConfig.osccEnvironmentConfig
        Timber.v("[onOsccEnvironmentChanged] newEnvironment=$newEnvironment, oldEnvironment=$oldEnvironment")
        if (newEnvironment != oldEnvironment) {
            restartRequired = true
            osccEnvironmentSpinnerPosition = newEnvironmentIndex
            environmentRepository.changeOsccEnvironment(environmentRepository.osccEnvironments[newEnvironmentIndex].osccEnvironmentType)
            updateEnvironmentInfo()
            _messageToUser.value = "!!! Restart required !!!"
            viewModelScope.launch(dispatcherProvider.IO) {
                userRepository.logout()
            }
        } else {
            Timber.v("[onOsccEnvironmentChanged] no changes needed as the same environment has been selected")
        }
    }

    fun onAuthEnvironmentChanged(newEnvironmentIndex: Int) {
        val newEnvironment = environmentRepository.authEnvironments[newEnvironmentIndex]
        val oldEnvironment = environmentRepository.selectedConfig.authEnvironmentConfig
        Timber.v("[onAuthEnvironmentChanged] newEnvironment=$newEnvironment, oldEnvironment=$oldEnvironment")
        if (newEnvironment != oldEnvironment) {
            restartRequired = true
            authEnvironmentSpinnerPosition = newEnvironmentIndex
            environmentRepository.changeAuthEnvironment(environmentRepository.authEnvironments[newEnvironmentIndex].authEnvironmentType)
            updateEnvironmentInfo()
            _messageToUser.value = "!!! Restart required !!!"
            viewModelScope.launch(dispatcherProvider.IO) {
                userRepository.logout()
            }
        } else {
            Timber.v("[onAuthEnvironmentChanged] no changes needed as the same environment has been selected")
        }
    }

    fun onConfigEnvironmentChanged(newEnvironmentIndex: Int) {
        val newEnvironment = environmentRepository.configEnvironments[newEnvironmentIndex]
        val oldEnvironment = environmentRepository.selectedConfig.configEnvironmentConfig
        Timber.v("[onConfigEnvironmentChanged] newEnvironment=$newEnvironment, oldEnvironment=$oldEnvironment")
        if (newEnvironment != oldEnvironment) {
            restartRequired = true
            configEnvironmentSpinnerPosition = newEnvironmentIndex
            environmentRepository.changeConfigEnvironment(environmentRepository.configEnvironments[newEnvironmentIndex].configEnvironmentType)
            updateEnvironmentInfo()
            _messageToUser.value = "!!! Restart required !!!"
            viewModelScope.launch(dispatcherProvider.IO) {
                userRepository.logout()
            }
        } else {
            Timber.v("[onConfigEnvironmentChanged] no changes needed as the same environment has been selected")
        }
    }

    fun onItemProcessorEnvironmentChanged(newEnvironmentIndex: Int) {
        val newEnvironment = environmentRepository.itemProcessorEnvironments[newEnvironmentIndex]
        val oldEnvironment = environmentRepository.selectedConfig.itemProcessorEnvironmentConfig
        Timber.v("[onItemProcessorEnvironmentChanged] newEnvironment=$newEnvironment, oldEnvironment=$oldEnvironment")
        if (newEnvironment != oldEnvironment) {
            restartRequired = true
            apsEnvironmentSpinnerPosition = newEnvironmentIndex
            environmentRepository.changeItemProcessorEnvironment(environmentRepository.itemProcessorEnvironments[newEnvironmentIndex].itemProcessorEnvironmentType)
            updateEnvironmentInfo()
            _messageToUser.value = "!!! Restart required !!!"
            viewModelScope.launch(dispatcherProvider.IO) {
                userRepository.logout()
            }
        } else {
            Timber.v("[onItemProcessorEnvironmentChanged] no changes needed as the same environment has been selected")
        }
    }

    fun onAutoLogoutChanged(selection: Int) {
        devOptionsRepositoryWriter.updateAutoLogoutTime(autoLogoutList[selection])
    }

    fun onApsEnvironmentDropdownDismissed() {
        _apsEnvironmentDropdownDismissed.postValue(Unit)
    }

    fun onOsccEnvironmentDropdownDismissed() {
        _osccEnvironmentDropdownDismissed.postValue(Unit)
    }

    fun onAuthEnvironmentDropdownDismissed() {
        _authEnvironmentDropdownDismissed.postValue(Unit)
    }

    fun onConfigEnvironmentDropdownDismissed() {
        _configEnvironmentDropdownDismissed.postValue(Unit)
    }

    fun onItemProcessorEnvironmentDropdownDismissed() {
        _itemProcessorEnvironmentDropdownDismissed.postValue(Unit)
    }

    fun onAutoLogoutDismissed() {
        _autoLogoutDropdownDismissed.postValue(Unit)
    }

    fun onRestartCtaClick() {
        Timber.v("[onRestartCtaClick] restarting app now...")
        ProcessPhoenix.triggerRebirth(app)
    }

    fun onAutoChooseLastSiteToggled(toggledOn: Boolean) {
        Timber.v("[onAutoChooseLastSiteToggled] toggledOn=$toggledOn")
        if (devOptionsRepositoryWriter.autoChooseLastSite != toggledOn) {
            devOptionsRepositoryWriter.updateAutoChooseLastSite(toggledOn)
        } else {
            Timber.v("[onAutoChooseLastSiteToggled] no changes needed as the toggle value hasn't changed")
        }
    }

    fun onOnlineInMemoryPickListStateToggled(toggledOn: Boolean) {
        Timber.v("[onOnlineInMemoryPickListStateToggled] toggledOn=$toggledOn")
        if (devOptionsRepositoryWriter.useOnlineInMemoryPickListState != toggledOn) {
            devOptionsRepositoryWriter.updateUseOnlineInMemoryPickListState(toggledOn)
        } else {
            Timber.v("[onOnlineInMemoryPickListStateToggled] no changes needed as the toggle value hasn't changed")
        }
    }

    fun onLeakCanaryToggled(toggledOn: Boolean) {
        Timber.v("[onLeakCanaryToggled] toggledOn=$toggledOn")
        if (devOptionsRepositoryWriter.useLeakCanary != toggledOn) {
            devOptionsRepositoryWriter.updateUseLeakCanary(toggledOn)
            configureLeakCanary(toggledOn)
        } else {
            Timber.v("[onLeakCanaryToggled] no changes needed as the toggle value hasn't changed")
        }
    }

    fun onCattFeatureToggled(toggledOn: Boolean) {
        Timber.v("[onCattFeatureToggled] toggledOn=$toggledOn")
        viewModelScope.launch(dispatcherProvider.IO) {
            AcuPickConfig.cattEnabled.emit(toggledOn)
        }
    }

    fun onBagBypassFeatureToggled(toggledOn: Boolean) {
        Timber.v("[onBagBypassFeatureToggled] toggledOn=$toggledOn")
        viewModelScope.launch(dispatcherProvider.IO) {
            AcuPickConfig.bagBypassEnabled.emit(toggledOn)
        }
    }

    fun onRxFeatureToggled(toggledOn: Boolean) {
        Timber.v("[onRxFeatureToggled] toggledOn=$toggledOn")
        viewModelScope.launch(dispatcherProvider.IO) {
            AcuPickConfig.rxEnabled.emit(toggledOn)
        }
    }

    fun onForceCrashCtaClicked() {
        Timber.v("[onForceCrashCtaClicked] forcing crash now...")
        forceCrashLogicImpl.forceCrashNow()
    }

    fun triggerRestartIfNecessary() {
        if (restartRequired) {
            Timber.v("[triggerRestartIfNecessary] restarting app now...")
            ProcessPhoenix.triggerRebirth(app)
        }
    }

    private fun updateEnvironmentInfo() {
        _baseApsUrl.value = environmentRepository.selectedConfig.apsEnvironmentConfig.baseApsUrl
        _baseAuthUrl.value = environmentRepository.selectedConfig.authEnvironmentConfig.baseAuthUrl
        _baseConfigUrl.value = environmentRepository.selectedConfig.configEnvironmentConfig.baseConfigUrl
        _baseItemProcessorUrl.value = environmentRepository.selectedConfig.itemProcessorEnvironmentConfig.baseItemProcessorUrl
    }
}
