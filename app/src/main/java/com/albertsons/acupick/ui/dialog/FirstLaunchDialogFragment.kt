package com.albertsons.acupick.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.viewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.FirstLaunchDialogFragmentBinding
import timber.log.Timber

class FirstLaunchDialogFragment : BaseCustomDialogFragment() {

    private lateinit var binding: FirstLaunchDialogFragmentBinding

    private val fragmentVm: FirstLaunchDialogViewModel by viewModels()

    override val shouldFillScreen
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getViewDataBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): ViewDataBinding {
        return DataBindingUtil.inflate<FirstLaunchDialogFragmentBinding>(
            inflater,
            R.layout.first_launch_dialog_fragment,
            container,
            false
        ).apply {
            lifecycleOwner = viewLifecycleOwner
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentVm
            binding = this
            setUpViews()
            fragmentVm.navigation.observe(viewLifecycleOwner) { closeAction ->
                Timber.v("[setupBinding closeActionEvent] closeAction=$closeAction")
                dismiss()
                // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
                findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
            }
        }
    }

    private fun setUpViews() {
        val adapter = OnboardingPagerAdapter(fragmentVm.pages)
        binding.viewPager.adapter = adapter

        setupIndicators(fragmentVm.pages.size)



        binding.btnNext.setOnClickListener {
            val current = fragmentVm.currentPage.value ?: 0
            if (current < fragmentVm.pages.lastIndex) {
                fragmentVm.currentPage.value = current + 1
            } else {
                fragmentVm.onGotItClicked()
            }
        }
        fragmentVm.currentPage.observe(viewLifecycleOwner) {
            updateIndicators(it)
        }
        fragmentVm.currentPage.observe(viewLifecycleOwner) {
            updateIndicators(it)
        }
    }

    private lateinit var indicators: Array<android.widget.ImageView>

    private fun setupIndicators(count: Int) {
        binding.dotIndicator.removeAllViews()
        indicators = Array(count) { android.widget.ImageView(requireContext()) }

        indicators.forEachIndexed { index, imageView ->
            imageView.layoutParams = android.widget.LinearLayout.LayoutParams(8.dp, 8.dp).apply {
                marginEnd = if (index != count - 1) 8.dp else 0
            }
            imageView.setBackgroundResource(R.drawable.dot_inactive)
            binding.dotIndicator.addView(imageView)
        }
    }

    private fun updateIndicators(selected: Int) {
        indicators.forEachIndexed { index, imageView ->
            imageView.setBackgroundResource(
                if (index == selected) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }


    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
