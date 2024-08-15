package org.wordpress.android.fluxc.model.metadata

data class UpdateMetadataRequest(
    val parentItemId: Long,
    val parentItemType: MetaDataParentItemType,
    val insertedMetadata: List<WCMetaData> = emptyList(),
    val updatedMetadata: List<WCMetaData> = emptyList(),
    val deletedMetadata: List<WCMetaData> = emptyList(),
)

