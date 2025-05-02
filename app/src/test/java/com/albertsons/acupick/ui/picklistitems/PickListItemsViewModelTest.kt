package com.albertsons.acupick.ui.picklistitems

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.picklist.InvalidItemScanTracker
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.image.ImagePreCacher
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.activityViewModelFactory
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.test.mocks.testItem
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.dialog.ALREADY_ASSIGNED_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.itemdetails.ItemDetailsParams
import com.albertsons.acupick.ui.manualentry.ManualEntryPickParams
import com.albertsons.acupick.ui.manualentry.ManualEntryType
import com.albertsons.acupick.ui.substitute.SubstituteParams
import com.albertsons.acupick.ui.util.StringIdHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.after
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times

class PickListItemsViewModelTest : BaseTest() {
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
    fun `WHEN loadPickList is called THEN UI initializes correctly`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val selectedItemIndexMock = vm.todoSelectedItemIndex.asLiveData().mock()
        val todoItemCountMock = vm.todoItemCount.mock()
        val pickedItemCountMock = vm.pickedItemCount.mock()
        val shortItemCountMock = vm.shortItemCount.mock()

        // Execute code to be tested
        vm.loadPickList("")

        // Verify
        assertThat(selectedItemIndexMock.verifyWithCapture(after(1100)?.atLeastOnce() ?: atLeastOnce()))
            .isEqualTo(0)
        assertThat(todoItemCountMock.verifyWithCapture(atLeastOnce())).isEqualTo("4")
        assertThat(pickedItemCountMock.verifyWithCapture(atLeastOnce())).isEqualTo("5")
        assertThat(shortItemCountMock.verifyWithCapture(atLeastOnce())).isEqualTo("1")
    }

    @Test
    fun `WHEN updateSelectedIndex is called and To-do tab is selected THEN todoSelectedItemIndex is updated`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val todoSelectedItemIndexMock = vm.todoSelectedItemIndex.asLiveData().mock()
        vm.loadPickList("543")

        // Execute code to be tested
        vm.changeView(listView = false)
        vm.setCurrentTab(PickListType.Todo.value)
        vm.updateSelectedItemIndex(1)

        // Verify
        assertThat(todoSelectedItemIndexMock.verifyWithCapture(atLeastOnce())).isEqualTo(1)
    }

    @Test
    fun `WHEN updateSelectedIndex is called and Picked tab is selected THEN pickedSelectedItemIndex is updated`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val pickedSelectedItemIndexMock = vm.pickedSelectedItemIndex.asLiveData().mock()
        vm.loadPickList("543")

        // Execute code to be tested
        vm.changeView(listView = false)
        vm.setCurrentTab(PickListType.Picked.value)
        vm.updateSelectedItemIndex(1)

        // Verify
        assertThat(pickedSelectedItemIndexMock.verifyWithCapture(atLeastOnce())).isEqualTo(1)
    }

    @Test
    fun `WHEN updateSelectedIndex is called and Short tab is selected THEN shortSelectedItemIndex is updated`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val shortSelectedItemIndexMock = vm.shortSelectedItemIndex.asLiveData().mock()
        vm.loadPickList("543")

        // Execute code to be tested

        vm.changeView(listView = false)
        vm.setCurrentTab(PickListType.Short.value)
        vm.updateSelectedItemIndex(1)

        // Verify
        assertThat(shortSelectedItemIndexMock.verifyWithCapture(atLeastOnce())).isEqualTo(1)
    }

    @Test
    fun `WHEN unAssignPicker is called THEN unAssign unsuccessful action event is triggered`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()

        // Init VM
        vm.loadPickList("1")

        // Execute code to be tested
        vm.unAssignPicker()

        // Verify
        assertThat(inlineDialogEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            CustomDialogArgDataAndTag(ALREADY_ASSIGNED_PICKLIST_ARG_DATA, PickListItemsViewModel.PICK_ASSIGNED_TO_DIFFERENT_USER_TAG)
        )
    }

    @Test @Ignore
    fun `WHEN unAssignPicker is called THEN unAssign successful action event is triggered`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        inlineKoinSingle<UserRepository>(override = false) {
            mock {
                on { user } doReturn stateFlowOf(null)
            }
        }

        // Setup mocks
        val unAssignSuccessfulActionMock = vm.unAssignSuccessfulAction.mock()

        // Init VM
        vm.loadPickList("")

        // Execute code to be tested
        vm.unAssignPicker()

        // Verify
        assertThat(
            unAssignSuccessfulActionMock.verifyWithCapture(
                after(3000)?.times(1) ?: times(1)
            )
        )?.isEqualTo(Unit)
    }

    @Test
    fun `WHEN detailsCTA is clicked THEN correct bottom sheet event is triggered`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val inlineDialogEventMock = vm.inlineBottomSheetEvent.mock()

        // Init VM
        vm.loadPickList("")

        // Execute code to be tested
        vm.onDetailsCtaClicked(testItem, false)

        // Verify
        assertThat(inlineDialogEventMock.verifyWithCapture())?.isEqualTo(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    title = StringIdHelper.Id(R.string.scan_item),
                    dialogType = BottomSheetType.ItemDetail,
                    customDataParcel = ItemDetailsParams(
                        iaId = 123,
                        actId = 0,
                        activityNo = "",
                        altItemLocations = emptyList(),
                        pickListType = PickListType.Todo
                    )
                ),
                tag = ITEM_DETAIL_BOTTOMSHEET_TAG
            )
        )
    }

    @Test
    fun `WHEN substitute CTA is clicked THEN correct navigation event is triggered`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val navigationEventMock = vm.navigationEvent.mock()

        // Init VM
        vm.loadPickList("543")

        // Execute code to be tested
        vm.onSubstituteCtaClicked(testItem)

        // Verify
        assertThat(navigationEventMock.verifyWithCapture())?.isEqualTo(
            NavigationEvent.Directions(
                PickListItemsFragmentDirections.actionPickListItemsFragmentToSubstituteFragment(
                    SubstituteParams(
                        iaId = 123,
                        pickListId = "543",
                        path = null,
                        swapSubstitutionReason = null,
                        substitutionRemovedQty = null
                    )
                )
            )
        )
    }

    @Test
    fun `WHEN short CTA is clicked and subs are allowed THEN substitution dialog event is triggered`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Init VM
        vm.loadPickList("")

        // Setup mocks
        val showSubShortDialogMock = vm.inlineDialogEvent.mock()

        // Execute code to be tested
        vm.onShortCtaClicked(testItem)

        // Verify
        assertThat(showSubShortDialogMock.verifyWithCapture())?.isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.short_item),
                    body = StringIdHelper.Id(R.string.short_sub_body),
                    positiveButtonText = StringIdHelper.Id(R.string.short_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                ),
                tag = "suggestSubstitutionDialogTag"
            )
        )
    }

    // TODO - Refactor test now that this screen is just a dialog in PickListItemsFragment
    /*@Test
    fun `WHEN short CTA is clicked and subs are not allowed THEN navigation event is triggered`() {
        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val navigationEventMock = vm.navigationEvent.mock()

        // Init VM
        vm.loadPickList("543")

        val noSubs: ItemActivityDto = mock {
            on { subAllowed } doReturn false
        }

        // Execute code to be tested
        vm.onShortCtaClicked(noSubs)

        // Verify
        assertThat(navigationEventMock.verifyWithCapture())?.isEqualTo(
            NavigationEvent.Directions(
                PickListItemsFragmentDirections.actionPickListItemsFragmentToShortItemFragment(item = noSubs)
            )
        )
    }*/

    @Test
    fun `WHEN totes CTA is clicked THEN correct navigation event is triggered`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val navigationEventMock = vm.navigationEvent.mock()

        // Init VM
        vm.loadPickList("543")

        // Execute code to be tested
        vm.onTotesCtaClicked()

        // Verify
        assertThat(navigationEventMock.verifyWithCapture())?.isEqualTo(
            NavigationEvent.Directions(
                PickListItemsFragmentDirections.actionPickListItemsFragmentToTotesFragment(
                    picklistid = "543"
                )
            )
        )
    }

    @Test
    fun `WHEN manualEntry CTA is clicked THEN correct navigation event is triggered`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val navigationEventMock = vm.navigationEvent.mock()

        // Init VM
        vm.loadPickList("543")

        // Execute code to be tested
        vm.onManualEntryCtaClicked()

        // Verify
        assertThat(navigationEventMock.verifyWithCapture())?.isEqualTo(
            NavigationEvent.Directions(
                PickListItemsFragmentDirections.actionPickListItemsFragmentToManualEntryPagerFragment(
                    ManualEntryPickParams(
                        selectedItem = testItem,
                        requestedQty = 2,
                        stageByTime = null,
                        isSubstitution = false,
                        entryType = ManualEntryType.UPC
                    ),
                    entryType = ManualEntryType.UPC
                )
            )
        )
    }

    @Test
    fun `WHEN onLabelSentToPrinter is called THEN correct navigation event is triggered`() {

        inlineKoinSingleSetUp()

        // Setup VM
        val vm = pickListItemsViewModelFactory()

        // Setup mocks
        val navigationEventMock = vm.navigationEvent.mock()

        // Init VM
        vm.loadPickList("543")

        // Execute code to be tested
        vm.onLabelSentToPrinter("66889")

        // Verify
        assertThat(navigationEventMock.verifyWithCapture())?.isEqualTo(
            NavigationEvent.Directions(
                PickListItemsFragmentDirections.actionPickListItemsFragmentToStagingFragment(
                    activityId = "66889",
                    isPreviousPrintSuccessful = false,
                    shouldClearData = true
                )
            )
        )
    }

    fun inlineKoinSingleSetUp() {
        inlineKoinSingle<MainActivityViewModel>(override = true) {
            mock {
                on { isLoading } doReturn MutableLiveData(false)
            }
        }

        inlineKoinSingle<InvalidItemScanTracker>(override = true) {
            mock {}
        }

        inlineKoinSingle<ImagePreCacher>(override = true) {
            mock {}
        }

        inlineKoinSingle<Toaster>(override = true) {
            mock {}
        }
    }

    // TODO - endpick variations

    // /////////////////////////////////////////////////////////////////////////
    // Test Objects and Factories
    // /////////////////////////////////////////////////////////////////////////

    private fun pickListItemsViewModelFactory(
        app: Application = testApplicationFactory()
    ) = PickListItemsViewModel(app = app, activityViewModelFactory(app))
}
