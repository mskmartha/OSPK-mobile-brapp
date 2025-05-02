package com.albertsons.acupick.ui.arrivals.destage

data class OrderCompletionState(
    val customerOrderNumber: String,
    val isComplete: Boolean = false,
)
