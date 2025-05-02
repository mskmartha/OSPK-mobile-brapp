package com.albertsons.acupick.data.model.picklistprocessor

import com.albertsons.acupick.data.model.OfflinePickData
import com.albertsons.acupick.data.model.OnlineInMemoryPickData
import com.albertsons.acupick.data.model.RequestResponse
import com.albertsons.acupick.data.model.SubstitutionCodeAdapter
import com.albertsons.acupick.data.model.barcode.BarcodeMapperImplementation
import com.albertsons.acupick.data.model.request.ActionTimeWrapper
import com.albertsons.acupick.data.model.request.ItemPickRequestDto
import com.albertsons.acupick.data.model.request.ShortPickRequestDto
import com.albertsons.acupick.data.model.request.UndoPickLocalDto
import com.albertsons.acupick.data.model.request.UndoPickRequestDto
import com.albertsons.acupick.data.model.request.UndoShortRequestDto
import com.albertsons.acupick.data.model.request.pickedTime
import com.albertsons.acupick.data.model.request.wrapActionTime
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ItemUpcDto
import com.albertsons.acupick.data.model.response.PickItemDto
import com.albertsons.acupick.data.model.response.ShortItemDto
import com.albertsons.acupick.data.network.IsoZonedDateTimeJsonAdapter
import com.albertsons.acupick.data.picklist.PickListOperationsImplementation
import com.albertsons.acupick.data.test.BaseTest
import com.albertsons.acupick.data.test.pick.OfflineTestType
import com.albertsons.acupick.data.test.pick.TEST_1_ACTIVITY_DETAIL_ONE_PICK_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_ACTIVITY_DETAIL_ONE_PICK_SHORT_REMAINING_QTY_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_ACTIVITY_DETAIL_ONE_SHORT_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_ACTIVITY_DETAIL_ONE_SUB_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_ACTIVITY_DETAIL_TWO_PICKS_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_ACTIVITY_DETAIL_ZERO_PICKS_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_ITEM_UPC_LIST_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_RECORD_PICK_ONE_ITEM_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_RECORD_SHORT_ONE_ITEM_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_RECORD_SUB_ONE_ITEM_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_RECORD_SUB_ONE_ITEM_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_UNDO_PICK_ONE_TTC02_ITEM_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_UNDO_PICK_ONE_ITEM_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_1_UNDO_SHORT_ONE_ITEM_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.TEST_2_RECORD_PICK_ONE_ITEM_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_2_RECORD_PICK_ONE_ITEM_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.TEST_2_UNDO_PICK_ONE_ITEM_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_3_ACTIVITY_DETAIL_ONE_PICK_AM_JSON
import com.albertsons.acupick.data.test.pick.TEST_3_ACTIVITY_DETAIL_ONE_PICK_FZ_JSON
import com.albertsons.acupick.data.test.pick.TEST_3_ACTIVITY_DETAIL_ZERO_PICKS_JSON
import com.albertsons.acupick.data.test.pick.TEST_3_ITEM_UPC_LIST_JSON
import com.albertsons.acupick.data.test.pick.TEST_3_RECORD_PICK_ONE_AM_ITEM_TTC01_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_3_RECORD_PICK_ONE_AM_ITEM_TTC01_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.TEST_3_RECORD_PICK_ONE_FZ_ITEM_TTC01_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_3_RECORD_PICK_ONE_FZ_ITEM_TTC01_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.TEST_3_UNDO_PICK_ONE_FZ_ITEM_REQUEST_JSON
import com.albertsons.acupick.data.test.pick.TEST_3_UNDO_PICK_ONE_FZ_ITEM_RESPONSE_JSON
import com.albertsons.acupick.data.test.pick.removeUnnecessaryOfflineData
import com.albertsons.acupick.data.test.pick.removeUnnecessaryOnlineData
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock
import java.time.Duration
import java.time.ZonedDateTime

private val TEST_1_ITEM_UPC_LIST by lazy { TEST_1_ITEM_UPC_LIST_JSON.asItemUpcDtoList() }
private val TEST_3_ITEM_UPC_LIST by lazy { TEST_3_ITEM_UPC_LIST_JSON.asItemUpcDtoList() }

private val TEST_1_ACTIVITY_DETAIL_NO_PICKS by lazy { TEST_1_ACTIVITY_DETAIL_ZERO_PICKS_JSON.asActivityDto() }
private val TEST_1_ACTIVITY_DETAIL_ONE_PICK by lazy { TEST_1_ACTIVITY_DETAIL_ONE_PICK_JSON.asActivityDto() }
private val TEST_1_ACTIVITY_DETAIL_TWO_PICKS by lazy { TEST_1_ACTIVITY_DETAIL_TWO_PICKS_JSON.asActivityDto() }
private val TEST_1_ACTIVITY_DETAIL_ONE_SHORT by lazy { TEST_1_ACTIVITY_DETAIL_ONE_SHORT_JSON.asActivityDto() }
private val TEST_1_ACTIVITY_DETAIL_ONE_PICK_SHORT_REMAINING_QTY by lazy { TEST_1_ACTIVITY_DETAIL_ONE_PICK_SHORT_REMAINING_QTY_JSON.asActivityDto() }
private val TEST_1_ACTIVITY_DETAIL_ONE_SUB by lazy { TEST_1_ACTIVITY_DETAIL_ONE_SUB_JSON.asActivityDto() }
private val TEST_3_ACTIVITY_DETAIL_ZERO_PICKS by lazy { TEST_3_ACTIVITY_DETAIL_ZERO_PICKS_JSON.asActivityDto() }
private val TEST_3_ACTIVITY_DETAIL_ONE_PICK_FZ by lazy { TEST_3_ACTIVITY_DETAIL_ONE_PICK_FZ_JSON.asActivityDto() }
private val TEST_3_ACTIVITY_DETAIL_ONE_PICK_AM by lazy { TEST_3_ACTIVITY_DETAIL_ONE_PICK_AM_JSON.asActivityDto() }

