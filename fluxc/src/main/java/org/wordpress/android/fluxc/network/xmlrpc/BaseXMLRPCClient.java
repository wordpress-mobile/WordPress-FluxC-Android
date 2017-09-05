package org.wordpress.android.fluxc.network.xmlrpc;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.RequestPayload;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.OnAuthFailedListener;
import org.wordpress.android.fluxc.network.BaseRequest.OnParseErrorListener;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.discovery.DiscoveryRequest;
import org.wordpress.android.fluxc.network.discovery.DiscoveryXMLRPCRequest;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError;

public abstract class BaseXMLRPCClient {
    private final RequestQueue mRequestQueue;
    protected final Dispatcher mDispatcher;
    protected UserAgent mUserAgent;
    protected HTTPAuthManager mHTTPAuthManager;

    public BaseXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, UserAgent userAgent,
                            HTTPAuthManager httpAuthManager) {
        mRequestQueue = requestQueue;
        mDispatcher = dispatcher;
        mUserAgent = userAgent;
        mHTTPAuthManager = httpAuthManager;
    }

    private OnAuthFailedListener getOnAuthFailedListener(final RequestPayload requestPayload) {
        return new OnAuthFailedListener() {
            @Override
            public void onAuthFailed(AccountStore.AuthenticationError authError) {
                AuthenticateErrorPayload payload = new AuthenticateErrorPayload(requestPayload, authError);
                mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateErrorAction(payload));
            }
        };
    }

    private OnParseErrorListener getOnParseErrorListener(final RequestPayload requestPayload) {
        return new OnParseErrorListener() {
            @Override
            public void onParseError(OnUnexpectedError event) {
                event.requestPayload = requestPayload;
                mDispatcher.emitChange(event);
            }
        };
    }

    protected Request add(RequestPayload requestPayload, XMLRPCRequest request) {
        return mRequestQueue.add(setRequestAuthParams(requestPayload, request));
    }

    protected Request add(RequestPayload requestPayload, DiscoveryRequest request) {
        return mRequestQueue.add(setRequestAuthParams(requestPayload, request));
    }

    protected Request add(RequestPayload requestPayload, DiscoveryXMLRPCRequest request) {
        return mRequestQueue.add(setRequestAuthParams(requestPayload, request));
    }

    private BaseRequest setRequestAuthParams(RequestPayload requestPayload, BaseRequest request) {
        request.setOnAuthFailedListener(getOnAuthFailedListener(requestPayload));
        request.setOnParseErrorListener(getOnParseErrorListener(requestPayload));
        request.setUserAgent(mUserAgent.getUserAgent());
        request.setHTTPAuthHeaderOnMatchingURL(mHTTPAuthManager);
        return request;
    }

    protected void reportParseError(RequestPayload requestPayload, Object response, String xmlrpcUrl, Class clazz) {
        if (response == null) return;

        try {
            clazz.cast(response);
        } catch (ClassCastException e) {
            OnUnexpectedError onUnexpectedError = new OnUnexpectedError(e,
                    "XML-RPC response parse error: " + e.getMessage());
            if (xmlrpcUrl != null) {
                onUnexpectedError.addExtra(OnUnexpectedError.KEY_URL, xmlrpcUrl);
            }
            onUnexpectedError.addExtra(OnUnexpectedError.KEY_RESPONSE, response.toString());
            getOnParseErrorListener(requestPayload).onParseError(onUnexpectedError);
        }
    }
}
