package com.leosoft.longevity.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PulseCameraManager {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null

    fun bindForPulse(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onFrame: (lumaMean: Double, ts: Long) -> Unit,
        onTorchAvailability: (Boolean) -> Unit
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build().also { analyzer ->
                    analyzer.setAnalyzer(executor) { image ->
                        val y = image.planes[0].buffer
                        var sum = 0.0
                        val size = y.remaining().coerceAtLeast(1)
                        while (y.hasRemaining()) {
                            sum += (y.get().toInt() and 0xFF)
                        }
                        onFrame(sum / size.toDouble(), image.imageInfo.timestamp)
                        image.close()
                    }
                }
            provider.unbindAll()
            camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
            onTorchAvailability(camera?.cameraInfo?.hasFlashUnit() == true)
        }, ContextCompat.getMainExecutor(context))
    }

    fun setTorch(enabled: Boolean): Boolean {
        val hasTorch = camera?.cameraInfo?.hasFlashUnit() == true
        if (hasTorch) camera?.cameraControl?.enableTorch(enabled)
        return hasTorch
    }

    fun unbind(context: Context) {
        ProcessCameraProvider.getInstance(context).get().unbindAll()
    }
}
