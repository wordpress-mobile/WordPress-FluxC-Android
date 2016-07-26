package org.wordpress.android.stores.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.stores.Payload;

import java.io.Serializable;

@Table
@RawConstraints({"UNIQUE (SITE_ID, URL)"})
public class SiteModel implements Identifiable, Payload, Serializable {

    @PrimaryKey
    @Column private int mId;
    // Only given a value for .COM and Jetpack sites - self-hosted sites use mDotOrgSiteId
    @Column private long mSiteId;
    @Column private String mUrl;
    @Column private String mAdminUrl;
    @Column private String mLoginUrl;
    @Column private String mName;
    @Column private String mDescription;
    @Column private boolean mIsWPCom;
    @Column private boolean mIsAdmin;
    @Column private boolean mIsFeaturedImageSupported;
    @Column private String mTimezone;


    // Self hosted specifics
    // The siteId for .org sites. Jetpack sites will also have a mSiteId, which is their id on .COM
    @Column private long mDotOrgSiteId;
    @Column private String mUsername;
    @Column private String mPassword;
    @Column(name = "XMLRPC_URL") private String mXmlRpcUrl;
    @Column private String mSoftwareVersion;

    // WPCom specifics
    @Column private boolean mIsJetpack;
    @Column private boolean mIsVisible;
    @Column private boolean mIsVideoPressSupported;
    @Column private long mPlanId;
    @Column private String mPlanShortName;

    // WPCom capabilities
    @Column private boolean mIsCapabilityEditPages;
    @Column private boolean mIsCapabilityEditPosts;
    @Column private boolean mIsCapabilityEditOthersPosts;
    @Column private boolean mIsCapabilityEditOthersPages;
    @Column private boolean mIsCapabilityDeletePosts;
    @Column private boolean mIsCapabilityDeleteOthersPosts;
    @Column private boolean mIsCapabilityEditThemeOptions;
    @Column private boolean mIsCapabilityEditUsers;
    @Column private boolean mIsCapabilityListUsers;
    @Column private boolean mIsCapabilityManageCategories;
    @Column private boolean mIsCapabilityManageOptions;
    @Column private boolean mIsCapabilityActivateWordads;
    @Column private boolean mIsCapabilityPromoteUsers;
    @Column private boolean mIsCapabilityPublishPosts;
    @Column private boolean mIsCapabilityUploadFiles;
    @Column private boolean mIsCapabilityDeleteUser;
    @Column private boolean mIsCapabilityRemoveUsers;
    @Column private boolean mIsCapabilityViewStats;

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    public SiteModel() {
    }

    public long getSiteId() {
        return mSiteId;
    }

