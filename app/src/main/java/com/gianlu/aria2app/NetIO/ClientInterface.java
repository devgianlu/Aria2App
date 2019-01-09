package com.gianlu.aria2app.NetIO;

import androidx.annotation.NonNull;

public interface ClientInterface {
    void close();

    <R> void send(@NonNull final AbstractClient.AriaRequestWithResult<R> request, final AbstractClient.OnResult<R> listener);

    void send(@NonNull AbstractClient.AriaRequest request, AbstractClient.OnSuccess listener);

    <R> void batch(AbstractClient.BatchSandbox<R> sandbox, AbstractClient.OnResult<R> listener);
}
