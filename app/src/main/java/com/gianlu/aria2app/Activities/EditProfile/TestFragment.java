package com.gianlu.aria2app.Activities.EditProfile;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.Testers.BaseTester;
import com.gianlu.aria2app.ProfilesManager.Testers.TestersFlow;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TestFragment extends Fragment implements TestersFlow.ITestFlow {
    private OnGetProfile listener;
    private LinearLayout testResults;
    private Button test;

    @NonNull
    public static TestFragment getInstance(Context context) {
        TestFragment fragment = new TestFragment();
        fragment.setRetainInstance(true);
        Bundle bundle = new Bundle();
        bundle.putString("title", context.getString(R.string.test));
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnGetProfile)
            listener = (OnGetProfile) context;
    }

    @Override
    public void addItem(@NonNull String message, @NonNull BaseTester.Color color) {
        if (testResults != null) {
            testResults.addView(new SuperTextView(getContext(), message, color.getResource(getContext())));
            testResults.postDelayed(() -> ((ScrollView) testResults.getParent()).fullScroll(ScrollView.FOCUS_DOWN), 100);
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
        if (getContext() == null) return;

        TestersFlow flow = new TestersFlow(getContext(), profile, this);
        flow.start();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_edit_profile_test, container, false);
        test = layout.findViewById(R.id.editProfile_test);
        test.setOnClickListener(v -> {
            if (listener != null) {
                MultiProfile.UserProfile profile = listener.getProfile();
                if (profile != null) startTest(profile);
            }
        });

        testResults = layout.findViewById(R.id.editProfile_testResults);

        return layout;
    }

    public interface OnGetProfile {
        @Nullable
        MultiProfile.UserProfile getProfile();
    }
}
