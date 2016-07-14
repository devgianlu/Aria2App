package com.gianlu.jtitan.Aria2Helper;

import java.util.List;

public class BitTorrent {
    // Global
    public List<String> announceList;
    public MODE mode;
    public String comment;
    public Integer creationDate;
    public String name;

    public BitTorrent(List<String> announceList, MODE mode, String comment, Integer creationDate, String name) {
        this.announceList = announceList;
        this.mode = mode;
        this.name = name;
        this.comment = comment;
        this.creationDate = creationDate;
    }

    public static MODE modeFromString(String mode) {
        if (mode == null) return MODE.SINGLE;
        switch (mode.toLowerCase()) {
            case "multi":
                return MODE.MULTI;
            case "single":
                return MODE.SINGLE;
            default:
                return MODE.SINGLE;
        }
    }


    // Mode
    public enum MODE {
        MULTI,
        SINGLE
    }
}
