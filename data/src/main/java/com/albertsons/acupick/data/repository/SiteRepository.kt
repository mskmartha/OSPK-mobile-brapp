package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.response.DigitizedAgeFlags
import com.albertsons.acupick.data.model.response.NotificationEtaTime
import com.albertsons.acupick.data.model.response.PrePickFeatureFlag
import com.albertsons.acupick.data.model.response.SiteDetailsDto
import com.albertsons.acupick.data.model.response.SiteType
import com.albertsons.acupick.data.model.response.StagingType
import com.albertsons.acupick.data.model.response.TwoWayCommsFlags
import com.albertsons.acupick.data.model.wrapExceptions
import com.albertsons.acupick.data.network.ApsService
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.orZero
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Response
import timber.log.Timber.Forest.e

/**
 * Provides the ability to save details of the user's current site/store and the APIs to retrieve them
 *
 * Can be expanded in the future to persist multiple sites/stores
 */
interface SiteRepository : Repository {
    val siteDetails: StateFlow<SiteDetailsDto?>
    suspend fun getSiteDetails(siteId: String, siteType: SiteType? = null): ApiResult<List<SiteDetailsDto>>
    val isMFCSite: Boolean
    val isReshopAllowed: Boolean
    val isFlashInterjectionEnabled: Boolean
    val isFlashOrderEnabled: Boolean
    val stagingType: StagingType
    val handoffDelayTime: Int
    val areIssueScanningFeaturesEnabled: Boolean
    val concernTime: Long
    val warningTime: Long
    val isDigitizeAgeVerificationEnabled: Boolean
    val isCustomerApprovedSubstitutionEnabled: Boolean
    val fixedItemTypesEnabled: Boolean
    val isHybridPickingDialogEnabled: Boolean
    val isDarkStoreEnabled: Boolean
    val isWineFulfillment: Boolean
    val isRxDugEnabled: Boolean
    val isCctEnabled: Boolean
    val isAgeVerificationCameraEnabled: Boolean
    val digitizeAgeFlags: DigitizedAgeFlags
    val twoWayCommsFlags: TwoWayCommsFlags
    val isDugInterjectionEnabled: Boolean
    val notificationEtaTime: NotificationEtaTime
    val isEtaArrivalEnabled: Boolean?
    val locationFullEnabled: Boolean
    val isCas1PL: Boolean
    val weightedItemThreshold: Double
    val isDisplayType3PWEnabled: Boolean
    val realTimeSubDelay: Int
    val prePickFeatureFlag: PrePickFeatureFlag
    val isAutoInitiated: Boolean
    val prepNotReadyDelayTime: Int
}

