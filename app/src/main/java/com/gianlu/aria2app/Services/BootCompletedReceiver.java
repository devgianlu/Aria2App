package com.gianlu.aria2app.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gianlu.aria2app.PK;
import com.gianlu.commonutils.Preferences.Prefs;

import java.util.Objects;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED) && Prefs.getBoolean(PK.A2_NOTIFS_AT_BOOT))
            NotificationService.start(context);
    }
}
