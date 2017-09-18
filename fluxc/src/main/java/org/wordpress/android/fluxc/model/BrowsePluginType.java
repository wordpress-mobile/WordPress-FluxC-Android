package org.wordpress.android.fluxc.model;

public enum BrowsePluginType {
    FEATURED,
    SEARCH;

    public String toString() {
        return this.name();
    }

    public static BrowsePluginType fromString(String string) {
        if (string != null) {
            for (BrowsePluginType type : BrowsePluginType.values()) {
                if (string.equalsIgnoreCase(type.name())) {
                    return type;
                }
            }
        }
        return FEATURED;
    }
}
