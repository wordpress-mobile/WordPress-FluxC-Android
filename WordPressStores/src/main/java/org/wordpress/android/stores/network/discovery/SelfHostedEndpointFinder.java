package org.wordpress.android.stores.network.discovery;

import android.text.TextUtils;
import android.util.Xml;
import android.webkit.URLUtil;

import com.android.volley.toolbox.RequestFuture;

import org.wordpress.android.stores.network.discovery.SelfHostedEndpointFinder.DiscoveryCallback.Error;
import org.wordpress.android.stores.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.stores.network.xmlrpc.XMLRPC;
import org.wordpress.android.stores.utils.WPUrlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.UrlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import static org.wordpress.android.stores.network.discovery.SelfHostedEndpointFinder.DiscoveryException.FailureType;

public class SelfHostedEndpointFinder {
    private final static int TIMEOUT_MS = 60000;

    public interface DiscoveryCallback {
        enum Error {
            INVALID_SOURCE_URL,
            WORDPRESS_COM_SITE,
            SSL_ERROR;
        }

        void onError(Error error, String endpoint);
        void onSuccess(String xmlrpcEndpoint, String restEndpoint);
    }

    public static class DiscoveryException extends Exception {
        public enum FailureType {
            SITE_URL_CANNOT_BE_EMPTY,
            INVALID_URL,
            MISSING_XMLRPC_METHOD,
            ERRONEOUS_SSL_CERTIFICATE,
            HTTP_AUTH_REQUIRED,
            SITE_TIME_OUT,
            NO_SITE_ERROR,
            XMLRPC_MALFORMED_RESPONSE,
            XMLRPC_ERROR
        }

        public final FailureType failureType;
        public final String failedUrl;
        public final String clientResponse;

        public DiscoveryException(FailureType failureType, String failedUrl, String clientResponse) {
            this.failureType = failureType;
            this.failedUrl = failedUrl;
            this.clientResponse = clientResponse;
        }
    }

    private DiscoveryCallback mCallback;
    private BaseXMLRPCClient mClient;

    @Inject
    public SelfHostedEndpointFinder(BaseXMLRPCClient baseXMLRPCClient) {
        mClient = baseXMLRPCClient;
    }

