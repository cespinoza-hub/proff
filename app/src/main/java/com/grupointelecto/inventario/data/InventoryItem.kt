package com.grupointelecto.inventario.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro individual de inventario guardado en la base de datos temporal.
 */
@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuario: String,
    val empresa: String,
    val cantidad: Int,
    val codigo: String,
    val tipoCodigo: String,
    val fechaHora: String
)
