package com.gianlu.aria2app;

import android.content.Context;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.UncaughtExceptionActivity;
import com.google.android.gms.analytics.HitBuilders;

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
            ThisApplication.sendAnalytics(context, new HitBuilders.ExceptionBuilder()
                    .setDescription(Logging.getStackTrace(throwable))
                    .setFatal(true)
                    .build());

            UncaughtExceptionActivity.startActivity(context, context.getString(R.string.app_name), throwable);
        }
    }
}
