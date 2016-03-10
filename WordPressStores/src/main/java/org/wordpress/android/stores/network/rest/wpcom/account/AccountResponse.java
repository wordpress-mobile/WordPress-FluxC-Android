package org.wordpress.android.stores.network.rest.wpcom.account;

import org.wordpress.android.stores.network.Response;

public class AccountResponse implements Response {
    // Account fields (/me)
    public String username;
    public long ID;
    public String display_name;
    public String profile_URL;
    public String avatar_URL;
    public long primary_blog;
    public int site_count;
    public int visible_site_count;
    public String email;

    // Account settings fields (/me/settings)
    public String user_login;
    public long primary_site_ID;
    public String first_name;
    public String last_name;
    public String description;
    public String date;
    public String new_user_email;
    public boolean user_email_change_pending;
    public String user_URL;
}
