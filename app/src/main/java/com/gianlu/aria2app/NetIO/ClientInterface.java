package com.gianlu.aria2app.NetIO;

import androidx.annotation.NonNull;

public interface ClientInterface {
    void close();

    <R> void send(@NonNull AbstractClient.AriaRequestWithResult<R> request, AbstractClient.OnResult<R> listener);

    void send(@NonNull AbstractClient.AriaRequest request, AbstractClient.OnSuccess listener);

    <R> void batch(@NonNull AbstractClient.BatchSandbox<R> sandbox, AbstractClient.OnResult<R> listener);
}
