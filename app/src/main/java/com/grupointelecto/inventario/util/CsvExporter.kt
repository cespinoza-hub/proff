package com.grupointelecto.inventario.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.grupointelecto.inventario.data.InventoryItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta el listado de inventario a un archivo CSV con el formato:
 * nombreUsuario_yyyyMMdd_HHmmss.csv
 * y lo guarda en el directorio público "Documentos" del dispositivo.
 */
object CsvExporter {

    fun exportar(context: Context, usuario: String, items: List<InventoryItem>): String? {
        val nombreArchivo = construirNombreArchivo(usuario)
        val contenido = construirContenidoCsv(items)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exportarConMediaStore(context, nombreArchivo, contenido)
            } else {
                exportarLegacy(nombreArchivo, contenido)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun construirNombreArchivo(usuario: String): String {
        val usuarioLimpio = usuario.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fecha = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${usuarioLimpio}_${fecha}.csv"
    }

    private fun construirContenidoCsv(items: List<InventoryItem>): String {
        val sb = StringBuilder()
        // Se usa ";" como separador por compatibilidad con Excel en configuración regional en español
        sb.append("Usuario;Empresa;Cantidad;Codigo;TipoCodigo;FechaHora\n")
        items.forEach { item ->
            sb.append("${item.usuario};${item.empresa};${item.cantidad};${item.codigo};${item.tipoCodigo};${item.fechaHora}\n")
        }
        return sb.toString()
    }

    /** Android 10 (API 29) en adelante: se usa MediaStore, no requiere permiso de almacenamiento. */
    private fun exportarConMediaStore(context: Context, nombreArchivo: String, contenido: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }
        val uri: Uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            ?: throw Exception("No se pudo crear el archivo en MediaStore")

        resolver.openOutputStream(uri)?.use { out ->
            out.write(contenido.toByteArray())
        }
        return "Documents/$nombreArchivo"
    }

    /** Android 8/9 (API 26-28): se escribe directo en almacenamiento externo (requiere permiso). */
    private fun exportarLegacy(nombreArchivo: String, contenido: String): String {
        val carpetaDocumentos = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!carpetaDocumentos.exists()) carpetaDocumentos.mkdirs()
        val archivo = File(carpetaDocumentos, nombreArchivo)
        FileOutputStream(archivo).use { fos ->
            fos.write(contenido.toByteArray())
        }
        return archivo.absolutePath
    }
}
