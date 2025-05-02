package com.albertsons.acupick.data.model.barcode

import com.albertsons.acupick.data.model.StorageType
import okhttp3.internal.toLongOrDefault
import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

interface BarcodeMapper {
    /**
     * Converts [barcodeIn] into the appropriate [BarcodeType] based on logic per type.
     *
     * **Use [generateEachBarcode] to create [BarcodeType.Item.Each] as this function will NOT infer them (they are not intended to be scanned)**
     */
    fun inferBarcodeType(barcodeIn: String, enableLogging: Boolean = false): BarcodeType

    /** Converts [plu] and [weightString] into the a [BarcodeType.Item.Weighted] */
    fun generateWeightedBarcode(plu: String, weightString: String): BarcodeType.Item.Weighted

    /** Converts [plu] into the a [BarcodeType.Item.Each] */
    fun generateEachBarcode(plu: String, itemActivityDbId: Long?): BarcodeType.Item.Each

    /** Converts 13 digit "catalog upc" [catalogUpc] from ABS backend into a normal 12 digit UPC-A barcode for display purposes */
    fun generateDisplayBarCode(catalogUpc: String? = null): String
}

class BarcodeMapperImplementation : BarcodeMapper {

    /** Converts [barcodeIn] into the appropriate [BarcodeType] based on logic per type */
    override fun inferBarcodeType(barcodeIn: String, enableLogging: Boolean): BarcodeType {
        if (enableLogging) Timber.v("[inferBarcodeType] barcodeIn=$barcodeIn, length=${barcodeIn.length}")
        val barcode = barcodeIn.trim()
        if (enableLogging) Timber.v("[inferBarcodeType] barcode=$barcode, length=${barcodeIn.length} (trimmed)")

        // Check if a tote barcode first as it has characters in it
        if (isValidTote(barcode)) {
            if (enableLogging) Timber.v("[inferBarcodeType] valid tote")
            return BarcodeType.Tote(barcode)
        }

        if (isValidMfcPickingToteLicensePlate(barcode)) {
            if (enableLogging) Timber.v("[inferBarcodeType] valid mfcPickingToteLicensePlate")
            return BarcodeType.MfcPickingToteLicensePlate(
                barcode,
                mfcStorageTypes = when {
                    barcode.startsWith("998", ignoreCase = true) -> listOf(StorageType.AM, StorageType.HT)
                    barcode.startsWith("999", ignoreCase = true) -> listOf(StorageType.CH, StorageType.FZ)
                    else -> null
                }
            )
        }

        if (isValidBag(barcode)) {
            if (enableLogging) Timber.v("[inferBarcodeType] valid bag")
            val customerOrderNumber = barcode.split("-")[0]
            if (enableLogging) Timber.v("[inferBarcodeType] customerOrderNumber=$customerOrderNumber")
            val toteId = barcode.split("-")[1]
            if (enableLogging) Timber.v("[inferBarcodeType] toteId=$toteId")
            val bagId = barcode.split("-")[2]
            if (enableLogging) Timber.v("[inferBarcodeType] bagId=$bagId")
            return BarcodeType.Bag(
                rawBarcode = barcode,
                bagOrToteId = bagId,
                customerOrderNumber = customerOrderNumber,
                displayToteId = toteId,
            )
        }

        /** Customer bag preference: Validate if the scanned barcode is a non-mfc tote's barcode. */
        if (isValidNonMfcTote(barcode)) {
            if (enableLogging) Timber.v("[inferBarcodeType] valid non mfc tote")
            val customerOrderNumber = barcode.split("-")[0]
            if (enableLogging) Timber.v("[inferBarcodeType] customerOrderNumber=$customerOrderNumber")
            val toteId = barcode.split("-")[1]
            if (enableLogging) Timber.v("[inferBarcodeType] non mfc toteId=$toteId")
            return BarcodeType.NonMfcTote(
                rawBarcode = barcode,
                bagOrToteId = toteId,
                customerOrderNumber = customerOrderNumber,
                displayToteId = toteId,
            )
        }
        if (isValidBox(barcode)) {
            if (enableLogging) Timber.v("[inferBarcodeType] valid box")
            val customerOrderNumber = barcode.split("-")[0]
            if (enableLogging) Timber.v("[inferBarcodeType] customerOrderNumber=$customerOrderNumber")
            val boxNumber = barcode.split("-")[4]
            if (enableLogging) Timber.v("[inferBarcodeType] toteId=$boxNumber")
            val boxType = barcode.split("-")[3]
            if (enableLogging) Timber.v("[inferBarcodeType] bagId=$boxType")
            return BarcodeType.Box(
                rawBarcode = barcode,
                bagOrToteId = boxNumber,
                customerOrderNumber = customerOrderNumber,
                displayToteId = boxType,
            )
        }

        if (isValidPharmacyArrivalLabel(barcode)) {
            if (enableLogging) Timber.v("[inferBarcodeType] isValidPharmacyArrivalLabel")
            return BarcodeType.PharmacyArrivalLabel(barcode)
        }

        if (isValidPharmacyReturnLabel(barcode)) {
            if (enableLogging) Timber.v("[inferBarcodeType] isValidPharmacyReturnLabel")
            return BarcodeType.PharmacyReturnLabel(barcode)
        }

        if (isValidZone(barcode)) {
            if (enableLogging) Timber.v("[inferBarcodeType] valid zone")
            return BarcodeType.Zone(
                barcode,
                when {
                    barcode.startsWith("AM", ignoreCase = true) -> StorageType.AM
                    barcode.startsWith("CH", ignoreCase = true) -> StorageType.CH
                    barcode.startsWith("FZ", ignoreCase = true) -> StorageType.FZ
                    else -> StorageType.HT
                }
            )
        }

        if (isValidMfcTote(barcode)) {
            val barcodeBeforeDash = barcode.substring(0, barcode.indexOf("-"))
            val displayMfcToteId = barcodeBeforeDash.substring(barcodeBeforeDash.length - 8)
            val customerOrderNumber = barcode.substring(barcode.indexOf("-") + 1)
            return BarcodeType.MfcTote(
                rawBarcode = barcode,
                bagOrToteId = barcodeBeforeDash,
                displayToteId = displayMfcToteId,
                customerOrderNumber = customerOrderNumber
            )
        }

        if (isValidMfcReshopTote(barcode)) {
            val bagId = barcode.substringAfter("-")
            val customerOrderNumber = barcode.substring(0, barcode.indexOf("-"))
            return BarcodeType.MfcReshopTote(
                rawBarcode = barcode,
                bagOrToteId = bagId,
                displayToteId = bagId,
                customerOrderNumber = customerOrderNumber
            )
        }

        if (isValidPharmacyBag(barcode)) {
            return BarcodeType.PharmacyBag(
                rawBarcode = barcode,
                bagOrToteId = barcode,
                customerOrderNumber = "", // ignore
                displayToteId = "", // ignore
            )
        }

        // If not a valid tote, check that the barcode is all digits
        if (!barcode.matches(ONLY_DIGITS_REGEX)) {
            if (enableLogging) Timber.v("[inferBarcodeType] non-tote barcodes are expected to only contain digits - marking as Unknown")
            return BarcodeType.Unknown(barcode)
        }

        return when (barcode.length) {
            // GS1(26 digit) barcode scanning.
            26 -> {
                val convertedBarcode = convertGS1BarcodeToThirteenDigit(barcode)
                return inferItemBarcodeType(convertedBarcode, enableLogging)
            }
            // 12 digit barcode when scanning with datawedge (leaves out leading digit); 13 digit when manually entering full barcode digits.
            12, 13 -> {
                return inferItemBarcodeType(barcode, enableLogging)
            }
            8 -> {
                if (enableLogging) Timber.v("[inferBarcodeType] short")
                val elevenDigitUpcA = convertUpcEToElevenDigitUpcA(barcode)
                if (enableLogging) Timber.v("[inferBarcodeType] elevenDigitUpcA=$elevenDigitUpcA, length=${elevenDigitUpcA.length}")
                val twelveDigitUpcA = elevenDigitUpcA + generateCheckDigitForElevenDigitUpcA(elevenDigitUpcA)
                if (enableLogging) Timber.v("[inferBarcodeType] twelveDigitUpcA=$twelveDigitUpcA, length=${twelveDigitUpcA.length}")
                val paddedBarcode = elevenDigitUpcA.padStart(BACKEND_CATALOG_UPC_LENGTH, '0')
                if (enableLogging) Timber.v("[inferBarcodeType] paddedBarcode=$paddedBarcode, length=${paddedBarcode.length}")
                BarcodeType.Item.Short(upcA = twelveDigitUpcA, catalogLookupUpc = paddedBarcode, rawBarcode = barcode)
            }
            else -> {
                if (enableLogging) Timber.v("[inferBarcodeType] unknown")
                BarcodeType.Unknown(rawBarcode = barcode)
            }
        }.also { if (enableLogging) Timber.v("[inferBarcodeType] barcodeType=$it") }
    }

