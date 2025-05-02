package com.albertsons.acupick.ui.picklists.open

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.request.AssignUserRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.ServerErrorCodeDto
import com.albertsons.acupick.data.model.response.ServerErrorDto
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.util.StringIdHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times

class OpenPickListsViewModelTest : BaseTest() {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    @Test
    fun `WHEN refreshData or loadData is called THEN UI initializes correctly`() {
        val vm = openPickListsViewModelFactory()
        val isDataLoadingMock = vm.isDataLoading.mock()
        val isDataRefreshingMock = vm.isDataRefreshing.mock()
        val pickListsMock = vm.pickLists.mock()
        vm.loadData()
        assertThat(isDataLoadingMock.verifyWithCapture(atLeastOnce())).isFalse()
        assertThat(isDataRefreshingMock.verifyWithCapture(atLeastOnce())).isFalse()
        assertThat(pickListsMock.verifyWithCapture()).isNotEmpty()
    }

    @Test
    fun `WHEN transferPickToMe is called and the PickRepository hasActivePickListActivityId THEN the showContinueOrder dialog will show`() {
        val vm = openPickListsViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        vm.transferPickToMe()
        assertThat(inlineDialogEventMock.verifyWithCapture()).isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.continue_order_title),
                    body = StringIdHelper.Id(R.string.continue_order_body),
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                ),
                tag = "openContinueOrderDialog"
            )
        )
    }

    @Test
    fun `WHEN transferPickToMe is called and the PickRepository does not hasActivePickListActivityId THEN navigate to PickListItemsFragment `() {
        inlineKoinSingle<PickRepository>(override = true) {
            mock {
                on { hasActivePickListActivityId() } doReturn false
                onBlocking {
                    assignUser(
                        AssignUserRequestDto(
                            actId = 123654789,
                            replaceOverride = false,
                            user = null
                        )
                    )
                } doReturn ApiResult.Success(ActivityDto())
            }
        }

        val vm = openPickListsViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        val selectedPickListActivityIdMock = vm.selectedPickListActivityId.mock()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        vm.onPickClicked(mock { on { actId } doReturn 123654789 })
        vm.transferPickToMe()
        assertThat(selectedPickListActivityIdMock.verifyWithCapture()).isEqualTo("123654789")
        assertThat(inlineDialogEventMock.verifyWithCapture()).isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = null,
                    title = StringIdHelper.Raw(""),
                    body = StringIdHelper.Id(R.string.select_picklist_confirmation_dialog_body),
                    positiveButtonText = StringIdHelper.Id(R.string.start),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = true
                ),
                tag = "openConfirmationDialog"
            )
        )
        assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
            NavigationEvent.Directions(
                OpenPickListsFragmentDirections.actionToPickListItemsFragment("123654789")
            )
        )
    }

    @Test
    fun `WHEN continueOrder is called and the pickList actId the same as the previousActivityId THEN navigate to StagingFragment`() {
        val vm = openPickListsViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        vm.loadData()
        vm.continueOrder()
        assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
            NavigationEvent.Directions(
                NavGraphDirections.actionPickListFragmentToStagingFragment(
                    activityId = "123654789",
                    isPreviousPrintSuccessful = true,
                    shouldClearData = false
                )
            )
        )
    }

    @Test
    fun `WHEN continueOrder is called and the there is no pickAssigned THEN navigate to PickListItemsFragment with the ActivePickListActivityId`() {
        val vm = openPickListsViewModelFactory()
        inlineKoinSingle<ConversationsRepository>(override = true) {
            mock {
                onBlocking {
                    getConversationId("")
                } doReturn ""
            }
        }
        val navigationEventMock = vm.navigationEvent.mock()
        vm.continueOrder()
        assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
            NavigationEvent.Directions(OpenPickListsFragmentDirections.actionToPickListItemsFragment("123456"))
        )
    }

    @Test
    fun `WHEN transferPickToMe is called and server returns NO_USER_TO_ASSIGN_ACTIVITY error THEN show dialog for showOrderAlreadyAssigned`() {
        inlineKoinSingle<PickRepository>(override = true) {
            mock {
                on { hasActivePickListActivityId() } doReturn false
                onBlocking {
                    assignUser(
                        AssignUserRequestDto(
                            actId = 123654789,
                            replaceOverride = false,
                            user = null
                        )
                    )
                } doReturn ApiResult.Failure.Server(
                    error = ServerErrorDto(
                        errorCode = ServerErrorCodeDto(
                            rawValue = ServerErrorCode.NO_USER_TO_ASSIGN_ACTIVITY.value,
                            resolvedType = ServerErrorCode.NO_USER_TO_ASSIGN_ACTIVITY
                        )
                    )
                )
            }
        }

        val vm = openPickListsViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        vm.onPickClicked(mock { on { actId } doReturn 123654789 })
        vm.transferPickToMe()
        assertThat(inlineDialogEventMock.verifyWithCapture(times(2))).isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.staging_server_error),
                    body = StringIdHelper.Id(R.string.open_pick_list_assign_error_body),
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                    cancelOnTouchOutside = false
                ),
                tag = "singleOrderErrorDialogTag"
            )
        )
    }

    @Test
    fun `WHEN onPickClicked is called with a pickList item that differs from the pickAssigned THEN display the continue order dialog`() {
        val vm = openPickListsViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        vm.loadData()
        vm.onPickClicked(mock { on { actId } doReturn 654321 })
        assertThat(inlineDialogEventMock.verifyWithCapture()).isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.continue_order_title),
                    body = StringIdHelper.Id(R.string.continue_order_body),
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                ),
                tag = "openContinueOrderDialog"
            )
        )
    }

    // /////////////////////////////////////////////////////
    // Test Objects and Factories
    // /////////////////////////////////////////////////////
    private fun openPickListsViewModelFactory(
        app: Application = testApplicationFactory(),
        activityViewModel: MainActivityViewModel = mock {},
    ) = OpenPickListsViewModel(
        app = app,
        activityViewModel = activityViewModel,
    )
}
