package com.albertsons.acupick.ui.storelist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.activityViewModelFactory
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.test.mocks.testUserRepo
import com.albertsons.acupick.ui.MainActivityViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock

class StoresViewModelTest : BaseTest() {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    // /////////////////////////////////////////////////////////////////////////
    // Initial UI setup tests
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun `WHEN VM initializes THEN UI initializes correctly`() {
        // Setup VM
        val vm = storesViewModelFactory()

        // Verify
        assertThat(vm.stores.getOrAwaitValue())?.isEqualTo(listOf("2941", "1234", "5678", "0000"))
    }

    // /////////////////////////////////////////////////////////////////////////
    // Test Click listeners
    // /////////////////////////////////////////////////////////////////////////
    @Test
    fun `WHEN store item is clicked THEN internal state is updated`() {
        // Setup VM
        val vm = storesViewModelFactory()

        // Setup mocks
        val confirmActiveMock = vm.confirmActive.mock()

        // Execute code to be tested
        vm.onStoreClicked("5678")

        // Verify
        assertThat(vm.store).isEqualTo("5678")
        assertThat(confirmActiveMock.verifyWithCapture(atLeastOnce())).isTrue()
    }

    @Test
    fun `WHEN confirm is clicked THEN store selection complete event is triggered`() {
        inlineKoinSingle(override = true) {
            mock<MainActivityViewModel> {
                activityViewModelFactory()
            }
        }
        // Setup VM
        val vm = storesViewModelFactory()
        vm.store = "5678"

        // Setup mocks
        val storeSelectionCompletActionMock = vm.storeSelectionCompleteAction.mock()

        // Execute code to be tested
        vm.onConfirmClick()

        // Verify
        assertThat(storeSelectionCompletActionMock.verifyWithCapture()).isEqualTo(Unit)
    }

    // /////////////////////////////////////////////////////////////////////////
    // Test Objects and Factories
    // /////////////////////////////////////////////////////////////////////////

    private fun storesViewModelFactory(
        stateHandle: SavedStateHandle = mock {},
        app: Application = testApplicationFactory(),
        userRepo: UserRepository = testUserRepo,
        dispatcherProvider: DispatcherProvider = TestDispatcherProvider(),
    ) = StoresViewModel(
        stateHandle = stateHandle,
        app = app,
        userRepo = userRepo,
        dispatcherProvider = dispatcherProvider
    )
}