    private fun inferItemBarcodeType(barcode: String, enableLogging: Boolean): BarcodeType {
        /** Inferring old manhattan [barcode]s that have no price are unknown  */
        if (barcode.matches(MANHATTAN_REJECT_REGEX)) {
            if (enableLogging) Timber.v("[inferItemBarcodeType] barcode matches an unsupported, legacy manhattan PLU barcode format - returning unknown")
            return BarcodeType.Unknown(rawBarcode = barcode)
        }
        val noCheckDigit = barcode.dropLast(1)
        if (enableLogging) Timber.v("[inferItemBarcodeType] noCheckDigit=$noCheckDigit, length=${noCheckDigit.length}")
        val paddedNoCheckDigit = noCheckDigit.padStart(BACKEND_CATALOG_UPC_LENGTH, '0')
        if (enableLogging) Timber.v("[inferItemBarcodeType] paddedNoCheckDigit=$paddedNoCheckDigit, length=${paddedNoCheckDigit.length}")
        return when {
            paddedNoCheckDigit.startsWith("004") -> {
                if (enableLogging) Timber.v("[inferItemBarcodeType] weighted")
                val plu = paddedNoCheckDigit.substring(3..7)
                if (enableLogging) Timber.v("[inferItemBarcodeType] plu=$plu, length=${plu.length}")
                val weight = paddedNoCheckDigit.substring(8..12)
                // Crude way to remove all leading zeros and make sure at least 1 zero exists before the period
                val weightIntegerPart = weight.substring(0..2).replace("0", "").ifEmpty { "0" }
                val weightFractionalPart = weight.substring(3..4)
                val formattedWeight = "$weightIntegerPart.$weightFractionalPart"
                if (enableLogging) Timber.v("[inferItemBarcodeType] weight=$weight ($formattedWeight)")
                val zeroLastFiveDigitRange = (paddedNoCheckDigit.lastIndex - 4)..(paddedNoCheckDigit.lastIndex)
                val catalogUpc = paddedNoCheckDigit.replaceRange(zeroLastFiveDigitRange, "00000")
                if (enableLogging) Timber.v("[inferItemBarcodeType] catalogUpc=$catalogUpc, length=${catalogUpc.length}")
                // scanning via datawedge results in 12 digit scan w/ leading zero not added (visible on printed barcode) - add it back here
                val paddedToThirteenDigits = barcode.padStart(BACKEND_CATALOG_UPC_LENGTH, '0')
                BarcodeType.Item.Weighted(plu = plu, rawWeight = weight, catalogLookupUpc = catalogUpc, rawBarcode = paddedToThirteenDigits)
            }
            paddedNoCheckDigit.startsWith("002") or paddedNoCheckDigit.startsWith("022") -> {
                if (enableLogging) Timber.v("[inferItemBarcodeType] priced")
                val plu = paddedNoCheckDigit.substring(3..7)
                if (enableLogging) Timber.v("[inferItemBarcodeType] plu=$plu, length=${plu.length}")
                val price = when {
                    paddedNoCheckDigit.startsWith("002") -> paddedNoCheckDigit.substring(9..12)
                    paddedNoCheckDigit.startsWith("022") -> paddedNoCheckDigit.substring(8..12)
                    else -> paddedNoCheckDigit.substring(8..12)
                }
                val priceInCents = price.toLongOrDefault(-1)
                if (priceInCents == 0L) {
                    if (enableLogging) Timber.v("[inferItemBarcodeType] invalid priced barcode with $0.00 price - returning unknown")
                    BarcodeType.Unknown(rawBarcode = barcode)
                } else {
                    val priceInDollars = priceInCents / 100f
                    val formattedPrice = NumberFormat.getCurrencyInstance(Locale.US).format(priceInDollars)
                    if (enableLogging) Timber.v("[inferItemBarcodeType] price=$price ($formattedPrice)")
                    val zeroLastFiveDigitRange = (paddedNoCheckDigit.lastIndex - 4)..(paddedNoCheckDigit.lastIndex)
                    val catalogUpc = paddedNoCheckDigit.replaceRange(zeroLastFiveDigitRange, "00000")
                    val upc = if (catalogUpc.startsWith("022")) {
                        catalogUpc.replaceRange(0..2, "002")
                    } else {
                        catalogUpc
                    }
                    if (enableLogging) Timber.v("[inferItemBarcodeType] catalogUpc=$catalogUpc, length=${catalogUpc.length}")
                    // scanning via datawedge results in 12 digit scan w/ leading zero not added (visible on printed barcode) - add it back here
                    val paddedToThirteenDigits = barcode.padStart(BACKEND_CATALOG_UPC_LENGTH, '0')
                    BarcodeType.Item.Priced(plu = plu, rawPrice = price, catalogLookupUpc = upc, rawBarcode = paddedToThirteenDigits)
                }
            }
            /**
             * Inferring the original 13 length [barcode] that starts with 9 or 8 zeros is Unknown, matching a legacy manhattan PLU barcode format.
             * Intentionally operating on [barcode] instead of [paddedNoCheckDigit] to prevent matching collisions with each generation below caused by the operations made to
             * normalize the [paddedNoCheckDigit] (remove check digit and zero pad the start to make the barcode [BACKEND_CATALOG_UPC_LENGTH] digits long) version
             * resulting in the same [paddedNoCheckDigit] values in certain circumstances. Since these legacy barcodes don't have the a check digit they can be matched based on [barcode].
             */
            barcode.length == 13 && barcode.startsWith("000000000") || barcode.startsWith("00000000") -> {
                if (enableLogging) Timber.v("[inferItemBarcodeType] barcode matches an unsupported, legacy manhattan PLU barcode format - returning unknown")
                BarcodeType.Unknown(rawBarcode = barcode)
            }
            /** Inferring the original Each from the [BarcodeType.Item.Each.generatedBarcode] (generated by [generateEachBarcode]) likely coming from backend data and not the picker directly */
            paddedNoCheckDigit.startsWith("000000") -> {
                if (enableLogging) Timber.v("[inferItemBarcodeType] each")
                val plu = paddedNoCheckDigit.substring(8..12).trimStart('0').padStart(4, '0')
                if (enableLogging) Timber.v("[inferItemBarcodeType] plu=$plu, length=${plu.length}")
                BarcodeType.Item.Each(plu = plu, catalogLookupUpc = paddedNoCheckDigit, rawBarcode = plu, generatedBarcode = barcode, itemActivityDbId = null)
            }
            else -> {
                if (enableLogging) Timber.v("[inferItemBarcodeType] normal barcode")
                BarcodeType.Item.Normal(catalogLookupUpc = paddedNoCheckDigit, rawBarcode = barcode)
            }
        }
    }

