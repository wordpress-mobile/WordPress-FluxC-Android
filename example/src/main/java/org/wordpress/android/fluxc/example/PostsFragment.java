package org.wordpress.android.fluxc.example;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostInstantiated;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.SiteStore;

import javax.inject.Inject;

public class PostsFragment extends Fragment {
    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_posts, container, false);
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


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostChanged(OnPostChanged event) {
        if (event.isError()) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type);
            return;
        }

        SiteModel firstSite = mSiteStore.getSites().get(0);
        if (!mPostStore.getPostsForSite(firstSite).isEmpty()) {
            if (event.causeOfChange.equals(PostAction.FETCH_POSTS)
                || event.causeOfChange.equals(PostAction.FETCH_PAGES)) {
                prependToLog("Fetched " + event.rowsAffected + " posts from: " + firstSite.getName());
            } else if (event.causeOfChange.equals(PostAction.DELETE_POST)) {
                prependToLog("Post deleted!");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostInstantiated(OnPostInstantiated event) {
        PostModel examplePost = event.post;
        examplePost.setTitle("From example activity");
        examplePost.setContent("Hi there, I'm a post from FluxC!");
        examplePost.setFeaturedImageId(0);

        RemotePostPayload payload = new RemotePostPayload(examplePost, mSiteStore.getSites().get(0));
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(payload));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        prependToLog("Post uploaded! Remote post id: " + event.post.getRemotePostId());
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
