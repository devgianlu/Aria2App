package com.gianlu.aria2app.Options;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.gianlu.aria2app.Adapters.OptionsAdapter;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.PKeys;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Prefs;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OptionsUtils {
    private static void showGlobalDialog(final Activity activity, List<Option> options) {
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
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleApplyGlobalOptions(activity, adapter.getOptions());
                    }
                });

        _showDialog(activity, optionsView, builder);
    }

    private static void showDownloadDialog(final Activity activity, final String gid, List<Option> options) {
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
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleApplyDownloadOptions(activity, gid, adapter.getOptions());
                    }
                });

        _showDialog(activity, optionsView, builder);
    }

    private static void handleApplyDownloadOptions(final Activity activity, String gid, List<Option> options) {
        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(activity, R.string.gathering_information);
        CommonUtils.showDialog(activity, pd);

        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(activity);
        } catch (JTA2.InitializingException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
            pd.dismiss();
            return;
        }

        Map<String, String> map = new HashMap<>();

        for (Option option : options)
            if (option.isValueChanged())
                map.put(option.name, option.newValue);

        jta2.changeOption(gid, map, new JTA2.ISuccess() {
            @Override
            public void onSuccess() {
                Toaster.show(activity, Utils.Messages.DOWNLOAD_OPTIONS_CHANGED);
                pd.dismiss();
            }

            @Override
            public void onException(Exception ex) {
                Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
                pd.dismiss();
            }
        });

        ThisApplication.sendAnalytics(activity, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_CHANGED_DOWNLOAD_OPTIONS)
                .build());
    }

    private static void handleApplyGlobalOptions(final Activity activity, List<Option> options) {
        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(activity, R.string.gathering_information);
        CommonUtils.showDialog(activity, pd);

        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(activity);
        } catch (JTA2.InitializingException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
            pd.dismiss();
            return;
        }

        Map<String, String> map = new HashMap<>();

        for (Option option : options)
            if (option.isValueChanged())
                map.put(option.name, option.newValue);

        jta2.changeGlobalOption(map, new JTA2.ISuccess() {
            @Override
            public void onSuccess() {
                Toaster.show(activity, Utils.Messages.GLOBAL_OPTIONS_CHANGED);
                pd.dismiss();
            }

            @Override
            public void onException(Exception ex) {
                Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
                pd.dismiss();
            }
        });

        ThisApplication.sendAnalytics(activity, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_CHANGED_GLOBAL_OPTIONS)
                .build());
    }

    public static void showEditOptionDialog(Activity activity, final OptionsAdapter adapter, final Option option) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(option.name)
                .setNegativeButton(android.R.string.cancel, null);

        LinearLayout layout = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.edit_option_dialog, null, false);
        SuperTextView value = layout.findViewById(R.id.editOptionDialog_value);
        value.setHtml(R.string.currentValue, option.value == null ? "not set" : option.value);
        final EditText edit = layout.findViewById(R.id.editOptionDialog_edit);
        edit.setText(option.value);

        builder.setView(layout)
                .setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        option.setNewValue(edit.getText().toString());
                        adapter.notifyItemChanged(option);
                    }
                });

        CommonUtils.showDialog(activity, builder);
    }

    private static void _showDialog(final Activity activity, final OptionsView layout, final AlertDialog.Builder builder) {
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

                CommonUtils.showDialog(activity, dialog);
            }
        });
    }

    public static void showGlobalDialog(final Activity activity, final boolean quick) {
        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(activity, R.string.gathering_information);
        CommonUtils.showDialog(activity, pd);

        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(activity);
        } catch (JTA2.InitializingException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
            pd.dismiss();
            return;
        }

        final List<String> allOptions;
        try {
            allOptions = OptionsManager.get(activity).loadGlobalOptions();
        } catch (IOException | JSONException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
            pd.dismiss();
            return;
        }

        jta2.getGlobalOption(new JTA2.IOption() {
            @Override
            @SuppressWarnings("ConstantConditions")
            public void onOptions(Map<String, String> options) {
                if (quick) {
                    Set<String> quickOptions = Prefs.getSet(activity, PKeys.A2_GLOBAL_QUICK_OPTIONS, new HashSet<String>());
                    if (quickOptions.isEmpty()) {
                        Toaster.show(activity, Utils.Messages.NO_QUICK_OPTIONS);
                        pd.dismiss();
                        return;
                    }

                    showGlobalDialog(activity, Option.fromOptionsMap(options, allOptions, quickOptions));
                } else {
                    showGlobalDialog(activity, Option.fromOptionsMap(options, allOptions));
                }

                pd.dismiss();
            }

            @Override
            public void onException(Exception ex) {
                Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
                pd.dismiss();
            }
        });
    }

    public static void showDownloadDialog(final Activity activity, final String gid, final boolean quick) {
        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(activity, R.string.gathering_information);
        CommonUtils.showDialog(activity, pd);

        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(activity);
        } catch (JTA2.InitializingException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
            pd.dismiss();
            return;
        }

        final List<String> allOptions;
        try {
            allOptions = OptionsManager.get(activity).loadDownloadOptions();
        } catch (IOException | JSONException ex) {
            Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
            pd.dismiss();
            return;
        }

        jta2.getOption(gid, new JTA2.IOption() {
            @Override
            @SuppressWarnings("ConstantConditions")
            public void onOptions(Map<String, String> options) {
                if (activity == null) {
                    pd.dismiss();
                    return;
                }

                if (quick) {
                    Set<String> quickOptions = Prefs.getSet(activity, PKeys.A2_QUICK_OPTIONS, new HashSet<String>());
                    if (quickOptions.isEmpty()) {
                        Toaster.show(activity, Utils.Messages.NO_QUICK_OPTIONS);
                        pd.dismiss();
                        return;
                    }

                    showDownloadDialog(activity, gid, Option.fromOptionsMap(options, allOptions, quickOptions));
                } else {
                    showDownloadDialog(activity, gid, Option.fromOptionsMap(options, allOptions));
                }

                pd.dismiss();
            }

            @Override
            public void onException(Exception ex) {
                Toaster.show(activity, Utils.Messages.FAILED_CHANGE_OPTIONS, ex);
                pd.dismiss();
            }
        });
    }
}
