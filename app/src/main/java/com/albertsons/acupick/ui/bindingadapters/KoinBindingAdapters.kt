package com.albertsons.acupick.ui.bindingadapters

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.custom.ZoomableImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso

/**
 * Wrapper class to provide [BindingAdapter] functions that need to rely on data injected via Koin. Pass the values as constructor args and use in functions below
 */
class KoinBindingAdapters(private val picasso: Picasso) {
    @BindingAdapter("loadImage")
    fun ImageView.loadImage(image: String?) = if (!image.isNullOrBlank()) {
        picasso.load(image).placeholder(R.drawable.ic_card_view_empty_image).error(R.drawable.ic_card_view_empty_image).into(this)
    } else {
        setImageResource(R.drawable.ic_card_view_empty_image)
    }

    @BindingAdapter(value = ["app:loadImage", "app:placeholderImage"])
    fun ImageView.loadImageOrPlaceholder(image: String?, placeHolder: Drawable) = if (!image.isNullOrBlank()) {
        picasso.load(image).placeholder(placeHolder).error(placeHolder).into(this)
    } else {
        setImageDrawable(placeHolder)
    }

    @BindingAdapter(value = ["app:imageUrl", "app:picassoCallback"])
    fun ImageView.loadImageNoCacheWithNoFallback(imageUrl: String?, picassoCallback: Callback) {
        if (!imageUrl.isNullOrBlank()) {
            picasso.load(imageUrl)
                .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .into(this, picassoCallback)
        } else {
            setImageResource(android.R.color.transparent)
        }
    }

    @BindingAdapter(value = ["app:mediaImageUrl"])
    fun ImageView.loadChatImage(imageUrl: String?) {
        imageUrl?.let {
            if (it.contains("https")) {
                picasso.load(it).into(this)
            } else if (imageUrl.isNotEmpty()) {
                picasso.load(Uri.parse(it)).into(this)
            }
        }
    }
    @BindingAdapter("app:localImageRes")
    fun ImageView.loadLocalImageResource(@DrawableRes imageResId: Int) {
        if (imageResId != 0) { // Check if a valid resource ID is provided
            picasso.load(imageResId).noFade().placeholder(imageResId).into(this)
        } else {
            // Optionally, clear the image or set a default if 0 is passed
            this.setImageDrawable(null)
        }
    }

    @BindingAdapter("loadImageUrl")
    fun ZoomableImageView.loadImageUrl(loadImageUrl: String?) = if (!loadImageUrl.isNullOrBlank()) {
        picasso.load(loadImageUrl).placeholder(R.drawable.ic_card_view_empty_image).error(R.drawable.ic_card_view_empty_image).into(this)
    } else {
        setImageResource(R.drawable.ic_card_view_empty_image)
    }
}
