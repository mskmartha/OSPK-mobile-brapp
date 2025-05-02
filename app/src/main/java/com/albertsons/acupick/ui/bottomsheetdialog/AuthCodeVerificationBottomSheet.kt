package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.BaseComposeViewBottomSheetBinding
import com.albertsons.acupick.infrastructure.utils.collectFlow
import com.albertsons.acupick.ui.arrivals.complete.AuthCodeVerificationScreen
import com.albertsons.acupick.ui.arrivals.complete.HandOffAuthInfo
import com.albertsons.acupick.ui.arrivals.complete.HandOffViewModel.Companion.AUTH_CODE_UNAVAILABLE
import com.albertsons.acupick.ui.arrivals.complete.HandOffViewModel.Companion.AUTH_CODE_VALIDATION_RESULTS
import com.albertsons.acupick.ui.arrivals.complete.HandOffViewModel.Companion.HANDOFF_AUTH_CODE_REQUEST_KEY
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Bottom sheet for scanning a tote */
class AuthCodeVerificationBottomSheet : BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<AuthCodeVerificationViewModel>()

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<BaseComposeViewBottomSheetBinding>(inflater, R.layout.base_compose_view_bottom_sheet, container, false).apply {
            val param = argData.customDataParcel as HandOffAuthInfo
            viewModel.setParam(param)
            baseComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val verificationCode by viewModel.verificationCodeText.collectAsStateWithLifecycle()
                    val isErrorShown by viewModel.isErrorShown.collectAsStateWithLifecycle()
                    val focusRequesters = remember { List(4) { FocusRequester() } }
                    val customerName = param.customerName?.let {
                        it.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                    } ?: ""

                    AuthCodeVerificationScreen(
                        verificationCode = verificationCode,
                        isErrorShown = isErrorShown,
                        focusRequesters = focusRequesters,
                        onConfirmClicked = {
                            viewModel.onClickConfirmAuthCode()
                        },
                        onCodeUnavailableClicked = {
                            viewModel.onClickCodeUnavailable()
                        },
                        customerName = customerName,
                        onAction = { action ->
                            when (action) {
                                is OtpAction.OnEnterNumber -> {
                                    viewModel.onEnterNumber(action.number, action.index)
                                }

                                OtpAction.OnKeyboardBack -> {
                                    viewModel.onKeyboardBack()
                                }
                            }
                        }
                    )

                    viewModel.apply {
                        collectFlow(focusedIndex) {
                            it?.let { index ->
                                focusRequesters.getOrNull(index)?.requestFocus()
                            }
                        }
                    }
                }
            }
            viewModel.apply {

                viewLifecycleOwner.lifecycleScope.launch {
                    // Send the code unavailable event to the parent fragment
                    collectFlow(codeUnavailableEvent) {
                        setFragmentResult(
                            HANDOFF_AUTH_CODE_REQUEST_KEY,
                            bundleOf(AUTH_CODE_UNAVAILABLE to true)
                        )
                    }
                    // Send the authCode entered by user to the parent fragment
                    collectFlow(authCode) {
                        setFragmentResult(
                            HANDOFF_AUTH_CODE_REQUEST_KEY,
                            bundleOf(AUTH_CODE_VALIDATION_RESULTS to it)
                        )
                    }
                }

                // collectFlow(focusedIndex) {
                //     it?.let { index ->
                //         focusRequesters.getOrNull(index)?.requestFocus()
                //     }
                // }

                sharedViewModel.bottomSheetRecordPickArgData.observe(viewLifecycleOwner) {
                    if (it.exit && it.dialogType == BottomSheetType.AuthCodeVerification) {
                        dismiss()
                    } else {
                        viewModel.apply {
                            isErrorShown.value = false
                            verificationCodeText.value = ""
                            resetFocussedIndex()
                        }
                    }
                }
            }
        }
    }
}
