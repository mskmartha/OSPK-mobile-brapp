package com.albertsons.acupick.ui

import android.graphics.drawable.Drawable
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.User
import com.albertsons.acupick.data.model.ValidCredentialModel
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.ServerErrorCodeDto
import com.albertsons.acupick.data.model.response.ServerErrorDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.network.NetworkAvailabilityManagerImplementation
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.activityViewModelFactory
import com.albertsons.acupick.test.getPrivateProperty
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.ui.MainActivityViewModelTest.TestBarcodeMapper.Companion.testBagOne
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.util.StringIdHelper
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import java.util.concurrent.TimeoutException

class MainActivityViewModelTest : BaseTest() {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    class TestUserRepo : UserRepository {
        override val user: StateFlow<User?>

            get() = MutableStateFlow(_user)
        override val isLoggedIn: StateFlow<Boolean>
            get() = MutableStateFlow(_isLoggedIn)
        private var _user: User? = null
        private var _isLoggedIn = true
        override suspend fun login(credentials: ValidCredentialModel): ApiResult<Unit> {
            _isLoggedIn = true
            return ApiResult.Success(Unit)
        }

        override suspend fun logout() {
            _isLoggedIn = false
        }

        override suspend fun updateUser(user: User) {
            _user = user
        }
    }

    class TestBarcodeMapper : BarcodeMapper {

        override fun inferBarcodeType(barcodeIn: String, enableLogging: Boolean): BarcodeType {
            return if (barcodeIn == "testbarcode")
                testBagOne
            else BarcodeType.Unknown(rawBarcode = barcodeIn)
        }

        override fun generateWeightedBarcode(plu: String, weightString: String): BarcodeType.Item.Weighted {
            TODO("Not yet implemented")
        }

        override fun generateEachBarcode(plu: String, itemActivityDbId: Long?): BarcodeType.Item.Each {
            TODO("Not yet implemented")
        }

        override fun generateDisplayBarCode(catalogUpc: String?): String {
            TODO("Not yet implemented")
        }

