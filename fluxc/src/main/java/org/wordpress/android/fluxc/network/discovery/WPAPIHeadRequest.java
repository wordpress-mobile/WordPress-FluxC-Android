package org.wordpress.android.fluxc.network.discovery;

import android.support.annotation.NonNull;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import org.wordpress.android.fluxc.network.BaseRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WPAPIHeadRequest extends BaseRequest<String> {
    private static final Pattern LINK_PATTERN = Pattern.compile("^<(.*)>; rel=\"https://api.w.org/\"$");

    private final Listener<String> mListener;
    private String mResponseLinkHeader;

    public WPAPIHeadRequest(String url, Listener<String> listener, BaseErrorListener errorListener) {
        super(Method.HEAD, url, errorListener);
        mListener = listener;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(mResponseLinkHeader);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        // Note: sometimes there are multiple "link" headers in the response. We don't use response.headers
        // because it will only populate the last "link" value. Instead, we loop over the link items in the
        // header response to find the appropriate one.
        for (int i = 0; i < response.allHeaders.size(); i++) {
            if (response.allHeaders.get(i).getName().equals("link")) {
                mResponseLinkHeader = extractEndpointFromLinkHeader(response.allHeaders.get(i).getValue());
                if (mResponseLinkHeader != null) {
                    break;
                }
            }
        }
        return Response.success("", HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        // no op
        return error;
    }

    private static String extractEndpointFromLinkHeader(String linkHeader) {
        if (linkHeader != null) {
            Matcher matcher = LINK_PATTERN.matcher(linkHeader);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
