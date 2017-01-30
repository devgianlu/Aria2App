package com.gianlu.aria2app.NetIO.JTA2;

import android.support.annotation.Nullable;

import org.json.JSONObject;


public class GlobalStats {
    public Integer downloadSpeed;
    public Integer uploadSpeed;
    private Integer numActive;
    private Integer numWaiting;
    private Integer numStopped;
    private Integer numStoppedTotal;

    private GlobalStats() {
    }

    @Nullable
    private static Integer parseInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            return 0;
        }
    }

    public static GlobalStats fromJSON(JSONObject jResult) {
        if (jResult == null) return null;

        GlobalStats stats = new GlobalStats();
        stats.downloadSpeed = parseInt(jResult.optString("downloadSpeed"));
        stats.uploadSpeed = parseInt(jResult.optString("uploadSpeed"));
        stats.numActive = parseInt(jResult.optString("numActive"));
        stats.numWaiting = parseInt(jResult.optString("numWaiting"));
        stats.numStopped = parseInt(jResult.optString("numStopped"));
        stats.numStoppedTotal = parseInt(jResult.optString("numStoppedTotal"));

        return stats;
    }
}
