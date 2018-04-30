package org.wordpress.android.fluxc.wc.stats

import org.junit.Test
import kotlin.test.assertEquals

class WCOrderStatsModelTest {
    @Test
    fun testGetFields() {
        val model = WCStatsTestUtils.generateSampleStatsModel()

        assertEquals(model.getFieldsList().size, model.getDataList()[0].size)

        assertEquals(7, model.getDataList().size)
    }
}
