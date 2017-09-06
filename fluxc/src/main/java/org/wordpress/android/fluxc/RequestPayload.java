package org.wordpress.android.fluxc;

import java.util.concurrent.atomic.AtomicLong;

public abstract class RequestPayload extends BasePayload {
    private static final AtomicLong GLOBAL_REQUEST_COUNTER = new AtomicLong(1);
    private final long mRequestId;

    public Object extra;

    public RequestPayload() {
        mRequestId = GLOBAL_REQUEST_COUNTER.getAndIncrement();
    }

    public RequestPayload(long forwardRequestId) {
        mRequestId = forwardRequestId;
    }

    public long getRequestId() {
        return mRequestId;
    }
}
