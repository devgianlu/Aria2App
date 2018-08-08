package com.gianlu.aria2app.Main;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.VersionAndSession;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;

public class AboutAria2Dialog extends DialogFragment implements AbstractClient.OnResult<VersionAndSession> {
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogFix);
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
            DialogUtils.showToast(requireContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
            dismissAllowingStateLoss();
            return null;
        }

        loading.setVisibility(View.VISIBLE);

        Button saveSession = layout.findViewById(R.id.aboutAria2Dialog_saveSession);
        saveSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helper.request(AriaRequests.saveSession(), new AbstractClient.OnSuccess() {
                    @Override
                    public void onSuccess() {
                        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.sessionSaved));
                    }

                    @Override
                    public void onException(Exception ex) {
                        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedSavingSession).ex(ex));
                    }
                });
            }
        });

        Button ok = layout.findViewById(R.id.aboutAria2Dialog_ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissAllowingStateLoss();
            }
        });

        helper.getVersionAndSession(this);

        return layout;
    }

    @Override
    public void onResult(@NonNull VersionAndSession result) {
        loading.setVisibility(View.GONE);
        version.setHtml(R.string.version, result.version.version);
        features.setHtml(R.string.features, CommonUtils.join(result.version.enabledFeatures, ", "));
        sessionId.setHtml(R.string.sessionId, result.session.sessionId);
    }

    @Override
    public void onException(Exception ex) {
        DialogUtils.showToast(requireContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
        dismissAllowingStateLoss();
    }
}
