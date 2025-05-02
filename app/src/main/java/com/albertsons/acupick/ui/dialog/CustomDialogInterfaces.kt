package com.albertsons.acupick.ui.dialog

import com.albertsons.acupick.infrastructure.utils.exhaustive

/**
 * Provider interface so that fragment/activity does not have to implement the [CloseActionListener] directly AND to allow multiple dialog listeners to exist per screen.
 */
interface CloseActionListenerProvider {
    /**
     * Return the appropriate [CloseActionListener] for the given fragment/activity [tag]
     */
    fun provide(tag: String?): CloseActionListener?
}

/** Provides type of action that closed the dialog. Extend in your fragment/activity showing the dialog to respond to dialog actions by user. */
interface CloseActionListener {
    fun onCloseAction(closeAction: CloseAction, result: Int?)
    fun onCloseActionWithData(closeAction: CloseAction, result: Pair<String, String>?) = Unit
}

fun closeActionFactory(
    positive: ((Int?) -> Unit)? = null,
    positiveWithData: ((Pair<String, String>?) -> Unit)? = null,
    negative: (() -> Unit)? = null,
    dismiss: (() -> Unit)? = null,
) = object : CloseActionListener {
    override fun onCloseAction(closeAction: CloseAction, result: Int?) {
        when (closeAction) {
            CloseAction.Positive -> positive?.invoke(result)
            CloseAction.Negative -> negative?.invoke()
            CloseAction.Dismiss -> dismiss?.invoke()
        }.exhaustive
    }
    override fun onCloseActionWithData(closeAction: CloseAction, result: Pair<String, String>?) {
        when (closeAction) {
            CloseAction.Positive -> positiveWithData?.invoke(result)
            CloseAction.Negative -> negative?.invoke()
            CloseAction.Dismiss -> dismiss?.invoke()
        }.exhaustive
    }
}
