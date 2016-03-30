package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient.AccountRestPayload;
import org.wordpress.android.stores.store.AccountStore.PostAccountSettingsPayload;

public class AccountActionBuilder extends ActionBuilder {
    public static Action<Void> generateFetchAction() {
        return generateNoPayloadAction(AccountAction.FETCH);
    }

    public static Action<Void> generateFetchAccountAction() {
        return generateNoPayloadAction(AccountAction.FETCH_ACCOUNT);
    }

    public static Action<AccountRestPayload> generateFetchedAccountAction(AccountRestPayload payload) {
        return new Action<>(AccountAction.FETCHED_ACCOUNT, payload);
    }

    public static Action<Void> generateFetchSettingsAction() {
        return generateNoPayloadAction(AccountAction.FETCH_SETTINGS);
    }

    public static Action<AccountRestPayload> generateFetchedSettingsAction(AccountRestPayload payload) {
        return new Action<>(AccountAction.FETCHED_SETTINGS, payload);
    }

    public static Action<PostAccountSettingsPayload> generatePostSettingsAction(PostAccountSettingsPayload payload) {
        return new Action<>(AccountAction.POST_SETTINGS, payload);
    }

    public static Action<AccountRestPayload> generatePostedSettingsAction(AccountRestPayload payload) {
        return new Action<>(AccountAction.POSTED_SETTINGS, payload);
    }

    public static Action<AccountModel> generateUpdateAction(AccountModel payload) {
        return new Action<>(AccountAction.UPDATE, payload);
    }

    public static Action<Void> generateSignOutAction() {
        return generateNoPayloadAction(AccountAction.SIGN_OUT);
    }
}
