package org.wordpress.android.fluxc.example;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;

import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class CommentsFragment extends Fragment {
    @Inject SiteStore mSiteStore;
    @Inject CommentStore mCommentStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_comments, container, false);
        view.findViewById(R.id.fetch_comments).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchCommentsFirstSite();
            }
        });
        view.findViewById(R.id.reply_to_comment).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getFirstComment() == null) {
                    ToastUtils.showToast(getActivity(), "Fetch comments first");
                    return;
                }
                replyToFirstComment();
            }
        });
        view.findViewById(R.id.like_comment).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getFirstComment() == null) {
                    ToastUtils.showToast(getActivity(), "Fetch comments first");
                    return;
                }
                likeOrUnlikeFirstComment();
            }
        });
        view.findViewById(R.id.show_link_address).setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                if (getFirstComment() == null) {
                    ToastUtils.showToast(getActivity(), "Fetch comments first");
                    return;
                }
                showLinkAddress();
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    private SiteModel getFirstSite() {
        return mSiteStore.getSites().get(0);
    }

    private CommentModel getFirstComment() {
        List<CommentModel> comments = mCommentStore.getCommentsForSite(getFirstSite(), false, CommentStatus.ALL);
        if (comments.size() == 0) {
            prependToLog("There is no comments on this site or comments haven't been fetched.");
            return null;
        }
        return mCommentStore.getCommentsForSite(getFirstSite(), false, CommentStatus.ALL).get(0);
    }

    private void fetchCommentsFirstSite() {
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(
                new FetchCommentsPayload(getFirstSite(), 5, 0)));
    }

    private void replyToFirstComment() {
        CommentModel comment = new CommentModel();
        comment.setRemoteSiteId(getFirstSite().getSiteId());
        comment.setContent("I'm a new comment id: " + new Random().nextLong() + " from FluxC Example App");
        mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(
                new RemoteCreateCommentPayload(getFirstSite(), getFirstComment(), comment)
        ));
    }

    private void likeOrUnlikeFirstComment() {
        mDispatcher.dispatch(CommentActionBuilder.newLikeCommentAction(
                new CommentStore.RemoteLikeCommentPayload(getFirstSite(), getFirstComment(),
                        !getFirstComment().getILike())));
    }

    private void showLinkAddress() {
        CommentModel commentModel = getFirstComment();
        ToastUtils.showToast(getActivity(), commentModel.getUrl());
        prependToLog("Comment link address " + commentModel.getUrl());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCommentChanged(OnCommentChanged event) {
        if (event.isError()) {
            String error = "Error: " + event.error.type + " - " + event.error.message;
            prependToLog(error);
            AppLog.i(T.TESTS, error);
        } else {
            if (event.causeOfChange == CommentAction.LIKE_COMMENT) {
                prependToLog("Comment " + (getFirstComment().getILike() ? "liked ツ" : "unliked (ಥ﹏ಥ)"));
            } else {
                prependToLog("OnCommentChanged: rowsAffected=" + event.rowsAffected);
                List<CommentModel> comments = mCommentStore.getCommentsForSite(getFirstSite(), false,
                        CommentStatus.ALL);
                for (CommentModel comment : comments) {
                    prependToLog(comment.getAuthorName() + " @" + comment.getDatePublished());
                }
            }
        }
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
