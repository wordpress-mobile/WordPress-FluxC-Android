package org.wordpress.android.fluxc.example.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.example.WCOrderListActivity

@Module
internal interface WCOrderListActivityModule {
    @ContributesAndroidInjector
    fun provideWCOrderListActivityInjector(): WCOrderListActivity
}
