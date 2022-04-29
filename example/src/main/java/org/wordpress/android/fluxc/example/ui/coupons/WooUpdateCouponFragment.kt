package org.wordpress.android.fluxc.example.ui.coupons

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_update_coupon.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.ARG_LIST_SELECTED_ITEM
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.ARG_LIST_SELECTED_ITEMS
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.LIST_MULTI_SELECTOR_REQUEST_CODE
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.LIST_SELECTOR_REQUEST_CODE
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.ListItem
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.model.coupon.UpdateCouponRequest
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import org.wordpress.android.fluxc.persistence.entity.CouponEntity.DiscountType
import org.wordpress.android.fluxc.store.CouponStore
import org.wordpress.android.fluxc.store.ProductCategoryStore
import org.wordpress.android.fluxc.store.ProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Calendar
import javax.inject.Inject

class WooUpdateCouponFragment : Fragment() {
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var couponStore: CouponStore
    @Inject internal lateinit var productStore: ProductStore
    @Inject internal lateinit var productCategoryStore: ProductCategoryStore

    private var siteId: Long = -1
    private var couponId: Long? = null
    private var isAddNewCoupon: Boolean = false

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val ARG_SELECTED_SITE_ID = "ARG_SELECTED_SITE_ID"
        const val ARG_SELECTED_COUPON_ID = "ARG_SELECTED_COUPON_ID"
        const val LIST_RESULT_CODE_EXCL_PRODUCTS = 101
        const val LIST_RESULT_CODE_PRODUCTS = 102
        const val LIST_RESULT_CODE_EXCL_CATEGORIES = 103
        const val LIST_RESULT_CODE_CATEGORIES = 104
        const val LIST_RESULT_CODE_DISCOUNT_TYPE = 105
        const val IS_ADD_NEW_COUPON = "IS_ADD_NEW_COUPON"

