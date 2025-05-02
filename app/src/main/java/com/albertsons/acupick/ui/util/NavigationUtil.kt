package com.albertsons.acupick.ui.util

import com.albertsons.acupick.R

fun Int?.isHandOffFlow() =
    when (this) {
        R.id.destageOrderFragment,
        R.id.handOffFragment,
        R.id.manualEntryHandOffFragment,
        R.id.manualEntryHandOffMfcFragment,
        R.id.updateCustomerAddFragment,
        R.id.updateCustomerChangeStatusFragment,
        R.id.handOffInterstitialFragment -> true
        else -> false
    }

fun Int?.isPickingFlow() =
    when (this) {
        R.id.pickListItemsFragment,
        R.id.itemDetailsFragment,
        R.id.substituteFragment,
        R.id.manualEntryPagerFragment,
        R.id.totesFragment -> true
        else -> false
    }

fun Int?.isStagingFlow() =
    when (this) {
        R.id.stagingFragment,
        R.id.stagingPart2Fragment,
        R.id.addBagsFragment,
        R.id.manualEntryStagingFragment,
        R.id.printLabelFragment -> true
        else -> false
    }

fun Int?.isCompleteHandOffFlow() =
    when (this) {
        R.id.handOffFragment,
        R.id.handOffInterstitialFragment,
        R.id.customerSignatureFragment,
        R.id.handOffRxInterstitialFragment,
        R.id.orderSummaryFragment,
        R.id.verificationCodeToolTipFragment,
        R.id.verificationIdTypeFragment,
        R.id.bagsPerTempZoneFragment,
        R.id.verificationManualEntryFragment, -> true
        else -> false
    }

fun Int?.isDestagingFlow() =
    when (this) {
        R.id.destageOrderFragment,
        R.id.manualEntryHandOffFragment,
        R.id.updateCustomerAddFragment,
        R.id.updateCustomerChangeStatusFragment,
        R.id.manualEntryHandOffMfcFragment,
        R.id.removeRejectedItemsFragment,
        R.id.reportMissingBagFragment, -> true
        else -> false
    }

fun Int?.shouldNotShowchatNotification() =
    when (this) {
        R.id.destageOrderFragment,
        R.id.manualEntryHandOffFragment,
        R.id.updateCustomerAddFragment,
        R.id.updateCustomerChangeStatusFragment,
        R.id.manualEntryHandOffMfcFragment,
        R.id.removeRejectedItemsFragment,
        R.id.reportMissingBagFragment,
        R.id.handOffFragment,
        R.id.handOffInterstitialFragment,
        R.id.customerSignatureFragment,
        R.id.handOffRxInterstitialFragment,
        R.id.orderSummaryFragment,
        R.id.verificationCodeToolTipFragment,
        R.id.verificationIdTypeFragment,
        R.id.bagsPerTempZoneFragment,
        R.id.verificationManualEntryFragment,
        R.id.splashFragment,
        R.id.loginFragment,
        R.id.chatFragment -> true
        else -> false
    }

fun Int?.shouldNotShowchatPickerJoinedNotification() =
    when (this) {
        R.id.destageOrderFragment,
        R.id.manualEntryHandOffFragment,
        R.id.updateCustomerAddFragment,
        R.id.updateCustomerChangeStatusFragment,
        R.id.manualEntryHandOffMfcFragment,
        R.id.removeRejectedItemsFragment,
        R.id.reportMissingBagFragment,
        R.id.handOffFragment,
        R.id.handOffInterstitialFragment,
        R.id.customerSignatureFragment,
        R.id.handOffRxInterstitialFragment,
        R.id.orderSummaryFragment,
        R.id.verificationCodeToolTipFragment,
        R.id.verificationIdTypeFragment,
        R.id.bagsPerTempZoneFragment,
        R.id.verificationManualEntryFragment,
        R.id.splashFragment,
        R.id.loginFragment -> true
        else -> false
    }

fun Int?.isPickingFlowExcludeSubstitution() =
    when (this) {
        R.id.pickListItemsFragment,
        R.id.itemDetailsFragment,
        R.id.manualEntryPagerFragment,
        R.id.totesFragment -> true
        else -> false
    }

fun Int?.isChatScreen() =
    when (this) {
        R.id.chatFragment -> true
        else -> false
    }
