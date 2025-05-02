package com.albertsons.acupick.ui.manualentry.pick.weight

import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.response.getWeightedItemMaxWeight
import com.albertsons.acupick.databinding.ManualEntryWeightFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.manualentry.ManualEntryType
import com.albertsons.acupick.ui.manualentry.ManualEntryWeightUi
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerFragmentArgs
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel
import com.albertsons.acupick.ui.manualentry.pick.weight.ManualEntryWeightViewModel.Companion.MAX_WEIGHT_ERROR_REQUEST_KEY
import com.albertsons.acupick.ui.manualentry.pick.weight.ManualEntryWeightViewModel.Companion.MAX_WEIGHT_ERROR_RESULTS
import org.koin.android.ext.android.inject
import timber.log.Timber

class ManualEntryWeightFragment : BaseFragment<ManualEntryWeightViewModel, ManualEntryWeightFragmentBinding>() {
    override fun getLayoutRes() = R.layout.manual_entry_weight_fragment

    // DI
    val barcodeMapper: BarcodeMapper by inject()

    // View models
    override val fragmentViewModel: ManualEntryWeightViewModel by navGraphViewModels(R.id.manualEntryScope)

    val pagerVm: ManualEntryPagerViewModel by navGraphViewModels(R.id.manualEntryScope)

    // Nav args
    private val args: ManualEntryPagerFragmentArgs by navArgs()

    override fun setupBinding(binding: ManualEntryWeightFragmentBinding) {
        super.setupBinding(binding)
        binding.pagerVm = pagerVm
        val entry = args.manualEntryParams
        with(fragmentViewModel) {
            manualEntryWeightUI.value = ManualEntryWeightUi(entry)
            pickListItem.value = entry.selectedItem

            pagerVm.triggerBarcodeCollection.observe(viewLifecycleOwner) {
                if (pagerVm.selectedTabFlow.value == ManualEntryType.Weight) {
                    // To show error promt if the entered weight will be greater than max weight limit
                    if ((isMaxWeightValidationRequired() && validateMaxWeight(weightEntryText.value.orEmpty()))) {
                        val result = Bundle().apply {
                            putString(MAX_WEIGHT_ERROR_RESULTS, pickListItem.value?.getWeightedItemMaxWeight().toString())
                        }
                        requireParentFragment().setFragmentResult(MAX_WEIGHT_ERROR_REQUEST_KEY, result)
                        return@observe
                    }
                    val barcode = barcodeMapper.generateWeightedBarcode(weightPluEntryText.value.orEmpty(), weightEntryText.value.orEmpty())
                    pagerVm.onBarcodeCollected(barcode)
                }
            }

            weightEntryText.observe(viewLifecycleOwner) {
                pagerVm.weightEntry.postValue(it)
            }

            continueEnabled.observe(viewLifecycleOwner) {
                Timber.e("isContinueEnabled weight set in child page view model")
                pagerVm.setContinueEnabled(it)
            }

            weightEntryText.observe(viewLifecycleOwner) {
                validateWeight()
            }

            weightPluEntryText.observe(viewLifecycleOwner) {
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
     * Request focus setting on edittext on Weighted tab got selected
     */
    override fun onResume() {
        super.onResume()
        pagerVm.isWeighted.postValue(true)
    }
}
