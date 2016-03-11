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

    public static void insertOrUpdateAccount(AccountModel account) {
        List<AccountModel> accountResults = WellSql.select(AccountModel.class)
                .where()
                .equals(AccountModelTable.USER_NAME, account.getUserName())
                .equals(AccountModelTable.PRIMARY_BLOG_ID, account.getPrimaryBlogId())
                .endWhere().getAsModel();
        if (accountResults.isEmpty()) {
            WellSql.insert(account).execute();
        } else {
            updateAccount(accountResults.get(0).getId(), new UpdateAllExceptId<AccountModel>().toCv(account));
        }
    }

    public static void insertOrUpdateOnlyAccount(AccountModel account) {
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

    public static void insertOrUpdateOnlyAccountSettings(AccountModel account) {
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

    public static void updateAccount(long localId, final ContentValues cv) {
        AccountModel account = getAccountByLocalId(localId);
        if (account == null) return;
        int oldId = account.getId();
        WellSql.update(AccountModel.class).whereId(oldId)
                .put(account, new InsertMapper<AccountModel>() {
                    @Override
                    public ContentValues toCv(AccountModel item) {
                        return cv;
                    }
                }).execute();
    }

    public static AccountModel getDefaultAccount() {
        return getAccountByLocalId(DEFAULT_ACCOUNT_LOCAL_ID);
    }

    public static AccountModel getAccountByLocalId(long localId) {
        List<AccountModel> accountResult = WellSql.select(AccountModel.class)
                .where().equals(AccountModelTable.ID, localId)
                .endWhere().getAsModel();
        return accountResult.isEmpty() ? null : accountResult.get(0);
    }

    public static AccountModel getAccountByRemoteId(long remoteId) {
        List<AccountModel> accountResult = WellSql.select(AccountModel.class)
                .where().equals(AccountModelTable.USER_ID, remoteId)
                .endWhere().getAsModel();
        return accountResult.isEmpty() ? null : accountResult.get(0);
    }
}
