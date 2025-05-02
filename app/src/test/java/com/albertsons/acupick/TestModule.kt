package com.albertsons.acupick

import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.mocks.testAnalyticsHelper
import com.albertsons.acupick.test.mocks.testApsRepo
import com.albertsons.acupick.test.mocks.testAutoLogoutLogic
import com.albertsons.acupick.test.mocks.testConfigApi
import com.albertsons.acupick.test.mocks.testConfigRepository
import com.albertsons.acupick.test.mocks.testDevOptionsRepo
import com.albertsons.acupick.test.mocks.testDevOptionsWriterRepo
import com.albertsons.acupick.test.mocks.testEnvironmentRepository
import com.albertsons.acupick.test.mocks.testLoginLogoutAnalyticsRepo
import com.albertsons.acupick.test.mocks.testNetworkAvailabilityManager
import com.albertsons.acupick.test.mocks.testPickRepository
import com.albertsons.acupick.test.mocks.testPushNotificationsRepo
import com.albertsons.acupick.test.mocks.testSiteRepo
import com.albertsons.acupick.test.mocks.testTokenizedLdapRepository
import com.albertsons.acupick.test.mocks.testUserRepo
import com.albertsons.acupick.ui.util.EventAction
import org.koin.dsl.module

object TestModule {
    val acuPickLoggerTestImpl = object : AcuPickLoggerInterface {
        override fun setUserData(key: String, value: Any?, debugUserData: Boolean) {}
        override fun e(error: String) {}
        override fun v(verbose: String) {}
        override fun w(warning: String) {}
        override fun i(info: String) {}
        override fun d(debug: String) {}
        override fun startNextSession() {}
        override fun reportMetric(value: String, count: Long, debugMetric: Boolean) {}
        override fun beginCall(arg1: String, arg2: String, arg3: Any, debugCall: Boolean) {}
        override fun endCall(error: String?, debugCall: Boolean) {}
        override fun startTimer(value: String, debugTimer: Boolean) {}
        override fun stopTimer(value: String, debugTimer: Boolean) {}
        override fun leaveBreadcrumb(breadcrumb: String, mode: Int) {}
    }

    private val firebaseAnalyticsTestInterfaceImpl = object : FirebaseAnalyticsInterface {
        override fun logEvent(eventCategory: String, eventAction: EventAction, eventLabel: String, valuePairList: List<Pair<String, String>>?) {}

        override fun setUserPropertyValue(key: String, value: String) {}
        override fun setuserId(userId: String) {}
    }

    val testModule = module {
        single<AcuPickLoggerInterface> {
            acuPickLoggerTestImpl
        }
        single<FirebaseAnalyticsInterface> {
            firebaseAnalyticsTestInterfaceImpl
        }
    }

    // Default mocks can be overridden using inlineKoinSingle function
    fun generateMockedTestModule() = module {
        single<AcuPickLoggerInterface> { acuPickLoggerTestImpl }
        single<FirebaseAnalyticsInterface> { firebaseAnalyticsTestInterfaceImpl }
        single<DispatcherProvider> { TestDispatcherProvider() }
        single { testAnalyticsHelper }
        single { testConfigApi }
        single { testConfigRepository }
        single { testEnvironmentRepository }
        single { testNetworkAvailabilityManager }
        single { testUserRepo }
        single { testPickRepository() }
        single { testApsRepo }
        single { testSiteRepo }
        single { testPushNotificationsRepo }
        single { testAutoLogoutLogic }
        single { testLoginLogoutAnalyticsRepo }
        single { testDevOptionsRepo }
        single { testDevOptionsWriterRepo }
        single { testTokenizedLdapRepository }
    }
}
