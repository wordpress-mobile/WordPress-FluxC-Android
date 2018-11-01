package org.wordpress.android.fluxc.persistence

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.wellsql.generated.StatsBlockTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

@Singleton
class StatsSqlUtils
@Inject constructor(private val gson: Gson, private val coroutineContext: CoroutineContext) {
    private val liveData = mutableMapOf<Key, MutableLiveData<*>>()
    fun <T> insert(site: SiteModel, type: Key, item: T) {
        val json = gson.toJson(item)
        WellSql.delete(StatsBlockBuilder::class.java)
                .where()
                .equals(StatsBlockTable.TYPE, type.name)
                .endWhere()
                .execute()
        WellSql.insert(StatsBlockBuilder(localSiteId = site.id, type = type.name, json = json))
                .execute()
        GlobalScope.launch(Dispatchers.Main) {
            liveData.getOrPutAndCast<T>(type).value = item
        }
    }

    fun <T> select(site: SiteModel, type: Key, classOfT: Class<T>): T? {
        val model = WellSql.select(StatsBlockBuilder::class.java)
                .where()
                .equals(StatsBlockTable.LOCAL_SITE_ID, site.id)
                .equals(StatsBlockTable.TYPE, type.name)
                .endWhere().asModel.firstOrNull()
        return model?.let { gson.fromJson(model.json, classOfT) }
    }

    fun <T> liveData(site: SiteModel, type: Key, classOfT: Class<T>): LiveData<T> {
        if (!liveData.containsKey(type)) {
            val mutableLiveData = liveData.getOrPutAndCast<T>(type)
            GlobalScope.launch(coroutineContext) {
                val data = select(site, type, classOfT)
                withContext(Dispatchers.Main) {
                    mutableLiveData.value = data
                }
            }
        }
        return liveData.getOrPutAndCast<T>(type)
    }

    private fun <T> MutableMap<Key, MutableLiveData<*>>.getOrPutAndCast(type: Key) =
            this.getOrPut(type) { MutableLiveData<T>() } as MutableLiveData<T>

    @Table(name = "StatsBlock")
    data class StatsBlockBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var type: String,
        @Column var json: String
    ) : Identifiable {
        constructor() : this(-1, -1, "", "")

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }

    enum class Key {
        ALL_TIME_INSIGHTS, MOST_POPULAR_INSIGHTS, LATEST_POST_DETAIL_INSIGHTS, LATEST_POST_STATS_INSIGHTS
    }
}
