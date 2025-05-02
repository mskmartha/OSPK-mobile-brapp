package com.albertsons.acupick.ui.picklists.team

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CategoryStatus
import com.albertsons.acupick.data.model.request.AssignUserRequestDto
import com.albertsons.acupick.data.model.response.ActivityAndErDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ActivityDtoByCategoryDto
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.test.mocks.testApsRepoFactory
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.picklists.open.OpenPickListsFragmentDirections
import com.albertsons.acupick.ui.util.StringIdHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class TeamPickListsViewModelTest : BaseTest() {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    @Test
    fun `WHEN refreshData or loadData is called THEN UI initializes correctly`() {
        val vm = teamPickListViewModelFactory()
        val isDataLoadingMock = vm.isDataLoading.mock()
        val isDataRefreshingMock = vm.isDataRefreshing.mock()
        val teamPickListsMock = vm.pickLists.mock()
        vm.loadData()
        assertThat(isDataLoadingMock.verifyWithCapture(atLeastOnce())).isFalse()
        assertThat(isDataRefreshingMock.verifyWithCapture(atLeastOnce())).isFalse()
        assertThat(teamPickListsMock.verifyWithCapture()).isNotEmpty()
    }

    private val testApsRepoSearchFailure: ApsRepository = testApsRepoFactory(
        searchResult = ApiResult.Failure.GeneralFailure(message = "")
    )

    @Test
    fun `WHEN loadData is called but an activity cannot be found THEN handle the error`() {
        // Load custom module and set override to replace if dependency already included in another module
        inlineKoinSingle(override = true) { testApsRepoSearchFailure }

        val vm = teamPickListViewModelFactory()
        vm.loadData()
    }

    @Test
    fun `WHEN transferPickToMe is called and the PickRepository hasActivePickListActivityId THEN the showContinueOrder dialog will show`() {
        val vm = teamPickListViewModelFactory()
        val showContinueOrderMock = vm.inlineDialogEvent.mock()
        val testActivityAndErDto: ActivityAndErDto = mock {
            on { actId } doReturn 123654789
        }
        vm.onPickClicked(testActivityAndErDto)
        vm.transferPickToMe()
        assertThat(showContinueOrderMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            CustomDialogArgDataAndTag(
                tag = "teamContinueOrderDialogTag",
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.continue_order_title),
                    body = StringIdHelper.Id(R.string.continue_order_body),
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                )
            )
        )
    }

    @Test
    fun `WHEN transferPickToMe is called and the PickRepository does not hasActivePickListActivityId THEN navigate to PickListItemsFragment`() {
        // Example using listOf demonstrating that several modules can be added at once
        inlineKoinSingle<PickRepository>(override = true) {
            mock {
                on { hasActivePickListActivityId() } doReturn false
                onBlocking {
                    assignUser(
                        AssignUserRequestDto(
                            actId = 123654789,
                            replaceOverride = true,
                            user = null
                        )
                    )
                } doReturn ApiResult.Success(ActivityDto())

                onBlocking { unAssignUser(anyString(), anyString(), anyString(), anyList()) } doReturn ApiResult.Success(Unit)
            }
        }

        val vm = teamPickListViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        val selectedPickListActivityIdMock = vm.selectedPickListActivityId.mock()
        val showConfirmationDialogMock = vm.inlineDialogEvent.mock()
        // set the selectedPickListActivityId
        val testActivityAndErDto: ActivityAndErDto = mock {
            on { actId } doReturn 123654789
        }
        vm.onPickClicked(testActivityAndErDto)
        vm.transferPickToMe()
        assertThat(selectedPickListActivityIdMock.verifyWithCapture(atLeastOnce())).isEqualTo("123654789")
        assertThat(showConfirmationDialogMock.verifyWithCapture()).isEqualTo(
            CustomDialogArgDataAndTag(
                tag = "transferTeamPickToMeDialogTag",
                data = CustomDialogArgData(
                    titleIcon = null,
                    title = StringIdHelper.Id(R.string.select_team_picklist_confirmation_dialog_title),
                    body = StringIdHelper.Id(R.string.select_team_picklist_confirmation_dialog_body),
                    positiveButtonText = StringIdHelper.Id(R.string.start),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = true
                )
            )
        )
        assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
            NavigationEvent.Directions(
                OpenPickListsFragmentDirections.actionToPickListItemsFragment("123654789")
            )
        )
    }

    // TODO: Due to ACUPICK-1313, the new behavior should be PickRepository.assignUser() being called
    // @Test
    // fun `WHEN onTeamPickClicked and the pickList actId the same as the previousActivityId THEN navigate to StagingFragment`() {
    //     val vm = teamPickListViewModelFactory()
    //     val testActivityAndErDto: ActivityAndErDto = mock {
    //         on { actId } doReturn 123654789
    //     }
    //     val navigationEventMock = vm.navigationEvent.mock()
    //     vm.loadData()
    //     vm.onTeamPickClicked(testActivityAndErDto)
    //     assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
    //         NavigationEvent.Directions(
    //             NavGraphDirections.actionPickListFragmentToStagingFragment(
    //                 activityId = "123654789",
    //                 isPreviousPrintSuccessful = true,
    //                 shouldClearData = false
    //             )
    //         )
    //     )
    // }

    @Test
    fun `WHEN continueOrder is called and the there is no pickAssigned THEN navigate to PickListItemsFragment with the ActivePickListActivityId`() {
        val vm = teamPickListViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        vm.continueOrder()
        assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
            NavigationEvent.Directions(OpenPickListsFragmentDirections.actionToPickListItemsFragment("123456"))
        )
    }

    @Test
    fun `WHEN onTeamPickClicked is called with a pickList item that differs from the pickAssigned THEN display the continue order dialog`() {
        val vm = teamPickListViewModelFactory()
        val showContinueOrderMock = vm.inlineDialogEvent.mock()
        vm.loadData()
        vm.onPickClicked(
            mock {
                on { actId } doReturn 654321
            }
        )
        assertThat(showContinueOrderMock.verifyWithCapture()).isEqualTo(
            CustomDialogArgDataAndTag(
                tag = "teamContinueOrderDialogTag",
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.continue_order_title),
                    body = StringIdHelper.Id(R.string.continue_order_body),
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                )
            )
        )
    }

    @Test
    fun `WHEN continueOrder is clicked but the ActivityStatus of the current pickAssigned is not released THEN navigate to PickListItemsFragment with the actId`() {
        inlineKoinSingle<ApsRepository>(override = true) {
            mock {
                onBlocking {
                    searchActivities(
                        "2941",
                        "8304636844",
                        assigned = true,
                        assignedToMe = true,
                        open = false,
                        pickUpReady = false,
                        stageByTime = null
                    )
                } doReturn searchActivitiesSuccessfulResponse2
            }
        }
        inlineKoinSingle<ConversationsRepository>(override = true) {
            mock {
                onBlocking {
                    getConversationId("")
                } doReturn ""
            }
        }
        val vm = teamPickListViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        vm.loadData()
        vm.continueOrder()
        assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
            NavigationEvent.Directions(
                TeamPickListsFragmentDirections.actionToPickListItemsFragment("1")
            )
        )
    }

    // /////////////////////////////////////////////////////
    // Test Objects and Factories
    // /////////////////////////////////////////////////////
    private val searchActivitiesSuccessfulResponse: ApiResult<List<ActivityDtoByCategoryDto>> = ApiResult.Success(
        listOf(
            ActivityDtoByCategoryDto(
                category = CategoryStatus.ASSIGNED,
                data = listOf(
                    mock {
                        on { status } doReturn ActivityStatus.RELEASED
                    }
                )
            ),
            ActivityDtoByCategoryDto(
                category = CategoryStatus.ASSIGNED_TO_ME,
                data = listOf(
                    mock {
                        on { status } doReturn ActivityStatus.RELEASED
                        on { prevActivityId } doReturn 123654789
                        on { actId } doReturn 987456321
                    }
                )
            )
        )
    )

    private val searchActivitiesSuccessfulResponse2: ApiResult<List<ActivityDtoByCategoryDto>> = ApiResult.Success(
        listOf(
            ActivityDtoByCategoryDto(
                category = CategoryStatus.ASSIGNED_TO_ME,
                data = listOf(
                    mock {
                        on { status } doReturn ActivityStatus.IN_PROGRESS
                        on { actId } doReturn 1
                    }
                )
            )
        )
    )

    private fun teamPickListViewModelFactory(
        app: Application = testApplicationFactory(),
        activityViewModel: MainActivityViewModel = mock {},
    ) = TeamPickListsViewModel(
        app = app,
        activityViewModel = activityViewModel,
    )
}
