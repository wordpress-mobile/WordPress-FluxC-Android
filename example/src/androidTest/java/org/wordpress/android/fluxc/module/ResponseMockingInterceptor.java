package org.wordpress.android.fluxc.module;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

@Singleton
public class ResponseMockingInterceptor implements Interceptor {
    private String mNextResponseFile;
    private int mNextResponseErrorCode;

    public void respondWith(String jsonResponseFile) {
        mNextResponseFile = jsonResponseFile;
        mNextResponseErrorCode = 0;
    }

    public void respondWithError(String jsonResponseFile) {
        respondWithError(jsonResponseFile, 404);
    }

    public void respondWithError(String jsonResponseFile, int errorCode) {
        mNextResponseFile = jsonResponseFile;
        mNextResponseErrorCode = errorCode;
    }

    @Override
    public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        // Give some time to create a realistic network event
        TestUtils.waitFor(1000);

        String requestUrl = request.url().toString();

        Response response;
        if (mNextResponseFile != null) {
            if (mNextResponseErrorCode == 0) {
                response = buildSuccessResponse(request, mNextResponseFile);
            } else {
                response = buildErrorResponse(request, mNextResponseFile, mNextResponseErrorCode);
            }
        } else {
            throw new IllegalStateException("Interceptor was not given a response for this request! URL: "
                                            + requestUrl);
        }

        // Clean up for the next call
        mNextResponseFile = null;
        mNextResponseErrorCode = 0;

        return response;
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
