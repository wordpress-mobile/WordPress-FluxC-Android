package org.wordpress.android.fluxc.network.rest.wpcom.media;

import org.wordpress.android.fluxc.network.BaseRequest;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class SharedOkHttpClientForMedia {
    private static OkHttpClient mOkHttpClient;

    public static OkHttpClient getOkHttpClientSharedInstance(OkHttpClient.Builder builder) {
        if (mOkHttpClient == null) {
            mOkHttpClient = builder
                    .connectTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                    .build();
        }
        return mOkHttpClient;
    }
}
