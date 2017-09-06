package org.wordpress.android.fluxc.network.wporg;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.RequestPayload;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.OnAuthFailedListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;

public abstract class BaseWPOrgAPIClient {
    private final RequestQueue mRequestQueue;
    private final Dispatcher mDispatcher;
    private UserAgent mUserAgent;

    public BaseWPOrgAPIClient(Dispatcher dispatcher, RequestQueue requestQueue,
                              UserAgent userAgent) {
        mDispatcher = dispatcher;
        mRequestQueue = requestQueue;
        mUserAgent = userAgent;
    }

    private OnAuthFailedListener getOnAuthFailedListener(final RequestPayload requestPayload) {
        return new OnAuthFailedListener() {
            @Override
            public void onAuthFailed(AuthenticationError authError) {
                AuthenticateErrorPayload payload = new AuthenticateErrorPayload(requestPayload, authError);
                mDispatcher.dispatchRet(AuthenticationActionBuilder.newAuthenticateErrorAction(payload));
            }
        };
    }

    protected Request add(RequestPayload requestPayload, WPOrgAPIGsonRequest request) {
        return mRequestQueue.add(setRequestAuthParams(requestPayload, request));
    }

    private BaseRequest setRequestAuthParams(RequestPayload requestPayload, BaseRequest request) {
        request.setOnAuthFailedListener(getOnAuthFailedListener(requestPayload));
        request.setUserAgent(mUserAgent.getUserAgent());
        return request;
    }
}
