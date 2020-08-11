package org.wordpress.android.fluxc.example.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.product_downloadable_file_list_item.view.*
import org.wordpress.android.fluxc.example.R
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
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.product_downloadable_file_list_item, parent, false)
        ) { deleteFile(it) }
    }

    private fun deleteFile(file: ProductFile) {
        filesList = filesList - file
    }

    override fun getItemCount(): Int = _filesList.size

    override fun onBindViewHolder(holder: ProductFileViewHolder, position: Int) {
        holder.bind(_filesList[position])
    }

    class ProductFileViewHolder(view: View, private val onDeleteClick: (ProductFile) -> Unit) : ViewHolder(view) {
        private val fileName = view.file_name
        private val fileUrl = view.file_url
        private val deleteButton = view.delete_button

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

