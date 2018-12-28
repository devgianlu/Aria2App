package com.gianlu.aria2app.Aria2;

import android.app.Activity;

import com.gianlu.aria2app.LoadingActivity;
import com.gianlu.aria2app.R;
import com.gianlu.aria2lib.BareConfigProvider;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

@Keep
public final class Aria2ConfigProvider implements BareConfigProvider {

    public Aria2ConfigProvider() {
    }

    @Override
    public int launcherIcon() {
        return R.mipmap.ic_launcher;
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
