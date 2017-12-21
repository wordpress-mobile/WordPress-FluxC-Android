package org.wordpress.android.fluxc.model.post;

public enum ContentType {
    POST("post"), PAGE("page"), PORTFOLIO("portfolio");

    private final String mValue;

    ContentType(String value) {
        mValue = value;
    }

    public String getValue() {
        return mValue;
    }

}

