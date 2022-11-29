package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_plans.*
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.PlansStore
import javax.inject.Inject

class PlansFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var store: PlansStore

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_plans, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_plans.setOnClickListener {
            lifecycleScope.launch {
                val plansResult = store.fetchPlans()
                when {
                    plansResult.isError -> {
                        prependToLog("Error fetching plans: ${plansResult.error}")
                    }
                    else -> {
                        val plans = plansResult.plans
                            ?.joinToString(separator = "\n") {
                                "${it.productName} (${it.billPeriodLabel}): ${it.price}"
                            }
                        prependToLog("Plans:\n$plans")
                    }
                }
            }
        }
    }
}
