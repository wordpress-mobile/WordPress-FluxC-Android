package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_products.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_REVIEWS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_VARIATIONS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW
import org.wordpress.android.fluxc.action.WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooProductsFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedPos: Int = -1
    private var selectedSite: SiteModel? = null
    private var pendingFetchSingleProductRemoteId: Long? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_products, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stats_select_site.setOnClickListener {
            showSiteSelectorDialog(selectedPos, object : StoreSelectorDialog.Listener {
                override fun onSiteSelected(site: SiteModel, pos: Int) {
                    selectedSite = site
                    selectedPos = pos
                    toggleSiteDependentButtons(true)
                    stats_select_site.text = site.name ?: site.displayName
                }
            })
        }

        fetch_single_product.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(activity, "Enter the remoteProductId of product to fetch:") { editText ->
                    pendingFetchSingleProductRemoteId = editText.text.toString().toLongOrNull()
                    pendingFetchSingleProductRemoteId?.let { id ->
                        prependToLog("Submitting request to fetch product by remoteProductID $id")
                        val payload = FetchSingleProductPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductAction(payload))
                    } ?: prependToLog("No valid remoteOrderId defined...doing nothing")
                }
            }
        }

        fetch_product_variations.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteProductId of product to fetch variations:"
                ) { editText ->
                    val remoteProductId = editText.text.toString().toLongOrNull()
                    remoteProductId?.let { id ->
                        prependToLog("Submitting request to fetch product variations by remoteProductID $id")
                        val payload = FetchProductVariationsPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchProductVariationsAction(payload))
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }

        fetch_reviews_for_product.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteProductId of product to fetch reviews:"
                ) { editText ->
                    val remoteProductId = editText.text.toString().toLongOrNull()
                    remoteProductId?.let { id ->
                        prependToLog("Submitting request to fetch product reviews for remoteProductID $id")
                        val payload = FetchProductReviewsPayload(site, productIds = listOf(remoteProductId))
                        dispatcher.dispatch(WCProductActionBuilder.newFetchProductReviewsAction(payload))
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
            }
        }

        fetch_all_reviews.setOnClickListener {
            selectedSite?.let { site ->
                prependToLog("Submitting request to fetch product reviews for site ${site.id}")
                val payload = FetchProductReviewsPayload(site)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductReviewsAction(payload))
            }
        }

        fetch_review_by_id.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteReviewId of the review to fetch:"
                ) { editText ->
                    val reviewId = editText.text.toString().toLongOrNull()
                    reviewId?.let { id ->
                        prependToLog("Submitting request to fetch product review for ID $id")
                        val payload = FetchSingleProductReviewPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductReviewAction(payload))
                    } ?: prependToLog("No valid remoteReviewId defined...doing nothing")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        selectedSite?.let { site ->
            when (event.causeOfChange) {
                FETCH_SINGLE_PRODUCT -> {
                    pendingFetchSingleProductRemoteId?.let { remoteId ->
                        pendingFetchSingleProductRemoteId = null
                        val product = wcProductStore.getProductByRemoteId(site, remoteId)
                        product?.let {
                            prependToLog("Single product fetched successfully! ${it.name}")
                        } ?: prependToLog("WARNING: Fetched product not found in the local database!")
                    }
                }
                FETCH_PRODUCT_VARIATIONS -> {
                    prependToLog("Fetched ${event.rowsAffected} product variations")
                }
                FETCH_PRODUCT_REVIEWS -> {
                    prependToLog("Fetched ${event.rowsAffected} product reviews")
                }
                FETCH_SINGLE_PRODUCT_REVIEW -> {
                    prependToLog("Fetched ${event.rowsAffected} single product review")
                }
                UPDATE_PRODUCT_REVIEW_STATUS -> {
                    prependToLog("${event.rowsAffected} product reviews updated")
                }
                else -> prependToLog("Product store was updated from a " + event.causeOfChange)
            }
        }
    }

    private fun showSiteSelectorDialog(selectedPos: Int, listener: StoreSelectorDialog.Listener) {
        fragmentManager?.let { fm ->
            val dialog = StoreSelectorDialog.newInstance(listener, selectedPos)
            dialog.show(fm, "StoreSelectorDialog")
        }
    }

    private fun toggleSiteDependentButtons(enabled: Boolean) {
        for (i in 0 until buttonContainer.childCount) {
            val child = buttonContainer.getChildAt(i)
            if (child is Button) {
                child.isEnabled = enabled
            }
        }
    }
}
