package com.albertsons.acupick.ui.manualentry.pick

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.widget.ViewPager2
import com.albertsons.acupick.EventCategory
import com.albertsons.acupick.EventKey
import com.albertsons.acupick.EventLabel
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.FulfilledQuantityResult
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemDetailDto
import com.albertsons.acupick.databinding.ManualEntryPagerFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.bottomsheetdialog.ConfirmAmountBottomSheet.Companion.CONFIRM_AMOUNT_REQUEST
import com.albertsons.acupick.ui.bottomsheetdialog.ConfirmAmountBottomSheet.Companion.CONFIRM_AMOUNT_REQUEST_RESULT
import com.albertsons.acupick.ui.manualentry.ManualEntryType
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.BARCODE_TYPE
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.BYPASS_QUANTITY_PICKER
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.MANUAL_ENTRY_PICK
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.MANUAL_ENTRY_PICK_RESULTS
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.MANUAL_ENTRY_SUBSTITUTION
import com.albertsons.acupick.ui.manualentry.pick.weight.ManualEntryWeightViewModel
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.util.EventAction
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.UserFeedback
import com.albertsons.acupick.ui.util.hideKeyboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ManualEntryPagerFragment : BaseFragment<ManualEntryPagerViewModel, ManualEntryPagerFragmentBinding>() {
    override val fragmentViewModel: ManualEntryPagerViewModel by navGraphViewModels(R.id.manualEntryScope)
    private val userFeedback by inject<UserFeedback>()

    var selectedTabIndex: Int = 0
    override fun getLayoutRes(): Int = R.layout.manual_entry_pager_fragment

    // Nav args
    private val args: ManualEntryPagerFragmentArgs by navArgs()

    override fun setupBinding(binding: ManualEntryPagerFragmentBinding) {
        super.setupBinding(binding)

        with(fragmentViewModel) {
            with(args.manualEntryParams) {
                setEntryParams(this)
                fragmentViewModel.tabVisibility.value = isSubstitution || isIssueScanning
                tabData.postValue(setTabData(this))

                // This prevents swiping inside of the ViewPager when in the Picking Flow
                binding.manualEntryViewPager.isUserInputEnabled = isSubstitution || isIssueScanning

                val listPair = listOf(Pair(EventKey.BOTTOM_SHEET_NAME, if (isSubstitution) MANUAL_ENTRY_SUBSTITUTION else MANUAL_ENTRY_PICK))
                fragmentViewModel.firebaseAnalytics.logEvent(EventCategory.BOTTOM_SHEET, EventAction.SCREEN_VIEW, EventLabel.BOTTOM_SHEET_STATE_OPEN, listPair)
            }

            quantity.observe(viewLifecycleOwner) {
                handleQuantityResult(
                    fulfilledQuantityResult = FulfilledQuantityResult.QuantityPicker(it),
                    weightEntry = weightEntry.value,
                    itemDetails = enteredItemDetails
                )
            }

            barcodeType.observe(viewLifecycleOwner) {
                byPassManualEntry(it)
            }

            fragmentViewModel.playScanSound.observe(viewLifecycleOwner) { isSuccess ->
                when (isSuccess) {
                    true -> userFeedback.setSuccessScannedSoundAndHaptic()
                    false -> userFeedback.setFailureScannedSoundAndHaptic()
                }
            }

            setFragmentResultListener(CONFIRM_AMOUNT_REQUEST) { _, bundle ->
                val serializableResult = bundle.getSerializable(CONFIRM_AMOUNT_REQUEST_RESULT)
                (serializableResult as? FulfilledQuantityResult.ConfirmNetWeightResult)?.let { confirmNetWeightResult ->
                    if (confirmNetWeightResult.itemType == SellByType.PriceScaled) {
                        handleQuantityResult(
                            fulfilledQuantityResult = confirmNetWeightResult,
                            weightEntry = weightEntry.value,
                            itemDetails = enteredItemDetails
                        )
                    } else if (confirmNetWeightResult.itemType == SellByType.PriceEachTotal) {
                        handleQuantityResult(
                            fulfilledQuantityResult = FulfilledQuantityResult.QuantityPicker(confirmNetWeightResult.toQuantity()),
                            weightEntry = weightEntry.value,
                            itemDetails = enteredItemDetails
                        )
                    }
                }
            }

            // Show error message too heavy weight on manual entry bottomsheet of weighted item type
            setFragmentResultListener(ManualEntryWeightViewModel.MAX_WEIGHT_ERROR_REQUEST_KEY) { _, bundle ->
                val maxWeight = bundle.getString(ManualEntryWeightViewModel.MAX_WEIGHT_ERROR_RESULTS)
                maxWeight?.let {
                    showSnackBar(AcupickSnackEvent(message = StringIdHelper.Format(R.string.error_message_description_entered_weight_too_heavy, it), SnackType.ERROR))
                }
            }
        }

        setAdapter(binding)
    }

    // Used to initially set the adapter and position and to refresh data
    private fun setAdapter(binding: ManualEntryPagerFragmentBinding) {
        binding.apply {
            with(manualEntryViewPager) {

                // when switching tabs, notify the destination tab/fragment to give focus to its first EditText
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        hideKeyboard() // Hide keyboard on tab got change
                        lifecycleScope.launch {
                            fragmentViewModel.tabData.value?.get(position)?.tabArguments?.entryType?.let {
                                fragmentViewModel.setSelectedTab(it)
                                fragmentViewModel.activeTab.emit(it.name)
                            }
                        }
                    }
                })
                offscreenPageLimit = 1
            }
            fragmentViewModel.tabData.distinctUntilChanged().observe(viewLifecycleOwner) { tabUi ->
                manualEntryViewPager.adapter = ManualEntryPagerAdapter(this@ManualEntryPagerFragment, tabUi)
                (manualEntryViewPager.adapter as ManualEntryPagerAdapter).menuTabMediatorFactory(manualEntryTabLayout, manualEntryViewPager).attach()
                selectedTabIndex = when (args.entryType) {
                    ManualEntryType.UPC -> 0
                    ManualEntryType.PLU -> 1
                    ManualEntryType.Weight -> 2
                    else -> 0
                }
                manualEntryViewPager.setCurrentItem(selectedTabIndex, false)
            }
        }
    }

    private fun handleQuantityResult(
        fulfilledQuantityResult: FulfilledQuantityResult,
        weightEntry: String?,
        itemDetails: ItemDetailDto?
    ) {
        // To notify the super parent fragment we need to use supportFragmentManager
        requireActivity().supportFragmentManager.setFragmentResult(
            MANUAL_ENTRY_PICK,
            bundleOf(
                MANUAL_ENTRY_PICK_RESULTS to ManualEntryPickResults(
                    quantity = fulfilledQuantityResult,
                    barcode = fragmentViewModel.itemEnteredBarcode,
                    weightEntry = weightEntry,
                    itemDetails = itemDetails
                )
            )
        )

        lifecycleScope.launchWhenResumed {
            delay(500) // Delay for smooth transition
            fragmentViewModel.navigateUp()
        }
    }

    private fun byPassManualEntry(
        barcode: BarcodeType,
    ) {
        requireParentFragment().setFragmentResult(
            BYPASS_QUANTITY_PICKER,
            bundleOf(
                BARCODE_TYPE to barcode
            )
        )
        lifecycleScope.launchWhenResumed {
            fragmentViewModel.navigateUp()
        }
    }

    override fun onDestroyView(binding: ManualEntryPagerFragmentBinding) {
        val listPair = listOf(Pair(EventKey.BOTTOM_SHEET_NAME, MANUAL_ENTRY_PICK))
        fragmentViewModel.firebaseAnalytics.logEvent(EventCategory.BOTTOM_SHEET, EventAction.SCREEN_VIEW, EventLabel.BOTTOM_SHEET_STATE_CLOSE, listPair)

        binding.apply { manualEntryViewPager.adapter = null }
        super.onDestroyView(binding)
    }
}
