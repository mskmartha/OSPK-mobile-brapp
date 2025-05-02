package com.albertsons.acupick.ui

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.ui.dialog.ALREADY_ASSIGNED_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.google.common.truth.Truth.assertThat
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times

class BaseViewModelTest : BaseTest() {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    class TestBaseViewModel(val app: Application) : BaseViewModel(app) {
        val testMutableLiveData: MutableLiveData<String> = MutableLiveData("Test")
        val testMutableLiveDataUpdate: MutableLiveData<List<Int>> = MutableLiveData(listOf())
        val testFlowUpdate: Flow<List<Int>> = listOf(listOf(1)).asFlow()
        var testMutableUpdatingLiveData: LiveData<Int> = MutableLiveData()
        val testLiveEventUpdate = LiveEvent<Unit>()

        fun doOnCleared() {
            onCleared()
        }

        fun getUiActive(): Boolean = isUiActive

        fun notifyObservers() {
            testMutableLiveData.notifyObservers()
        }

        fun runUpdate() {
            testMutableUpdatingLiveData = testLiveEventUpdate.updating(source = testMutableLiveDataUpdate, block = { it.filter { filterIt -> filterIt == 1 }.sumBy { sumIt -> sumIt * 2 } })
        }

        fun runUpdateFlow() {
            testMutableUpdatingLiveData = testLiveEventUpdate.updating(source = testFlowUpdate, block = { it.filter { filterIt -> filterIt == 1 }.sumBy { sumIt -> sumIt * 2 } })
        }

        fun postToUpdatingLiveData() {
            testMutableLiveDataUpdate.postValue(listOf(1, 2, 3, 4))
            testLiveEventUpdate.postValue(Unit)
        }
    }

    private val context: Context = mock {
        // Order is important here, most generic match first. Last match is final value.
        on { getString(any(), anyVararg()) } doReturn ""
    }

    private val resources: Resources = mock {}

    // private val viewLifeCycleOwner: LifecycleOwner = mock {}
    //
    // private val navController: NavController = mock {}
    //
    // private val fragmentActivity: FragmentActivity = mock {}

    private fun testApplicationFactory(mockContext: Context = context): Application = mock {
        on { applicationContext } doReturn mockContext
        on { resources } doReturn resources
    }

    @Test
    fun baseViewModel_showReassignDialog() {
        val bvm = TestBaseViewModel(testApplicationFactory())
        val inlineDialogEventMock = bvm.inlineDialogEvent.mock()
        assertThat(bvm.inlineDialogEvent.value).isNull()
        bvm.serverErrorCannotAssignUser(CannotAssignToOrderDialogTypes.REGULAR, false)
        assertThat(inlineDialogEventMock.verifyWithCapture(atLeastOnce()))
            ?.isEqualTo(CustomDialogArgDataAndTag(data = ALREADY_ASSIGNED_PICKLIST_ARG_DATA, tag = BaseViewModel.SINGLE_ORDER_ERROR_DIALOG_TAG))
    }

    @Test
    fun baseViewModel_onCleared() {
        val bvm = TestBaseViewModel(testApplicationFactory())
        bvm.dialogTagCloseActionListenerMap.isNotEmpty()
        assertThat(bvm.dialogTagCloseActionListenerMap.isNotEmpty()).isTrue()
        bvm.doOnCleared()
        assertThat(bvm.dialogTagCloseActionListenerMap.isEmpty()).isTrue()
    }

    @Test
    fun baseViewModel_updateUiLifecycle() {
        val bvm = TestBaseViewModel(testApplicationFactory())
        bvm.updateUiLifecycle(active = false)
        assertThat(bvm.getUiActive()).isFalse()
        bvm.updateUiLifecycle(active = true)
        assertThat(bvm.getUiActive()).isTrue()
    }

    // TODO I'm unsure on how to complete this test.  It throws a NPE when it's run.
    // @Test
    // fun baseViewModel_observeNavigationEvents() {
    //     val bvm = TestBaseViewModel(testApplicationFactory())
    //     val externalNavigationEventMock = bvm.externalNavigationEvent.mock()
    //     var mockFragment: Fragment = mock {
    //         on { viewLifecycleOwner } doReturn viewLifeCycleOwner
    //         on { findNavController() } doReturn navController
    //         on { requireActivity() } doReturn fragmentActivity
    //     }
    //     bvm.observeNavigationEvents(mockFragment)
    //     assertThat(externalNavigationEventMock.verifyWithCapture(atLeastOnce())).isNotNull()
    // }

    @Test
    fun baseViewModel_notifyObservers() {
        val bvm = TestBaseViewModel(testApplicationFactory())
        val testMutableLiveDataMock = bvm.testMutableLiveData.mock()
        bvm.notifyObservers()
        assertThat(testMutableLiveDataMock.verifyWithCapture(times(2)))
            ?.isEqualTo("Test")
        bvm.notifyObservers()
        assertThat(testMutableLiveDataMock.verifyWithCapture(times(3)))
            ?.isEqualTo("Test")
    }

    @Test
    fun baseViewModel_handleApiError() {
        val bvm = TestBaseViewModel(testApplicationFactory())
        val failureApiResult: ApiResult.Failure = mock {}
        val apiErrorEventMock = bvm.apiErrorEvent.mock()
        bvm.handleApiError(errorType = failureApiResult, tag = "test tag here")
        assertThat(apiErrorEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(Pair("test tag here", failureApiResult))
    }

    @Test
    fun baseViewModel_updating() {
        val bvm = TestBaseViewModel(testApplicationFactory())
        bvm.runUpdate()
        val testMutableUpdatingLiveDataMock = bvm.testMutableUpdatingLiveData.mock()
        bvm.postToUpdatingLiveData()
        assertThat(testMutableUpdatingLiveDataMock.verifyWithCapture(atLeastOnce())).isEqualTo(2)
    }

    @Test
    fun baseViewModel_updatingFlow() {
        val bvm = TestBaseViewModel(testApplicationFactory())
        bvm.runUpdateFlow()
        val testMutableUpdatingLiveDataMock = bvm.testMutableUpdatingLiveData.mock()
        bvm.postToUpdatingLiveData()
        assertThat(testMutableUpdatingLiveDataMock.verifyWithCapture(atLeastOnce())).isEqualTo(2)
    }
}
