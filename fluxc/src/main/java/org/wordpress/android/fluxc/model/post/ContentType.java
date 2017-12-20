package org.wordpress.android.fluxc.model.post;

public enum ContentType {
    POST(0), PAGE(1), PORTFOLIO(2);

    private final int mValue;

    ContentType(int value) {
        mValue = value;
    }

    public static ContentType getContentType(int value) {
        switch (value) {
            case 0:
                return POST;
            case 1:
                return PAGE;
            case 2:
                return PORTFOLIO;
            default:
                return POST;
        }
    }

    public int getValue() {
        return mValue;
    }
}

