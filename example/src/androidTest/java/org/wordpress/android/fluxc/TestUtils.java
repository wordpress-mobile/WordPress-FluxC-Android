package org.wordpress.android.fluxc;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.UUID;

public class TestUtils {
    public static final int DEFAULT_TIMEOUT_MS = 30000;
    public static final int MULTIPLE_UPLOAD_TIMEOUT_MS = 120000;

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
}

