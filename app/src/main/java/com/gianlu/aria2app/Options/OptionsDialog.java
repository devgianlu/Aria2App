package com.gianlu.aria2app.Options;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.gianlu.aria2app.Adapters.OptionsAdapter;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.Option;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class OptionsDialog extends DialogFragment implements AbstractClient.OnResult<Map<String, String>>, OptionsAdapter.Listener {
    private ProgressBar loading;
    private OptionsView optionsView;
    private Button export;
    private Button apply;
    private boolean quick = false;
    private boolean global = true;
    private String gid = null;
    private Aria2Helper helper;

    @NonNull
    public static OptionsDialog getDownload(String gid, boolean quick) {
        OptionsDialog dialog = new OptionsDialog();
        Bundle args = new Bundle();
        args.putBoolean("global", false);
        args.putBoolean("quick", quick);
        args.putString("gid", gid);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    public static OptionsDialog getGlobal(boolean quick) {
        OptionsDialog dialog = new OptionsDialog();
        Bundle args = new Bundle();
        args.putBoolean("global", true);
        args.putBoolean("quick", quick);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (args != null && args.getBoolean("global", false))
            dialog.setTitle(R.string.globalOptions);
        else
            dialog.setTitle(R.string.downloadOptions);

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                final Window window = dialog.getWindow();
                if (window != null)
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });
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
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_options, container, false);
        loading = layout.findViewById(R.id.optionsDialog_loading);
        optionsView = layout.findViewById(R.id.optionsDialog_options);
        export = layout.findViewById(R.id.optionsDialog_export);
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                export();
            }
        });
        apply = layout.findViewById(R.id.optionsDialog_apply);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apply();
            }
        });
        Button cancel = layout.findViewById(R.id.optionsDialog_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });

        loading.setVisibility(View.VISIBLE);
        optionsView.setVisibility(View.GONE);
        export.setVisibility(View.GONE);
        apply.setVisibility(View.GONE);

        Bundle args = getArguments();
        if (args == null) {
            dismissAllowingStateLoss();
            return null;
        }

        quick = args.getBoolean("quick", false);
        global = args.getBoolean("global", true);
        gid = args.getString("gid", null);

        if (quick && Prefs.isSetEmpty(PK.A2_QUICK_OPTIONS_MIXED)) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.noQuickOptions));
            dismissAllowingStateLoss();
            return null;
        }

        AbstractClient.AriaRequestWithResult<Map<String, String>> req;
        if (global || gid == null) req = AriaRequests.getGlobalOptions();
        else req = AriaRequests.getDownloadOptions(gid);

        try {
            helper = Aria2Helper.instantiate(requireContext());
        } catch (Aria2Helper.InitializingException ex) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
            dismissAllowingStateLoss();
            return null;
        }

        helper.request(req, this);

        return layout;
    }

    private void apply() {
        OptionsAdapter adapter = optionsView.getAdapter();
        if (adapter == null) return;

        Map<String, String> map = new HashMap<>();
        for (Option option : adapter.getOptions())
            if (option.isValueChanged())
                map.put(option.name, option.newValue);

        AbstractClient.AriaRequest req;
        try {
            if (global) req = AriaRequests.changeGlobalOptions(map);
            else req = AriaRequests.changeDownloadOptions(gid, map);
        } catch (JSONException ex) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedChangingOptions).ex(ex));
            return;
        }

        if (getContext() == null) return;

        final ProgressDialog pd = DialogUtils.progressDialog(getContext(), R.string.gathering_information);
        DialogUtils.showDialog(getActivity(), pd);
        helper.request(req, new AbstractClient.OnSuccess() {
            @Override
            public void onSuccess() {
                pd.dismiss();
                DialogUtils.showToast(getContext(), Toaster.build().message(global ? R.string.globalOptionsChanged : R.string.downloadOptionsChanged));
                dismissAllowingStateLoss();
                AnalyticsApplication.sendAnalytics(global ? Utils.ACTION_CHANGED_GLOBAL_OPTIONS : Utils.ACTION_CHANGED_DOWNLOAD_OPTIONS);
            }

            @Override
            public void onException(Exception ex) {
                pd.dismiss();
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedChangingOptions).ex(ex));
            }
        });
    }

    private void doExportOptions() {
        OptionsAdapter adapter = optionsView.getAdapter();
        if (adapter == null) return;

        StringBuilder builder = new StringBuilder();
        for (Option option : adapter.getOptions()) {
            if ((option.value == null || option.value.isEmpty()) && !option.isValueChanged())
                continue;

            builder.append(option.name).append('=');
            if (option.newValue != null) builder.append(option.newValue);
            else builder.append(option.value);
            builder.append('\n');
        }

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "aria2app-exported-" + (global ? "global" : "download") + ".conf");
        if (file.exists())
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ThreadLocalRandom.current().nextInt(1000) + "-aria2app-exported-" + (global ? "global" : "download") + ".conf");

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false)))) {
            writer.append(builder.toString());
            writer.flush();
        } catch (IOException ex) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedExportingOptions).ex(ex));
            return;
        }

        dismissAllowingStateLoss();
        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.exportedOptions, file.getAbsolutePath()));
    }

    private void export() {
        if (getActivity() == null) return;

        AskPermission.ask(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, new AskPermission.Listener() {
            @Override
            public void permissionGranted(@NonNull String permission) {
                doExportOptions();
            }

            @Override
            public void permissionDenied(@NonNull String permission) {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.writePermissionDenied).error(true));
            }

            @Override
            public void askRationale(@NonNull AlertDialog.Builder builder) {
                builder.setTitle(R.string.writeExternalStorageRequest_title)
                        .setMessage(R.string.exportOptionsGrantWrite);
            }
        });
    }

    @Override
    public void onResult(@NonNull Map<String, String> result) {
        if (getContext() == null) return;

        loading.setVisibility(View.GONE);
        optionsView.setVisibility(View.VISIBLE);
        apply.setVisibility(View.VISIBLE);
        export.setVisibility(View.VISIBLE);

        try {
            optionsView.setAdapter(OptionsAdapter.setup(getContext(), result, global, quick, false, this));
        } catch (IOException | JSONException ex) {
            onException(ex);
        }
    }

    @Override
    public void onException(Exception ex) {
        if (!isAdded()) return;

        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
        dismissAllowingStateLoss();
    }

    @Override
    public void onEditOption(@NonNull final Option option) {
        if (getContext() == null) return;
        DialogUtils.showDialog(getActivity(), OptionsUtils.getEditOptionDialog(getContext(), option, optionsView.getAdapter()));
    }
}
