package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

import java.util.LinkedList;
import java.util.Queue;

public class TestersFlow extends Thread implements BaseTester.IPublish {
    private final Queue<BaseTester> testers;
    private final Context context;
    private final ITestFlow listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startTime;

    public TestersFlow(Context context, MultiProfile.UserProfile profile, ITestFlow listener) {
        this.context = context;
        this.listener = listener;
        this.testers = new LinkedList<>();

        if (profile.connectionMethod == MultiProfile.ConnectionMethod.HTTP)
            testers.add(new HttpTester(context, profile, this));
        else
            testers.add(new WebSocketTester(context, profile, this));

        testers.add(new Aria2Tester(context, profile, this));
        if (profile.isDirectDownloadEnabled())
            testers.add(new DirectDownloadTester(context, profile, this));
    }

    @Override
    public void run() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.clearViews();
                if (listener != null) listener.setButtonEnabled(false);
            }
        });

        startTime = System.currentTimeMillis();

        for (BaseTester tester : testers)
            if (!tester.start()) break;

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.setButtonEnabled(true);
            }
        });
    }

    @Override
    public void startedNewTest(BaseTester tester) {
        publishGeneralMessage("Started " + tester.describe() + "...", android.R.color.secondary_text_light);
    }

    @Override
    public void publishGeneralMessage(String message, @ColorRes final int color) {
        final String finalMessage = (System.currentTimeMillis() - startTime) + ": " + message;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null)
                    listener.addView(new SuperTextView(context, finalMessage, ContextCompat.getColor(context, color)));
            }
        });
    }

    @Override
    public void endedTest(BaseTester tester, boolean successful) {
        publishGeneralMessage("Finished " + tester.describe(), successful ? android.R.color.secondary_text_light : R.color.red);
    }

    public interface ITestFlow {
        void addView(View view);

        void clearViews();

        void setButtonEnabled(boolean successful);
    }
}
