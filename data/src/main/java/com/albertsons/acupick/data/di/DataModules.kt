package com.albertsons.acupick.data.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.albertsons.acupick.config.api.ConfigApiImpl
import com.albertsons.acupick.config.api.ConfigApi
import com.albertsons.acupick.data.crashreporting.ForceCrashLogic
import com.albertsons.acupick.data.crashreporting.ForceCrashLogicImpl
import com.albertsons.acupick.data.environment.EnvironmentRepository
import com.albertsons.acupick.data.environment.EnvironmentRepositoryImpl
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.BoxTypeDto
import com.albertsons.acupick.data.model.CartType
import com.albertsons.acupick.data.model.ContainerActivityStatus
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.HandshakeType
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.ResponseToApiResultMapperImplementation
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.ShortReasonCode
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.SubReasonCode
import com.albertsons.acupick.data.model.SubstitutionCodeAdapter
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeMapperImplementation
import com.albertsons.acupick.data.model.picklistprocessor.PickListProcessor
import com.albertsons.acupick.data.model.picklistprocessor.PickListProcessorImplementation
import com.albertsons.acupick.data.model.request.RxOrderStatus
import com.albertsons.acupick.data.model.response.AddressDto
import com.albertsons.acupick.data.model.response.CustomerDto
import com.albertsons.acupick.data.model.response.InventoryAttributeDto
import com.albertsons.acupick.data.model.response.RelationshipType
import com.albertsons.acupick.data.model.response.ServiceType
import com.albertsons.acupick.data.model.response.SiteType
import com.albertsons.acupick.data.model.response.Title
import com.albertsons.acupick.data.network.ApsService
import com.albertsons.acupick.data.network.ConfigService
import com.albertsons.acupick.data.network.IsoZonedDateTimeJsonAdapter
import com.albertsons.acupick.data.network.ItemProcessorService
import com.albertsons.acupick.data.network.NetworkAvailabilityController
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.network.NetworkAvailabilityManagerImplementation
import com.albertsons.acupick.data.network.OsccService
import com.albertsons.acupick.data.network.ServerErrorCodeAdapter
import com.albertsons.acupick.data.repository.CredentialsRepository
import com.albertsons.acupick.data.network.auth.token.TokenAuthInterceptor
import com.albertsons.acupick.data.network.auth.token.TokenAuthService
import com.albertsons.acupick.data.network.logging.HeaderInterceptor
import com.albertsons.acupick.data.network.logging.OSCCHeaderInterceptor
import com.albertsons.acupick.data.picklist.InvalidItemScanTracker
import com.albertsons.acupick.data.picklist.InvalidItemScanTrackerImplementation
import com.albertsons.acupick.data.picklist.PickListOperations
import com.albertsons.acupick.data.picklist.PickListOperationsImplementation
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.ApsRepositoryImplementation
import com.albertsons.acupick.data.repository.ArrivalsRepository
import com.albertsons.acupick.data.repository.ArrivalsRepositoryImplementation
import com.albertsons.acupick.data.repository.CompleteHandoff1PLRepository
import com.albertsons.acupick.data.repository.CompleteHandoff1PLRepositoryImplementation
import com.albertsons.acupick.data.repository.CompleteHandoffRepository
import com.albertsons.acupick.data.repository.CompleteHandoffRepositoryImplementation
import com.albertsons.acupick.data.repository.ConfigRepository
import com.albertsons.acupick.data.repository.ConfigRepositoryImpl
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.ConversationsRepositoryImpl
import com.albertsons.acupick.data.repository.DevOptionsRepository
import com.albertsons.acupick.data.repository.DevOptionsRepositoryImplementation
import com.albertsons.acupick.data.repository.DevOptionsRepositoryWriter
import com.albertsons.acupick.data.repository.IdRepository
import com.albertsons.acupick.data.repository.IdRepositoryImplementation
import com.albertsons.acupick.data.repository.ItemProcessorRepository
import com.albertsons.acupick.data.repository.ItemProcessorRepositoryImpl
import com.albertsons.acupick.data.repository.LoginLogoutAnalyticsRepository
import com.albertsons.acupick.data.repository.LoginLogoutAnalyticsRepositoryImplementation
import com.albertsons.acupick.data.repository.LogoutLocalDataStorage
import com.albertsons.acupick.data.repository.LogoutLocalDataStorageImpl
import com.albertsons.acupick.data.repository.MessagesRepository
import com.albertsons.acupick.data.repository.MessagesRepositoryImpl
import com.albertsons.acupick.data.repository.OsccRepository
import com.albertsons.acupick.data.repository.OsccRepositoryImpl
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.PickRepositoryImplementation
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepositoryImplementation
import com.albertsons.acupick.data.repository.RemoveItemRepositoryImplementation
import com.albertsons.acupick.data.repository.RemoveItemsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.SiteRepositoryImplementation
import com.albertsons.acupick.data.repository.StagingStateRepository
import com.albertsons.acupick.data.repository.StagingStateRepositoryImplementation
import com.albertsons.acupick.data.repository.TokenizedLdapRepository
import com.albertsons.acupick.data.repository.TokenizedLdapRepositoryImplementation
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.repository.UserRepositoryImplementation
import com.albertsons.acupick.data.repository.WineShippingRepository
import com.albertsons.acupick.data.repository.WineShippingRepositoryImplementation
import com.albertsons.acupick.data.repository.WineShippingStageStateRepository
import com.albertsons.acupick.data.repository.WineShippingStageStateRepositoryImplementation
import com.albertsons.acupick.infrastructure.coroutine.AlbApplicationCoroutineScope
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProviderImpl
import com.albertsons.acupick.infrastructure.utils.ObfuscatedKey
import com.albertsons.acupick.usecase.handoff.CompleteHandoff1PLUseCase
import com.albertsons.acupick.usecase.handoff.CompleteHandoffUseCase
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.SupervisorJob
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** General app configuration (repositories/viewmodels/etc) */
object Data {
    val dataModule = module {
        single { AlbApplicationCoroutineScope(SupervisorJob() + get<DispatcherProvider>().Default + CoroutineName("albApplicationScope")) }
        single<DispatcherProvider> { DispatcherProviderImpl() }
        single<Moshi> {
            Moshi.Builder()
                .add(ZonedDateTime::class.java, IsoZonedDateTimeJsonAdapter().nullSafe())
                // Allows non-matching strings to return nullable fallback enum values when converted to the enum
                // nullSafe() allows null in the json response to be a null object (if defined as such in the DTO)
                .add(ActivityType::class.java, EnumJsonAdapter.create(ActivityType::class.java).withUnknownFallback(null).nullSafe())
                .add(ServerErrorCodeAdapter())
                .add(SubstitutionCodeAdapter())
                .add(ActivityStatus::class.java, EnumJsonAdapter.create(ActivityStatus::class.java).withUnknownFallback(null).nullSafe())
                .add(CartType::class.java, EnumJsonAdapter.create(CartType::class.java).withUnknownFallback(null).nullSafe())
                .add(RxOrderStatus::class.java, EnumJsonAdapter.create(RxOrderStatus::class.java).withUnknownFallback(null).nullSafe())
                .add(FulfillmentType::class.java, EnumJsonAdapter.create(FulfillmentType::class.java).withUnknownFallback(null).nullSafe())
                .add(FulfillmentSubType::class.java, EnumJsonAdapter.create(FulfillmentSubType::class.java).withUnknownFallback(null).nullSafe())
                .add(ContainerActivityStatus::class.java, EnumJsonAdapter.create(ContainerActivityStatus::class.java).withUnknownFallback(null).nullSafe())
                .add(Title::class.java, EnumJsonAdapter.create(Title::class.java).withUnknownFallback(null).nullSafe())
                .add(HandshakeType::class.java, EnumJsonAdapter.create(HandshakeType::class.java).withUnknownFallback(null).nullSafe())
                .add(InventoryAttributeDto.State::class.java, EnumJsonAdapter.create(InventoryAttributeDto.State::class.java).withUnknownFallback(null).nullSafe())
                .add(InventoryAttributeDto.SupplyBucket::class.java, EnumJsonAdapter.create(InventoryAttributeDto.SupplyBucket::class.java).withUnknownFallback(null).nullSafe())
                .add(AddressDto.Type::class.java, EnumJsonAdapter.create(AddressDto.Type::class.java).withUnknownFallback(null).nullSafe())
                .add(CustomerDto.Type::class.java, EnumJsonAdapter.create(CustomerDto.Type::class.java).withUnknownFallback(null).nullSafe())
                .add(CustomerDto.Segment::class.java, EnumJsonAdapter.create(CustomerDto.Segment::class.java).withUnknownFallback(null).nullSafe())
                .add(StorageType::class.java, EnumJsonAdapter.create(StorageType::class.java).withUnknownFallback(null).nullSafe())
                .add(SellByType::class.java, EnumJsonAdapter.create(SellByType::class.java).withUnknownFallback(null).nullSafe())
                .add(ShortReasonCode::class.java, EnumJsonAdapter.create(ShortReasonCode::class.java).withUnknownFallback(null).nullSafe())
                .add(CustomerArrivalStatus::class.java, EnumJsonAdapter.create(CustomerArrivalStatus::class.java).withUnknownFallback(null).nullSafe())
                .add(ContainerType::class.java, EnumJsonAdapter.create(ContainerType::class.java).withUnknownFallback(null).nullSafe())
                .add(SiteType::class.java, EnumJsonAdapter.create(SiteType::class.java).withUnknownFallback(null).nullSafe())
                .add(RelationshipType::class.java, EnumJsonAdapter.create(RelationshipType::class.java).withUnknownFallback(null).nullSafe())
                .add(ServiceType::class.java, EnumJsonAdapter.create(ServiceType::class.java).withUnknownFallback(null).nullSafe())
                .add(SubReasonCode::class.java, EnumJsonAdapter.create(SubReasonCode::class.java).withUnknownFallback(null).nullSafe())
                .add(BoxTypeDto::class.java, EnumJsonAdapter.create(BoxTypeDto::class.java).withUnknownFallback(null).nullSafe())
                .build()
        }
        single<ConfigRepository> {
            ConfigRepositoryImpl(
                coroutineScope = get(),
                configService = get(),
                context = androidContext(),
                responseToApiResultMapper = get()
            )
        }
        single<ConfigApi> { ConfigApiImpl() }
        single<ApsRepository> { ApsRepositoryImplementation(apsService = get(), pickRepository = get(), itemProcessorRepository = get(), responseToApiResultMapper = get()) }
        single<WineShippingRepository> { WineShippingRepositoryImplementation(apsService = get(), responseToApiResultMapper = get()) }
        single<ConversationsRepository> { ConversationsRepositoryImpl(conversationsClientWrapper = get(), dispatchers = get(), siteRepository = get(), moshi = get()) }
        single<OsccRepository> { OsccRepositoryImpl(osccService = get(), responseToApiResultMapper = get()) }
        single<MessagesRepository> {
            MessagesRepositoryImpl(
                conversationsClient = get(),
                conversationsRepository = get(),
                networkAvailabilityManager = get(),
                dispatchers = get(),
                siteRepository = get()
            )
        }
        single<ItemProcessorRepository> {
            ItemProcessorRepositoryImpl(
                itemProcessorService = get(),
                offlineMissingLocFile = get(named(KoinNamedFiles.OfflineMissingLoc)),
                networkAvailabilityManager = get(),
                dispatcherProvider = get(),
                albApplicationCoroutineScope = get(),
                moshi = get(),
                responseToApiResultMapper = get()
            )
        }
        single<LoginLogoutAnalyticsRepository> {
            LoginLogoutAnalyticsRepositoryImplementation(
                apsService = get(),
                logoutLocalDataStorage = get(),
                sharedPreferences = get(named(KoinNamedSharedPreferences.LogInLogOut)),
                userRepository = get()
            )
        }
        single<UserRepository> {
            UserRepositoryImplementation(
                tokenAuthService = get(),
                configRepository = get(),
                credentialsRepository = get(),
                pickRepository = get(),
                itemProcessorRepository = get(),
                responseToApiResultMapper = get(),
                dispatcherProvider = get(),
                pushNotificationsRepository = get(),
                chatRepository = get(),
                loggingDataProvider = get(),
            )
        }
        single<TokenizedLdapRepository> {
            TokenizedLdapRepositoryImplementation(
                tokenAuthService = get(),
                responseToApiResultMapper = get(),
                sharedPrefs = get(named(KoinNamedSharedPreferences.TokenizedLdapId))
            )
        }
        single<EnvironmentRepository> { EnvironmentRepositoryImpl(sharedPrefs = get(named(KoinNamedSharedPreferences.Environment)), buildConfigProvider = get()) }
        single<IdRepository> { IdRepositoryImplementation(moshi = get(), sharedPrefs = get(named(KoinNamedSharedPreferences.Idinfo))) }
        single<PickRepository> {
            PickRepositoryImplementation(
                app = get(),
                moshi = get(),
                offlinePickFile = get(named(KoinNamedFiles.OfflinePick)),
                apsService = get(),
                barcodeMapper = get(),
                pickListProcessor = get(),
                pickListOperations = get(),
                responseToApiResultMapper = get(),
                devOptionsRepository = get(),
                networkAvailabilityManager = get(),
                toaster = get(),
                dispatcherProvider = get(),
                albApplicationCoroutineScope = get(),
                siteRepository = get(),
                conversationsRepository = get(),
                conversationsClientWrapper = get(),
                messagesRepository = get(),
                osccRepository = get(),
                itemProcessorRepository = get(),
                sharedPrefs = get(named(KoinNamedSharedPreferences.LastSubOrOutOfStockTime)),
            )
        }
        single<StagingStateRepository> {
            StagingStateRepositoryImplementation(
                moshi = get(),
                sharedPrefs = get(named(KoinNamedSharedPreferences.Staging))
            )
        }
        single<WineShippingStageStateRepository> {
            WineShippingStageStateRepositoryImplementation(
                moshi = get(),
                sharedPrefs = get(named(KoinNamedSharedPreferences.WineShipping))
            )
        }
        single<CompleteHandoffRepository> {
            CompleteHandoffRepositoryImplementation(
                moshi = get(),
                sharedPrefs = get(named(KoinNamedSharedPreferences.CompleteHandoff))
            )
        }
        single<CompleteHandoff1PLRepository> {
            CompleteHandoff1PLRepositoryImplementation(
                moshi = get(),
                sharedPrefs = get(named(KoinNamedSharedPreferences.Complete1PLHandoff))
            )
        }
        single<RemoveItemsRepository> {
            RemoveItemRepositoryImplementation(
                moshi = get(),
                encryptedSharedPreferences = get(named(KoinNamedSharedPreferences.RemoveItems)),
                userRepository = get(),
                apsRepository = get(),
                networkAvailabilityManager = get(),
                dispatcherProvider = get(),
                albApplicationCoroutineScope = get(),
                acuPickLogger = get()
            )
        }
        single<LogoutLocalDataStorage> {
            LogoutLocalDataStorageImpl(
                moshi = get(),
                sharedPreferences = get(named(KoinNamedSharedPreferences.LogInLogOut))
            )
        }
        single<SiteRepository> {
            SiteRepositoryImplementation(
                apsService = get(),
                responseToApiResultMapper = get(),
                sharedPrefs = get(named(KoinNamedSharedPreferences.Site)),
                moshi = get(),
            )
        }
        single<PushNotificationsRepository> {
            PushNotificationsRepositoryImplementation(
                apsRepository = get(),
                sharedPrefs = get(named(KoinNamedSharedPreferences.PushNotifications)),
                loggingDataProvider = get(),
            )
        }
        single<ArrivalsRepository> {
            ArrivalsRepositoryImplementation(
                sharedPrefs = get(named(KoinNamedSharedPreferences.Arrivals)),
            )
        }
        single<ForceCrashLogic> { ForceCrashLogicImpl(buildConfigProvider = get()) }
        factory<InvalidItemScanTracker> { InvalidItemScanTrackerImplementation() }
        single<BarcodeMapper> { BarcodeMapperImplementation() }
        single<PickListProcessor> { PickListProcessorImplementation(barcodeMapper = get(), pickListOperations = get()) }
        single<PickListOperations> { PickListOperationsImplementation(siteRepo = get()) }
        single<ResponseToApiResultMapper> { ResponseToApiResultMapperImplementation(moshi = get()) }
        // Binds to both types listed inside binds: https://doc.insert-koin.io/#/koin-core/definitions?id=additional-type-binding
        single { NetworkAvailabilityManagerImplementation() } binds (arrayOf(NetworkAvailabilityManager::class, NetworkAvailabilityController::class))
        // Binds to both types listed inside binds: https://doc.insert-koin.io/#/koin-core/definitions?id=additional-type-binding
        single {
            DevOptionsRepositoryImplementation(sharedPrefs = get(named(KoinNamedSharedPreferences.Environment)), buildConfigProvider = get())
        } binds (arrayOf(DevOptionsRepository::class, DevOptionsRepositoryWriter::class))
        single { CredentialsRepository(encryptedSharedPrefs = get(named(KoinNamedSharedPreferences.Credentials)), moshi = get()) }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.Environment)) {
            androidContext().getSharedPreferences("dev_options_prefs", Context.MODE_PRIVATE)
        }
        single(named(KoinNamedSharedPreferences.LogInLogOut)) {
            androidContext().getSharedPreferences("log_in_log_out_prefs", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.AutoLogout)) {
            androidContext().getSharedPreferences("auto_logout_prefs", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.DevOptions)) {
            androidContext().getSharedPreferences("dev_options_prefs", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.Staging)) {
            androidContext().getSharedPreferences("staging_prefs", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.WineShipping)) {
            androidContext().getSharedPreferences("wineShipping_prefs", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.CompleteHandoff)) {
            androidContext().getSharedPreferences("compete_handoff_prefs", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.Complete1PLHandoff)) {
            androidContext().getSharedPreferences("compete_1pl_handoff_prefs", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.Site)) {
            androidContext().getSharedPreferences("site_details", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.TokenizedLdapId)) {
            androidContext().getSharedPreferences("ldap_token", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.PushNotifications)) {
            androidContext().getSharedPreferences("push_notifications", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.Arrivals)) {
            androidContext().getSharedPreferences("arrivals", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.CompleteHandoffNotifications)) {
            androidContext().getSharedPreferences("complete_handoff_notifications", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.Idinfo)) {
            androidContext().getSharedPreferences("complete_handoff_id_info", Context.MODE_PRIVATE)
        }
        single<SharedPreferences>(named(KoinNamedSharedPreferences.LastSubOrOutOfStockTime)) {
            androidContext().getSharedPreferences("picking_last_sub_or_oos_time", Context.MODE_PRIVATE)
        }
        single(named(KoinNamedSharedPreferences.Credentials)) {
            EncryptedSharedPreferences.create(
                "secureBbCredentials",
                MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                androidContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        }
        single(named(KoinNamedSharedPreferences.PickUpUser)) {
            EncryptedSharedPreferences.create(
                "securePickUpUserInfo",
                MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                androidContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        }
        single(named(KoinNamedSharedPreferences.RemoveItems)) {
            EncryptedSharedPreferences.create(
                "secureRemoveItemsInfo",
                MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                androidContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        }
        single<File>(named(KoinNamedFiles.OfflinePick)) {
            File(androidContext().filesDir, "offline_picks_data.json")
        }
        single<File>(named(KoinNamedFiles.OfflineMissingLoc)) {
            File(androidContext().filesDir, "offline_missing_item_loc.json")
        }
        single { CompleteHandoffUseCase(get(), get(), get(), get()) }
        single { CompleteHandoff1PLUseCase(get(), get(), get()) }
    }
}

/** Allows multiple types of [SharedPreferences] to co-exist in the koin graph */
enum class KoinNamedSharedPreferences {
    Credentials, Environment, Idinfo, AutoLogout, DevOptions, Staging, WineShipping, Site, PushNotifications, Arrivals, CompleteHandoffNotifications, LogInLogOut, Analytics, CompleteHandoff,
    Complete1PLHandoff,
    PickUpUser,
    RemoveItems,
    LastSubOrOutOfStockTime,
    TokenizedLdapId
}

/** Allows multiple types of [Files] to co-exist in the koin graph */
enum class KoinNamedFiles {
    OfflinePick, OfflineMissingLoc
}

/** Allows multiple types of network related implementations to co-exist in the koin graph */
private enum class KoinNamedNetwork {
    Authentication, APS, Config, OSCC, ItemProcessor
}

/** General network configuration */
object NetworkObject {
    val networkModule = module {
        single<OkHttpClient>(named(KoinNamedNetwork.Authentication)) {
            OkHttpClient.Builder()
                .addInterceptor(ChuckerInterceptor(androidContext()))
                .connectTimeout(3, TimeUnit.SECONDS)
                .addInterceptor(
                    HeaderInterceptor(
                        loggingDataProvider = get(),
                        acuPickLogger = get(),
                        environmentRepository = get(),
                        context = androidContext()
                    )
                )
                .build()
        }

        single<OkHttpClient>(named(KoinNamedNetwork.OSCC)) {
            OkHttpClient.Builder()
                .addInterceptor(ChuckerInterceptor(androidContext()))
                .addInterceptor(
                    OSCCHeaderInterceptor(
                        loggingDataProvider = get(),
                        acuPickLogger = get(),
                        environmentRepository = get(),
                        context = androidContext()
                    )
                )
                .connectTimeout(3, TimeUnit.SECONDS)
                .build()
        }

        single<Retrofit>(named(KoinNamedNetwork.OSCC)) {
            provideOsccRetrofit(
                get(named(KoinNamedNetwork.OSCC)),
                get(), get()
            )
        }

        single<Retrofit>(named(KoinNamedNetwork.ItemProcessor)) {
            provideItemProcessorRetrofit(
                get(named(KoinNamedNetwork.ItemProcessor)),
                get(), get()
            )
        }

        single<Retrofit>(named(KoinNamedNetwork.APS)) {
            provideApsRetrofit(
                get(named(KoinNamedNetwork.APS)),
                get(), get()
            )
        }
        single<OsccService> { provideOsccService(get(named(KoinNamedNetwork.OSCC))) }
        single<ApsService> { provideApsService(get(named(KoinNamedNetwork.APS))) }
        single<ItemProcessorService> { provideItemProcessorService(get(named(KoinNamedNetwork.ItemProcessor))) }
    }

    private fun provideApsRetrofit(okHttpClient: OkHttpClient, moshi: Moshi, environmentRepository: EnvironmentRepository): Retrofit {
        return Retrofit.Builder()
            .baseUrl(environmentRepository.selectedConfig.apsEnvironmentConfig.baseApsUrl)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private fun provideOsccRetrofit(okHttpClient: OkHttpClient, moshi: Moshi, environmentRepository: EnvironmentRepository): Retrofit {
        return Retrofit.Builder()
            .baseUrl(environmentRepository.selectedConfig.osccEnvironmentConfig.baseOsccUrl)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    private fun provideItemProcessorRetrofit(okHttpClient: OkHttpClient, moshi: Moshi, environmentRepository: EnvironmentRepository): Retrofit {
        return Retrofit.Builder()
            .baseUrl(environmentRepository.selectedConfig.itemProcessorEnvironmentConfig.baseItemProcessorUrl)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private fun provideApsService(retrofit: Retrofit): ApsService {
        return retrofit.create(ApsService::class.java)
    }

    private fun provideOsccService(retrofit: Retrofit): OsccService {
        return retrofit.create(OsccService::class.java)
    }
    private fun provideItemProcessorService(retrofit: Retrofit): ItemProcessorService {
        return retrofit.create(ItemProcessorService::class.java)
    }
}

/** Config network configuration */
object ConfigObject {
    private const val OCP_APIM_SUBSCRIPTION_KEY_NAME = "Ocp-Apim-Subscription-Key"
    private val CONFIG_PROD_SUBSCRIPTION_KEY = ObfuscatedKey().n6._f.n2._d.n1.n1.n1._d.n6.n7._b.n2.n4.n4.n9.n8.n8.n6.n0._c._b.n7._d._c.n0.n8._c.n0.n2._b.n0.n5.key
    private val CONFIG_QA1_SUBSCRIPTION_KEY = ObfuscatedKey().n9.n6.n5.n5.n0._c.n2.n0._a.n8.n4._b.n4.n6.n8.n4._b.n2._c.n9.n0.n8.n5.n8.n3.n8.n0._b.n2.n3.n0.n5.key
    val configModule = module {
        single<OkHttpClient>(named(KoinNamedNetwork.Config)) {
            OkHttpClient.Builder()
                .addInterceptor(ChuckerInterceptor(androidContext()))
                .connectTimeout(3, TimeUnit.SECONDS)
                .addInterceptor(
                    HeaderInterceptor(
                        loggingDataProvider = get(),
                        acuPickLogger = get(),
                        environmentRepository = get(),
                        context = androidContext()
                    )
                )
                .build()
        }
        single<Retrofit>(named(KoinNamedNetwork.Config)) {
            provideConfigRetrofit(
                okHttpClient = get(named(KoinNamedNetwork.Config)),
                moshi = get(),
                environmentRepository = get()
            )
        }
        single<ConfigService> {
            provideConfigService(
                get(named(KoinNamedNetwork.Config))
            )
        }
    }

    private fun provideConfigRetrofit(okHttpClient: OkHttpClient, moshi: Moshi, environmentRepository: EnvironmentRepository): Retrofit {
        return Retrofit.Builder()
            .baseUrl(environmentRepository.selectedConfig.configEnvironmentConfig.baseConfigUrl)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private fun provideConfigService(retrofit: Retrofit): ConfigService {
        return retrofit.create(ConfigService::class.java)
    }

    private fun provideHeaderInterceptor(environmentRepository: EnvironmentRepository): Interceptor {
        val isConfigProduction = environmentRepository.selectedConfig.configEnvironmentConfig.isProd

        val subscriptionKey = if (isConfigProduction) CONFIG_PROD_SUBSCRIPTION_KEY else CONFIG_QA1_SUBSCRIPTION_KEY
        return Interceptor { chain ->
            val request = chain.request()

            val newRequest = request.newBuilder()
                .addHeader(OCP_APIM_SUBSCRIPTION_KEY_NAME, subscriptionKey)
                .build()

            chain.proceed(newRequest)
        }
    }
}

/** Token auth only configuration **/
object TokenAuth {
    val tokenAuthModule = module {
        single { TokenAuthInterceptor(tokenAuthService = get(), credentialsRepo = get()) }
        single<TokenAuthService> { provideTokenAuthService(okHttpClient = get(named(KoinNamedNetwork.Authentication)), environmentRepository = get()) }
        single<OkHttpClient>(named(KoinNamedNetwork.APS)) {
            provideTokenAuthOkHttpClient(
                okHttpClient = get(named(KoinNamedNetwork.Authentication)),
                tokenAuthInterceptor = get()
            )
        }
        single<OkHttpClient>(named(KoinNamedNetwork.ItemProcessor)) {
            provideTokenAuthOkHttpClient(
                okHttpClient = get(named(KoinNamedNetwork.Authentication)),
                tokenAuthInterceptor = get()
            )
        }
    }

    private fun provideTokenAuthService(okHttpClient: OkHttpClient, environmentRepository: EnvironmentRepository): TokenAuthService {
        val retrofit = Retrofit.Builder()
            .baseUrl(environmentRepository.selectedConfig.authEnvironmentConfig.baseAuthUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        return retrofit.create(TokenAuthService::class.java)
    }

    private fun provideTokenAuthOkHttpClient(okHttpClient: OkHttpClient, tokenAuthInterceptor: TokenAuthInterceptor): OkHttpClient {
        return okHttpClient.newBuilder()
            .addInterceptor(tokenAuthInterceptor)
            .build()
    }
}
