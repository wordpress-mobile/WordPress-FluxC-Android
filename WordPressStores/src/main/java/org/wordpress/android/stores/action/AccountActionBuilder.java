package org.wordpress.android.stores.action;

import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;

public class AccountActionBuilder {
    public static <T> Action<T> generateFetchAction() {
        return new Action<>(AccountAction.FETCH, null);
    }

    public static Action<AccountModel> generateUpdateAction(AccountModel payload) {
        return new Action<>(AccountAction.UPDATE, payload);
    }
}
