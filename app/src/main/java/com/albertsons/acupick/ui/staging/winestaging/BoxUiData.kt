package com.albertsons.acupick.ui.staging.winestaging

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.BoxData
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class BoxUiData(
    val boxDataList: List<BoxData>
) : Parcelable