    /** True if the barcode matches the scheme for a tote. */
    private fun isValidTote(barcode: String): Boolean = TOTE_REGEX.matches(barcode) &&
        !ZONE_REGEX.matches(barcode) &&
        !PHARMACY_RETURN_REGEX.matches(barcode) &&
        !PHARMACY_ARRIVAL_REGEX.matches(barcode)

    /** True if the barcode matches the scheme for a tote. */
    private fun isValidMfcPickingToteLicensePlate(barcode: String): Boolean = MFCPICKINGTOTELICENSEPLATE_REGEX.matches(barcode)

    /** True if the barcode matches the scheme for a bag. */
    private fun isValidBag(barcode: String): Boolean = BAG_REGEX.matches(barcode)

    /** True if the barcode matches the scheme for a non-mfc tote. */
    private fun isValidNonMfcTote(barcode: String): Boolean = NONMFC_TOTE_REGEX.matches(barcode)

    private fun isValidBox(barcode: String): Boolean = BOX_REGEX.matches(barcode)

    /** True if the barcode matches the scheme for a zone location. */

    private fun isValidPharmacyBag(barcode: String): Boolean = PHARMACY_BAG_REGEX.matches(barcode)

    private fun isValidZone(barcode: String): Boolean = ZONE_REGEX.matches(barcode)

