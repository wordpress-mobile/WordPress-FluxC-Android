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
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnSinglePostFetched
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

interface ListItemDataSource<T> {
    fun getItem(listItemModel: ListItemModel): T?
    fun loadMore()
}

private const val LOCAL_SITE_ID = "LOCAL_SITE_ID"

class PostListActivity : AppCompatActivity() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var postStore: PostStore
    @Inject internal lateinit var siteStore: SiteStore

    private val listType = ListType.POSTS_ALL
    private lateinit var site: SiteModel
    private var postListAdapter: PostListAdapter? = null
    private val listItems = ArrayList<ListItemModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_list_activity)

        dispatcher.register(this)
        site = siteStore.getSiteByLocalId(intent.getIntExtra(LOCAL_SITE_ID, 0))

        setupViews()

        dispatcher.dispatch(PostActionBuilder.newFetchPostsAction(FetchPostsPayload(site, listType)))
    }

    private fun setupViews() {
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        postListAdapter = PostListAdapter(this, getItems(), object : ListItemDataSource<PostModel> {
            override fun loadMore() {
                dispatcher.dispatch(PostActionBuilder.newFetchPostsAction(
                        FetchPostsPayload(site, listType, listItems.size)))
            }

            override fun getItem(listItemModel: ListItemModel): PostModel? {
                val postFromStore = postStore.getPostByRemotePostId(listItemModel.remoteItemId, site)
                if (postFromStore != null) {
                    return postFromStore
                }
                val postToFetch = PostModel()
                postToFetch.remotePostId = listItemModel.remoteItemId
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

    private fun getItems(): List<ListItemModel> = listStore.getListItems(site, listType)

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onListChanged(event: OnListChanged) {
        if (event.localSiteId != site.id || event.listType != listType || event.isError) {
            return
        }
        swipeToRefresh.isRefreshing = false
        postListAdapter?.setItems(getItems())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onSinglePostFetched(event: OnSinglePostFetched) {
        if (event.isError || event.localSiteId != site.id) {
            return
        }
        postListAdapter?.onItemChanged(event.remotePostId)
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
        items: List<ListItemModel>,
        private val dataSource: ListItemDataSource<PostModel>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val layoutInflater = LayoutInflater.from(context)
        private val items = ArrayList<ListItemModel>()

        init {
            this.items.addAll(items)
        }

        fun setItems(items: List<ListItemModel>) {
            this.items.clear()
            this.items.addAll(items)
            notifyDataSetChanged()
        }

        fun onItemChanged(remoteItemId: Long) {
            val index = items.indexOfFirst { it.remoteItemId == remoteItemId }
            if (index != -1) {
                notifyItemChanged(index)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.post_list_row, parent, false)
            return PostViewHolder(view)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position == items.size - 1) {
                dataSource.loadMore()
            }
            val postHolder = holder as PostViewHolder
            val postModel = dataSource.getItem(items[position])
            postHolder.postTitle.text = postModel?.title ?: ""
        }

        private class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val postTitle: TextView = itemView.findViewById(R.id.post_list_row_post_title)
        }
    }
}
