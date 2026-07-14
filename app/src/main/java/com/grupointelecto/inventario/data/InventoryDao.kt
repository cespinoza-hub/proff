package com.grupointelecto.inventario.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Insert
    suspend fun insertar(item: InventoryItem): Long

    @Delete
    suspend fun eliminar(item: InventoryItem)

    @Query("SELECT * FROM inventory_items ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items ORDER BY id ASC")
    suspend fun obtenerTodosParaExportar(): List<InventoryItem>

    @Query("DELETE FROM inventory_items")
    suspend fun eliminarTodos()
}