    private fun isValidPharmacyArrivalLabel(barcode: String): Boolean = PHARMACY_ARRIVAL_REGEX.matches(barcode)

    private fun isValidPharmacyReturnLabel(barcode: String): Boolean = PHARMACY_RETURN_REGEX.matches(barcode)

    private fun isValidMfcTote(barcode: String): Boolean = MFCTOTE_REGEX.matches(barcode)

    private fun isValidMfcReshopTote(barcode: String): Boolean =
        MFCRESHOPTOTE_REGEX.matches(barcode)

    /** Converts from 8 digit upc e to 11 digit upc a (no trailing check digit) */
    private fun convertUpcEToElevenDigitUpcA(scannedUpc: String): String {
        val shortUpc = when (scannedUpc.length) {
            7 -> scannedUpc.substring(0, scannedUpc.length - 1)
            8 -> scannedUpc.substring(1, scannedUpc.length - 1)
            else -> scannedUpc
        }
        val upcLastValue = shortUpc.lastOrNull()?.toString().orEmpty()
        val upcFirstVal = "0"

        return when (upcLastValue.toIntOrNull()) {
            0, 1, 2 -> upcFirstVal + shortUpc.substring(0, 2) + upcLastValue + "0000" + shortUpc.substring(2, 5)
            3 -> upcFirstVal + shortUpc.substring(0, 3) + "00000" + shortUpc.substring(3, 5)
            4 -> upcFirstVal + shortUpc.substring(0, 4) + "00000" + shortUpc.substring(4, 5)
            5, 6, 7, 8, 9 -> upcFirstVal + shortUpc.substring(0, 5) + "0000" + shortUpc.substring(5, 6)
            else -> shortUpc
        }
    }

