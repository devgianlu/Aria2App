package com.gianlu.aria2app.NetIO.JTA2;

import org.json.JSONObject;


public class GlobalStats {
    public final int downloadSpeed;
    public final int uploadSpeed;
    public final int numActive;
    public final int numWaiting;
    public final int numStopped;
    public final int numStoppedTotal;

    public GlobalStats(JSONObject obj) {
        downloadSpeed = Integer.parseInt(obj.optString("downloadSpeed", "0"));
        uploadSpeed = Integer.parseInt(obj.optString("uploadSpeed", "0"));
        numActive = Integer.parseInt(obj.optString("numActive", "0"));
        numWaiting = Integer.parseInt(obj.optString("numWaiting", "0"));
        numStopped = Integer.parseInt(obj.optString("numStopped", "0"));
        numStoppedTotal = Integer.parseInt(obj.optString("numStoppedTotal", "0"));
    }
}
