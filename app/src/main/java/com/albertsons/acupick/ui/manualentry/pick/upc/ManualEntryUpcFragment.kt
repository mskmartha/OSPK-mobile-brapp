package com.albertsons.acupick.ui.manualentry.pick.upc

import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ManualEntryUpcFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.manualentry.ManualEntryType
import com.albertsons.acupick.ui.manualentry.ManualEntryUpcUi
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerFragmentArgs
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel
import timber.log.Timber

class ManualEntryUpcFragment : BaseFragment<ManualEntryUpcViewModel, ManualEntryUpcFragmentBinding>() {
    override fun getLayoutRes() = R.layout.manual_entry_upc_fragment

    // View models
    override val fragmentViewModel: ManualEntryUpcViewModel by navGraphViewModels(R.id.manualEntryScope)

    val pagerVm: ManualEntryPagerViewModel by navGraphViewModels(R.id.manualEntryScope)

    // Nav args
    private val args: ManualEntryPagerFragmentArgs by navArgs()

    override fun setupBinding(binding: ManualEntryUpcFragmentBinding) {
        super.setupBinding(binding)
        binding.pagerVm = pagerVm

        val entry = args.manualEntryParams

        with(fragmentViewModel) {
            manualEntryUPCUI.value = ManualEntryUpcUi(entry)
            pickListItem.value = entry.selectedItem

            pagerVm.triggerBarcodeCollection.observe(viewLifecycleOwner) {
                validateUpcTextEntry()
                if (pagerVm.selectedTabFlow.value == ManualEntryType.UPC && noActiveUpcErrors()) {
                    val barcode = barcodeMapper.inferBarcodeType(upcEntryText.value.orEmpty(), enableLogging = true)
                    pagerVm.onBarcodeCollected(barcode)
                }
            }

            continueEnabled.observe(viewLifecycleOwner) {
                Timber.e("isContinueEnabled upc set in child page view model")
                pagerVm.setContinueEnabled(it)
            }

            upcEntryText.observe(viewLifecycleOwner) {
                clearUpcError()
            }

            // Never get called
           /* closeKeyboard.observe(viewLifecycleOwner) {
                hideKeyboard(clearFocus = true)
            }*/

            quantity.observe(viewLifecycleOwner) {
                pagerVm.quantity.postValue(it)
            }
        }
    }

    /**
     * Request focus setting on edittext on UPC tab got selected
     */
    override fun onResume() {
        super.onResume()
        pagerVm.isUPC.postValue(true)
    }
}
