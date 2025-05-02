package com.albertsons.acupick.ui.itemphoto

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.fragment.app.DialogFragment
import com.albertsons.acupick.databinding.ItemPhotoDialogBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class ItemPhotoDialog : DialogFragment() {
    private val viewModel: ItemPhotoViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ItemPhotoDialogBinding.inflate(layoutInflater, null, false).apply {
            viewModel = this@ItemPhotoDialog.viewModel
            viewModel?.imageUrl = arguments?.getString(IMAGE_URL_KEY) ?: ""
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.dismissEvent.observe(viewLifecycleOwner) { dismiss() }
    }

    override fun onStart() {
        super.onStart()

        // Force Dialog to full size with transparent background
        dialog?.window?.apply {
            setLayout(MATCH_PARENT, MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    companion object {
        private const val IMAGE_URL_KEY = "imageUrl"
        const val TAG = "itemPhotoDialogTag"

        fun newInstance(imageUrl: String) =
            ItemPhotoDialog().apply { arguments = Bundle().apply { putString(IMAGE_URL_KEY, imageUrl) } }
    }
}
