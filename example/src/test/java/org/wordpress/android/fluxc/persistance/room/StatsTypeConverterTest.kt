package org.wordpress.android.fluxc.persistance.room

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.values
import org.wordpress.android.fluxc.persistence.room.StatsTypeConverter

@RunWith(MockitoJUnitRunner::class)
class StatsTypeConverterTest {
    private val statsTypeConverter = StatsTypeConverter()
    private val statsTypeValues = listOf(
            "INSIGHTS",
            "DAY",
            "WEEK",
            "MONTH",
            "YEAR"
    )

    @Test
    fun `converts stats type to string and back`() {
        for (testedType in values()) {
            val stringBlockType = statsTypeConverter.toString(testedType)
            assertThat(stringBlockType).isNotBlank()
            val parsedBlockType = statsTypeConverter.toStatsType(stringBlockType)
            assertThat(parsedBlockType).isEqualTo(testedType)
        }
    }

    @Test
    fun `parses all the database values`() {
        for (stringValue in statsTypeValues) {
            val parsedBlockType = statsTypeConverter.toStatsType(stringValue)
            assertThat(values()).contains(parsedBlockType)
        }
    }

    @Test
    fun `all the enum values are tested`() {
        for (testedType in values()) {
            val stringBlockType = statsTypeConverter.toString(testedType)
            assertThat(statsTypeValues).contains(stringBlockType)
        }
    }
}
