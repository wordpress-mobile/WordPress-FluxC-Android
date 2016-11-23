package org.wordpress.android.fluxc.example;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.InstantiateCommentPayload;
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.fluxc.store.CommentStore.OnCommentInstantiated;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;

public class CommentsFragment extends Fragment {
    @Inject SiteStore mSiteStore;
    @Inject CommentStore mCommentStore;
    @Inject Dispatcher mDispatcher;

    // Needed for instantiate action :/
    private CommentModel mNewComment;
    private CountDownLatch mCountDownLatch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
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
        List<CommentModel> comments = mCommentStore.getCommentsForSite(getFirstSite(), CommentStatus.ALL);
        if (comments.size() == 0) {
            prependToLog("There is no comments on this site or comments haven't been fetched.");
            return null;
        }
        return mCommentStore.getCommentsForSite(getFirstSite(), CommentStatus.ALL).get(0);
    }

    private void fetchCommentsFirstSite() {
        mDispatcher.dispatch(CommentActionBuilder.newFetchCommentsAction(
                new FetchCommentsPayload(getFirstSite(), 5, 0)));
    }

    private void replyToFirstComment() {
        InstantiateCommentPayload payload = new InstantiateCommentPayload(getFirstSite());
        // Count down latch used for waiting for the asynchronous Instantiate action.
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(CommentActionBuilder.newInstantiateCommentAction(payload));
        try {
            assertEquals(true, mCountDownLatch.await(2, TimeUnit.SECONDS));
            mNewComment.setContent("I'm a new comment id: " + new Random().nextLong() + " from FluxC Example App");
            mDispatcher.dispatch(CommentActionBuilder.newCreateNewCommentAction(
                    new RemoteCreateCommentPayload(getFirstSite(), getFirstComment(), mNewComment)
            ));
        } catch (Exception e) {
            // noop
        }
    }

    private void likeOrUnlikeFirstComment() {
        mDispatcher.dispatch(CommentActionBuilder.newLikeCommentAction(
                new CommentStore.RemoteLikeCommentPayload(getFirstSite(), getFirstComment(),
                        !getFirstComment().getILike())));
    }

    @Subscribe
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
                List<CommentModel> comments = mCommentStore.getCommentsForSite(getFirstSite(), CommentStatus.ALL);
                for (CommentModel comment : comments) {
                    prependToLog(comment.getAuthorName() + " @" + comment.getDatePublished());
                }
            }
        }
    }

    @Subscribe
    public void onCommentInstantiated(OnCommentInstantiated event) {
        mNewComment = event.comment;
        mCountDownLatch.countDown();
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
