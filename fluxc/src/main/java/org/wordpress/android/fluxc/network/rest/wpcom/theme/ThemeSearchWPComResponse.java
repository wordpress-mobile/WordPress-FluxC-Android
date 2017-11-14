package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import java.util.List;

public class ThemeSearchWPComResponse {
    public class ThemeSearchArrayResponse {
        public List<ThemeSearchWPComResponse> themes;
    }

    public String id;
    public String slug;
    public String stylesheet;
    public String name;
    public String author;
    public String author_uri;
    public String theme_uri;
    public String demo_uri;
    public String version;
    public String template;
    public String screenshot;
    public String description;
    public String date_launched;
    public String date_updated;
    public String language;
    public String download_uri;
    public String price;
}
