package org.wordpress.android.fluxc.utils;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.TermModel;

public class WellSqlUtils {
    public static int getTotalPostsCount() {
        return WellSql.select(PostModel.class).getAsCursor().getCount();
    }

    public static int getTotalTermsCount() {
        return WellSql.select(TermModel.class).getAsCursor().getCount();
    }
}
