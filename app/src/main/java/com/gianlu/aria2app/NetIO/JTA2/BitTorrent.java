package com.gianlu.aria2app.NetIO.JTA2;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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

    private BitTorrent() {
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

    @Nullable
    private static Integer parseInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            return 0;
        }
    }

    public static BitTorrent fromJSON(JSONObject obj) {
        if (obj == null) return null;

        BitTorrent bitTorrent = new BitTorrent();
        bitTorrent.comment = obj.optString("comment");
        bitTorrent.creationDate = parseInt(obj.optString("creationDate"));
        bitTorrent.mode = modeFromString(obj.optString("mode"));
        bitTorrent.announceList = new ArrayList<>();

        if (!obj.isNull("announceList")) {
            JSONArray array = obj.optJSONArray("announceList");

            for (int i = 0; i < array.length(); i++)
                bitTorrent.announceList.add(obj.optJSONArray("announceList").optJSONArray(i).optString(0));
        }

        if (!obj.isNull("info")) bitTorrent.name = obj.optJSONObject("info").optString("name");

        return bitTorrent;
    }

    public enum MODE {
        MULTI,
        SINGLE
    }
}
