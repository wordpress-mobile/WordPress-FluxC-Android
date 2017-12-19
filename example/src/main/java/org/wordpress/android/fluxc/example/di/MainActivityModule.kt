package org.wordpress.android.fluxc.example.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.example.MainExampleActivity

@Module
internal abstract class MainActivityModule {
    @ContributesAndroidInjector(modules = arrayOf(FragmentsModule::class))
    abstract fun provideMainActivityInjector(): MainExampleActivity
}
