package com.albertsons.acupick.infrastructure.utils

import com.albertsons.acupick.data.test.BaseTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal

@RunWith(Enclosed::class)
class DateTimeUtilsTest {

    @RunWith(Parameterized::class)
    class ParameterizedTests(private val testName: String, val formatter: DateTimeFormatter, private val input: Temporal, val output: String) : BaseTest() {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf(
                    "HOUR_MINUTE_AM_PM_TIME_FORMATTER_withInput_displaysCorrectly",
                    HOUR_MINUTE_AM_PM_TIME_FORMATTER,
                    LocalTime.of(13, 4),
                    "1:04 PM"
                )
            )
        }

        @Test
        fun dateTimeFormatter_givenInput_returnsExpectedResult() {
            val result = input.formattedWith(formatter)
            assertThat(result).isEqualTo(output)
        }
    }

    class StandardTests {

        @Test
        fun formattedWith_receiverNull_returnsNull() {
            val localDate: LocalDate? = null
            val result = localDate.formattedWith(HOUR_MINUTE_AM_PM_TIME_FORMATTER)
            assertThat(result).isNull()
        }

        @Test
        fun prettyPrint_withOneYearInput_returnsExpectedResult() {
            val input = Duration.ofDays(365)
            val result = input.prettyPrint()
            assertThat(result).isEqualTo("1 years, 0 days, 0 hours, 0 minutes, 0 seconds")
        }
        @Test
        fun prettyPrint_withOneDayInput_returnsExpectedResult() {
            val input = Duration.ofDays(1)
            val result = input.prettyPrint()
            assertThat(result).isEqualTo("1 days, 0 hours, 0 minutes, 0 seconds")
        }
        @Test
        fun prettyPrint_withOneHourInput_returnsExpectedResult() {
            val input = Duration.ofHours(1)
            val result = input.prettyPrint()
            assertThat(result).isEqualTo("1 hours, 0 minutes, 0 seconds")
        }
        @Test
        fun prettyPrint_withOneMinuteInput_returnsExpectedResult() {
            val input = Duration.ofMinutes(1)
            val result = input.prettyPrint()
            assertThat(result).isEqualTo("1 minutes, 0 seconds")
        }
        @Test
        fun prettyPrint_withOneSecondInput_returnsExpectedResult() {
            val input = Duration.ofSeconds(1)
            val result = input.prettyPrint()
            assertThat(result).isEqualTo("1 seconds")
        }
        @Test
        fun prettyPrint_withEmptyValuesTrue_returnsExpectedResult() {
            val input = Duration.ofSeconds(1)
            val result = input.prettyPrint(true)
            assertThat(result).isEqualTo("0 years, 0 days, 0 hours, 0 minutes, 1 seconds")
        }
    }
}
