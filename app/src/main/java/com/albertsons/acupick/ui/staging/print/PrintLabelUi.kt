package com.albertsons.acupick.ui.staging.print

import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.ui.models.BagUI
import com.albertsons.acupick.ui.models.ToteUI
import com.albertsons.acupick.ui.models.fullContactName
import com.albertsons.acupick.ui.staging.print.PrintLabelsSubItemUi.Companion.setBagOrToteNumber
import com.albertsons.acupick.ui.util.displayName
import kotlinx.android.parcel.Parcelize
import org.koin.java.KoinJavaComponent

@Parcelize
@Keep
data class PrintLabelUi(
    val printLabelsUi: List<PrintLabelsHeaderUi>? = null,
) : Parcelable

@Parcelize
@Keep
data class PrintLabelsHeaderUi(
    val nameOrType: String?,
    val storageType: StorageType,
    val customerOrderNumber: String?,
    val isTotes: Boolean?,
    val subItemUiList: List<PrintLabelsSubItemUi>?,
    val shortOrderId: String? = null,
    val customeName: String?,
    val isCustomerPreferBag: Boolean,
) : Parcelable {

    constructor(
        bagsUiList: List<BagUI>? = null,
        toteUiList: List<ToteUI>? = null,
        scannedBagIds: List<String>? = null,
        customerOrderNumber: String?,
        shortOrderId: String?,
        customeName: String?,
        isCustomerPreferBag: Boolean
    ) :
        this(
            nameOrType = setNameOrTypeString(bagsUiList, toteUiList),
            customerOrderNumber = customerOrderNumber,
            isTotes = bagsUiList == null && toteUiList != null,
            subItemUiList = getItemsSubViews(bagsUiList, toteUiList, scannedBagIds, isCustomerPreferBag),
            storageType = setStorageType(bagsUiList, toteUiList),
            shortOrderId = shortOrderId,
            customeName = customeName,
            isCustomerPreferBag = isCustomerPreferBag
        )

    companion object {
        val context: Context by KoinJavaComponent.inject(Context::class.java)

        private fun setCustomerName(bagsUiList: List<BagUI>?, toteUiList: List<ToteUI>?, isCustomerPreferBag: Boolean): String? {
            val printLabelSubItemUi = getItemsSubViews(bagsUiList, toteUiList, null, isCustomerPreferBag)
            val nameGroup = printLabelSubItemUi?.groupBy { it.customerName }
            var customerName: String? = ""

            bagsUiList?.map { bag ->
                customerName = nameGroup?.entries?.find { entry ->
                    entry.key == bag.fullContactName()
                }?.key
            } ?: toteUiList?.map { tote ->
                customerName = nameGroup?.entries?.find { entry ->
                    entry.key == tote.customerName
                }?.key
            }
            return customerName
        }

        fun setNameOrTypeString(bagsUiList: List<BagUI>?, toteUiList: List<ToteUI>?): String? {
            return bagsUiList?.getOrNull(0)?.zoneType?.displayName(context) ?: toteUiList?.getOrNull(0)?.storageType?.displayName(context)
        }

        fun setStorageType(bagsUiList: List<BagUI>?, toteUiList: List<ToteUI>?): StorageType {
            return bagsUiList?.getOrNull(0)?.zoneType ?: toteUiList?.getOrNull(0)?.storageType ?: StorageType.AM
        }

        fun setLabelCount(dbViewModel: List<PrintLabelsSubItemDbViewModel>?, isTotes: Boolean?, context: Context): String {
            val labelCount = dbViewModel?.count() ?: 0
            return if (isTotes == true) {
                context.resources.getQuantityString(R.plurals.staging_totes_plural, labelCount, labelCount)
            } else {
                context.resources.getQuantityString(R.plurals.staging_bags_plural, labelCount, labelCount)
            }
        }

        // Todo figure out how to pull Loose Items Count or if even needed because it shouldn't be
        fun setLooseItemCount(bagsUiList: List<BagUI>?) = bagsUiList?.count() ?: 0

        fun getItemsSubViews(bagsUiList: List<BagUI>?, toteUiList: List<ToteUI>?, scannedBagIds: List<String>?, isCustomerPreferBag: Boolean) =
            bagsUiList?.map { bagUI ->
                PrintLabelsSubItemUi(
                    bagOrToteNumber = setBagOrToteNumber(false, bagUI.bagId, isCustomerPreferBag, bagUI.isLoose),
                    bagOrToteNumberRawValue = bagUI.bagId,
                    customerName = bagUI.fullContactName(),
                    isScanned = scannedBagIds?.any { it == bagUI.bagId } ?: false,
                )
            } ?: toteUiList?.map { toteUi ->
                PrintLabelsSubItemUi(
                    bagOrToteNumber = setBagOrToteNumber(true, toteUi.toteId, isCustomerPreferBag),
                    bagOrToteNumberRawValue = toteUi.toteId,
                    customerName = toteUi.customerName,
                    isScanned = false,
                )
            }
    }
}

@Parcelize
@Keep
// TODO: Using non-null types is preferable, especially for these domain model style classes where none of the current nullable types should be nullable.
data class PrintLabelsSubItemUi(
    val bagOrToteNumber: String?,
    val bagOrToteNumberRawValue: String?,
    val customerName: String?,
    val isScanned: Boolean,
) : Parcelable {
    companion object {
        fun setBagOrToteNumber(isTotes: Boolean?, string: String?, isCustomerPreferBag: Boolean, isLoose: Boolean = false) =
            if (isTotes == false) {
                if (isLoose) {
                    "Loose ${string?.substring(4)}"
                } else if (!isCustomerPreferBag) {
                    "Tote ${string?.substring(3)}"
                } else {
                    "Bag ${string?.substring(4)}"
                }
            } else {
                string
            }
    }
}
