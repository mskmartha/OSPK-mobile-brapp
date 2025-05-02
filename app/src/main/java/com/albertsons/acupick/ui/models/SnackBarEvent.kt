package com.albertsons.acupick.ui.models

import com.albertsons.acupick.ui.util.StringIdHelper
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.Job

data class SnackBarEvent<T>(
    val prompt: StringIdHelper?,
    val cta: StringIdHelper? = null,
    val payload: T? = null,
    val onDismissEventCallback: (() -> Any?)? = null,
    val action: ((T?) -> Unit)? = null,
    val isError: Boolean = false,
    val isSuccess: Boolean = false,
    val isIndefinite: Boolean = false,
    val startDelayMs: Long = 0L,
    val dismissLiveEvent: LiveEvent<Unit>? = LiveEvent(),
    var callback: BaseTransientBottomBar.BaseCallback<Snackbar>? = null,
    var pendingStartJob: Job? = null,
)
