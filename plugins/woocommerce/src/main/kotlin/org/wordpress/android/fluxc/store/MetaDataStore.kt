package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetaDataStore @Inject internal constructor(
    private val metaDataDao: MetaDataDao
) {
    fun observeMetaData(
        site: SiteModel,
        parentItemId: Long
    ) = metaDataDao.observeMetaData(site.localId(), parentItemId).map { list -> list.map { it.toDomainModel() } }

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
    ) = metaDataDao.getMetaData(localSiteId = site.localId(), parentItemId = parentItemId, id = metaDataId)
        ?.toDomainModel()

    suspend fun getMetaDataByKey(
        site: SiteModel,
        parentItemId: Long,
        key: String
    ) = metaDataDao.getMetaDataByKey(site.localId(), parentItemId, key)?.map { it.toDomainModel() }
}
