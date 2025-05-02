package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.transform
import kotlinx.coroutines.flow.combine

class BagsPerTempZoneViewModel(val app: Application) : BaseViewModel(app) {

    val bagsPerTempParams = MutableLiveData<BagsPerTempZoneParams?>()
    private val amBags = bagsPerTempParams.transform { it?.bagsPerTempZoneDataList?.filter { it.storageType == StorageType.AM && it.containerType == ContainerType.BAG }?.size?.toString() ?: "0" }
    val amLoose = bagsPerTempParams.transform { it?.bagsPerTempZoneDataList?.filter { it.storageType == StorageType.AM && it.containerType == ContainerType.LOOSE_ITEM }?.size?.toString() ?: "0" }
    private val chBags = bagsPerTempParams.transform { it?.bagsPerTempZoneDataList?.filter { it.storageType == StorageType.CH && it.containerType == ContainerType.BAG }?.size?.toString() ?: "0" }
    val chLoose = bagsPerTempParams.transform { it?.bagsPerTempZoneDataList?.filter { it.storageType == StorageType.CH && it.containerType == ContainerType.LOOSE_ITEM }?.size?.toString() ?: "0" }
    private val fzBags = bagsPerTempParams.transform { it?.bagsPerTempZoneDataList?.filter { it.storageType == StorageType.FZ && it.containerType == ContainerType.BAG }?.size?.toString() ?: "0" }
    val fzLoose = bagsPerTempParams.transform { it?.bagsPerTempZoneDataList?.filter { it.storageType == StorageType.FZ && it.containerType == ContainerType.LOOSE_ITEM }?.size?.toString() ?: "0" }
    private val htBags = bagsPerTempParams.transform { it?.bagsPerTempZoneDataList?.filter { it.storageType == StorageType.HT && it.containerType == ContainerType.BAG }?.size?.toString() ?: "0" }
    val htLoose = bagsPerTempParams.transform { it?.bagsPerTempZoneDataList?.filter { it.storageType == StorageType.HT && it.containerType == ContainerType.LOOSE_ITEM }?.size?.toString() ?: "0" }

    val amZoneVisible = combine(amBags.asFlow(), amLoose.asFlow()) { bags, loose -> bags.toIntOrNull().getOrZero() > 0 || loose.toIntOrNull().getOrZero() > 0 }.asLiveData()
    val chZoneVisible = combine(chBags.asFlow(), chLoose.asFlow()) { bags, loose -> bags.toIntOrNull().getOrZero() > 0 || loose.toIntOrNull().getOrZero() > 0 }.asLiveData()
    val fzZoneVisible = combine(fzBags.asFlow(), fzLoose.asFlow()) { bags, loose -> bags.toIntOrNull().getOrZero() > 0 || loose.toIntOrNull().getOrZero() > 0 }.asLiveData()
    val htZoneVisible = combine(htBags.asFlow(), htLoose.asFlow()) { bags, loose -> bags.toIntOrNull().getOrZero() > 0 || loose.toIntOrNull().getOrZero() > 0 }.asLiveData()

    val amBagLabel = amBags.map { StringIdHelper.Plural(R.plurals.missing_bag_header_plural, it.toIntOrNull().getOrZero()).getString(getApplication()) }
    val chBagLabel = chBags.map { StringIdHelper.Plural(R.plurals.missing_bag_header_plural, it.toIntOrNull().getOrZero()).getString(getApplication()) }
    val fzBagLabel = fzBags.map { StringIdHelper.Plural(R.plurals.missing_bag_header_plural, it.toIntOrNull().getOrZero()).getString(getApplication()) }
    val htBagLabel = htBags.map { StringIdHelper.Plural(R.plurals.missing_bag_header_plural, it.toIntOrNull().getOrZero()).getString(getApplication()) }

    val amBagVisibility = amBags.map { it.toIntOrNull().getOrZero() > 0 }
    val chBagVisibility = chBags.map { it.toIntOrNull().getOrZero() > 0 }
    val fzBagVisibility = fzBags.map { it.toIntOrNull().getOrZero() > 0 }
    val htBagVisibility = htBags.map { it.toIntOrNull().getOrZero() > 0 }

    val amLooseVisibility = amLoose.map { it.toIntOrNull().getOrZero() > 0 }
    val chLooseVisibility = chLoose.map { it.toIntOrNull().getOrZero() > 0 }
    val fzLooseVisibility = fzLoose.map { it.toIntOrNull().getOrZero() > 0 }
    val htLooseVisibility = htLoose.map { it.toIntOrNull().getOrZero() > 0 }
    fun loadData(params: BagsPerTempZoneParams?) {
        bagsPerTempParams.postValue(params)
        changeToolbarTitleEvent.postValue(app.getString(R.string.bags_per_temp_zone_title, params?.bagAndLooseItemCount.toString()))
    }
}
