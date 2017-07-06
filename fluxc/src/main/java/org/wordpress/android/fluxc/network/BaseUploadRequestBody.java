package org.wordpress.android.fluxc.network;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.model.MediaModel;

import java.io.IOException;

import okhttp3.RequestBody;
import okio.Buffer;
import okio.ForwardingSink;
import okio.Sink;

/**
 * Wrapper for {@link okhttp3.MultipartBody} that reports upload progress as body data is written.
 *
 * A {@link ProgressListener} is required, use {@link okhttp3.MultipartBody} if progress is not needed.
 *
 * ref http://stackoverflow.com/questions/35528751/okhttp-3-tracking-multipart-upload-progress
 */
public abstract class BaseUploadRequestBody extends RequestBody {
    /**
     * Callback to report upload progress as body data is written to the sink for network delivery.
     */
    public interface ProgressListener {
        void onProgress(MediaModel media, float progress);
    }

    protected abstract String hasAllRequiredData(MediaModel media);

    private final MediaModel mMedia;
    private final ProgressListener mListener;

    public BaseUploadRequestBody(MediaModel media, ProgressListener listener) {
        // validate arguments
        if (listener == null) {
            throw new IllegalArgumentException("progress listener cannot be null");
        }
        String mediaError = hasAllRequiredData(media);
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
