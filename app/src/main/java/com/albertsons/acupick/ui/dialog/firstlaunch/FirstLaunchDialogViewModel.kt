package com.albertsons.acupick.ui.dialog.firstlaunch

import android.app.Application
import androidx.annotation.Keep
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.dialog.CustomDialogViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@Keep
data class OnboardingPage(
    val title: String,
    val imageRes: Int,
    val description: String,
    val description1: String,
)

class FirstLaunchDialogViewModel(app: Application) : CustomDialogViewModel(app) {


    val pages = listOf(
        OnboardingPage(
            "Welcome to\nHandoff Heroes!",
            R.drawable.ic_my_score_hand_off,
            "Handoff Heroes is a points-based challenge that will run every 2 weeks. The better you run every 2 weeks. The better your OTH, the more points you'll earn!",
            ""
        ),
        OnboardingPage(
            "How it works",
            R.drawable.ic_my_score_hand_off,
            "Top 3 win prizes including a <b>$50 gift card!</b>",
            "How to win"
        )
    )

    val currentPage = MutableLiveData(0)

    val buttonText = MediatorLiveData<String>().apply {
        addSource(currentPage) { page ->
            value = if (page == pages.lastIndex) "Got it!" else "Next"
        }
    }

    fun onGotItClicked() {
        viewModelScope.launch {
            Timber.e("onGotItClicked [invoked]")
            onCloseIconClick()
        }
    }
}