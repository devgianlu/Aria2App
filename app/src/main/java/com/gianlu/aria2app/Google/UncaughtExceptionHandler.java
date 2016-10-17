package com.gianlu.aria2app.Google;

import com.gianlu.aria2app.BuildConfig;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.PrintWriter;
import java.io.StringWriter;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
        if (BuildConfig.DEBUG) {
            throwable.printStackTrace();
        } else {
            Tracker tracker = Analytics.getTracker();

            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));

            if (tracker != null)
                tracker.send(new HitBuilders.ExceptionBuilder()
                        .setDescription(writer.toString())
                        .setFatal(true)
                        .build());
        }
    }
}
