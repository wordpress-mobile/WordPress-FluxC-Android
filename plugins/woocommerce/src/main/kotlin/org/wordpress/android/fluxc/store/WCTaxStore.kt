package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.taxes.WCTaxClassMapper
import org.wordpress.android.fluxc.model.taxes.WCTaxClassModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes.WCTaxRestClient
import org.wordpress.android.fluxc.persistence.WCTaxSqlUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR

@Singleton
class WCTaxStore @Inject constructor(
    private val restClient: WCTaxRestClient,
    private val coroutineContext: CoroutineContext,
    private val mapper: WCTaxClassMapper
) {
    /**
     * returns a list of tax classes for a specific site in the database
     */
    fun getTaxClassListForSite(site: SiteModel): List<WCTaxClassModel> =
            WCTaxSqlUtils.getTaxClassesForSite(site.id)

    suspend fun fetchTaxClassList(site: SiteModel): WooResult<List<WCTaxClassModel>> {
        return withContext(coroutineContext) {
            val response = restClient.fetchTaxClassList(site)
            return@withContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val taxClassModels = response.result.map {
                        mapper.map(it).apply { localSiteId = site.id }
                    }

                    // delete existing tax classes for site before adding incoming entries
                    WCTaxSqlUtils.deleteTaxClassesForSite(site)
                    WCTaxSqlUtils.insertOrUpdateTaxClasses(taxClassModels)
                    WooResult(taxClassModels)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
