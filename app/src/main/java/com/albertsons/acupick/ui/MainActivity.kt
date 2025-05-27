package com.albertsons.acupick.ui

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.albertsons.acupick.AppDynamicsConfig
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.autologout.AutoLogoutLogic
import com.albertsons.acupick.data.duginterjection.DugInterjectionState
import com.albertsons.acupick.data.model.notification.NotificationType
import com.albertsons.acupick.data.model.notification.getNotificationData
import com.albertsons.acupick.data.model.response.ArrivalsCountDetailsDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.LoginLogoutAnalyticsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.data.repository.RemoveItemsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.databinding.ActivityMainBinding
import com.albertsons.acupick.databinding.NavHeaderViewBinding
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.ui.MainActivityViewModel.Companion.RETRY_ERROR_DIALOG_TAG
import com.albertsons.acupick.ui.MainActivityViewModel.Companion.TWILIO_ERROR_DIALOG_TAG
import com.albertsons.acupick.ui.dialog.BaseCustomDialogFragment
import com.albertsons.acupick.ui.dialog.CANNOT_CHANGE_STORE_WITH_ACTIVE_PICK_LIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.dialog.CloseActionListener
import com.albertsons.acupick.ui.dialog.CloseActionListenerProvider
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.showWithActivity
import com.albertsons.acupick.ui.notification.NotificationViewModel
import com.albertsons.acupick.ui.notification.NotificationViewModel.Companion.FLASH_ORDER_ACCEPTED_USER_ACTION
import com.albertsons.acupick.ui.util.AnalyticsHelper
import com.albertsons.acupick.ui.util.AnalyticsHelper.ErrorType
import com.albertsons.acupick.ui.util.AnalyticsHelper.EventKey
import com.albertsons.acupick.ui.util.AnalyticsHelper.NetworkErrorType
import com.albertsons.acupick.ui.util.NetworkBroadcastReceiver
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.UserFeedback
import com.albertsons.acupick.ui.util.addKeyboardListener
import com.albertsons.acupick.ui.util.hideKeyboard
import com.albertsons.acupick.ui.util.isDestagingFlow
import com.albertsons.acupick.ui.util.isHandOffFlow
import com.albertsons.acupick.ui.util.keepScreenOn
import com.albertsons.acupick.ui.util.removeKeyboardListener
import com.albertsons.acupick.ui.util.setStartMargin
import com.albertsons.acupick.wifi.Configuration
import com.albertsons.acupick.wifi.SIZE_MAX
import com.albertsons.acupick.wifi.manager.WiFiManagerWrapper
import com.albertsons.acupick.wifi.model.WiFiData
import com.albertsons.acupick.wifi.model.WiFiDetail
import com.albertsons.acupick.wifi.permission.PermissionService
import com.albertsons.acupick.wifi.predicate.makeAccessPointsPredicate
import com.albertsons.acupick.wifi.scanner.ScannerService
import com.albertsons.acupick.wifi.scanner.UpdateNotifier
import com.albertsons.acupick.wifi.scanner.makeScannerService
import com.albertsons.acupick.wifi.settings.Settings
import com.albertsons.acupick.wifi.vendor.VendorService
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.Date

