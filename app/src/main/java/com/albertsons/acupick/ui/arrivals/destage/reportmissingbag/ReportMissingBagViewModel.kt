package com.albertsons.acupick.ui.arrivals.destage.reportmissingbag

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.transform
import com.albertsons.acupick.wifi.utils.toCapitalize
import java.util.Locale

class ReportMissingBagViewModel(val app: Application) : BaseViewModel(app) {
    val reportMissingBagsParams = MutableLiveData<ReportMissingBagsParams>()
    val radioChecked = MutableLiveData<Int>()
    val confirmSelection = MutableLiveData<Unit>()
    val selectionId = MutableLiveData<String?>()
    val header = reportMissingBagsParams.transform { getStorageTypeLabel(it?.storageType).toCapitalize(Locale.getDefault()) }
    val missingItems = reportMissingBagsParams.transform { it.missingBagLabels() }
    private val REPORT_BAG_LABEL_MISSING_TAG = "reportBagLabelMissingTag${this.hashCode()}"

    init {
        registerCloseAction(REPORT_BAG_LABEL_MISSING_TAG) {
            closeActionFactory(positive = {
                confirmSelection.postValue(Unit)
            })
        }
    }

    // Extracted the common condition into a separate function
    private fun isToteOrLooseItem(params: ReportMissingBagsParams) = params.isMfcSite.orFalse() || params.isCustomerBagPreference == false

    fun toolbarTitle(params: ReportMissingBagsParams) = if (isToteOrLooseItem(params)) toteOrLooseItemToolbarTitle() else missingBagsToolbarTitle()

    fun buttonTitle(params: ReportMissingBagsParams) = if (isToteOrLooseItem(params)) reportToteOrLooseItem() else reportMissingBag()

    private fun missingBagsToolbarTitle() =
        app.getString(if (isMissingBags()) (R.string.bag_bypass_missing_bag_title) else R.string.bag_bypass_missing_bag_label_title)

    private fun reportMissingBag() =
        app.getString(if (isMissingBags()) (R.string.report_missing_bag) else R.string.report_missing_bag_label)

// Simplified function names and extracted common logic into separate functions
    private fun toteOrLooseItemToolbarTitle(): String {
        return when {
            isMissingLooseItem() -> app.getString(R.string.report_missing_loose_item)
            isMissingLooseItemLabel() -> app.getString(R.string.report_missing_tote_and_loose_item)
            isMissingBags() -> app.getString(R.string.bag_bypass_missing_tote_title)
            else -> app.getString(R.string.bag_bypass_missing_tote_label_title)
        }
    }

    private fun reportToteOrLooseItem(): String {
        return when {
            isMissingLooseItem() -> app.getString(R.string.report_missing_loose_item_btn)
            isMissingLooseItemLabel() -> app.getString(R.string.report_missing_loose_item_label_btn)
            isMissingBags() -> app.getString(R.string.report_missing_tote)
            else -> app.getString(R.string.report_missing_tote_label)
        }
    }
    private fun getStorageTypeLabel(storageType: StorageType?) = when (storageType) {
        StorageType.FZ -> app.getString(R.string.frozen)
        StorageType.CH -> app.getString(R.string.chilled)
        StorageType.AM -> app.getString(R.string.ambient)
        StorageType.HT -> app.getString(R.string.hot)
        else -> ""
    }

    fun getNoMissingItemsText(): String {
        when {
            isLooseItemOrLabelMissing() -> return app.getString(R.string.no_loose_item_missing)
            isMfcSiteOrCustomerBagPreferenceFalse() -> return app.getString(R.string.no_totes_item_missing)
            else -> return app.getString(R.string.no_bags_item_missing)
        }
    }

