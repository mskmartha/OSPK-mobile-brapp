package com.albertsons.acupick.ui.bottomsheetdialog

import android.content.Context
import android.os.Parcelable
import android.view.View
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import com.albertsons.acupick.R
import androidx.annotation.StringRes
import com.albertsons.acupick.data.model.DomainModel
import com.albertsons.acupick.ui.util.StringIdHelper
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/** All values needed to show the bottom sheet */
@Parcelize
data class CustomBottomSheetArgData(
    val dialogType: BottomSheetType = BottomSheetType.ToteScan,
    @DrawableRes val largeImage: Int? = null,
    @DrawableRes val titleIcon: Int? = null,
    val title: StringIdHelper,
    val textToBeHighlightedInTitle: StringIdHelper? = null,
    val body: StringIdHelper? = null,
    val shouldBoldTitle: Boolean = false,
    val secondaryBody: StringIdHelper? = null,
    val imageUrl: String? = null,
    val positiveButtonText: StringIdHelper? = null,
    val negativeButtonText: StringIdHelper? = null,
    val cancelable: Boolean = true,
    val cancelOnTouchOutside: Boolean = false,
    // val customData: Serializable? = null,
    val customDataParcel: Parcelable? = null,
    val retryAction: Unit? = null,
    val draggable: Boolean = true,
    val exit: Boolean = false,
    @DimenRes val peekHeight: Int = R.dimen.default_bottomsheet_peek_height,
    val isFullScreen: Boolean = false
) : DomainModel, Parcelable

data class BottomSheetArgDataAndTag(val data: CustomBottomSheetArgData, val tag: String) : DomainModel, Serializable

/** Internal view state representation used by databinding on the layout */
data class CustomBottomSheetViewData(
    val dialogType: BottomSheetType = BottomSheetType.ToteScan,
    @DrawableRes val largeImage: Int?,
    @DrawableRes val titleIcon: Int?,
    val titleIconVisibility: Int,
    val shouldBoldTitle: Boolean = false,
    val title: String,
    val textToBeHighlightedInTitle: String?,
    val titleVisibility: Int,
    val body: String?,
    val secondaryBody: String?,
    val secondaryBodyVisibility: Int,
    val positiveButtonText: String?,
    val imageUrl: String?,
    val positiveButtonVisibility: Int,
    val negativeButtonText: String?,
    val negativeButtonVisibility: Int
)

fun CustomBottomSheetArgData.toViewData(context: Context): CustomBottomSheetViewData {
    return CustomBottomSheetViewData(
        largeImage = largeImage,
        titleIcon = titleIcon,
        titleIconVisibility = if (titleIcon == null) View.INVISIBLE else View.VISIBLE,
        title = title.getString(context),
        textToBeHighlightedInTitle = textToBeHighlightedInTitle?.getString(context),
        titleVisibility = if (title.getString(context).isBlank()) View.GONE else View.VISIBLE,
        body = body?.getString(context),
        secondaryBody = secondaryBody?.getString(context),
        secondaryBodyVisibility = if (secondaryBody == null) View.GONE else View.VISIBLE,
        imageUrl = imageUrl,
        positiveButtonText = positiveButtonText?.getString(context),
        negativeButtonText = negativeButtonText?.getString(context),
        positiveButtonVisibility = if (positiveButtonText == null) View.GONE else View.VISIBLE,
        negativeButtonVisibility = if (negativeButtonText == null) View.GONE else View.VISIBLE,
        shouldBoldTitle = shouldBoldTitle,
        dialogType = dialogType
    )
}

/** Action user took to close the dialog */
@Parcelize
enum class CloseAction : Parcelable {
    /** Positive action was taken by the user */
    Positive,

    /** Negative action was taken by the user */
    Negative,

    /** User dismissed dialog (system back press or tap outside bounds) */
    Dismiss
}

@Parcelize
data class ActionSheetOptions(
    @DrawableRes val settingsIcon: Int,
    @StringRes val settingsString: Int
) : Parcelable

@Parcelize
data class ActionSheetDetails(
    val options: List<ActionSheetOptions>
) : Parcelable
