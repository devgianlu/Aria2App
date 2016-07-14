package com.gianlu.jtitan.Aria2Helper;

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

    public static GlobalStats fromString(JSONObject string) throws JSONException {
        JSONObject jResult = string.getJSONObject("result");
        return new GlobalStats(Integer.parseInt(jResult.optString("downloadSpeed")),
                Integer.parseInt(jResult.optString("uploadSpeed")),
                Integer.parseInt(jResult.optString("numActive")),
                Integer.parseInt(jResult.optString("numWaiting")),
                Integer.parseInt(jResult.optString("numStopped")),
                Integer.parseInt(jResult.optString("numStoppedTotal")));
    }
}