internal class SiteRepositoryImplementation(
    private val apsService: ApsService,
    private val responseToApiResultMapper: ResponseToApiResultMapper,
    private val sharedPrefs: SharedPreferences,
    moshi: Moshi,
) : SiteRepository {

    companion object {
        private const val SITE_DETAILS = "SiteDetails"
        private const val DEFAULT_TIME_DELAY = 0
    }

    private val SITE_ADAPTER: JsonAdapter<SiteDetailsDto> by lazy { moshi.adapter(SiteDetailsDto::class.java) }

    private val _siteDetails = MutableStateFlow(loadSiteDetails())

    override val siteDetails: StateFlow<SiteDetailsDto?>
        get() = _siteDetails

    override suspend fun getSiteDetails(siteId: String, siteType: SiteType?): ApiResult<List<SiteDetailsDto>> {
        val result = wrapExceptions("SiteRepository", "getSiteDetails") {
            apsService.getSiteDetails(siteId, siteType).toResult()
        }
        when (result) {
            is ApiResult.Success -> {
                result.data.firstOrNull()?.let { siteDetails ->
                    storeSiteDetails(siteDetails)
                    _siteDetails.value = siteDetails
                }
            }

            is ApiResult.Failure -> Unit // no-op
        }.exhaustive
        return result
    }

    override val isMFCSite: Boolean
        get() = siteDetails.value?.mfc ?: false

    override val isReshopAllowed: Boolean
        get() = siteDetails.value?.reshop ?: false

    override val isFlashInterjectionEnabled: Boolean
        get() = siteDetails.value?.isFlashInterjectionEnabled ?: false

    override val isFlashOrderEnabled: Boolean
        get() = siteDetails.value?.isFlashOrderEnabled ?: false

    override val stagingType: StagingType
        get() = siteDetails.value?.stagingType ?: StagingType.TEMP_ZONE

    override val handoffDelayTime: Int
        get() = siteDetails.value?.handoffDelayTime ?: DEFAULT_TIME_DELAY

    override val areIssueScanningFeaturesEnabled: Boolean
        get() = siteDetails.value?.issueScanning ?: false

    override val concernTime: Long
        get() = siteDetails.value?.concernTime?.toLong() ?: 60000

    override val warningTime: Long
        get() = siteDetails.value?.warningTime?.toLong() ?: 720000

    override val isDigitizeAgeVerificationEnabled: Boolean
        get() = siteDetails.value?.isDigitizeAgeVerificationEnabled ?: false

    override val isCustomerApprovedSubstitutionEnabled: Boolean
        get() = siteDetails.value?.isCustomerApprovedSubstitutionEnabled ?: false

    override val fixedItemTypesEnabled: Boolean
        get() = siteDetails.value?.fixedItemTypesEnabled ?: false

    override val isHybridPickingDialogEnabled: Boolean
        get() = false

    override val isDarkStoreEnabled: Boolean
        get() = siteDetails.value?.isDarkStoreEnabled ?: false

    override val isWineFulfillment: Boolean
        get() = siteDetails.value?.isWineFulfillment ?: false

    override val isRxDugEnabled: Boolean
        get() = siteDetails.value?.isRxDugEnabled ?: false

    override val isAgeVerificationCameraEnabled: Boolean
        get() = siteDetails.value?.isAgeVerificationCameraEnabled ?: false

    override val isCctEnabled: Boolean
        get() = siteDetails.value?.isCctEnabled ?: false

    override val twoWayCommsFlags: TwoWayCommsFlags
        get() = siteDetails.value?.twoWayCommsFlags ?: TwoWayCommsFlags()

    override val digitizeAgeFlags: DigitizedAgeFlags
        get() = siteDetails.value?.digitizeAgeFlags ?: DigitizedAgeFlags()

    override val isDugInterjectionEnabled: Boolean
        get() = siteDetails.value?.dugInterjectionEnabled ?: false

    // TODO: Use siteDetails.value?.firstNotificationETATime once backend is deployed
    override val notificationEtaTime: NotificationEtaTime
        get() = siteDetails.value?.notificationEtaTime ?: NotificationEtaTime()

    // TODO: siteDetails.value?.secondNotificationETATime once backend is deployed
    // override val secondNotificationETATime: Int
    //     get() = siteDetails.value?.secondNotificationETATime ?: 4

    override val isEtaArrivalEnabled: Boolean?
        get() = siteDetails.value?.isEtaArrivalEnabled

    override val realTimeSubDelay: Int
        get() = siteDetails.value?.realTimeSubDelay ?: 0
    override val isAutoInitiated: Boolean
        get() = siteDetails.value?.prePickFeatureFlag?.autoInitiate == "true" || siteDetails.value?.prePickFeatureFlag?.b2BAutoInitiate == "true"
    override val prepNotReadyDelayTime: Int
        get() = siteDetails.value?.prepNotReadyDelayTime ?: 0

    override val locationFullEnabled: Boolean
        get() = siteDetails.value?.islocationFullEnabled ?: false

    override val isCas1PL: Boolean
        get() = siteDetails.value?.isCas1PL ?: false
    override val weightedItemThreshold: Double
        get() = siteDetails.value?.weightedItemLimit.orZero()

    override val isDisplayType3PWEnabled: Boolean
        get() = siteDetails.value?.weightedItemLimit.orZero() > 0.0

    override val prePickFeatureFlag: PrePickFeatureFlag
        get() = siteDetails.value?.prePickFeatureFlag ?: PrePickFeatureFlag()

    private fun loadSiteDetails(): SiteDetailsDto? {
        val siteDetailsJson = sharedPrefs.getString(SITE_DETAILS, null)
        return if (siteDetailsJson.isNotNullOrEmpty()) {
            try {
                SITE_ADAPTER.fromJson(siteDetailsJson!!)
            } catch (e: JsonDataException) {
                e("[loadSiteDetails] Error when de-serializing site details from JSON")
                null
            }
        } else {
            e("[loadSiteDetails] SiteRepository could not load site details")
            null
        }
    }

    private fun storeSiteDetails(siteDetails: SiteDetailsDto) {
        sharedPrefs.edit().putString(SITE_DETAILS, SITE_ADAPTER.toJson((siteDetails))).commit()
    }

    private fun <T : Any> Response<T>.toResult(): ApiResult<T> {
        return responseToApiResultMapper.toResult(this)
    }
}
