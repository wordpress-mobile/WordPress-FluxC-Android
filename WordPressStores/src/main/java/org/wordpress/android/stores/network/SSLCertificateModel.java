package org.wordpress.android.stores.network;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;

@RawConstraints({"UNIQUE (DOMAIN)"})
public class SSLCertificateModel implements Identifiable {
    @PrimaryKey
    @Column private int mId;
    @Column private String mDomain;
    @Column private String mCertificate;

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    public String getDomain() {
        return mDomain;
    }

    public void setDomain(String domain) {
        mDomain = domain;
    }

    public String getCertificate() {
        return mCertificate;
    }

    public void setCertificate(String certificate) {
        mCertificate = certificate;
    }
}
