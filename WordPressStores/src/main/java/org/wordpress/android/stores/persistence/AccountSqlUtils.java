package org.wordpress.android.stores.persistence;

import android.content.ContentValues;

import com.wellsql.generated.AccountModelTable;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.stores.model.AccountModel;

import java.util.List;

public class AccountSqlUtils {
    private static final long DEFAULT_ACCOUNT_LOCAL_ID = 1;

    /**
     * This convenience method is provided to gather attributes related exclusively to the Account
     * endpoint (/me). Account Settings endpoint (/me/settings) attributes are not included.
     */
    public static ContentValues getOnlyAccountContent(AccountModel account) {
        ContentValues cv = new ContentValues();
        if (account == null) return cv;
        cv.put(AccountModelTable.USER_ID, account.getUserId());
        cv.put(AccountModelTable.DISPLAY_NAME, account.getDisplayName());
        cv.put(AccountModelTable.PROFILE_URL, account.getProfileUrl());
        cv.put(AccountModelTable.AVATAR_URL, account.getAvatarUrl());
        cv.put(AccountModelTable.SITE_COUNT, account.getSiteCount());
        cv.put(AccountModelTable.VISIBLE_SITE_COUNT, account.getVisibleSiteCount());
        cv.put(AccountModelTable.EMAIL, account.getEmail());
        return cv;
    }

    /**
     * This convenience method is provided to gather attributes related exclusively to the Account
     * Settings endpoint (/me/settings). Account endpoint (/me) attributes are not included.
     */
    public static ContentValues getOnlySettingsContent(AccountModel account) {
        ContentValues cv = new ContentValues();
        if (account == null) return cv;
        cv.put(AccountModelTable.FIRST_NAME, account.getFirstName());
        cv.put(AccountModelTable.LAST_NAME, account.getLastName());
        cv.put(AccountModelTable.ABOUT_ME, account.getAboutMe());
        cv.put(AccountModelTable.DATE, account.getDate());
        cv.put(AccountModelTable.NEW_EMAIL, account.getNewEmail());
        cv.put(AccountModelTable.PENDING_EMAIL_CHANGE, account.getPendingEmailChange());
        cv.put(AccountModelTable.WEB_ADDRESS, account.getWebAddress());
        return cv;
    }

    /**
     * Adds or overwrites all columns for a matching row in the Account Table.
     */
    public static void insertOrUpdateAccount(AccountModel account) {
        if (account == null) return;
        List<AccountModel> accountResults = WellSql.select(AccountModel.class)
                .where()
                .equals(AccountModelTable.ID, account.getId())
                .endWhere().getAsModel();
        if (accountResults.isEmpty()) {
            WellSql.insert(account).execute();
        } else {
            updateAccount(accountResults.get(0).getId(), new UpdateAllExceptId<AccountModel>().toCv(account));
        }
    }

    /**
     * Adds or updates only Account column data for a row in the Account Table that matches the
     * given {@link AccountModel}'s username and primary blog ID.
     *
     * Used by {@link org.wordpress.android.stores.store.AccountStore} to handle asynchronous REST
     * endpoint responses.
     */
    public static void insertOrUpdateOnlyAccount(AccountModel account) {
        if (account == null) return;
        List<AccountModel> accountResults = WellSql.select(AccountModel.class)
                .where()
                .equals(AccountModelTable.USER_NAME, account.getUserName())
                .equals(AccountModelTable.PRIMARY_BLOG_ID, account.getPrimaryBlogId())
                .endWhere().getAsModel();
        if (accountResults.isEmpty()) {
            WellSql.insert(account).execute();
        } else {
            updateAccount(accountResults.get(0).getId(), getOnlyAccountContent(account));
        }
    }

    /**
     * Adds or updates only Account Settings column data for a row in the Account Table that matches
     * the given {@link AccountModel}'s username and primary blog ID.
     *
     * Used by {@link org.wordpress.android.stores.store.AccountStore} to handle asynchronous REST
     * endpoint responses.
     */
    public static void insertOrUpdateOnlyAccountSettings(AccountModel account) {
        if (account == null) return;
        List<AccountModel> accountResults = WellSql.select(AccountModel.class)
                .where()
                .equals(AccountModelTable.USER_NAME, account.getUserName())
                .equals(AccountModelTable.PRIMARY_BLOG_ID, account.getPrimaryBlogId())
                .endWhere().getAsModel();
        if (accountResults.isEmpty()) {
            WellSql.insert(account).execute();
        } else {
            updateAccount(accountResults.get(0).getId(), getOnlySettingsContent(account));
        }
    }

    /**
     * Updates an existing row in the Account Table that matches the given local ID. Only columns
     * defined in the given {@link ContentValues} keys are modified.
     */
    public static void updateAccount(long localId, final ContentValues cv) {
        AccountModel account = getAccountByLocalId(localId);
        if (account == null || cv == null) return;
        int oldId = account.getId();
        WellSql.update(AccountModel.class).whereId(oldId)
                .put(account, new InsertMapper<AccountModel>() {
                    @Override
                    public ContentValues toCv(AccountModel item) {
                        return cv;
                    }
                }).execute();
    }

    /**
     * Passthrough to {@link #getAccountByLocalId(long)} using the default Account local ID.
     */
    public static AccountModel getDefaultAccount() {
        return getAccountByLocalId(DEFAULT_ACCOUNT_LOCAL_ID);
    }

    /**
     * Attempts to load an Account with the given local ID from the Account Table.
     *
     * @return the Account row as {@link AccountModel}, null if no row matches the given ID
     */
    public static AccountModel getAccountByLocalId(long localId) {
        List<AccountModel> accountResult = WellSql.select(AccountModel.class)
                .where().equals(AccountModelTable.ID, localId)
                .endWhere().getAsModel();
        return accountResult.isEmpty() ? null : accountResult.get(0);
    }

    /**
     * Attempts to load an Account with the given remote ID from the Account Table.
     *
     * @return the Account row as {@link AccountModel}, null if no row matches the given ID
     */
    public static AccountModel getAccountByRemoteId(long remoteId) {
        List<AccountModel> accountResult = WellSql.select(AccountModel.class)
                .where().equals(AccountModelTable.USER_ID, remoteId)
                .endWhere().getAsModel();
        return accountResult.isEmpty() ? null : accountResult.get(0);
    }
}
