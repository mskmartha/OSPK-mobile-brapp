package com.albertsons.acupick.ui.arrivals.complete.idverification

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.IdentificationBarcodeScanFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.arrivals.complete.HandOffVerificationSharedViewModel
import com.albertsons.acupick.ui.arrivals.complete.VerificationIdTypeFragmentArgs
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.concurrent.Executors

class IdentificationBarcodeScanFragment : BaseFragment<IdentificationBarcodeScanViewModel, IdentificationBarcodeScanFragmentBinding>() {

    override val fragmentViewModel: IdentificationBarcodeScanViewModel by viewModel()

    val sharedViewModel: HandOffVerificationSharedViewModel by navGraphViewModels(R.id.handOffScope)

    private val args: VerificationIdTypeFragmentArgs by navArgs()

    override fun getLayoutRes() = R.layout.identification_barcode_scan_fragment
    private var preview: Preview? = null

    override fun setupBinding(binding: IdentificationBarcodeScanFragmentBinding) {
        super.setupBinding(binding)

        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        if (cameraPermissionGranted()) {
            startCamera()
        } else {
            requestCameraPermission()
        }

        with(fragmentViewModel) {
            orderNumber.value = args.orderNumber

            lifecycleScope.launchWhenStarted {
                scannedDriversLicense.filterNotNull().collect { driversLicenseData ->
                    sharedViewModel.orderInfoMap[orderNumber.value]?.identificationInfo = driversLicenseData
                    clearImageAnalyzer()
                }
            }

            lifecycleScope.launchWhenStarted {
                navigateBackEvent.collect {
                    navigateBack()
                }
            }
        }
    }

    // /////////////////////////////
    // / Camera and Barcode Scanning
    // /////////////////////////////
    // Select the back camera
    private val cameraSelector: CameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    //  Configure the barcode scanner
    private val barcodeScannerClient = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_PDF417
            ).build()
    )

    // Setup ML Kit image analyzer use case
    @ExperimentalGetImage
    private val imageAnalysis = ImageAnalysis.Builder()
        .setTargetResolution(Size(1440, 2560))
        .build().apply {
            setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                fragmentViewModel.processImage(imageProxy, barcodeScannerClient)
            }
        }

    // Show the camera image on the screen
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Bind the usecases to the lifecycle
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)
            } catch (e: Exception) {
                Timber.d("[BarcodeScan] Use case binding failed $e")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun clearImageAnalyzer() {
        imageAnalysis.clearAnalyzer()
    }

    private fun requestCameraPermission() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { permissionGranted ->
            if (permissionGranted) {
                startCamera()
            }
        }

        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun cameraPermissionGranted() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)?.supportActionBar?.hide()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity?)?.supportActionBar?.show()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
}