private val TEST_1_RECORD_PICK_ONE_ITEM_REQUEST by lazy { TEST_1_RECORD_PICK_ONE_ITEM_REQUEST_JSON.asItemPickRequestDto() }
private val TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE by lazy { TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE_JSON.asPickItemDtoList() }
private val TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_REQUEST by lazy { TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_REQUEST_JSON.asItemPickRequestDto() }
private val TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_RESPONSE by lazy { TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_RESPONSE_JSON.asPickItemDtoList() }
private val TEST_1_UNDO_PICK_ONE_TTC02_ITEM_REQUEST by lazy { TEST_1_UNDO_PICK_ONE_TTC02_ITEM_REQUEST_JSON.asUndoPickLocalDto() }
private val TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST by lazy { TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST_JSON.asUndoPickLocalDto() }
private val TEST_1_UNDO_PICK_ONE_ITEM_RESPONSE by lazy { TEST_1_UNDO_PICK_ONE_ITEM_RESPONSE_JSON.asPickItemDtoList() }
private val TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST by lazy { TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST_JSON.asShortPickRequestDto() }
private val TEST_1_RECORD_SHORT_ONE_ITEM_RESPONSE by lazy { TEST_1_RECORD_SHORT_ONE_ITEM_RESPONSE_JSON.asShortItemDtoList() }
private val TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST by lazy { TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST_JSON.asUndoShortRequestDto() }
private val TEST_1_UNDO_SHORT_ONE_ITEM_RESPONSE by lazy { TEST_1_UNDO_SHORT_ONE_ITEM_RESPONSE_JSON.asShortItemDtoList() }
private val TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_REQUEST by lazy { TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_REQUEST_JSON.asShortPickRequestDto() }
private val TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_RESPONSE by lazy { TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_RESPONSE_JSON.asShortItemDtoList() }
private val TEST_1_RECORD_SUB_ONE_ITEM_REQUEST by lazy { TEST_1_RECORD_SUB_ONE_ITEM_REQUEST_JSON.asItemPickRequestDto() }
private val TEST_1_RECORD_SUB_ONE_ITEM_RESPONSE by lazy { TEST_1_RECORD_SUB_ONE_ITEM_RESPONSE_JSON.asPickItemDtoList() }

private val TEST_2_RECORD_PICK_ONE_ITEM_REQUEST by lazy { TEST_2_RECORD_PICK_ONE_ITEM_REQUEST_JSON.asItemPickRequestDto() }
private val TEST_2_RECORD_PICK_ONE_ITEM_RESPONSE by lazy { TEST_2_RECORD_PICK_ONE_ITEM_RESPONSE_JSON.asPickItemDtoList() }
private val TEST_2_UNDO_PICK_ONE_ITEM_REQUEST by lazy { TEST_2_UNDO_PICK_ONE_ITEM_REQUEST_JSON.asUndoPickLocalDto() }

private val TEST_3_RECORD_PICK_ONE_FZ_ITEM_TTC01_REQUEST by lazy { TEST_3_RECORD_PICK_ONE_FZ_ITEM_TTC01_REQUEST_JSON.asItemPickRequestDto() }
private val TEST_3_RECORD_PICK_ONE_FZ_ITEM_TTC01_RESPONSE by lazy { TEST_3_RECORD_PICK_ONE_FZ_ITEM_TTC01_RESPONSE_JSON.asPickItemDtoList() }
private val TEST_3_UNDO_PICK_ONE_FZ_ITEM_REQUEST by lazy { TEST_3_UNDO_PICK_ONE_FZ_ITEM_REQUEST_JSON.asUndoPickLocalDto() }
private val TEST_3_UNDO_PICK_ONE_FZ_ITEM_RESPONSE by lazy { TEST_3_UNDO_PICK_ONE_FZ_ITEM_RESPONSE_JSON.asPickItemDtoList() }
private val TEST_3_RECORD_PICK_ONE_AM_ITEM_TTC01_REQUEST by lazy { TEST_3_RECORD_PICK_ONE_AM_ITEM_TTC01_REQUEST_JSON.asItemPickRequestDto() }
private val TEST_3_RECORD_PICK_ONE_AM_ITEM_TTC01_RESPONSE by lazy { TEST_3_RECORD_PICK_ONE_AM_ITEM_TTC01_RESPONSE_JSON.asPickItemDtoList() }

// Prevents ktlint from failing on line length due to long test names - prefer to not auto format these test lines into multiple for readability
/* ktlint-disable max-line-length */
@RunWith(Enclosed::class)
class PickListProcessorImplementationTest {

    @RunWith(Parameterized::class)
    class OnlineParameterizedTests(private val testName: String, private val input: PickListProcessorInput, private val output: ActivityDto) : BaseTest() {

