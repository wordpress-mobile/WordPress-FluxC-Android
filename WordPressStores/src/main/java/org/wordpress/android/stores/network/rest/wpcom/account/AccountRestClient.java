package org.wordpress.android.stores.network.rest.wpcom.account;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.action.AccountAction;
import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.stores.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccountRestClient extends BaseWPComRestClient {
    @Inject
    public AccountRestClient(Dispatcher dispatcher, RequestQueue requestQueue,
                             AccessToken accessToken, UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
    }

    public void getMe() {
        final String url = WPCOM_PREFIX_V1_1 + "/me/";
        final WPComGsonRequest<AccountResponse> request = new WPComGsonRequest<>(Method.GET,
                url, null, AccountResponse.class,
                new Listener<AccountResponse>() {
                    @Override
                    public void onResponse(AccountResponse response) {
                        AccountModel accountModel = responseToAccountModel(response);
                        mDispatcher.dispatch(AccountAction.FETCHED, accountModel);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.API, "Volley error", error);
                        // TODO: Error, dispatch network error
                    }
                }
        );
        add(request);
    }

    public void getMySettings() {
        final String url = WPCOM_PREFIX_V1_1 + "/me/settings/";
        final WPComGsonRequest<AccountResponse> request = new WPComGsonRequest<>(Method.GET,
                url, null, AccountResponse.class,
                new Listener<AccountResponse>() {
                    @Override
                    public void onResponse(AccountResponse response) {
                        AccountModel settings = responseToAccountSettingsModel(response);
                        mDispatcher.dispatch(AccountAction.FETCHED_SETTINGS, settings);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.API, "Volley error", error);
                    }
                }
        );
        add(request);
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

    private AccountModel responseToAccountSettingsModel(AccountResponse from) {
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
