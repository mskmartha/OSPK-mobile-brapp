package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.response.AuthUserDto
import com.albertsons.acupick.data.model.response.SiteDto
import com.albertsons.acupick.data.test.BaseTest
import junit.framework.Assert.assertEquals
import org.junit.Test

class UserTest : BaseTest() {
    @Test
    fun `WHEN user has access to only one store THEN that store should become the selected store`() {
        val authUserDto = AuthUserDto(sites = listOf(SiteDto("1234", false)))
        assertEquals("1234", authUserDto.toUser().selectedStoreId)
    }

    @Test
    fun `WHEN user has access to only more than one store THEN no selected store will be set`() {
        val authUserDto = AuthUserDto(sites = listOf(SiteDto("1234", false), SiteDto("5678", true)))
        assertEquals(null, authUserDto.toUser().selectedStoreId)
    }

    @Test
    fun `WHEN user has access to no stores THEN no selected store will be set`() {
        val authUserDto = AuthUserDto()
        assertEquals(null, authUserDto.toUser().selectedStoreId)
    }
}
