package com.albertsons.acupick.ui.arrivals.results

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import org.junit.Rule
import org.junit.rules.TestRule

class ArrivalsInProgressViewModelTest : BaseTest() {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    // Todo determine if needed or can move to PagerVm
    // @Test
    // fun searchOrdersResultsViewModel_containsTag() {
    //     val vm = searchOrdersResultsViewModelFactory()
    //     assertThat(vm.containsTag(HANDOFF_ERROR_DIALOG_TAG)).isTrue()
    //     assertThat(vm.containsTag("the blewp")).isFalse()
    // }
    //
    // @Test
    // fun searchOrdersResultsViewModel_loadResults() {
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
