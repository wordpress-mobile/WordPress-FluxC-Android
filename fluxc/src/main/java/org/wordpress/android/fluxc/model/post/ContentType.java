package org.wordpress.android.fluxc.model.post;

public enum ContentType {
    POST("post"), PAGE("page"), PORTFOLIO("jetpack-portfolio");

    private final String mValue;

    ContentType(String value) {
        mValue = value;
    }

    public String getValue() {
        return mValue;
    }

    public static ContentType getContentType(String type) {
        for (ContentType contentType : values()) {
            if (contentType.getValue().equals(type)) {
                return contentType;
            }
        }
        //this should never happen
        return POST;
    }
}

