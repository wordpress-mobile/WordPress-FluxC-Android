package org.wordpress.android.fluxc.module;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.network.rest.wpcom.media.RestUploadRequestBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

class FailingInterceptor implements Interceptor {
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
        } else {
            throw new IllegalStateException("Interceptor was given a request with no mocks - URL: " + requestUrl);
        }
    }

    private static Response buildMediaErrorResponse(Request request) {
        String responseJson = "{\"error\": \"invalid_token\","
                + "\"message\": \"The OAuth2 token is invalid.\""
                + "}";
        return buildResponse(request, responseJson, 404);
    }

    private static Response buildMediaSuccessResponse(Request request) {
        String responseJson = "{\"media\":[{\"ID\":9999,\"URL\":\"https:\\/\\/place.com\\/photo"
                + ".jpg\",\"guid\":\"http:\\/\\/place.com\\/photo.jpg\","
                + "\"date\":\"2017-08-11T12:17:49+00:00\",\"post_ID\":0,\"author_ID\":12345,"
                + "\"file\":\"photo.jpg\",\"mime_type\":\"image\\/jpeg\",\"extension\":\"jpg\","
                + "\"title\":\"\",\"caption\":\"Test Caption\",\"description\":\"\",\"alt\":\"\","
                + "\"icon\":\"\",\"thumbnails\":{\"thumbnail\":\"https:\\/\\/place.com\\/photo"
                + ".jpg?w=150\",\"medium\":\"https:\\/\\/place.com\\/photo.jpg?w=300\","
                + "\"large\":\"https:\\/\\/place.com\\/photo.jpg?w=880\"},\"height\":585,"
                + "\"width\":880,\"exif\":{\"aperture\":\"0\",\"credit\":\"\",\"camera\":\"\","
                + "\"caption\":\"\",\"created_timestamp\":\"0\",\"copyright\":\"\","
                + "\"focal_length\":\"0\",\"iso\":\"0\",\"shutter_speed\":\"0\",\"title\":\"\","
                + "\"orientation\":\"0\",\"keywords\":[]},\"meta\":{\"links\":{\"self\":\"\","
                + "\"help\":\"\",\"site\":\"\"}}}]}";
        return buildResponse(request, responseJson, 200);
    }

    private static Response buildPostSuccessResponse(Request request) {
        String responseJson = "{\"ID\":7970,\"type\":\"post\"}";
        return buildResponse(request, responseJson, 200);
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
}
