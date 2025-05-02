package com.albertsons.acupick.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class RejectedItemsByStorageType(
    val customerOrderNumber: String? = null,
    val storageType: StorageType? = null,
    val rejectedItems: List<RejectedItem>? = null,
) : Parcelable
