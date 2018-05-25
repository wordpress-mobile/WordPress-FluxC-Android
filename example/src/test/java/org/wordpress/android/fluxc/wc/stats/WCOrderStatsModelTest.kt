package org.wordpress.android.fluxc.wc.stats

import org.junit.Test
import kotlin.test.assertEquals

class WCOrderStatsModelTest {
    @Test
    fun testGetFields() {
        val model = WCStatsTestUtils.generateSampleStatsModel()

        assertEquals(model.fieldsList.size, model.dataList[0].size)

        assertEquals(7, model.dataList.size)
    }
}
