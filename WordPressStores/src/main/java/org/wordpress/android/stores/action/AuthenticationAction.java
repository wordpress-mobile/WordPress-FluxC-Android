package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator.AuthenticateErrorPayload;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;

@ActionEnum
public enum AuthenticationAction implements IAction {
    @Action(payloadType = AuthenticatePayload.class)
    AUTHENTICATE,
    @Action(payloadType = AuthenticateErrorPayload.class)
    AUTHENTICATE_ERROR,
    UNAUTHORIZED,
}
