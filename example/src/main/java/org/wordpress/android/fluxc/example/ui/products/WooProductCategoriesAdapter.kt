package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.fluxc.example.databinding.ProductCategoryListItemBinding
import org.wordpress.android.fluxc.example.ui.products.WooProductCategoriesAdapter.ProductCategoryViewHolder
import org.wordpress.android.fluxc.example.ui.products.WooUpdateProductFragment.ProductCategory

class WooProductCategoriesAdapter(
    private val context: Context,
    private val clickListener: OnProductCategoryClickListener
) : RecyclerView.Adapter<ProductCategoryViewHolder>() {
    private val productCategoryList = ArrayList<ProductCategoryViewHolderModel>()

    interface OnProductCategoryClickListener {
        fun onProductCategoryClick(productCategoryViewHolderModel: ProductCategoryViewHolderModel)
    }

    @Suppress("DataClassShouldBeImmutable")
    data class ProductCategoryViewHolderModel(
        val category: ProductCategory,
        var isSelected: Boolean = false
    )

    override fun getItemCount() = productCategoryList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductCategoryViewHolder {
        return ProductCategoryViewHolder(
            ProductCategoryListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductCategoryViewHolder, position: Int) {
        val productCategory = productCategoryList[position]

        holder.txtCategoryName.text = productCategory.category.name
        holder.checkBox.isChecked = productCategory.isSelected

        holder.checkBox.setOnClickListener {
            handleCategoryClick(holder, productCategory)
        }

        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
            handleCategoryClick(holder, productCategory)
        }
    }

    private fun handleCategoryClick(
        holder: ProductCategoryViewHolder,
        productCategory: ProductCategoryViewHolderModel
    ) {
        productCategory.isSelected = holder.checkBox.isChecked
        clickListener.onProductCategoryClick(productCategory)
    }

    fun setProductCategories(productsCategories: List<ProductCategoryViewHolderModel>) {
        if (productCategoryList.isEmpty()) {
            productCategoryList.addAll(productsCategories)
            notifyDataSetChanged()
        } else {
            val diffResult = DiffUtil.calculateDiff(ProductItemDiffUtil(productCategoryList, productsCategories))
            productCategoryList.clear()
            productCategoryList.addAll(productsCategories)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    class ProductCategoryViewHolder(
        binding: ProductCategoryListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        val txtCategoryName: TextView = binding.categoryName
        val checkBox: CheckBox = binding.categorySelected
    }

    private class ProductItemDiffUtil(
        val items: List<ProductCategoryViewHolderModel>,
        val result: List<ProductCategoryViewHolderModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition].category.name == result[newItemPosition].category.name

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        fun isSameCategory(left: ProductCategory, right: ProductCategory): Boolean {
            return left.name == right.name
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return isSameCategory(oldItem.category, newItem.category)
        }
    }
}
