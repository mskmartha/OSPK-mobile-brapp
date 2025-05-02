package com.albertsons.acupick.data.repository

import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ItemUpcDto
import com.albertsons.acupick.data.test.BaseTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class PickRepositoryImplementationTest {

    @RunWith(Parameterized::class)
    class AppendPrimaryBarcodeParameterizedTests(val testName: String, val itemActivities: List<ItemActivityDto>, val input: List<ItemUpcDto>, val output: List<ItemUpcDto>) : BaseTest() {

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf(
                    "WHEN there is an empty upc list return new list with only the primary upc",
                    listOf(ItemActivityDto(itemId = "12345", primaryUpc = "777")),
                    emptyList<ItemUpcDto>(),
                    listOf(ItemUpcDto("12345", listOf("777"))),
                ),
                arrayOf(
                    "WHEN there is a null primary UPC return unaltered list",
                    listOf(ItemActivityDto(itemId = "12345", primaryUpc = null)),
                    listOf(ItemUpcDto("12345", listOf("1111", "2222"))),
                    listOf(ItemUpcDto("12345", listOf("1111", "2222"))),
                ),
                arrayOf(
                    "WHEN there is an empty primary UPC return unaltered list",
                    listOf(ItemActivityDto(itemId = "12345", primaryUpc = "")),
                    listOf(ItemUpcDto("12345", listOf("1111", "2222"))),
                    listOf(ItemUpcDto("12345", listOf("1111", "2222"))),
                ),
                arrayOf(
                    "WHEN there is no matching item return unaltered list",
                    listOf(ItemActivityDto(itemId = "55555", primaryUpc = "")),
                    listOf(ItemUpcDto("12345", listOf("1111", "2222"))),
                    listOf(ItemUpcDto("12345", listOf("1111", "2222"))),
                ),
                arrayOf(
                    "WHEN there is a matching primary UPC return list with primary upc appended",
                    listOf(ItemActivityDto(itemId = "55555", primaryUpc = "555"), ItemActivityDto(itemId = "12345", primaryUpc = "777")),
                    listOf(ItemUpcDto("12345", listOf("1111", "2222")), ItemUpcDto("55555", listOf("888", "999"))),
                    listOf(ItemUpcDto("12345", listOf("1111", "2222", "777")), ItemUpcDto("55555", listOf("888", "999", "555"))),
                ),
                arrayOf(
                    "WHEN there is a matching primary UPC and it is already in the original list return unaltered list",
                    listOf(ItemActivityDto(itemId = "55555", primaryUpc = "555"), ItemActivityDto(itemId = "12345", primaryUpc = "777")),
                    listOf(ItemUpcDto("12345", listOf("777", "1111", "2222")), ItemUpcDto("55555", listOf("888", "999", "555"))),
                    listOf(ItemUpcDto("12345", listOf("777", "1111", "2222")), ItemUpcDto("55555", listOf("888", "999", "555"))),
                ),
            )
        }

        @Test
        fun appendPrimaryUpcToItemUpcList_givenInput_returnsExpectedResult() {
            val result = input.appendPrimaryUpcToItemUpcList(itemActivities)
            assertThat(result).isEqualTo(output)
        }
    }
}
