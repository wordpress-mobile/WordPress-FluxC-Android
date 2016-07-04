package org.wordpress.android.stores.network.rest.wpcom.account;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.Payload;
import org.wordpress.android.stores.action.AccountAction;
import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.stores.network.rest.wpcom.WPCOMREST;
import org.wordpress.android.stores.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccountRestClient extends BaseWPComRestClient {
    public static class AccountRestPayload implements Payload {
        public AccountRestPayload(AccountModel account, VolleyError error) {
            this.account = account;
            this.error = error;
        }
        public boolean isError() { return error != null; }
        public VolleyError error;
        public AccountModel account;
    }

    public static class AccountPostResponsePayload implements Payload {
        public AccountPostResponsePayload(VolleyError error) {
            this.error = error;
        }
        public boolean isError() { return error != null; }
        public VolleyError error;
        public Map<String, Object> settings;
    }

    @Inject
    public AccountRestClient(Dispatcher dispatcher, RequestQueue requestQueue,
                             AccessToken accessToken, UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
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
     * with a payload of type {@link AccountPostResponsePayload}. {@link AccountPostResponsePayload#isError()} can
     * be used to determine the result of the request.
     *
     * No HTTP POST call is made if the given parameter map is null or contains no entries.
     */
    public void postAccountSettings(Map<String, String> params) {
        if (params == null || params.isEmpty()) return;
        String url = WPCOMREST.ME_SETTINGS.getUrlV1_1();
        // Note: we have to use a HashMap as a response here because the API response format is different depending
        // of the request we do.
        add(new WPComGsonRequest<>(Method.POST, url, params, HashMap.class,
                new Listener<HashMap>() {
                    @Override
                    public void onResponse(HashMap response) {
                        AccountPostResponsePayload payload = new AccountPostResponsePayload(null);
                        payload.settings = response;
                        mDispatcher.dispatch(AccountAction.POSTED_SETTINGS, payload);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AccountPostResponsePayload payload = new AccountPostResponsePayload(error);
                        mDispatcher.dispatch(AccountAction.POSTED_SETTINGS, payload);
                    }
                }
        ));
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
        accountSettings.setDisplayName(from.display_name);
        return accountSettings;
    }

    public static boolean updateAccountModelFromGenericResponse(AccountModel accountModel, Map<String, Object> from) {
        AccountModel old = new AccountModel();
        old.copyAccountAttributes(accountModel);
        old.copyAccountSettingsAttributes(accountModel);
        if (from.containsKey("user_login")) accountModel.setUserName((String) from.get("user_login"));
        if (from.containsKey("primary_site_ID")) accountModel.setPrimaryBlogId((Long) from.get("primary_site_ID"));
        if (from.containsKey("first_name")) accountModel.setFirstName((String) from.get("first_name"));
        if (from.containsKey("last_name")) accountModel.setLastName((String) from.get("last_name"));
        if (from.containsKey("description")) accountModel.setAboutMe((String) from.get("description"));
        if (from.containsKey("date")) accountModel.setDate((String) from.get("date"));
        if (from.containsKey("new_user_email")) accountModel.setNewEmail((String) from.get("new_user_email"));
        if (from.containsKey("user_email_change_pending")) accountModel.setPendingEmailChange((Boolean) from.get
                ("user_email_change_pending"));
        if (from.containsKey("user_URL")) accountModel.setWebAddress((String) from.get("user_URL"));
        if (from.containsKey("username")) accountModel.setUserName((String) from.get("username"));
        if (from.containsKey("ID")) accountModel.setUserId((Long) from.get("ID"));
        if (from.containsKey("profile_URL")) accountModel.setProfileUrl((String) from.get("profile_URL"));
        if (from.containsKey("avatar_URL")) accountModel.setAvatarUrl((String) from.get("avatar_URL"));
        if (from.containsKey("email")) accountModel.setEmail((String) from.get("email"));
        if (from.containsKey("display_name")) accountModel.setDisplayName((String) from.get("display_name"));
        return !old.equals(accountModel);
    }
}
