package com.albertsons.acupick.infrastructure.utils

import com.albertsons.acupick.data.test.BaseTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StringUtilsTest : BaseTest() {

    @Test
    fun isNotNullOrEmpty_receiverIsNull_returnsFalse() {
        val sut: String? = null
        val result = sut.isNotNullOrEmpty()
        assertThat(result).isFalse()
    }

    @Test
    fun isNotNullOrEmpty_receiverIsEmpty_returnsFalse() {
        val sut: String? = ""
        val result = sut.isNotNullOrEmpty()
        assertThat(result).isFalse()
    }

    @Test
    fun isNotNullOrEmpty_receiverIsNotEmpty_returnsTrue() {
        val sut: String? = "a"
        val result = sut.isNotNullOrEmpty()
        assertThat(result).isTrue()
    }

    @Test
    fun isNotNullOrBlank() {
        val sut: String? = null
        val result = sut.isNotNullOrBlank()
        assertThat(result).isFalse()
    }

    @Test
    fun isNotNullOrBlank_receiverIsEmpty_returnsFalse() {
        val sut: String? = ""
        val result = sut.isNotNullOrBlank()
        assertThat(result).isFalse()
    }

    @Test
    fun isNotNullOrBlank_receiverHasSpace_returnsFalse() {
        val sut: String? = " "
        val result = sut.isNotNullOrBlank()
        assertThat(result).isFalse()
    }

    @Test
    fun isNotNullOrBlank_receiverHasWhiteSpace_returnsFalse() {
        assertThat("\n".isNotNullOrBlank()).isFalse()
        assertThat("\t".isNotNullOrBlank()).isFalse()
        assertThat("\r".isNotNullOrBlank()).isFalse()
        assertThat(" ".isNotNullOrBlank()).isFalse()
    }

    @Test
    fun isNotNullOrBlank_receiverIsNotBlank_returnsTrue() {
        val sut: String? = "a"
        val result = sut.isNotNullOrBlank()
        assertThat(result).isTrue()
    }
}
