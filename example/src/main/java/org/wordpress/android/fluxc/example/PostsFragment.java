package org.wordpress.android.fluxc.example;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.OnPostsSearched;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.PostStore.SearchPostsPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.ToastUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

public class PostsFragment extends Fragment {
    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;

    private int mSearchOffset = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getActivity().getApplication()).getComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_posts, container, false);
        view.findViewById(R.id.fetch_first_site_posts).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FetchPostsPayload payload = new FetchPostsPayload(getFirstSite());
                mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(payload));
            }
        });

        view.findViewById(R.id.create_new_post_first_site).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PostModel examplePost = mPostStore.instantiatePostModel(getFirstSite(), false);
                examplePost.setTitle("From example activity");
                examplePost.setContent("Hi there, I'm a post from FluxC!");
                examplePost.setFeaturedImageId(0);
                RemotePostPayload payload = new RemotePostPayload(examplePost, getFirstSite());
                mDispatcher.dispatch(PostActionBuilder.newPushPostAction(payload));
            }
        });

        view.findViewById(R.id.delete_a_post_from_first_site).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel firstSite = getFirstSite();
                List<PostModel> posts = mPostStore.getPostsForSite(firstSite);
                Collections.sort(posts, new Comparator<PostModel>() {
                    @Override
                    public int compare(PostModel lhs, PostModel rhs) {
                        return (int) (rhs.getRemotePostId() - lhs.getRemotePostId());
                    }
                });
                if (!posts.isEmpty()) {
                    RemotePostPayload payload = new RemotePostPayload(posts.get(0), firstSite);
                    mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(payload));
                }
            }
        });

        final TextView searchQuery = (TextView) view.findViewById(R.id.search_posts_query);
        view.findViewById(R.id.search_posts).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (searchQuery == null) {
                    ToastUtils.showToast(getActivity(), "Couldn't find EditText, refresh fragment");
                    return;
                }
                searchPosts(searchQuery.getText().toString(), 0);
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


    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostChanged(OnPostChanged event) {
        if (event.isError()) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type);
            return;
        }

        SiteModel firstSite = getFirstSite();
        if (!mPostStore.getPostsForSite(firstSite).isEmpty()) {
            if (event.causeOfChange.equals(PostAction.FETCH_POSTS)
                || event.causeOfChange.equals(PostAction.FETCH_PAGES)) {
                prependToLog("Fetched " + event.rowsAffected + " posts from: " + firstSite.getName());
            } else if (event.causeOfChange.equals(PostAction.DELETE_POST)) {
                prependToLog("Post deleted!");
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        prependToLog("Post uploaded! Remote post id: " + event.post.getRemotePostId());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostsSearched(OnPostsSearched event) {
        if (event.isError()) {
            prependToLog("Error searching posts: " + event.error.type);
            return;
        }

        List<PostModel> results = event.searchResults != null ? event.searchResults.getPosts() : null;
        int resultCount = results == null ? 0 : results.size();
        prependToLog("Found " + resultCount + " posts from the search.");

        if (event.canLoadMore) {
            prependToLog("Can search more posts, dispatching...");
            mSearchOffset += resultCount;
            searchPosts(event.searchTerm, mSearchOffset);
        } else {
            mSearchOffset = 0;
        }
    }

    private void searchPosts(String searchQuery, int offset) {
        SearchPostsPayload payload = new SearchPostsPayload(getFirstSite(), searchQuery, offset);
        mDispatcher.dispatch(PostActionBuilder.newSearchPostsAction(payload));
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }

    private SiteModel getFirstSite() {
        return mSiteStore.getSites().get(0);
    }
}
