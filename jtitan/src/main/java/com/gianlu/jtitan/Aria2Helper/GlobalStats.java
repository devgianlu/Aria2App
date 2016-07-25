package com.gianlu.jtitan.Aria2Helper;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class GlobalStats {
    public Integer downloadSpeed;
    public Integer uploadSpeed;
    public Integer numActive;
    public Integer numWaiting;
    public Integer numStopped;
    public Integer numStoppedTotal;

    public GlobalStats(Integer downloadSpeed, Integer uploadSpeed, Integer numActive, Integer numWaiting, Integer numStopped, Integer numStoppedTotal) {
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.numActive = numActive;
        this.numWaiting = numWaiting;
        this.numStopped = numStopped;
        this.numStoppedTotal = numStoppedTotal;
    }

    @Nullable
    private static Integer parseInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            return null;
        }
    }

    public static GlobalStats fromString(JSONObject string) throws JSONException {
        JSONObject jResult = string.getJSONObject("result");
        return new GlobalStats(parseInt(jResult.optString("downloadSpeed")),
                parseInt(jResult.optString("uploadSpeed")),
                parseInt(jResult.optString("numActive")),
                parseInt(jResult.optString("numWaiting")),
                parseInt(jResult.optString("numStopped")),
                parseInt(jResult.optString("numStoppedTotal")));
    }
}
