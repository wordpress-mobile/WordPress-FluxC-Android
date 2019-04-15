package org.wordpress.android.fluxc.persistence.room

import android.arch.persistence.room.TypeConverter
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType

class BlockTypeConverter {
    @TypeConverter
    fun toBlockType(value: String?): BlockType? {
        return if (value == null) null else BlockType.valueOf(value)
    }

    @TypeConverter
    fun toString(value: BlockType?): String? {
        return value?.name
    }
}
