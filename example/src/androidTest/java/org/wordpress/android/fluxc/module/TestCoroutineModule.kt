package org.wordpress.android.fluxc.module

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.experimental.Dispatchers
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

@Module
class TestCoroutineModule {
    @Singleton
    @Provides
    fun provideCoroutineContext(): CoroutineContext {
        return Dispatchers.Unconfined
    }
}
