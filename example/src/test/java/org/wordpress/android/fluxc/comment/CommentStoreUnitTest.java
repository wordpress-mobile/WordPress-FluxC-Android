package org.wordpress.android.fluxc.comment;

import android.content.Context;

import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.WellSql;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.CommentSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.util.DateTimeUtils;

import java.util.Date;
import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class CommentStoreUnitTest {
    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();
        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testGetCommentBySiteAndRemoteId() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(21);
        long remoteCommentId = 42;

        // Init Comment Model
        CommentModel commentModel = new CommentModel();
        commentModel.setContent("Best ponies come from the future.");
        commentModel.setLocalSiteId(siteModel.getId());
        commentModel.setRemoteCommentId(remoteCommentId);
        CommentSqlUtils.insertOrUpdateComment(commentModel);

        // Get comment by site and remote id
        CommentModel queriedComment = CommentSqlUtils.getCommentBySiteAndRemoteId(siteModel, remoteCommentId);
        Assert.assertEquals("Best ponies come from the future.", queriedComment.getContent());
    }

    @Test
    public void testMultiGetCommentBySiteAndRemoteId() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(21);

        insertTestComments(siteModel);

        // Get comment by site and remote id
        CommentModel queriedComment = CommentSqlUtils.getCommentBySiteAndRemoteId(siteModel, 10);
        Assert.assertEquals("Pony #10", queriedComment.getContent());

        // Get comment by site and remote id
        queriedComment = CommentSqlUtils.getCommentBySiteAndRemoteId(siteModel, 11);
        Assert.assertEquals("Pony #11", queriedComment.getContent());

        // Get comment by site and remote id
        queriedComment = CommentSqlUtils.getCommentBySiteAndRemoteId(siteModel, 12);
        Assert.assertEquals("Pony #12", queriedComment.getContent());
    }

    @Test
    public void testFailToGetCommentBySiteAndRemoteId() {
        Assert.assertEquals(null, CommentSqlUtils.getCommentBySiteAndRemoteId(new SiteModel(), 42));
    }


    @Test
    public void testGetCommentBySiteAscendingOrder() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(21);
        insertTestComments(siteModel);

        List<CommentModel> ascComments = CommentSqlUtils.getCommentsForSite(siteModel, SelectQuery.ORDER_ASCENDING,
                CommentStatus.ALL);
        CommentModel previousComment = ascComments.get(0);
        for (CommentModel comment : ascComments.subList(1, ascComments.size())) {
            Date d0 = DateTimeUtils.dateFromIso8601(previousComment.getDatePublished());
            Date d1 = DateTimeUtils.dateFromIso8601(comment.getDatePublished());
            Assert.assertTrue("ascending comment list seems incorrectly ordered", d0.before(d1));
            previousComment = comment;
        }
    }

    @Test
    public void testGetCommentBySiteDescendingOrder() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(21);
        insertTestComments(siteModel);

        List<CommentModel> ascComments = CommentSqlUtils.getCommentsForSite(siteModel, SelectQuery.ORDER_DESCENDING,
                CommentStatus.ALL);
        CommentModel previousComment = ascComments.get(0);
        for (CommentModel comment : ascComments.subList(1, ascComments.size())) {
            Date d0 = DateTimeUtils.dateFromIso8601(previousComment.getDatePublished());
            Date d1 = DateTimeUtils.dateFromIso8601(comment.getDatePublished());
            Assert.assertTrue("descending comment list seems incorrectly ordered", d1.before(d0));
            previousComment = comment;
        }
    }

    @Test
    public void testGetCommentsBySingleStatus() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(21);

        insertTestComments(siteModel);

        // Get APPROVED comments
        List<CommentModel> queriedComments = CommentSqlUtils.getCommentsForSite(siteModel, SelectQuery.ORDER_ASCENDING,
                CommentStatus.APPROVED);
        Assert.assertEquals(8, queriedComments.size());

        // Get TRASH comments
        queriedComments = CommentSqlUtils.getCommentsForSite(siteModel, SelectQuery.ORDER_ASCENDING,
                CommentStatus.TRASH);
        Assert.assertEquals(1, queriedComments.size());

        // Get ALL comments
        queriedComments = CommentSqlUtils.getCommentsForSite(siteModel, SelectQuery.ORDER_ASCENDING, CommentStatus.ALL);
        Assert.assertEquals(15, queriedComments.size());
    }

    @Test
    public void testGetCommentsByMultipleStatuses() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(21);

        insertTestComments(siteModel);

        // Get APPROVED, UNAPPROVED and SPAM comments
        List<CommentModel> queriedComments = CommentSqlUtils.getCommentsForSite(siteModel, SelectQuery.ORDER_ASCENDING,
                CommentStatus.APPROVED, CommentStatus.SPAM, CommentStatus.UNAPPROVED);
        Assert.assertEquals(14, queriedComments.size());

        // Get SPAM and UNAPPROVED comments
        queriedComments = CommentSqlUtils.getCommentsForSite(siteModel, SelectQuery.ORDER_ASCENDING,
                CommentStatus.SPAM, CommentStatus.UNAPPROVED);
        Assert.assertEquals(6, queriedComments.size());
    }

    @Test
    public void testGetCommentCountBySingleStatus() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(21);

        insertTestComments(siteModel);

        // Get APPROVED count
        Assert.assertEquals(8, CommentSqlUtils.getCommentsCountForSite(siteModel, CommentStatus.APPROVED));

        // Get TRASH count
        Assert.assertEquals(1, CommentSqlUtils.getCommentsCountForSite(siteModel, CommentStatus.TRASH));

        // Get ALL comments
        Assert.assertEquals(15, CommentSqlUtils.getCommentsCountForSite(siteModel, CommentStatus.ALL));
    }

    @Test
    public void testGetCommentCountByMultipleStatuses() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(21);

        insertTestComments(siteModel);

        // Get SPAM and UNAPPROVED comments
        Assert.assertEquals(6, CommentSqlUtils.getCommentsCountForSite(siteModel, CommentStatus.SPAM,
                CommentStatus.UNAPPROVED));

        // Get ALL (and SPAM) comments
        Assert.assertEquals(15, CommentSqlUtils.getCommentsCountForSite(siteModel, CommentStatus.SPAM, CommentStatus.ALL));
    }

    @Test
    public void testRemoveAllComments() {
        SiteModel site1 = new SiteModel();
        site1.setId(21);
        insertTestComments(site1);

        SiteModel site2 = new SiteModel();
        site2.setId(22);
        insertTestComments(site2);

        // Make sure the comments are inserted successfully before
        Assert.assertEquals(15, CommentSqlUtils.getCommentsCountForSite(site1, CommentStatus.ALL));
        Assert.assertEquals(15, CommentSqlUtils.getCommentsCountForSite(site2, CommentStatus.ALL));

        CommentSqlUtils.deleteAllComments();

        // Test if all the comments are deleted successfully
        Assert.assertEquals(0, CommentSqlUtils.getCommentsCountForSite(site1, CommentStatus.ALL));
        Assert.assertEquals(0, CommentSqlUtils.getCommentsCountForSite(site2, CommentStatus.ALL));
    }

    private void insertTestComments(SiteModel siteModel) {
        // Init Comment Models
        insertNewComment(siteModel, "Pony #10", 10, CommentStatus.APPROVED);
        insertNewComment(siteModel, "Pony #11", 11, CommentStatus.APPROVED);
        insertNewComment(siteModel, "Pony #12", 12, CommentStatus.APPROVED);
        insertNewComment(siteModel, "Pony #13", 13, CommentStatus.APPROVED);
        insertNewComment(siteModel, "Pony #14", 14, CommentStatus.APPROVED);
        insertNewComment(siteModel, "Pony #15", 15, CommentStatus.APPROVED);
        insertNewComment(siteModel, "Pony #16", 16, CommentStatus.APPROVED);
        insertNewComment(siteModel, "Pony #17", 17, CommentStatus.APPROVED);

        insertNewComment(siteModel, "Pony #20", 20, CommentStatus.UNAPPROVED);
        insertNewComment(siteModel, "Pony #21", 21, CommentStatus.UNAPPROVED);
        insertNewComment(siteModel, "Pony #22", 22, CommentStatus.UNAPPROVED);
        insertNewComment(siteModel, "Pony #23", 23, CommentStatus.UNAPPROVED);

        insertNewComment(siteModel, "Pony #30", 30, CommentStatus.SPAM);
        insertNewComment(siteModel, "Pony #31", 31, CommentStatus.SPAM);

        insertNewComment(siteModel, "Pony #40", 40, CommentStatus.TRASH);
    }

    private void insertNewComment(SiteModel site, String content, long remoteId, CommentStatus status) {
        CommentModel commentModel = new CommentModel();
        commentModel.setLocalSiteId(site.getId());
        commentModel.setContent(content);
        commentModel.setRemoteCommentId(remoteId);
        commentModel.setStatus(status.toString());
        commentModel.setDatePublished(DateTimeUtils.iso8601FromTimestamp(new Random().nextInt()));
        CommentSqlUtils.insertOrUpdateComment(commentModel);
    }
}
