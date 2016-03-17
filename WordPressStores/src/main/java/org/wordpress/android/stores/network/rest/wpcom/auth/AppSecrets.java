package org.wordpress.android.stores.network.rest.wpcom.auth;

public class AppSecrets {
    private final String mAppId;
    private final String mAppSecret;
    private final String mRedirectUri;

    public AppSecrets(String appId, String appSecret, String redirectUri) {
        mAppId = appId;
        mAppSecret = appSecret;
        mRedirectUri = redirectUri;
    }

    public String getAppId() {
        return mAppId;
    }

    public String getAppSecret() {
        return mAppSecret;
    }

    public String getRedirectUri() {
        return mRedirectUri;
    }
}