    fun confirmClicked() {
        val isMfc = reportMissingBagsParams.value?.isMfcSite.orFalse()
        val isCustomerBagPreference = reportMissingBagsParams.value?.isCustomerBagPreference.orFalse()
        val titleId = if (isMfc || !isCustomerBagPreference) confirmTitleToteOrLooseItem() else confirmTitleBagId()
        val bodyId = if (isMfc || !isCustomerBagPreference) confirmBodyToteOrLooseItem() else confirmBodyBagId()
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.Informational,
                    title = StringIdHelper.Id(titleId),
                    titleIcon = R.drawable.ic_alert,
                    shouldBoldTitle = true,
                    body = StringIdHelper.Format(bodyId, getStorageTypeLabel(reportMissingBagsParams.value?.storageType)),
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                ),
                tag = REPORT_BAG_LABEL_MISSING_TAG
            )
        )
    }

    private fun confirmTitleToteOrLooseItem(): Int {
        return when {
            isMissingLooseItem() -> R.string.report_losse_item_missing
            isMissingLooseItemLabel() -> R.string.report_losse_item_label_missing
            isMissingBags() -> R.string.report_tote_missing
            else -> R.string.report_tote_label_missing
        }
    }

    private fun confirmTitleBagId() =
        if (isMissingBags()) R.string.report_bag_missing else R.string.report_bag_label_missing

    // Extracted the common conditions into separate functions
    private fun isLooseItemOrLabelMissing() = reportMissingBagsParams.value?.isLooseItemMissing.orFalse() || reportMissingBagsParams.value?.isLooseItemLableMissing.orFalse()

    private fun isMfcSiteOrCustomerBagPreferenceFalse() = reportMissingBagsParams.value?.isMfcSite.orFalse() || reportMissingBagsParams.value?.isCustomerBagPreference == false

    private fun confirmBodyToteOrLooseItem(): Int {
        return when {
            isMissingLooseItem() -> R.string.report_loose_item_missing_body
            isMissingLooseItemLabel() -> R.string.report_loose_item_label_missing_body
            isMissingBags() -> R.string.report_tote_missing_body
            else -> R.string.report_tote_label_missing_body
        }
    }

    private fun confirmBodyBagId() =
        if (isMissingBags()) R.string.report_bag_missing_body else R.string.report_bag_label_missing_body

// Broke down the long function into smaller ones
    private fun ReportMissingBagsParams?.missingBagLabels() = run {
        when {
            isLooseItemOrLabelMissing() -> getLooseItemLabels()
            isMfcSiteOrCustomerBagPreferenceFalse() -> getToteLabels()
            else -> getBagLabels()
        }
    }

    private fun ReportMissingBagsParams?.getLooseItemLabels() = this?.zoneDataList?.filter {
        it.bagData?.zoneType == this.storageType && it.isComplete().not() && it.bagData?.isLoose == true
    }?.map {
        val prefix = if (isLooseItemMissing) R.string.loose_item_prefix else R.string.loose_item_label_prefix
        getStringWithLabelId(prefix, it.bagData?.labelId)
    }

    private fun ReportMissingBagsParams?.getToteLabels() = this?.zoneDataList?.filter {
        it.bagData?.zoneType == this.storageType && it.isComplete().not() && it.bagData?.isLoose == false
    }?.map {
        val prefix = if (isMissingBags()) R.string.tote_prefix else R.string.tote_label_prefix
        getStringWithLabelId(prefix, it.bagData?.labelId)
    }

    private fun ReportMissingBagsParams?.getBagLabels() = this?.zoneDataList?.filter {
        it.bagData?.zoneType == this.storageType && it.isComplete().not()
    }?.map {
        val prefix = if (isMissingBags == true) R.string.bag_prefix else R.string.bag_label_prefix
        getStringWithLabelId(prefix, it.bagData?.labelId)
    }

    /**
     * Returns a string with the given label ID appended to the end.
     * The label ID is shortened to its last two characters.
     */
    private fun getStringWithLabelId(stringId: Int, labelId: String?): String {
        return app.getString(stringId, labelId?.takeLast(2))
    }

    /*    private fun getStringWithFullToteId(stringId: Int, labelId: String?): String {
            return app.getString(stringId, labelId.orEmpty())
        }*/

    fun updateSelection(index: Int) {
        selectionId.postValue(selectionLabelId(index))
    }

    private fun isMissingBags() = reportMissingBagsParams.value?.isMissingBags.orFalse()
    private fun isMissingLooseItem() = reportMissingBagsParams.value?.isLooseItemMissing.orFalse()
    private fun isMissingLooseItemLabel() = reportMissingBagsParams.value?.isLooseItemLableMissing.orFalse()

    fun selectionLabelId(index: Int) = reportMissingBagsParams.value?.zoneDataList?.filter { it.bagData?.zoneType == reportMissingBagsParams.value?.storageType }?.get(index)?.bagData?.labelId
    fun navigateUp() = _navigationEvent.postValue(NavigationEvent.Up)
}
