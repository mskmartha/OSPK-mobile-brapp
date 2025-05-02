package com.albertsons.acupick.ui.itemdetails

import android.os.Bundle
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ItemDetailsFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.itemphoto.ItemPhotoDialog
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/** Item details (for a specific item in a Pick List) */
class ItemDetailsFragment : BaseFragment<ItemDetailsViewModel, ItemDetailsFragmentBinding>() {
    override val fragmentViewModel: ItemDetailsViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    override fun getLayoutRes(): Int = R.layout.item_details_fragment

    private val args: ItemDetailsFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(fragmentViewModel) {
            iaId.value = args.itemDetailsParams.iaId
            actId = args.itemDetailsParams.actId
            pickNumber.value = args.itemDetailsParams.activityNo
            altLocations.value = args.itemDetailsParams.altItemLocations
        }
    }

    override fun setupBinding(binding: ItemDetailsFragmentBinding) {
        super.setupBinding(binding)
        activityViewModel.setToolbarTitle(getString(R.string.title_item_details))

        activityViewModel.scannedData.observe(viewLifecycleOwner) {
            fragmentViewModel.onScannerBarcodeReceived(it)
        }

        fragmentViewModel.showItemPhotoDialog.observe(viewLifecycleOwner) { imageUrl ->
            ItemPhotoDialog.newInstance(imageUrl).let { newDialog ->
                if (isDetached) return@let

                with(childFragmentManager) {
                    if (isStateSaved) return@let

                    with(beginTransaction()) {
                        findFragmentByTag(ItemPhotoDialog.TAG)?.let { remove(it) }
                        newDialog.show(this, ItemPhotoDialog.TAG)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // using fragment lifecycle stages to transfer data between item details and pick list items
        // pick list items stage will be onCreateView() when this is called
        if (fragmentViewModel.barcodeType.value != null) {
            activityViewModel.setScannedData(fragmentViewModel.barcodeType.value?.rawBarcode)
        }
    }
}
