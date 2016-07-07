package org.wordpress.android.stores.network.rest.wpcom.account;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.Payload;
import org.wordpress.android.stores.action.AccountAction;
import org.wordpress.android.stores.generated.AccountActionBuilder;
import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.stores.network.rest.wpcom.WPCOMREST;
import org.wordpress.android.stores.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedAccountAction(payload));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AccountRestPayload payload = new AccountRestPayload(null, error);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedAccountAction(payload));
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
                        AccountModel settings = settingsResponseToAccountModel(response);
                        AccountRestPayload payload = new AccountRestPayload(settings, null);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedSettingsAction(payload));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AccountRestPayload payload = new AccountRestPayload(null, error);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedSettingsAction(payload));
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
                        AccountModel settings = settingsResponseToAccountModel(response);
                        AccountRestPayload payload = new AccountRestPayload(settings, null);
                        mDispatcher.dispatch(AccountActionBuilder.newPostedSettingsAction(payload));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AccountRestPayload payload = new AccountRestPayload(null, error);
                        mDispatcher.dispatch(AccountActionBuilder.newPostedSettingsAction(payload));
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
        if (from.date != null) {
            account.setDateCreated(iso8601ToJavaDate(from.date));
        }
        return account;
    }

    private AccountModel settingsResponseToAccountModel(AccountSettingsResponse from) {
        AccountModel account = new AccountModel();
        account.setUserName(from.user_login);
        account.setPrimaryBlogId(from.primary_site_ID);
        account.setFirstName(from.first_name);
        account.setLastName(from.last_name);
        account.setAboutMe(from.description);
        account.setNewEmail(from.new_user_email);
        account.setPendingEmailChange(from.user_email_change_pending);
        account.setWebAddress(from.user_URL);
        if (from.date != null) {
            account.setDateCreated(iso8601ToJavaDate(from.date));
        }
        return account;
    }

    private Date iso8601ToJavaDate(final String strDate) {
        try {
            SimpleDateFormat ISO8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
            return ISO8601Format.parse(strDate);
        } catch (ParseException e) {
            return new Date();
        }
    }
}
