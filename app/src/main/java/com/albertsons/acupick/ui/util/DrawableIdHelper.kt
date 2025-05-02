package com.albertsons.acupick.ui.util

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.albertsons.acupick.data.model.DomainModel
import java.io.Serializable

sealed class DrawableIdHelper : DomainModel, Serializable {
    data class Id(@DrawableRes val idRes: Int) : DrawableIdHelper() {
        override fun get(context: Context) =
            try {
                ContextCompat.getDrawable(context, idRes)
            } catch (e: Exception) {
                null
            }
    }

    data class Raw(val rawDrawable: Drawable) : DrawableIdHelper() {
        override fun get(context: Context) = rawDrawable
    }

    abstract fun get(context: Context): Drawable?
}
