package org.wordpress.android.stores.network;

import org.wordpress.android.stores.Payload;

public enum AuthError implements Payload {
    INVALID_TOKEN,
    NOT_AUTHENTICATED,
    INCORRECT_USERNAME_OR_PASSWORD,
    UNAUTHORIZED,
    HTTP_AUTH_ERROR,
    NEEDS_2FA, // indicates that a two-factor authentication code is required
    INVALID_OTP, // indicates the provided two-factor authentication code is incorrect
    INVALID_SSL_CERTIFICATE,
}
