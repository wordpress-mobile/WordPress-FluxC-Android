package org.wordpress.android.fluxc.model.post;

import java.util.Arrays;

public enum PostType {
    TypePost("post", 0),
    TypePage("page", 1),
    TypePortfolio("jetpack-portfolio", 2);

    private final String mApiValue;
    private final int mStorageValue;

    PostType(String apiValue, int storageValue) {
        mApiValue = apiValue;
        mStorageValue = storageValue;
    }

    public String apiValue() {
        return mApiValue;
    }

    public int modelValue() {
        return mStorageValue;
    }

    public static PostType fromApiValue(String apiTypeName) {
        for (PostType postType : PostType.values()) {
            if (postType.apiValue().equals(apiTypeName)) {
                return postType;
            }
        }
        throw new IllegalArgumentException("Unknown type: " + apiTypeName);
    }

    public static PostType fromModelValue(int storageTypeValue) {
        for (PostType postType : PostType.values()) {
            if (postType.modelValue() == storageTypeValue) {
                return postType;
            }
        }
        throw new IllegalArgumentException("Unknown type: " + storageTypeValue);
    }

    public boolean isOneOf(PostType... types) {
        return Arrays.asList(types).contains(this);
    }
}
