package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.JsonLoaderUtils.jsonFileAs
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.WCMetaData.BundleMetadataKeys
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemotePriceType.FlatFee
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteRestrictionsType.AnyText
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.Checkbox
import kotlin.test.fail

class ProductDtoMapperTest {
    private val productDtoMapper = ProductDtoMapper(
        stripProductMetaData = mock {
            on { invoke(anyOrNull()) } doAnswer { it.arguments[0] as String }
        }
    )

    @Test
    fun `Product addons should be serialized correctly`() {
        val productModelUnderTest =
            "wc/product-with-addons.json"
                .jsonFileAs(ProductApiResponse::class.java)
                ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
                ?.product

        assertThat(productModelUnderTest).isNotNull
        assertThat(productModelUnderTest?.addons).isNotEmpty
        assertThat(productModelUnderTest?.addons?.size).isEqualTo(3)
    }

    @Test
    fun `Product addons should be serialized with enum values correctly`() {
        val productModelUnderTest =
            "wc/product-with-addons.json"
                .jsonFileAs(ProductApiResponse::class.java)
                ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
                ?.product

        assertThat(productModelUnderTest).isNotNull
        assertThat(productModelUnderTest?.addons).isNotEmpty

        productModelUnderTest?.addons?.first()?.let {
            assertThat(it.priceType).isEqualTo(FlatFee)
            assertThat(it.restrictionsType).isEqualTo(AnyText)
            assertThat(it.type).isEqualTo(Checkbox)
        } ?: fail("Addons list shouldn't be empty")
    }

    @Test
    fun `Product addons should contain Addon options serialized correctly`() {
        val addonOptions = "wc/product-with-addons.json"
            .jsonFileAs(ProductApiResponse::class.java)
            ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
            ?.product
            ?.addons
            ?.takeIf { it.isNotEmpty() }
            ?.first()
            ?.options

        assertThat(addonOptions).isNotNull
        assertThat(addonOptions).isNotEmpty
    }

    @Test
    fun `Product metadata is serialized correctly`() {
        val productModelUnderTest =
            "wc/product-with-addons.json"
                .jsonFileAs(ProductApiResponse::class.java)
                ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
                ?.product

        assertThat(productModelUnderTest).isNotNull
        assertThat(productModelUnderTest?.metadata).isNotNull
    }

    @Test
    fun `Product addons with incorrect key should be null`() {
        val productModelUnderTest =
            "wc/product-with-incorrect-addons-key.json"
                .jsonFileAs(ProductApiResponse::class.java)
                ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
                ?.product

        assertThat(productModelUnderTest).isNotNull
        assertThat(productModelUnderTest?.metadata).isNotNull
        assertThat(productModelUnderTest?.addons).isNull()
    }

    @Test
    fun `Bundled product with max size is serialized correctly`() {
        val product = "wc/product-bundle-with-max-quantity.json"
            .jsonFileAs(ProductApiResponse::class.java)
            ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
            ?.product

        assertThat(product?.metadata).isNotNull
        val map = product?.parsedMetaData?.associateBy { it.key }!!

        assertThat(map).containsKey(BundleMetadataKeys.BUNDLE_MAX_SIZE)
        assertThat(map).doesNotContainKey(BundleMetadataKeys.BUNDLE_MIN_SIZE)
        assertThat(map.getValue(BundleMetadataKeys.BUNDLE_MAX_SIZE).valueAsString).isEqualTo("5")
    }

    @Test
    fun `Bundled product with min size is serialized correctly`() {
        val product = "wc/product-bundle-with-min-quantity.json"
            .jsonFileAs(ProductApiResponse::class.java)
            ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
            ?.product

        assertThat(product?.metadata).isNotNull
        val map = product?.parsedMetaData?.associateBy { it.key }!!

        assertThat(map).containsKey(BundleMetadataKeys.BUNDLE_MIN_SIZE)
        assertThat(map).doesNotContainKey(BundleMetadataKeys.BUNDLE_MAX_SIZE)
        assertThat(map.getValue(BundleMetadataKeys.BUNDLE_MIN_SIZE).valueAsString).isEqualTo("5")
    }

    @Test
    fun `Bundled product with quantity rules is serialized correctly`() {
        val product = "wc/product-bundle-with-quantity-rules.json"
            .jsonFileAs(ProductApiResponse::class.java)
            ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
            ?.product

        assertThat(product?.metadata).isNotNull
        val map = product?.parsedMetaData?.associateBy { it.key }!!

        assertThat(map).containsKey(BundleMetadataKeys.BUNDLE_MIN_SIZE)
        assertThat(map).containsKey(BundleMetadataKeys.BUNDLE_MAX_SIZE)
        assertThat(map.getValue(BundleMetadataKeys.BUNDLE_MAX_SIZE).valueAsString).isEqualTo("5")
        assertThat(map.getValue(BundleMetadataKeys.BUNDLE_MIN_SIZE).valueAsString).isEqualTo("5")
    }

    @Test
    fun `Bundled product with special stock status is serialized correctly`() {
        val product = "wc/product-bundle-with-max-quantity.json"
            .jsonFileAs(ProductApiResponse::class.java)
            ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
            ?.product

        assertThat(product?.specialStockStatus).isEqualTo("insufficientstock")
    }
}
