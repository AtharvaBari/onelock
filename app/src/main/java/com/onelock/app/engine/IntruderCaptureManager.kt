package com.onelock.app.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.onelock.app.OneLockApp
import com.onelock.app.data.IntruderLogItem
import java.io.File
import java.util.UUID

object IntruderCaptureManager {
    private const val TAG = "IntruderCaptureManager"

    @Volatile
    var activePreviewView: PreviewView? = null

    fun captureIntruderSelfie(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        targetPackage: String,
        appName: String
    ) {
        val shield = OneLockApp.preferencesManager.getShieldConfig()
        if (!shield.intruderSelfieEnabled) {
            Log.d(TAG, "Intruder selfie disabled in shield settings")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission not granted for intruder capture")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Check if front camera is available
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                if (!cameraProvider.hasCamera(cameraSelector)) {
                    Log.w(TAG, "Front camera not found")
                    return@addListener
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()

                val preview = Preview.Builder().build()
                activePreviewView?.surfaceProvider?.let { surfaceProvider ->
                    preview.setSurfaceProvider(surfaceProvider)
                }

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                val dir = File(context.filesDir, "intruder_selfies").apply { mkdirs() }
                val photoFile = File(dir, "intruder_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.d(TAG, "Intruder photo saved: ${photoFile.absolutePath}")
                            cameraProvider.unbindAll()

                            val item = IntruderLogItem(
                                id = UUID.randomUUID().toString(),
                                imagePath = photoFile.absolutePath,
                                packageName = targetPackage,
                                appName = appName,
                                timestamp = System.currentTimeMillis()
                            )
                            OneLockApp.preferencesManager.addIntruderLog(item)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Failed to capture intruder photo", exception)
                            cameraProvider.unbindAll()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing CameraX for intruder selfie", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}

