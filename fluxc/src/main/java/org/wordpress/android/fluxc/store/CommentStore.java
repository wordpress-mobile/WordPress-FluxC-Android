package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentXMLRPCClient;
import org.wordpress.android.fluxc.persistence.CommentSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;

import javax.inject.Inject;

public class CommentStore extends Store {
    CommentRestClient mCommentRestClient;
    CommentXMLRPCClient mCommentXMLRPCClient;

    // Payloads

    public static class FetchCommentsPayload extends Payload {
        public final SiteModel site;
        public final boolean loadMore;

        public FetchCommentsPayload(@NonNull SiteModel site) {
            this.site = site;
            this.loadMore = false;
        }

        public FetchCommentsPayload(@NonNull SiteModel site, boolean loadMore) {
            this.site = site;
            this.loadMore = loadMore;
        }
    }

    public static class RemoteCommentPayload extends Payload {
        public final SiteModel site;
        public final CommentModel comment;

        public RemoteCommentPayload(@NonNull SiteModel site, @NonNull CommentModel comment) {
            this.site = site;
            this.comment = comment;
        }
    }

    public static class InstantiateCommentPayload extends Payload {
        public final SiteModel site;

        public InstantiateCommentPayload(@NonNull SiteModel site) {
            this.site = site;
        }
    }

    public static class FetchCommentsResponsePayload extends Payload {
        public final List<CommentModel> comments;
        public CommentError error;
        public FetchCommentsResponsePayload(@NonNull List<CommentModel> comments) {
            this.comments = comments;
        }
    }

    public static class RemoteCommentResponsePayload extends Payload {
        public final CommentModel comment;
        public CommentError error;
        public RemoteCommentResponsePayload(@NonNull CommentModel comment) {
            this.comment = comment;
        }
    }

    public static class RemoteCreateCommentPayload extends Payload {
        public final SiteModel site;
        public final CommentModel comment;
        public final CommentModel reply;
        public final PostModel post;

        public CommentError error;
        public RemoteCreateCommentPayload(@NonNull SiteModel site, @NonNull PostModel post,
                                          @NonNull CommentModel comment) {
            this.site = site;
            this.post = post;
            this.comment = comment;
            this.reply = null;
        }

        public RemoteCreateCommentPayload(@NonNull SiteModel site, @NonNull CommentModel comment,
                                          @NonNull CommentModel reply) {
            this.site = site;
            this.comment = comment;
            this.reply = reply;
            this.post = null;
        }
    }

    // Errors

