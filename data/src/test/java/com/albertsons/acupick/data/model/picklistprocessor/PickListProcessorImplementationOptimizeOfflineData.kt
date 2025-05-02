package com.albertsons.acupick.data.model.picklistprocessor

import com.albertsons.acupick.data.model.barcode.BarcodeMapperImplementation
import com.albertsons.acupick.data.picklist.PickListOperationsImplementation
import com.albertsons.acupick.data.test.pick.valid_optimize_sync_data_undo_qty_1
import com.albertsons.acupick.data.test.pick.valid_optimize_sync_data_undo_qty_3
import com.google.common.truth.Truth
import org.junit.Test
import org.mockito.kotlin.mock

class PickListProcessorImplementationOptimizeOfflineData {

    @Test
    fun optimizeOfflineData_givenValid_no_split_undo_picks_return_1_then_0_picks() {
        val pickListProcessor = PickListProcessorImplementation(BarcodeMapperImplementation(), PickListOperationsImplementation(mock {}))
        val offlineDataOne = pickListProcessor.optimizeOfflineData(valid_optimize_sync_data_undo_qty_3)
        Truth.assertThat(offlineDataOne?.itemPickRequestDtos?.size).isEqualTo(1)
        val offlineDataTwo = pickListProcessor.optimizeOfflineData(valid_optimize_sync_data_undo_qty_1)
        Truth.assertThat(offlineDataTwo?.itemPickRequestDtos?.size).isEqualTo(0)
    }
}
