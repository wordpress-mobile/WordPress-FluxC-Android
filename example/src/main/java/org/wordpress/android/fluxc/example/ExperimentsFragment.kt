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
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.databinding.FragmentExperimentsBinding
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.store.ExperimentStore
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentExperimentsBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentExperimentsBinding.bind(view)) {
            platformSpinner.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item).apply {
                addAll(Platform.values().map { it.value })
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            platformSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    selectedPlatform = Platform.fromValue(parent.getItemAtPosition(pos) as String)
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit // Do nothing (ignore)
            }
            generateAnonIdButton.setOnClickListener { anonIdEditText.setText(UUID.randomUUID().toString()) }
            fetchAssignments.setOnClickListener {
                val platform = selectedPlatform ?: WORDPRESS_COM
                val experimentNames = experimentNamesEditText.text.split(',').map { it.trim() }
                val anonymousId = anonIdEditText.text.toString()
                prependToLog("Fetching assignments with: platform=$platform, experimentNames=$experimentNames, " +
                        "anonymousId=$anonymousId")
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = experimentStore.fetchAssignments(platform, experimentNames, anonymousId)
                    withContext(Dispatchers.Main) {
                        onAssignmentsFetched(result)
                    }
                }
            }
            getCachedAssignments.setOnClickListener {
                val assignments = experimentStore.getCachedAssignments()
                if (assignments == null) {
                    prependToLog("No cached assignments")
                } else {
                    prependToLog("Got ${assignments.variations.size} cached assignments")
                    handleAssignments(assignments)
                }
            }
            clearCachedAssignments.setOnClickListener {
                experimentStore.clearCachedAssignments()
                prependToLog("Cleared cached assignments")
            }
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
            prependToLog("${entry.key}: ${entry.value.name}")
        }
    }

    private fun prependToLog(s: String) = (activity as MainExampleActivity).prependToLog(s)
}
