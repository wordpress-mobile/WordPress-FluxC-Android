package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.UpdateMetadataRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.metadata.MetaDataRestClient
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.entity.MetaDataEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetaDataStore @Inject internal constructor(
    private val metaDataRestClient: MetaDataRestClient,
    private val metaDataDao: MetaDataDao
) {
    suspend fun updateMetaData(
        site: SiteModel,
        request: UpdateMetadataRequest
    ): WooResult<Unit> {
        val result = metaDataRestClient.updateMetaData(site, request)

        result.result?.let {
            metaDataDao.updateMetaData(
                localSiteId = site.localId(),
                parentItemId = request.parentItemId,
                metaData = it.map { metaData ->
                    MetaDataEntity.fromDomainModel(
                        metaData = metaData,
                        localSiteId = site.localId(),
                        parentItemId = request.parentItemId,
                        parentItemType = request.parentItemType
                    )
                }
            )
        }

        @Suppress("RedundantUnitExpression")
        return result.asWooResult { Unit }
    }

    suspend fun refreshMetaData(
        site: SiteModel,
        parentItemId: Long,
        parentItemType: MetaDataParentItemType
    ): WooResult<Unit> {
        val result = metaDataRestClient.refreshMetaData(site, parentItemId, parentItemType)

        result.result?.let {
            metaDataDao.updateMetaData(
                localSiteId = site.localId(),
                parentItemId = parentItemId,
                metaData = it.map { metaData ->
                    MetaDataEntity.fromDomainModel(
                        metaData = metaData,
                        localSiteId = site.localId(),
                        parentItemId = parentItemId,
                        parentItemType = parentItemType
                    )
                }
            )
        }

        @Suppress("RedundantUnitExpression")
        return result.asWooResult { Unit }
    }

    fun observeMetaData(
        site: SiteModel,
        parentItemId: Long
    ) = metaDataDao.observeMetaData(site.localId(), parentItemId)
        .map { list -> list.map { it.toDomainModel() } }

    fun observeDisplayableMetaData(
        site: SiteModel,
        parentItemId: Long
    ) = metaDataDao.observeDisplayableMetaData(site.localId(), parentItemId)
        .map { list -> list.map { it.toDomainModel() } }

    suspend fun getMetaData(
        site: SiteModel,
        parentItemId: Long
    ) = metaDataDao.getMetaData(site.localId(), parentItemId).map { it.toDomainModel() }

    suspend fun getDisplayableMetaData(
        site: SiteModel,
        parentItemId: Long
    ) = metaDataDao.getDisplayableMetaData(site.localId(), parentItemId).map { it.toDomainModel() }

    suspend fun getMetaData(
        site: SiteModel,
        parentItemId: Long,
        metaDataId: Long
    ) = metaDataDao.getMetaData(
        localSiteId = site.localId(),
        parentItemId = parentItemId,
        id = metaDataId
    )
        ?.toDomainModel()

    suspend fun getMetaDataByKey(
        site: SiteModel,
        parentItemId: Long,
        key: String
    ) = metaDataDao.getMetaDataByKey(site.localId(), parentItemId, key)?.map { it.toDomainModel() }
}
