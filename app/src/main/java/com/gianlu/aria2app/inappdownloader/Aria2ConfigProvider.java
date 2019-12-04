package com.gianlu.aria2app.inappdownloader;

import android.app.Activity;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.gianlu.aria2app.LoadingActivity;
import com.gianlu.aria2app.R;
import com.gianlu.aria2lib.BareConfigProvider;

@Keep
public final class Aria2ConfigProvider implements BareConfigProvider {

    public Aria2ConfigProvider() {
    }

    @Override
    public int launcherIcon() {
        return R.mipmap.ic_launcher_round;
    }

    @Override
    public int notificationIcon() {
        return R.drawable.ic_aria2_notification;
    }

    @NonNull
    @Override
    public Class<? extends Activity> actionClass() {
        return LoadingActivity.class;
    }
}
