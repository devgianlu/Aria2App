package com.gianlu.aria2app.Google;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.google.android.gms.analytics.HitBuilders;

import java.util.Arrays;
import java.util.Locale;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static Application application;
    private Activity context;

    public UncaughtExceptionHandler(Activity context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
        throwable.printStackTrace();
        Analytics.getDefaultTracker(application).send(new HitBuilders.ExceptionBuilder().setDescription(String.format(Locale.getDefault(), "Thread %d: %s @@ %s", thread.getId(), thread.getName(), throwable.toString() + "\n" + Arrays.toString(throwable.getStackTrace()))).setFatal(true).build());

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.fatalException)
                .setMessage(R.string.fatalException_message)
                .setPositiveButton(R.string.sendReport, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("message/rfc822");
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{context.getString(R.string.email)});
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Aria2App");
                        intent.putExtra(Intent.EXTRA_TEXT, "OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")" +
                                "\nOS API Level: " + android.os.Build.VERSION.SDK_INT +
                                "\nDevice: " + android.os.Build.DEVICE +
                                "\nModel (and Product): " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")" +
                                "\n\nException" + throwable.toString() +
                                "\nStacktrace: " + Arrays.toString(throwable.getStackTrace()));
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send mail to the developer..."));
                            Utils.UIToast(context, "Thank you! :)");
                        } catch (android.content.ActivityNotFoundException ex) {
                            Utils.UIToast(context, Utils.TOAST_MESSAGES.NO_EMAIL_CLIENT);
                        }

                        System.exit(1);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        System.exit(1);
                    }
                });

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.create().show();
            }
        });
    }
}
