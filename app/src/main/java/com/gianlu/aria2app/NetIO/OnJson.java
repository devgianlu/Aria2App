package com.gianlu.aria2app.NetIO;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

public interface OnJson {
    @WorkerThread
    void onResponse(@NonNull JSONObject response) throws JSONException;

    @WorkerThread
    void onException(@NonNull Exception ex);
}

