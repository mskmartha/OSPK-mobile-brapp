package com.albertsons.acupick.data.duginterjection

sealed class DugInterjectionState {
    // This state indicates that the app has already showing a DUG interjection.
    data class Appear(val orderNumber: String? = null) : DugInterjectionState()

    // This state indicates Batch failure reason of DUG interjection
    sealed class BatchFailureReason : DugInterjectionState() {
        // This state indicates that the picker is destaging an Rx order
        object DestagingRxOrder : BatchFailureReason()
        // This state indicates that the picker is destaging 3 orders and their handoff order limit has been reached.
        object MaxHandoffAssigned : BatchFailureReason()
    }

    // This state indicates that the picker is working on handoff flow.
    object HandoffInProgress : DugInterjectionState()

    // Default State
    object None : DugInterjectionState()
    companion object {
        const val FE_EXCEPTION = "FE_EXCEPTION"
        const val FE_BATCH_FAILURE = "FE_BATCH_FAILURE"
        const val FE_HANDOFF_FAILURE = "FE_IN_HANDOFF"
    }
}

// TODO: DUG interjection Will refactor it later, try to remove boolean check
/** Failure text to be sent to the BE for DUGInterjectionFailures*/
fun DugInterjectionState.failureReasonTextValue(): String? {
    return when (this) {
        is DugInterjectionState.Appear -> DugInterjectionState.FE_EXCEPTION
        is DugInterjectionState.BatchFailureReason -> DugInterjectionState.FE_BATCH_FAILURE
        is DugInterjectionState.HandoffInProgress -> DugInterjectionState.FE_HANDOFF_FAILURE
        is DugInterjectionState.None -> null
    }
}

fun DugInterjectionState.isMaxHandOffAssigned() = this == DugInterjectionState.BatchFailureReason.MaxHandoffAssigned

fun DugInterjectionState.isDestagingRxOrder() = this == DugInterjectionState.BatchFailureReason.DestagingRxOrder

fun DugInterjectionState.isAppear(): Boolean {
    return when (this) {
        is DugInterjectionState.Appear -> true
        else -> false
    }
}
