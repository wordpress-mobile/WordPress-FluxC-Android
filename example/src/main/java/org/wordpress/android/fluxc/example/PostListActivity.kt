package org.wordpress.android.fluxc.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.post_list_activity.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.ListModel.ListType
import org.wordpress.android.fluxc.model.PostListModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostListChanged
import org.wordpress.android.fluxc.store.PostStore.OnSinglePostFetched
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

private const val LOCAL_SITE_ID = "LOCAL_SITE_ID"

class PostListActivity: AppCompatActivity() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var postStore: PostStore
    @Inject internal lateinit var siteStore: SiteStore

    private val listType = ListType.POSTS_ALL
    private lateinit var site: SiteModel
    private var postListAdapter: PostListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_list_activity)

        dispatcher.register(this)
        site = siteStore.getSiteByLocalId(intent.getIntExtra(LOCAL_SITE_ID, 0))

        setupViews()

        postListAdapter?.updateItems(postStore.getPostList(site, listType))
        dispatcher.dispatch(PostActionBuilder.newFetchPostsAction(FetchPostsPayload(site, listType)))
    }

    private fun setupViews() {
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        postListAdapter = PostListAdapter(this, postStore, site, dispatcher)
        recycler.adapter = postListAdapter

        swipeToRefresh.setOnRefreshListener {
            val payload = FetchPostsPayload(site, listType)
            dispatcher.dispatch(PostActionBuilder.newFetchPostsAction(payload))
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onPostListChanged(event: OnPostListChanged) {
        if (event.site.id != site.id || event.listType != listType || event.isError) {
            return
        }
        postListAdapter?.updateItems(postStore.getPostList(site, listType))
        swipeToRefresh.isRefreshing = false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onSinglePostFetched(event: OnSinglePostFetched) {
        if (event.isError || event.localSiteId != site.id) {
            return
        }
        postListAdapter?.updateItems(postStore.getPostList(site, listType))
    }

    companion object {
        fun newInstance(context: Context, localSiteId: Int): Intent {
            val intent = Intent(context, PostListActivity::class.java)
            intent.putExtra(LOCAL_SITE_ID, localSiteId)
            return intent
        }
    }

    private class PostListAdapter(
        context: Context,
        private val postStore: PostStore,
        private val site: SiteModel,
        private val dispatcher: Dispatcher
    ): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val layoutInflater = LayoutInflater.from(context)
        private var postList: List<PostListModel>? = null

        fun updateItems(newList: List<PostListModel>) {
            postList = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.post_list_row, parent, false)
            return PostViewHolder(view)
        }

        override fun getItemCount(): Int {
            return postList?.size ?: 0
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val postHolder = holder as PostViewHolder
            postList?.get(position)?.remotePostId?.let { remotePostId ->
                val postModel = postStore.getPostByRemotePostId(remotePostId, site)
                if (postModel != null) {
                    postHolder.postTitle.text = postModel.title
                } else {
                    val postToFetch = PostModel()
                    postToFetch.remotePostId = remotePostId
                    val payload = RemotePostPayload(postToFetch, site)
                    dispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload))
                }
            }
        }

        private class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val postTitle: TextView = itemView.findViewById(R.id.post_list_row_post_title)
        }
    }
}
