package org.wordpress.android.fluxc.network.rest.wpapi;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.RequestPayload;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.OnAuthFailedListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.discovery.WPAPIHeadRequest;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;

public abstract class BaseWPAPIRestClient {
    private final RequestQueue mRequestQueue;
    private final Dispatcher mDispatcher;
    private UserAgent mUserAgent;

    public BaseWPAPIRestClient(Dispatcher dispatcher, RequestQueue requestQueue,
                               UserAgent userAgent) {
        mDispatcher = dispatcher;
        mRequestQueue = requestQueue;
        mUserAgent = userAgent;
    }

    private OnAuthFailedListener getOnAuthFailedListener(final RequestPayload requestPayload) {
        return new OnAuthFailedListener() {
            @Override
            public void onAuthFailed(AccountStore.AuthenticationError authError) {
                AuthenticateErrorPayload payload = new AuthenticateErrorPayload(requestPayload, authError);
                mDispatcher.dispatchRet(AuthenticationActionBuilder.newAuthenticateErrorAction(payload));
            }
        };
    }

    protected Request add(RequestPayload requestPayload, WPAPIGsonRequest request) {
        return mRequestQueue.add(setRequestAuthParams(requestPayload, request));
    }

    protected Request add(RequestPayload requestPayload, WPAPIHeadRequest request) {
        return mRequestQueue.add(setRequestAuthParams(requestPayload, request));
    }

    private BaseRequest setRequestAuthParams(RequestPayload requestPayload, BaseRequest request) {
        request.setOnAuthFailedListener(getOnAuthFailedListener(requestPayload));
        request.setUserAgent(mUserAgent.getUserAgent());
        return request;
    }
}
