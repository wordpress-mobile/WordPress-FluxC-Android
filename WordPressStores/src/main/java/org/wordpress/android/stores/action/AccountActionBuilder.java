package org.wordpress.android.stores.action;

import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;

public class AccountActionBuilder extends ActionBuilder {
    public static Action<Void> generateFetchAction() {
        return generateNoPayloadAction(AccountAction.FETCH);
    }

    public static Action<AccountModel> generateUpdateAction(AccountModel payload) {
        return new Action<>(AccountAction.UPDATE, payload);
    }
}
