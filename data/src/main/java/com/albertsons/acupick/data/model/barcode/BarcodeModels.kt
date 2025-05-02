package com.albertsons.acupick.data.model.barcode

import android.os.Parcelable
import com.albertsons.acupick.data.model.DomainModel
import com.albertsons.acupick.data.model.StorageType
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.text.NumberFormat
import java.util.Locale

interface BarcodeValue : Parcelable {
    /** Barcode value from datawedge/manual entry that has been trimmed (only modification to the raw value received) */
    val rawBarcode: String
}

interface CatalogBarcodeValue : Parcelable {
    /** Upc value that is only expected to be used for lookup against the upcs returned for a given item. Do not send to the backend! */
    val catalogLookupUpc: String
}

interface StagingContainer : Parcelable {
    val customerOrderNumber: String
    val bagOrToteId: String
    val displayToteId: String
}

sealed interface PickingContainer : Parcelable {
    val rawBarcode: String
}

fun PickingContainer.asBarcodeType(): BarcodeType? = this as? BarcodeType

/** Defines all supported (as well as Unknown) barcode types needed for the picker app, with more granular information on the specific types. */
sealed class BarcodeType : BarcodeValue, DomainModel {
    /** Types of Items that a barcode can represent. Generate check digits and convert upc a <-> upc e: https://www.morovia.com/education/utility/upc-ean.asp */
    sealed class Item : BarcodeType(), CatalogBarcodeValue {
        /**
         * Starts with 04
         *
         * Certain products picked at Safeway have specially-printed barcodes which have additional data embedded within the barcode in addition to the item’s UPC.
         * Catch-weight items (items sold by weight) have the weight embedded as a barcode suffix. The format is always consistent (004PPPPP#####C).
         * The two leading zeros followed by a 4 means its Wt embedded. They are followed by the 5 digit PLU, then the 5-digit weight of the product, and finally the check digit.
         * For example, PLU UPC 00404011001038 where 004 indicates it is a weight embedded barcode (from C/701). 04011 is the PLU and 001.03 is the weight of the item. 8 is the check digit.
         */
        @Parcelize
        data class Weighted(
            /** 5 digit plu */
            val plu: String,
            /** 13 digit barcode to match against the backend item upc list. */
            override val catalogLookupUpc: String,
            private val rawWeight: String,
            /** 13 digit weighted barcode */
            override val rawBarcode: String,
        ) : Item() {

            /** Weight that is embedded in the [rawBarcode]. Unsure on unit of measurement */
            val weight: Double = (rawWeight.toIntOrNull() ?: 0) / 100.0

            // Overriding toString to add formatted weight; need to keep properties and property names updated manually now though :(
            override fun toString(): String {
                return "Weighted(plu='$plu', catalogLookupUpc='$catalogLookupUpc', weight='$weight', rawWeight='$rawWeight', rawBarcode='$rawBarcode')"
            }
        }

        /**
         * Starts with 02
         *
         * Certain items in the meat and produce sections at Safeway have the price for each unit embedded as a barcode suffix. The format is always consistent (002PPPPP#####C).
         * The two leading zeros followed by a 2 is Price embedded. They are followed by the 5 digit PLU, then the 5-digit price of the unit, and finally the check digit.
         * For example, 00206011009997 where 002 indicates price embedded, 06011 is the PLU, 00999 is the price ($9.99), and 7 is the check digit.
         */
        @Parcelize
        data class Priced(
            /** 5 digit plu */
            val plu: String,
            /** 13 digit barcode to match against the backend item upc list. */
            override val catalogLookupUpc: String,
            private val rawPrice: String,
            /** 13 digit priced barcode */
            override val rawBarcode: String,
        ) : Item() {

            /** Price that is embedded in the [rawBarcode]. Assume it is in dollars (ex: 9.63 would be 9 dollars and 63 cents) */
            val price: Double = (rawPrice.toIntOrNull() ?: 0) / 100.0
            private val formattedPrice: String by lazy {
                try {
                    NumberFormat.getCurrencyInstance(Locale.US).format(price)
                } catch (e: Exception) {
                    Timber.w(e, "error formatting price")
                    price.toString()
                }
            }

            // Overriding toString to add formatted price; need to keep properties and property names updated manually now though :(
            override fun toString(): String {
                return "Priced(plu='$plu', catalogLookupUpc='$catalogLookupUpc', price='$price' ($formattedPrice), rawPrice='$rawPrice', rawBarcode='$rawBarcode')"
            }
        }

        @Parcelize
        data class Short(
            /** 12 digit upc a converted from the [rawBarcode] with leading (number system) and trailing (check) digits. See http://www.barcodeisland.com/upca.phtml */
            val upcA: String,
            /** 13 digit barcode to match against the backend item upc list. */
            override val catalogLookupUpc: String,
            /** 8 digit upc e with leading (number system) and trailing (check) digits. See http://www.barcodeisland.com/upce.phtml */
            override val rawBarcode: String,
        ) : Item()

        @Parcelize
        data class Normal(
            /** 13 digit barcode to match against the backend item upc list. */
            override val catalogLookupUpc: String,
            /** 12 digit upc a with leading (number system) and trailing (check) digits */
            override val rawBarcode: String,
        ) : Item()

        /**
         * Each class is used to represent manually entered PLU code for Each type items.
         * CatalogLookup and rawBarCode will both hold the entered PLU code.
         */
        @Parcelize
        data class Each(
            /** 5 digit plu */
            val plu: String,
            /** 13 digit barcode to match against the backend item upc list. Prefixed with 0s + PLU to make a 13 digit barcode. */
            override val catalogLookupUpc: String,
            /** 5 digit plu (override required) */
            override val rawBarcode: String,
            /** 12 digit generated barcode to send to the backend: Prefixed with 0s + PLU + generated check digit (over 0s + PLU) */
            val generatedBarcode: String,
            /** Maps to [com.albertsons.acupick.data.model.response.ItemActivityDto.id]. NOT a bpn id. Null for unknown substitute items. */
            val itemActivityDbId: Long?,
        ) : Item()

