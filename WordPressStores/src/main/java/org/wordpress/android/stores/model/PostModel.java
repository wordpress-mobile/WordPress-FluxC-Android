package org.wordpress.android.stores.model;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.stores.Payload;

@Table
public class PostModel implements Identifiable, Payload {
    private static long FEATURED_IMAGE_INIT_VALUE = -2;

    @PrimaryKey
    @Column private int mId;

    @Column private int mSiteId;
    @Column private int mPostId;
    @Column private String mTitle;
    @Column private long mDateCreated;
    @Column private long mDateCreatedGmt;
    @Column private String mCategories;
    @Column private String mCustomFields;
    @Column private String mDescription;
    @Column private String mLink;
    @Column private boolean mAllowComments;
    @Column private boolean mAllowPings;
    @Column private String mExcerpt;
    @Column private String mKeywords;
    @Column private String mMoreText;
    @Column private String mPermaLink;
    @Column private String mStatus;
    @Column private int mUserId;
    @Column private String mAuthorDisplayName;
    @Column private String mAuthorId;
    @Column private String mPassword;
    @Column private long mFeaturedImageId = FEATURED_IMAGE_INIT_VALUE;
    @Column private String mPostFormat;
    @Column private String mSlug;
    @Column private double mLatitude;
    @Column private double mLongitude;

    // Page specific
    @Column private boolean mIsPage;
    @Column private String mPageParentId;
    @Column private String mPageParentTitle;

    @Column private boolean mIsLocalDraft;
    @Column private boolean mIsLocallyChanged;

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public int getId() {
        return mId;
    }

    public int getSiteId() {
        return mSiteId;
    }

    public void setSiteId(int siteId) {
        mSiteId = siteId;
    }

    public int getPostId() {
        return mPostId;
    }

    public void setPostId(int postId) {
        mPostId = postId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public long getDateCreated() {
        return mDateCreated;
    }

    public void setDateCreated(long dateCreated) {
        mDateCreated = dateCreated;
    }

    public long getDateCreatedGmt() {
        return mDateCreatedGmt;
    }

    public void setDateCreatedGmt(long dateCreatedGmt) {
        mDateCreatedGmt = dateCreatedGmt;
    }

    public String getCategories() {
        return mCategories;
    }

    public void setCategories(String categories) {
        mCategories = categories;
    }

    public String getCustomFields() {
        return mCustomFields;
    }

    public void setCustomFields(String customFields) {
        mCustomFields = customFields;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getLink() {
        return mLink;
    }

    public void setLink(String link) {
        mLink = link;
    }

    public boolean getAllowComments() {
        return mAllowComments;
    }

    public void setAllowComments(boolean allowComments) {
        mAllowComments = allowComments;
    }

    public boolean getAllowPings() {
        return mAllowPings;
    }

    public void setAllowPings(boolean allowPings) {
        mAllowPings = allowPings;
    }

    public String getExcerpt() {
        return mExcerpt;
    }

    public void setExcerpt(String excerpt) {
        mExcerpt = excerpt;
    }

    public String getKeywords() {
        return mKeywords;
    }

    public void setKeywords(String keywords) {
        mKeywords = keywords;
    }

    public String getMoreText() {
        return mMoreText;
    }

    public void setMoreText(String moreText) {
        mMoreText = moreText;
    }

    public String getPermaLink() {
        return mPermaLink;
    }

    public void setPermaLink(String permaLink) {
        mPermaLink = permaLink;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    public int getUserId() {
        return mUserId;
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    public String getAuthorDisplayName() {
        return mAuthorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName) {
        mAuthorDisplayName = authorDisplayName;
    }

    public String getAuthorId() {
        return mAuthorId;
    }

    public void setAuthorId(String authorId) {
        mAuthorId = authorId;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public long getFeaturedImageId() {
        return mFeaturedImageId;
    }

    public void setFeaturedImageId(long featuredImageId) {
        mFeaturedImageId = featuredImageId;
    }

    public String getPostFormat() {
        return mPostFormat;
    }

    public void setPostFormat(String postFormat) {
        mPostFormat = postFormat;
    }

    public String getSlug() {
        return mSlug;
    }

    public void setSlug(String slug) {
        mSlug = slug;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public PostLocation getPostLocation() {
        return new PostLocation(mLatitude, mLongitude);
    }

    public void setPostLocation(PostLocation postLocation) {
        mLatitude = postLocation.getLatitude();
        mLongitude = postLocation.getLongitude();
    }

    public void setPostLocation(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public boolean isPage() {
        return mIsPage;
    }

    public void setIsPage(boolean isPage) {
        mIsPage = isPage;
    }

    public String getPageParentId() {
        return mPageParentId;
    }

    public void setPageParentId(String pageParentId) {
        this.mPageParentId = pageParentId;
    }

    public String getPageParentTitle() {
        return mPageParentTitle;
    }

    public void setPageParentTitle(String pageParentTitle) {
        this.mPageParentTitle = pageParentTitle;
    }

    public boolean isLocalDraft() {
        return mIsLocalDraft;
    }

    public void setIsLocalDraft(boolean isLocalDraft) {
        mIsLocalDraft = isLocalDraft;
    }

    public boolean isLocallyChanged() {
        return mIsLocallyChanged;
    }

    public void setIsLocallyChanged(boolean isLocallyChanged) {
        mIsLocallyChanged = isLocallyChanged;
    }
}
