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
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.ListType.POST
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

private const val LOCAL_SITE_ID = "LOCAL_SITE_ID"

class PostListActivity : AppCompatActivity() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var postStore: PostStore
    @Inject internal lateinit var siteStore: SiteStore

    private val listDescriptor: ListDescriptor
        get() = ListDescriptor(type = POST, localSiteId = site.id)
    private lateinit var site: SiteModel
    private var postListAdapter: PostListAdapter? = null
    private lateinit var listManager: ListManager<PostModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_list_activity)

        dispatcher.register(this)
        site = siteStore.getSiteByLocalId(intent.getIntExtra(LOCAL_SITE_ID, 0))
        listManager = getListDataFromStore()

        setupViews()

        listManager.refresh()
    }

    private fun setupViews() {
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        postListAdapter = PostListAdapter(this, listManager)
        recycler.adapter = postListAdapter

        swipeToRefresh.setOnRefreshListener {
            listManager.refresh()
        }
    }

    private fun refreshListData() {
        listManager = getListDataFromStore()
        swipeToRefresh.isRefreshing = listManager.isFetchingFirstPage
        loadingMoreProgressBar.visibility = if (listManager.isLoadingMore) View.VISIBLE else View.GONE
        postListAdapter?.setListManager(listManager)
    }

    private fun getListDataFromStore(): ListManager<PostModel> =
        listStore.getListManager(listDescriptor, object : ListItemDataSource<PostModel> {
            override fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long) {
                val postToFetch = PostModel()
                postToFetch.remotePostId = remoteItemId
                val payload = RemotePostPayload(postToFetch, site)
                dispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload))
            }

            override fun getItem(listDescriptor: ListDescriptor, remoteItemId: Long): PostModel? {
                return postStore.getPostByRemotePostId(remoteItemId, site)
            }
        })

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onListChanged(event: OnListChanged) {
        if (!event.listDescriptors.contains(listDescriptor)) {
            return
        }
        refreshListData()
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    @Suppress("unused")
//    fun onSinglePostFetched(event: OnSinglePostFetched) {
//        if (event.isError || event.localSiteId != site.id) {
//            return
//        }
//        postListAdapter?.onItemChanged(event.remotePostId)
//    }

    companion object {
        fun newInstance(context: Context, localSiteId: Int): Intent {
            val intent = Intent(context, PostListActivity::class.java)
            intent.putExtra(LOCAL_SITE_ID, localSiteId)
            return intent
        }
    }

    private class PostListAdapter(
        context: Context,
        private var listManager: ListManager<PostModel>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val layoutInflater = LayoutInflater.from(context)

        fun setListManager(listManager: ListManager<PostModel>) {
            val shouldUpdate = this.listManager.hasDataChanged(listManager)
            this.listManager = listManager
            if (shouldUpdate) {
                notifyDataSetChanged()
            }
        }

        fun onItemChanged(remoteItemId: Long) {
            val index = listManager.indexOfItem(remoteItemId)
            if (index != null) {
                notifyItemChanged(index)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.post_list_row, parent, false)
            return PostViewHolder(view)
        }

        override fun getItemCount(): Int {
            return listManager.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val postHolder = holder as PostViewHolder
            val postModel = listManager.getRemoteItem(position)
            postHolder.postTitle.text = postModel?.title ?: "Loading.."
        }

        private class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val postTitle: TextView = itemView.findViewById(R.id.post_list_row_post_title)
        }
    }
}
