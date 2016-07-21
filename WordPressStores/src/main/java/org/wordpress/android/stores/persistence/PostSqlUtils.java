package org.wordpress.android.stores.persistence;

import com.wellsql.generated.PostModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.stores.model.PostModel;

import java.util.List;

public class PostSqlUtils {
    public static int insertOrUpdatePost(PostModel post, boolean overwriteLocalChanges) {
        if (post == null) {
            return 0;
        }

        List<PostModel> postResult = WellSql.select(PostModel.class)
                .where().beginGroup()
                .equals(PostModelTable.POST_ID, post.getPostId())
                .equals(PostModelTable.SITE_ID, post.getSiteId())
                .equals(PostModelTable.IS_PAGE, post.isPage())
                .endGroup().endWhere().getAsModel();

        if (overwriteLocalChanges) {
            post.setIsLocallyChanged(false);
        }

        if (postResult.isEmpty()) {
            // insert
            WellSql.insert(post).asSingleTransaction(true).execute();
        } else {
            // Update only if local changes for this post don't exist
            if (overwriteLocalChanges || !postResult.get(0).isLocallyChanged()) {
                int oldId = postResult.get(0).getId();
                return WellSql.update(PostModel.class).whereId(oldId)
                        .put(post, new UpdateAllExceptId<PostModel>()).execute();
            }
        }
        return 0;
    }

    public static int insertOrUpdatePostKeepingLocalChanges(PostModel post) {
        return insertOrUpdatePost(post, false);
    }

    public static int insertOrUpdatePostOverwritingLocalChanges(PostModel post) {
        return insertOrUpdatePost(post, true);
    }
}
