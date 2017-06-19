package com.gianlu.aria2app.Activities.EditProfile;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

// TODO: Stop button
public class TestFragment extends FieldErrorFragment {
    public static final int RESULT_POSITIVE = 0;
    public static final int RESULT_NEGATIVE = 1;
    private ITestProfile handler;
    private LinearLayout testResults;
    private Button test;

    public static TestFragment getInstance(Context context, ITestProfile handler) {
        TestFragment fragment = new TestFragment();
        fragment.handler = handler;
        Bundle bundle = new Bundle();
        bundle.putString("title", context.getString(R.string.test));
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.edit_profile_test_fragment, container, false);
        test = (Button) layout.findViewById(R.id.editProfile_test);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onTestRequested();
                if (testResults != null) testResults.removeAllViews();
                test.setEnabled(false);
            }
        });

        testResults = (LinearLayout) layout.findViewById(R.id.editProfile_testResults);

        return layout;
    }

    @Override
    public void onFieldError(int id, String message) {
        Context context = getContext();
        if (context == null) return;

        switch (id) {
            case RESULT_POSITIVE:
                testResults.addView(new SuperTextView(context, message, Color.GREEN));
                test.setEnabled(true);
                break;
            case RESULT_NEGATIVE:
                testResults.addView(new SuperTextView(context, message, Color.RED));
                test.setEnabled(true);
                break;
        }
    }

    public interface ITestProfile {
        void onTestRequested();
    }
}