        companion object {

            private val ONLINE_TEST_1_ACTIVITY_DETAIL_NO_PICKS = TEST_1_ACTIVITY_DETAIL_NO_PICKS.removeUnnecessaryOnlineData()
            private val ONLINE_TEST_1_ACTIVITY_DETAIL_ONE_PICK = TEST_1_ACTIVITY_DETAIL_ONE_PICK.removeUnnecessaryOnlineData()
            private val ONLINE_TEST_1_ACTIVITY_DETAIL_TWO_PICKS = TEST_1_ACTIVITY_DETAIL_TWO_PICKS.removeUnnecessaryOnlineData()
            private val ONLINE_TEST_1_ACTIVITY_DETAIL_ONE_SHORT = TEST_1_ACTIVITY_DETAIL_ONE_SHORT.removeUnnecessaryOnlineData()
            private val ONLINE_TEST_1_ACTIVITY_DETAIL_ONE_PICK_SHORT_REMAINING_QTY = TEST_1_ACTIVITY_DETAIL_ONE_PICK_SHORT_REMAINING_QTY.removeUnnecessaryOnlineData()

            private val ONLINE_TEST_3_ACTIVITY_DETAIL_ZERO_PICKS = TEST_3_ACTIVITY_DETAIL_ZERO_PICKS.removeUnnecessaryOnlineData()
            private val ONLINE_TEST_3_ACTIVITY_DETAIL_ONE_PICK_FZ = TEST_3_ACTIVITY_DETAIL_ONE_PICK_FZ.removeUnnecessaryOnlineData()
            private val ONLINE_TEST_3_ACTIVITY_DETAIL_ONE_PICK_AM = TEST_3_ACTIVITY_DETAIL_ONE_PICK_AM.removeUnnecessaryOnlineData()

            // TODO: Use Koin test graph rather than manual creation
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf("WHEN no online data THEN return same data as input", setupInput(), ONLINE_TEST_1_ACTIVITY_DETAIL_NO_PICKS),
                arrayOf(
                    "WHEN 1 online record pick THEN return best representation of network activity details call",
                    setupInput().copy(
                        onlineInMemoryPickData = OnlineInMemoryPickData(
                            itemPickRequestDtos = listOf(
                                RequestResponse(request = TEST_1_RECORD_PICK_ONE_ITEM_REQUEST, response = TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE),
                            ),
                        ),
                    ),
                    ONLINE_TEST_1_ACTIVITY_DETAIL_ONE_PICK
                ),
                arrayOf(
                    "WHEN 2 online record picks to different totes THEN return best representation of network activity details call",
                    setupInput().copy(
                        onlineInMemoryPickData = OnlineInMemoryPickData(
                            itemPickRequestDtos = listOf(
                                RequestResponse(request = TEST_1_RECORD_PICK_ONE_ITEM_REQUEST, response = TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE),
                                RequestResponse(request = TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_REQUEST, response = TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_RESPONSE),
                            ),
                        ),
                    ),
                    ONLINE_TEST_1_ACTIVITY_DETAIL_TWO_PICKS
                ),
                arrayOf(
                    "WHEN changing online tote storage type THEN return same storage type as latest online pick",
                    setupInput(activityDto = ONLINE_TEST_3_ACTIVITY_DETAIL_ZERO_PICKS, itemUpcDtoList = TEST_3_ITEM_UPC_LIST).copy(
                        onlineInMemoryPickData = OnlineInMemoryPickData(
                            itemPickRequestDtos = listOf(
                                RequestResponse(request = TEST_3_RECORD_PICK_ONE_FZ_ITEM_TTC01_REQUEST, response = TEST_3_RECORD_PICK_ONE_FZ_ITEM_TTC01_RESPONSE),
                                RequestResponse(request = TEST_3_RECORD_PICK_ONE_AM_ITEM_TTC01_REQUEST, response = TEST_3_RECORD_PICK_ONE_AM_ITEM_TTC01_RESPONSE),
                            ),
                            undoItemPickRequestDtos = listOf(
                                RequestResponse(request = TEST_3_UNDO_PICK_ONE_FZ_ITEM_REQUEST.wrapActionTime(), response = TEST_3_UNDO_PICK_ONE_FZ_ITEM_RESPONSE),
                            ),
                        ),
                    ),
                    ONLINE_TEST_3_ACTIVITY_DETAIL_ONE_PICK_AM
                ),
                arrayOf(
                    "WHEN 1 online undo pick THEN return best representation of network activity details call",
                    setupInput(activityDto = ONLINE_TEST_1_ACTIVITY_DETAIL_ONE_PICK).copy(
                        onlineInMemoryPickData = OnlineInMemoryPickData(
                            undoItemPickRequestDtos = listOf(
                                RequestResponse(request = TEST_1_UNDO_PICK_ONE_TTC02_ITEM_REQUEST.wrapActionTime(), response = TEST_1_UNDO_PICK_ONE_ITEM_RESPONSE),
                            ),
                        ),
                    ),
                    ONLINE_TEST_1_ACTIVITY_DETAIL_NO_PICKS
                ),
                arrayOf(
                    "WHEN 1 online record pick followed by 1 undo pick THEN return same result as baseline (no-op)",
                    setupInput().copy(
                        onlineInMemoryPickData = OnlineInMemoryPickData(
                            itemPickRequestDtos = listOf(
                                RequestResponse(request = TEST_1_RECORD_PICK_ONE_ITEM_REQUEST, response = TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE),
                            ),
                            undoItemPickRequestDtos = listOf(
                                RequestResponse(request = TEST_1_UNDO_PICK_ONE_TTC02_ITEM_REQUEST.wrapActionTime(), response = TEST_1_UNDO_PICK_ONE_ITEM_RESPONSE),
                            ),
                        ),
                    ),
                    ONLINE_TEST_1_ACTIVITY_DETAIL_NO_PICKS
                ),
                arrayOf(
                    "WHEN 1 online record short THEN return best representation of network activity details call",
                    setupInput().copy(
                        onlineInMemoryPickData = OnlineInMemoryPickData(
                            shortPickRequestDtos = listOf(
                                RequestResponse(request = TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST, response = TEST_1_RECORD_SHORT_ONE_ITEM_RESPONSE),
                            ),
                        ),
                    ),
                    ONLINE_TEST_1_ACTIVITY_DETAIL_ONE_SHORT
                ),
                arrayOf(
                    "WHEN 1 online record pick and short remaining qty THEN return best representation of network activity details call",
                    setupInput().copy(
                        onlineInMemoryPickData = OnlineInMemoryPickData(
                            itemPickRequestDtos = listOf(
                                RequestResponse(request = TEST_1_RECORD_PICK_ONE_ITEM_REQUEST, response = TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE),
                            ),
                            shortPickRequestDtos = listOf(
                                RequestResponse(request = TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_REQUEST, response = TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_RESPONSE),
                            ),
                        ),
                    ),
                    ONLINE_TEST_1_ACTIVITY_DETAIL_ONE_PICK_SHORT_REMAINING_QTY
                ),
                arrayOf(
                    "WHEN 1 online undo short THEN return best representation of network activity details call",
                    setupInput(activityDto = ONLINE_TEST_1_ACTIVITY_DETAIL_ONE_SHORT).copy(
                        onlineInMemoryPickData = OnlineInMemoryPickData(
                            undoShortRequestDtos = listOf(
                                RequestResponse(request = TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST.wrapActionTime(), response = TEST_1_UNDO_SHORT_ONE_ITEM_RESPONSE),
                            ),
                        ),
                    ),
                    ONLINE_TEST_1_ACTIVITY_DETAIL_NO_PICKS
                ),
                arrayOf(
                    "WHEN 1 baseline short THEN return best representation of network activity details call",
                    setupInput(activityDto = ONLINE_TEST_1_ACTIVITY_DETAIL_ONE_SHORT),
                    ONLINE_TEST_1_ACTIVITY_DETAIL_ONE_SHORT
                ),
                arrayOf(
                    "WHEN 1 online record short followed by 1 undo short THEN return same result as baseline (no-op)",
                    setupInput().copy(
                        onlineInMemoryPickData = OnlineInMemoryPickData(
                            shortPickRequestDtos = listOf(
                                RequestResponse(request = TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST, response = TEST_1_RECORD_SHORT_ONE_ITEM_RESPONSE),
                            ),
                            undoShortRequestDtos = listOf(
                                RequestResponse(request = TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST.wrapActionTime(), response = TEST_1_UNDO_SHORT_ONE_ITEM_RESPONSE),
                            ),
                        ),
                    ),
                    ONLINE_TEST_1_ACTIVITY_DETAIL_NO_PICKS
                ),
            )

