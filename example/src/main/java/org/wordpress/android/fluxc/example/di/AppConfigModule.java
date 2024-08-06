package org.wordpress.android.fluxc.example.di;

import android.content.Context;
import android.text.TextUtils;

import com.goterl.lazysodium.exceptions.SodiumException;

import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLoggingKey;
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptionUtils;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.lang.reflect.Field;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppConfigModule {
    public String getStringBuildConfigValue(String fieldName) {
        try {
            Class<?> clazz = Class.forName(BuildConfig.APPLICATION_ID + ".BuildConfig");
            Field field = clazz.getField(fieldName);
            return (String) field.get(null);
        } catch (Exception e) {
            return "";
        }
    }

    @Provides
    public AppSecrets provideAppSecrets() {
        String appId = getStringBuildConfigValue("OAUTH_APP_ID");
        String appSecret = getStringBuildConfigValue("OAUTH_APP_SECRET");
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(appSecret)) {
            AppLog.e(T.API, "OAUTH_APP_ID or OAUTH_APP_SECRET is empty, check your gradle.properties");
        }
        return new AppSecrets(appId, appSecret);
    }

    @Provides
    @Singleton
    public UserAgent provideUserAgent(Context appContext) {
        return new UserAgent(appContext, "fluxc-example-android");
    }

    @Provides
    public EncryptedLoggingKey provideEncryptedLoggingKey() {
        try {
            return new EncryptedLoggingKey(EncryptionUtils.getSodium().cryptoBoxKeypair().getPublicKey());
        } catch (SodiumException e) {
            e.printStackTrace();
            return null;
        }
    }
}
