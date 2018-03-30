package com.gianlu.aria2app.Options;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.gianlu.aria2app.Adapters.OptionsAdapter;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.Option;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.PKeys;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class OptionsUtils {

    @NonNull
    public static JSONObject toJson(@Nullable Map<String, String> options) throws JSONException {
        if (options == null) return new JSONObject();

        JSONObject json = new JSONObject();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (Objects.equals(entry.getKey(), "header") || Objects.equals(entry.getKey(), "index-out")) {
                if (entry.getValue().contains(";")) {
                    json.put(entry.getKey(), CommonUtils.toJSONArray(entry.getValue().split(";")));
                    continue;
                }
            }

            json.put(entry.getKey(), entry.getValue());
        }

        return json;
    }

    private static void showGlobalDialog(@NonNull final ActivityWithDialog activity, List<Option> options) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.globalOptions)
                .setNegativeButton(android.R.string.cancel, null);

        OptionsView optionsView = new OptionsView(activity);
        optionsView.setIsDialog(true);

        final OptionsAdapter adapter = new OptionsAdapter(activity, options, true, false);
        adapter.setHandler(new OptionsAdapter.IAdapter() {
            @Override
            public void onEditOption(Option option) {
                showEditOptionDialog(activity, adapter, option);
            }
        });
        optionsView.setAdapter(adapter);

        builder.setView(optionsView)
                .setNeutralButton(R.string.export, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exportOptions(activity, adapter.getOptions(), true);
                    }
                })
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleApplyGlobalOptions(activity, adapter.getOptions());
                    }
                });

        _showDialog(activity, optionsView, builder);
    }

    private static void showDownloadDialog(@NonNull final ActivityWithDialog activity, final String gid, List<Option> options) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.options)
                .setNegativeButton(android.R.string.cancel, null);

        OptionsView optionsView = new OptionsView(activity);
        optionsView.setIsDialog(true);

        final OptionsAdapter adapter = new OptionsAdapter(activity, options, false, false);
        adapter.setHandler(new OptionsAdapter.IAdapter() {
            @Override
            public void onEditOption(Option option) {
                showEditOptionDialog(activity, adapter, option);
            }
        });
        optionsView.setAdapter(adapter);

        builder.setView(optionsView)
                .setNeutralButton(R.string.export, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exportOptions(activity, adapter.getOptions(), false);
                    }
                })
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleApplyDownloadOptions(activity, gid, adapter.getOptions());
                    }
                });

        _showDialog(activity, optionsView, builder);
    }

    private static void handleApplyDownloadOptions(@NonNull final ActivityWithDialog activity, String gid, List<Option> options) {
        Map<String, String> map = new HashMap<>();
        for (Option option : options)
            if (option.isValueChanged())
                map.put(option.name, option.newValue);

        try {
            activity.showDialog(DialogUtils.progressDialog(activity, R.string.gathering_information));
            Aria2Helper.instantiate(activity).request(AriaRequests.changeOptions(gid, map), new AbstractClient.OnSuccess() {
                @Override
                public void onSuccess() {
                    Toaster.show(activity, Utils.Messages.DOWNLOAD_OPTIONS_CHANGED);
                    activity.dismissDialog();
                }

                @Override
                public void onException(Exception ex, boolean shouldForce) {
                    Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
                    activity.dismissDialog();
                }
            });
        } catch (Aria2Helper.InitializingException | JSONException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
            activity.dismissDialog();
        }

        AnalyticsApplication.sendAnalytics(activity, Utils.ACTION_CHANGED_DOWNLOAD_OPTIONS);
    }

    private static void handleApplyGlobalOptions(@NonNull final ActivityWithDialog activity, List<Option> options) {
        Map<String, String> map = new HashMap<>();
        for (Option option : options)
            if (option.isValueChanged())
                map.put(option.name, option.newValue);

        try {
            activity.showDialog(DialogUtils.progressDialog(activity, R.string.gathering_information));
            Aria2Helper.instantiate(activity).request(AriaRequests.changeGlobalOptions(map), new AbstractClient.OnSuccess() {
                @Override
                public void onSuccess() {
                    Toaster.show(activity, Utils.Messages.GLOBAL_OPTIONS_CHANGED);
                    activity.dismissDialog();
                }

                @Override
                public void onException(Exception ex, boolean shouldForce) {
                    Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
                    activity.dismissDialog();
                }
            });
        } catch (Aria2Helper.InitializingException | JSONException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
            activity.dismissDialog();
        }

        AnalyticsApplication.sendAnalytics(activity, Utils.ACTION_CHANGED_GLOBAL_OPTIONS);
    }

    public static void showEditOptionDialog(Activity activity, final OptionsAdapter adapter, final Option option) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(option.name)
                .setNegativeButton(android.R.string.cancel, null);

        LinearLayout layout = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.dialog_edit_option, null, false);
        SuperTextView value = layout.findViewById(R.id.editOptionDialog_value);
        value.setHtml(R.string.currentValue, option.value == null ? "not set" : option.value);
        final EditText edit = layout.findViewById(R.id.editOptionDialog_edit);
        edit.setText(option.value);

        layout.findViewById(R.id.editOptionDialog_multipleHelp).setVisibility(
                Objects.equals(option.name, "header") || Objects.equals(option.name, "index-out") ? View.VISIBLE : View.GONE);

        builder.setView(layout)
                .setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        option.setNewValue(edit.getText().toString());
                        adapter.notifyItemChanged(option);
                    }
                });

        DialogUtils.showDialog(activity, builder);
    }

    private static void _showDialog(final ActivityWithDialog activity, final OptionsView layout, final AlertDialog.Builder builder) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Dialog dialog = builder.create();
                final Window window = dialog.getWindow();
                if (window != null) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

                    ViewTreeObserver vto = layout.getViewTreeObserver();
                    vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
                            params.copyFrom(window.getAttributes());
                            params.width = dialog.getWindow().getDecorView().getWidth();
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                            dialog.getWindow().setAttributes(params);

                            layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    });
                }

                activity.showDialog(builder);
            }
        });
    }

    public static void showGlobalDialog(@NonNull final ActivityWithDialog activity, final boolean quick) {
        activity.showDialog(DialogUtils.progressDialog(activity, R.string.gathering_information));

        try {
            final List<String> allOptions = OptionsManager.get(activity).loadGlobalOptions();

            Aria2Helper.instantiate(activity).request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<Map<String, String>>() {
                @Override
                public void onResult(Map<String, String> result) {
                    activity.dismissDialog();

                    if (quick) {
                        Set<String> quickOptions = Prefs.getSet(activity, PKeys.A2_GLOBAL_QUICK_OPTIONS, new HashSet<String>());
                        if (quickOptions.isEmpty()) {
                            Toaster.show(activity, Utils.Messages.NO_QUICK_OPTIONS);
                            activity.dismissDialog();
                            return;
                        }

                        showGlobalDialog(activity, Option.fromOptionsMap(result, allOptions, quickOptions));
                    } else {
                        showGlobalDialog(activity, Option.fromOptionsMap(result, allOptions));
                    }
                }

                @Override
                public void onException(Exception ex, boolean shouldForce) {
                    Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
                    activity.dismissDialog();
                }
            });
        } catch (Aria2Helper.InitializingException | IOException | JSONException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
            activity.dismissDialog();
        }
    }

    public static void showDownloadDialog(@NonNull final ActivityWithDialog activity, final String gid, final boolean quick) {
        activity.showDialog(DialogUtils.progressDialog(activity, R.string.gathering_information));

        try {
            final List<String> allOptions = OptionsManager.get(activity).loadDownloadOptions();

            Aria2Helper.instantiate(activity).request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<Map<String, String>>() {
                @Override
                public void onResult(Map<String, String> result) {
                    activity.dismissDialog();

                    if (quick) {
                        Set<String> quickOptions = Prefs.getSet(activity, PKeys.A2_QUICK_OPTIONS, new HashSet<String>());
                        if (quickOptions.isEmpty()) {
                            Toaster.show(activity, Utils.Messages.NO_QUICK_OPTIONS);
                            activity.dismissDialog();
                            return;
                        }

                        showDownloadDialog(activity, gid, Option.fromOptionsMap(result, allOptions, quickOptions));
                    } else {
                        showDownloadDialog(activity, gid, Option.fromOptionsMap(result, allOptions));
                    }
                }

                @Override
                public void onException(Exception ex, boolean shouldForce) {
                    Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
                    activity.dismissDialog();
                }
            });
        } catch (Aria2Helper.InitializingException | IOException | JSONException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
            activity.dismissDialog();
        }
    }

    private static void exportOptions(ActivityWithDialog activity, List<Option> options, boolean global) {
        StringBuilder builder = new StringBuilder();

        for (Option option : options) {
            if ((option.value == null || option.value.isEmpty()) && !option.isValueChanged())
                continue;

            builder.append(option.name).append('=');
            if (option.newValue != null) builder.append(option.newValue);
            else builder.append(option.value);
            builder.append('\n');
        }

        if (!Utils.requestWritePermission(activity, 3)) {
            Toaster.show(activity, Utils.Messages.EXPORT_OPTIONS_GRANT_WRITE);
            return;
        }

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "aria2app-exported-" + (global ? "global" : "download") + ".conf");
        if (file.exists())
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ThreadLocalRandom.current().nextInt(1000) + "-aria2app-exported-" + (global ? "global" : "download") + ".conf");

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false)))) {
            writer.append(builder.toString());
            writer.flush();
        } catch (IOException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_EXPORTING_OPTIONS, ex);
            return;
        }

        Toaster.show(activity, activity.getString(R.string.exportedOptions, file.getAbsolutePath()), Toast.LENGTH_LONG, null, null, null);
    }
}
