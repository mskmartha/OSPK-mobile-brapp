package com.albertsons.acupick.ui.staging

import com.albertsons.acupick.data.model.BagData
import com.albertsons.acupick.data.model.ScannedBagData
import com.albertsons.acupick.data.model.StagingOneData
import com.albertsons.acupick.data.model.StagingOneDataJsonAdapter
import com.albertsons.acupick.data.model.StagingTwoData
import com.albertsons.acupick.data.model.StagingTwoDataJsonAdapter
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.Tote
import com.albertsons.acupick.data.network.IsoZonedDateTimeJsonAdapter
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test
import java.time.ZonedDateTime

private const val customerOrderNumber = "60606010"
private const val testActivityShortId = 1050
private const val testStagingActivityId = 123456L
private const val testStagingNextActivityId = 654321L

class StagingRepositoryTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(ZonedDateTime::class.java, IsoZonedDateTimeJsonAdapter().nullSafe()).build()

    private val stagingOneAdapter = StagingOneDataJsonAdapter(moshi)
    private val stagingTwoAdapter = StagingTwoDataJsonAdapter(moshi)

    private val ambientTote1 = "TTA61"
    private val ambientTote2 = "TTA62"
    private val ambientTote3 = "TTA63"

    @Test
    fun `WHEN stagingOne is serialized THEN it deserializes to the same values`() {

        val stagingOne = generateStagingOne()

        val jsonStagingOne = stagingOneAdapter.toJson(stagingOne)
        // not testing shared preferences - just serialize and deserialize to test Moshi-generated adapter
        val retrievedStagingOne = stagingOneAdapter.fromJson(jsonStagingOne)

        assertThat(stagingOne.toString()).isEqualTo(retrievedStagingOne.toString())
        val toteMap = stagingOne.totesByToteIdMap
        val retrievedToteMap = retrievedStagingOne?.totesByToteIdMap!!

        assertThat(toteMap.size).isEqualTo(retrievedToteMap.size)
        assertThat(retrievedToteMap[ambientTote1]?.bagCount).isEqualTo(toteMap[ambientTote1]?.bagCount)
        assertThat(retrievedToteMap[ambientTote3]?.looseItemCount).isEqualTo(toteMap[ambientTote3]?.looseItemCount)
    }

    @Test
    fun `WHEN stagingTwo is serialized THEN it deserializes to the same values`() {

        val stagingTwo = generateStagingTwo()

        val jsonStagingTwo = stagingTwoAdapter.toJson(stagingTwo)
        // not testing shared preferences - just serialize and deserialize to test Moshi-generated adapter
        val retrievedStagingTwo = stagingTwoAdapter.fromJson(jsonStagingTwo)

        assertThat(stagingTwo.toString()).isEqualTo(retrievedStagingTwo.toString())
    }

    @Test
    fun `WHEN stagingOne is completed THEN stagingTwo is synced`() {

        val stagingOne = generateStagingOne()
        stagingOne.complete() // set bags printed flag and next activity
        val stagingTwo = generateStagingTwo()

        assertThat(stagingOne.nextActivityId).isEqualTo(stagingTwo.activityId)
    }

    @Test
    fun `WHEN stagingTwo bags are scanned THEN bags are in sync`() {

        val stagingTwo = generateStagingTwo()

        val zoneAM = "AMG61"
        val zoneCH = "CHG61"
        val zoneFZ = "FZG61"

        // scan two bags to each temperature zone
        stagingTwo.scanBagsToZone(zoneAM, 2)
        stagingTwo.scanBagsToZone(zoneCH, 2)
        stagingTwo.scanBagsToZone(zoneFZ, 2)

        val stagedBags = stagingTwo.scannedBagList
        assertThat(stagedBags.size).isEqualTo(6)

        // verify bags as properly constructed
        val secondAmbientBag = stagedBags[1]
        assertThat(secondAmbientBag.zone).isEqualTo(zoneAM)
        assertThat(secondAmbientBag.bag.bagId).isEqualTo("${testActivityShortId}02")

        val secondChilledBag = stagedBags[3]
        assertThat(secondChilledBag.zone).isEqualTo(zoneCH)
        assertThat(secondChilledBag.bag.bagId).isEqualTo("${testActivityShortId}04")

        val secondFrozenBag = stagedBags[5]
        assertThat(secondFrozenBag.zone).isEqualTo(zoneFZ)
        assertThat(secondFrozenBag.bag.bagId).isEqualTo("${testActivityShortId}06")
    }

    private fun generateStagingOne(): StagingOneData {

        val toteMap = mutableMapOf<String, Tote>()
        toteMap[ambientTote1] = Tote(customerOrderNumber = customerOrderNumber, toteId = ambientTote1, bagCount = 1, looseItemCount = 2, storageType = StorageType.AM)
        toteMap[ambientTote2] = Tote(customerOrderNumber = customerOrderNumber, toteId = ambientTote2, bagCount = 3, looseItemCount = 4, storageType = StorageType.CH)
        toteMap[ambientTote3] = Tote(customerOrderNumber = customerOrderNumber, toteId = ambientTote3, bagCount = 5, looseItemCount = 6, storageType = StorageType.FZ)

        return StagingOneData(
            savedTitle = String(),
            activityId = testStagingActivityId,
            nextActivityId = -1L,
            bagLabelsPrintedSuccessfully = false,
            totesByToteIdMap = toteMap,
            isMultiSource = false
        )
    }

    private fun generateStagingTwo(): StagingTwoData {

        return StagingTwoData(
            customerOrderNumber = customerOrderNumber,
            activityId = testStagingNextActivityId,
            scannedBagList = mutableListOf(),
            unassignedToteIdList = mutableListOf(),
            isMultiSource = true
        )
    }

    private fun generateScannedBag(index1Based: Int): BagData {

        val formattedIndex = String.format("%02d", index1Based)
        return BagData(
            bagId = "$testActivityShortId$formattedIndex",
            zoneType = StorageType.AM,
            fulfillmentOrderNumber = customerOrderNumber,
            customerOrderNumber = customerOrderNumber,
            contactFirstName = "Amber",
            contactLastName = "Pan",
            containerId = "$testActivityShortId$formattedIndex",
            isBatch = false,
        )
    }

    private fun StagingOneData.complete() {
        this.bagLabelsPrintedSuccessfully = true
        this.nextActivityId = testStagingNextActivityId
    }

    private fun StagingTwoData.scanBagsToZone(zoneId: String, bagsToScan: Int) {

        val bagList = this.scannedBagList.toMutableList()
        for (i in 0 until bagsToScan) {
            val bag = generateScannedBag(bagList.size + 1)
            val scannedBagData = ScannedBagData(customerOrderNumber = customerOrderNumber, zone = zoneId, bag = bag, isLoose = false)
            bagList.add(scannedBagData)
        }

        scannedBagList = bagList
    }
}
