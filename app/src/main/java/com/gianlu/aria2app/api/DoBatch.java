package com.gianlu.aria2app.api;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

public interface DoBatch<R> {
    @WorkerThread
    void onSandboxReturned(@NonNull R result);

    @WorkerThread
    void onException(@NonNull Exception ex);
}
