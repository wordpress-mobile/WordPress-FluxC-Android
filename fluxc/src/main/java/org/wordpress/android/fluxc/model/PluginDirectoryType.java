package org.wordpress.android.fluxc.model;

public enum PluginDirectoryType {
    NEW,
    POPULAR;

    public String toString() {
        return this.name();
    }

    public static PluginDirectoryType fromString(String string) {
        if (string != null) {
            for (PluginDirectoryType type : PluginDirectoryType.values()) {
                if (string.equalsIgnoreCase(type.name())) {
                    return type;
                }
            }
        }
        return NEW;
    }
}
