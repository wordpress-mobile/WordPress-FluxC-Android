package org.wordpress.android.fluxc;

public abstract class ResponsePayload extends BasePayload {
    private final RequestPayload mRequestPayload;

    public ResponsePayload(RequestPayload requestPayload) {
        this.mRequestPayload = requestPayload;
    }

    public RequestPayload getRequestPayload() {
        return mRequestPayload;
    }
}
