package com.halvaor.gamingknights.domain.id;

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
