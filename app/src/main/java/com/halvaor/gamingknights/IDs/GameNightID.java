package com.halvaor.gamingknights.IDs;

import java.io.Serializable;

public class GameNightID implements Serializable {

    private final String id;

    public GameNightID(String id) {
        this.id = IdPrefix.GAMENIGHT_ID + id;
    }

    public String getId() {
        return id;
    }
}
