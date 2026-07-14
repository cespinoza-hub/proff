package com.grupointelecto.inventario

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.grupointelecto.inventario.databinding.ActivityScannerBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private val codigoDetectado = AtomicBoolean(false)

    companion object {
        const val EXTRA_CODIGO = "extra_codigo"
        const val EXTRA_TIPO = "extra_tipo"

        fun crearIntent(context: Context): Intent = Intent(context, ScannerActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        iniciarCamara()

        binding.btnManual.setOnClickListener { mostrarDialogoManual() }
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // FORMAT_ALL_FORMATS habilita la detección de QR, EAN, UPC, Code128, Code39,
            // Code93, Codabar, Data Matrix, PDF417, Aztec, ITF, etc.
            val opciones = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
            val scanner = BarcodeScanning.getClient(opciones)

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                procesarImagen(imageProxy, scanner)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                )
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo iniciar la cámara: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun procesarImagen(imageProxy: ImageProxy, scanner: BarcodeScanner) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !codigoDetectado.get()) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && codigoDetectado.compareAndSet(false, true)) {
                        val barcode = barcodes[0]
                        val valor = barcode.rawValue ?: barcode.displayValue
                        val tipo = nombreFormato(barcode.format)
                        if (!valor.isNullOrEmpty()) {
                            runOnUiThread { retornarResultado(valor, tipo) }
                        } else {
                            codigoDetectado.set(false)
                        }
                    }
                }
                .addOnFailureListener {
                    // Se ignora el error puntual; se sigue intentando con el siguiente frame
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun nombreFormato(formato: Int): String = when (formato) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_ITF -> "ITF"
        else -> "OTRO"
    }

    private fun mostrarDialogoManual() {
        val input = EditText(this)
        input.hint = "Código del producto"
        AlertDialog.Builder(this)
            .setTitle("Ingreso manual")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val texto = input.text?.toString()?.trim().orEmpty()
                if (texto.isNotEmpty()) {
                    retornarResultado(texto, "MANUAL")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun retornarResultado(codigo: String, tipo: String) {
        val data = Intent().apply {
            putExtra(EXTRA_CODIGO, codigo)
            putExtra(EXTRA_TIPO, tipo)
        }
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
