package com.gianlu.aria2app.NetIO;

public interface IBatch<R> {
    void onSandboxReturned(R result);

    void onException(Exception ex);
}