            private fun setupInput(activityDto: ActivityDto = ONLINE_TEST_1_ACTIVITY_DETAIL_NO_PICKS, itemUpcDtoList: List<ItemUpcDto> = TEST_1_ITEM_UPC_LIST): PickListProcessorInput {
                val upcToItemIdMap = itemUpcDtoList.asUpcToItemIdMap()
                return PickListProcessorInput(
                    baselineActivityDetails = activityDto,
                    OnlineInMemoryPickData(),
                    OfflinePickData(baselineActivityDetails = activityDto, itemUpcs = itemUpcDtoList),
                    upcToItemIdMap = upcToItemIdMap,
                )
            }
        }

        @Test
        fun deriveBaselineAndOnlineCombination_givenInput_returnsExpectedResult() {
            // TODO: Consider using fakes/mocks here
            val sut = PickListProcessorImplementation(BarcodeMapperImplementation(), PickListOperationsImplementation(mock {}))
            val result = sut.deriveBaselineAndOnlineCombination(input)
            assertThat(result).isEqualTo(output)
        }
    }

    @RunWith(Parameterized::class)
    class OfflineParameterizedTests(private val testName: String, private val input: PickListProcessorInput, private val output: ActivityDto) : BaseTest() {

        companion object {
            private val OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS = TEST_1_ACTIVITY_DETAIL_NO_PICKS.removeUnnecessaryOfflineData(OfflineTestType.Additive)
            private val OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_ONE_PICK = TEST_1_ACTIVITY_DETAIL_ONE_PICK.removeUnnecessaryOfflineData(OfflineTestType.Additive)
            private val OFFLINE_SUBTRACTIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS = TEST_1_ACTIVITY_DETAIL_NO_PICKS.removeUnnecessaryOfflineData(OfflineTestType.Subtractive)
            private val OFFLINE_SUBTRACTIVE_TEST_1_ACTIVITY_DETAIL_ONE_PICK = TEST_1_ACTIVITY_DETAIL_ONE_PICK.removeUnnecessaryOfflineData(OfflineTestType.Subtractive)

            private val OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_ONE_SHORT = TEST_1_ACTIVITY_DETAIL_ONE_SHORT.removeUnnecessaryOfflineData(OfflineTestType.Additive)
            private val OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_ONE_PICK_SHORT_REMAINING_QTY =
                TEST_1_ACTIVITY_DETAIL_ONE_PICK_SHORT_REMAINING_QTY.removeUnnecessaryOfflineData(OfflineTestType.Additive)
            private val OFFLINE_SUBTRACTIVE_TEST_1_ACTIVITY_DETAIL_ONE_SHORT = TEST_1_ACTIVITY_DETAIL_ONE_SHORT.removeUnnecessaryOfflineData(OfflineTestType.Subtractive)

            private val OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_ONE_SUB = TEST_1_ACTIVITY_DETAIL_ONE_SUB.removeUnnecessaryOfflineData(OfflineTestType.Additive)

            // TODO: Use Koin test graph rather than manual creation
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf("WHEN no offline data THEN return same data as input", setupInput(OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS), OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS),
                arrayOf(
                    "WHEN 1 offline record pick THEN return best representation of network activity details call",
                    setupInput(OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS).let {
                        it.copy(
                            offlinePickData = it.offlinePickData.copy(
                                itemPickRequestDtos = listOf(TEST_1_RECORD_PICK_ONE_ITEM_REQUEST),
                            ),
                        )
                    },
                    OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_ONE_PICK
                ),
                // arrayOf(
                //     "WHEN 1 offline undo pick THEN return best representation of network activity details call",
                //     setupInput(activityDto = OFFLINE_SUBTRACTIVE_TEST_1_ACTIVITY_DETAIL_ONE_PICK).let {
                //         it.copy(
                //             offlinePickData = it.offlinePickData.copy(
                //                 undoItemPickRequestDtos = listOf(TEST_1_UNDO_PICK_ONE_TTC02_ITEM_REQUEST.wrapActionTime()),
                //             ),
                //         )
                //     },
                //     OFFLINE_SUBTRACTIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS
                // ),
                arrayOf(
                    "WHEN 1 offline record short THEN return best representation of network activity details call",
                    setupInput(OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS).let {
                        it.copy(
                            offlinePickData = it.offlinePickData.copy(
                                shortPickRequestDtos = listOf(TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST),
                            ),
                        )
                    },
                    OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_ONE_SHORT
                ),
                arrayOf(
                    "WHEN 1 offline record pick and short remaining qty THEN return best representation of network activity details call",
                    setupInput(OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS).let {
                        it.copy(
                            offlinePickData = it.offlinePickData.copy(
                                itemPickRequestDtos = listOf(TEST_1_RECORD_PICK_ONE_ITEM_REQUEST),
                                shortPickRequestDtos = listOf(TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_REQUEST),
                            ),
                        )
                    },
                    OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_ONE_PICK_SHORT_REMAINING_QTY
                ),
                arrayOf(
                    "WHEN 1 offline undo short THEN return best representation of network activity details call",
                    setupInput(activityDto = OFFLINE_SUBTRACTIVE_TEST_1_ACTIVITY_DETAIL_ONE_SHORT).let {
                        it.copy(
                            offlinePickData = it.offlinePickData.copy(
                                undoShortRequestDtos = listOf(TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST.wrapActionTime()),
                            ),
                        )
                    },
                    OFFLINE_SUBTRACTIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS
                ),
                arrayOf(
                    "WHEN 1 offline record short and undo short THEN return same result as baseline (no-op)",
                    setupInput(activityDto = OFFLINE_SUBTRACTIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS).let {
                        it.copy(
                            offlinePickData = it.offlinePickData.copy(
                                shortPickRequestDtos = listOf(TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST),
                                undoShortRequestDtos = listOf(TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST.wrapActionTime()),
                            ),
                        )
                    },
                    OFFLINE_SUBTRACTIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS
                ),
                arrayOf(
                    "WHEN 1 offline record sub THEN return best representation of network activity details call",
                    setupInput(OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS).let {
                        it.copy(
                            offlinePickData = it.offlinePickData.copy(
                                itemPickRequestDtos = listOf(TEST_1_RECORD_SUB_ONE_ITEM_REQUEST),
                            ),
                        )
                    },
                    OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_ONE_SUB
                ),
            )

            private fun setupInput(
                activityDto: ActivityDto,
                itemUpcDtoList: List<ItemUpcDto> = TEST_1_ITEM_UPC_LIST
            ): PickListProcessorInput {
                val upcToItemIdMap = itemUpcDtoList.asUpcToItemIdMap()
                return PickListProcessorInput(
                    baselineActivityDetails = activityDto,
                    OnlineInMemoryPickData(),
                    OfflinePickData(baselineActivityDetails = activityDto, itemUpcs = itemUpcDtoList),
                    upcToItemIdMap = upcToItemIdMap,
                )
            }
        }

        @Test
        fun deriveNewBaselineWithOffline_givenInput_returnsExpectedResult() {
            // TODO: Consider using fakes/mocks here
            val sut = PickListProcessorImplementation(BarcodeMapperImplementation(), PickListOperationsImplementation(mock {}))
            val result = sut.deriveNewBaselineWithOffline(input.baselineActivityDetails, input)
            assertThat(result).isEqualTo(output)
        }
    }

    @RunWith(Parameterized::class)
    class PickedUpcIdLookupParameterizedTests(
        private val testName: String,
        private val baselineActivityDetails: ActivityDto,
        private val onlineInMemoryPickData: OnlineInMemoryPickData,
        private val output: Set<Long>,
    ) : BaseTest() {

        companion object {

            // TODO: Use Koin test graph rather than manual creation
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf("WHEN no picked upc ids THEN return empty set", TEST_1_ACTIVITY_DETAIL_NO_PICKS, OnlineInMemoryPickData(), emptySet<Long>()),
                arrayOf("WHEN 1 picked upc id in baseline THEN return matching set of 1", TEST_1_ACTIVITY_DETAIL_ONE_PICK, OnlineInMemoryPickData(), setOf(2119L)),
                arrayOf(
                    "WHEN 1 picked upc id in online THEN return matching set of 1",
                    TEST_1_ACTIVITY_DETAIL_NO_PICKS,
                    OnlineInMemoryPickData(
                        itemPickRequestDtos = listOf(RequestResponse(request = TEST_1_RECORD_PICK_ONE_ITEM_REQUEST, response = TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE)),
                    ),
                    setOf(2119L),
                ),
                arrayOf(
                    "WHEN 1 picked upc id in both baseline and in online THEN return matching set of 1",
                    TEST_1_ACTIVITY_DETAIL_ONE_PICK,
                    OnlineInMemoryPickData(
                        itemPickRequestDtos = listOf(RequestResponse(request = TEST_1_RECORD_PICK_ONE_ITEM_REQUEST, response = TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE)),
                    ),
                    setOf(2119L),
                ),
                arrayOf(
                    "WHEN 1 picked upc id in baseline and another 1 in online THEN return matching set of 2",
                    TEST_1_ACTIVITY_DETAIL_ONE_PICK,
                    OnlineInMemoryPickData(
                        itemPickRequestDtos = listOf(RequestResponse(request = TEST_2_RECORD_PICK_ONE_ITEM_REQUEST, response = TEST_2_RECORD_PICK_ONE_ITEM_RESPONSE)),
                    ),
                    setOf(2119L, 2120L),
                ),
            )
        }

        @Test
        fun lookupBaselineOnlinePickedUpcIds_givenInput_returnsExpectedResult() {
            // TODO: Consider using fakes/mocks here
            val sut = PickListProcessorImplementation(BarcodeMapperImplementation(), PickListOperationsImplementation(mock {}))
            val result = sut.lookupBaselineOnlinePickedUpcIds(baselineActivityDetails, onlineInMemoryPickData)
            assertThat(result).isEqualTo(output)
        }
    }

    @RunWith(Parameterized::class)
    class OptimizeOfflineDataParameterizedTests(private val testName: String, private val input: OfflinePickData?, private val output: OfflinePickData?) : BaseTest() {

        companion object {

            private val EMPTY_OFFLINE_PICK_DATA = OfflinePickData(baselineActivityDetails = TEST_1_ACTIVITY_DETAIL_NO_PICKS)

            // TODO: Use Koin test graph rather than manual creation
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf("WHEN null offline data THEN return same data as input", null, null),
                arrayOf("WHEN no offline data THEN return same data as input", EMPTY_OFFLINE_PICK_DATA, EMPTY_OFFLINE_PICK_DATA),
                // Pick/Undo Pick
                arrayOf(
                    "WHEN 1 offline record pick and 1 offline undo pick for the same item THEN return empty offline data",
                    EMPTY_OFFLINE_PICK_DATA.copy(
                        itemPickRequestDtos = listOf(TEST_1_RECORD_PICK_ONE_ITEM_REQUEST),
                        undoItemPickRequestDtos = listOf(TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.wrapActionTime()),
                    ),
                    EMPTY_OFFLINE_PICK_DATA
                ),
                arrayOf(
                    "WHEN 5 offline record pick and 5 offline undo pick with qty 1 for the same item THEN return empty offline data",
                    EMPTY_OFFLINE_PICK_DATA.copy(
                        itemPickRequestDtos = MutableList(5) { TEST_1_RECORD_PICK_ONE_ITEM_REQUEST },
                        undoItemPickRequestDtos = listOf(
                            TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.copy(undoPickRequestDto = TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.undoPickRequestDto.copy(qty = 1.0)).wrapActionTime(),
                            TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.copy(undoPickRequestDto = TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.undoPickRequestDto.copy(qty = 1.0)).wrapActionTime(),
                            TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.copy(undoPickRequestDto = TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.undoPickRequestDto.copy(qty = 1.0)).wrapActionTime(),
                            TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.copy(undoPickRequestDto = TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.undoPickRequestDto.copy(qty = 1.0)).wrapActionTime(),
                            TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.copy(undoPickRequestDto = TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.undoPickRequestDto.copy(qty = 1.0)).wrapActionTime()
                        ),
                    ),
                    EMPTY_OFFLINE_PICK_DATA
                ),
                arrayOf(
                    "WHEN 5 offline record pick and 5 offline undo pick for the same item THEN return empty offline data",
                    EMPTY_OFFLINE_PICK_DATA.copy(
                        itemPickRequestDtos = MutableList(5) { TEST_1_RECORD_PICK_ONE_ITEM_REQUEST },
                        undoItemPickRequestDtos = MutableList(5) { index -> TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.wrapActionTime(ZonedDateTime.now() + Duration.ofSeconds(index.toLong())) },
                    ),
                    EMPTY_OFFLINE_PICK_DATA
                ),
                arrayOf(
                    "WHEN 7 offline record pick and 5 offline undo pick for the same item THEN return 2 offline record picks",
                    EMPTY_OFFLINE_PICK_DATA.copy(
                        itemPickRequestDtos = MutableList(7) { TEST_1_RECORD_PICK_ONE_ITEM_REQUEST },
                        undoItemPickRequestDtos = MutableList(5) { index -> TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.wrapActionTime(ZonedDateTime.now() + Duration.ofSeconds(index.toLong())) },
                    ),
                    EMPTY_OFFLINE_PICK_DATA.copy(itemPickRequestDtos = MutableList(2) { TEST_1_RECORD_PICK_ONE_ITEM_REQUEST })
                ),
                arrayOf(
                    "WHEN 16 offline record pick (14 of item 1 and 2 of item 2) and 1 offline undo pick for item 2 THEN return 1 less of item 2 offline record picks",
                    EMPTY_OFFLINE_PICK_DATA.copy(
                        itemPickRequestDtos = MutableList(7) { TEST_1_RECORD_PICK_ONE_ITEM_REQUEST } + MutableList(2) { TEST_2_RECORD_PICK_ONE_ITEM_REQUEST } + MutableList(7) { TEST_1_RECORD_PICK_ONE_ITEM_REQUEST },
                        undoItemPickRequestDtos = MutableList(1) { index -> TEST_2_UNDO_PICK_ONE_ITEM_REQUEST.wrapActionTime(ZonedDateTime.now() + Duration.ofSeconds(index.toLong())) },
                    ),
                    EMPTY_OFFLINE_PICK_DATA.copy(itemPickRequestDtos = MutableList(7) { TEST_1_RECORD_PICK_ONE_ITEM_REQUEST } + MutableList(1) { TEST_2_RECORD_PICK_ONE_ITEM_REQUEST } + MutableList(7) { TEST_1_RECORD_PICK_ONE_ITEM_REQUEST })
                ),
                // Short/Undo Short
                arrayOf(
                    "WHEN 1 offline record short and 1 offline undo short for the same item THEN return empty offline data",
                    EMPTY_OFFLINE_PICK_DATA.copy(
                        shortPickRequestDtos = listOf(TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST),
                        undoShortRequestDtos = listOf(TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST.wrapActionTime()),
                    ),
                    EMPTY_OFFLINE_PICK_DATA
                ),
                arrayOf(
                    "WHEN 5 offline record short and 1 offline undo short with qty 5 for the same item THEN return empty offline data",
                    EMPTY_OFFLINE_PICK_DATA.copy(
                        shortPickRequestDtos = MutableList(5) { TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST },
                        undoShortRequestDtos = listOf(TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST.copy(qty = 5.0).wrapActionTime()),
                    ),
                    EMPTY_OFFLINE_PICK_DATA
                ),
                arrayOf(
                    "WHEN 5 offline record short and 5 offline undo short for the same item THEN return empty offline data",
                    EMPTY_OFFLINE_PICK_DATA.copy(
                        shortPickRequestDtos = MutableList(5) { TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST },
                        undoShortRequestDtos = MutableList(5) { index -> TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST.wrapActionTime(ZonedDateTime.now() + Duration.ofSeconds(index.toLong())) },
                    ),
                    EMPTY_OFFLINE_PICK_DATA
                ),
            )
        }

        @Test
        fun deriveBaselineAndOnlineCombination_givenInput_returnsExpectedResult() {
            // TODO: Consider using fakes/mocks here
            val sut = PickListProcessorImplementation(BarcodeMapperImplementation(), PickListOperationsImplementation(mock {}))
            val result = sut.optimizeOfflineData(input)
            val sortedResult = result?.sortForMatching()
            val sortedOutput = output?.sortForMatching()
            assertThat(sortedResult).isEqualTo(sortedOutput)
        }

        /** Since we are generating random shuffled lists, sort data for proper data class comparison */
        private fun OfflinePickData.sortForMatching(): OfflinePickData {
            return copy(
                itemPickRequestDtos = this.itemPickRequestDtos.sortedBy { it.pickedTime },
                undoItemPickRequestDtos = this.undoItemPickRequestDtos.sortedBy { it.actionTime }
            )
        }
    }

    @RunWith(Parameterized::class)
    class UnifiedPickListStateParameterizedTests(private val testName: String, private val input: PickListProcessorInput, private val output: ActivityDto) : BaseTest() {

        companion object {

            private val OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS = TEST_1_ACTIVITY_DETAIL_NO_PICKS.removeUnnecessaryOfflineData(OfflineTestType.Additive)
            private val OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_TWO_PICKS = TEST_1_ACTIVITY_DETAIL_TWO_PICKS.removeUnnecessaryOfflineData(OfflineTestType.Additive)

            // TODO: Use Koin test graph rather than manual creation
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf("WHEN no online/offline data THEN return same data as input", setupInput(), OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS),
                arrayOf(
                    "WHEN 1 online record and 1 offline record pick THEN return best representation of network activity details call",
                    setupInput().let {
                        it.copy(
                            onlineInMemoryPickData = OnlineInMemoryPickData(
                                itemPickRequestDtos = listOf(
                                    RequestResponse(request = TEST_1_RECORD_PICK_ONE_ITEM_REQUEST, response = TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE),
                                ),
                            ),
                            offlinePickData = it.offlinePickData.copy(
                                itemPickRequestDtos = listOf(TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_REQUEST),
                            ),
                        )
                    },
                    OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_TWO_PICKS
                ),
                arrayOf(
                    "WHEN many pick actions that cancel each other out are taken THEN return same result as baseline (no-op)",
                    setupInput().let {
                        it.copy(
                            onlineInMemoryPickData = OnlineInMemoryPickData(
                                itemPickRequestDtos = listOf(
                                    RequestResponse(request = TEST_1_RECORD_PICK_ONE_ITEM_REQUEST, response = TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE),
                                ),
                                undoItemPickRequestDtos = listOf(
                                    RequestResponse(request = TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.wrapActionTime(), response = TEST_1_UNDO_PICK_ONE_ITEM_RESPONSE),
                                ),
                                shortPickRequestDtos = listOf(
                                    RequestResponse(request = TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST, response = TEST_1_RECORD_SHORT_ONE_ITEM_RESPONSE),
                                ),
                                undoShortRequestDtos = listOf(
                                    RequestResponse(request = TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST.wrapActionTime(), response = TEST_1_UNDO_SHORT_ONE_ITEM_RESPONSE),
                                ),
                            ),
                            offlinePickData = it.offlinePickData.copy(
                                itemPickRequestDtos = listOf(TEST_1_RECORD_PICK_ONE_ITEM_REQUEST),
                                undoItemPickRequestDtos = listOf(TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST.wrapActionTime()),
                                shortPickRequestDtos = listOf(TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST),
                                undoShortRequestDtos = listOf(TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST.wrapActionTime()),
                            ),
                        )
                    },
                    OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS,
                ),
            )

            private fun setupInput(activityDto: ActivityDto = OFFLINE_ADDITIVE_TEST_1_ACTIVITY_DETAIL_NO_PICKS, itemUpcDtoList: List<ItemUpcDto> = TEST_1_ITEM_UPC_LIST): PickListProcessorInput {
                val upcToItemIdMap = itemUpcDtoList.asUpcToItemIdMap()
                return PickListProcessorInput(
                    baselineActivityDetails = activityDto,
                    OnlineInMemoryPickData(),
                    OfflinePickData(baselineActivityDetails = activityDto, itemUpcs = itemUpcDtoList),
                    upcToItemIdMap = upcToItemIdMap,
                )
            }
        }

        @Test
        fun processUnifiedPickListState_givenInput_returnsExpectedResult() {
            // TODO: Consider using fakes/mocks here
            val sut = PickListProcessorImplementation(BarcodeMapperImplementation(), PickListOperationsImplementation(mock {}))
            // Standard processing expects the offline data to be optimized before processing the unified state
            val optimizedInput = input.copy(offlinePickData = sut.optimizeOfflineData(input.offlinePickData)!!)
            val result = sut.processUnifiedPickListState(optimizedInput)
            assertThat(result.removeUnnecessaryOfflineData(OfflineTestType.Additive)).isEqualTo(output)
        }
    }
}

