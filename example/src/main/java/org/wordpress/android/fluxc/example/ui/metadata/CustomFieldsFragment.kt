package org.wordpress.android.fluxc.example.ui.metadata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import dagger.android.support.DaggerFragment
import org.wordpress.android.fluxc.example.ui.metadata.CustomFieldsViewModel.CustomFieldsState
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue
import org.wordpress.android.fluxc.store.MetaDataStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class CustomFieldsFragment : DaggerFragment() {
    companion object {
        private const val ARG_PARENT_ITEM_TYPE = "parentItemType"
        private const val ARG_PARENT_ITEM_ID = "parentItemId"
        private const val ARG_SITE_ID = "siteId"

        fun newInstance(
            siteId: LocalId,
            parentItemId: Long,
            parentItemType: MetaDataParentItemType
        ) = CustomFieldsFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_SITE_ID, siteId.value)
                putLong(ARG_PARENT_ITEM_ID, parentItemId)
                putString(ARG_PARENT_ITEM_TYPE, parentItemType.name)
            }
        }
    }

    @Inject lateinit var metaDataStore: MetaDataStore

    @Inject lateinit var siteStore: SiteStore

    private val viewModel by lazy {
        CustomFieldsViewModel(
            coroutineScope = lifecycleScope,
            site = siteStore.getSiteByLocalId(requireArguments().getInt(ARG_SITE_ID))!!,
            parentItemId = requireArguments().getLong(ARG_PARENT_ITEM_ID),
            parentItemType = MetaDataParentItemType.valueOf(
                requireArguments().getString(ARG_PARENT_ITEM_TYPE)!!
            ),
            metaDataStore = metaDataStore
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                MaterialTheme {
                    CustomFieldsScreen(viewModel.state)
                }
            }
        }
    }
}

@Composable
private fun CustomFieldsScreen(state: CustomFieldsState) {
    when (state) {
        CustomFieldsState.Loading -> CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize()
        )

        is CustomFieldsState.Error -> ErrorView(state)
        is CustomFieldsState.Loaded -> ContentView(state)
    }
}

@Composable
private fun ErrorView(state: CustomFieldsState.Error) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("An error occurred!")
        Text(state.message)
        Spacer(modifier = Modifier.height(16.dp))
        Button(state.onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun ContentView(state: CustomFieldsState.Loaded) {
    var fieldBeingEdited by remember { mutableStateOf<WCMetaData?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button({ fieldBeingEdited = WCMetaData(id = 0L, "", "") }) {
                Text("Add")
            }
            Button(onClick = state.onSave, enabled = state.hasChanges) {
                Text("Save")
            }
        }

        state.customFields.forEach { customField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = customField.key,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = customField.valueAsString,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.body2
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton({ fieldBeingEdited = customField }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton({ state.onDelete(customField) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }

    if (fieldBeingEdited != null) {
        val field = fieldBeingEdited!!
        Dialog(onDismissRequest = { fieldBeingEdited = null }) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colors.surface)
                    .padding(8.dp)
            ) {
                Text("Edit field")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    label = { Text("Key") },
                    value = field.key,
                    onValueChange = {
                        fieldBeingEdited = field.copy(key = it)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    label = { Text("Value") },
                    value = field.valueAsString,
                    onValueChange = {
                        fieldBeingEdited = field.copy(
                            value = WCMetaDataValue.StringValue(it)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (field.id == 0L) {
                            state.onAdd(field)
                        } else {
                            state.onEdit(field)
                        }
                        fieldBeingEdited = null
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Done")
                }
            }
        }
    }
}
