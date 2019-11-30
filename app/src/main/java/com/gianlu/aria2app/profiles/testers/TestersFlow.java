package com.gianlu.aria2app.profiles.testers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.profiles.MultiProfile;

import java.util.LinkedList;
import java.util.Queue;

public class TestersFlow extends Thread implements BaseTester.PublishListener {
    private final Queue<BaseTester<?>> testers;
    private final ITestFlow listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startTime;

    @SuppressWarnings("unchecked")
    public TestersFlow(@NonNull Context context, @NonNull MultiProfile.UserProfile profile, @NonNull ITestFlow listener) {
        this.listener = listener;
        this.testers = new LinkedList<>();

        testers.add(new NetTester(context, profile, this));
        testers.add(new Aria2Tester(context, profile, this));
        if (profile.directDownload != null)
            testers.add(new DirectDownloadTester(context, profile, this));
    }

    @Override
    public void run() {
        handler.post(() -> {
            if (listener != null) listener.clearViews();
            if (listener != null) listener.setButtonEnabled(false);
        });

        startTime = System.currentTimeMillis();

        Object lastResult = null;
        for (BaseTester tester : testers) {
            lastResult = tester.start(lastResult);
            if (lastResult == null) break;
        }

        handler.post(() -> {
            if (listener != null) listener.setButtonEnabled(true);
        });
    }

    @Override
    public void startedNewTest(@NonNull BaseTester tester) {
        publishGeneralMessage("Started " + tester.describe() + "...", BaseTester.Color.PRIMARY);
    }

    @Override
    public void publishGeneralMessage(@NonNull String message, @NonNull final BaseTester.Color color) {
        handler.post(() -> {
            if (listener != null)
                listener.addItem((System.currentTimeMillis() - startTime) + ": " + message, color);
        });
    }

    @Override
    public void endedTest(@NonNull BaseTester tester, @Nullable Object result) {
        publishGeneralMessage("Finished " + tester.describe(), result != null ? BaseTester.Color.PRIMARY : BaseTester.Color.RED);
    }

    public interface ITestFlow {
        void addItem(@NonNull String message, @NonNull BaseTester.Color color);

        void clearViews();

        void setButtonEnabled(boolean successful);
    }
}
