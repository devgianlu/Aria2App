package com.gianlu.aria2app.NetIO;

public interface DoBatch<R> {
    void onSandboxReturned(R result);

    void onException(Exception ex);
}