    public void findEndpoint(final String url, final String httpUsername, final String httpPassword,
            final DiscoveryCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String xmlRpcEndpoint = verifyOrDiscoverXMLRPCEndpoint(url, httpUsername, httpPassword, callback);
                    String wpRestEndpoint = discoverWPRESTEndpoint(url, callback);
                    mCallback.onSuccess(xmlRpcEndpoint, wpRestEndpoint);
                } catch (DiscoveryException e) {
                    // TODO: Handle reporting/tracking of XMLRPCDiscoveryException
                }
            }
        }).start();
    }

    public String verifyOrDiscoverXMLRPCEndpoint(final String siteUrl, final String httpUsername,
            final String httpPassword, final DiscoveryCallback callback) throws DiscoveryException {
        mCallback = callback;

        if (TextUtils.isEmpty(siteUrl)) {
            mCallback.onError(Error.INVALID_SOURCE_URL, siteUrl);
            return null;
        }

        if (WPUrlUtils.isWordPressCom(siteUrl)) {
            mCallback.onError(Error.WORDPRESS_COM_SITE, siteUrl);
            return null;
        }

        String xmlrpcUrl = verifyXMLRPCUrl(siteUrl, httpUsername, httpPassword);

        if (xmlrpcUrl == null) {
            AppLog.w(T.NUX, "The XML-RPC endpoint was not found by using our 'smart' cleaning approach. " +
                    "Time to start the Endpoint discovery process");
            // TODO: Remove this line once discovery process has been implemented
            mCallback.onError(Error.INVALID_SOURCE_URL, xmlrpcUrl);
            throw new DiscoveryException(FailureType.INVALID_URL, xmlrpcUrl, null);
            // Try to discover the XML-RPC Endpoint address
            // TODO: Implement discovery process
        }

        // Validate the XML-RPC URL we've found before. This check prevents a crash that can occur
        // during the setup of self-hosted sites that have malformed xmlrpc URLs in their declaration.
        if (!URLUtil.isValidUrl(xmlrpcUrl)) {
            throw new DiscoveryException(FailureType.NO_SITE_ERROR, xmlrpcUrl, null);
        }

        return xmlrpcUrl;
    }

    private  String verifyXMLRPCUrl(final String siteUrl, final String httpUsername, final String httpPassword)
            throws DiscoveryException {
        // Ordered set of Strings that contains the URLs we want to try
        final Set<String> urlsToTry = new LinkedHashSet<>();

        final String sanitizedSiteUrlHttps = sanitizeSiteUrl(siteUrl, true);
        final String sanitizedSiteUrlHttp = sanitizeSiteUrl(siteUrl, false);

        // start by adding the https URL with 'xmlrpc.php'. This will be the first URL to try.
        urlsToTry.add(DiscoveryUtils.appendXMLRPCPath(sanitizedSiteUrlHttp));
        urlsToTry.add(DiscoveryUtils.appendXMLRPCPath(sanitizedSiteUrlHttps));

        // add the sanitized https URL without the '/xmlrpc.php' suffix added to it
        urlsToTry.add(sanitizedSiteUrlHttp);
        urlsToTry.add(sanitizedSiteUrlHttps);

        // add the user provided URL as well
        urlsToTry.add(siteUrl);

        AppLog.i(T.NUX, "The app will call system.listMethods on the following URLs: " + urlsToTry);
        for (String url : urlsToTry) {
            try {
                if (checkXMLRPCEndpointValidity(url, httpUsername, httpPassword)) {
                    // Endpoint found and works fine.
                    return url;
                }
            } catch (DiscoveryException e) {
                // Stop execution for errors requiring user interaction
                // TODO: These should trigger an appropriate DiscoveryCallback.Error
                if (e.failureType == FailureType.ERRONEOUS_SSL_CERTIFICATE ||
                        e.failureType == FailureType.HTTP_AUTH_REQUIRED ||
                        e.failureType == FailureType.MISSING_XMLRPC_METHOD) {
                    throw e;
                }
                // Otherwise. swallow the error since we are just verifying various URLs
                continue;
            } catch (RuntimeException re) {
                // Depending how corrupt the user entered URL is, it can generate several kinds of runtime exceptions,
                // ignore them
                continue;
            }
        }
        // input url was not verified to be working
        return null;
    }

    // Attempts to retrieve the xmlrpc url for a self-hosted site, in this order:
    // 1: Try to retrieve it by finding the ?rsd url in the site's header
    // 2: Take whatever URL the user entered to see if that returns a correct response
    // 3: Finally, just guess as to what the xmlrpc url should be
    private String discoverXMLRPCEndpoint(String url) {
        // Attempt to get the XMLRPC URL via RSD
        String rsdUrl;
        try {
            rsdUrl = UrlUtils.addUrlSchemeIfNeeded(getRsdUrl(url), false);
        } catch (SSLHandshakeException e) {
            mCallback.onError(Error.SSL_ERROR, url);
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
            mCallback.onError(Error.SSL_ERROR, rsdUrl);
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
        RequestFuture<String> future = RequestFuture.newFuture();
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

    private String sanitizeSiteUrl(String siteUrl, boolean addHttps) throws DiscoveryException {
        // remove padding whitespace
        String url = siteUrl.trim();

        if (TextUtils.isEmpty(url)) {
            throw new DiscoveryException(FailureType.SITE_URL_CANNOT_BE_EMPTY, siteUrl, null);
        }

        // Convert IDN names to punycode if necessary
        url = UrlUtils.convertUrlToPunycodeIfNeeded(url);

        // Add http to the beginning of the URL if needed
        url = UrlUtils.addUrlSchemeIfNeeded(UrlUtils.removeScheme(url), addHttps);

        // strip url from known usual trailing paths
        url = DiscoveryUtils.stripKnownPaths(url);

        if (!(URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url))) {
            throw new DiscoveryException(FailureType.INVALID_URL, url, null);
        }

        return url;
    }

    private boolean checkXMLRPCEndpointValidity(String url, String httpUsername, String httpPassword)
            throws DiscoveryException {
        try {
            Object[] methods = doSystemListMethodsXMLRPC(url, httpUsername, httpPassword);
            if (methods == null) {
                AppLog.e(T.NUX, "The response of system.listMethods was empty for " + url);
                return false;
            }
            // Exit the loop on the first URL that replies with a XML-RPC doc.
            AppLog.i(T.NUX, "system.listMethods replied with XML-RPC objects on the URL: " + url);
            AppLog.i(T.NUX, "Validating the XML-RPC response...");
            if (DiscoveryUtils.validateListMethodsResponse(methods)) {
                // Endpoint address found and works fine.
                AppLog.i(T.NUX, "Validation ended with success! Endpoint found!");
                return true;
            } else {
                // Endpoint found, but it has problem.
                AppLog.w(T.NUX, "Validation ended with errors! Endpoint found but doesn't contain all the " +
                        "required methods.");
                throw new DiscoveryException(FailureType.MISSING_XMLRPC_METHOD, url, null);
            }
        } catch (DiscoveryException e) {
            AppLog.e(T.NUX, "system.listMethods failed on: " + url, e);
            if (DiscoveryUtils.isHTTPAuthErrorMessage(e)) {
                throw new DiscoveryException(FailureType.HTTP_AUTH_REQUIRED, url, null);
            }
        } catch (SSLHandshakeException | SSLPeerUnverifiedException e) {
            if (!WPUrlUtils.isWordPressCom(url)) {
                throw new DiscoveryException(FailureType.ERRONEOUS_SSL_CERTIFICATE, url, null);
            }
            AppLog.e(T.NUX, "SSL error. Erroneous SSL certificate detected.", e);
        } catch (IOException | XmlPullParserException e) {
            AppLog.e(T.NUX, "system.listMethods failed on: " + url, e);
            if (DiscoveryUtils.isHTTPAuthErrorMessage(e)) {
                throw new DiscoveryException(FailureType.HTTP_AUTH_REQUIRED, url, null);
            }
        } catch (IllegalArgumentException e) {
            // The XML-RPC client returns this error in case of redirect to an invalid URL.
            throw new DiscoveryException(FailureType.INVALID_URL, url, null);
        }

        return false;
    }

    private Object[] doSystemListMethodsXMLRPC(String url, String httpUsername, String httpPassword) throws
            IOException, XmlPullParserException, DiscoveryException {
        if (!UrlUtils.isValidUrlAndHostNotNull(url)) {
            AppLog.e(T.NUX, "invalid URL: " + url);
            throw new DiscoveryException(FailureType.INVALID_URL, url, null);
        }

        AppLog.i(T.NUX, "Trying system.listMethods on the following URL: " + url);

        List<Object> params = new ArrayList<>(2);
        params.add(httpUsername);
        params.add(httpPassword);

        RequestFuture<Object[]> future = RequestFuture.newFuture();
        // TODO: This might need to be modified not to emit the events to OnAuthFailedListener once SSL/HTTP AUTH are implemented
        XMLRPCRequest request = new XMLRPCRequest(url, XMLRPC.LIST_METHODS, params, future, future);
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
