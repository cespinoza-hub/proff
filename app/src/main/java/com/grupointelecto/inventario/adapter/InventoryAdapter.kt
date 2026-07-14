package com.grupointelecto.inventario.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.grupointelecto.inventario.data.InventoryItem
import com.grupointelecto.inventario.databinding.ItemInventoryBinding

class InventoryAdapter(
    private val onEliminarClick: (InventoryItem) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.ItemViewHolder>() {

    private val items = mutableListOf<InventoryItem>()

    fun actualizarLista(nuevaLista: List<InventoryItem>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = nuevaLista.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos].id == nuevaLista[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == nuevaLista[newPos]
        })
        items.clear()
        items.addAll(nuevaLista)
        diff.dispatchUpdatesTo(this)
    }

    inner class ItemViewHolder(val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvCodigo.text = item.codigo
        holder.binding.tvDetalle.text =
            "Cantidad: ${item.cantidad}  •  ${item.tipoCodigo}  •  ${item.fechaHora}"
        holder.binding.btnEliminar.setOnClickListener { onEliminarClick(item) }
    }

    override fun getItemCount() = items.size
}
