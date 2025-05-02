package com.albertsons.acupick.ui.chat

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.EventCategory
import com.albertsons.acupick.EventLabel
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.request.NetworkCalls
import com.albertsons.acupick.databinding.ChatFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.chatImagePreview.ChatImagePreviewDialog
import com.albertsons.acupick.ui.util.EventAction
import com.albertsons.acupick.ui.util.forceShowKeyboard
import com.albertsons.acupick.ui.util.getOrZero
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatFragment : BaseFragment<ChatViewModel, ChatFragmentBinding>() {

    private val args: ChatFragmentArgs by navArgs()
    private var imageCaptureUri: Uri = Uri.EMPTY
    lateinit var chatFragmentBinding: ChatFragmentBinding

    override val fragmentViewModel: ChatViewModel by viewModel {
        parametersOf(args.convetsationId, args.orderNumber, args.fulfullmentOrderNumber)
    }

    override fun getLayoutRes(): Int = R.layout.chat_fragment

    override fun setupBinding(binding: ChatFragmentBinding) {
        super.setupBinding(binding)
        chatFragmentBinding = binding
        binding.lifecycleOwner = this
        binding.viewModel = fragmentViewModel

        binding.messageAttachmentButton.setOnClickListener {
            startImageCapture()
        }
        binding.imageClose.setOnClickListener {
            fragmentViewModel.firebaseAnalytics.logEvent(EventCategory.CHAT, EventAction.CLICK, EventLabel.CHAT_IMAGE_PREVIEW_CLOSE)
            binding.previewImage.setImageURI(Uri.EMPTY)
            fragmentViewModel.setPhotoPreviewBoolean(false, "")
        }

        binding.editText.setEndIconOnClickListener {
            if (imageCaptureUri != Uri.EMPTY) {
                sendMediaMessage(imageCaptureUri)
                fragmentViewModel.setPhotoPreviewBoolean(false, "")
                imageCaptureUri = Uri.EMPTY
            } else {
                fragmentViewModel.sendMessage()
            }
        }

        fragmentViewModel.dismissChatNotication.observe(viewLifecycleOwner) {
            it?.let {
                val notificationManager = activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                it.forEach { id ->
                    notificationManager.cancel(id.toInt())
                }
            }
            fragmentViewModel.clearNotification()
        }

        fragmentViewModel.showItemPhotoDialog.observe(viewLifecycleOwner) { imageUrl ->
            ChatImagePreviewDialog.newInstance(imageUrl).let { newDialog ->
                if (isDetached) return@let

                with(childFragmentManager) {
                    if (isStateSaved) return@let

                    with(beginTransaction()) {
                        findFragmentByTag(ChatImagePreviewDialog.TAG)?.let { remove(it) }
                        newDialog.show(this, ChatImagePreviewDialog.TAG)
                    }
                }
            }
        }

        fragmentViewModel.typedMessage.observe(viewLifecycleOwner) {
            chatFragmentBinding.messageInput.apply {
                if (text?.length.getOrZero() > 0 && selectionStart == 0) {
                    setSelection(text?.length ?: 0)
                }
            }
        }
    }

    private fun startImageCapture() {
        val packageManager = requireContext().packageManager
        val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        // Check if the device has a camera app enabled
        if (!hasCamera) {
            Timber.d("No camera app enabled on the device")
            fragmentViewModel.handleCameraAccessError(NetworkCalls.CAMERA_PERMISSION_FEATURE_DISABLED)
            return
        }

        // avoid crash if permission is not granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("Camera Permission not granted")
            fragmentViewModel.handleCameraAccessError(NetworkCalls.CAMERA_PERMISSION_NOT_PROVIDED)
            return
        }

        fragmentViewModel.firebaseAnalytics.logEvent(EventCategory.CHAT, EventAction.CLICK, EventLabel.CHAT_IMAGE_UPLOAD_BTN)
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val picturesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", picturesDir)
        imageCaptureUri =
            FileProvider.getUriForFile(requireContext(), "com.albertsons.acupick.app.fileprovider", photoFile)
        Timber.d("Capturing image to $imageCaptureUri")

        try {
            takePicture.launch(imageCaptureUri)
        } catch (e: ActivityNotFoundException) {
            Timber.e("No activity found to handle the picture capture intent: $e")
            fragmentViewModel.handleCameraAccessError(NetworkCalls.CAMERA_PERMISSION_FEATURE_DISABLED)
        } catch (e: Exception) {
            fragmentViewModel.handleCameraAccessError(NetworkCalls.CAMERA_UNKNOWN_ERROR)
        }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val messageText = context?.resources?.getText(R.string.chat_image_preview_text)
            fragmentViewModel.setPhotoPreviewBoolean(true, messageText ?: "")
            activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            chatFragmentBinding.messageInput.apply {
                requestFocus()
                forceShowKeyboard()
            }
            chatFragmentBinding.previewImage.setImageURI(imageCaptureUri)
        }
    }

    private fun sendMediaMessage(uri: Uri) {
        val contentResolver = requireContext().contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val type = contentResolver.getType(uri)
        val name = contentResolver.getString(uri, OpenableColumns.DISPLAY_NAME)
        if (inputStream != null) {
            fragmentViewModel.sendMediaMessage(uri.toString(), inputStream, name, type)
        } else {
            // fragmentViewModel.onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
            Timber.w("Could not get input stream for file reading: $uri")
        }
    }

    fun ContentResolver.getString(uri: Uri, columnName: String): String? {
        val cursor = query(uri, arrayOf(columnName), null, null, null)
        return cursor?.let {
            it.moveToFirst()
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val name = cursor.getString(index)
            it.close()
            return@let name
        }
    }
}
