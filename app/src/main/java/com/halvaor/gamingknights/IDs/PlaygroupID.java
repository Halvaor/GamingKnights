package com.halvaor.gamingknights.IDs;

import java.io.Serializable;

public class PlaygroupID implements Serializable {

    private final String id;

    public PlaygroupID(String playgroupID) {
        this.id = IdPrefix.PLAYGROUP_ID + playgroupID;
    }

    public String getId() {
        return id;
    }
}
