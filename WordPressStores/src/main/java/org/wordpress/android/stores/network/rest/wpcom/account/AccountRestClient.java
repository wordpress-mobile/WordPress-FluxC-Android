package org.wordpress.android.stores.network.rest.wpcom.account;

import android.support.annotation.NonNull;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.Payload;
import org.wordpress.android.stores.action.AccountAction;
import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.stores.network.rest.wpcom.WPCOMREST;
import org.wordpress.android.stores.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.stores.store.AccountStore.NewUserErrors;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccountRestClient extends BaseWPComRestClient {
    private final AppSecrets mAppSecrets;

    public static class AccountRestPayload implements Payload {
        public AccountRestPayload(AccountModel account, VolleyError error) {
            this.account = account;
            this.error = error;
        }
        public boolean isError() { return error != null; }
        public VolleyError error;
        public AccountModel account;
    }

    public static class NewAccountResponsePayload implements Payload {
        public NewAccountResponsePayload() {
        }
        public NewUserErrors errorType;
        public String errorMessage;
        public boolean isError;
    }

    @Inject
    public AccountRestClient(Dispatcher dispatcher, RequestQueue requestQueue, AppSecrets appSecrets,
                             AccessToken accessToken, UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
        mAppSecrets = appSecrets;
    }

    /**
     * Performs an HTTP GET call to the v1.1 {@link WPCOMREST#ME} endpoint. Upon receiving a
     * response (success or error) a {@link AccountAction#FETCHED_ACCOUNT} action is dispatched
     * with a payload of type {@link AccountRestPayload}. {@link AccountRestPayload#isError()} can
     * be used to determine the result of the request.
     */
    public void fetchAccount() {
        String url = WPCOMREST.ME.getUrlV1_1();
        add(new WPComGsonRequest<>(Method.GET, url, null, AccountResponse.class,
                new Listener<AccountResponse>() {
                    @Override
                    public void onResponse(AccountResponse response) {
                        AccountModel account = responseToAccountModel(response);
                        AccountRestPayload payload = new AccountRestPayload(account, null);
                        mDispatcher.dispatch(AccountAction.FETCHED_ACCOUNT, payload);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AccountRestPayload payload = new AccountRestPayload(null, error);
                        mDispatcher.dispatch(AccountAction.FETCHED_ACCOUNT, payload);
                    }
                }
        ));
    }

    /**
     * Performs an HTTP GET call to the v1.1 {@link WPCOMREST#ME_SETTINGS} endpoint. Upon receiving
     * a response (success or error) a {@link AccountAction#FETCHED_SETTINGS} action is dispatched
     * with a payload of type {@link AccountRestPayload}. {@link AccountRestPayload#isError()} can
     * be used to determine the result of the request.
     */
    public void fetchAccountSettings() {
        String url = WPCOMREST.ME_SETTINGS.getUrlV1_1();
        add(new WPComGsonRequest<>(Method.GET, url, null, AccountSettingsResponse.class,
                new Listener<AccountSettingsResponse>() {
                    @Override
                    public void onResponse(AccountSettingsResponse response) {
                        AccountModel settings = responseToAccountSettingsModel(response);
                        AccountRestPayload payload = new AccountRestPayload(settings, null);
                        mDispatcher.dispatch(AccountAction.FETCHED_SETTINGS, payload);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AccountRestPayload payload = new AccountRestPayload(null, error);
                        mDispatcher.dispatch(AccountAction.FETCHED_SETTINGS, payload);
                    }
                }
        ));
    }

    /**
     * Performs an HTTP POST call to the v1.1 {@link WPCOMREST#ME_SETTINGS} endpoint. Upon receiving
     * a response (success or error) a {@link AccountAction#POSTED_SETTINGS} action is dispatched
     * with a payload of type {@link AccountRestPayload}. {@link AccountRestPayload#isError()} can
     * be used to determine the result of the request.
     *
     * No HTTP POST call is made if the given parameter map is null or contains no entries.
     */
    public void postAccountSettings(Map<String, String> params) {
        if (params == null || params.isEmpty()) return;
        String url = WPCOMREST.ME_SETTINGS.getUrlV1_1();
        add(new WPComGsonRequest<>(Method.POST, url, params, AccountSettingsResponse.class,
                new Listener<AccountSettingsResponse>() {
                    @Override
                    public void onResponse(AccountSettingsResponse response) {
                        AccountModel settings = responseToAccountSettingsModel(response);
                        AccountRestPayload payload = new AccountRestPayload(settings, null);
                        mDispatcher.dispatch(AccountAction.POSTED_SETTINGS, payload);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AccountRestPayload payload = new AccountRestPayload(null, error);
                        mDispatcher.dispatch(AccountAction.POSTED_SETTINGS, payload);
                    }
                }
        ));
    }

    public void validateNewAccount(@NonNull String username, @NonNull String password, @NonNull String email) {
        String url = WPCOMREST.USERS_NEW.getUrlV1();
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        params.put("email", email);
        params.put("validate", "1");
        params.put("client_id", mAppSecrets.getAppId());
        params.put("client_secret", mAppSecrets.getAppSecret());
        add(new WPComGsonRequest<>(Method.POST, url, params, NewAccountResponse.class,
                new Listener<NewAccountResponse>() {
                    @Override
                    public void onResponse(NewAccountResponse response) {
                        NewAccountResponsePayload payload = new NewAccountResponsePayload();
                        payload.isError = false;
                        mDispatcher.dispatch(AccountAction.VALIDATED_NEW_ACCOUNT, payload);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.NOTIFS, new String(error.networkResponse.data));
                        mDispatcher.dispatch(AccountAction.VALIDATED_NEW_ACCOUNT,
                                volleyErrorToAccountResponsePayload(error));
                    }
                }
        ));
    }

    private NewAccountResponsePayload volleyErrorToAccountResponsePayload(VolleyError error) {
        NewAccountResponsePayload payload = new NewAccountResponsePayload();
        payload.isError = true;
        payload.errorType = NewUserErrors.GENERIC_ERROR;
        if (error.networkResponse != null && error.networkResponse.data != null) {
            String jsonString = new String(error.networkResponse.data);
            try {
                JSONObject errorObj = new JSONObject(jsonString);
                payload.errorType = errorStringToErrorType((String) errorObj.get("error"));
                payload.errorMessage = (String) errorObj.get("message");
            } catch (JSONException e) {
                // Do nothing (keep default error)
            }
        }
        return payload;
    }

    private NewUserErrors errorStringToErrorType(String error) {
        if (error.equals("username_only_lowercase_letters_and_numbers")) {
            return NewUserErrors.USERNAME_ONLY_LOWERCASE_LETTERS_AND_NUMBERS;
        }
        if (error.equals("username_required")) {
            return NewUserErrors.USERNAME_REQUIRED;
        }
        if (error.equals("username_not_allowed")) {
            return NewUserErrors.USERNAME_NOT_ALLOWED;
        }
        if (error.equals("username_must_be_at_least_four_characters")) {
            return NewUserErrors.USERNAME_MUST_BE_AT_LEAST_FOUR_CHARACTERS;
        }
        if (error.equals("username_contains_invalid_characters")) {
            return NewUserErrors.USERNAME_CONTAINS_INVALID_CHARACTERS;
        }
        if (error.equals("username_must_include_letters")) {
            return NewUserErrors.USERNAME_MUST_INCLUDE_LETTERS;
        }
        if (error.equals("username_exists")) {
            return NewUserErrors.USERNAME_EXISTS;
        }
        if (error.equals("email_cant_be_used_to_signup")) {
            return NewUserErrors.EMAIL_CANT_BE_USED_TO_SIGNUP;
        }
        if (error.equals("email_invalid")) {
            return NewUserErrors.EMAIL_INVALID;
        }
        if (error.equals("email_not_allowed")) {
            return NewUserErrors.EMAIL_NOT_ALLOWED;
        }
        if (error.equals("email_exists")) {
            return NewUserErrors.EMAIL_EXISTS;
        }
        if (error.equals("username_reserved_but_may_be_available")) {
            return NewUserErrors.USERNAME_RESERVED_BUT_MAY_BE_AVAILABLE;
        }
        if (error.equals("email_reserved")) {
            return NewUserErrors.EMAIL_RESERVED;
        }
        if (error.equals("password_invalid")) {
            return NewUserErrors.PASSWORD_INVALID;
        }
        if (error.equals("username_invalid")) {
            return NewUserErrors.USERNAME_INVALID;
        }
        return NewUserErrors.GENERIC_ERROR;
    }

    private AccountModel responseToAccountModel(AccountResponse from) {
        AccountModel account = new AccountModel();
        account.setUserName(from.username);
        account.setUserId(from.ID);
        account.setDisplayName(from.display_name);
        account.setProfileUrl(from.profile_URL);
        account.setAvatarUrl(from.avatar_URL);
        account.setPrimaryBlogId(from.primary_blog);
        account.setSiteCount(from.site_count);
        account.setVisibleSiteCount(from.visible_site_count);
        account.setEmail(from.email);
        return account;
    }

    private AccountModel responseToAccountSettingsModel(AccountSettingsResponse from) {
        AccountModel accountSettings = new AccountModel();
        accountSettings.setUserName(from.user_login);
        accountSettings.setPrimaryBlogId(from.primary_site_ID);
        accountSettings.setFirstName(from.first_name);
        accountSettings.setLastName(from.last_name);
        accountSettings.setAboutMe(from.description);
        accountSettings.setDate(from.date);
        accountSettings.setNewEmail(from.new_user_email);
        accountSettings.setPendingEmailChange(from.user_email_change_pending);
        accountSettings.setWebAddress(from.user_URL);
        return accountSettings;
    }
}
