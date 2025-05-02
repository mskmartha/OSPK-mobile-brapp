package com.albertsons.acupick.ui.arrivals.destage.reportmissingbag

import android.os.Bundle
import android.widget.RadioGroup
import androidx.activity.addCallback
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ReportMissingBagFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.notification.NotificationViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReportMissingBagFragment : BaseFragment<ReportMissingBagViewModel, ReportMissingBagFragmentBinding>() {
    private val notificationViewModel: NotificationViewModel by sharedViewModel()
    private val args: ReportMissingBagFragmentArgs by navArgs()
    override val fragmentViewModel: ReportMissingBagViewModel by viewModel()
    override fun getLayoutRes() = R.layout.report_missing_bag_fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            setResultAndExit(null)
        }
    }
    override fun setupBinding(binding: ReportMissingBagFragmentBinding) {
        super.setupBinding(binding)
        binding.viewModel = fragmentViewModel.apply {
            reportMissingBagsParams.value = args.reportMissingBagsParams
            confirmSelection.observe(viewLifecycleOwner) {
                val selectionIndex = binding.radioGroup.indexOfChild(binding.root.findViewById<RadioGroup>(binding.radioGroup.checkedRadioButtonId))
                fragmentViewModel.updateSelection(selectionIndex)
            }
            selectionId.observe(viewLifecycleOwner) {
                setResultAndExit(it)
            }
        }
        notificationViewModel.notificationMessageSnackEvent.observe(viewLifecycleOwner) { snackBarEvent ->
            snackBarEvent?.let { it -> fragmentViewModel.showSnackBar(it) }
        }
        with(activityViewModel) {
            setToolbarTitle(fragmentViewModel.toolbarTitle(args.reportMissingBagsParams))
            setToolbarNavigationIcon(context?.getDrawable(R.drawable.ic_back_arrow))
            navigationButtonIntercept.observe(viewLifecycleOwner) {
                setResultAndExit(null)
            }
        }
    }
    private fun setResultAndExit(selectionId: String?) {
        val result = Bundle().apply {
            putBoolean(REPORT_ISSUE_CANCELLED_KEY, selectionId.isNullOrEmpty())
            putString(REPORT_ISSUE_SELECTION_KEY, selectionId)
            putInt(REPORT_ISSUE_PREVIOUS_PAGE, fragmentViewModel.reportMissingBagsParams.value?.currentPage ?: 0)
        }
        setFragmentResult(REPORT_ISSUE_REQUEST, result)
        fragmentViewModel.navigateUp()
    }
    companion object {
        const val REPORT_ISSUE_REQUEST = "700"
        const val REPORT_ISSUE_CANCELLED_KEY = "800"
        const val REPORT_ISSUE_SELECTION_KEY = "900"
        const val REPORT_ISSUE_PREVIOUS_PAGE = "1010"
    }
}
