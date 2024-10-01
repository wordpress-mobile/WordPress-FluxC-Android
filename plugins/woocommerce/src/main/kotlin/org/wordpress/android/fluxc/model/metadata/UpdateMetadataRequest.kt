package org.wordpress.android.fluxc.model.metadata

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject

/**
 * Represents a request to update metadata for a parent item.
 *
 * @param parentItemId The ID of the parent item.
 * @param parentItemType The type of the parent item.
 * @param insertedMetadata The metadata to insert.
 * @param updatedMetadata The metadata to update.
 * @param deletedMetadataIds The IDs of the metadata to delete, this is used when we want to delete values by ID.
 * @param deletedMetaDataKeys The keys of the metadata to delete, this is used when we want to delete all values that
 * match a given key.
 */
data class UpdateMetadataRequest(
    val parentItemId: Long,
    val parentItemType: MetaDataParentItemType,
    val insertedMetadata: List<WCMetaData> = emptyList(),
    val updatedMetadata: List<WCMetaData> = emptyList(),
    val deletedMetadataIds: List<Long> = emptyList(),
    val deletedMetaDataKeys: List<String> = emptyList()
) {
    init {
        // The ID of inserted metadata is ignored, so to ensure that there is no data loss here,
        // we require that all inserted metadata have an ID of 0.
        require(insertedMetadata.all { it.id == 0L }) {
            "Inserted metadata must have an ID of 0"
        }
    }

    internal fun asJsonArray(): JsonArray {
        val metaDataJson = JsonArray()
        insertedMetadata.forEach {
            metaDataJson.add(
                JsonObject().apply {
                    addProperty(WCMetaData.KEY, it.key)
                    add(WCMetaData.VALUE, it.value.jsonValue)
                }
            )
        }
        updatedMetadata.forEach {
            metaDataJson.add(it.toJson())
        }
        deletedMetadataIds.forEach {
            metaDataJson.add(
                JsonObject().apply {
                    addProperty(WCMetaData.ID, it)
                    add(WCMetaData.VALUE, JsonNull.INSTANCE)
                }
            )
        }
        deletedMetaDataKeys.forEach {
            metaDataJson.add(
                JsonObject().apply {
                    addProperty(WCMetaData.KEY, it)
                    add(WCMetaData.VALUE, JsonNull.INSTANCE)
                }
            )
        }

        return metaDataJson
    }
}
