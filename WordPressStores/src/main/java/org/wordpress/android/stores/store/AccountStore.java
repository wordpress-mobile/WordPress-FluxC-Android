package org.wordpress.android.stores.store;

import android.support.annotation.NonNull;

import com.android.volley.VolleyError;
import com.squareup.otto.Subscribe;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.Payload;
import org.wordpress.android.stores.action.AccountAction;
import org.wordpress.android.stores.action.AuthenticationAction;
import org.wordpress.android.stores.annotations.action.Action;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient.AccountRestPayload;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient.NewAccountResponsePayload;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator.Token;
import org.wordpress.android.stores.persistence.AccountSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.Map;

import javax.inject.Inject;

/**
 * In-memory based and persisted in SQLite.
 */
public class AccountStore extends Store {
    // Payloads
    public static class AuthenticatePayload implements Payload {
        public String username;
        public String password;
        public Action nextAction;
        public AuthenticatePayload(@NonNull String username, @NonNull String password) {
            this.username = username;
            this.password = password;
        }
        public AuthenticatePayload(@NonNull String username, @NonNull String password, @NonNull Action nextAction) {
            this(username, password);
            this.nextAction = nextAction;
        }
    }

    public static class PostAccountSettingsPayload implements Payload {
        public Map<String, String> params;
        public PostAccountSettingsPayload() {
        }
    }

