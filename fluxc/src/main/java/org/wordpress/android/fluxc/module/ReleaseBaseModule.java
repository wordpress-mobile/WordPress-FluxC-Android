package org.wordpress.android.fluxc.module;

import android.content.Context;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.persistence.room.StatsDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ReleaseBaseModule {
    @Singleton
    @Provides
    public Dispatcher provideDispatcher() {
        return new Dispatcher();
    }

    @Singleton
    @Provides
    public StatsDatabase provideStatsDatabase(Context context) {
        return StatsDatabase.Companion.build(context);
    }
}
