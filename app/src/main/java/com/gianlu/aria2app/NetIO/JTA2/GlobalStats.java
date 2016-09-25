package com.gianlu.aria2app.NetIO.JTA2;

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;

import org.json.JSONObject;


class GlobalStats {
    @SuppressLint("unused")
    private Integer downloadSpeed;
    @SuppressLint("unused")
    private Integer uploadSpeed;
    @SuppressLint("unused")
    private Integer numActive;
    @SuppressLint("unused")
    private Integer numWaiting;
    @SuppressLint("unused")
    private Integer numStopped;
    @SuppressLint("unused")
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
