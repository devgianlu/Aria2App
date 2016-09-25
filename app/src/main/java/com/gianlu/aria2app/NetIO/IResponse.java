package com.gianlu.aria2app.NetIO;

public interface IResponse {
    void onResponse(String response);

    void onException(Exception exception);

    void onFailed(int code, String message);
}
