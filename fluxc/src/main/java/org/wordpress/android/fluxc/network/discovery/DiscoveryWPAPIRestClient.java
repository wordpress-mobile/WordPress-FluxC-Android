package org.wordpress.android.fluxc.network.discovery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.network.BaseRequestFuture;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient;
import org.wordpress.android.fluxc.network.rest.wpapi.OnWPAPIErrorListener;
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest;
import org.wordpress.android.util.AppLog;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.TIMEOUT_MS;

@SuppressWarnings("CommentedOutCode")
@Singleton
public class DiscoveryWPAPIRestClient extends BaseWPAPIRestClient {
    @Inject public DiscoveryWPAPIRestClient(
            Dispatcher dispatcher,
            @Named("custom-ssl") RequestQueue requestQueue,
            UserAgent userAgent) {
        super(dispatcher, requestQueue, userAgent);
    }

    @Nullable
    public String discoverWPAPIBaseURL(@NonNull String url) {
        BaseRequestFuture<String> future = BaseRequestFuture.newFuture();
        WPAPIHeadRequest request = new WPAPIHeadRequest(url, future, future);
        add(request);
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            AppLog.e(AppLog.T.API, "Couldn't get HEAD response from server.");
        } catch (ExecutionException e) {
            // TODO: Add support for HTTP AUTH and self-signed SSL WP-API sites
//            if (e.getCause() instanceof AuthFailureError) {
//                throw new DiscoveryException(DiscoveryError.HTTP_AUTH_REQUIRED, url);
//            } else if (e.getCause() instanceof NoConnectionError && e.getCause().getCause() != null
//                    && e.getCause().getCause() instanceof SSLHandshakeException) {
//                // In the event of an SSL error we should stop attempting discovery
//                throw new DiscoveryException(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE, url);
//            }
        }
        return null;
    }

    @Nullable
    public String verifyWPAPIV2Support(@NonNull String wpApiBaseUrl) {
        BaseRequestFuture<RootWPAPIRestResponse> future = BaseRequestFuture.newFuture();
        OnWPAPIErrorListener errorListener = future::onErrorResponse;

        WPAPIGsonRequest<RootWPAPIRestResponse> request = new WPAPIGsonRequest<>(
                Request.Method.GET,
                wpApiBaseUrl,
                null,
                null,
                RootWPAPIRestResponse.class,
                future,
                errorListener
        );
        add(request);
        try {
            RootWPAPIRestResponse response = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            List<String> namespaces = response.getNamespaces();
            if (namespaces != null && !namespaces.contains("wp/v2")) {
                AppLog.i(AppLog.T.NUX, "Site does not have the full WP-API available "
                                       + "(missing wp/v2 namespace)");
                return null;
            } else {
                AppLog.i(AppLog.T.NUX, "Found valid WP-API endpoint! - " + wpApiBaseUrl);
                // TODO: Extract response.authentication and float it up
                return wpApiBaseUrl;
            }
        } catch (InterruptedException | TimeoutException e) {
            AppLog.e(AppLog.T.API, "Couldn't get response from root endpoint.");
        } catch (ExecutionException e) {
            // TODO: Add support for HTTP AUTH and self-signed SSL WP-API sites
//            if (e.getCause() instanceof AuthFailureError) {
//                throw new DiscoveryException(DiscoveryError.HTTP_AUTH_REQUIRED, url);
//            } else if (e.getCause() instanceof NoConnectionError && e.getCause().getCause() != null
//                    && e.getCause().getCause() instanceof SSLHandshakeException) {
//                // In the event of an SSL error we should stop attempting discovery
//                throw new DiscoveryException(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE, url);
//            }
        }
        return null;
    }
}