class MainActivity :
    AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    CloseActionListenerProvider,
    NetworkBroadcastReceiver.ConnectionListener,
    SharedPreferences.OnSharedPreferenceChangeListener,
    UpdateNotifier {

    // DI
    private val analyticsHelper: AnalyticsHelper by inject()
    private val autoLogoutLogic: AutoLogoutLogic by inject()
    private val loginLogoutAnalyticsRepo: LoginLogoutAnalyticsRepository by inject()
    private val pickRepository: PickRepository by inject()
    private val appDynamicsConfig: AppDynamicsConfig by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val pushNotificationsRepository: PushNotificationsRepository by inject()
    private val removeItemsRepository: RemoveItemsRepository by inject()
    private val userRepository: UserRepository by inject()
    private val siteRepository: SiteRepository by inject()
    private val userFeedback: UserFeedback by inject()

    // WiFi Analyzer
    val configuration: Configuration by inject()
    val settings: Settings by inject()
    val vendorService: VendorService by inject()
    val wiFiManagerWrapper: WiFiManagerWrapper by inject()
    val scannerService: ScannerService = makeScannerService(this, wiFiManagerWrapper, Handler(Looper.getMainLooper()), settings)
    val wiFiDetails: MutableList<WiFiDetail> = mutableListOf()
    internal lateinit var permissionService: PermissionService
    private var currentCountryCode: String = ""

    private val activityViewModel: MainActivityViewModel by viewModel()
    private val notificationViewModel: NotificationViewModel by viewModel {
        parametersOf(activityViewModel)
    }
    private var drawer: DrawerLayout? = null
    private var networkStateReceiver: NetworkBroadcastReceiver? = null
    private var currentNotificationDialog: BaseCustomDialogFragment? = null

    private val confirmationDialogListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            Timber.v("[onCloseAction] closeAction=$closeAction")
            when (closeAction) {
                CloseAction.Positive -> {
                    lifecycleScope.launch {
                        // If connected, logout
                        if (networkAvailabilityManager.isConnected.first()) {
                            activityViewModel.manualLogout()
                        } else {
                            // Otherwise, launch warning dialog
                            BaseCustomDialogFragment.newInstance(
                                CustomDialogArgData(
                                    titleIcon = R.drawable.ic_alert,
                                    title = StringIdHelper.Id(R.string.offline_logout_warning_title),
                                    body = StringIdHelper.Id(R.string.offline_logout_warning_body),
                                    positiveButtonText = StringIdHelper.Id(R.string.offline_logout_warning_sign_out),
                                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                                    cancelOnTouchOutside = true
                                )
                            ).showWithActivity(this@MainActivity, NO_CONNECTION_LOGOUT_WARNING)
                        }
                    }
                }

                CloseAction.Negative,
                CloseAction.Dismiss,
                -> Any()
            }.exhaustive
        }
    }

    private val chatErrorListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            when (closeAction) {
                CloseAction.Positive -> {
                    ProcessPhoenix.triggerRebirth(this@MainActivity)
                }

                CloseAction.Negative,
                CloseAction.Dismiss,
                -> Any()
            }.exhaustive
        }
    }

    private val offlineWarningListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            Timber.v("[onCloseAction] closeAction=$closeAction")
            when (closeAction) {
                CloseAction.Positive -> {
                    activityViewModel.manualLogout()
                }

                CloseAction.Negative,
                CloseAction.Dismiss,
                -> Any()
            }.exhaustive
        }
    }

    private val offlineRetryListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, selection: Int?) {
            when (closeAction) {
                CloseAction.Positive -> activityViewModel.retryNetworkRequest()
                CloseAction.Negative -> {}
                CloseAction.Dismiss -> {}
            }
        }
    }

    private val retryErrorDialogListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            when (closeAction) {
                CloseAction.Positive -> activityViewModel.retryAction?.invoke()
                CloseAction.Negative -> {}
                CloseAction.Dismiss -> {}
            }
        }
    }

    private val cannotChangeStoreListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            Timber.v("[onCloseAction] closeAction=$closeAction")
            when (closeAction) {
                CloseAction.Positive -> {}
                CloseAction.Negative,
                CloseAction.Dismiss,
                -> Any()
            }.exhaustive
        }
    }

    private val endPickListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            when (closeAction) {
                CloseAction.Positive -> notificationViewModel.endCurrentPick()
                else -> {}
            }
        }
    }

    private val continueStagingListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            when (closeAction) {
                CloseAction.Positive -> notificationViewModel.navigateToStaging()
                else -> {}
            }
        }
    }

    private val skipToDestagingListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            when (closeAction) {
                CloseAction.Positive -> notificationViewModel.skipToDestaging()
                else -> {}
            }
        }
    }

    private val dataWedgeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == DW_ACTION) {
                try {
                    autoLogoutLogic.onUserInteraction()
                    loginLogoutAnalyticsRepo.onUserInteraction()
                    val scanData = intent.getStringExtra(DW_INTENT_KEY_DATA)
                    Timber.v("[dataWedgeReceiver onReceive] scanData=$scanData")
                    activityViewModel.setScannedData(scanData)
                } catch (e: Exception) {
                    Timber.e("dataWedgeReceiver: intent=$intent error=$e")
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        instance = this

        setWiFiChannelPairs(settings)

        super.onCreate(savedInstanceState)

        settings.registerOnSharedPreferenceChangeListener(this)
        keepScreenOn()
        permissionService = PermissionService(this)

        appDynamicsConfig.initialize()
        activityViewModel.acuPickLogger.setUserData(EventKey.DEVICE_ID, analyticsHelper.deviceId)

        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
            viewModel = activityViewModel
            lifecycleOwner = this@MainActivity
            drawer = drawerLayout
            // To show orignal icon color in navigation view menu item: https://stackoverflow.com/questions/44515664/bottomnavigationview-original-icon-color
            navView.itemIconTintList = null

            // Inflate the nav_header_view manually, add to navView, and set the viewModel/lifecycleOwner: https://stackoverflow.com/questions/33962548/how-to-data-bind-to-a-header
            DataBindingUtil.inflate<NavHeaderViewBinding>(layoutInflater, R.layout.nav_header_view, navView, false).apply {
                viewModel = activityViewModel
                lifecycleOwner = this@MainActivity
                navView.addHeaderView(root)
                devOptionsCta.setOnClickListener {
                    findNavController(R.id.navHostFragment).navigate(NavGraphDirections.actionGlobalToDevOptionsFragment())
                    drawerLayout.close()
                }
                navClose.setOnClickListener {
                    drawer?.closeDrawer(GravityCompat.START, true)
                }
            }
            setupNavigation(this)

            activityViewModel.storeTitle.observe(this@MainActivity) {
                navView.menu.findItem(R.id.changeStore).title = it
            }

            activityViewModel.activityNavigationEvent.observe(this@MainActivity) {
                findNavController(R.id.navHostFragment).navigate(it)
            }

            notificationViewModel.notificationNavEvent.observe(this@MainActivity) {
                it.navigate(findNavController(R.id.navHostFragment))
            }

            activityViewModel.triggerOfflineError.observe(this@MainActivity) {
                val argData = CustomDialogArgData(
                    title = StringIdHelper.Id(R.string.wifi_error_title),
                    body = StringIdHelper.Id(R.string.wifi_error_body),
                    positiveButtonText = StringIdHelper.Id(R.string.try_again),
                    cancelOnTouchOutside = true,
                    negativeButtonText = StringIdHelper.Id(R.string.close)
                )
                BaseCustomDialogFragment.newInstance(argData).apply {
                    showWithActivity(this@MainActivity, OFFLINE_DIALOG_TAG)
                }
            }

            lifecycleScope.launch {
                networkAvailabilityManager.isConnected.collect { connected ->
                    activityViewModel.acuPickLogger.setUserData(
                        key = if (connected) "Last connection time" else "Last disconnection time",
                        value = Date(System.currentTimeMillis()),
                        debugUserData = true
                    )
                    val timerName = "Disconnected from network"
                    if (connected) {
                        activityViewModel.acuPickLogger.stopTimer(timerName)
                    } else {
                        activityViewModel.acuPickLogger.startTimer(timerName)
                    }
                }
            }

            // Splash screen visible first in every use case
            activityViewModel.userLoggedIn.observe(this@MainActivity) { isLoggedIn ->
                if (!isLoggedIn) {
                    findNavController(R.id.navHostFragment).navigate(NavGraphDirections.actionGlobalToLoginFragment())
                }
            }

            activityViewModel.blockUi.observe(this@MainActivity) { isLoading ->
                supportFragmentManager.fragments.firstOrNull()?.childFragmentManager?.fragments?.getOrNull(0)?.view?.alpha = when (isLoading) {
                    true -> 0.5f
                    false -> 1.0f
                }
            }

            activityViewModel.hasSingleSiteAssigned.observe(this@MainActivity) { hasSingleSiteAssigned ->
                showOrHideChangeStoreButton(binding = this, hasSingleSiteAssigned = hasSingleSiteAssigned)
            }

            activityViewModel.toolbarNavigationIcon.observe(this@MainActivity) {
                this.toolbar.navigationIcon = it
            }

            activityViewModel.emptyToolbarEvent.observe(this@MainActivity) {
                this.toolbar.title = null
            }

            notificationViewModel.addBadgeAction.observe(this@MainActivity) {
                activityViewModel.setAcknowledgedPickerDetails(it)
                bottomNav.getOrCreateBadge(R.id.pickListPagerFragment)
            }

            notificationViewModel.removeBadgeAction.observe(this@MainActivity) {
                bottomNav.removeBadge(R.id.pickListPagerFragment)
            }

            notificationViewModel.badgeArrivalsAction.observe(this@MainActivity) {
                bottomNav.setupBadge(it)
            }

            /**
             *  In order to hide the snack bar when the drawer is open, we move the drawerLayout to an elevation of 8dp.
             *  The snack bar pops at an elevation of 7dp, this allows the snack bar to reappear with the same message when the
             *  drawer is closed
             *  */
            val drawerToggle: ActionBarDrawerToggle =
                object : ActionBarDrawerToggle(this@MainActivity, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
                    override fun onDrawerOpened(drawerView: View) {
                        super.onDrawerOpened(drawerView)
                        drawerLayout.elevation = resources.getDimension(R.dimen.drawer_layout_eight_elevation)
                    }

                    override fun onDrawerClosed(drawerView: View) {
                        super.onDrawerClosed(drawerView)
                        drawerLayout.elevation = resources.getDimension(R.dimen.drawer_layout_zero_elevation)
                    }
                }
            drawerLayout.addDrawerListener(drawerToggle)
            drawerToggle.syncState()
        }

        val dataWedgeIntent = IntentFilter().apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addAction(DW_ACTION)
        }
        registerReceiver(dataWedgeReceiver, dataWedgeIntent)

        networkStateReceiver = NetworkBroadcastReceiver(this.applicationContext).apply {
            addListener(this@MainActivity)
            registerReceiver(this, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }

        notificationViewModel.dismissNotification.observe(this@MainActivity) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(it)
        }

        notificationViewModel.dismissChatNotification.observe(this@MainActivity) {
            it?.let {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                it.forEach { id ->
                    notificationManager.cancel(id.toInt())
                }
            }
        }

        activityViewModel.activityDialogEvent.observe(this@MainActivity) { (argData, tag) ->
            BaseCustomDialogFragment.newInstance(argData).showWithActivity(this@MainActivity, tag)
        }

        /**
         * Close existing interjection dialog before open new interjection dialog.
         * example- If FLASH interjection is appearing on the screen and if DUG interjection is received.
         * We will close FLASH interjection dialog and show DUG interjection dialog.
         */
        notificationViewModel.notificationDialogEvent.observe(this@MainActivity) { (argData, tag) ->
            currentNotificationDialog?.let {
                it.dismiss()
                currentNotificationDialog = null
            }
            currentNotificationDialog = BaseCustomDialogFragment.newInstance(argData)
            currentNotificationDialog?.showWithActivity(this@MainActivity, tag)
        }

        setupFcm()
        removeItemsRepository.monitorForRequests()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        userFeedback.initializeSoundPool()
    }

    private fun BottomNavigationView.setupBadge(arrivalsCountDetailsDto: ArrivalsCountDetailsDto?) {
        arrivalsCountDetailsDto?.customerArrivalsCount?.let { count ->
            getOrCreateBadge(R.id.arrivalsPagerFragment).apply {
                backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.semiDarkerRed)
                number = count
            }
            return
        }
        removeBadge(R.id.arrivalsPagerFragment)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (!permissionService.granted(requestCode, grantResults)) {
            grantResults.forEachIndexed { index, result ->
                if (result != PackageManager.PERMISSION_GRANTED)
                    userRepository.user.value?.let { user ->
                        activityViewModel.acuPickLogger.d("${getPermissionString(index)} service denied for userId=${user.userId}.")
                    } ?: run {
                        activityViewModel.acuPickLogger.d("${getPermissionString(index)} service denied.")
                    }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getPermissionString(index: Int) = if (index == 0) "Location" else "Camera"

    override fun update(wiFiData: WiFiData) {
        configuration.size = SIZE_MAX
        val predicate = makeAccessPointsPredicate(settings)
        wiFiDetails.clear()
        wiFiDetails.addAll(wiFiData.wiFiDetails(predicate, settings.sortBy(), settings.groupBy()))

        val connectedWiFi = wiFiDetails.firstOrNull { wiFiDetail ->
            wiFiDetail.wiFiAdditional.wiFiConnection.ipAddress.isNotEmpty() &&
                wiFiDetail.wiFiAdditional.wiFiConnection.linkSpeed > -1
        }
        connectedWiFi?.let { wiFi ->
            val closestAccessPoints = wiFiDetails.filter { wiFiDetail ->
                wiFiDetail.wiFiAdditional.wiFiConnection.ipAddress.isEmpty() &&
                    wiFiDetail.wiFiAdditional.wiFiConnection.linkSpeed == -1 &&
                    wiFiDetail.wiFiSignal.level > wiFi.wiFiSignal.level
            }
            closestAccessPoints.forEach { wiFiDetail ->
                analyticsHelper.sendWiFiClosestAccessPointEvent(wiFiDetail)
            }
        }
    }

    private fun setWiFiChannelPairs(settings: Settings) {
        val countryCode = settings.countryCode()
        if (countryCode != currentCountryCode) {
            configuration.wiFiChannelPair(countryCode)
            currentCountryCode = countryCode
        }
    }

    fun update() {
        scannerService.update()
    }

    public override fun onStop() {
        scannerService.stop()
        super.onStop()
    }

    public override fun onStart() {
        super.onStart()
        if (permissionService.permissionGranted()) {
            scannerService.resume()
        } else {
            permissionService.check()
        }
    }

    private fun setupFcm() {
        FirebaseApp.initializeApp(application)
        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.e(task.exception, "Fetching FCM registration token failed")
                    activityViewModel.acuPickLogger.e("Fetching FCM registration token failed")
                    return@OnCompleteListener
                }

                val fcmRegistrationToken = task.result

                // Log new FCM registration token
                Timber.e("FCM Token: $fcmRegistrationToken")
                activityViewModel.acuPickLogger.i("Fetching FCM registration token successful")

                notificationViewModel.persistAndSendFcmToken(fcmRegistrationToken)
            }
        )
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.signOut -> {
                showLogoutConfirmationDialog()
                drawer?.closeDrawer(GravityCompat.START, false)
                false
            }

            R.id.restartApp -> {
                Timber.v("[MenuRestart] restarting app now...")
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        analyticsHelper.persistErrorEvent(ErrorType.FORCE_RESTART, "Menu Item Restart")
                        drawer?.closeDrawer(GravityCompat.START, false)
                    }
                    delay(150)
                    withContext(Dispatchers.Main) {
                        ProcessPhoenix.triggerRebirth(this@MainActivity)
                    }
                }
                false
            }

            R.id.changeStore -> {
                if (pickRepository.hasActivePickListActivityId()) {
                    // Picker has active pick list, not able to change store
                    BaseCustomDialogFragment.newInstance(
                        CANNOT_CHANGE_STORE_WITH_ACTIVE_PICK_LIST_ARG_DATA
                    ).showWithActivity(this@MainActivity, CANNOT_CHANGE_STORE_DIALOG_TAB)
                } else {
                    // Picker does not have active pick list, can change store
                    drawer?.closeDrawer(GravityCompat.START, false)
                    findNavController(R.id.navHostFragment).navigate(R.id.action_global_changeStoreFragment)
                }
                false
            }

            else -> true
        }
    }

    private fun showOrHideChangeStoreButton(binding: ActivityMainBinding, hasSingleSiteAssigned: Boolean) {
        binding.navView.menu.findItem(R.id.changeStore).isVisible = !hasSingleSiteAssigned
    }

    private fun setupNavigation(binding: ActivityMainBinding) {
        binding.apply {
            val navController = findNavController(R.id.navHostFragment)
            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.homeFragment,
                    R.id.loginFragment,
                    R.id.stagingFragment,
                    R.id.stagingPart2Fragment,
                    R.id.destageOrderFragment,
                    R.id.pickListsFragment,
                    R.id.pickListPagerFragment,
                    R.id.myScoreFragment,
                    R.id.arrivalsPagerFragment,
                    R.id.metricsFragment
                ),
                drawerLayout
            )
            toolbar.setupWithNavController(navController, appBarConfiguration)
            setSupportActionBar(toolbar)
            bottomNav.setupWithNavController(navController)
            bottomNav.setOnItemSelectedListener {
                if (it.itemId != bottomNav.selectedItemId) {
                    navController.popBackStack(it.itemId, inclusive = true, saveState = false)
                    navController.navigate(it.itemId)
                }
                return@setOnItemSelectedListener true
            }

            with(navView) {
                setupWithNavController(navController)
                setNavigationItemSelectedListener(this@MainActivity)
            }

            navController.addOnDestinationChangedListener { _, destination, _ ->
                pushNotificationsRepository.setCurrentDestination(destination.id)

                this@MainActivity.hideKeyboard()
                activityViewModel.setLoadingState(false)
                if (destination.id != R.id.manualEntryBottomSheet && destination.id != R.id.pickListItemsFragment && destination.id != R.id.substituteFragment) {
                    activityViewModel.clearToolbar()
                }

                // Lock drawer based on destination
                when (destination.id) {
                    R.id.loginFragment,
                    R.id.fieldServicesFragment,
                    R.id.handOffInterstitialFragment,
                    R.id.handOffRxInterstitialFragment,
                    -> this@MainActivity.lockDrawer()

                    else -> this@MainActivity.unlockDrawer()
                }

                // Show or hide bottom nav
                bottomNavContainer.visibility = when (destination.id) {
                    R.id.homeFragment,
                    R.id.pickListPagerFragment,
                    R.id.arrivalsPagerFragment,
                    R.id.myScoreFragment,
                    -> {
                        if (siteRepository.isFlashInterjectionEnabled) {
                            // On screens that show bottom nav, make API call and show/hide flash order acknowledgement badge
                            notificationViewModel.checkForAcknowledgedFlashOrder()
                        }
                        notificationViewModel.checkForArrivedOrders()
                        View.VISIBLE
                    }

                    else -> View.GONE
                }

                // Add keyboard listener on Search Orders flow. Remove it when not
                when (destination.id) {
                    R.id.changeStoreFragment,
                    R.id.arrivalsPagerFragment,
                    R.id.stagingFragment,
                    -> this.root.addKeyboardListener(activityViewModel.keyboardCallback)

                    else -> this.root.removeKeyboardListener(activityViewModel.keyboardCallback)
                }

                // Show or hide toolbar
                when (destination.id) {
                    R.id.handOffInterstitialFragment,
                    R.id.handOffRxInterstitialFragment,
                    R.id.loginFragment,
                    R.id.splashFragment,
                    R.id.handoff1plFragment,
                    -> {
                        toolbar.visibility = View.GONE
                    }

                    R.id.homeFragment -> {
                        actionBar?.setHomeButtonEnabled(true)
                        actionBar?.setDisplayShowTitleEnabled(false)
                        toolbar.visibility = View.VISIBLE
                    }

                    R.id.pickListPagerFragment -> {
                        actionBar?.setDisplayShowTitleEnabled(false)
                        toolbar.visibility = View.VISIBLE
                    }
                    R.id.myScoreFragment ->{
                        actionBar?.setHomeButtonEnabled(false)
                        actionBar?.setDisplayShowTitleEnabled(false)
                        toolbar.visibility = View.VISIBLE
                    }

                    else -> {
                        toolbar.visibility = View.VISIBLE
                        actionBar?.setHomeButtonEnabled(true)
                    }
                }

                // Handle nav icons
                toolbar.navigationIcon = when (destination.id) {
                    R.id.homeFragment -> ContextCompat.getDrawable(applicationContext, R.drawable.ic_hamburger)
                    R.id.storeSelectionFragment,
                    R.id.wineStaging2Fragment,
                    R.id.prescriptionReturnFragment,
                    R.id.pickListPagerFragment,
                    R.id.myScoreFragment,
                    R.id.arrivalsPagerFragment,
                    R.id.handoff1plFragment,
                    -> null

                    R.id.manualEntryPagerFragment,
                    R.id.manualEntryHandOffMfcFragment,
                    R.id.manualEntryStagingFragment,
                    R.id.boxInputWeightFragment,
                    R.id.reportMissingBagFragment,
                    -> ContextCompat.getDrawable(applicationContext, R.drawable.ic_cancel_white)

                    else -> ContextCompat.getDrawable(applicationContext, R.drawable.ic_back_arrow)
                }
                toolbarLeftImage.setStartMargin(
                    if (toolbar.navigationIcon == null) TOOLBAR_LEFT_IMAGE_START_MARGIN_SMALL else TOOLBAR_LEFT_IMAGE_START_MARGIN_LARGE
                )

                // Handle nav onClick destinations allowing SubstitutionFragment to show a dialog onBackPressed instead of navigateUp
                toolbar.setNavigationOnClickListener {
                    when (destination.id) {
                        R.id.homeFragment -> drawerLayout.open()
                        R.id.stagingFragment,
                        R.id.stagingPart2Fragment,
                        R.id.substituteFragment,
                        R.id.reportMissingBagFragment,
                        -> viewModel?.onNavigationIntercepted()

                        R.id.handOffFragment,
                        R.id.destageOrderFragment,
                        R.id.removeRejectedItemsFragment,
                        R.id.remove1PLRejectedFragment,
                        R.id.remove1PLRejectedItemsFragment,
                        -> viewModel?.triggerHomeButtonEvent()

                        else -> findNavController(R.id.navHostFragment).navigateUp()
                    }
                }

                window.setSoftInputMode(
                    when (destination.id) {
                        // TODO: Add login screen here
                        R.id.changeStoreFragment,
                        R.id.fieldServicesFragment,
                        R.id.manualEntryPagerFragment,
                        R.id.boxInputWeightFragment,
                        R.id.manualEntryHandOffFragment,
                        R.id.arrivalsPagerFragment,
                        R.id.storeSelectionFragment,
                        R.id.substituteFragment,
                        R.id.handOffFragment,
                        R.id.stagingFragment,
                        R.id.verificationManualEntryFragment,
                        R.id.chatFragment,
                        -> SOFT_INPUT_ADJUST_RESIZE

                        else -> SOFT_INPUT_ADJUST_NOTHING
                    }
                )

                // Show or hide status bar
                when (destination.id) {
                    R.id.splashFragment -> {
                        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

                        // TODO: Changing the status bar color doesn't seem to work
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.albertsonsBlue)
                    }

                    else -> {
                        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.white)
                    }
                }
                // Reset DUG interjection state to NONE for all flow except destaging and handoff flow
                if (destination.id.isDestagingFlow().not() && destination.id.isHandOffFlow().not()) {
                    pushNotificationsRepository.setDugInterjectionState(DugInterjectionState.None)
                }
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        BaseCustomDialogFragment.newInstance(
            CustomDialogArgData(
                titleIcon = R.drawable.ic_sign_out,
                title = StringIdHelper.Id(R.string.logout),
                body = StringIdHelper.Id(R.string.logout_confirmation),
                positiveButtonText = StringIdHelper.Id(R.string.ok),
                negativeButtonText = StringIdHelper.Id(R.string.cancel),
                cancelOnTouchOutside = true
            )
        ).showWithActivity(this@MainActivity, CONFIRMATION_DIALOG_TAG)
    }

    override fun provide(tag: String?) = when (tag) {
        CONFIRMATION_DIALOG_TAG -> confirmationDialogListener
        NO_CONNECTION_LOGOUT_WARNING -> offlineWarningListener
        CANNOT_CHANGE_STORE_DIALOG_TAB -> cannotChangeStoreListener
        OFFLINE_DIALOG_TAG -> offlineRetryListener // No-op
        RETRY_ERROR_DIALOG_TAG -> retryErrorDialogListener
        TWILIO_ERROR_DIALOG_TAG -> chatErrorListener
        NotificationViewModel.FLASH_AND_PARTNERPICK_ORDER_END_PICK_DIALOG_TAG -> endPickListener
        NotificationViewModel.FLASH_AND_PARTNERPICK_ORDER_FINISH_STAGING_DIALOG_TAG -> continueStagingListener
        NotificationViewModel.FLASH_DRIVER_ARRIVED_TAG -> skipToDestagingListener
        NotificationViewModel.FLASH_INTERJECTION_DIALOG_TAG,
        NotificationViewModel.URGENT_DRIVER_ARRIVED_DIALOG,
        NotificationViewModel.INTERJECTION_FOR_ALL_USER_DIALOG_TAG,
        NotificationViewModel.DUG_INTERJECTION_DIALOG_TAG,
        ->
            notificationViewModel.notifcationListener

        else -> {
            Timber.e("[provide] unhandled dialog listener")
            null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getNotificationData()?.let { data ->
            // Updating the flag to identify whether the call is from notification/interjection or not.
            activityViewModel.setAppResumesFromNotification(true)
            if (data.isShowingHybridMessage) {
                when (data.notificationType) {
                    NotificationType.PICKING -> {
                        val picker = userRepository.user.value?.firstName
                        notificationViewModel.displayHybridPickingDialog(picker, data)
                    }

                    NotificationType.ARRIVED_INTERJECTION -> {
                        notificationViewModel.handleDugInterjection(data)
                    }

                    NotificationType.ARRIVED_INTERJECTION_ALL_USER -> {
                        notificationViewModel.handleDugInterjection(data)
                    }

                    NotificationType.DISMISS -> {
                        dismissDugInterjectionDialog()
                    }

                    else -> notificationViewModel.handleUrgentOrFlashArrival(data)
                }
            } else {
                notificationViewModel.onNotificationClicked(data, FLASH_ORDER_ACCEPTED_USER_ACTION)
            }
        }
    }

    private fun dismissDugInterjectionDialog() {
        currentNotificationDialog?.let {
            it.dismiss()
            currentNotificationDialog = null
            pushNotificationsRepository.setDugInterjectionState(DugInterjectionState.None)
        }
    }

    public override fun onPause() {
        scannerService.pause()
        scannerService.unregister(this)

        super.onPause()

        Timber.v("[onPause]")
        autoLogoutLogic.applicationPaused()
    }

    public override fun onResume() {
        super.onResume()
        Timber.v("[onResume]")
        lifecycleScope.launch {
            val isLoggedIn = userRepository.isLoggedIn.value
            Timber.d("ACUPICK-1213 onResume is user logged in $isLoggedIn")
            autoLogoutLogic.performAutoLogoutOnApplicationResumed {
                Timber.v("[onResume] auto logout threshold met - logging the user out now and navigating to the login screen")
                activityViewModel.logout()
            }
        }

        if (permissionService.permissionGranted()) {
            scannerService.resume()
        } else {
            scannerService.pause()
        }
        scannerService.register(this)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        autoLogoutLogic.onUserInteraction()
        loginLogoutAnalyticsRepo.onUserInteraction()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataWedgeReceiver)
        networkStateReceiver?.let {
            it.removeListener(this)
            unregisterReceiver(it)
        }
        userFeedback.releaseSoundPool()
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        activityViewModel.updateNetworkStatus(isConnected)

        // Persist error event when no connection is available, and send it when connection is available.
        // This also handles restart app scenario when Wifi is not connected, then reconnects after restart.
        if (!isConnected) {
            analyticsHelper.persistErrorEvent(NetworkErrorType.NOT_CONNECTED_ERROR, "onNetworkConnectionChanged")
        } else {
            analyticsHelper.sendPersistedErrorEvent(NetworkErrorType.NOT_CONNECTED_ERROR)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (findNavController(R.id.navHostFragment).currentDestination?.id == R.id.boxInputWeightFragment) {
            return super.dispatchTouchEvent(ev)
        }
        val viewCanDismissKeyboard = findNavController(R.id.navHostFragment).currentDestination?.id != R.id.stagingFragment &&
            findNavController(R.id.navHostFragment).currentDestination?.id != R.id.chatFragment
        if (currentFocus != null && viewCanDismissKeyboard) {
            hideKeyboard(true)
        }
        return super.dispatchTouchEvent(ev)
    }

    // /////////////////////////////////////////////////////////////////////////
    // Drawer helpers
    // /////////////////////////////////////////////////////////////////////////
    private fun lockDrawer() = drawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    private fun unlockDrawer() = drawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

    // Constants
    companion object {
        lateinit var instance: MainActivity

        private const val TOOLBAR_LEFT_IMAGE_START_MARGIN_SMALL = 32
        private const val TOOLBAR_LEFT_IMAGE_START_MARGIN_LARGE = 88

        private const val ITEM_DETAILS_ARGUMENTS = "itemDetailsParams"

        // the intent action data wedge uses
        private const val DW_ACTION = "com.datawedge.ACTION"
        private const val DW_INTENT_KEY_DATA = "com.symbol.datawedge.data_string"
        // Leaving these as reference
        // private const val DW_INTENT_KEY_SOURCE = "com.symbol.datawedge.source"
        // private const val DW_INTENT_KEY_LABEL_TYPE = "com.symbol.datawedge.label_type"

        private const val CONFIRMATION_DIALOG_TAG = "confirmationDialog"
        private const val CANNOT_CHANGE_STORE_DIALOG_TAB = "cannotChangeStoreDialog"
        private const val OFFLINE_DIALOG_TAG = "offlineDialog"
        private const val NO_CONNECTION_LOGOUT_WARNING = "noConnectionLogoutWarning"
        private const val OF_AGE_ASSOCIATE_VERIFICATION_TAG = "ofAgeAssociateVerificationTag"
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        keepScreenOn()
        setWiFiChannelPairs(settings)
        update()
    }
}
