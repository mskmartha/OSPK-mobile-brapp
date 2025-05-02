package com.albertsons.acupick.ui.staging

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.response.StagingType
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.StagingStateRepository
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.models.BagScanUI
import com.albertsons.acupick.ui.models.BagUI
import com.albertsons.acupick.ui.models.StagingPart2UiData
import com.albertsons.acupick.ui.models.ZoneBagCountUI
import com.albertsons.acupick.ui.models.ZoneLocationScanUI
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.koin.core.component.inject

class StagingPart2ViewModel(app: Application) : BaseViewModel(app) {

    // DI
    private val stagingStateRepo: StagingStateRepository by inject()
    private val siteRepo: SiteRepository by inject()

    // input
    val bagList = MutableLiveData<MutableList<BagUI>>(mutableListOf())
    val scannedBags = MutableStateFlow<List<BagScanUI>>(emptyList())

    // Input and used by layout
    val activity = MutableLiveData<StagingPart2UiData>()

    // Input
    val currentZone: MutableStateFlow<ZoneBagCountUI?> = MutableStateFlow(null)
    val scannedOrderNumber: MutableStateFlow<String?> = MutableStateFlow(null)

    // Input - Scanned Zone Locations
    // Scanned zone locations from remote, filtered by a specific order number
    val existingScannedZoneLocations = MutableStateFlow<List<ZoneLocationScanUI>>(emptyList())
    // Scanned zone locations from both remote and local sources, filtered by a specific order number
    val mergedZoneLocations = MutableStateFlow<List<ZoneLocationScanUI>>(emptyList())

    val isMultiSource = MutableLiveData(false)
    private val isShopFloor = siteRepo.stagingType == StagingType.SHOP_FLOOR

    // Pager refresh event - used to trigger updates of tote info to pager
    private val updatePagerEvent = LiveEvent<Unit>()

    val isCustomerPreferBag = activity.map { it.isCustomerBagPreference ?: true }

    init {
        updatePagerEvent.postValue(Unit)
    }

    // Group bags by zone to count, also mark current zone here
    private val zoneCounts = combine(currentZone, scannedBags, isMultiSource.asFlow(), scannedOrderNumber) { current, bags, isMultiSource, order ->
        if (!(isShopFloor && isMultiSource)) {
            bags.groupBy { it.zone }.map {
                ZoneBagCountUI(
                    zone = it.key,
                    zoneType = it.value.first().bag.zoneType,
                    scannedBagCount = it.value.count(),
                    isCurrent = if (order == it.value.first().customerOrderNumber) {
                        it.key == current?.zone
                    } else {
                        false
                    },
                    isMultiSource = isMultiSource,
                    bagOrToteScannedCount = it.value.count { bagScanUi -> !bagScanUi.bag.isLoose },
                    looseScannedCount = it.value.count { bagScanUi -> bagScanUi.bag.isLoose }
                )
            }
        } else {
            bags.groupBy { it.bag.zoneType }.map { zoneTypeMap ->
                zoneTypeMap.value.groupBy { it.zone }.map {
                    ZoneBagCountUI(
                        zone = it.value.first().zone,
                        zoneType = zoneTypeMap.key,
                        scannedBagCount = it.value.count(),
                        isCurrent = if (order == it.value.first().customerOrderNumber) {
                            zoneTypeMap.key == current?.zoneType
                        } else {
                            false
                        },
                        bagOrToteScannedCount = it.value.count { bagScanUi -> !bagScanUi.bag.isLoose },
                        looseScannedCount = it.value.count { bagScanUi -> bagScanUi.bag.isLoose },
                        isMultiSource = isMultiSource
                    )
                }
            }.flatten()
        }
    }

    val amZoneBagCounts = zoneCounts.map { counts -> counts.filter { it.zoneType == StorageType.AM } }.asLiveData()
    val chZoneBagCounts = zoneCounts.map { counts -> counts.filter { it.zoneType == StorageType.CH } }.asLiveData()
    val fzZoneBagCounts = zoneCounts.map { counts -> counts.filter { it.zoneType == StorageType.FZ } }.asLiveData()
    val htZoneBagCounts = zoneCounts.map { counts -> counts.filter { it.zoneType == StorageType.HT } }.asLiveData()

    // UI
    val amBagOrToteScannedCount = amZoneBagCounts.map { counts -> counts.sumOf { it.bagOrToteScannedCount } }
    val chBagOrToteScannedCount = chZoneBagCounts.map { counts -> counts.sumOf { it.bagOrToteScannedCount } }
    val fzBagOrToteScannedCount = fzZoneBagCounts.map { counts -> counts.sumOf { it.bagOrToteScannedCount } }
    val htBagOrToteScannedCount = htZoneBagCounts.map { counts -> counts.sumOf { it.bagOrToteScannedCount } }

