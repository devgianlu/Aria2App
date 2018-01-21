package com.gianlu.aria2app.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gianlu.aria2app.PKeys;
import com.gianlu.commonutils.Preferences.Prefs;

import java.util.Objects;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED) && Prefs.getBoolean(context, PKeys.A2_NOTIFS_AT_BOOT, true))
            NotificationService.start(context, false);
    }
}
