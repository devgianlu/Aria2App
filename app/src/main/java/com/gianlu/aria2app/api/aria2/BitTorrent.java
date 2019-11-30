package com.gianlu.aria2app.api.aria2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class BitTorrent {
    public final ArrayList<String> announceList;
    public final Mode mode;
    public final String comment;
    public final long creationDate;
    public final String name;

    private BitTorrent(@NonNull JSONObject obj) throws JSONException {
        comment = CommonUtils.optString(obj, "comment");
        creationDate = obj.optInt("creationDate", -1);
        mode = Mode.parse(obj.optString("mode"));
        announceList = new ArrayList<>();

        if (obj.has("announceList")) {
            JSONArray array = obj.getJSONArray("announceList");
            for (int i = 0; i < array.length(); i++)
                announceList.add(array.optJSONArray(i).optString(0));
        }

        if (obj.has("info")) name = CommonUtils.optString(obj.getJSONObject("info"), "name");
        else name = null;
    }

    @Nullable
    public static BitTorrent create(JSONObject obj) throws JSONException {
        if (obj.has("bittorrent")) return new BitTorrent(obj.getJSONObject("bittorrent"));
        else return null;
    }

    public enum Mode {
        MULTI,
        SINGLE;

        public static Mode parse(@Nullable String val) {
            if (val != null && "multi".equals(val.toLowerCase())) return Mode.MULTI;
            else return Mode.SINGLE;
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
}
