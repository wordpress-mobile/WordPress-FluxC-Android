package org.wordpress.android.fluxc.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.dao.ProductCategoriesDao
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import javax.inject.Singleton

@Module
class WCDatabaseModule {
    @Singleton @Provides fun provideDatabase(context: Context): WCAndroidDatabase {
        return WCAndroidDatabase.buildDb(context)
    }

    @Provides internal fun provideAddonsDao(database: WCAndroidDatabase): AddonsDao {
        return database.addonsDao
    }

    @Provides fun provideOrdersDao(database: WCAndroidDatabase): OrdersDao {
        return database.ordersDao
    }

    @Provides fun provideCouponsDao(database: WCAndroidDatabase): CouponsDao {
        return database.couponsDao
    }

    @Provides fun provideProductsDao(database: WCAndroidDatabase): ProductsDao {
        return database.productsDao
    }

    @Provides fun provideProductCategoriesDao(database: WCAndroidDatabase): ProductCategoriesDao {
        return database.productCategoriesDao
    }

    @Provides fun provideOrderNotesDao(database: WCAndroidDatabase) = database.orderNotesDao
}
