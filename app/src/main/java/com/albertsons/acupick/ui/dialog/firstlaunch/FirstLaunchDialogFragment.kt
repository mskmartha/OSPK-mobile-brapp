package com.albertsons.acupick.ui.dialog.firstlaunch

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
import com.albertsons.acupick.ui.dialog.BaseCustomDialogFragment

class FirstLaunchDialogFragment : BaseCustomDialogFragment() {

    private lateinit var binding: FirstLaunchDialogFragmentBinding
    private val viewModel: FirstLaunchDialogViewModel by viewModels()

    override val shouldFillScreen
        get() = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getViewDataBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ViewDataBinding {
        binding = DataBindingUtil.inflate(inflater, R.layout.first_launch_dialog_fragment, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = OnboardingPagerAdapter(viewModel.pages)
        binding.viewPager.adapter = adapter

        setupIndicators(viewModel.pages.size)

        viewModel.currentPage.observe(viewLifecycleOwner) {
            updateIndicators(it)
        }

        binding.btnNext.setOnClickListener {
            val current = viewModel.currentPage.value ?: 0
            if (current < viewModel.pages.lastIndex) {
                viewModel.currentPage.value = current + 1
            } else {
                dismiss()
            }
        }
        viewModel.currentPage.observe(viewLifecycleOwner) {
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
