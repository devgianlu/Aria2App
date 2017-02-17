package com.gianlu.aria2app;

import android.content.Context;

import com.gianlu.commonutils.UncaughtExceptionActivity;
import com.google.android.gms.analytics.HitBuilders;

import java.io.PrintWriter;
import java.io.StringWriter;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Context context;

    public UncaughtExceptionHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
        if (BuildConfig.DEBUG) {
            throwable.printStackTrace();
        } else {
            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));

            ThisApplication.sendAnalytics(context, new HitBuilders.ExceptionBuilder()
                    .setDescription(writer.toString())
                    .setFatal(true)
                    .build());

            UncaughtExceptionActivity.startActivity(context, context.getString(R.string.app_name));
        }
    }
}
