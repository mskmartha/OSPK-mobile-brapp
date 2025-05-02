package com.albertsons.acupick.ui.staging.winestaging

import android.os.Bundle
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.WineStaging3FragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingData
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingViewModel
import com.albertsons.acupick.ui.staging.winestaging.weight.WineStaging2FragmentArgs
import com.albertsons.acupick.ui.staging.winestaging.weight.WineStaging3ViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class WineStaging3Fragment : BaseFragment<WineStaging3ViewModel, WineStaging3FragmentBinding>() {
    private val args: WineStaging2FragmentArgs by navArgs()
    override fun getLayoutRes() = R.layout.wine_staging3_fragment
    override val fragmentViewModel: WineStaging3ViewModel by viewModel() {
        parametersOf(args.boxUiData, args.wineStagingParams)
    }
    override fun setupBinding(binding: WineStaging3FragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentLifecycleOwner = viewLifecycleOwner
        activityViewModel.scannedData.observe(viewLifecycleOwner) {
            fragmentViewModel.onScannerBarcodeReceived(it)
        }
        setUpFragmentResultListeners()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentViewModel.setupHeader(args.wineStagingParams, args.boxUiData?.boxDataList?.size ?: 0, args.shouldShowPrintReminder)
        if (args.boxUiData != null) {
            fragmentViewModel.loadData(args.boxUiData, args.wineStagingParams?.activityId)
        } else {
            fragmentViewModel.fetchData(args.wineStagingParams)
        }
    }

    private fun setUpFragmentResultListeners() {
        setFragmentResultListener(ManualEntryStagingViewModel.MANUAL_ENTRY_STAGING) { _, bundle ->
            Timber.d("1292 MANUAL_ENTRY_WINE_STAGING Recieved")
            // Any type can be passed via to the bundle
            val manualEntryResults = bundle.get(ManualEntryStagingViewModel.MANUAL_ENTRY_STAGING_RESULTS)
            (manualEntryResults as? ManualEntryStagingData)?.let { stagingData ->
                fragmentViewModel.isScanFromManualEntry.value = true
                stagingData.zone?.let {
                    fragmentViewModel.onManualEntryBarcodeReceived(stagingData)
                }
            }
        }
    }
}
