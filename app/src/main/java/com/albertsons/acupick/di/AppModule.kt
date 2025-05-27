package com.albertsons.acupick.di

import android.content.Context
import android.net.wifi.WifiManager
import androidx.lifecycle.SavedStateHandle
import com.albertsons.acupick.AcuPickLoggerImpl
import com.albertsons.acupick.AppDynamicsConfig
import com.albertsons.acupick.FirebaseAnalyticsImpl
import com.albertsons.acupick.FirebaseAnalyticsInterface
import com.albertsons.acupick.LoggingDataProviderImpl
import com.albertsons.acupick.buildconfig.BuildConfigProviderImpl
import com.albertsons.acupick.data.autologout.AutoLogoutLogic
import com.albertsons.acupick.data.autologout.AutoLogoutLogicImpl
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider
import com.albertsons.acupick.data.di.KoinNamedSharedPreferences
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.response.RejectedItemsByZone
import com.albertsons.acupick.data.network.logging.LoggingDataProvider
import com.albertsons.acupick.data.repository.ConversationsClientWrapper
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.data.toast.ToasterImplementation
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.image.ImagePreCacher
import com.albertsons.acupick.image.ImagePreCacherImplementation
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.ToolTipViewModel
import com.albertsons.acupick.ui.arrivals.ArrivalsPagerViewModel
import com.albertsons.acupick.ui.arrivals.ArrivalsViewModel
import com.albertsons.acupick.ui.arrivals.complete.BagsPerTempZoneViewModel
import com.albertsons.acupick.ui.arrivals.complete.CustomerSignatureViewModel
import com.albertsons.acupick.ui.arrivals.complete.HandOffBottomSheetDialogViewModel
import com.albertsons.acupick.ui.arrivals.complete.HandOffInterstitialViewModel
import com.albertsons.acupick.ui.arrivals.complete.HandOffRemoveItemsViewModel
import com.albertsons.acupick.ui.arrivals.complete.HandOffRxInterstitialViewModel
import com.albertsons.acupick.ui.arrivals.complete.HandOffUI
import com.albertsons.acupick.ui.arrivals.complete.HandOffVerificationSharedViewModel
import com.albertsons.acupick.ui.arrivals.complete.HandOffViewModel
import com.albertsons.acupick.ui.arrivals.complete.OrderSummaryViewModel
import com.albertsons.acupick.ui.arrivals.complete.VerificationCodeToolTipViewModel
import com.albertsons.acupick.ui.arrivals.complete.VerificationIdTypeViewModel
import com.albertsons.acupick.ui.arrivals.complete.VerificationManualEntryViewModel
import com.albertsons.acupick.ui.arrivals.complete.idverification.IdentificationBarcodeScanViewModel
import com.albertsons.acupick.ui.arrivals.destage.ArrivalsOptionsViewModel
import com.albertsons.acupick.ui.arrivals.destage.reportmissingbag.ReportMissingBagSheetViewModel
import com.albertsons.acupick.ui.arrivals.destage.DestageBottomSheetViewModel
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderViewModel
import com.albertsons.acupick.ui.arrivals.destage.removeitems.HandOff1PLViewModel
import com.albertsons.acupick.ui.arrivals.destage.removeitems.RemoveRejected1PLItemsViewModel
import com.albertsons.acupick.ui.arrivals.destage.removeitems.RemoveRejected1PLViewModel
import com.albertsons.acupick.ui.arrivals.destage.removeitems.RemoveRejectedItemViewModel
import com.albertsons.acupick.ui.arrivals.destage.reportmissingbag.ReportMissingBagViewModel
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.add.AddCustomerViewModel
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.changestatus.ChangeCustomerStatusViewModel
import com.albertsons.acupick.ui.arrivals.pharmacy.PrescriptionReturnViewModel
import com.albertsons.acupick.ui.auth.LoginViewModel
import com.albertsons.acupick.ui.bindingadapters.AlbertsonsDataBindingComponent
import com.albertsons.acupick.ui.bindingadapters.KoinBindingAdapters
import com.albertsons.acupick.ui.bottomsheetdialog.ActionSheetBottomSheetViewModel
import com.albertsons.acupick.ui.bottomsheetdialog.AuthCodeVerificationViewModel
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetDialogViewModel
import com.albertsons.acupick.ui.bottomsheetdialog.BulkSubstituteConfirmationViewModel
import com.albertsons.acupick.ui.bottomsheetdialog.SubstituteConfirmationViewModel
import com.albertsons.acupick.ui.chat.ChatButtonViewModel
import com.albertsons.acupick.ui.chat.ChatViewModel
import com.albertsons.acupick.ui.chat.ConversationsClientWrapperImp
import com.albertsons.acupick.ui.devoptions.DevOptionsViewModel
import com.albertsons.acupick.ui.dialog.AlternativeLocationViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogViewModel
import com.albertsons.acupick.ui.dialog.DestagingDialogViewmodel
import com.albertsons.acupick.ui.dialog.QuantityPickerDialogViewModel
import com.albertsons.acupick.ui.fieldservices.FieldServicesViewModel
import com.albertsons.acupick.ui.home.HomeViewModel
import com.albertsons.acupick.ui.itemdetails.ItemDetailsViewModel
import com.albertsons.acupick.ui.itemphoto.ItemPhotoViewModel
import com.albertsons.acupick.ui.chatImagePreview.ChatImagePreviewViewModel
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryHandOffMfcViewModel
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryHandOffViewModel
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingMfcViewModel
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingViewModel
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryToolTipViewModel
import com.albertsons.acupick.ui.manualentry.pharmacy.ManualEntryPharmacyViewModel
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel
import com.albertsons.acupick.ui.manualentry.pick.plu.ManualEntryPluViewModel
import com.albertsons.acupick.ui.manualentry.pick.upc.ManualEntryUpcViewModel
import com.albertsons.acupick.ui.manualentry.pick.weight.ManualEntryWeightViewModel
import com.albertsons.acupick.ui.metrics.MetricsViewModel
import com.albertsons.acupick.ui.missingItemLocation.MissingItemLocationViewModel
import com.albertsons.acupick.ui.missingItemLocation.WhereToFindLocationViewModel
import com.albertsons.acupick.ui.models.RemoveRejectedItemUiData
import com.albertsons.acupick.ui.my_score.MyScoreViewModel
import com.albertsons.acupick.ui.notification.NotificationViewModel
import com.albertsons.acupick.ui.picklistitems.ConfirmAmountViewModel
import com.albertsons.acupick.ui.picklistitems.PickListItemsViewModel
import com.albertsons.acupick.ui.picklists.PickListPagerViewModel
import com.albertsons.acupick.ui.picklists.open.OpenPickListsViewModel
import com.albertsons.acupick.ui.picklists.team.TeamPickListsViewModel
import com.albertsons.acupick.ui.profile.ProfileViewModel
import com.albertsons.acupick.ui.settings.SettingsViewModel
import com.albertsons.acupick.ui.splash.SplashViewModel
import com.albertsons.acupick.ui.staging.AddBagsViewModel
import com.albertsons.acupick.ui.staging.PicklistSummaryViewModel
import com.albertsons.acupick.ui.staging.StagingPart2ViewModel
import com.albertsons.acupick.ui.staging.StagingViewModel
import com.albertsons.acupick.ui.staging.UnAssignTotesViewModel
import com.albertsons.acupick.ui.staging.winestaging.WineStagingViewModel
import com.albertsons.acupick.ui.staging.winestaging.weight.BoxInputWeightViewModel
import com.albertsons.acupick.ui.staging.winestaging.weight.WineStaging2ViewModel
import com.albertsons.acupick.ui.staging.winestaging.weight.WineStaging3ViewModel
import com.albertsons.acupick.ui.storelist.StoresViewModel
import com.albertsons.acupick.ui.substitute.SubstituteViewModel
import com.albertsons.acupick.ui.swapsubstitution.QuickTaskPagerViewModel
import com.albertsons.acupick.ui.swapsubstitution.SwapSubstitutionViewModel
import com.albertsons.acupick.ui.swapsubstitution.myitems.QuickTaskMyItemsViewModel
import com.albertsons.acupick.ui.swapsubstitution.otherShoppersItems.QuickTaskOtherShoppersitemsViewModel
import com.albertsons.acupick.ui.totes.TotesViewModel
import com.albertsons.acupick.ui.util.AnalyticsHelper
import com.albertsons.acupick.ui.util.UserFeedback
import com.albertsons.acupick.wifi.Configuration
import com.albertsons.acupick.wifi.manager.WiFiManagerWrapper
import com.albertsons.acupick.wifi.settings.Repository
import com.albertsons.acupick.wifi.settings.Settings
import com.albertsons.acupick.wifi.vendor.VendorService
import com.squareup.picasso.Picasso
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** General app configuration (repositories/viewmodels/etc) */
object AppModule {
    val appModule = module {
        viewModel {
            MainActivityViewModel(
                app = get(),
                userRepo = get(),
                barcodeMapper = get(),
                networkAvailabilityManager = get(),
                networkAvailabilityController = get(),
                dispatcherProvider = get(),
                buildConfigProvider = get(),
                moshi = get(),
            )
        }
        viewModel { (activityViewModel: MainActivityViewModel) -> NotificationViewModel(app = get(), activityViewModel = activityViewModel) }
        viewModel { (ui: HandOffUI, activityViewModel: MainActivityViewModel) ->
            HandOffViewModel(app = get(), initialUi = ui, activityViewModel = activityViewModel)
        }
        viewModel { (ui: RemoveRejectedItemUiData, storageType: StorageType) ->
            RemoveRejectedItemViewModel(app = get(), ui = ui, currentZone = storageType)
        }
        viewModel { ReportMissingBagSheetViewModel(app = get()) }
        viewModel { ReportMissingBagViewModel(app = get()) }
        viewModel { HandOffBottomSheetDialogViewModel(app = get()) }
        viewModel { DestageBottomSheetViewModel(app = get()) }
        viewModel { HandOffInterstitialViewModel(app = get(), get()) }
        viewModel { HandOffRxInterstitialViewModel(app = get(), completeHandoffUseCase = get()) }
        viewModel { SplashViewModel(app = get(), userRepo = get(), dispatcherProvider = get()) }
        viewModel { (activityViewModel: MainActivityViewModel) ->
            FieldServicesViewModel(app = get(), environmentRepository = get(), activityViewModel = activityViewModel, dispatcherProvider = get())
        }
        viewModel { (activityViewModel: MainActivityViewModel) ->
            LoginViewModel(
                app = get(),
                userRepo = get(),
                forceCrashLogic = get(),
                buildConfigProvider = get(),
                dispatcherProvider = get(),
                toaster = get(),
                activityViewModel = activityViewModel,
                networkAvailabilityManager = get(),
            )
        }
        viewModel { (activityViewModel: MainActivityViewModel) ->
            HomeViewModel(
                app = get(),
                userRepo = get(),
                apsRepo = get(),
                pickRepository = get(),
                dispatcherProvider = get(),
                networkAvailabilityManager = get(),
                completeHandoffUseCase = get(),
                completeHandoff1PLUseCase = get(),
                activityViewModel = activityViewModel
            )
        }
        viewModel { (conversationId: String, orderId: String, fulFillmentOrderNumber: String) ->
            ChatViewModel(conversationSid = conversationId, orderId, fulFillmentOrderNumber, app = get())
        }

        viewModel {
            ChatButtonViewModel(
                app = get()
            )
        }
        viewModel {
            DevOptionsViewModel(
                app = get(),
                forceCrashLogicImpl = get(),
                environmentRepository = get(),
                userRepository = get(),
                devOptionsRepositoryWriter = get(),
                buildConfigProvider = get(),
                dispatcherProvider = get(),
            )
        }
        viewModel { (activityViewModel: MainActivityViewModel) ->
            PickListItemsViewModel(
                app = get(),
                activityViewModel = activityViewModel,
            )
        }
        viewModel { (activityViewModel: MainActivityViewModel) ->
            OpenPickListsViewModel(
                app = get(),
                activityViewModel = activityViewModel,
            )
        }
        viewModel { MetricsViewModel(app = get()) }
        viewModel { SettingsViewModel(app = get()) }
        viewModel { (activityViewModel: MainActivityViewModel) -> ArrivalsPagerViewModel(app = get(), activityViewModel = activityViewModel) }
        viewModel { (activityViewModel: MainActivityViewModel) -> ArrivalsViewModel(app = get(), activityViewModel = activityViewModel) }
        viewModel { ArrivalsOptionsViewModel(app = get()) }
        viewModel { RemoveRejected1PLViewModel(app = get()) }
        viewModel { HandOff1PLViewModel(app = get(), completeHandoff1PLUseCase = get()) }
        viewModel { (rejectedItemsByZone: RejectedItemsByZone/*, vanId: String*/) ->
            RemoveRejected1PLItemsViewModel(app = get(), rejectedItemsByZone/*, vanId*/)
        }
        viewModel { (activityViewModel: MainActivityViewModel) ->
            ItemDetailsViewModel(
                app = get(),
                pickRepository = get(),
                activityViewModel = activityViewModel,
                dispatcherProvider = get(),
                toaster = get(),
                barcodeMapper = get(),
                devOptionsRepository = get(),
                networkAvailabilityManager = get(),
                userRepository = get(),
                siteRepository = get()
            )
        }
        viewModel { BoxInputWeightViewModel(app = get()) }
        viewModel { WineStaging2ViewModel(app = get()) }
        viewModel { WineStaging3ViewModel(app = get()) }
        viewModel { WineStagingViewModel(app = get()) }
        viewModel { OrderSummaryViewModel(app = get()) }
        viewModel { PrescriptionReturnViewModel(app = get()) }
        viewModel { ManualEntryPharmacyViewModel(app = get()) }
        viewModel { DestageOrderViewModel(app = get()) }
        viewModel { ProfileViewModel(app = get()) }
        viewModel { CustomDialogViewModel(app = get()) }
        viewModel { BottomSheetDialogViewModel(app = get()) }
        viewModel { PickListPagerViewModel(app = get()) }
        viewModel { (activityViewModel: MainActivityViewModel) ->
            TeamPickListsViewModel(
                app = get(),
                activityViewModel = activityViewModel,
            )
        }

        viewModel { ManualEntryPagerViewModel(app = get()) }
        viewModel { ManualEntryUpcViewModel(app = get()) }
        viewModel { ManualEntryPluViewModel(app = get()) }
        viewModel { ManualEntryWeightViewModel(app = get()) }
        viewModel { ManualEntryStagingViewModel(app = get()) }
        viewModel { ManualEntryStagingMfcViewModel(app = get()) }
        viewModel { ManualEntryHandOffViewModel(app = get()) }
        viewModel { ManualEntryHandOffMfcViewModel(app = get()) }
        viewModel { ManualEntryToolTipViewModel(app = get()) }
        viewModel { VerificationCodeToolTipViewModel(app = get()) }
        viewModel { BagsPerTempZoneViewModel(app = get()) }

        viewModel { StagingViewModel(app = get()) }

        viewModel { StagingPart2ViewModel(app = get()) }
        viewModel { AddBagsViewModel(app = get()) }

        viewModel { PicklistSummaryViewModel(app = get()) }

        viewModel { UnAssignTotesViewModel(app = get()) }

        viewModel { (handle: SavedStateHandle) ->
            StoresViewModel(
                stateHandle = handle,
                app = get(),
                userRepo = get(),
                dispatcherProvider = get(),
            )
        }
        viewModel { (activityViewModel: MainActivityViewModel) ->
            TotesViewModel(app = get(), activityViewModel = activityViewModel, pickRepository = get(), dispatcherProvider = get())
        }
        viewModel { ItemPhotoViewModel(get()) }
        viewModel { ChatImagePreviewViewModel(get()) }
        viewModel { (activityViewModel: MainActivityViewModel, handle: SavedStateHandle) ->
            SubstituteViewModel(
                stateHandle = handle,
                app = get(),
                barcodeMapper = get(),
                pickRepo = get(),
                userRepo = get(),
                networkAvailabilityManager = get(),
                dispatcherProvider = get(),
                toaster = get(),
                activityViewModel = activityViewModel
            )
        }

        viewModel { QuantityPickerDialogViewModel(app = get()) }
        viewModel { SubstituteConfirmationViewModel(app = get()) }

        viewModel { ActionSheetBottomSheetViewModel(app = get()) }
        viewModel { BulkSubstituteConfirmationViewModel(app = get()) }

        viewModel { AddCustomerViewModel(app = get()) }
        viewModel { ChangeCustomerStatusViewModel(app = get()) }
        viewModel { AlternativeLocationViewModel(app = get()) }
        viewModel { DestagingDialogViewmodel(app = get()) }
        viewModel { ConfirmAmountViewModel(app = get()) }
        viewModel { ToolTipViewModel(app = get()) }
        viewModel { VerificationIdTypeViewModel(app = get()) }
        viewModel { VerificationManualEntryViewModel(app = get()) }
        viewModel { HandOffVerificationSharedViewModel(app = get()) }
        viewModel { IdentificationBarcodeScanViewModel(app = get()) }
        viewModel { CustomerSignatureViewModel(app = get()) }
        viewModel { SwapSubstitutionViewModel(app = get()) }
        viewModel { MissingItemLocationViewModel(app = get()) }
        viewModel { WhereToFindLocationViewModel(app = get()) }
        viewModel { AuthCodeVerificationViewModel(app = get()) }
        viewModel { HandOffRemoveItemsViewModel(app = get()) }
        viewModel { QuickTaskPagerViewModel(app = get()) }
        viewModel { QuickTaskOtherShoppersitemsViewModel(app = get()) }
        viewModel { QuickTaskMyItemsViewModel(app = get()) }
        viewModel { MyScoreViewModel(app = get(), repo = get(), dispatcherProvider = get()) }
        single { AnalyticsHelper(sharedPreferences = get(named(KoinNamedSharedPreferences.AutoLogout))) }
        single<BuildConfigProvider> { BuildConfigProviderImpl() }
        single<AutoLogoutLogic> {
            AutoLogoutLogicImpl(
                userRepository = get(), sharedPreferences = get(named(KoinNamedSharedPreferences.AutoLogout)),
                loginAnalyticsRepository = get(), devOptionsRepositoryWriter = get()
            )
        }
        single<Toaster> { ToasterImplementation(app = get()) }
        single { Picasso.get() }
        single<ConversationsClientWrapper> { ConversationsClientWrapperImp(applicationContext = get(), osccRepository = get(), acuPickLogger = get(), moshi = get()) }
        single { KoinBindingAdapters(picasso = get()) }
        single { AlbertsonsDataBindingComponent(koinBindingAdaptersArg = get()) }
        single<ImagePreCacher> { ImagePreCacherImplementation(picasso = get()) }
        single { UserFeedback(context = get()) }
        single { AppDynamicsConfig(app = get(), buildConfigProvider = get()) }
        single<AcuPickLoggerInterface> { AcuPickLoggerImpl(appDynamicsConfig = get()) }
        single<FirebaseAnalyticsInterface> { FirebaseAnalyticsImpl() }
        single<LoggingDataProvider> { LoggingDataProviderImpl(app = get()) }
        single { Configuration() }
        single { Settings(Repository(context = get())) }
        single { VendorService(context = get()) }
        single {
            WiFiManagerWrapper(
                context = get(),
                wifiManager = androidContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
            )
        }
    }
}
