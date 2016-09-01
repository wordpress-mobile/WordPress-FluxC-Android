package org.wordpress.android.fluxc.utils;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.PostModel;

public class WellSqlUtils {
    public static int getTotalPostsCount() {
        return WellSql.select(PostModel.class).getAsCursor().getCount();
    }
}
