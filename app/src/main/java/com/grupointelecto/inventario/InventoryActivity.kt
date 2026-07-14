package com.grupointelecto.inventario

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.grupointelecto.inventario.adapter.InventoryAdapter
import com.grupointelecto.inventario.data.AppDatabase
import com.grupointelecto.inventario.data.InventoryItem
import com.grupointelecto.inventario.databinding.ActivityInventoryBinding
import com.grupointelecto.inventario.util.CsvExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInventoryBinding
    private lateinit var adapter: InventoryAdapter
    private val db by lazy { AppDatabase.getInstance(this) }

    private var usuario: String = ""
    private var empresa: String = ""

    // Resultado que llega desde la pantalla de escaneo
    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val codigo = result.data?.getStringExtra(ScannerActivity.EXTRA_CODIGO)
            val tipo = result.data?.getStringExtra(ScannerActivity.EXTRA_TIPO) ?: "DESCONOCIDO"
            if (!codigo.isNullOrEmpty()) {
                guardarItem(codigo, tipo)
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scannerLauncher.launch(ScannerActivity.crearIntent(this))
        } else {
            Toast.makeText(this, "Se necesita permiso de cámara para escanear", Toast.LENGTH_LONG).show()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) exportarCsv()
        else Toast.makeText(this, "Se necesita permiso de almacenamiento para exportar", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val prefs = getSharedPreferences("inventario_prefs", MODE_PRIVATE)
        usuario = prefs.getString("usuario", "") ?: ""
        empresa = prefs.getString("empresa", "") ?: ""
        binding.tvHeader.text = "Usuario: $usuario  |  Empresa: $empresa"

        adapter = InventoryAdapter { item -> confirmarEliminar(item) }
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter

        observarItems()

        binding.btnEscanear.setOnClickListener { iniciarEscaneo() }
        binding.btnExportar.setOnClickListener { verificarPermisoYExportar() }
    }

    private fun observarItems() {
        lifecycleScope.launch {
            db.inventoryDao().obtenerTodos().collect { lista ->
                adapter.actualizarLista(lista)
                binding.tvTotal.text =
                    "Artículos escaneados: ${lista.size}  |  Suma de cantidades: ${lista.sumOf { it.cantidad }}"
            }
        }
    }

    private fun iniciarEscaneo() {
        val cantidadTexto = binding.etCantidad.text?.toString()?.trim().orEmpty()
        val cantidad = cantidadTexto.toIntOrNull()

        if (cantidad == null || cantidad <= 0) {
            binding.tilCantidad.error = "Ingresa una cantidad válida"
            return
        }
        binding.tilCantidad.error = null

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        scannerLauncher.launch(ScannerActivity.crearIntent(this))
    }

    private fun guardarItem(codigo: String, tipo: String) {
        val cantidad = binding.etCantidad.text?.toString()?.trim()?.toIntOrNull() ?: 1
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val item = InventoryItem(
            usuario = usuario,
            empresa = empresa,
            cantidad = cantidad,
            codigo = codigo,
            tipoCodigo = tipo,
            fechaHora = fecha
        )

        lifecycleScope.launch {
            db.inventoryDao().insertar(item)
            binding.etCantidad.text?.clear()
            Toast.makeText(this@InventoryActivity, "Código guardado: $codigo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarEliminar(item: InventoryItem) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar registro")
            .setMessage("¿Eliminar el código ${item.codigo}?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch { db.inventoryDao().eliminar(item) }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun verificarPermisoYExportar() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            exportarCsv()
        }
    }

    private fun exportarCsv() {
        lifecycleScope.launch {
            val items = db.inventoryDao().obtenerTodosParaExportar()
            if (items.isEmpty()) {
                Toast.makeText(this@InventoryActivity, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val ruta = CsvExporter.exportar(this@InventoryActivity, usuario, items)
            if (ruta != null) {
                Toast.makeText(this@InventoryActivity, "Archivo exportado en: $ruta", Toast.LENGTH_LONG).show()
                preguntarLimpiarDatos()
            } else {
                Toast.makeText(this@InventoryActivity, "Ocurrió un error al exportar", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun preguntarLimpiarDatos() {
        AlertDialog.Builder(this)
            .setTitle("Exportación completa")
            .setMessage("¿Deseas limpiar los datos temporales del dispositivo?")
            .setPositiveButton("Sí, limpiar") { _, _ ->
                lifecycleScope.launch { db.inventoryDao().eliminarTodos() }
            }
            .setNegativeButton("No, mantener", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_inventory, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_reset) {
            confirmarReinicioTotal()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Borra todos los registros escaneados y los datos de usuario/empresa
     * guardados, para que la app quede como recién instalada y se pueda
     * comenzar un inventario desde cero.
     */
    private fun confirmarReinicioTotal() {
        AlertDialog.Builder(this)
            .setTitle("Reiniciar todo")
            .setMessage("Esto eliminará todos los registros escaneados y los datos de usuario/empresa de este dispositivo para comenzar desde cero. Esta acción no se puede deshacer. ¿Deseas continuar?")
            .setPositiveButton("Sí, reiniciar") { _, _ -> reiniciarTodo() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reiniciarTodo() {
        lifecycleScope.launch {
            db.inventoryDao().eliminarTodos()
            getSharedPreferences("inventario_prefs", MODE_PRIVATE).edit().clear().apply()

            val intent = Intent(this@InventoryActivity, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
