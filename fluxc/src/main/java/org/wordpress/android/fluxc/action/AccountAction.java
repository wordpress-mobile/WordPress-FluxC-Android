package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountPushSettingsResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountRestPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.IsAvailableResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.NewAccountResponsePayload;
import org.wordpress.android.fluxc.store.AccountStore.AvailableBlogPayload;
import org.wordpress.android.fluxc.store.AccountStore.AvailableDomainPayload;
import org.wordpress.android.fluxc.store.AccountStore.AvailableEmailPayload;
import org.wordpress.android.fluxc.store.AccountStore.AvailableUsernamePayload;
import org.wordpress.android.fluxc.store.AccountStore.FetchAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.FetchSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.NewAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.SignOutPayload;
import org.wordpress.android.fluxc.store.AccountStore.UpdateAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload;
import org.wordpress.android.fluxc.store.AccountStore.VerificationEmailPayload;

@ActionEnum
public enum AccountAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchAccountPayload.class)
    FETCH_ACCOUNT,          // request fetch of Account information
    @Action(payloadType = FetchSettingsPayload.class)
    FETCH_SETTINGS,         // request fetch of Account Settings
    @Action(payloadType = VerificationEmailPayload.class)
    SEND_VERIFICATION_EMAIL, // request verification email for unverified accounts
    @Action(payloadType = PushAccountSettingsPayload.class)
    PUSH_SETTINGS,          // request saving Account Settings remotely
    @Action(payloadType = NewAccountPayload.class)
    CREATE_NEW_ACCOUNT,     // create a new account (can be used to validate the account before creating it)
    @Action(payloadType = AvailableBlogPayload.class)
    IS_AVAILABLE_BLOG,
    @Action(payloadType = AvailableDomainPayload.class)
    IS_AVAILABLE_DOMAIN,
    @Action(payloadType = AvailableEmailPayload.class)
    IS_AVAILABLE_EMAIL,
    @Action(payloadType = AvailableUsernamePayload.class)
    IS_AVAILABLE_USERNAME,

    // Remote responses
    @Action(payloadType = AccountRestPayload.class)
    FETCHED_ACCOUNT,        // response received from Account fetch request
    @Action(payloadType = AccountRestPayload.class)
    FETCHED_SETTINGS,       // response received from Account Settings fetch
    @Action(payloadType = NewAccountResponsePayload.class)
    SENT_VERIFICATION_EMAIL,
    @Action(payloadType = AccountPushSettingsResponsePayload.class)
    PUSHED_SETTINGS,        // response received from Account Settings post
    @Action(payloadType = NewAccountResponsePayload.class)
    CREATED_NEW_ACCOUNT,    // create a new account response
    @Action(payloadType = IsAvailableResponsePayload.class)
    CHECKED_IS_AVAILABLE,

    // Local actions
    @Action(payloadType = UpdateAccountPayload.class)
    UPDATE_ACCOUNT,         // update in-memory and persisted Account in AccountStore
    @Action(payloadType = UpdateTokenPayload.class)
    UPDATE_ACCESS_TOKEN,    // update in-memory and persisted Access Token
    @Action(payloadType = SignOutPayload.class)
    SIGN_OUT                // delete persisted Account, reset in-memory Account, delete access token
}
