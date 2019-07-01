package org.wordpress.android.fluxc.persistence.room

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType

class StatsTypeConverter {
    @TypeConverter
    fun toStatsType(value: String?): StatsType? {
        return if (value == null) null else StatsType.valueOf(value)
    }

    @TypeConverter
    fun toString(value: StatsType?): String? {
        return value?.name
    }
}
