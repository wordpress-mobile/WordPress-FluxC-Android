package org.wordpress.android.fluxc.network;

import android.content.Context;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.io.FileNotFoundException;
import java.io.IOException;

import okhttp3.RequestBody;
import okio.Buffer;
import okio.ForwardingSink;
import okio.Sink;

/**
 * Wrapper for {@link okhttp3.MultipartBody} that reports upload progress as body data is written.
 * <p>
 * A {@link ProgressListener} is required, use {@link okhttp3.MultipartBody} if progress is not needed.
 * <p>
 * ref http://stackoverflow.com/questions/35528751/okhttp-3-tracking-multipart-upload-progress
 */
public abstract class BaseUploadRequestBody extends RequestBody {
    /**
     * Callback to report upload progress as body data is written to the sink for network delivery.
     */
    public interface ProgressListener {
        void onProgress(MediaModel media, float progress);
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
    public static String hasRequiredData(Context context, MediaModel media) {
        if (media == null) {
            return "media cannot be null";
        }

        // validate MIME type is recognized
        String mimeType = media.getMimeType();
        if (!MediaUtils.isSupportedMimeType(mimeType)) {
            return "media must define a valid MIME type";
        }

        // verify file path is defined
        String filePath = media.getFilePath();
        if (TextUtils.isEmpty(filePath)) {
            return "media must define a local file path";
        }

        // verify file exists and is not a directory
        try {
            ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(Uri.parse(filePath), "r");
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                descriptor.checkError();
            }
        } catch (FileNotFoundException e) {
            return "local file path for media does not exist";
        } catch (IOException e) {
            return "local file not accessible";
        }
        return null;
    }

    private final MediaModel mMedia;
    private final ProgressListener mListener;

    public BaseUploadRequestBody(Context mAppContext, MediaModel media, ProgressListener listener) {
        // validate arguments
        if (listener == null) {
            throw new IllegalArgumentException("progress listener cannot be null");
        }
        String mediaError = hasRequiredData(mAppContext, media);
        if (mediaError != null) {
            throw new IllegalArgumentException(mediaError);
        }

        mMedia = media;
        mListener = listener;
    }

    protected abstract float getProgress(long bytesWritten);

    public MediaModel getMedia() {
        return mMedia;
    }

    /**
     * Custom Sink that reports progress to listener as bytes are written.
     */
    protected final class CountingSink extends ForwardingSink {
        private static final int ON_PROGRESS_THROTTLE_RATE = 100;
        private long mBytesWritten = 0;
        private long mLastTimeOnProgressCalled = 0;

        public CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(@NonNull Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            mBytesWritten += byteCount;
            long currentTimeMillis = System.currentTimeMillis();
            // Call the mListener.onProgress callback at maximum every 100ms.
            if ((currentTimeMillis - mLastTimeOnProgressCalled) > ON_PROGRESS_THROTTLE_RATE
                || mLastTimeOnProgressCalled == 0) {
                mLastTimeOnProgressCalled = currentTimeMillis;
                mListener.onProgress(mMedia, getProgress(mBytesWritten));
            }
        }
    }
}
