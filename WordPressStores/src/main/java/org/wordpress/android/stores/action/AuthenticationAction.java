package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;

@ActionEnum
public enum AuthenticationAction implements IAction {
    @Action(payloadType = AuthenticatePayload.class)
    AUTHENTICATE,
    @Action(payloadType = AuthError.class)
    AUTHENTICATE_ERROR,
    UNAUTHORIZED,
}
