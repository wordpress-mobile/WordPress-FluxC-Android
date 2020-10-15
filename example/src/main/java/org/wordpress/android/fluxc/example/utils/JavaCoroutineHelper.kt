package org.wordpress.android.fluxc.example.utils

import androidx.core.util.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.XPostsStore

object JavaCoroutineHelper {
    fun fetchXposts(
        xPostsStore: XPostsStore,
        site: SiteModel,
        consumer: Consumer<String>
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            val response = xPostsStore.fetchXPosts(site)
            consumer.accept(response.toString())
        }
    }
}
