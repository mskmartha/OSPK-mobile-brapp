package com.albertsons.acupick.ui.arrivals.pharmacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.PrescriptionReturnFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.manualentry.pharmacy.ManualEntryPharmacyViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class PrescriptionReturnFragment : BaseFragment<PrescriptionReturnViewModel, PrescriptionReturnFragmentBinding>() {

    override fun getLayoutRes() = R.layout.prescription_return_fragment
    override val fragmentViewModel: PrescriptionReturnViewModel by viewModel()
    private val args: PrescriptionReturnFragmentArgs by navArgs()

    override fun setupBinding(binding: PrescriptionReturnFragmentBinding) {
        super.setupBinding(binding)
        // binding.fragmentLifecycleOwner = viewLifecycleOwner
        activityViewModel.scannedData.observe(viewLifecycleOwner) {
            fragmentViewModel.onScannerBarcodeReceived(it)
        }
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // Block back button and do nothing here
        }
        setUpFragmentResultListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentViewModel.updateData(args.prescriptionReturnData)
        fragmentViewModel.loadData(args.erId.toLongOrNull() ?: 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentViewModel.showPrescritonReturned.observe(viewLifecycleOwner) {
            activityViewModel.setToolBarVisibility(!it)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setUpFragmentResultListeners() {
        setFragmentResultListener(ManualEntryPharmacyViewModel.MANUAL_ENTRY_PHARMACY) { _, bundle ->
            Timber.d("1292 MANUAL_ENTRY_WINE_STAGING Recieved")
            // Any type can be passed via to the bundle
            val manualEntryResults = bundle.get(ManualEntryPharmacyViewModel.MANUAL_ENTRY_PHARMACY_RESULTS)
            (manualEntryResults as? ManualEntryPharmacyData)?.let { stagingData ->
                stagingData.stagingContainer?.let {
                    fragmentViewModel.onManualEntryBarcodeReceived(stagingData)
                }
            }
        }
    }
}