    /**
     * Given an 11 digit upc a, generates the trailing check digit (can be appended to 11 digit string to make the 12 digit upc a).
     *
     * Compare result with/manually generate check digit from https://www.morovia.com/education/utility/upc-ean.asp
     */
    private fun generateCheckDigitForElevenDigitUpcA(upc: String): String {
        val result = upc.map { Integer.parseInt(it.toString()) }
        var evenVal = 0
        var oddVal = 0
        for (i in result.indices) {
            if (i % 2 == 0) oddVal += result[i] else evenVal += result[i]
        }
        val checkDigit = (evenVal + (oddVal * 3)) % 10
        return if (checkDigit != 0) {
            (10 - checkDigit)
        } else {
            checkDigit
        }.toString()
    }

    /** RegEx used to determine if a scanned barcode belongs to a tote */
    private val TOTE_REGEX = Regex("""[A-Za-z]{3}\d{2}""")
    private val BAG_REGEX = Regex("""\d{7,9}-[A-Za-z]{3}\d{2}-\d{6}""")
    private val NONMFC_TOTE_REGEX = Regex("""\d{7,9}-[A-Za-z]{3}\d{2}""")
    private val BOX_REGEX = Regex("""\d{7,9}-BOX-\d{2}-(XS|SS|MM|LL|XL)-\d{6}""")
    private val PHARMACY_BAG_REGEX = Regex("""\d{15}""")
    private val ZONE_REGEX = Regex("""(?i)(am|ch|fz)[a-z]\d{2}|hot\d{2}""")
    private val PHARMACY_ARRIVAL_REGEX = Regex("""((?i)(rx)[a-z])\d{2}""")
    private val PHARMACY_RETURN_REGEX = Regex("""(?i)R6x0Re1t9u6rn""")
    private val MFCTOTE_REGEX = Regex("""\d{14}-\d{1,11}""")
    private val MFCRESHOPTOTE_REGEX = Regex("""\d{7,9}-\d{6}""")
    private val MFCPICKINGTOTELICENSEPLATE_REGEX = Regex("""(999|998)([0-9]{11})""")
    private val ONLY_DIGITS_REGEX = Regex("""[0-9]+""")

