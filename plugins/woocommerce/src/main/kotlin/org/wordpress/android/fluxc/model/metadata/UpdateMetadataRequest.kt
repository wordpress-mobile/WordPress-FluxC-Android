package org.wordpress.android.fluxc.model.metadata

data class UpdateMetadataRequest(
    val parentItemId: Long,
    val parentItemType: MetaDataParentItemType,
    val insertedMetadata: List<WCMetaData> = emptyList(),
    val updatedMetadata: List<WCMetaData> = emptyList(),
    val deletedMetadataIds: List<Long> = emptyList(),
) {
    init {
        // The ID of inserted metadata is ignored, so to ensure that there is no data loss here,
        // we require that all inserted metadata have an ID of 0.
        require(insertedMetadata.all { it.id == 0L }) {
            "Inserted metadata must have an ID of 0"
        }
    }
}
