package com.albertsons.acupick.ui.arrivals.ready

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import org.junit.Rule
import org.junit.rules.TestRule

class ArrivalsAllOrdersViewModelTest : BaseTest() {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    // Todo determine if needed or can move to PagerVm
    // @Test
    // fun searchOrderReadyViewModel_showOldDataToast() {
    //     var toastedCorrectly = false
    //     val toasterMock: Toaster = mock {
    //         on {
    //             toast("Failed to refresh data")
    //         } doAnswer { toastedCorrectly = true }
    //     }
    //     val vm = searchOrderReadyViewModelFactory()
    //     vm.runPrivateMethod("showOldDataToast")
    //     assertThat(toastedCorrectly).isTrue()
    // }
    //
    // @Test
    // fun searchOrderReadyViewModel_containsTag() {
    //     val vm = searchOrderReadyViewModelFactory()
    //     assertThat( vm.containsTag(tag = "blewp")).isFalse()
    //     assertThat(vm.containsTag(tag = RELOAD_RESULTS_TAG)).isTrue()
    // }
    //
    // @Test
    // fun searchOrderReadyViewModel_loadResults() {
    //     inlineKoinSingle(override = true) {
    //         mockAps
    //     }
    //     inlineKoinSingle<UserRepository>(override = true) {
    //         mock {
    //             on { user } doReturn stateFlowOf(testUser)
    //         }
    //     }
    //     var vm = searchOrderReadyViewModelFactory()
    //     vm.loadResults(true)
    //     assertThat(vm.results.value?.isNotEmpty())
    //
    //     inlineKoinSingle<UserRepository>(override = true) {
    //         mock {
    //             on { user } doReturn stateFlowOf(testFailUser)
    //         }
    //     }
    //     vm = searchOrderReadyViewModelFactory()
    //     vm.loadResults(true)
    //     assertThat(vm.results.value?.isEmpty())
    //
    //     val networkAvailabilityManagerMock: NetworkAvailabilityManager = mock {
    //         onBlocking { isConnected } doReturn stateFlowOf(false)
    //         onBlocking { triggerOfflineError { } } doAnswer {
    //         }
    //     }
    //     inlineKoinSingle(override = true) { networkAvailabilityManagerMock }
    //     vm = searchOrderReadyViewModelFactory(app = testApplicationFactory())
    //     vm.loadResults(true)
    //     runBlocking {
    //         verify(networkAvailabilityManagerMock, times(1)).triggerOfflineError(any())
    //     }
    // }
}
