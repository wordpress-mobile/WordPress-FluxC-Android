package org.wordpress.android.fluxc.example.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.fluxc.example.databinding.ProductDownloadableFileListItemBinding
import org.wordpress.android.fluxc.example.ui.products.WooProductDownloadsAdapter.ProductFileViewHolder
import org.wordpress.android.fluxc.example.ui.products.WooProductDownloadsFragment.ProductFile
import org.wordpress.android.fluxc.example.utils.onTextChanged

class WooProductDownloadsAdapter : RecyclerView.Adapter<ProductFileViewHolder>() {
    private val _filesList = mutableListOf<ProductFile>()
    var filesList: List<ProductFile>
        get() = _filesList
        set(value) {
            val diffResult = DiffUtil.calculateDiff(ProductFileItemDiffUtil(_filesList, value))
            _filesList.clear()
            _filesList.addAll(value)
            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductFileViewHolder {
        return ProductFileViewHolder(
            ProductDownloadableFileListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ) { deleteFile(it) }
    }

    private fun deleteFile(file: ProductFile) {
        filesList = filesList - file
    }

    override fun getItemCount(): Int = _filesList.size

    override fun onBindViewHolder(holder: ProductFileViewHolder, position: Int) {
        holder.bind(_filesList[position])
    }

    class ProductFileViewHolder(
        binding: ProductDownloadableFileListItemBinding,
        private val onDeleteClick: (ProductFile) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val fileName = binding.fileName
        private val fileUrl = binding.fileUrl
        private val deleteButton = binding.deleteButton

        fun bind(file: ProductFile) {
            fileName.setText(file.name)
            fileUrl.setText(file.url)

            fileName.onTextChanged { file.name = it }
            fileUrl.onTextChanged { file.url = it }
            deleteButton.setOnClickListener { onDeleteClick.invoke(file) }
        }
    }

    private class ProductFileItemDiffUtil(
        val oldList: List<ProductFile>,
        val newList: List<ProductFile>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition] == newList[newItemPosition]

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                areItemsTheSame(oldItemPosition, newItemPosition)
    }
}
