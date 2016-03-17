package org.wordpress.android.stores.network.discovery;

import android.util.Xml;
import android.webkit.URLUtil;

import org.wordpress.android.stores.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.stores.utils.WPUrlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.UrlUtils;

import org.wordpress.android.stores.network.discovery.SelfHostedEndpointFinder.DiscoveryCallback.Error;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;

public class SelfHostedEndpointFinder {
    private final static int TIMEOUT_MS = 60000;

    public interface DiscoveryCallback {
        enum Error {
            INVALID_SOURCE_URL,
            WORDPRESS_COM_SITE,
            SSL_ERROR;
        }

        void onError(Error error);
        void onSuccess(String xmlrpcEndpoint, String restEndpoint);
    }

    private DiscoveryCallback mCallback;
    private BaseXMLRPCClient mClient;

    @Inject
    public SelfHostedEndpointFinder(BaseXMLRPCClient baseXMLRPCClient) {
        mClient = baseXMLRPCClient;
    }

    public void findEndpoint(final String url, final DiscoveryCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String xmlRpcEndpoint = discoverXMLRPCEndpoint(url, callback);
                String wpRestEndpoint = discoverWPRESTEndpoint(url, callback);
                mCallback.onSuccess(xmlRpcEndpoint, wpRestEndpoint);
            }
        }).start();
    }

    // Attempts to retrieve the xmlrpc url for a self-hosted site, in this order:
    // 1: Try to retrieve it by finding the ?rsd url in the site's header
    // 2: Take whatever URL the user entered to see if that returns a correct response
    // 3: Finally, just guess as to what the xmlrpc url should be
    private String discoverXMLRPCEndpoint(String url, final DiscoveryCallback callback) {
        mCallback = callback;

        // Convert IDN names to punycode if necessary
        url = UrlUtils.convertUrlToPunycodeIfNeeded(url);

        // Add http to the beginning of the URL if needed
        url = UrlUtils.addUrlSchemeIfNeeded(url, false);

        if (WPUrlUtils.isWordPressCom(url)) {
            mCallback.onError(Error.WORDPRESS_COM_SITE);
            return null;
        }

        if (!URLUtil.isValidUrl(url)) {
            mCallback.onError(Error.INVALID_SOURCE_URL);
            return null;
        }

        // Attempt to get the XMLRPC URL via RSD
        String rsdUrl;
        try {
            rsdUrl = UrlUtils.addUrlSchemeIfNeeded(getRsdUrl(url), false);
        } catch (SSLHandshakeException e) {
            mCallback.onError(Error.SSL_ERROR);
            return null;
        }

        try {
            if (rsdUrl != null) {
                url = UrlUtils.addUrlSchemeIfNeeded(getXMLRPCUrl(rsdUrl), false);
                if (url == null) {
                    url = UrlUtils.addUrlSchemeIfNeeded(rsdUrl.replace("?rsd", ""), false);
                }
            }
        } catch (SSLHandshakeException e) {
            mCallback.onError(Error.SSL_ERROR);
            return null;
        }

        return(url);
    }

    private String discoverWPRESTEndpoint(String url, final DiscoveryCallback callback) {
        // TODO: See http://v2.wp-api.org/guide/discovery/
        return url + "/wp-json/wp/v2/";
    }

    private String getRsdUrl(String baseUrl) throws SSLHandshakeException {
        String rsdUrl;
        rsdUrl = getRSDMetaTagHrefRegEx(baseUrl);
        if (rsdUrl == null) {
            rsdUrl = getRSDMetaTagHref(baseUrl);
        }
        return rsdUrl;
    }

    /**
     * Regex pattern for matching the RSD link found in most WordPress sites.
     */
    private static final Pattern rsdLink = Pattern.compile(
            "<link\\s*?rel=\"EditURI\"\\s*?type=\"application/rsd\\+xml\"\\s*?title=\"RSD\"\\s*?href=\"(.*?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Returns RSD URL based on regex match.
     */
    private String getRSDMetaTagHrefRegEx(String urlString)
            throws SSLHandshakeException {
        String html = getResponse(urlString);
        if (html != null) {
            Matcher matcher = rsdLink.matcher(html);
            if (matcher.find()) {
                String href = matcher.group(1);
                return href;
            }
        }
        return null;
    }

    /**
     * Returns RSD URL based on html tag search.
     */
    private String getRSDMetaTagHref(String urlString)
            throws SSLHandshakeException {
        // get the html code
        String data = getResponse(urlString);

        // parse the html and get the attribute for xmlrpc endpoint
        if (data != null) {
            StringReader stringReader = new StringReader(data);
            XmlPullParser parser = Xml.newPullParser();
            try {
                // auto-detect the encoding from the stream
                parser.setInput(stringReader);
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String name = null;
                    String rel = "";
                    String type = "";
                    String href = "";
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            name = parser.getName();
                            if (name.equalsIgnoreCase("link")) {
                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                    String attrName = parser.getAttributeName(i);
                                    String attrValue = parser.getAttributeValue(i);
                                    if (attrName.equals("rel")) {
                                        rel = attrValue;
                                    } else if (attrName.equals("type"))
                                        type = attrValue;
                                    else if (attrName.equals("href"))
                                        href = attrValue;
                                }

                                if (rel.equals("EditURI") && type.equals("application/rsd+xml")) {
                                    return href;
                                }
                            }
                            break;
                    }
                    eventType = parser.next();
                }
            } catch (XmlPullParserException e) {
                AppLog.e(T.API, e);
                return null;
            } catch (IOException e) {
                AppLog.e(T.API, e);
                return null;
            }
        }
        return null; // never found the rsd tag
    }

    /**
     * Discover the XML-RPC endpoint for the WordPress API associated with the specified blog URL.
     *
     * @param urlString URL of the blog to get the XML-RPC endpoint for.
     * @return XML-RPC endpoint for the specified blog, or null if unable to discover endpoint.
     */
    private String getXMLRPCUrl(String urlString) throws SSLHandshakeException {
        Pattern xmlrpcLink = Pattern.compile("<api\\s*?name=\"WordPress\".*?apiLink=\"(.*?)\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        String html = getResponse(urlString);
        if (html != null) {
            Matcher matcher = xmlrpcLink.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null; // never found the rsd tag
    }

    /**
     * Obtain the HTML response from a GET request for the given URL.
     */
    private String getResponse(String url) {
        DiscoveryRequestFuture<String> future = DiscoveryRequestFuture.newFuture();
        DiscoveryRequest request = new DiscoveryRequest(url, future, future);
        mClient.add(request);
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            AppLog.e(T.API, "Couldn't get XML-RPC response.");
        } catch (ExecutionException e) {
            // TODO: Handle redirect errors
        } catch (TimeoutException e) {
            AppLog.e(T.API, "Couldn't get XML-RPC response.");
        }
        return null;
    }
}
