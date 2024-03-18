package com.halvaor.gamingknights.IDs;

import java.io.Serializable;

public class PlaygroupID implements Serializable {

    private String playgroupID;

    public PlaygroupID(String playgroupID) {
        this.playgroupID = IdPrefix.PLAYGROUP_ID + playgroupID;
    }

    public String getId() {
        return playgroupID;
    }
}
