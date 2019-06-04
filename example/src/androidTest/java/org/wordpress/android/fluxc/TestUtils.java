package org.wordpress.android.fluxc;

import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.fluxc.example.test.BuildConfig;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

public class TestUtils {
    public static final int DEFAULT_TIMEOUT_MS = 30000;
    public static final int MULTIPLE_UPLOADS_TIMEOUT_MS = 60000;

    private static final String SAMPLE_IMAGE = "pony.jpg";
    private static final String SAMPLE_VIDEO = "pony.mp4";

    public static void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            AppLog.e(T.API, "Thread interrupted");
        }
    }

    public static void waitForNetworkCall() {
        waitFor(DEFAULT_TIMEOUT_MS);
    }

    public static String randomString(int length) {
        String randomString = UUID.randomUUID().toString();
        return length > randomString.length() ? randomString : randomString.substring(0, length);
    }

    /**
     * Copy the file from the Asset manager (in the APK) to a cache directory.
     * If the file already exists, do nothing.
     * @return absolute path of the copied file.
     */
    private static String copyAndGetAssetPath(@NonNull Context context, @NonNull Context targetContext,
                                              @NonNull String filename) {
        File cacheFile = new File(targetContext.getCacheDir(), filename);
        if (cacheFile.exists()) {
            return cacheFile.getAbsolutePath();
        }
        try {
            InputStream inputStream = context.getAssets().open(filename);
            OutputStream outputStream = new FileOutputStream(cacheFile);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            outputStream.write(buffer);
            outputStream.close();
            return cacheFile.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSampleImagePath(@NonNull Context context, @NonNull Context targetContext) {
        if (!TextUtils.isEmpty(BuildConfig.TEST_LOCAL_IMAGE)) {
            return BuildConfig.TEST_LOCAL_IMAGE;
        }
        return copyAndGetAssetPath(context, targetContext, SAMPLE_IMAGE);
    }

    public static String getSampleVideoPath(@NonNull Context context, @NonNull Context targetContext) {
        if (!TextUtils.isEmpty(BuildConfig.TEST_LOCAL_VIDEO)) {
            return BuildConfig.TEST_LOCAL_VIDEO;
        }
        return copyAndGetAssetPath(context, targetContext, SAMPLE_VIDEO);
    }

    public static Context updateLocale(Context context, Locale locale) {
        Configuration config = context.getApplicationContext().getResources().getConfiguration();
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }
}
