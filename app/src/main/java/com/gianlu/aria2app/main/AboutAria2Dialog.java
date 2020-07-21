package com.gianlu.aria2app.main;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.AriaRequests;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.VersionAndSession;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.ui.Toaster;

public class AboutAria2Dialog extends DialogFragment implements AbstractClient.OnResult<VersionAndSession> {
    private static final String TAG = AboutAria2Dialog.class.getSimpleName();
    private Aria2Helper helper;
    private SuperTextView version;
    private SuperTextView features;
    private SuperTextView sessionId;
    private ProgressBar loading;

    @NonNull
    public static AboutAria2Dialog get() {
        return new AboutAria2Dialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.about_aria2);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_about_aria2, container, false);

        loading = layout.findViewById(R.id.aboutAria2Dialog_loading);
        version = layout.findViewById(R.id.aboutAria2Dialog_version);
        features = layout.findViewById(R.id.aboutAria2Dialog_features);
        sessionId = layout.findViewById(R.id.aboutAria2Dialog_sessionId);

        try {
            helper = Aria2Helper.instantiate(requireContext());
        } catch (Aria2Helper.InitializingException ex) {
            Log.e(TAG, "Failed initialising.", ex);
            DialogUtils.showToast(requireContext(), Toaster.build().message(R.string.failedLoading));
            dismissAllowingStateLoss();
            return null;
        }

        loading.setVisibility(View.VISIBLE);

        Button saveSession = layout.findViewById(R.id.aboutAria2Dialog_saveSession);
        saveSession.setOnClickListener(view -> helper.request(AriaRequests.saveSession(), new AbstractClient.OnSuccess() {
            @Override
            public void onSuccess() {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.sessionSaved));
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed saving session.", ex);
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedSavingSession));
            }
        }));

        Button ok = layout.findViewById(R.id.aboutAria2Dialog_ok);
        ok.setOnClickListener(view -> dismissAllowingStateLoss());

        helper.getVersionAndSession(this);

        return layout;
    }

    @Override
    public void onResult(@NonNull VersionAndSession result) {
        loading.setVisibility(View.GONE);
        version.setHtml(R.string.versionLabel, result.version.version);
        features.setHtml(R.string.features, CommonUtils.join(result.version.enabledFeatures, ", "));
        sessionId.setHtml(R.string.sessionId, result.session.sessionId);
    }

    @Override
    public void onException(@NonNull Exception ex) {
        if (!isAdded()) return;

        Log.e(TAG, "Failed loading info.", ex);
        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
        dismissAllowingStateLoss();
    }
}