private fun generateItemPickRequestDto(repeat: Int, itemPickRequestDto: ItemPickRequestDto): MutableList<ActionTimeWrapper<ItemPickRequestDto>> {
    return MutableList(repeat) { index ->
        itemPickRequestDto.wrapActionTime(ZonedDateTime.now() + Duration.ofSeconds(index.toLong()))
    }
}

private fun generateUndoItemPickRequest(repeat: Int, undoPickRequestDto: UndoPickRequestDto): MutableList<ActionTimeWrapper<UndoPickRequestDto>> {
    return MutableList(repeat) { index ->
        undoPickRequestDto.wrapActionTime(ZonedDateTime.now() + Duration.ofSeconds(index.toLong()))
    }
}

// TODO: Consider adding to PickListOperations and removing from here and PickListRepository generateUpcToItemIdMap
private fun List<ItemUpcDto>.asUpcToItemIdMap(): Map<String, String> {
    val upcToItemIdMap = hashMapOf<String, String>()
    // build a map of upc -> itemId
    forEach { itemId ->
        itemId.upcList?.forEach { upc ->
            upcToItemIdMap[upc] = itemId.itemId ?: ""
        }
    }
    return upcToItemIdMap
}

private val moshi: Moshi = Moshi.Builder().add(ZonedDateTime::class.java, IsoZonedDateTimeJsonAdapter().nullSafe()).add(SubstitutionCodeAdapter()).build()
private val activityDtoAdapter: JsonAdapter<ActivityDto> = moshi.adapter(ActivityDto::class.java)
private val itemPickRequestDtoAdapter: JsonAdapter<ItemPickRequestDto> = moshi.adapter(ItemPickRequestDto::class.java)
private val undoPickRequestDtoAdapter: JsonAdapter<UndoPickRequestDto> = moshi.adapter(UndoPickRequestDto::class.java)
private val itemUpcDtoListAdapter: JsonAdapter<List<ItemUpcDto>> = moshi.adapter(Types.newParameterizedType(MutableList::class.java, ItemUpcDto::class.java))
private val pickItemDtoListAdapter: JsonAdapter<List<PickItemDto>> = moshi.adapter(Types.newParameterizedType(MutableList::class.java, PickItemDto::class.java))
private val shortPickRequestDtoAdapter: JsonAdapter<ShortPickRequestDto> = moshi.adapter(ShortPickRequestDto::class.java)
private val undoShortRequestDtoAdapter: JsonAdapter<UndoShortRequestDto> = moshi.adapter(UndoShortRequestDto::class.java)
private val shortItemDtoListAdapter: JsonAdapter<List<ShortItemDto>> = moshi.adapter(Types.newParameterizedType(MutableList::class.java, ShortItemDto::class.java))
private val undoPickLocalDtoAdapter: JsonAdapter<UndoPickLocalDto> = moshi.adapter(UndoPickLocalDto::class.java).lenient().nullSafe()

private fun String.asItemUpcDtoList(): List<ItemUpcDto> = itemUpcDtoListAdapter.fromJson(this)!!
private fun String.asActivityDto(): ActivityDto = activityDtoAdapter.fromJson(this)!!
private fun String.asItemPickRequestDto(): ItemPickRequestDto = itemPickRequestDtoAdapter.fromJson(this)!!
private fun String.asPickItemDtoList(): List<PickItemDto> = pickItemDtoListAdapter.fromJson(this)!!
private fun String.asUndoPickRequestDto(): UndoPickRequestDto = undoPickRequestDtoAdapter.fromJson(this)!!
private fun String.asShortPickRequestDto(): ShortPickRequestDto = shortPickRequestDtoAdapter.fromJson(this)!!
private fun String.asUndoShortRequestDto(): UndoShortRequestDto = undoShortRequestDtoAdapter.fromJson(this)!!
private fun String.asShortItemDtoList(): List<ShortItemDto> = shortItemDtoListAdapter.fromJson(this)!!
private fun String.asUndoPickLocalDto(): UndoPickLocalDto = undoPickLocalDtoAdapter.fromJson(this)!!

/* ktlint-enable max-line-length */