        fun newInstance(selectedSiteId: Long, isAddNewCoupon: Boolean = false): WooUpdateCouponFragment {
            val fragment = WooUpdateCouponFragment()
            val args = Bundle()
            args.putLong(ARG_SELECTED_SITE_ID, selectedSiteId)
            args.putBoolean(IS_ADD_NEW_COUPON, isAddNewCoupon)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            siteId = it.getLong(ARG_SELECTED_SITE_ID, 0)
            isAddNewCoupon = it.getBoolean(IS_ADD_NEW_COUPON, false)
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_update_coupon, container, false)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(ARG_SELECTED_SITE_ID, siteId)
        couponId?.let { outState.putLong(ARG_SELECTED_COUPON_ID, it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isAddNewCoupon) {
            enter_coupon_id.setOnClickListener {
                showSingleLineDialog(activity, "Enter the ID of coupon to fetch:") { editText ->
                    couponId = editText.text.toString().toLongOrNull()
                    couponId?.let { id ->
                        updateSelectedCouponId(id)
                    } ?: prependToLog("No valid coupon ID defined...doing nothing")
                }
            }
        } else {
            enableCouponDependentButtons()

            entered_coupon_id.visibility = View.GONE
            enter_coupon_id.visibility = View.GONE
        }

        product_ids.isEnabled = false
        excluded_product_ids.isEnabled = false
        category_ids.isEnabled = false
        excluded_category_ids.isEnabled = false

        discount_type.setOnClickListener {
            showListSelectorDialog(
                listOf(DiscountType.Percent, DiscountType.FixedCart, DiscountType.FixedProduct).map { it.value },
                LIST_RESULT_CODE_DISCOUNT_TYPE, discount_type.text.toString()
            )
        }

        select_expiry_date.setOnClickListener {
            showDatePickerDialog(
                expiry_date.getText()
            ) { _, year, month, dayOfMonth ->
                expiry_date.setText(DateUtils.getFormattedDateString(year, month, dayOfMonth))
            }
        }

        coupon_update.setOnClickListener {
            getWCSite()?.let { site ->
                // update categories only if new categories has been selected
                val products = product_ids.getText().toIdsList()
                val excludedProducts = excluded_product_ids.getText().toIdsList()
                val excludedCategories = excluded_category_ids.getText().toIdsList()
                val categories = category_ids.getText().toIdsList()
                val emails = restricted_emails.getText()
                    .replace(" ", "")
                    .split(",")

                val request = UpdateCouponRequest(
                    code = coupon_code.getText(),
                    amount = coupon_amount.getText(),
                    discountType = discount_type.text.toString(),
                    description = coupon_description.getText(),
                    expiryDate = expiry_date.getText(),
                    minimumAmount = minimum_amount.getText(),
                    maximumAmount = maximum_amount.getText(),
                    productIds = products,
                    excludedProductIds = excludedProducts,
                    productCategoryIds = categories,
                    excludedProductCategoryIds = excludedCategories,
                    isShippingFree = shipping_free.isChecked,
                    isForIndividualUse = individual_use.isChecked,
                    areSaleItemsExcluded = sale_item_excluded.isChecked,
                    usageLimit = usage_limit.getText().toIntOrNull(),
                    usageLimitPerUser = usage_limit_per_user.getText().toIntOrNull(),
                    limitUsageToXItems = limit_usage_to_x_items.getText().toIntOrNull(),
                    restrictedEmails = emails
                )

                coroutineScope.launch {
                    when {
                        couponId != null -> {
                            val result = couponStore.updateCoupon(couponId!!, site, request)
                            if (result.isError) {
                                prependToLog("Coupon update failed:\n${result.error.message}")
                            } else {
                                prependToLog("Coupon update successful")
                            }
                        }
                        else -> {
                            prependToLog("No valid couponId defined...doing nothing")
                        }
                    }
                }
            } ?: prependToLog("No site found...doing nothing")
        }

        select_product_ids.setOnClickListener {
            getWCSite()?.let {
                coroutineScope.launch {
                    val products = productStore.getProducts(siteId)
                    val selectedIds = product_ids.getText().toIdsList()
                    if (products.isNotEmpty()) {
                        showMultiSelectorDialog(
                            itemNames = products.map { it.name ?: "[${it.id}]" },
                            itemIds = products.map { it.id },
                            selectedIds = selectedIds,
                            resultCode = LIST_RESULT_CODE_PRODUCTS
                        )
                    } else {
                        prependToLog("No products found: please fetch some first")
                    }
                }
            }
        }

        select_excluded_product_ids.setOnClickListener {
            getWCSite()?.let {
                coroutineScope.launch {
                    val products = productStore.getProducts(siteId)
                    val selectedIds = excluded_product_ids.getText().toIdsList()
                    if (products.isNotEmpty()) {
                        showMultiSelectorDialog(
                            itemNames = products.map { it.name ?: "[${it.id}]" },
                            itemIds = products.map { it.id },
                            selectedIds = selectedIds,
                            resultCode = LIST_RESULT_CODE_EXCL_PRODUCTS
                        )
                    } else {
                        prependToLog("No products found: please fetch some first")
                    }
                }
            }
        }

        select_category_ids.setOnClickListener {
            getWCSite()?.let {
                coroutineScope.launch {
                    val categories = productCategoryStore.getCategories(siteId)
                    val selectedIds = category_ids.getText().toIdsList()
                    if (categories.isNotEmpty()) {
                        showMultiSelectorDialog(
                            itemNames = categories.map { it.name ?: "[${it.id}]" },
                            itemIds = categories.map { it.id },
                            selectedIds = selectedIds,
                            resultCode = LIST_RESULT_CODE_CATEGORIES
                        )
                    } else {
                        prependToLog("No categories found: please fetch some first")
                    }
                }
            }
        }

        select_excluded_category_ids.setOnClickListener {
            getWCSite()?.let {
                coroutineScope.launch {
                    val categories = productCategoryStore.getCategories(siteId)
                    val selectedIds = excluded_category_ids.getText().toIdsList()
                    if (categories.isNotEmpty()) {
                        showMultiSelectorDialog(
                            itemNames = categories.map { it.name ?: "[${it.id}]" },
                            itemIds = categories.map { it.id },
                            selectedIds = selectedIds,
                            resultCode = LIST_RESULT_CODE_EXCL_CATEGORIES
                        )
                    } else {
                        prependToLog("No categories found: please fetch some first")
                    }
                }
            }
        }

        savedInstanceState?.let { bundle ->
            couponId = bundle.getLong(ARG_SELECTED_COUPON_ID)
            siteId = bundle.getLong(ARG_SELECTED_SITE_ID)
        }
        couponId?.let { updateSelectedCouponId(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LIST_SELECTOR_REQUEST_CODE) {
            val selectedItem = data?.getStringExtra(ARG_LIST_SELECTED_ITEM)
            when (resultCode) {
                LIST_RESULT_CODE_DISCOUNT_TYPE -> {
                    selectedItem?.let {
                        discount_type.text = it
                    }
                }
            }
        } else if (requestCode == LIST_MULTI_SELECTOR_REQUEST_CODE) {
            val selectedItems = data?.getLongArrayExtra(ARG_LIST_SELECTED_ITEMS)
            val view = when (resultCode) {
                LIST_RESULT_CODE_PRODUCTS -> product_ids
                LIST_RESULT_CODE_EXCL_PRODUCTS -> excluded_product_ids
                LIST_RESULT_CODE_CATEGORIES -> category_ids
                LIST_RESULT_CODE_EXCL_CATEGORIES -> excluded_category_ids
                else -> null
            }
            view?.setText(selectedItems?.joinToString { it.toString() } ?: "")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSelectedCouponId(couponId: Long) {
        getWCSite()?.let { siteModel ->
            enableCouponDependentButtons()
            enter_coupon_id.text = couponId.toString()

            coroutineScope.launch {
                couponStore.fetchCoupon(siteModel, couponId)
                couponStore.getCoupon(siteModel, couponId)?.let {
                    updateCouponProperties(it)
                }
            }
        } ?: prependToLog("No valid site found...doing nothing")
    }

    private fun String.toIdsList() =
        replace(" ", "")
        .split(",")
        .mapNotNull { it.toLongOrNull() }

    private fun updateCouponProperties(couponModel: CouponDataModel) {
        coupon_code.setText(couponModel.coupon.code ?: "")
        coupon_amount.setText(couponModel.coupon.amount.toString())
        discount_type.text = couponModel.coupon.discountType.toString()
        coupon_description.setText(couponModel.coupon.description ?: "")
        expiry_date.setText(couponModel.coupon.dateExpires?.split('T')?.get(0) ?: "")
        minimum_amount.setText(couponModel.coupon.minimumAmount.toString())
        maximum_amount.setText(couponModel.coupon.maximumAmount.toString())
        product_ids.setText(couponModel.products.joinToString { it.id.toString() })
        excluded_product_ids.setText(couponModel.excludedProducts.joinToString { it.id.toString() })
        category_ids.setText(couponModel.categories.joinToString { it.id.toString() })
        excluded_category_ids.setText(couponModel.excludedCategories.joinToString { it.id.toString() })
        shipping_free.isChecked = couponModel.coupon.isShippingFree ?: false
        individual_use.isChecked = couponModel.coupon.isForIndividualUse ?: false
        sale_item_excluded.isChecked = couponModel.coupon.areSaleItemsExcluded ?: false
        usage_limit.setText(couponModel.coupon.usageLimit?.toString() ?: "")
        usage_limit_per_user.setText(couponModel.coupon.usageLimitPerUser?.toString() ?: "")
        limit_usage_to_x_items.setText(couponModel.coupon.limitUsageToXItems?.toString() ?: "")
        restricted_emails.setText(couponModel.restrictedEmails.joinToString { it.email })
    }

    private fun showListSelectorDialog(listItems: List<String>, resultCode: Int, selectedItem: String?) {
        fragmentManager?.let { fm ->
            val dialog = ListSelectorDialog.newInstance(
                fragment = this,
                listItems = listItems,
                resultCode = resultCode,
                selectedListItem = selectedItem
            )
            dialog.show(fm, "ListSelectorDialog")
        }
    }

    private fun showMultiSelectorDialog(
        itemNames: List<String>,
        itemIds: List<Long>,
        selectedIds: List<Long> = emptyList(),
        resultCode: Int
    ) {
        val items = itemNames.mapIndexed { i, item ->
            ListItem(itemIds[i], item, selectedIds.contains(itemIds[i]))
        }
        fragmentManager?.let { fm ->
            val dialog = ListSelectorDialog.newInstance(
                this, items, resultCode
            )
            dialog.show(fm, "ListSelectorDialog")
        }
    }

    private fun showDatePickerDialog(dateString: String?, listener: OnDateSetListener) {
        val date = if (dateString.isNullOrEmpty()) {
            DateUtils.getCurrentDateString()
        } else dateString
        val calendar = DateUtils.getCalendarInstance(date)
        DatePickerDialog(
            requireActivity(), listener, calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE)
        ).show()
    }

    private fun getWCSite() = wooCommerceStore.getWooCommerceSites()
        .firstOrNull { it.siteId == siteId }

    private fun enableCouponDependentButtons() {
        for (i in 0 until couponContainer.childCount) {
            val child = couponContainer.getChildAt(i)
            if (child is Button || child is EditText) {
                child.isEnabled = true
            }
        }
    }
}
