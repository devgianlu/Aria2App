package com.gianlu.aria2app.NetIO;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

public interface DoBatch<R> {
    @WorkerThread
    void onSandboxReturned(@NonNull R result);

    @WorkerThread
    void onException(@NonNull Exception ex);
}