        /** Use upcA value for Short, generatedBarcode for Each, and rawBarcode for all other types */
        fun getBarcodeToSendToBackend(): String {
            return when (this) {
                is Short -> upcA
                is Each -> generatedBarcode
                is Weighted,
                is Priced,
                is Normal,
                -> rawBarcode
            }
        }
    }

    /** Barcode type that represents a tote. */
    @Parcelize
    data class Tote(override val rawBarcode: String) : BarcodeType(), PickingContainer

    /** Barcode type that represents a bag label */
    @Parcelize
    data class Bag(
        override val rawBarcode: String,
        /** 6-digit number that comprises of a 4-digit pick list number, and a 2-digit bag number.  Returned in the containerId field in ContainerActivityDto. */
        override val bagOrToteId: String,
        /** 8- or 9-digit order number that should match the customerOrderNumber field in ActivityDto */
        override val customerOrderNumber: String,
        /** 5-digit tote identifier (e.g., "TTA00") */
        override val displayToteId: String,
    ) : BarcodeType(), StagingContainer

    @Parcelize
    data class Box(
        override val rawBarcode: String,
        /** Box type returned in boxDetails of BoxInfoDto (XS, SS, MM, LL, XL) */
        override val bagOrToteId: String,
        /** Order number returned in boxDetails of BoxInfoDto */
        override val customerOrderNumber: String,
        /** Box number returned in boxDetails of BoxInfoDto */
        override val displayToteId: String,
    ) : BarcodeType(), StagingContainer

    @Parcelize
    data class PharmacyBag(
        override val rawBarcode: String, // 8000000018-RX1
        override val bagOrToteId: String,
        override val customerOrderNumber: String,
        override val displayToteId: String,
    ) : BarcodeType(), StagingContainer

    /** Barcode type that represents a MFC picking totes 'license plate'
     * This is the shorter version of the MFC tote barcode. It is 14 digits and starts with 998
     * or 999. */
    @Parcelize
    data class MfcPickingToteLicensePlate(
        override val rawBarcode: String,
        /**Barcodes that start with 998 are for StorageTypes Ambient and Hot.
         * Barcodes that start with 999 are for StorageTypes Chilled and Frozen.
         * Group them together on barcode scanned IOT easily check storageType by tote type.
         * **/
        val mfcStorageTypes: List<StorageType?>? = null,
    ) : BarcodeType(), PickingContainer

    /** Barcode type that represents an MFC tote.
     * It is the longer version and is the same as the MFC tote license plate
     * The MFCTote barcode is the longer version and includes the tote license plate a dash
     * and the order number so */
    @Parcelize
    data class MfcTote(
        /** The full barcode which is the 14 digit tote license plate - and the order number **/
        override val rawBarcode: String,
        /** Raw 14 digit number that should equal the containerId returned by the api.
         *  It will match the 14 digit MFC Tote license plate that was used in picking*/
        override val bagOrToteId: String,
        /** Shorter 8 digit number parsed from the full containerId that will be displayed in toasts and what folks will enter in manual entry **/
        override val displayToteId: String,
        /** 8- or 9-digit order number that should match the customerOrderNumber field, or 11-digit fulfillment order number that should match reference->entityId in ActivityDto */
        override val customerOrderNumber: String,
    ) : BarcodeType(), StagingContainer

    /** Barcode type that represents an MFC Reshop tote. */
    @Parcelize
    data class MfcReshopTote(
        /** Full 14 digit barcode **/
        override val rawBarcode: String,
        /** 6 digit number that should equal the containerId returned by the api. **/
        override val bagOrToteId: String,
        /** same as bagId to show on successful scan totes **/
        override val displayToteId: String,
        /** 7- or 8-digit order number that should match the customerOrderNumber field */
        override val customerOrderNumber: String,
    ) : BarcodeType(), StagingContainer

    /** Barcode type that represents an non MFC tote.
     * The non MFCTote barcode includes the order number a dash and toteId
     */
    @Parcelize
    data class NonMfcTote(
        /** The full barcode which is the order number - and toteId**/
        override val rawBarcode: String,
        /** Raw 14 digit number that should equal the containerId returned by the api.
         *  It will match the toteId that was used in picking*/
        override val bagOrToteId: String,
        /** Shorter 5 digit toteId (TTA01) parsed from the full containerId that will be displayed in toasts and what folks will enter in manual entry **/
        override val displayToteId: String,
        /** 8- or 9-digit order number that should match the customerOrderNumber field, or 11-digit fulfillment order number that should match reference->entityId in ActivityDto */
        override val customerOrderNumber: String,
    ) : BarcodeType(), StagingContainer

    /** Barcode type that represents a zone location label */
    @Parcelize
    data class Zone(
        override val rawBarcode: String,
        val zoneType: StorageType,
    ) : BarcodeType()

    /** Barcode type that represents a pharmacy location label */
    @Parcelize
    data class PharmacyArrivalLabel(
        override val rawBarcode: String,
    ) : BarcodeType()

    @Parcelize
    data class PharmacyReturnLabel(
        override val rawBarcode: String,
    ) : BarcodeType()

    /** Unexpected/unsupported/unknown barcode value. Can use for error cases as no logic in the app is expected to support barcodes of this type. */
    @Parcelize
    data class Unknown(override val rawBarcode: String) : BarcodeType()
}
