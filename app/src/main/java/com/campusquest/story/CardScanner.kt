package com.campusquest.story

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/** Owns only its use cases; every ImageProxy is closed even after scanner failure. */
class CardScanner(private val fragment: Fragment, private val onCard: (String) -> Unit, private val onState: (String) -> Unit) {
    private val executor = Executors.newSingleThreadExecutor()
    private val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var analysis: ImageAnalysis? = null
    private var generation = 0
    var torchOn = false; private set
    var hasFlash = false; private set

    @androidx.camera.core.ExperimentalGetImage
    fun start(view: PreviewView) {
        val context = fragment.requireContext()
        val current = ++generation
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (current != generation || fragment.view == null) return@addListener
            try {
                val p = future.get(); provider = p
                val pv = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }; preview = pv
                val az = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build(); analysis = az
                az.setAnalyzer(executor) { proxy ->
                    val frame = proxy.image
                    if (frame == null || current != generation) { proxy.close(); return@setAnalyzer }
                    scanner.process(InputImage.fromMediaImage(frame,proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codes -> if (current == generation) codes.firstNotNullOfOrNull { it.rawValue }?.let(onCard) }
                        .addOnFailureListener { if (current == generation) onState("Could not read the card. Try again or enter its code.") }
                        .addOnCompleteListener { proxy.close() }
                }
                camera = p.bindToLifecycle(fragment.viewLifecycleOwner,CameraSelector.DEFAULT_BACK_CAMERA,pv,az)
                hasFlash = camera!!.cameraInfo.hasFlashUnit()
                camera!!.cameraInfo.torchState.observe(fragment.viewLifecycleOwner) { state ->
                    if (current == generation) { torchOn = state == TorchState.ON; onState(if (torchOn) "Light is on" else "Light is off") }
                }
                onState(if (hasFlash) "Point at the repair card. Use Light on if it is dark." else "No flashlight available. Use available light or enter the code.")
            } catch (_: Exception) { onState("Camera unavailable. Enter the code printed under the card.") }
        },ContextCompat.getMainExecutor(context))
    }
    fun toggleTorch() {
        val c = camera ?: return
        if (!hasFlash) { onState("This camera has no flashlight. You can enter the card code."); return }
        val result = c.cameraControl.enableTorch(!torchOn)
        result.addListener({ runCatching { result.get() }.onFailure { onState("Light unavailable. Try again or enter the card code.") } },ContextCompat.getMainExecutor(fragment.requireContext()))
    }
    fun stop() {
        generation++
        camera?.cameraControl?.enableTorch(false)
        analysis?.clearAnalyzer()
        val cases = listOfNotNull(preview,analysis).toTypedArray()
        if (cases.isNotEmpty()) provider?.unbind(*cases)
        camera=null;preview=null;analysis=null;torchOn=false
    }
    fun close() { stop(); scanner.close(); executor.shutdown() }
}
