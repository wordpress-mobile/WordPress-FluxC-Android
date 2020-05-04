package org.wordpress.android.fluxc.example

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.product_category_list_item.view.*
import org.wordpress.android.fluxc.example.ProductCategoriesAdapter.ProductCategoryViewHolder
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.util.HtmlUtils

class ProductCategoriesAdapter(
    private val context: Context,
    private val clickListener: OnProductCategoryClickListener
) : RecyclerView.Adapter<ProductCategoryViewHolder>() {
    private val productCategoryList = ArrayList<ProductCategoryViewHolderModel>()

    interface OnProductCategoryClickListener {
        fun onProductCategoryClick(productCategoryViewHolderModel: ProductCategoryViewHolderModel)
    }

    data class ProductCategoryViewHolderModel(val category: WCProductCategoryModel, var isSelected: Boolean = false)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = productCategoryList[position].category.remoteCategoryId

    override fun getItemCount() = productCategoryList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductCategoryViewHolder {
        return ProductCategoryViewHolder(
                LayoutInflater.from(context)
                        .inflate(R.layout.product_category_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductCategoryViewHolder, position: Int) {
        val productCategory = productCategoryList[position]

        holder.txtCategoryName.text = if (productCategory.category.name.isEmpty()) {
            context.getString(R.string.untitled)
        } else {
            HtmlUtils.fastStripHtml(productCategory.category.name)
        }

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
            productCategoryList.clear()
            productCategoryList.addAll(productsCategories)
            notifyDataSetChanged()
        } else {
            val diffResult = DiffUtil.calculateDiff(ProductItemDiffUtil(productCategoryList, productsCategories))
            productCategoryList.clear()
            productCategoryList.addAll(productsCategories)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    class ProductCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCategoryName: TextView = view.categoryName
        val checkBox: CheckBox = view.categorySelected
    }

    private class ProductItemDiffUtil(
        val items: List<ProductCategoryViewHolderModel>,
        val result: List<ProductCategoryViewHolderModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition].category.remoteCategoryId == result[newItemPosition].category.remoteCategoryId

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        fun isSameCategory(left: WCProductCategoryModel, right: WCProductCategoryModel): Boolean {
            return left.remoteCategoryId == right.remoteCategoryId &&
                    left.name == right.name &&
                    left.slug == right.slug &&
                    left.parent == right.parent
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return isSameCategory(oldItem.category, newItem.category)
        }
    }
}
