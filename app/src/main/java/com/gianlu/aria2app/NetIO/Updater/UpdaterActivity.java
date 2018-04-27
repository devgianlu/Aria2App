package com.gianlu.aria2app.NetIO.Updater;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;

import java.util.HashSet;
import java.util.Set;

public abstract class UpdaterActivity<P> extends ActivityWithDialog implements UpdaterFramework.Interface<P> {
    private final UpdaterFramework<P> framework;
    private final Set<UpdaterFragment<P>> attachedFragments = new HashSet<>();
    private P lastPayload;

    public UpdaterActivity() {
        framework = new UpdaterFramework<>(this);
    }

    protected void onPreCreate(@Nullable Bundle savedInstanceState) {
    }

    protected void onPostCreate() {
    }

    @Override
    protected final void onCreate(@Nullable Bundle savedInstanceState) {
        onPreCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        onPostCreate();

        framework.startUpdater();
        framework.requirePayload(new AbstractClient.OnResult<P>() {
            @Override
            public void onResult(@NonNull P result) {
                lastPayload = result;
                onLoad(result);

                for (UpdaterFragment<P> fragment : attachedFragments)
                    fragment.callOnLoad(result);
            }

            @Override
            public void onException(Exception ex, boolean shouldForce) {
                onCouldntLoad(ex);
            }
        });
    }

    public void attachFragment(UpdaterFragment<P> fragment) {
        attachedFragments.add(fragment);
    }

    public void requireLoadCall(UpdaterFragment<P> fragment) {
        if (lastPayload != null) fragment.callOnLoad(lastPayload);
    }

    @Override
    public final void onPayload(@NonNull P payload) {
        lastPayload = payload;
        for (UpdaterFragment<P> fragment : attachedFragments)
            fragment.onUpdateUi(payload);

        onUpdateUi(payload);
    }

    public abstract void onUpdateUi(@NonNull P payload);

    public abstract void onLoad(@NonNull P payload);

    @Override
    protected void onResume() {
        super.onResume();
        framework.startUpdater();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        framework.stopUpdater();
    }

    @Override
    protected void onPause() {
        super.onPause();
        framework.stopUpdater();
    }

    @Override
    protected void onStop() {
        super.onStop();
        framework.stopUpdater();
    }

    @NonNull
    @Override
    public Bundle getArguments() {
        Bundle args = getIntent().getExtras();
        return args == null ? new Bundle() : args;
    }

    protected void refresh(OnRefresh listener) {
        framework.refresh(listener);
    }

    @Nullable
    protected BaseUpdater getUpdater() {
        return framework.getUpdater();
    }
}
