package org.wordpress.android.fluxc.example.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.example.MainExampleActivity
import org.wordpress.android.fluxc.example.PostListActivity
import org.wordpress.android.fluxc.example.WooOrderListActivity

@Module
internal abstract class MainActivityModule {
    @ContributesAndroidInjector(modules = arrayOf(FragmentsModule::class))
    abstract fun provideMainActivityInjector(): MainExampleActivity

    @ContributesAndroidInjector
    abstract fun providePostListActivityInjector(): PostListActivity

    @ContributesAndroidInjector
    abstract fun provideWooOrderListActivityInjector(): WooOrderListActivity
}