        companion object {
            val testBagOne = BarcodeType.Bag(rawBarcode = "1234", bagOrToteId = "test bag id", customerOrderNumber = "test order num", displayToteId = "test tote id")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getNetworkAvailabilityManager(vm: MainActivityViewModel): NetworkAvailabilityManager {
        return (vm.getPrivateProperty("networkAvailabilityManager") as NetworkAvailabilityManager)
    }

    @Test
    fun mainActivityViewModel_onNavigationIntercepted() {
        val maVM = activityViewModelFactory()
        val navigationButtonMock = maVM.navigationButtonIntercept.mock()
        maVM.onNavigationIntercepted()
        assertThat(navigationButtonMock.verifyWithCapture(times(1))).isNotNull()
    }

    @Test
    fun mainActivityViewModel_updateNetworkStatus() {
        val testNetManager = NetworkAvailabilityManagerImplementation()
        val maVM = activityViewModelFactory(networkAvailabilityManager = testNetManager, networkAvailabilityController = testNetManager)
        maVM.updateNetworkStatus(false)
        assertThat(getNetworkAvailabilityManager(maVM).isConnected.value).isEqualTo(false)
        maVM.updateNetworkStatus(true)
        assertThat(getNetworkAvailabilityManager(maVM).isConnected.value).isEqualTo(true)
    }

    @Test
    fun mainActivityViewModel_logout() {
        val userRepo = TestUserRepo()
        val maVM = activityViewModelFactory(userRepo = userRepo)
        runBlocking {
            assertThat(userRepo.isLoggedIn.firstOrNull()).isEqualTo(true)
            maVM.manualLogout()
            assertThat(userRepo.isLoggedIn.firstOrNull()).isEqualTo(false)
        }
    }

    @Test
    fun mainActivityViewModel_setScannedData() {
        val barcodeMapper = TestBarcodeMapper()
        val maVM = activityViewModelFactory(barcodeMapper = barcodeMapper)
        val scannedDataMock = maVM.scannedData.mock()
        maVM.setScannedData("testbarcode")
        assertThat(scannedDataMock.verifyWithCapture(atLeastOnce())).isEqualTo(testBagOne)
    }

    @Test
    fun mainActivityViewModel_setToolbarLeftExtraImage() {
        val maVM = activityViewModelFactory()
        val toolbarLeftExtraImageMock = maVM.toolbarLeftExtraImage.mock()
        val toolbarLeftExtraImage: Drawable = mock {}
        maVM.setToolbarLeftExtraImage()
        try {
            assertThat(maVM.toolbarLeftExtraImage.getOrAwaitValue())
        } catch (e: TimeoutException) {
            assertThat(e.localizedMessage).isEqualTo("LiveData value was never set.")
        }
        maVM.setToolbarLeftExtraImage(toolbarLeftExtraImage)
        assertThat(toolbarLeftExtraImageMock.verifyWithCapture(atLeastOnce())).isEqualTo(toolbarLeftExtraImage)
    }

    @Test
    fun mainActivityViewModel_clearToolbar() {
        val maVM = activityViewModelFactory()

        val toolbarTitleMock = maVM.toolbarTitle.mock()
        val toolbarTitle = "toolbar title"
        maVM.setToolbarTitle()
        assertThat(toolbarTitleMock.verifyWithCapture(atLeastOnce())).isEqualTo("")
        maVM.setToolbarTitle(toolbarTitle)
        assertThat(toolbarTitleMock.verifyWithCapture(atLeastOnce())).isEqualTo(toolbarTitle)

        val toolbarSmallTitleMock = maVM.toolbarSmallTitle.mock()
        val toolbarSmallTitle = "toolbar small title"
        maVM.setToolbarSmallTitle()
        assertThat(toolbarSmallTitleMock.verifyWithCapture(atLeastOnce())).isEqualTo("")
        maVM.setToolbarSmallTitle(toolbarSmallTitle)
        assertThat(toolbarSmallTitleMock.verifyWithCapture(atLeastOnce())).isEqualTo(toolbarSmallTitle)

        val toolbarLeftExtraMock = maVM.toolbarLeftExtra.mock()
        val toolbarLeftExtra = "toolbar left extra"
        maVM.setToolbarLeftExtra()
        assertThat(toolbarLeftExtraMock.verifyWithCapture(atLeastOnce())).isEqualTo("")
        maVM.setToolbarLeftExtra(toolbarLeftExtra)
        assertThat(toolbarLeftExtraMock.verifyWithCapture(atLeastOnce())).isEqualTo(toolbarLeftExtra)

        val toolbarExtraRightMock = maVM.toolbarExtraRight.mock()
        val testToolbarRightExtra = "test toolbar right extra"
        maVM.setToolbarRightExtra()
        assertThat(toolbarExtraRightMock.verifyWithCapture(atLeastOnce())).isEqualTo("")
        maVM.setToolbarRightExtra(testToolbarRightExtra)
        assertThat(toolbarExtraRightMock.verifyWithCapture(atLeastOnce())).isEqualTo(testToolbarRightExtra)

        val toolbarExtraRightCtaMock = maVM.toolbarExtraRightCta.mock()
        val testToolbarExtra = "test toolbar right extra cta"
        var toolbarClickString = "beforeString"
        maVM.setToolbarRightExtraCta(clickBlock = { toolbarClickString = "afterString1" })
        maVM.onToolbarRightExtraClick()
        assertThat(toolbarExtraRightCtaMock.verifyWithNullableCapture(atLeastOnce())).isEqualTo("")
        assertThat(toolbarClickString).isEqualTo("afterString1")
        maVM.setToolbarRightExtraCta(value = testToolbarExtra, clickBlock = { toolbarClickString = "afterString2" })
        maVM.onToolbarRightExtraClick()
        assertThat(toolbarExtraRightCtaMock.verifyWithNullableCapture(atLeastOnce())).isEqualTo(testToolbarExtra)
        assertThat(toolbarClickString).isEqualTo("afterString2")

        val toolbarRightExtraBottomMock = maVM.toolbarRightExtraBottom.mock()
        val toolbarRightExtraBottom = "toolbar right extra bottom"
        maVM.setToolbarRightExtraBottom()
        assertThat(toolbarRightExtraBottomMock.verifyWithCapture(atLeastOnce())).isEqualTo("")
        maVM.setToolbarRightExtraBottom(toolbarRightExtraBottom)
        assertThat(toolbarRightExtraBottomMock.verifyWithCapture(atLeastOnce())).isEqualTo(toolbarRightExtraBottom)

        val toolbarRightExtraTopMock = maVM.toolbarRightExtraTop.mock()
        val toolbarRightExtraTop = "toolbar right extra top"
        maVM.setToolbarRightExtraTop()
        assertThat(toolbarRightExtraTopMock.verifyWithCapture(atLeastOnce())).isEqualTo("")
        maVM.setToolbarRightExtraTop(toolbarRightExtraTop)
        assertThat(toolbarRightExtraTopMock.verifyWithCapture(atLeastOnce())).isEqualTo(toolbarRightExtraTop)

        val toolbarLeftExtraImageMock = maVM.toolbarLeftExtraImage.mock()
        val toolbarLeftExtraImage: Drawable = mock {}
        maVM.setToolbarLeftExtraImage()
        try {
            assertThat(maVM.toolbarLeftExtraImage.getOrAwaitValue())
        } catch (e: TimeoutException) {
            assertThat(e.localizedMessage).isEqualTo("LiveData value was never set.")
        }
        maVM.setToolbarLeftExtraImage(toolbarLeftExtraImage)
        assertThat(toolbarLeftExtraImageMock.verifyWithCapture(atLeastOnce())).isEqualTo(toolbarLeftExtraImage)
    }

    @Test
    fun mainActivityViewModel_triggerHomeButtonEvent() {
        val maVM = activityViewModelFactory()
        val triggerHomeClickEventMock = maVM.triggerHomeClickIntercept.mock()
        maVM.triggerHomeButtonEvent()
        assertThat(triggerHomeClickEventMock.verifyWithCapture(times(1))).isNotNull()
    }

    @Test
    fun mainActivityViewModel_showServerErrorDialog() {
        val maVM = activityViewModelFactory(app = testApplicationFactory())
        val testErrorTag = "errorDialogTag"
        val showErrorDialogMock = maVM.activityDialogEvent.mock()
        runBlocking {
            maVM.handleApiErrors(
                ApiResult.Failure.Server(
                    ServerErrorDto(
                        httpErrorCode = 500,
                        errorCode = ServerErrorCodeDto(
                            rawValue = 57,
                            resolvedType = ServerErrorCode.USER_NOT_VALID
                        ),
                        message = "Server message here",
                    )
                )
            )
        }
        assertThat(showErrorDialogMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            CustomDialogArgDataAndTag(
                tag = testErrorTag,
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.something_went_wrong),
                    body = StringIdHelper.Id(R.string.something_wrong_body),
                    secondaryBody = StringIdHelper.Raw(
                        "TYPE: API Response Error\n" +
                            "SOURCE: Backend\n" +
                            "HTTP CODE: 500\n" +
                            "SERVER CODE: Server error here\n" +
                            "MESSAGE: Server message here\n"
                    ),
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                    cancelOnTouchOutside = false
                )
            )
        )
    }

