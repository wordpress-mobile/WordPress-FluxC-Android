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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.databinding.FragmentWooUpdateCouponBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.ARG_LIST_SELECTED_ITEM
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.ARG_LIST_SELECTED_ITEMS
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.LIST_MULTI_SELECTOR_REQUEST_CODE
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.Companion.LIST_SELECTOR_REQUEST_CODE
import org.wordpress.android.fluxc.example.ui.ListSelectorDialog.ListItem
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.model.coupon.UpdateCouponRequest
import org.wordpress.android.fluxc.persistence.entity.CouponEntity.DiscountType
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails
import org.wordpress.android.fluxc.store.CouponStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Calendar
import javax.inject.Inject

class WooUpdateCouponFragment : Fragment() {
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var couponStore: CouponStore
    @Inject internal lateinit var productStore: WCProductStore

    private var siteId: Long = -1
    private var couponId: Long? = null
    private var isAddNewCoupon: Boolean = false

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var binding: FragmentWooUpdateCouponBinding? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWooUpdateCouponBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(ARG_SELECTED_SITE_ID, siteId)
        couponId?.let { outState.putLong(ARG_SELECTED_COUPON_ID, it) }
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isAddNewCoupon) {
            binding?.enterCouponId?.setOnClickListener {
                showSingleLineDialog(
                    activity = activity,
                    message = "Enter the ID of coupon to fetch:",
                    isNumeric = true
                ) { editText ->
                    couponId = editText.text.toString().toLongOrNull()
                    couponId?.let { id ->
                        binding?.updateSelectedCouponId(id)
                    } ?: prependToLog("No valid coupon ID defined...doing nothing")
                }
            }
        } else {
            binding?.enableCouponDependentButtons()

            binding?.enteredCouponId?.visibility = View.GONE
            binding?.enterCouponId?.visibility = View.GONE
        }

        binding?.productIds?.isEnabled = false
        binding?.excludedProductIds?.isEnabled = false
        binding?.categoryIds?.isEnabled = false
        binding?.excludedCategoryIds?.isEnabled = false

        binding?.discountType?.setOnClickListener {
            showListSelectorDialog(
                listOf(DiscountType.Percent, DiscountType.FixedCart, DiscountType.FixedProduct).map { it.value },
                LIST_RESULT_CODE_DISCOUNT_TYPE, binding?.discountType?.text.toString()
            )
        }

        binding?.selectExpiryDate?.setOnClickListener {
            showDatePickerDialog(
                binding?.expiryDate?.getText()
            ) { _, year, month, dayOfMonth ->
                binding?.expiryDate?.setText(DateUtils.getFormattedDateString(year, month, dayOfMonth))
            }
        }

        binding?.couponUpdate?.setOnClickListener {
            getWCSite()?.let { site ->
                // update categories only if new categories has been selected
                val products = binding?.productIds?.getText()?.toIdsList()
                val excludedProducts = binding?.excludedProductIds?.getText()?.toIdsList()
                val excludedCategories = binding?.excludedCategoryIds?.getText()?.toIdsList()
                val categories = binding?.categoryIds?.getText()?.toIdsList()
                val emails = binding?.restrictedEmails?.getText()
                    ?.replace(" ", "")
                    ?.split(",")

                val request = UpdateCouponRequest(
                    code = binding?.couponCode?.getText(),
                    amount = binding?.couponAmount?.getText(),
                    discountType = binding?.discountType?.text.toString(),
                    description = binding?.couponDescription?.getText(),
                    expiryDate = binding?.expiryDate?.getText(),
                    minimumAmount = binding?.minimumAmount?.getText(),
                    maximumAmount = binding?.maximumAmount?.getText(),
                    productIds = products,
                    excludedProductIds = excludedProducts,
                    productCategoryIds = categories,
                    excludedProductCategoryIds = excludedCategories,
                    isShippingFree = binding?.shippingFree?.isChecked,
                    isForIndividualUse = binding?.individualUse?.isChecked,
                    areSaleItemsExcluded = binding?.saleItemExcluded?.isChecked,
                    usageLimit = binding?.usageLimit?.getText()?.toIntOrNull(),
                    usageLimitPerUser = binding?.usageLimitPerUser?.getText()?.toIntOrNull(),
                    limitUsageToXItems = binding?.limitUsageToXItems?.getText()?.toIntOrNull(),
                    restrictedEmails = emails
                )

                coroutineScope.launch {
                    when {
                        isAddNewCoupon -> {
                            val result = couponStore.createCoupon(site, request)
                            if (result.isError) {
                                prependToLog("Coupon creation failed:\n${result.error.message}")
                            } else {
                                prependToLog("Coupon creation successful")
                            }
                        }
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

        binding?.selectProductIds?.setOnClickListener {
            getWCSite()?.let {
                coroutineScope.launch {
                    val products = productStore.getProductsForSite(it)
                    val selectedIds = binding?.productIds?.getText()?.toIdsList()
                    if (products.isNotEmpty()) {
                        showMultiSelectorDialog(
                            itemNames = products.map { it.name },
                            itemIds = products.map { it.remoteProductId },
                            selectedIds = selectedIds ?: emptyList(),
                            resultCode = LIST_RESULT_CODE_PRODUCTS
                        )
                    } else {
                        prependToLog("No products found: please fetch some first")
                    }
                }
            }
        }

        binding?.selectExcludedProductIds?.setOnClickListener {
            getWCSite()?.let {
                coroutineScope.launch {
                    val products = productStore.getProductsForSite(it)
                    val selectedIds = binding?.excludedProductIds?.getText()?.toIdsList()
                    if (products.isNotEmpty()) {
                        showMultiSelectorDialog(
                            itemNames = products.map { it.name },
                            itemIds = products.map { it.remoteProductId },
                            selectedIds = selectedIds ?: emptyList(),
                            resultCode = LIST_RESULT_CODE_EXCL_PRODUCTS
                        )
                    } else {
                        prependToLog("No products found: please fetch some first")
                    }
                }
            }
        }

        binding?.selectCategoryIds?.setOnClickListener {
            getWCSite()?.let {
                coroutineScope.launch {
                    val categories = productStore.getProductCategoriesForSite(it)
                    val selectedIds = binding?.categoryIds?.getText()?.toIdsList()
                    if (categories.isNotEmpty()) {
                        showMultiSelectorDialog(
                            itemNames = categories.map { it.name },
                            itemIds = categories.map { it.remoteCategoryId },
                            selectedIds = selectedIds ?: emptyList(),
                            resultCode = LIST_RESULT_CODE_CATEGORIES
                        )
                    } else {
                        prependToLog("No categories found: please fetch some first")
                    }
                }
            }
        }

        binding?.selectExcludedCategoryIds?.setOnClickListener {
            getWCSite()?.let {
                coroutineScope.launch {
                    val categories = productStore.getProductCategoriesForSite(it)
                    val selectedIds = binding?.excludedCategoryIds?.getText()?.toIdsList()
                    if (categories.isNotEmpty()) {
                        showMultiSelectorDialog(
                            itemNames = categories.map { it.name },
                            itemIds = categories.map { it.remoteCategoryId },
                            selectedIds = selectedIds ?: emptyList(),
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
        couponId?.let { binding?.updateSelectedCouponId(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LIST_SELECTOR_REQUEST_CODE) {
            val selectedItem = data?.getStringExtra(ARG_LIST_SELECTED_ITEM)
            when (resultCode) {
                LIST_RESULT_CODE_DISCOUNT_TYPE -> {
                    selectedItem?.let {
                        binding?.discountType?.text = it
                    }
                }
            }
        } else if (requestCode == LIST_MULTI_SELECTOR_REQUEST_CODE) {
            val selectedItems = data?.getLongArrayExtra(ARG_LIST_SELECTED_ITEMS)
            val view = when (resultCode) {
                LIST_RESULT_CODE_PRODUCTS -> binding?.productIds
                LIST_RESULT_CODE_EXCL_PRODUCTS -> binding?.excludedProductIds
                LIST_RESULT_CODE_CATEGORIES -> binding?.categoryIds
                LIST_RESULT_CODE_EXCL_CATEGORIES -> binding?.excludedCategoryIds
                else -> null
            }
            view?.setText(selectedItems?.asIterable().toText())
        }
    }

    @SuppressLint("SetTextI18n")
    private fun FragmentWooUpdateCouponBinding.updateSelectedCouponId(couponId: Long) {
        getWCSite()?.let { siteModel ->
            enableCouponDependentButtons()
            enterCouponId.text = couponId.toString()

            coroutineScope.launch {
                couponStore.fetchCoupon(siteModel, couponId)
                couponStore.getCoupon(siteModel, couponId)?.let {
                    updateCouponProperties(it)
                }
            }
        } ?: prependToLog("No valid site found...doing nothing")
    }

    private fun String.toIdsList() = replace(" ", "")
        .split(",")
        .mapNotNull { it.toLongOrNull() }

    private fun Iterable<Long>?.toText() = this?.joinToString { it.toString() } ?: ""

    private fun FragmentWooUpdateCouponBinding.updateCouponProperties(couponModel: CouponWithEmails) {
        couponCode.setText(couponModel.coupon.code ?: "")
        couponAmount.setText(couponModel.coupon.amount.toString())
        discountType.text = couponModel.coupon.discountType.toString()
        couponDescription.setText(couponModel.coupon.description ?: "")
        expiryDate.setText(couponModel.coupon.dateExpires?.split('T')?.get(0) ?: "")
        minimumAmount.setText(couponModel.coupon.minimumAmount.toString())
        maximumAmount.setText(couponModel.coupon.maximumAmount.toString())
        productIds.setText(couponModel.coupon.includedProductIds.toText())
        excludedProductIds.setText(couponModel.coupon.excludedProductIds.toText())
        categoryIds.setText(couponModel.coupon.includedCategoryIds.toText())
        excludedCategoryIds.setText(couponModel.coupon.excludedCategoryIds.toText())
        shippingFree.isChecked = couponModel.coupon.isShippingFree ?: false
        individualUse.isChecked = couponModel.coupon.isForIndividualUse ?: false
        saleItemExcluded.isChecked = couponModel.coupon.areSaleItemsExcluded ?: false
        usageLimit.setText(couponModel.coupon.usageLimit?.toString() ?: "")
        usageLimitPerUser.setText(couponModel.coupon.usageLimitPerUser?.toString() ?: "")
        limitUsageToXItems.setText(couponModel.coupon.limitUsageToXItems?.toString() ?: "")
        restrictedEmails.setText(couponModel.restrictedEmails.joinToString { it.email })
    }

    private fun showListSelectorDialog(listItems: List<String>, resultCode: Int, selectedItem: String?) {
        val dialog = ListSelectorDialog.newInstance(
            fragment = this,
            listItems = listItems,
            resultCode = resultCode,
            selectedListItem = selectedItem
        )
        dialog.show(parentFragmentManager, "ListSelectorDialog")
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
        val dialog = ListSelectorDialog.newInstance(
            this, items, resultCode
        )
        dialog.show(parentFragmentManager, "ListSelectorDialog")
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

    private fun FragmentWooUpdateCouponBinding.enableCouponDependentButtons() {
        for (i in 0 until couponContainer.childCount) {
            val child = couponContainer.getChildAt(i)
            if (child is Button || child is EditText) {
                child.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
