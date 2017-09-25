package org.wordpress.android.fluxc.network.rest.wpcom.media;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

/**
 * Wrapper for {@link MultipartBody} that reports upload progress as body data is written.
 *
 * A {@link ProgressListener} is required, use {@link MultipartBody} if progress is not needed.
 *
 * ref http://stackoverflow.com/questions/35528751/okhttp-3-tracking-multipart-upload-progress
 */
public class RestUploadRequestBody extends BaseUploadRequestBody {
    private static final String MEDIA_DATA_KEY = "media[0]";
    private static final String MEDIA_ATTRIBUTES_KEY = "attrs[0]";
    private static final String MEDIA_PARAM_FORMAT = MEDIA_ATTRIBUTES_KEY + "[%s]";

    private final MultipartBody mMultipartBody;

    public RestUploadRequestBody(MediaModel media, Map<String, Object> params, ProgressListener listener) {
        super(media, listener);
        mMultipartBody = buildMultipartBody(params);
    }

    /**
     * Determines if media data is sufficient for upload. Valid media must:
     * <ul>
     *     <li>be non-null</li>
     *     <li>define a recognized MIME type</li>
     *     <li>define a file path to a valid local file</li>
     * </ul>
     *
     * @return null if {@code media} is valid, otherwise a string describing why it's invalid
     */
    public static String hasRequiredWPCOMData(MediaModel media) {
        if (media == null) return "media cannot be null";

        // validate MIME type is recognized
        String mimeType = media.getMimeType();

        if (!MediaUtils.isSupportedMimeTypeWPCOM(mimeType)) {
            return "media must define a valid MIME type";
        }

        // verify file path is defined
        String filePath = media.getFilePath();
        if (TextUtils.isEmpty(filePath)) {
            return "media must define a local file path";
        }

        // verify file exists and is not a directory
        File file = new File(filePath);
        if (!file.exists()) {
            return "local file path for media does not exist";
        } else if (file.isDirectory()) {
            return "supplied file path is a directory, a file is required";
        }

        return null;
    }

    @Override
    protected String hasRequiredData(MediaModel media) {
        return hasRequiredWPCOMData(media);
    }

    @Override
    protected float getProgress(long bytesWritten) {
        return (float) bytesWritten / contentLength();
    }

    @Override
    public long contentLength() {
        try {
            return mMultipartBody.contentLength();
        } catch (IOException e) {
            AppLog.w(AppLog.T.MEDIA, "Error determining mMultipartBody content length: " + e);
        }
        return -1L;
    }

    @Override
    public MediaType contentType() {
        return mMultipartBody.contentType();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);
        mMultipartBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    private MultipartBody buildMultipartBody(Map<String, Object> params) {
        MediaModel media = getMedia();
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        // add media attributes
        for (String key : params.keySet()) {
            builder.addFormDataPart(String.format(MEDIA_PARAM_FORMAT, key), params.get(key).toString());
        }

        // add media file data
        File mediaFile = new File(media.getFilePath());
        RequestBody body = RequestBody.create(MediaType.parse(media.getMimeType()), mediaFile);
        builder.addFormDataPart(MEDIA_DATA_KEY, media.getFileName(), body);

        return builder.build();
    }
}