    val amBagOrToteTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.AM && !it.isLoose }.size }
    val chBagOrToteTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.CH && !it.isLoose }.size }
    val fzBagOrToteTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.FZ && !it.isLoose }.size }
    val htBagOrToteTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.HT && !it.isLoose }.size }

    val amLooseScannedCount = amZoneBagCounts.map { counts -> counts.sumOf { it.looseScannedCount } }
    val chLooseScannedCount = chZoneBagCounts.map { counts -> counts.sumOf { it.looseScannedCount } }
    val fzLooseScannedCount = fzZoneBagCounts.map { counts -> counts.sumOf { it.looseScannedCount } }
    val htLooseScannedCount = htZoneBagCounts.map { counts -> counts.sumOf { it.looseScannedCount } }

    val amLooseTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.AM && it.isLoose }.size }
    val chLooseTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.CH && it.isLoose }.size }
    val fzLooseTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.FZ && it.isLoose }.size }
    val htLooseTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.HT && it.isLoose }.size }

    val amTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.AM }.size }
    val chTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.CH }.size }
    val fzTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.FZ }.size }
    val htTotalCount = bagList.map { bags -> bags.filter { it.zoneType == StorageType.HT }.size }

    val scannedCount = combine(amZoneBagCounts.asFlow(), chZoneBagCounts.asFlow(), fzZoneBagCounts.asFlow(), htZoneBagCounts.asFlow()) { amCounts, chCounts, fzCounts, htCounts ->
        amCounts.sumOf { it.scannedBagCount } + chCounts.sumOf { it.scannedBagCount } + fzCounts.sumOf { it.scannedBagCount } + htCounts.sumOf { it.scannedBagCount }
    }.asLiveData()

    val isComplete = combine(bagList.asFlow(), scannedCount.asFlow()) { bagsInThisOrder, scannedCount ->
        scannedCount == bagsInThisOrder.size && bagsInThisOrder.size != 0
    }.asLiveData()

    // Events
    val scrollToZone: LiveData<StorageType> = currentZone.map { it?.zoneType }.filterNotNull().asLiveData()

    val locationLabel: LiveData<Int> = isMultiSource.map { if (it && siteRepo.isDarkStoreEnabled) R.string.pallet_label else R.string.location_label }

    val amScannedZoneLocations = getZoneLocationsByStorageType(StorageType.AM)
    val chScannedZoneLocations = getZoneLocationsByStorageType(StorageType.CH)
    val fzScannedZoneLocations = getZoneLocationsByStorageType(StorageType.FZ)
    val htScannedZoneLocations = getZoneLocationsByStorageType(StorageType.HT)

    val amScannedZoneLocationsCount = getZoneLocationsCountByStorageType(StorageType.AM)
    val chScannedZoneLocationsCount = getZoneLocationsCountByStorageType(StorageType.CH)
    val fzScannedZoneLocationsCount = getZoneLocationsCountByStorageType(StorageType.FZ)
    val htScannedZoneLocationsCount = getZoneLocationsCountByStorageType(StorageType.HT)

    val amZoneHasNewLocation = hasNewScannedLocationsByStorageType(StorageType.AM)
    val chZoneHasNewLocation = hasNewScannedLocationsByStorageType(StorageType.CH)
    val fzZoneHasNewLocation = hasNewScannedLocationsByStorageType(StorageType.FZ)
    val htZoneHasNewLocation = hasNewScannedLocationsByStorageType(StorageType.HT)

    /**
     * Retrieves a LiveData that emits a list of unique location strings for a given storage type.
     * This function observes the list of existing scanned zone locations and transforms it
     * to emit only the locations associated with the specified storage type.
     *
     * @param storageType The StorageType for which to retrieve the locations.
     * @return A LiveData that emits a list of location strings for the given storage type.
     */
    private fun getZoneLocationsByStorageType(storageType: StorageType): LiveData<List<String>> {
        return existingScannedZoneLocations.map { zoneLocations ->
            getZoneLocationsByType(zoneLocations, storageType)
        }.asLiveData()
    }

    /**
     * Retrieves a LiveData that emits a count of unique location strings for a given storage type.
     * This function observes the list of existing scanned zone locations and transforms it
     * to emit only the count of locations associated with the specified storage type.
     *
     * @param storageType The StorageType for which to retrieve the locations.
     * @return A LiveData that emits a count of scanned locations for the given storage type.
     */
    private fun getZoneLocationsCountByStorageType(storageType: StorageType): LiveData<Int> {
        return existingScannedZoneLocations.map { zoneLocations ->
            getZoneLocationsByType(zoneLocations, storageType).size
        }.asLiveData()
    }

    /**
     * Checks if there are any new scanned locations for a specific storage type compared to the existing scanned locations.
     *
     * @param storageType The type of storage to filter the locations by.
     * @return A LiveData of Boolean. True if there are new scanned locations for the given storage type, false otherwise.
     * The LiveData emits a new Boolean value whenever the `scannedZoneLocations` LiveData emits a new list.
     */
    private fun hasNewScannedLocationsByStorageType(storageType: StorageType) = mergedZoneLocations
        .map { zoneLocations ->
            val scannedZoneLocations = getZoneLocationsByType(zoneLocations, storageType)
            val existingScannedZoneLocations = getZoneLocationsByType(existingScannedZoneLocations.value, storageType)
            scannedZoneLocations.any { it !in existingScannedZoneLocations }
        }.asLiveData()

    /**
     * Extracts a flattened list of location strings from a list of ZoneLocationScanUI objects
     * for a specific storage type.
     *
     * @param zoneLocations A list of ZoneLocationScanUI objects representing scanned zone locations.
     * @param zoneType The StorageType to filter the locations by.
     * @return A flattened list of location strings belonging to the specified storage type across all provided zone locations.
     */
    private fun getZoneLocationsByType(zoneLocations: List<ZoneLocationScanUI>, zoneType: StorageType) = zoneLocations.flatMap { zoneLocation ->
        // For each zone location, access its storage types (handling potential null),
        zoneLocation.storageTypes.orEmpty()
            // Filter the storage types to include only those with a container type matching the provided zoneType.
            .filter { it.containerType == zoneType }
            // For each matching storage type, extract its list of locations and flatten the resulting lists into a single list.
            .flatMap { it.locations }
    }
}
