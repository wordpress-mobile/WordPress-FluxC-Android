package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_experiments.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.model.experiments.Variation
import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.fluxc.model.experiments.Variation.Treatment
import org.wordpress.android.fluxc.store.ExperimentStore
import org.wordpress.android.fluxc.store.ExperimentStore.FetchAssignmentsPayload
import org.wordpress.android.fluxc.store.ExperimentStore.OnAssignmentsFetched
import org.wordpress.android.fluxc.store.ExperimentStore.Platform
import org.wordpress.android.fluxc.store.ExperimentStore.Platform.WORDPRESS_COM
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import java.util.UUID
import javax.inject.Inject

class ExperimentsFragment : Fragment() {
    @Inject internal lateinit var experimentStore: ExperimentStore

    private var selectedPlatform: Platform? = WORDPRESS_COM

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_experiments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        platform_spinner.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item).apply {
            addAll(Platform.values().map { it.value })
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        platform_spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedPlatform = Platform.fromValue(parent.getItemAtPosition(pos) as String)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        generate_anon_id_button.setOnClickListener { anon_id_edit_text.setText(UUID.randomUUID().toString()) }
        fetch_assignments.setOnClickListener {
            val platform = selectedPlatform ?: WORDPRESS_COM
            val anonymousId = anon_id_edit_text.text.toString()
            val payload = FetchAssignmentsPayload(platform, anonymousId)
            prependToLog("Fetching assignments with payload: $payload")
            GlobalScope.launch(Dispatchers.Default) {
                val result = experimentStore.fetchAssignments(payload)
                withContext(Dispatchers.Main) {
                    onAssignmentsFetched(result)
                }
            }
        }
        get_cached_assignments.setOnClickListener {
            val assignments = experimentStore.getCachedAssignments()
            if (assignments == null) {
                prependToLog("No cached assignments")
            } else {
                prependToLog("Got ${assignments.variations.size} cached assignments")
                handleAssignments(assignments)
            }
        }
        clear_cached_assignments.setOnClickListener {
            experimentStore.clearCachedAssignments()
            prependToLog("Cleared cached assignments")
        }
    }

    private fun onAssignmentsFetched(event: OnAssignmentsFetched) {
        AppLog.i(API, "OnAssignmentsFetched: $event")
        if (event.isError) {
            prependToLog("Error: ${event.error}")
        } else {
            prependToLog("Success: fetched ${event.assignments.variations.size} assignments")
            handleAssignments(event.assignments)
        }
    }

    private fun handleAssignments(assignments: Assignments) {
        assignments.variations.forEach { entry ->
            prependToLog("${entry.key}: ${getVariationString(entry.value)}")
        }
    }

    private fun getVariationString(variation: Variation) = when (variation) {
        is Control -> "control"
        is Treatment -> variation.name
    }

    private fun prependToLog(s: String) = (activity as MainExampleActivity).prependToLog(s)
}
