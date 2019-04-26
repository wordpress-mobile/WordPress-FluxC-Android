package org.wordpress.android.fluxc.persistance.room

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.values
import org.wordpress.android.fluxc.persistence.room.BlockTypeConverter

@RunWith(MockitoJUnitRunner::class)
class BlockTypeConverterTest {
    private val blockTypeConverter = BlockTypeConverter()
    private val blockTypeValues = listOf(
            "ALL_TIME_INSIGHTS",
            "MOST_POPULAR_INSIGHTS",
            "LATEST_POST_DETAIL_INSIGHTS",
            "DETAILED_POST_STATS",
            "TODAYS_INSIGHTS",
            "WP_COM_FOLLOWERS",
            "EMAIL_FOLLOWERS",
            "COMMENTS_INSIGHTS",
            "TAGS_AND_CATEGORIES_INSIGHTS",
            "POSTS_AND_PAGES_VIEWS",
            "REFERRERS",
            "CLICKS",
            "VISITS_AND_VIEWS",
            "COUNTRY_VIEWS",
            "AUTHORS",
            "SEARCH_TERMS",
            "VIDEO_PLAYS",
            "PUBLICIZE_INSIGHTS",
            "POSTING_ACTIVITY"
    )

    @Test
    fun `converts block type to string and back`() {
        for (testedType in values()) {
            val stringBlockType = blockTypeConverter.toString(testedType)
            assertThat(stringBlockType).isNotBlank()
            val parsedBlockType = blockTypeConverter.toBlockType(stringBlockType)
            assertThat(parsedBlockType).isEqualTo(testedType)
        }
    }

    @Test
    fun `parses all the database values`() {
        for (stringValue in blockTypeValues) {
            val parsedBlockType = blockTypeConverter.toBlockType(stringValue)
            assertThat(values()).contains(parsedBlockType)
        }
    }

    @Test
    fun `all the enum values are tested`() {
        for (testedType in values()) {
            val stringBlockType = blockTypeConverter.toString(testedType)
            assertThat(blockTypeValues).contains(stringBlockType)
        }
    }
}
