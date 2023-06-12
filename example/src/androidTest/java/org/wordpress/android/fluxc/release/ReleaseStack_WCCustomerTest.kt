@file:Suppress("DEPRECATION_ERROR")

package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.store.WCCustomerStore
import javax.inject.Inject

class ReleaseStack_WCCustomerTest : ReleaseStack_WCBase() {
    @Inject internal lateinit var wcCustomerStore: WCCustomerStore

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
    }

    @Test
    fun testFetchOrdersFirstPage() {
        runBlocking {
            val result = wcCustomerStore.fetchCustomersFromAnalytics(sSite, page = 1)
            assertThat(result.model?.size).isEqualTo(1)
            assertThat(result.model?.get(0)?.id).isEqualTo(1)
            assertThat(result.model?.get(0)?.name).isEqualTo("John Johnson")
            assertThat(result.model?.get(0)?.city).isEqualTo("Toronto")
            assertThat(result.model?.get(0)?.email).isEqualTo("null@bia.is")
            assertThat(result.model?.get(0)?.country).isEqualTo("CA")
            assertThat(result.model?.get(0)?.postcode).isEqualTo("M6H 1Z8")
        }
    }

    @Test
    fun testFetchOrdersSecondPage() {
        runBlocking {
            val result = wcCustomerStore.fetchCustomersFromAnalytics(sSite, page = 2)
            assertThat(result.model?.size).isEqualTo(0)
        }
    }

    @Test
    fun testFetchOrdersSearchWithResults() {
        runBlocking {
            val result = wcCustomerStore.fetchCustomersFromAnalytics(
                sSite,
                page = 1,
                searchQuery = "John",
                searchBy = "name",
            )
            assertThat(result.model?.size).isEqualTo(1)
            assertThat(result.model?.get(0)?.id).isEqualTo(1)
        }
    }

    @Test
    fun testFetchOrdersSearchByEmailWithoutResults() {
        runBlocking {
            val result = wcCustomerStore.fetchCustomersFromAnalytics(
                sSite,
                page = 1,
                searchQuery = "John",
                searchBy = "email",
            )
            assertThat(result.model?.size).isEqualTo(0)
        }
    }

    @Test
    fun testFetchOrdersSearchByWithResults() {
        runBlocking {
            val result = wcCustomerStore.fetchCustomersFromAnalytics(
                sSite,
                page = 1,
                searchQuery = "bia",
                searchBy = "email",
            )
            assertThat(result.model?.size).isEqualTo(1)
            assertThat(result.model?.get(0)?.id).isEqualTo(1)
        }
    }
}
