package com.nikhiljain.fundoonotes.cameraexample

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_auth.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AuthCameraDialogFragment : DialogFragment() {
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        button_start_camera.setOnClickListener {
            startCamera()
        }
        button_sign_in.setOnClickListener {
            savePhoto()
        }
    }

    private fun savePhoto() {
        // Get a stable reference of the modifiable image capture use case
        imageCapture?.let { imageCapture ->

            // Create output file to hold the image
            val photoFile = createFile(
                getOutputDirectory(requireContext()),
                FILENAME, EXTENSION
            )

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture
                .OutputFileOptions
                .Builder(photoFile)
                .build()

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {

                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        Log.d(TAG, "Photo capture succeeded: $savedUri")

                        // If the folder selected is an external media directory, this is
                        // unnecessary but otherwise other apps will not be able to access our
                        // images unless we scan them using [MediaScannerConnection]
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(savedUri.toFile().extension)
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(savedUri.toFile().absolutePath),
                            arrayOf(mimeType)
                        ) { _, uri ->
                            Log.d(TAG, "Image capture scanned into media store: $uri")
                        }
                    }
                })

            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Display flash animation to indicate that photo was captured
                previewView.postDelayed({
                    previewView.foreground = ColorDrawable(Color.WHITE)
                    previewView.postDelayed(
                        { previewView.foreground = null }, ANIMATION_FAST_MILLIS
                    )
                }, ANIMATION_SLOW_MILLIS)
            }
            return
        }

        // showing toast if camera has not been started yet
        Toast.makeText(requireContext(), "Camera has not been started yet", Toast.LENGTH_SHORT)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    /** Helper function used to create a timestamped file */
    private fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension
        )

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            Runnable {

                val cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder().build()
                preview?.setSurfaceProvider(previewView.createSurfaceProvider())
                imageCapture = ImageCapture.Builder().build()

                // here we are taking front camera
                // but we can initial check if camera is available or not
                // -> cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                // or cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    /** Use external media if it is available, our app's file directory otherwise */
    private fun getOutputDirectory(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }

    companion object {
        const val TAG = "AuthDialogFragment"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val EXTENSION = ".jpg"

        /** Milliseconds used for UI animations */
        private const val ANIMATION_FAST_MILLIS = 50L
        private const val ANIMATION_SLOW_MILLIS = 100L
    }
}