    /** Converts [plu] and [weightString] into the a [BarcodeType.Item.Weighted] */
    override fun generateWeightedBarcode(plu: String, weightString: String): BarcodeType.Item.Weighted {
        val weightedItemPrefix = "4"
        val pluDigits = plu.take(5).padStart(5, '0')
        val weight = weightString.toDoubleOrNull()?.coerceIn(0.0..999.99) ?: 0.0
        val weightDigits = DecimalFormat("###.00").format(weight).replace(".", "").padStart(5, '0')
        val elevenDigitUpcA = "$weightedItemPrefix$pluDigits$weightDigits"
        val checkDigit = generateCheckDigitForElevenDigitUpcA(elevenDigitUpcA)
        val finalBarcode = "$elevenDigitUpcA$checkDigit".padStart(BACKEND_CATALOG_UPC_LENGTH, '0')

        return inferBarcodeType(finalBarcode, enableLogging = true) as BarcodeType.Item.Weighted
    }

    /** Takes in [plu] and [itemActivityDbId] to create [BarcodeType.Item.Each] */
    override fun generateEachBarcode(plu: String, itemActivityDbId: Long?): BarcodeType.Item.Each {
        //  4889 (4) -> 00000004889 (11)
        // 94889 (5) -> 00000094889 (11)
        val elevenDigitBarCode = plu.padStart(UPC_A_WITHOUT_CHECK_DIGIT, '0')
        //  4889 (4) -> 0000000004889 (13)
        // 94889 (5) -> 0000000094889 (13)
        val catalogUpc = plu.padStart(BACKEND_CATALOG_UPC_LENGTH, '0')
        val checkDigit = generateCheckDigitForElevenDigitUpcA(elevenDigitBarCode)
        val generatedBarcode = "$elevenDigitBarCode$checkDigit"
        return BarcodeType.Item.Each(plu = plu, catalogLookupUpc = catalogUpc, rawBarcode = plu, generatedBarcode = generatedBarcode, itemActivityDbId = itemActivityDbId)
    }

    override fun generateDisplayBarCode(catalogUpc: String?): String {
        if (catalogUpc.isNullOrBlank()) {
            return ""
        } else if (catalogUpc.length != BACKEND_CATALOG_UPC_LENGTH) {
            Timber.w("[generateDisplayBarCode] catalogUpc length (${catalogUpc.length}) does not match expected length of $BACKEND_CATALOG_UPC_LENGTH digits - passing back catalogUpc unchanged")
            return catalogUpc
        }

        // 0001200004434 (13) -> 01200004434 ( 11)
        return if (catalogUpc.startsWith("00")) {
            val elevenDigitBarCode = catalogUpc.substring(2)
            val checkDigit = generateCheckDigitForElevenDigitUpcA(elevenDigitBarCode)
            "$elevenDigitBarCode$checkDigit"
        } else {
            catalogUpc.substring(1)
        }
    }

    //
    // TODO rewrite below code if needed
    //

    private fun getScanUpc(upcId: String): String {
        val modifiedUpc = getModifiedUpc(upcId)
        return if (isShortUpc(modifiedUpc)) {
            padLeadingZeroesToUpc(upcId + generateCheckDigitForElevenDigitUpcA(upcId))
        } else {
            padLeadingZeroesToUpc(modifiedUpc)
        }
    }

    private fun getModifiedUpc(itemId: String): String {
        return if (isShortUpc(itemId)) {
            convertUpcEToElevenDigitUpcA(itemId)
        } else {
            itemId.substring(0, itemId.length - 1)
        }
    }

