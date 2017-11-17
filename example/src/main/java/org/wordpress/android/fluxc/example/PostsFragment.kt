package org.wordpress.android.fluxc.example

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.fragment_posts.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.PostStore.OnPostsSearched
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.PostStore.SearchPostsPayload
import org.wordpress.android.fluxc.store.SiteStore
import java.util.Collections
import javax.inject.Inject

class PostsFragment : Fragment() {
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var postStore: PostStore
    @Inject internal lateinit var dispatcher: Dispatcher

    private var searchOffset = 0

    override fun onAttach(context: Context?) {
        AndroidInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.fragment_posts, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_first_site_posts.setOnClickListener {
            val payload = FetchPostsPayload(getFirstSite())
            dispatcher.dispatch(PostActionBuilder.newFetchPostsAction(payload))
        }

        create_new_post_first_site.setOnClickListener {
            val examplePost = postStore.instantiatePostModel(getFirstSite(), false)
            examplePost.title = "From example activity"
            examplePost.content = "Hi there, I'm a post from FluxC!"
            examplePost.featuredImageId = 0
            val payload = RemotePostPayload(examplePost, getFirstSite())
            dispatcher.dispatch(PostActionBuilder.newPushPostAction(payload))
        }

        delete_a_post_from_first_site.setOnClickListener {
            val firstSite = getFirstSite()
            val posts = postStore.getPostsForSite(firstSite)
            Collections.sort(posts) { lhs, rhs -> (rhs.remotePostId - lhs.remotePostId).toInt() }
            if (!posts.isEmpty()) {
                val payload = RemotePostPayload(posts[0], firstSite)
                dispatcher.dispatch(PostActionBuilder.newDeletePostAction(payload))
            }
        }

        search_posts.setOnClickListener { searchPosts(search_posts_query.text.toString(), 0) }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        val firstSite = getFirstSite()
        if (!postStore.getPostsForSite(firstSite).isEmpty()) {
            if (event.causeOfChange == PostAction.FETCH_POSTS || event.causeOfChange == PostAction.FETCH_PAGES) {
                prependToLog("Fetched " + event.rowsAffected + " posts from: " + firstSite.name)
            } else if (event.causeOfChange == PostAction.DELETE_POST) {
                prependToLog("Post deleted!")
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        prependToLog("Post uploaded! Remote post id: " + event.post.remotePostId)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostsSearched(event: OnPostsSearched) {
        if (event.isError) {
            prependToLog("Error searching posts: " + event.error.type)
            return
        }

        val results = if (event.searchResults != null) event.searchResults.posts else null
        val resultCount = results?.size ?: 0
        prependToLog("Found $resultCount posts from the search.")

        if (event.canLoadMore) {
            prependToLog("Can search more posts, dispatching...")
            searchOffset += resultCount
            searchPosts(event.searchTerm, searchOffset)
        } else {
            searchOffset = 0
        }
    }

    private fun searchPosts(searchQuery: String, offset: Int) {
        val payload = SearchPostsPayload(getFirstSite(), searchQuery, offset)
        dispatcher.dispatch(PostActionBuilder.newSearchPostsAction(payload))
    }

    private fun getFirstSite(): SiteModel = siteStore.sites[0]

    private fun prependToLog(s: String) = (activity as MainExampleActivity).prependToLog(s)
}
