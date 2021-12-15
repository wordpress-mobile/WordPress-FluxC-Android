package org.wordpress.android.fluxc.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.dao.SSRDao
import javax.inject.Singleton

@Module
class WCDatabaseModule {
    @Singleton @Provides fun provideDatabase(context: Context): WCAndroidDatabase {
        return WCAndroidDatabase.buildDb(context)
    }

    @Provides internal fun provideAddonsDao(database: WCAndroidDatabase): AddonsDao {
        return database.addonsDao()
    }

    @Provides fun provideSSRDao(database: WCAndroidDatabase): SSRDao {
        return database.ssrDao()
    }

    @Provides fun provideOrdersDao(database: WCAndroidDatabase): OrdersDao {
        return database.ordersDao()
    }
}
