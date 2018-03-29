package com.gianlu.aria2app.NetIO.Aria2;

import org.json.JSONObject;


public class GlobalStats {
    public final int downloadSpeed;
    public final int uploadSpeed;
    public final int numActive;
    public final int numWaiting;
    public final int numStopped;
    public final int numStoppedTotal;

    public GlobalStats(JSONObject obj) {
        downloadSpeed = obj.optInt("downloadSpeed", 0);
        uploadSpeed = obj.optInt("uploadSpeed", 0);
        numActive = obj.optInt("numActive", 0);
        numWaiting = obj.optInt("numWaiting", 0);
        numStopped = obj.optInt("numStopped", 0);
        numStoppedTotal = obj.optInt("numStoppedTotal", 0);
    }
}
