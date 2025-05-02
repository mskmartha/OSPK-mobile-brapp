package com.albertsons.acupick.image

import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import timber.log.Timber

/** Pre-caches images to help ensure they are downloaded/available when offline */
interface ImagePreCacher {
    fun preCacheImages(imageUrls: Collection<String>)
}

class ImagePreCacherImplementation(private val picasso: Picasso) : ImagePreCacher {
    override fun preCacheImages(imageUrls: Collection<String>) {
        imageUrls.forEach { imageUrl ->
            picasso.load(imageUrl).fetch(object : Callback {
                override fun onSuccess() {
                    Timber.v("[preCacheImages onSuccess] imageUrl=$imageUrl")
                }

                override fun onError(e: Exception?) {
                    Timber.w(e, "[preCacheImages onError] imageUrl=$imageUrl")
                }
            })
        }
    }
}
