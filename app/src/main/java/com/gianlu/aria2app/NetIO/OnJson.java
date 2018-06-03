package com.gianlu.aria2app.NetIO;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;

public interface OnJson {
    @WorkerThread
    void onResponse(@NonNull JSONObject response) throws JSONException;

    @WorkerThread
    void onException(@NonNull Exception ex);
}

