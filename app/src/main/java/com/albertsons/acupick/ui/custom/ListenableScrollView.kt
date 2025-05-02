package com.albertsons.acupick.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import com.airbnb.lottie.LottieAnimationView

class ListenableScrollView(context: Context?, attrs: AttributeSet?) : ScrollView(context, attrs) {
    private var lastScrollUpdate: Long = -1
    internal var onStart: () -> Unit = {}
    internal var onStop: () -> Unit = {}

    private inner class ScrollStateHandler : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastScrollUpdate > 100) {
                lastScrollUpdate = -1
                onScrollEnd()
            } else {
                postDelayed(this, 100)
            }
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (lastScrollUpdate == -1L) {
            onScrollStart()
            postDelayed(ScrollStateHandler(), 100)
        }
        lastScrollUpdate = System.currentTimeMillis()
    }

    private fun onScrollStart() {
        onStart()
    }

    private fun onScrollEnd() {
        onStop()
    }
}

@BindingAdapter("shouldPlay")
fun ListenableScrollView.setOnScrollStopped(shouldPlay: MutableLiveData<Boolean>) {
    this.onStart = {
        shouldPlay.postValue(false)
    }
    this.onStop = {
        shouldPlay.postValue(true)
    }
}

@BindingAdapter("playPauseAnimation")
fun LottieAnimationView.setPlayPause(shouldPlay: MutableLiveData<Boolean>) {
    if (shouldPlay.value == true) {
        this.resumeAnimation()
    } else {
        this.pauseAnimation()
    }
}