    public void setSiteId(long siteId) {
        mSiteId = siteId;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getLoginUrl() {
        return mLoginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        mLoginUrl = loginUrl;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public boolean isWPCom() {
        return mIsWPCom;
    }

    public void setIsWPCom(boolean WPCom) {
        mIsWPCom = WPCom;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public String getXmlRpcUrl() {
        return mXmlRpcUrl;
    }

    public void setXmlRpcUrl(String xmlRpcUrl) {
        mXmlRpcUrl = xmlRpcUrl;
    }

    public long getDotOrgSiteId() {
        return mDotOrgSiteId;
    }

    public void setDotOrgSiteId(long dotOrgSiteId) {
        mDotOrgSiteId = dotOrgSiteId;
    }

    public boolean isAdmin() {
        return mIsAdmin;
    }

    public void setIsAdmin(boolean admin) {
        mIsAdmin = admin;
    }

    public boolean isJetpack() {
        return mIsJetpack;
    }

    public void setIsJetpack(boolean jetpack) {
        mIsJetpack = jetpack;
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public void setIsVisible(boolean visible) {
        mIsVisible = visible;
    }

    public boolean isFeaturedImageSupported() {
        return mIsFeaturedImageSupported;
    }

    public void setIsFeaturedImageSupported(boolean featuredImageSupported) {
        mIsFeaturedImageSupported = featuredImageSupported;
    }

    public String getSoftwareVersion() {
        return mSoftwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        mSoftwareVersion = softwareVersion;
    }

    public String getAdminUrl() {
        return mAdminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        mAdminUrl = adminUrl;
    }

    public boolean isVideoPressSupported() {
        return mIsVideoPressSupported;
    }

    public void setIsVideoPressSupported(boolean videoPressSupported) {
        mIsVideoPressSupported = videoPressSupported;
    }

    public boolean isCapabilityEditPages() {
        return mIsCapabilityEditPages;
    }

    public void setIsCapabilityEditPages(boolean capabilityEditPages) {
        mIsCapabilityEditPages = capabilityEditPages;
    }

    public boolean isCapabilityEditPosts() {
        return mIsCapabilityEditPosts;
    }

    public void setIsCapabilityEditPosts(boolean capabilityEditPosts) {
        mIsCapabilityEditPosts = capabilityEditPosts;
    }

    public boolean isCapabilityEditOthersPosts() {
        return mIsCapabilityEditOthersPosts;
    }

    public void setIsCapabilityEditOthersPosts(boolean capabilityEditOthersPosts) {
        mIsCapabilityEditOthersPosts = capabilityEditOthersPosts;
    }

    public boolean isCapabilityEditOthersPages() {
        return mIsCapabilityEditOthersPages;
    }

    public void setIsCapabilityEditOthersPages(boolean capabilityEditOthersPages) {
        mIsCapabilityEditOthersPages = capabilityEditOthersPages;
    }

    public boolean isCapabilityDeletePosts() {
        return mIsCapabilityDeletePosts;
    }

    public void setIsCapabilityDeletePosts(boolean capabilityDeletePosts) {
        mIsCapabilityDeletePosts = capabilityDeletePosts;
    }

    public boolean isCapabilityDeleteOthersPosts() {
        return mIsCapabilityDeleteOthersPosts;
    }

    public void setIsCapabilityDeleteOthersPosts(boolean capabilityDeleteOthersPosts) {
        mIsCapabilityDeleteOthersPosts = capabilityDeleteOthersPosts;
    }

    public boolean isCapabilityEditThemeOptions() {
        return mIsCapabilityEditThemeOptions;
    }

    public void setIsCapabilityEditThemeOptions(boolean capabilityEditThemeOptions) {
        mIsCapabilityEditThemeOptions = capabilityEditThemeOptions;
    }

    public boolean isCapabilityEditUsers() {
        return mIsCapabilityEditUsers;
    }

    public void setIsCapabilityEditUsers(boolean capabilityEditUsers) {
        mIsCapabilityEditUsers = capabilityEditUsers;
    }

    public boolean isCapabilityListUsers() {
        return mIsCapabilityListUsers;
    }

    public void setIsCapabilityListUsers(boolean capabilityListUsers) {
        mIsCapabilityListUsers = capabilityListUsers;
    }

    public boolean isCapabilityManageCategories() {
        return mIsCapabilityManageCategories;
    }

    public void setIsCapabilityManageCategories(boolean capabilityManageCategories) {
        mIsCapabilityManageCategories = capabilityManageCategories;
    }

    public boolean isCapabilityManageOptions() {
        return mIsCapabilityManageOptions;
    }

    public void setIsCapabilityManageOptions(boolean capabilityManageOptions) {
        mIsCapabilityManageOptions = capabilityManageOptions;
    }

    public boolean isCapabilityActivateWordads() {
        return mIsCapabilityActivateWordads;
    }

    public void setIsCapabilityActivateWordads(boolean capabilityActivateWordads) {
        mIsCapabilityActivateWordads = capabilityActivateWordads;
    }

    public boolean isCapabilityPromoteUsers() {
        return mIsCapabilityPromoteUsers;
    }

    public void setIsCapabilityPromoteUsers(boolean capabilityPromoteUsers) {
        mIsCapabilityPromoteUsers = capabilityPromoteUsers;
    }

    public boolean isCapabilityPublishPosts() {
        return mIsCapabilityPublishPosts;
    }

    public void setIsCapabilityPublishPosts(boolean capabilityPublishPosts) {
        mIsCapabilityPublishPosts = capabilityPublishPosts;
    }

    public boolean isCapabilityUploadFiles() {
        return mIsCapabilityUploadFiles;
    }

    public void setIsCapabilityUploadFiles(boolean capabilityUploadFiles) {
        mIsCapabilityUploadFiles = capabilityUploadFiles;
    }

    public boolean isCapabilityDeleteUser() {
        return mIsCapabilityDeleteUser;
    }

    public void setIsCapabilityDeleteUser(boolean capabilityDeleteUser) {
        mIsCapabilityDeleteUser = capabilityDeleteUser;
    }

    public boolean isCapabilityRemoveUsers() {
        return mIsCapabilityRemoveUsers;
    }

    public void setIsCapabilityRemoveUsers(boolean capabilityRemoveUsers) {
        mIsCapabilityRemoveUsers = capabilityRemoveUsers;
    }

    public boolean isCapabilityViewStats() {
        return mIsCapabilityViewStats;
    }

    public void setIsCapabilityViewStats(boolean capabilityViewStats) {
        mIsCapabilityViewStats = capabilityViewStats;
    }

    public String getTimezone() {
        return mTimezone;
    }

    public void setTimezone(String timezone) {
        mTimezone = timezone;
    }

    public String getPlanShortName() {
        return mPlanShortName;
    }

    public void setPlanShortName(String planShortName) {
        mPlanShortName = planShortName;
    }

    public long getPlanId() {
        return mPlanId;
    }

    public void setPlanId(long planId) {
        mPlanId = planId;
    }
}
