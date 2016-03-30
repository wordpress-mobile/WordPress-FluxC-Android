package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.action.Action;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;

public class AuthenticationActionBuilder extends ActionBuilder {
    public static Action<AuthenticatePayload> generateAuthenticateAction(AuthenticatePayload payload) {
        return new Action<>(AuthenticationAction.AUTHENTICATE, payload);
    }

    public static Action<AuthError> generateAuthenticateErrorAction(AuthError payload) {
        return new Action<>(AuthenticationAction.AUTHENTICATE_ERROR, payload);
    }
}
