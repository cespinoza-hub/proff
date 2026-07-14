package com.grupointelecto.inventario

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.grupointelecto.inventario.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private val PREFS_NAME = "inventario_prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si ya hay una sesión previa en este dispositivo, se precargan los campos
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        binding.etUsuario.setText(prefs.getString("usuario", ""))
        binding.etEmpresa.setText(prefs.getString("empresa", ""))

        binding.btnContinuar.setOnClickListener {
            val usuario = binding.etUsuario.text?.toString()?.trim().orEmpty()
            val empresa = binding.etEmpresa.text?.toString()?.trim().orEmpty()

            if (usuario.isEmpty()) {
                binding.tilUsuario.error = "Ingresa el nombre de usuario"
                return@setOnClickListener
            }
            if (empresa.isEmpty()) {
                binding.tilEmpresa.error = "Ingresa el nombre de la empresa"
                return@setOnClickListener
            }

            prefs.edit()
                .putString("usuario", usuario)
                .putString("empresa", empresa)
                .apply()

            startActivity(Intent(this, InventoryActivity::class.java))
        }

        binding.etUsuario.setOnFocusChangeListener { _, _ -> binding.tilUsuario.error = null }
        binding.etEmpresa.setOnFocusChangeListener { _, _ -> binding.tilEmpresa.error = null }
    }
}
