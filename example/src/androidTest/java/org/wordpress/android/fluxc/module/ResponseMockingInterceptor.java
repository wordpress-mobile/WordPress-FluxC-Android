package org.wordpress.android.fluxc.module;

import android.net.Uri;
import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.network.rest.wpcom.media.RestUploadRequestBody;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.annotation.Nullable;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

class ResponseMockingInterceptor implements Interceptor {
    @Override
    public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        // Give some time to create a realistic network event
        TestUtils.waitFor(1000);

        String requestUrl = request.url().toString();

        if (requestUrl.contains("media/new")) {
            // WP.com media upload request
            RestUploadRequestBody requestBody = (RestUploadRequestBody) request.body();
            if (requestBody != null
                    && requestBody.getMedia().getAuthorId() == MockedNetworkModule.MEDIA_FAILURE_AUTHOR_CODE) {
                return buildMediaErrorResponse(request);
            } else {
                return buildMediaSuccessResponse(request);
            }
        } else if (requestUrl.contains("posts/new")) {
            // WP.com post upload request
            return buildPostSuccessResponse(request);
        } else if (requestUrl.contains("jetpack-blogs")) {
            String path = "";
            String pathRequestParam = Uri.parse(requestUrl).getQueryParameter("path");
            if (pathRequestParam != null) {
                // GET request
                String[] params = pathRequestParam.split("&");
                path = params[0];
            } else {
                // POST request
                // TODO
            }
            switch (path) {
                case "/":
                    if (requestUrl.contains(String.valueOf(MockedNetworkModule.FAILURE_SITE_ID))) {
                        return buildJetpackTunnelRootFailureResponse(request);
                    } else {
                        return buildJetpackTunnelRootSuccessResponse(request);
                    }
            }
        }
        throw new IllegalStateException("Interceptor was given a request with no mocks - URL: " + requestUrl);
    }

    private Response buildMediaErrorResponse(Request request) {
        return buildErrorResponse(request, "media-upload-response-failure.json", 404);
    }

    private Response buildMediaSuccessResponse(Request request) {
        return buildSuccessResponse(request, "media-upload-response-success.json");
    }

    private Response buildPostSuccessResponse(Request request) {
        return buildSuccessResponse(request, "post-upload-response-success.json");
    }

    private Response buildJetpackTunnelRootSuccessResponse(Request request) {
        return buildSuccessResponse(request, "jetpack-tunnel-root-response-success.json");
    }

    private Response buildJetpackTunnelRootFailureResponse(Request request) {
        return buildErrorResponse(request, "jetpack-tunnel-root-response-failure.json", 404);
    }

    private Response buildSuccessResponse(Request request, String resourceFileName) {
        String responseJson = getStringFromResourceFile(resourceFileName);
        return buildResponse(request, responseJson, 200);
    }

    @SuppressWarnings("SameParameterValue")
    private Response buildErrorResponse(Request request, String resourceFileName, int errorCode) {
        String responseJson = getStringFromResourceFile(resourceFileName);
        return buildResponse(request, responseJson, errorCode);
    }

    private static Response buildResponse(Request request, final String responseJson, int responseCode) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("")
                .body(new ResponseBody() {
                    @Nullable
                    @Override
                    public MediaType contentType() {
                        return null;
                    }

                    @Override
                    public long contentLength() {
                        return -1;
                    }

                    @Override
                    public BufferedSource source() {
                        try {
                            InputStream stream = new ByteArrayInputStream(responseJson.getBytes("UTF-8"));
                            return Okio.buffer(Okio.source(stream));
                        } catch (UnsupportedEncodingException e) {
                            return null;
                        }
                    }
                })
                .code(responseCode)
                .build();
    }

    private String getStringFromResourceFile(String filename) {
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(filename);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            StringBuilder buffer = new StringBuilder();
            String lineString;

            while ((lineString = bufferedReader.readLine()) != null) {
                buffer.append(lineString);
            }

            bufferedReader.close();
            return buffer.toString();
        } catch (IOException e) {
            AppLog.e(T.TESTS, "Could not load response JSON file.");
            return null;
        }
    }
}
