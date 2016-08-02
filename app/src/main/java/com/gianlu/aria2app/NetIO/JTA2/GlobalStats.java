package com.gianlu.aria2app.NetIO.JTA2;

import android.support.annotation.Nullable;

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
