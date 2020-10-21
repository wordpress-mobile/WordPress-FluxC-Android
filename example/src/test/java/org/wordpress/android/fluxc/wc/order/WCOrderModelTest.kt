package org.wordpress.android.fluxc.wc.order

import org.junit.Test
import org.wordpress.android.fluxc.UnitTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WCOrderModelTest {
    @Test
    fun testGetShippingAddress() {
        val model = OrderTestUtils.generateSampleOrder(61).apply {
            shippingAddress1 = "Some place"
        }

        assertEquals("Some place", model.getShippingAddress().address1)
        model.shippingAddress1 = "something else"
        assertEquals("something else", model.getShippingAddress().address1)
    }

    @Test
    fun testGetShippingVSBillingAddress() {
        val model = OrderTestUtils.generateSampleOrder(61).apply {
            billingAddress1 = "Some place"
            billingCountry = "Canada"
            shippingAddress1 = "A different place"
            shippingCountry = "Canada"
        }

        assertTrue(model.hasSeparateShippingDetails())
        assertEquals("Some place", model.getBillingAddress().address1)
        assertEquals("A different place", model.getShippingAddress().address1)
        model.billingAddress1 = "something else"
        assertEquals("something else", model.getBillingAddress().address1)
    }

    @Test
    fun testGetLineItems() {
        val model = OrderTestUtils.generateSampleOrder(61).apply {
            lineItems = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/lineitems.json")
        }
        val renderedLineItems = model.getLineItemList()
        assertEquals(2, renderedLineItems.size)

        with(renderedLineItems[0]) {
            assertEquals("A test", name)
            assertEquals(15, productId)
            assertNull(variationId)
            assertEquals("10.00", total)
            assertNull(sku)
        }

        with(renderedLineItems[1]) {
            assertEquals("A second test", name)
            assertEquals(65, productId)
            assertEquals(3, variationId)
            assertEquals("20.00", total)
            assertEquals("blabla", sku)
        }
    }

    @Test
    fun testGetLineItemAttributes() {
        val model = OrderTestUtils.generateSampleOrder(61).apply {
            lineItems = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/lineitems.json")
        }
        val renderedLineItems = model.getLineItemList()
        assertEquals(2, renderedLineItems.size)

        with(renderedLineItems[0]) {
            val attributes = getAttributeList()
            assertEquals(2, attributes.size)

            assertEquals("color", attributes[0].key)
            assertEquals("Red", attributes[0].value)

            assertEquals("size", attributes[1].key)
            assertEquals("Medium", attributes[1].value)
        }

        with(renderedLineItems[1]) {
            val attributes = getAttributeList()
            assertEquals(1, attributes.size)

            assertEquals("size", attributes[0].key)
            assertEquals("Medium", attributes[0].value)
        }
    }

    @Test
    fun testGetSubtotal() {
        val model = OrderTestUtils.generateSampleOrder(61).apply {
            lineItems = "[{\"subtotal\": \"12.26\"},{\"subtotal\": \"15.39\"}]"
        }

        assertEquals(27.65, model.getOrderSubtotal())

        model.lineItems = "[{\"total\": \"12.26\"},{\"total\": \"15.39\"}]"
        assertEquals(0.0, model.getOrderSubtotal())
    }
}
