package org.wordpress.android.fluxc.wc.taxes

import org.wordpress.android.fluxc.model.taxes.WCTaxClassModel

object TaxTestUtils {
    fun generateSampleTaxClass(
        name: String = "",
        slug: String = "",
        siteId: Int = 6
    ): WCTaxClassModel {
        return WCTaxClassModel().apply {
            localSiteId = siteId
            this.name = name
            this.slug = slug
        }
    }

    fun generateTaxClassList(siteId: Int = 6, slug: String = "slug"): List<WCTaxClassModel> {
        with(ArrayList<WCTaxClassModel>()) {
            add(generateSampleTaxClass(siteId = siteId, slug = slug.plus(1)))
            add(generateSampleTaxClass(siteId = siteId, slug = slug.plus(2)))
            add(generateSampleTaxClass(siteId = siteId, slug = slug.plus(3)))
            add(generateSampleTaxClass(siteId = siteId, slug = slug.plus(4)))
            add(generateSampleTaxClass(siteId = siteId, slug = slug.plus(5)))
            return this
        }
    }
}
