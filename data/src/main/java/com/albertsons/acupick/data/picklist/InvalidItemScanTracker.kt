package com.albertsons.acupick.data.picklist

import com.albertsons.acupick.data.model.barcode.BarcodeType
import timber.log.Timber

/** Used to track invalid item scans and provides [isInvalidItemScanLimitReached] for callers to act when the limit is reached */
interface InvalidItemScanTracker {
    fun trackInvalidItemScan(barcodeType: BarcodeType.Item)
    fun isInvalidItemScanLimitReached(): Boolean
    fun reset()
}

class InvalidItemScanTrackerImplementation() : InvalidItemScanTracker {

    private var invalidScanBarcodeCount = InvalidScanBarcodeCount()

    override fun trackInvalidItemScan(barcodeType: BarcodeType.Item) {
        if (invalidScanBarcodeCount.rawBarcode != barcodeType.rawBarcode) {
            invalidScanBarcodeCount.rawBarcode = barcodeType.rawBarcode
            invalidScanBarcodeCount.scanCount = 1
            Timber.w("[trackInvalidItemScan] first invalid item scan for $invalidScanBarcodeCount")
        } else {
            invalidScanBarcodeCount.scanCount++
            Timber.w("[trackInvalidItemScan] $invalidScanBarcodeCount${if (isInvalidItemScanLimitReached()) " - limit reached" else ""}")
        }
    }

    override fun isInvalidItemScanLimitReached(): Boolean = invalidScanBarcodeCount.scanCount >= MAX_INVALID_SCAN_COUNT

    override fun reset() {
        invalidScanBarcodeCount.clear()
    }

    data class InvalidScanBarcodeCount(var rawBarcode: String = "", var scanCount: Int = 0) {
        fun clear() {
            rawBarcode = ""
            scanCount = 1
        }
    }
}

private const val MAX_INVALID_SCAN_COUNT = 3
