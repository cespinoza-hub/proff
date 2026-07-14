package com.grupointelecto.inventario.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local temporal. Los datos viven en el dispositivo hasta que
 * el usuario exporta a CSV (y opcionalmente los limpia) o desinstala la app.
 */
@Database(entities = [InventoryItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventario_temporal_db"
                ).build()
                INSTANCE = instancia
                instancia
            }
        }
    }
}
