package com.gianlu.aria2app.Activities.EditProfile;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.Testers.TestersFlow;
import com.gianlu.aria2app.R;

public class TestFragment extends Fragment implements TestersFlow.ITestFlow {
    private IGetProfile handler;
    private LinearLayout testResults;
    private Button test;

    public static TestFragment getInstance(Context context, IGetProfile handler) {
        TestFragment fragment = new TestFragment();
        fragment.handler = handler;
        Bundle bundle = new Bundle();
        bundle.putString("title", context.getString(R.string.test));
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void addView(View view) {
        if (testResults != null) {
            testResults.addView(view);
            testResults.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((ScrollView) testResults.getParent()).fullScroll(ScrollView.FOCUS_DOWN);
                }
            }, 100);
        }
    }

    @Override
    public void clearViews() {
        if (testResults != null) testResults.removeAllViews();
    }

    @Override
    public void setButtonEnabled(boolean enabled) {
        if (test != null) test.setEnabled(enabled);
    }

    private void startTest(@NonNull MultiProfile.UserProfile profile) {
        TestersFlow flow = new TestersFlow(getContext(), profile, this);
        flow.start();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_edit_profile_test, container, false);
        test = layout.findViewById(R.id.editProfile_test);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) {
                    MultiProfile.UserProfile profile = handler.getProfile();
                    if (profile != null) startTest(profile);
                }
            }
        });

        testResults = layout.findViewById(R.id.editProfile_testResults);

        return layout;
    }

    public interface IGetProfile {
        @Nullable
        MultiProfile.UserProfile getProfile();
    }
}
