package com.halvaor.gamingknights.util;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class UserID implements Serializable {

    private final String id;

    public UserID(String id) {

        this.id = IdPrefix.USER_ID + id;
    }

    public String getId() {
        return id;
    }
}
