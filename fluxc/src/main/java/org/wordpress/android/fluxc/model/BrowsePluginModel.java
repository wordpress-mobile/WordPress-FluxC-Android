package org.wordpress.android.fluxc.model;


import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import java.io.Serializable;

@Table
public class BrowsePluginModel implements Identifiable, Serializable {
    @PrimaryKey @Column
    private int mId;
    @Column private String mName;

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }
}

