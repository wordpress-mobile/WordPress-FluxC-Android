package org.wordpress.android.stores.network.discovery;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.wordpress.android.stores.action.AuthenticationAction;
import org.wordpress.android.stores.network.xmlrpc.XMLRPC;
import org.wordpress.android.stores.network.xmlrpc.XMLRPCRequest;

import java.util.List;

/**
 * A custom XMLRPCRequest intended for XML-RPC discovery, which doesn't emit global
 * {@link AuthenticationAction#AUTHENTICATE_ERROR} events.
 */
public class DiscoveryXMLRPCRequest extends XMLRPCRequest {
    private final ErrorListener mErrorListener;

    public DiscoveryXMLRPCRequest(String url, XMLRPC method, List<Object> params, Listener listener,
                                  ErrorListener errorListener) {
        super(url, method, params, listener, errorListener);
        mErrorListener = errorListener;
    }

    @Override
    public void deliverError(VolleyError error) {
        if (mErrorListener != null) {
            mErrorListener.onErrorResponse(error);
        }
    }
}
