package com.albertsons.acupick.data.environment

import android.content.SharedPreferences
import com.albertsons.acupick.data.model.ApsEnvironmentConfig
import com.albertsons.acupick.data.model.AuthEnvironmentConfig
import com.albertsons.acupick.data.test.BaseTest
import com.albertsons.acupick.data.test.MockBuildConfigProviders
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class EnvironmentRepositoryImplTest : BaseTest() {

    private val QA_BASE_APS_URL = "https://ospk.qa1.westus.aks.az.albertsons.com/ospk-services/"
    private val QA3_BASE_APS_URL = "https://ospk.qa3.westus.aks.az.albertsons.com/ospk-services/"
    private val QA7_BASE_APS_URL = "https://ospk.qa5.westus.aks.az.albertsons.com/ospk-services/"
    private val APIM_QA1_WEST_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa1int/ospkwu/pickservice/"
    private val APIM_QA2_WEST_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa2int/ospkwu/pickservice/"
    private val APIM_QA3_WEST_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa3int/ospkwu/pickservice/"
    private val APIM_QA4_WEST_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa4int/ospkwu/pickservice/"
    private val APIM_QA5_WEST_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa5int/ospkwu/pickservice/"
    private val APIM_PERF_WEST_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/perfint/ospkwu/pickservice/"
    // private val APIM_QA3_EAST_BASE_APS_URL = "https://apim-dev-02.albertsons.com/abs/qa3/pickservice/"
    // private val APIM_QA1_EAST_BASE_APS_URL = "https://esap-share-nonprod-apim-01-west-az.albertsons.com/abs/qa1int/pickservice/"
    private val CANARY_BASE_APS_URL = "https://apim-dev-01.albertsons.com/abs/perf/pickservicecanary/"
    private val PROD_CANARY_BASE_APS_URL = "https://esag-intgw-prod-westus-01.albertsons.com/abs/pilotint/ospkwu/pickservice/"
    private val DEV_BASE_APS_URL = "https://ospk.dev.westus.aks.az.albertsons.com/lospk-services/"
    private val PROD_BASE_APS_URL = "https://osco-pick-services-prod.apps.prod.stratus.albertsons.com/"
    private val APIM_PROD_BASE_APS_URL = "https://esag-intgw-prod-westus-01.albertsons.com/abs/int/ospkwu/pickservice/"
    private val APIM_PROD_EAST_BASE_APS_URL = "https://esag-intgw-prod-eastus-01.albertsons.com/abs/int/ospkeu/pickservice/"
    private val QA2_BASE_APS_URL = "https://ospk.qa2.westus.aks.az.albertsons.com/ospk-services/"

    private val QA_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa1int/ospkwu/authservice/"
    private val QA2_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa2int/ospkwu/authservice/ "
    private val QA3_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa3int/ospkwu/authservice/"
    private val QA4_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa4int/ospkwu/authservice/"
    private val QA5_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa5int/ospkwu/authservice/"
    private val PERF_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/perf1int/ospkwu/authservice/"
    // private val PROD_BASE_AUTH_URL = "https://authentication-service-prod.apps.prod.stratus.albertsons.com/"
    private val APIM_PROD_BASE_AUTH_URL = "https://esag-intgw-prod-westus-01.albertsons.com/abs/int/ospkwu/authservice/"
    private val APIM_PROD_BASE_AUTH_URL_CANARY = "https://esag-intgw-prod-westus-01.albertsons.com/abs/pilotint/ospkwu/authservice/"

    private val APS_ENVIRONMENT_DEV = ApsEnvironmentConfig(ApsEnvironmentType.DEV, DEV_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_QA = ApsEnvironmentConfig(ApsEnvironmentType.QA, QA_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_QA3 = ApsEnvironmentConfig(ApsEnvironmentType.QA3, QA3_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_QA7 = ApsEnvironmentConfig(ApsEnvironmentType.QA7, QA7_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_QA1 = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA1, APIM_QA1_WEST_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_QA2 = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA2, APIM_QA2_WEST_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_QA3 = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA3, APIM_QA3_WEST_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_QA4 = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA4, APIM_QA4_WEST_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_QA5 = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA5, APIM_QA5_WEST_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_PERF = ApsEnvironmentConfig(ApsEnvironmentType.APIM_PERF, APIM_PERF_WEST_APS_URL, false)

    // private val APS_ENVIRONMENT_APIM_QA3_EAST = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA3_EAST, APIM_QA3_EAST_BASE_APS_URL, false)

    private val APS_ENVIRONMENT_CANARY = ApsEnvironmentConfig(ApsEnvironmentType.CANARY, CANARY_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_PROD_CANARY = ApsEnvironmentConfig(ApsEnvironmentType.PRODUCTION_CANARY, PROD_CANARY_BASE_APS_URL, true)
    private val APS_ENVIRONMENT_PROD = ApsEnvironmentConfig(ApsEnvironmentType.PRODUCTION, PROD_BASE_APS_URL, true)
    private val APS_ENVIRONMENT_APIM_PROD = ApsEnvironmentConfig(ApsEnvironmentType.APIM_PRODUCTION, APIM_PROD_BASE_APS_URL, true)
    private val APS_ENVIRONMENT_APIM_PROD_EAST = ApsEnvironmentConfig(ApsEnvironmentType.APIM_PRODUCTION_EAST, APIM_PROD_EAST_BASE_APS_URL, true)
    private val APS_ENVIRONMENT_QA2 = ApsEnvironmentConfig(ApsEnvironmentType.QA2, QA2_BASE_APS_URL, false)

    private val AUTH_ENVIRONMENT_QA = AuthEnvironmentConfig(AuthEnvironmentType.QA, QA_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_QA2 = AuthEnvironmentConfig(AuthEnvironmentType.QA2, QA2_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_QA3 = AuthEnvironmentConfig(AuthEnvironmentType.QA3, QA3_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_QA4 = AuthEnvironmentConfig(AuthEnvironmentType.QA4, QA4_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_QA5 = AuthEnvironmentConfig(AuthEnvironmentType.QA5, QA5_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_PERF = AuthEnvironmentConfig(AuthEnvironmentType.PERF, PERF_BASE_AUTH_URL)
    // private val AUTH_ENVIRONMENT_PROD = AuthEnvironmentConfig(AuthEnvironmentType.PRODUCTION, PROD_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_APIM_PROD = AuthEnvironmentConfig(AuthEnvironmentType.APIM_PRODUCTION, APIM_PROD_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_PROD_CANARY = AuthEnvironmentConfig(AuthEnvironmentType.PRODUCTION_CANARY, APIM_PROD_BASE_AUTH_URL_CANARY)

    private val APS_DEV_ENVIRONMENT_CONFIGS = listOf(
        APS_ENVIRONMENT_DEV,
        APS_ENVIRONMENT_QA,
        APS_ENVIRONMENT_QA3,
        APS_ENVIRONMENT_QA7,
        APS_ENVIRONMENT_QA2,
        APS_ENVIRONMENT_APIM_QA1,
        APS_ENVIRONMENT_APIM_QA2,
        APS_ENVIRONMENT_APIM_QA3,
        APS_ENVIRONMENT_APIM_QA4,
        APS_ENVIRONMENT_APIM_QA5,
        APS_ENVIRONMENT_APIM_PERF,
        APS_ENVIRONMENT_CANARY,
        APS_ENVIRONMENT_PROD_CANARY,
        APS_ENVIRONMENT_PROD,
        APS_ENVIRONMENT_APIM_PROD,
        APS_ENVIRONMENT_APIM_PROD_EAST,
    )
    private val APS_PROD_ENVIRONMENT_CONFIGS = listOf(APS_ENVIRONMENT_APIM_PROD)
    private val APS_PROD_EAST_ENVIRONMENT_CONFIGS = listOf(APS_ENVIRONMENT_APIM_PROD_EAST)

    private val AUTH_DEV_ENVIRONMENT_CONFIGS = listOf(
        AUTH_ENVIRONMENT_QA,
        AUTH_ENVIRONMENT_QA2,
        AUTH_ENVIRONMENT_QA3,
        AUTH_ENVIRONMENT_QA4,
        AUTH_ENVIRONMENT_QA5,
        AUTH_ENVIRONMENT_PERF,
        // AUTH_ENVIRONMENT_PROD,
        AUTH_ENVIRONMENT_APIM_PROD,
        AUTH_ENVIRONMENT_PROD_CANARY
    )
    private val AUTH_PROD_ENVIRONMENT_CONFIGS = listOf(AUTH_ENVIRONMENT_APIM_PROD)

    @Test
    fun getApsEnvironments_devBuild_matchesExpectedList() {
        val sut = EnvironmentRepositoryImpl(mock {}, MockBuildConfigProviders.DEV)
        assertThat(sut.apsEnvironments).isEqualTo(APS_DEV_ENVIRONMENT_CONFIGS)
    }

    @Test
    fun getApsEnvironments_prodReleaseBuild_matchesExpectedList() {
        val sut = EnvironmentRepositoryImpl(mock {}, MockBuildConfigProviders.PROD_RELEASE)
        assertThat(sut.apsEnvironments).isEqualTo(APS_PROD_ENVIRONMENT_CONFIGS)
    }

    @Test
    fun getApsEnvironments_prodEastReleaseBuild_matchesExpectedList() {
        val sut = EnvironmentRepositoryImpl(mock {}, MockBuildConfigProviders.PROD_EAST_RELEASE)
        assertThat(sut.apsEnvironments).isEqualTo(APS_PROD_EAST_ENVIRONMENT_CONFIGS)
    }

    @Test
    fun getAuthEnvironments_devBuild_matchesExpectedList() {
        val sut = EnvironmentRepositoryImpl(mock {}, MockBuildConfigProviders.DEV)
        assertThat(sut.authEnvironments).isEqualTo(AUTH_DEV_ENVIRONMENT_CONFIGS)
    }

    @Test
    fun getAuthEnvironments_prodReleaseBuild_matchesExpectedList() {
        val sut = EnvironmentRepositoryImpl(mock {}, MockBuildConfigProviders.PROD_RELEASE)
        assertThat(sut.authEnvironments).isEqualTo(AUTH_PROD_ENVIRONMENT_CONFIGS)
    }

    @Test
    fun getAuthEnvironments_prodEastReleaseBuild_matchesExpectedList() {
        val sut = EnvironmentRepositoryImpl(mock {}, MockBuildConfigProviders.PROD_EAST_RELEASE)
        assertThat(sut.authEnvironments).isEqualTo(AUTH_PROD_ENVIRONMENT_CONFIGS)
    }

    @Test
    fun getSelectedApsConfig_prodReleaseBuild_alwaysReturnProdEnvironment() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_dev"
            },
            buildConfigProvider = MockBuildConfigProviders.PROD_RELEASE
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_APIM_PROD)
    }

    @Test
    fun getSelectedApsConfig_prodEastReleaseBuild_alwaysReturnProdEastEnvironment() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_dev"
            },
            buildConfigProvider = MockBuildConfigProviders.PROD_EAST_RELEASE
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_APIM_PROD_EAST)
    }

    @Test
    fun getSelectedApsConfig_devBuildWithDevSelected_returnsDev() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_dev"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_DEV)
    }

    @Test
    fun getSelectedApsConfig_devBuildWithQaSelected_returnsQa() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_qa"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_QA)
    }

    @Test
    fun getSelectedApsConfig_devBuildWithQa3Selected_returnsQa3() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_qa3"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_QA3)
    }

    @Test
    fun getSelectedApsConfig_devBuildWithCanarySelected_returnsCanary() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_canary"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_CANARY)
    }

    @Test
    fun getSelectedApsConfig_devBuildWithProductionCanarySelected_returnsProductionCanary() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_production_canary"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_PROD_CANARY)
    }

    @Test
    fun getSelectedApsConfig_devBuildNothingSelected_returnsFallback() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(any(), any()) } doReturn ""
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_APIM_QA3)
    }

    @Test
    fun getSelectedApsConfig_devBuildInvalidSharedPrefsValue_returnsFallback() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "asfsdf"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_APIM_QA3)
    }

    @Test
    fun getSelectedApsConfig_devBuildProductionSelected_returnsProduction() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_production"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_PROD)
    }

    @Test
    fun getSelectedApsConfig_qa3BuildProductionSelected_returnsQa3() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_apim_qa3"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_APIM_QA3)
    }

    @Test
    fun getSelectedApsConfig_qa3EastBuildProductionSelected_returnsQa3East() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_apim_qa3_east"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_APIM_QA3)
    }

    @Test
    fun getSelectedApsConfig_qa1uildProductionSelected_returnsQa1() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_aps_environment_type"), any()) } doReturn "aps_env_apim_qa1"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.apsEnvironmentConfig).isEqualTo(APS_ENVIRONMENT_APIM_QA1)
    }

    @Test
    fun getSelectedAuthConfig_prodReleaseBuild_alwaysReturnProdEnvironment() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_auth_environment_type"), any()) } doReturn "auth_env_dev"
            },
            buildConfigProvider = MockBuildConfigProviders.PROD_RELEASE
        )
        assertThat(sut.selectedConfig.authEnvironmentConfig).isEqualTo(AUTH_ENVIRONMENT_APIM_PROD)
    }

    @Test
    fun getSelectedAuthConfig_prodEastReleaseBuild_alwaysReturnProdEnvironment() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_auth_environment_type"), any()) } doReturn "auth_env_dev"
            },
            buildConfigProvider = MockBuildConfigProviders.PROD_EAST_RELEASE
        )
        assertThat(sut.selectedConfig.authEnvironmentConfig).isEqualTo(AUTH_ENVIRONMENT_APIM_PROD)
    }

    @Test
    fun getSelectedAuthConfig_devBuildWithQa3Selected_returnsFallback() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_auth_environment_type"), any()) } doReturn "auth_env_qa3"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.authEnvironmentConfig).isEqualTo(AUTH_ENVIRONMENT_QA3)
    }

    @Test
    fun getSelectedAuthConfig_devBuildNothingSelected_returnsFallback() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(any(), any()) } doReturn ""
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.authEnvironmentConfig).isEqualTo(AUTH_ENVIRONMENT_QA3)
    }

    @Test
    fun getSelectedAuthConfig_devBuildInvalidSharedPrefsValue_returnsFallback() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_auth_environment_type"), any()) } doReturn "asfsdf"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.authEnvironmentConfig).isEqualTo(AUTH_ENVIRONMENT_QA3)
    }

    @Test
    fun getSelectedAuthConfig_devBuildProductionSelected_returnsProduction() {
        val sut = EnvironmentRepositoryImpl(
            sharedPrefs = mock {
                on { getString(eq("selected_auth_environment_type"), any()) } doReturn "auth_env_apim_production"
            },
            buildConfigProvider = MockBuildConfigProviders.DEV
        )
        assertThat(sut.selectedConfig.authEnvironmentConfig).isEqualTo(AUTH_ENVIRONMENT_APIM_PROD)
    }

    @Test
    fun changeApsEnvironment_prodReleaseBuildChangeToDev_noOp() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.PROD_RELEASE)
        sut.changeApsEnvironment(ApsEnvironmentType.DEV)
        verify(mockSharedPreferencesEditor, never()).putString(any(), any())
    }

    @Test
    fun changeApsEnvironment_prodReleaseBuildChangeToQa_noOp() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.PROD_RELEASE)
        sut.changeApsEnvironment(ApsEnvironmentType.QA)
        verify(mockSharedPreferencesEditor, never()).putString(any(), any())
    }

    @Test
    fun changeApsEnvironment_prodEastReleaseBuildChangeToDev_noOp() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.PROD_EAST_RELEASE)
        sut.changeApsEnvironment(ApsEnvironmentType.DEV)
        verify(mockSharedPreferencesEditor, never()).putString(any(), any())
    }

    @Test
    fun changeApsEnvironment_prodEastReleaseBuildChangeToQa_noOp() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.PROD_EAST_RELEASE)
        sut.changeApsEnvironment(ApsEnvironmentType.QA)
        verify(mockSharedPreferencesEditor, never()).putString(any(), any())
    }

    @Test
    fun changeApsEnvironment_devBuildChangeToDev_changedToDev() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.DEV)

        sut.changeApsEnvironment(ApsEnvironmentType.DEV)
        val keyCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(1)).putString(keyCaptor.capture(), valueCaptor.capture())
        assertThat(keyCaptor.lastValue).isEqualTo("selected_aps_environment_type")
        assertThat(valueCaptor.lastValue).isEqualTo("aps_env_dev")
    }

    @Test
    fun changeApsEnvironment_devBuildChangeToQa_changedToQa() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.DEV)

        sut.changeApsEnvironment(ApsEnvironmentType.QA)
        val keyCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(1)).putString(keyCaptor.capture(), valueCaptor.capture())
        assertThat(keyCaptor.lastValue).isEqualTo("selected_aps_environment_type")
        assertThat(valueCaptor.lastValue).isEqualTo("aps_env_qa")
    }

    @Test
    fun changeApsEnvironment_devBuildChangeToQa3_changedToQa3() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.DEV)

        sut.changeApsEnvironment(ApsEnvironmentType.QA3)
        val keyCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(1)).putString(keyCaptor.capture(), valueCaptor.capture())
        assertThat(keyCaptor.lastValue).isEqualTo("selected_aps_environment_type")
        assertThat(valueCaptor.lastValue).isEqualTo("aps_env_qa3")
    }

    @Test
    fun changeApsEnvironment_devBuildChangeToCanary_changedToCanary() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.DEV)

        sut.changeApsEnvironment(ApsEnvironmentType.CANARY)
        val keyCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(1)).putString(keyCaptor.capture(), valueCaptor.capture())
        assertThat(keyCaptor.lastValue).isEqualTo("selected_aps_environment_type")
        assertThat(valueCaptor.lastValue).isEqualTo("aps_env_canary")
    }

    @Test
    fun changeApsEnvironment_devBuildChangeToProductionCanary_changedToProductionCanary() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.DEV)

        sut.changeApsEnvironment(ApsEnvironmentType.PRODUCTION_CANARY)
        val keyCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(1)).putString(keyCaptor.capture(), valueCaptor.capture())
        assertThat(keyCaptor.lastValue).isEqualTo("selected_aps_environment_type")
        assertThat(valueCaptor.lastValue).isEqualTo("aps_env_production_canary")
    }

    @Test
    fun changeApsEnvironment_devBuildChangeToOldProd_changedToOldProd() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.DEV)

        sut.changeApsEnvironment(ApsEnvironmentType.PRODUCTION)
        val keyCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(1)).putString(keyCaptor.capture(), valueCaptor.capture())
        assertThat(keyCaptor.lastValue).isEqualTo("selected_aps_environment_type")
        assertThat(valueCaptor.lastValue).isEqualTo("aps_env_production")
    }

    @Test
    fun changeApsEnvironment_devBuildChangeToProd_changedToProd() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.DEV)

        sut.changeApsEnvironment(ApsEnvironmentType.APIM_PRODUCTION)
        val keyCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(1)).putString(keyCaptor.capture(), valueCaptor.capture())
        assertThat(keyCaptor.lastValue).isEqualTo("selected_aps_environment_type")
        assertThat(valueCaptor.lastValue).isEqualTo("aps_env_apim_production")
    }

    @Test
    fun changeApsEnvironment_devBuildChangeToProdEast_changedToProdEast() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.DEV)

        sut.changeApsEnvironment(ApsEnvironmentType.APIM_PRODUCTION_EAST)
        val keyCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(1)).putString(keyCaptor.capture(), valueCaptor.capture())
        assertThat(keyCaptor.lastValue).isEqualTo("selected_aps_environment_type")
        assertThat(valueCaptor.lastValue).isEqualTo("aps_env_apim_production_east")
    }

    @Test
    fun changeAuthEnvironment_prodReleaseBuildChangeToQa3_noOp() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.PROD_RELEASE)
        sut.changeAuthEnvironment(AuthEnvironmentType.QA3)
        verify(mockSharedPreferencesEditor, never()).putString(any(), any())
    }

    @Test
    fun changeAuthEnvironment_prodEastReleaseBuildChangeToQa3_noOp() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.PROD_EAST_RELEASE)
        sut.changeAuthEnvironment(AuthEnvironmentType.QA3)
        verify(mockSharedPreferencesEditor, never()).putString(any(), any())
    }

    @Test
    fun changeAuthEnvironment_devBuildChangeToQa3_changedToQa3() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.DEV)

        sut.changeAuthEnvironment(AuthEnvironmentType.QA3)
        val keyCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(1)).putString(keyCaptor.capture(), valueCaptor.capture())
        assertThat(keyCaptor.lastValue).isEqualTo("selected_auth_environment_type")
        assertThat(valueCaptor.lastValue).isEqualTo("auth_env_qa3")
    }

    @Test
    fun changeAuthEnvironment_devBuildChangeToProd_changedToProd() {
        val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> { editor ->
            on { putString(any(), any()) } doAnswer { editor }
        }
        val mockSharedPrefs = mock<SharedPreferences> {
            on { edit() } doReturn mockSharedPreferencesEditor
        }
        val sut = EnvironmentRepositoryImpl(mockSharedPrefs, MockBuildConfigProviders.DEV)

        sut.changeAuthEnvironment(AuthEnvironmentType.APIM_PRODUCTION)
        val keyCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(1)).putString(keyCaptor.capture(), valueCaptor.capture())
        assertThat(keyCaptor.lastValue).isEqualTo("selected_auth_environment_type")
        assertThat(valueCaptor.lastValue).isEqualTo("auth_env_apim_production")
    }
}
