package com.albertsons.acupick.ui.util

import android.content.Context
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.StorageType

fun StorageType.displayName(context: Context) = when (this) {
    StorageType.AM -> context.getString(R.string.storage_type_ambient)
    StorageType.CH -> context.getString(R.string.storage_type_chilled)
    StorageType.FZ -> context.getString(R.string.storage_type_frozen)
    StorageType.HT -> context.getString(R.string.storage_type_hot)
}
