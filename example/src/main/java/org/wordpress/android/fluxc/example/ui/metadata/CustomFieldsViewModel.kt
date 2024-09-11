package org.wordpress.android.fluxc.example.ui.metadata

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.metadata.UpdateMetadataRequest
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.store.MetaDataStore

class CustomFieldsViewModel(
    private val coroutineScope: CoroutineScope,
    private val site: SiteModel,
    private val parentItemId: Long,
    private val parentItemType: MetaDataParentItemType,
    private val metaDataStore: MetaDataStore
) {
    private val loadingState = MutableStateFlow<LoadingState>(LoadingState.Loaded)
    var state by mutableStateOf<CustomFieldsState>(CustomFieldsState.Loading)
        private set

    private val pendingUpdateRequest = MutableStateFlow(
        UpdateMetadataRequest(
            parentItemId = parentItemId,
            parentItemType = parentItemType,
            insertedMetadata = emptyList(),
            updatedMetadata = emptyList(),
            deletedMetadataIds = emptyList()
        )
    )

    init {
        observeLoadingState()
        loadCustomFields()
    }

    private fun observeLoadingState() {
        val customFields = combine(
            metaDataStore.observeDisplayableMetaData(
                site,
                parentItemId
            ),
            pendingUpdateRequest
        ) { customFields, pendingUpdateRequest ->
            customFields.filterNot { it.id in pendingUpdateRequest.deletedMetadataIds }
                .map { field ->
                    pendingUpdateRequest.updatedMetadata.find { it.key == field.key }
                        ?: field
                } + pendingUpdateRequest.insertedMetadata
        }

        combine(
            loadingState,
            customFields,
            pendingUpdateRequest.map {
                it.insertedMetadata.isNotEmpty() ||
                    it.updatedMetadata.isNotEmpty() ||
                    it.deletedMetadataIds.isNotEmpty()
            }
        ) { loadingState, metaData, hasChanges ->
            when (loadingState) {
                LoadingState.Loading -> CustomFieldsState.Loading
                LoadingState.Loaded -> CustomFieldsState.Loaded(
                    customFields = metaData,
                    onDelete = { field ->
                        pendingUpdateRequest.update {
                            it.copy(
                                deletedMetadataIds = it.deletedMetadataIds + field.id
                            )
                        }
                    },
                    onEdit = { field ->
                        pendingUpdateRequest.update {
                            it.copy(
                                updatedMetadata = it.updatedMetadata + field
                            )
                        }
                    },
                    onAdd = { field ->
                        pendingUpdateRequest.update {
                            it.copy(
                                insertedMetadata = it.insertedMetadata + field
                            )
                        }
                    },
                    onSave = { saveChanges() },
                    hasChanges = hasChanges
                )

                is LoadingState.Error -> CustomFieldsState.Error(loadingState.message) {
                    loadCustomFields()
                }
            }
        }.onEach {
            state = it
        }.launchIn(coroutineScope)
    }

    private fun loadCustomFields() {
        coroutineScope.launch {
            launch {
                loadingState.value = LoadingState.Loading
                metaDataStore.refreshMetaData(site, parentItemId, parentItemType).let {
                    if (it.isError) {
                        loadingState.value = LoadingState.Error(
                            message = it.error?.message ?: "Unknown error"
                        )
                    } else {
                        loadingState.value = LoadingState.Loaded
                    }
                }
            }
        }
    }

    private fun saveChanges() {
        coroutineScope.launch {
            loadingState.value = LoadingState.Loading
            val request = pendingUpdateRequest.value
            metaDataStore.updateMetaData(site, request).let { result ->
                if (result.isError) {
                    loadingState.value = LoadingState.Error(
                        message = result.error?.message ?: "Unknown error"
                    )
                } else {
                    pendingUpdateRequest.update {
                        it.copy(
                            insertedMetadata = emptyList(),
                            updatedMetadata = emptyList(),
                            deletedMetadataIds = emptyList()
                        )
                    }
                    loadingState.value = LoadingState.Loaded
                }
            }
        }
    }

    private sealed interface LoadingState {
        data object Loading : LoadingState
        data object Loaded : LoadingState
        data class Error(val message: String) : LoadingState
    }

    sealed interface CustomFieldsState {
        data object Loading : CustomFieldsState
        data class Loaded(
            val customFields: List<WCMetaData>,
            val onDelete: (WCMetaData) -> Unit,
            val onEdit: (WCMetaData) -> Unit,
            val onAdd: (WCMetaData) -> Unit,
            val onSave: () -> Unit,
            val hasChanges: Boolean = false
        ) : CustomFieldsState

        data class Error(
            val message: String,
            val onRetry: () -> Unit
        ) : CustomFieldsState
    }
}