    @Test
    fun mainActivityViewModel_showNetworkErrorDialog() {
        val maVM = activityViewModelFactory(app = testApplicationFactory())
        val showErrorDialogMock = maVM.activityDialogEvent.mock()
        val testErrorTag = "genericErrorDialogTag"
        runBlocking {
            maVM.handleApiErrors(ApiResult.Failure.NetworkFailure.Timeout(Exception()))
        }
        assertThat(showErrorDialogMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            CustomDialogArgDataAndTag(
                tag = testErrorTag,
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.something_went_wrong),
                    body = StringIdHelper.Id(R.string.something_wrong_body),
                    secondaryBody = StringIdHelper.Raw(
                        "TYPE: Network Error\n" +
                            "SOURCE: Device\n" +
                            "CAUSE: java.lang.Exception\n" +
                            "ADDITIONAL INFO: Possible timeout or connection issue\n"
                    ),
                    positiveButtonText = StringIdHelper.Id(R.string.try_again),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                )
            )
        )
        runBlocking {
            maVM.handleApiErrors(ApiResult.Failure.NetworkFailure.VpnError(Exception()))
        }
        assertThat(showErrorDialogMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            CustomDialogArgDataAndTag(
                tag = testErrorTag,
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.something_went_wrong),
                    body = StringIdHelper.Id(R.string.something_wrong_body),
                    secondaryBody = StringIdHelper.Raw(
                        "TYPE: Network Error\n" +
                            "SOURCE: Device\n" +
                            "CAUSE: java.lang.Exception\n" +
                            "ADDITIONAL INFO: Possible VPN connection issue\n"
                    ),
                    positiveButtonText = StringIdHelper.Id(R.string.try_again),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                )
            )
        )
    }
}
