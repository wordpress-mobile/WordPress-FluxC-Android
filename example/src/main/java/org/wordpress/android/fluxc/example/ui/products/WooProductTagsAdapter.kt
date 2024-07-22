package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.fluxc.example.databinding.ProductCategoryListItemBinding
import org.wordpress.android.fluxc.example.ui.products.WooProductTagsAdapter.ProductTagViewHolder
import org.wordpress.android.fluxc.example.ui.products.WooUpdateProductFragment.ProductTag

class WooProductTagsAdapter(
    private val context: Context,
    private val clickListener: OnProductTagClickListener
) : RecyclerView.Adapter<ProductTagViewHolder>() {
    private val productTagList = ArrayList<ProductTagViewHolderModel>()

    interface OnProductTagClickListener {
        fun onProductTagClick(productTagViewHolderModel: ProductTagViewHolderModel)
    }

    @Suppress("DataClassShouldBeImmutable")
    data class ProductTagViewHolderModel(
        val tag: ProductTag,
        var isSelected: Boolean = false
    )

    override fun getItemCount() = productTagList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductTagViewHolder {
        return ProductTagViewHolder(
            ProductCategoryListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductTagViewHolder, position: Int) {
        val productTag = productTagList[position]

        holder.txtTagName.text = productTag.tag.name
        holder.checkBox.isChecked = productTag.isSelected

        holder.checkBox.setOnClickListener {
            handleTagClick(holder, productTag)
        }

        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
            handleTagClick(holder, productTag)
        }
    }

    private fun handleTagClick(
        holder: ProductTagViewHolder,
        productTag: ProductTagViewHolderModel
    ) {
        productTag.isSelected = holder.checkBox.isChecked
        clickListener.onProductTagClick(productTag)
    }

    fun setProductTags(productsTags: List<ProductTagViewHolderModel>) {
        if (productTagList.isEmpty()) {
            productTagList.addAll(productsTags)
            notifyDataSetChanged()
        } else {
            val diffResult = DiffUtil.calculateDiff(ProductItemDiffUtil(productTagList, productsTags))
            productTagList.clear()
            productTagList.addAll(productsTags)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    class ProductTagViewHolder(
        binding: ProductCategoryListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        val txtTagName: TextView = binding.categoryName
        val checkBox: CheckBox = binding.categorySelected
    }

    private class ProductItemDiffUtil(
        val items: List<ProductTagViewHolderModel>,
        val result: List<ProductTagViewHolderModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition].tag.name == result[newItemPosition].tag.name

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        fun isSameTag(left: ProductTag, right: ProductTag): Boolean {
            return left.name == right.name
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return isSameTag(oldItem.tag, newItem.tag)
        }
    }
}