    private fun isShortUpc(upcValue: String): Boolean {
        return upcValue.length == 6 || upcValue.length == 7 || upcValue.length == 8
    }

    private fun padLeadingZeroesToUpc(upcValue: String): String {
        return when (upcValue.length) {
            9 -> "0000$upcValue"
            10 -> "000$upcValue"
            11 -> "00$upcValue"
            12 -> "0$upcValue"
            else -> upcValue
        }
    }

    private fun isWeightedOrPackedItem(itemId: String): Boolean {
        return itemId.length > 2 && (itemId.substring(0, 3) == "002" || itemId.substring(0, 3) == "004")
    }

    private fun isWeightedItem(itemId: String): Boolean {
        return itemId.length > 2 && itemId.substring(0, 3) == "004"
    }

    private fun isPackedItem(itemId: String): Boolean {
        return itemId.length > 2 && itemId.substring(0, 3) == "002"
    }

    private fun getItemIdWithZeroedOutLastDigits(itemId: String): String {
        return if (isWeightedOrPackedItem(itemId)) {
            if (itemId.length > 7)
                itemId.substring(0, 8) + "00000"
            else
                itemId
        } else {
            itemId
        }
    }

    private fun getSellByWeightId(itemId: String): String {
        return if (isPackedItem(itemId)) {
            "P"
        } else {
            "W"
        }
    }

    private fun getTolerancePercentage(): Double {
        return 15.00.div(100)
    }

    private fun getActualWeightOfItem(upc: String): Double {
        val len = upc.length
        // PLU code 5 digit + wt is 5 digit + check digit
        return if (len > 10) {
            // considering last 5 digits excluding check digit
            Integer.parseInt(upc.substring(len - 6, len - 1)) * .01
        } else {
            0.00
        }
    }

    private fun getSubstituteUpcForItemMatch(upcId: String): String {
        val modifiedUpc = padLeadingZeroesToUpc(getModifiedUpc(upcId))
        return if (!isShortUpc(upcId)) {
            getItemIdWithZeroedOutLastDigits(modifiedUpc)
        } else {
            modifiedUpc
        }
    }

    private fun getScannedUpcForSubstitution(upcId: String): String {
        return if (isShortUpc(upcId)) {
            convertUpcEToElevenDigitUpcA(upcId) + generateCheckDigitForElevenDigitUpcA(upcId)
        } else {
            upcId
        }
    }

    private fun getCleanedUpUpc(input: String): String {
        val modifiedUpc = input.replace("\u0000".toRegex(), "") // removes NUL chars
            .replace("\\u0000".toRegex(), "") // removes backslash+u0000
            .replace("\n".toRegex(), "") // removes backslash+u0000
            .replace("\r".toRegex(), "") // removes backslash+u0000
        return getFirstValueInBarcode(modifiedUpc)
    }

    private fun getFirstValueInBarcode(barcodeValue: String): String {
        return barcodeValue.split("-")[0]
    }

    private fun getUpcForEachType(plu: String): String {
        val upcWithoutCheckDigit = "0000000$plu"
        return upcWithoutCheckDigit + generateCheckDigitForElevenDigitUpcA(upcWithoutCheckDigit)
    }

    /**
     * @return 13-digit barcode.
     * @param barcode GS1 26-digit barcode.
     */
    private fun convertGS1BarcodeToThirteenDigit(barcode: String): String {
        // 01000000009407403203001995 We are replacing first ten digit with 04, next 5 digits are plu, removing 5 digit checksum from 16th- 20th digit, and the last 6 digits are weight and checksum.
        return barcode.replaceRange(0..9, "04").replaceRange(7..11, "")
    }

    companion object {
        /** Length of the Albertsons catalog upc representation of barcodes */
        private const val BACKEND_CATALOG_UPC_LENGTH = 13
        private const val UPC_A_WITHOUT_CHECK_DIGIT = 11
        // TODO maybe remove MANHATTAN is probably not something we need to worry about anymore
        private val MANHATTAN_REJECT_REGEX = Regex("""00[0-9]\d{5}00000""")
    }
}
