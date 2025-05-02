package com.albertsons.acupick.ui.arrivals

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.request.AssignUserWrapperRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ServerErrorDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.IdRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.arrivalsPagerViewModelFactory
import com.albertsons.acupick.test.arrivalsViewModelFactory
import com.albertsons.acupick.test.getPrivateProperty
import com.albertsons.acupick.test.mocks.baseData1
import com.albertsons.acupick.test.mocks.baseData2
import com.albertsons.acupick.test.mocks.testUser
import com.albertsons.acupick.test.setPrivateProperty
import com.albertsons.acupick.ui.arrivals.ArrivalsViewModel.Companion.TRANSFER_ORDER_DIALOG_TAG
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.models.OrderItemUI
import com.albertsons.acupick.ui.util.StringIdHelper
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class ArrivalsPagerViewModelTest : BaseTest() {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    @Suppress("UNCHECKED_CAST")
    fun getSelectedItemIds(vm: ArrivalsViewModel): List<Long> {
        return (vm.getPrivateProperty("selectedItemIds") as MutableStateFlow<List<Long>>).value
    }

    @Suppress("UNCHECKED_CAST")
    fun setSelectedItemIds(vm: ArrivalsViewModel, newVal: List<Long>) {
        vm.setPrivateProperty("selectedItemIds", newVal = MutableStateFlow(newVal))
    }

    @Suppress("UNCHECKED_CAST")
    fun getLoadEvent(vm: ArrivalsPagerViewModel): Boolean? {
        return (vm.getPrivateProperty("loadEvent") as MutableStateFlow<Boolean?>).value
    }

    @Test
    fun baseOrderListViewModel_beginHandoff() {
        // assignedToMe = false, isAssigned = false, reAssign = false
        inlineKoinSingle<ApsRepository>(override = true) {
            mock {
                onBlocking {
                    pickUpActivityDetails(id = 1234)
                } doReturn ApiResult.Success(ActivityDto(actId = 4321))
                onBlocking {
                    pickUpActivityDetails(
                        id = 2345,
                        loadCI = false
                    )
                } doReturn ApiResult.Failure.Server(ServerErrorDto())
            }
        }
        inlineKoinSingle<ApsRepository>(override = true) {
            mock {
                onBlocking {
                    assignUserToHandoffs(
                        AssignUserWrapperRequestDto(
                            actIds = listOf(4321),
                            replaceOverride = false,
                            // Todo find out what this does
                            resetPickList = true,
                            user = null
                        )
                    )
                } doReturn ApiResult.Success(listOf(ActivityDto()))
            }
        }
        var arrivalsViewModel = arrivalsViewModelFactory()

        val navigationEventMock = arrivalsViewModel.navigationEvent.mock()

        // Success
        arrivalsViewModel.results.postValue(listOf(baseData1).map { OrderItemUI(it) })
        setSelectedItemIds(arrivalsViewModel, newVal = listOf(13579135))
        arrivalsViewModel.beginHandoff(false)
        // TODO: Fix this test
        // assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
        //     NavigationEvent.Directions(
        //         NavGraphDirections.actionToSearchOrderDetailsFragment(activityList = SelectedActivities(listOf(ActivityDto())))
        //     )
        // )

        // Failure
        arrivalsViewModel.results.postValue(listOf(baseData2, baseData1).map { OrderItemUI(it) })
        setSelectedItemIds(arrivalsViewModel, newVal = listOf(97531975))
        arrivalsViewModel.beginHandoff(true)
        // TODO: Fix this test
        // assertThat(balVM.activityViewModel.isServerError.getOrAwaitValue()).isEqualTo(true)

        // assignedToMe = false, isAssigned = true, reAssign = false -> only one that goes forward
        inlineKoinSingle<UserRepository>(override = true) {
            mock {
                on { user } doReturn stateFlowOf(testUser)
                on { isLoggedIn } doReturn stateFlowOf(true)
            }
        }
        arrivalsViewModel = arrivalsViewModelFactory()
        val inlineDialogEventMock = arrivalsViewModel.inlineDialogEvent.mock()
        arrivalsViewModel.results.postValue(listOf(baseData2, baseData1).map { OrderItemUI(it) })
        setSelectedItemIds(arrivalsViewModel, newVal = listOf(97531975))
        arrivalsViewModel.beginHandoff(false)
        assertThat(inlineDialogEventMock.verifyWithCapture()).isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.handoff_reassign_title),
                    body = StringIdHelper.Raw("Handoff is in progress by Blerp Glerp. Are you sure you want to take this handoff?"),
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false,
                    cancelable = true
                ),
                tag = TRANSFER_ORDER_DIALOG_TAG
            )
        )

        // With no network
        val networkAvailabilityManagerMock: NetworkAvailabilityManager = mock {
            onBlocking { isConnected } doReturn stateFlowOf(false)
            onBlocking { triggerOfflineError { } } doAnswer {
            }
        }
        inlineKoinSingle(override = true) { networkAvailabilityManagerMock }
        arrivalsViewModel = arrivalsViewModelFactory()
        arrivalsViewModel.results.postValue(listOf(baseData1).map { OrderItemUI(it) })
        setSelectedItemIds(arrivalsViewModel, newVal = listOf(13579135))
        arrivalsViewModel.beginHandoff(false)

        runBlocking {
            verify(networkAvailabilityManagerMock, times(1)).triggerOfflineError(any())
        }
    }

    @Test
    fun baseOrderListViewModel_updateArrivalStatus() {
        inlineKoinSingle<ApsRepository>(override = true) {
            mock {
                onBlocking {
                    cancelHandoff(
                        any()
                    )
                } doReturn ApiResult.Success(Unit)
            }
        }
        val arrivalsViewModel = arrivalsViewModelFactory()
        arrivalsViewModel.results.postValue(listOf(baseData2, baseData1).map { OrderItemUI(it) })
        arrivalsViewModel.updateArrivalStatus(id = 97531975, arrived = false)
        assertThat(arrivalsViewModel.results.value?.size)?.isEqualTo(1)
    }

    @Test
    fun baseOrderListViewModel_load() {
        inlineKoinSingle<IdRepository>(override = true) {
            mock()
        }
        val arrivalsPagerVM = arrivalsPagerViewModelFactory()
        arrivalsPagerVM.load(true)
        assertThat(getLoadEvent(arrivalsPagerVM)).isTrue()
        arrivalsPagerVM.load(false)
        assertThat(getLoadEvent(arrivalsPagerVM)).isFalse()
    }
}
