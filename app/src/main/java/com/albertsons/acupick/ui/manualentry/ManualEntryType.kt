package com.albertsons.acupick.ui.manualentry

import androidx.annotation.Keep

@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
enum class ManualEntryType {
    UPC,
    Weight,
    PLU,
    Zone,
    Bag,
    CustomerOrderNumber,
    Barcode
}