    public static class NewAccountPayload implements Payload {
        public String username;
        public String password;
        public String email;
        public boolean dryRun;
        public NewAccountPayload(@NonNull String username, @NonNull String password, @NonNull String email,
                                 boolean dryRun) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.dryRun = dryRun;
        }
    }

    public static class UpdateTokenPayload implements Payload {
        public UpdateTokenPayload(String token) { this.token = token; }
        public String token;
    }

    // OnChanged Events
    public class OnAccountChanged extends OnChanged {
        public boolean accountInfosChanged;
        public AccountAction causeOfChange;
    }

    public class OnAuthenticationChanged extends OnChanged {
        public boolean isError;
        public AuthError authError;
    }

    public class OnNewUserCreated extends OnChanged {
        public boolean isError;
        public NewUserErrors errorType;
        public String errorMessage;
        public boolean dryRun;
    }

    // Enums
    public enum NewUserErrors {
        USERNAME_ONLY_LOWERCASE_LETTERS_AND_NUMBERS,
        USERNAME_REQUIRED,
        USERNAME_NOT_ALLOWED,
        USERNAME_MUST_BE_AT_LEAST_FOUR_CHARACTERS,
        USERNAME_CONTAINS_INVALID_CHARACTERS,
        USERNAME_MUST_INCLUDE_LETTERS,
        USERNAME_EXISTS,
        USERNAME_RESERVED_BUT_MAY_BE_AVAILABLE,
        USERNAME_INVALID,
        PASSWORD_INVALID,
        EMAIL_CANT_BE_USED_TO_SIGNUP,
        EMAIL_INVALID,
        EMAIL_NOT_ALLOWED,
        EMAIL_EXISTS,
        EMAIL_RESERVED,
        GENERIC_ERROR,
    }

    // Fields
    private AccountRestClient mAccountRestClient;
    private Authenticator mAuthenticator;
    private AccountModel mAccount;
    private AccessToken mAccessToken;

    @Inject
    public AccountStore(Dispatcher dispatcher, AccountRestClient accountRestClient,
                        Authenticator authenticator, AccessToken accessToken) {
        super(dispatcher);
        mAuthenticator = authenticator;
        mAccountRestClient = accountRestClient;
        mAccount = loadAccount();
        mAccessToken = accessToken;
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "AccountStore onRegister");
    }

    @Subscribe
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (actionType == AuthenticationAction.AUTHENTICATE_ERROR) {
            OnAuthenticationChanged event = new OnAuthenticationChanged();
            event.isError = true;
            event.authError = (AuthError) action.getPayload();
            emitChange(event);
        } else if (actionType == AuthenticationAction.AUTHENTICATE) {
            AuthenticatePayload payload = (AuthenticatePayload) action.getPayload();
            authenticate(payload.username, payload.password, payload);
        } else if (actionType == AccountAction.FETCH) {
            // fetch Account and Account Settings
            mAccountRestClient.fetchAccount();
            mAccountRestClient.fetchAccountSettings();
        } else if (actionType == AccountAction.FETCH_ACCOUNT) {
            // fetch only Account
            mAccountRestClient.fetchAccount();
        } else if (actionType == AccountAction.FETCH_SETTINGS) {
            // fetch only Account Settings
            mAccountRestClient.fetchAccountSettings();
        } else if (actionType == AccountAction.POST_SETTINGS) {
            PostAccountSettingsPayload payload = (PostAccountSettingsPayload) action.getPayload();
            mAccountRestClient.postAccountSettings(payload.params);
        } else if (actionType == AccountAction.FETCHED_ACCOUNT) {
            AccountRestPayload data = (AccountRestPayload) action.getPayload();
            if (!checkError(data, "Error fetching Account via REST API (/me)")) {
                mAccount.copyAccountAttributes(data.account);
                update(mAccount, AccountAction.FETCH_ACCOUNT);
            }
        } else if (actionType == AccountAction.FETCHED_SETTINGS) {
            AccountRestPayload data = (AccountRestPayload) action.getPayload();
            if (!checkError(data, "Error fetching Account Settings via REST API (/me/settings)")) {
                mAccount.copyAccountSettingsAttributes(data.account);
                update(mAccount, AccountAction.FETCH_SETTINGS);
            }
        } else if (actionType == AccountAction.POSTED_SETTINGS) {
            AccountRestPayload data = (AccountRestPayload) action.getPayload();
            if (!checkError(data, "Error saving Account Settings via REST API (/me/settings)")) {
                update(data.account, AccountAction.POST_SETTINGS);
            }
        } else if (actionType == AccountAction.UPDATE) {
            AccountModel accountModel = (AccountModel) action.getPayload();
            update(accountModel, AccountAction.UPDATE);
        } else if (actionType == AccountAction.UPDATE_ACCESS_TOKEN) {
            UpdateTokenPayload updateTokenPayload = (UpdateTokenPayload) action.getPayload();
            updateToken(updateTokenPayload);
        } else if (actionType == AccountAction.SIGN_OUT) {
            signOut();
        } else if (actionType == AccountAction.CREATE_NEW_ACCOUNT) {
            newAccount((NewAccountPayload) action.getPayload());
        } else if (actionType == AccountAction.CREATED_NEW_ACCOUNT) {
            NewAccountResponsePayload payload = (NewAccountResponsePayload) action.getPayload();
            OnNewUserCreated onNewUserCreated = new OnNewUserCreated();
            onNewUserCreated.isError = payload.isError;
            onNewUserCreated.errorType = payload.errorType;
            onNewUserCreated.errorMessage = payload.errorMessage;
            onNewUserCreated.dryRun = payload.dryRun;
            emitChange(onNewUserCreated);
        }
    }

    private void newAccount(NewAccountPayload payload) {
        mAccountRestClient.newAccount(payload.username, payload.password, payload.email, payload.dryRun);
    }

    private void signOut() {
        // Remove Account
        AccountSqlUtils.deleteAccount(mAccount);
        mAccount.init();
        OnAccountChanged accountChanged = new OnAccountChanged();
        accountChanged.accountInfosChanged = true;
        emitChange(accountChanged);

        // Remove authentication token
        mAccessToken.set(null);
        emitChange(new OnAuthenticationChanged());
    }

    public AccountModel getAccount() {
        return mAccount;
    }

    /**
     * Can be used to check if Account is signed into WordPress.com.
     */
    public boolean hasAccessToken() {
        return mAccessToken.exists();
    }

    /**
     * Should be used for very specific purpose (like forwarding the token to a Webview)
     */
    public String getAccessToken() {
        return mAccessToken.get();
    }

    /**
     * Checks if an Account is currently signed in to WordPress.com or any WordPress.org sites.
     */
    public boolean isSignedIn() {
        return hasAccessToken() || mAccount.getVisibleSiteCount() > 0;
    }

    private void updateToken(UpdateTokenPayload updateTokenPayload) {
        mAccessToken.set(updateTokenPayload.token);
    }

    private void update(AccountModel accountModel, AccountAction cause) {
        // Update memory instance
        mAccount = accountModel;

        AccountSqlUtils.insertOrUpdateAccount(accountModel);

        OnAccountChanged accountChanged = new OnAccountChanged();
        accountChanged.accountInfosChanged = true;
        accountChanged.causeOfChange = cause;
        emitChange(accountChanged);
    }

    private AccountModel loadAccount() {
        AccountModel account = AccountSqlUtils.getDefaultAccount();
        return account == null ? new AccountModel() : account;
    }

    private void authenticate(String username, String password, final AuthenticatePayload payload) {
        mAuthenticator.authenticate(username, password, null, false, new Authenticator.Listener() {
            @Override
            public void onResponse(Token token) {
                mAccessToken.set(token.getAccessToken());
                if (payload.nextAction != null) {
                    mDispatcher.dispatch(payload.nextAction);
                }
                emitChange(new OnAuthenticationChanged());
            }
        }, new Authenticator.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.API, "Authentication error");
                OnAuthenticationChanged event = new OnAuthenticationChanged();
                event.isError = true;
                emitChange(event);
            }
        });
    }

    private boolean checkError(AccountRestPayload payload, String log) {
        if (payload.isError()) {
            AppLog.w(T.API, log + "\nError: " + payload.error.getMessage());
            return true;
        }
        return false;
    }
}
