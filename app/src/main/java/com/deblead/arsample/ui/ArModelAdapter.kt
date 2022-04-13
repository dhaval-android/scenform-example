package com.deblead.arsample.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.deblead.arsample.model.ArModel
import com.xplora.arsample.databinding.ItemModelsBinding

const val SELECT_MODEL_COLOR = Color.YELLOW
const val UNSELECT_MODEL_COLOR = Color.LTGRAY

class ArModelAdapter(
    private val models: List<ArModel>
) : RecyclerView.Adapter<ArModelAdapter.ModelViewHolder>() {

    var selectedModel = MutableLiveData<ArModel>()
    private var selectedModelIndex = 0

    inner class ModelViewHolder(val binding: ItemModelsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val binding = ItemModelsBinding.inflate(LayoutInflater.from(parent.context))
        return ModelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        if (selectedModelIndex == holder.layoutPosition) {
            holder.binding.cvMain.setCardBackgroundColor(SELECT_MODEL_COLOR)
            selectedModel.value = models[holder.layoutPosition]
        } else {
            holder.binding.cvMain.setCardBackgroundColor(UNSELECT_MODEL_COLOR)
        }
        holder.itemView.apply {
            holder.binding.tvTitle.text = models[position].title
            holder.binding.ivThumbnail.setImageResource(models[position].resourceId)
            setOnClickListener { selectModel(holder) }
        }
    }

    override fun getItemCount() = models.size

    private fun selectModel(holder: ModelViewHolder) {
        if (selectedModelIndex != holder.layoutPosition) {
            holder.binding.cvMain.setCardBackgroundColor(SELECT_MODEL_COLOR)
            notifyItemChanged(selectedModelIndex)
            selectedModelIndex = holder.layoutPosition
            selectedModel.value = models[holder.layoutPosition]

        }
    }
}