    public enum CommentErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        INVALID_COMMENT
    }

    public static class CommentError implements OnChangedError {
        public CommentErrorType type;
        public String message;
        public CommentError(CommentErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }
    }

    // Actions

    public class OnCommentChanged extends OnChanged<CommentError> {
        public int rowsAffected;
        public CommentAction causeOfChange;
        public OnCommentChanged(int rowsAffected) {
            this.rowsAffected = rowsAffected;
        }
    }

    public class OnCommentInstantiated extends OnChanged<CommentError> {
        public CommentModel comment;
        public OnCommentInstantiated(CommentModel comment) {
            this.comment = comment;
        }
    }

    // Constructor

    @Inject
    public CommentStore(Dispatcher dispatcher, CommentRestClient commentRestClient, CommentXMLRPCClient
            commentXMLRPCClient) {
        super(dispatcher);
        mCommentRestClient = commentRestClient;
        mCommentXMLRPCClient = commentXMLRPCClient;
    }

    // Getters

    public List<CommentModel> getCommentsForSite(SiteModel site) {
        return CommentSqlUtils.getCommentsForSite(site);
    }

    public CommentModel getCommentByLocalId(int localId) {
        return CommentSqlUtils.getCommentByLocalCommentId(localId);
    }

    // Store Methods

    @Override
    @Subscribe
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof CommentAction)) {
            return;
        }

        switch ((CommentAction) actionType) {
            case FETCH_COMMENTS:
                fetchComments((FetchCommentsPayload) action.getPayload());
                break;
            case FETCHED_COMMENTS:
                handleFetchCommentsResponse((FetchCommentsResponsePayload) action.getPayload());
                break;
            case FETCH_COMMENT:
                fetchComment((org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload) action.getPayload());
                break;
            case FETCHED_COMMENT:
                handleFetchCommentResponse((org.wordpress.android.fluxc.store.CommentStore.RemoteCommentResponsePayload) action.getPayload());
                break;
            case INSTANTIATE_COMMENT:
                instantiateComment((InstantiateCommentPayload) action.getPayload());
                break;
            case UPDATE_COMMENT:
                updateComment((CommentModel) action.getPayload());
                break;
            case PUSH_COMMENT:
                pushComment((RemoteCommentPayload) action.getPayload());
                break;
            case PUSHED_COMMENT:
                handlePushCommentResponse((RemoteCommentResponsePayload) action.getPayload());
                break;
            case REMOVE_COMMENT:
                removeComment((CommentModel) action.getPayload());
                break;
            case DELETE_COMMENT:
                deleteComment((RemoteCommentPayload) action.getPayload());
                break;
            case DELETED_COMMENT:
                handleDeletedCommentResponse((RemoteCommentResponsePayload) action.getPayload());
                break;
        }
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, this.getClass().getName() + ": onRegister");
    }

    // Private methods

    private void createNewComment(RemoteCreateCommentPayload payload) {
        if (payload.reply == null) {
            // Create a new comment on a specific Post
            if (payload.site.isWPCom()) {
                mCommentRestClient.createNewComment(payload.site, payload.post, payload.comment);
            } else {
                mCommentXMLRPCClient.createNewComment(payload.site, payload.post, payload.comment);
            }
        } else {
            // Create a new reply to a specific Comment
            if (payload.site.isWPCom()) {
                mCommentRestClient.createNewReply(payload.site, payload.comment, payload.reply);
            } else {
                mCommentXMLRPCClient.createNewReply(payload.site, payload.comment, payload.reply);
            }
        }
    }
    private void updateComment(CommentModel payload) {
        int rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload);
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.causeOfChange = CommentAction.UPDATE_COMMENT;
        emitChange(event);
    }

    private void removeComment(CommentModel payload) {
        CommentSqlUtils.deleteComment(payload);
    }

    private void instantiateComment(InstantiateCommentPayload payload) {
        CommentModel comment = new CommentModel();
        comment.setLocalSiteId(payload.site.getId());
        CommentSqlUtils.insertOrUpdateComment(comment);
        emitChange(new OnCommentInstantiated(comment));
    }

    private void deleteComment(RemoteCommentPayload payload) {
        // FIXME
    }

    private void handleDeletedCommentResponse(RemoteCommentResponsePayload payload) {
        // FIXME
    }

    private void fetchComments(FetchCommentsPayload payload) {
        int offset = 0;
        if (payload.loadMore) {
            offset = 20; // FIXME: do something here
        }
        if (payload.site.isWPCom()) {
            mCommentRestClient.fetchComments(payload.site, offset, CommentStatus.ALL);
        } else {
            mCommentXMLRPCClient.fetchComments(payload.site, offset, CommentStatus.ALL);
        }
    }

    private void handleFetchCommentsResponse(FetchCommentsResponsePayload payload) {
        int rowsAffected = 0;
        for (CommentModel comment : payload.comments) {
            rowsAffected += CommentSqlUtils.insertOrUpdateComment(comment);
        }
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.causeOfChange = CommentAction.FETCH_COMMENTS;
        event.error = payload.error;
        emitChange(event);
    }

    private void pushComment(RemoteCommentPayload payload) {
        if (payload.site.isWPCom()) {
            mCommentRestClient.pushComment(payload.site, payload.comment);
        } else {
            mCommentXMLRPCClient.pushComment(payload.site, payload.comment);
        }
    }

    private void handlePushCommentResponse(RemoteCommentResponsePayload payload) {
        int rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload.comment);
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.causeOfChange = CommentAction.PUSH_COMMENT;
        event.error = payload.error;
        emitChange(event);
    }

    private void fetchComment(org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload payload) {
        if (payload.site.isWPCom()) {
            mCommentRestClient.fetchComment(payload.site, payload.comment);
        } else {
            mCommentXMLRPCClient.fetchComment(payload.site, payload.comment);
        }
    }

    private void handleFetchCommentResponse(
            org.wordpress.android.fluxc.store.CommentStore.RemoteCommentResponsePayload payload) {
        int rowsAffected = CommentSqlUtils.insertOrUpdateComment(payload.comment);
        OnCommentChanged event = new OnCommentChanged(rowsAffected);
        event.causeOfChange = CommentAction.FETCH_COMMENT;
        event.error = payload.error;
        emitChange(event);
    }
}
