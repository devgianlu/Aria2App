package com.gianlu.aria2app.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.commonutils.preferences.Prefs;

import java.util.Objects;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            if (Prefs.getBoolean(PK.A2_NOTIFS_AT_BOOT))
                NotificationService.start(context);

            if (Prefs.getBoolean(PK.IN_APP_DOWNLOADER_AT_BOOT))
                ((ThisApplication) context.getApplicationContext()).startAria2ServiceFromReceiver();
        }
    }
}
