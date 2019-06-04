package org.wordpress.android.fluxc.module;

import android.graphics.Bitmap;
import androidx.collection.LruCache;

import com.android.volley.toolbox.ImageLoader.ImageCache;

import dagger.Module;
import dagger.Provides;

@Module
public class MockedToolsModule {
    @Provides
    public ImageCache getBitmapCache() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 16;
        return new PrettyUselessCache(cacheSize);
    }

    private class PrettyUselessCache extends LruCache<String, Bitmap> implements ImageCache {
        PrettyUselessCache(int maxSize) {
            super(maxSize);
        }

        @Override
        public Bitmap getBitmap(String key) {
            return null;
        }

        @Override
        public void putBitmap(String key, Bitmap bitmap) {
            // no op
        }
    }
}
