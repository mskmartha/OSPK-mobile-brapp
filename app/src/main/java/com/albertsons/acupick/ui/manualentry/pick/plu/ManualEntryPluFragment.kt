package com.albertsons.acupick.ui.manualentry.pick.plu

import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.databinding.ManualEntryPluFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.manualentry.ManualEntryPluUi
import com.albertsons.acupick.ui.manualentry.ManualEntryType
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerFragmentArgs
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel

import org.koin.android.ext.android.inject
import timber.log.Timber

class ManualEntryPluFragment : BaseFragment<ManualEntryPluViewModel, ManualEntryPluFragmentBinding>() {
    override fun getLayoutRes() = R.layout.manual_entry_plu_fragment

    // DI
    val barcodeMapper: BarcodeMapper by inject()

    // View models
    override val fragmentViewModel: ManualEntryPluViewModel by navGraphViewModels(R.id.manualEntryScope)

    val pagerVm: ManualEntryPagerViewModel by navGraphViewModels(R.id.manualEntryScope)

    // Nav args
    private val args: ManualEntryPagerFragmentArgs by navArgs()

    override fun setupBinding(binding: ManualEntryPluFragmentBinding) {
        super.setupBinding(binding)
        binding.pagerVm = pagerVm
        val entry = args.manualEntryParams
        with(fragmentViewModel) {
            manualEntryPLUUI.value = ManualEntryPluUi(entry)
            pickListItem.value = entry.selectedItem

            pagerVm.triggerBarcodeCollection.observe(viewLifecycleOwner) {
                if (pagerVm.selectedTabFlow.value == ManualEntryType.PLU) {
                    val barcode = barcodeMapper.generateEachBarcode(pluEntryText.value.orEmpty(), pickListItem.value?.id)
                    pagerVm.onBarcodeCollected(barcode)
                }
            }

            continueEnabled.observe(viewLifecycleOwner) {
                Timber.e("isContinueEnabled plu set in child page view model")
                pagerVm.setContinueEnabled(it)
            }

            manualEntryPLUUI.observe(viewLifecycleOwner) {
                setDefaultPlu(it.defaultValue)
            }

            pluEntryText.observe(viewLifecycleOwner) {
                validatePluTextEntry()
            }

            // Never get called
            /*closeKeyboard.observe(viewLifecycleOwner) {
                hideKeyboard(clearFocus = true)
            }*/

            quantity.observe(viewLifecycleOwner) {
                pagerVm.quantity.postValue(it)
            }
        }
    }

    /**
     * Request focus setting on edittext on PLU tab got selected
     */
    override fun onResume() {
        super.onResume()
        pagerVm.isPlu.postValue(true)
    }
}
