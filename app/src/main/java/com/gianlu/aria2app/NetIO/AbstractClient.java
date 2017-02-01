package com.gianlu.aria2app.NetIO;

import org.json.JSONObject;

public abstract class AbstractClient {
    public abstract void send(JSONObject request, IReceived handler);
}
