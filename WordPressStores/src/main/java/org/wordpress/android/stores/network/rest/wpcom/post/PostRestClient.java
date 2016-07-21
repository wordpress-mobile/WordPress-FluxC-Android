package org.wordpress.android.stores.network.rest.wpcom.post;

import com.android.volley.RequestQueue;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.AppSecrets;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PostRestClient extends BaseWPComRestClient {
    @Inject
    public PostRestClient(Dispatcher dispatcher, RequestQueue requestQueue, AppSecrets appSecrets,
                          AccessToken accessToken, UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
    }
}
