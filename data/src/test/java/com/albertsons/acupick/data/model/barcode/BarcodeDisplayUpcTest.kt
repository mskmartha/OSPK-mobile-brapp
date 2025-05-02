package com.albertsons.acupick.data.model.barcode

import com.albertsons.acupick.data.test.BaseTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BarcodeDisplayUpcTest : BaseTest() {

    @Test
    fun `WHEN given valid primary UPC of type regular converts to regular UPC correctly`() {
        val barCodeMapper = BarcodeMapperImplementation()
        // short
        assertThat(barCodeMapper.generateDisplayBarCode("0001300000218")).isEqualTo("013000002189") // real value 01321809
        assertThat(barCodeMapper.generateDisplayBarCode("0005210000256")).isEqualTo("052100002569") // real value 05225619
        assertThat(barCodeMapper.generateDisplayBarCode("0005210000428")).isEqualTo("052100004280") // real value 05242810
        assertThat(barCodeMapper.generateDisplayBarCode("0007310000882")).isEqualTo("073100008825") // real value 07388215

        // regular
        assertThat(barCodeMapper.generateDisplayBarCode("0003800033360")).isEqualTo("038000333606")
        assertThat(barCodeMapper.generateDisplayBarCode("0007255400153")).isEqualTo("072554001536")
        // assertThat(barCodeMapper.generateDisplayBarCode("0008200077659")).isEqualTo("082000776591") BAD check digit comes back 8 instead of 1
        assertThat(barCodeMapper.generateDisplayBarCode("0008158445262")).isEqualTo("081584452621")
        assertThat(barCodeMapper.generateDisplayBarCode("0001100000291")).isEqualTo("011000002918")
        assertThat(barCodeMapper.generateDisplayBarCode("0085509900702")).isEqualTo("855099007023")
        assertThat(barCodeMapper.generateDisplayBarCode("0002190850507")).isEqualTo("021908505077")
        assertThat(barCodeMapper.generateDisplayBarCode("0007045900910")).isEqualTo("070459009107")
        assertThat(barCodeMapper.generateDisplayBarCode("0003663202631")).isEqualTo("036632026316")
    }

    // Valid primary UPC that would ideally be converted to a short but is not being done at this time as the backend doesn't provide a clue on whether the barcode is a short or not
    @Test
    fun `WHEN given valid primary UPC of type regular that would ideally be converted to a short converts to regular UPC correctly`() {
        val barCodeMapper = BarcodeMapperImplementation()
        assertThat(barCodeMapper.generateDisplayBarCode("0001300000218")).isEqualTo("013000002189") // real value 01321809
    }

    @Test
    fun `WHEN given null primary UPC returns empty`() {
        val barCodeMapper = BarcodeMapperImplementation()
        assertThat(barCodeMapper.generateDisplayBarCode(null)).isEqualTo("")
    }

    @Test
    fun `WHEN given invalid primary UPC of type regular THEN converts to regular UPC correctly`() {
        val barCodeMapper = BarcodeMapperImplementation()
        assertThat(barCodeMapper.generateDisplayBarCode("00038000333606")).isNotEqualTo("38000333606")
    }

    @Test
    fun `WHEN given 12 length barcode THEN passes through input unchanged`() {
        val barCodeMapper = BarcodeMapperImplementation()
        assertThat(barCodeMapper.generateDisplayBarCode("3800033360")).isEqualTo("3800033360")
    }

    @Test
    fun `WHEN given 14 length barcode THEN passes through input unchanged`() {
        val barCodeMapper = BarcodeMapperImplementation()
        assertThat(barCodeMapper.generateDisplayBarCode("380003336061")).isEqualTo("380003336061")
    }
}
