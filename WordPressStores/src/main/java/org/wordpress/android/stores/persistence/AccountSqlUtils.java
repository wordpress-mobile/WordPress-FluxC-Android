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
            updateAccount(accountResults.get(0).getId(), account.getAccountContent());
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
            updateAccount(accountResults.get(0).getId(), account.getSettingsContent());
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
