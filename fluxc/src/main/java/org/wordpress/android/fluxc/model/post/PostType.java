package org.wordpress.android.fluxc.model.post;

public enum PostType {
    POST("post"), PAGE("page"), PORTFOLIO("jetpack-portfolio");

    private final String mValue;

    PostType(String value) {
        mValue = value;
    }

    public String getValue() {
        return mValue;
    }

    public static PostType getPostType(String type) {
        for (PostType postType : values()) {
            if (postType.getValue().equals(type)) {
                return postType;
            }
        }
        // This should never happen.
        return POST;
    }
}
