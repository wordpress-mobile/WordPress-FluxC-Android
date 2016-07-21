package org.wordpress.android.stores.network.xmlrpc;

public enum XMLRPC {
    GET_OPTIONS("wp.getOptions"),

    GET_USERS_BLOGS("wp.getUsersBlogs"),
    LIST_METHODS("system.listMethods"),

    DELETE_PAGE("wp.deletePage"),
    DELETE_POST("wp.deletePost"),
    EDIT_POST("metaWeblog.editPost"), // Note: WPAndroid uses metaWeblog.editPost; wp.editPost is used by EditMediaItemTask
    GET_PAGE("wp.getPage"),
    GET_PAGES("wp.getPages"),
    GET_POST("metaWeblog.getPost"), // Note: WPAndroid uses metaWeblog.getPost, maybe we can use wp.getPost?
    GET_POSTS("metaWeblog.getRecentPosts"); // Note: WPAndroid uses metaWeblog.getRecentPosts, maybe we can use wp.getPosts?

    private final String mEndpoint;

    XMLRPC(String endpoint) {
        mEndpoint = endpoint;
    }

    @Override
    public String toString() {
        return mEndpoint;
    }
}
