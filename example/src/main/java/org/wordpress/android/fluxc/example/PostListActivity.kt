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
import org.wordpress.android.fluxc.model.ListItemModel
import org.wordpress.android.fluxc.model.ListModel.ListType
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostListChanged
import org.wordpress.android.fluxc.store.PostStore.OnSinglePostFetched
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

interface PostListInterface {
    fun getItemCount(): Int
    fun getItem(position: Int): PostModel?
}

private const val LOCAL_SITE_ID = "LOCAL_SITE_ID"

class PostListActivity : AppCompatActivity() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var postStore: PostStore
    @Inject internal lateinit var siteStore: SiteStore

    private val listType = ListType.POSTS_ALL
    private lateinit var site: SiteModel
    private var postListAdapter: PostListAdapter? = null
    private val listItems = ArrayList<ListItemModel>()
    private val postMap = HashMap<Long, PostModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_list_activity)

        dispatcher.register(this)
        site = siteStore.getSiteByLocalId(intent.getIntExtra(LOCAL_SITE_ID, 0))

        setupViews()

        updatePostIds()
        dispatcher.dispatch(PostActionBuilder.newFetchPostsAction(FetchPostsPayload(site, listType)))
    }

    private fun setupViews() {
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        postListAdapter = PostListAdapter(this, object : PostListInterface {
            override fun getItemCount(): Int {
                return listItems.size
            }

            override fun getItem(position: Int): PostModel? {
                if (position == listItems.size - 1) {
                    dispatcher.dispatch(PostActionBuilder.newFetchPostsAction(FetchPostsPayload(site, listType, true)))
                }
                val remotePostId = listItems[position].remoteItemId
                val postFromMap = postMap[remotePostId]
                if (postFromMap != null) {
                    return postFromMap
                }
                val postFromStore = postStore.getPostByRemotePostId(remotePostId, site)
                if (postFromStore != null) {
                    postMap[postFromStore.remotePostId] = postFromStore
                    return postFromStore
                }
                val postToFetch = PostModel()
                postToFetch.remotePostId = remotePostId
                val payload = RemotePostPayload(postToFetch, site)
                dispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload))
                return null
            }
        })
        recycler.adapter = postListAdapter

        swipeToRefresh.setOnRefreshListener {
            val payload = FetchPostsPayload(site, listType)
            dispatcher.dispatch(PostActionBuilder.newFetchPostsAction(payload))
        }
    }

    private fun updatePostIds() {
        listItems.clear()
        listItems.addAll(postStore.getPostList(site, listType))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onPostListChanged(event: OnPostListChanged) {
        if (event.site.id != site.id || event.listType != listType || event.isError) {
            return
        }
        swipeToRefresh.isRefreshing = false
        updatePostIds()
        postListAdapter?.refresh()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onSinglePostFetched(event: OnSinglePostFetched) {
        if (event.isError || event.localSiteId != site.id) {
            return
        }
        postStore.getPostByRemotePostId(event.remotePostId, site)?.let { postModel ->
            postMap[postModel.remotePostId] = postModel
            val index = listItems.indexOfFirst { it.remoteItemId == postModel.remotePostId }
            if (index != -1) {
                postListAdapter?.refreshPosition(index)
            }
        }
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
        private val postListInterface: PostListInterface
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val layoutInflater = LayoutInflater.from(context)

        fun refresh() {
            notifyDataSetChanged()
        }

        fun refreshPosition(position: Int) {
            notifyItemChanged(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.post_list_row, parent, false)
            return PostViewHolder(view)
        }

        override fun getItemCount(): Int {
            return postListInterface.getItemCount()
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val postHolder = holder as PostViewHolder
            val postModel = postListInterface.getItem(position)
            postHolder.postTitle.text = postModel?.title ?: ""
        }

        private class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val postTitle: TextView = itemView.findViewById(R.id.post_list_row_post_title)
        }
    }
}
