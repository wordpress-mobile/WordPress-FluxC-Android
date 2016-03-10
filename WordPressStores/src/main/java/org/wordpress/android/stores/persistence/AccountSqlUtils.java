package org.wordpress.android.stores.persistence;

import android.content.ContentValues;

import com.wellsql.generated.AccountModelTable;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.stores.model.AccountModel;

import java.util.List;

public class AccountSqlUtils {
    public static void insertOrUpdateAccount(AccountModel account) {
        List<AccountModel> accountResults = WellSql.select(AccountModel.class)
                .where()
                .equals(AccountModelTable.USER_NAME, account.getUserName())
                .equals(AccountModelTable.PRIMARY_BLOG_ID, account.getPrimaryBlogId())
                .endWhere().getAsModel();
        if (accountResults.isEmpty()) {
            // insert
            WellSql.insert(account).execute();
        } else {
            // update
            int oldId = accountResults.get(0).getId();
            WellSql.update(AccountModel.class).whereId(oldId)
                    .put(account, new InsertMapper<AccountModel>() {
                        @Override
                        public ContentValues toCv(AccountModel item) {
                            ContentValues cv = new ContentValues();
                            cv.put(AccountModelTable.USER_ID, item.getUserId());
                            cv.put(AccountModelTable.DISPLAY_NAME, item.getDisplayName());
                            cv.put(AccountModelTable.PROFILE_URL, item.getProfileUrl());
                            cv.put(AccountModelTable.AVATAR_URL, item.getAvatarUrl());
                            cv.put(AccountModelTable.SITE_COUNT, item.getSiteCount());
                            cv.put(AccountModelTable.VISIBLE_SITE_COUNT, item.getVisibleSiteCount());
                            cv.put(AccountModelTable.EMAIL, item.getEmail());
                            return cv;
                        }
                    }).execute();
        }
    }

    public static void insertOrUpdateAccountSettings(AccountModel account) {
        List<AccountModel> accountResults = WellSql.select(AccountModel.class)
                .where()
                .equals(AccountModelTable.USER_NAME, account.getUserName())
                .equals(AccountModelTable.PRIMARY_BLOG_ID, account.getPrimaryBlogId())
                .endWhere().getAsModel();
        if (accountResults.isEmpty()) {
            // insert
            WellSql.insert(account).execute();
        } else {
            // update
            int oldId = accountResults.get(0).getId();
            WellSql.update(AccountModel.class).whereId(oldId)
                    .put(account, new InsertMapper<AccountModel>() {
                        @Override
                        public ContentValues toCv(AccountModel item) {
                            ContentValues cv = new ContentValues();
                            cv.put(AccountModelTable.FIRST_NAME, item.getFirstName());
                            cv.put(AccountModelTable.LAST_NAME, item.getLastName());
                            cv.put(AccountModelTable.ABOUT_ME, item.getAboutMe());
                            cv.put(AccountModelTable.DATE, item.getDate());
                            cv.put(AccountModelTable.NEW_EMAIL, item.getNewEmail());
                            cv.put(AccountModelTable.PENDING_EMAIL_CHANGE, item.getPendingEmailChange());
                            cv.put(AccountModelTable.WEB_ADDRESS, item.getWebAddress());
                            return cv;
                        }
                    }).execute();
        }
    }

    public static AccountModel getAccountByLocalId(long localId) {
        List<AccountModel> accountResult = WellSql.select(AccountModel.class)
                .where().equals(AccountModelTable.ID, localId)
                .endWhere().getAsModel();
        return accountResult.isEmpty() ? null : accountResult.get(0);
    }
}
