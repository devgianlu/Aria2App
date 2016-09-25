package com.gianlu.aria2app.Options;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;

import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OptionsDialog extends AlertDialog.Builder {
    private final Activity context;
    private final int allowedOptions;
    private final boolean quickOptionsFilter;
    private final IDialog handler;
    private final FrameLayout view;
    private JTA2 jta2;
    private boolean hideHearts;

    public OptionsDialog(@NonNull final Activity context, @ArrayRes int allowedOptions, boolean quickOptionsFilter, IDialog handler) {
        super(context);

        this.context = context;
        this.allowedOptions = allowedOptions;
        this.quickOptionsFilter = quickOptionsFilter;
        if (quickOptionsFilter)
            setTitle(R.string.menu_downloadQuickOptions);
        else
            setTitle(R.string.options);
        this.handler = handler;
        view = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.options_dialog, null);

        setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        try {
            jta2 = JTA2.newInstance(context);
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
        }
    }

    public OptionsDialog hideHearts() {
        hideHearts = true;
        return this;
    }

    public void showDialog() {
        if (quickOptionsFilter) {
            Set<String> quickOptions = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("a2_quickOptions", new HashSet<String>());
            if (quickOptions.size() <= 0) {
                Utils.UIToast(context, Utils.TOAST_MESSAGES.ADD_QUICK_OPTIONS);
                return;
            }
        }

        final Map<String, Option> localOptions = Option.loadOptionsMap(context);

        if (localOptions == null) {
            final ProgressDialog pd = Utils.fastIndeterminateProgressDialog(context, R.string.downloading_source);

            new Thread(new DownloadSource(context, new DownloadSource.IDownload() {
                @Override
                public void onStart() {
                    Utils.showDialog(context, pd);
                }

                @Override
                public void onDone() {
                    pd.dismiss();
                    Utils.UIToast(context, Utils.TOAST_MESSAGES.SOURCE_REFRESHED);
                    showDialog();
                }

                @Override
                public void onConnectionFailed(int code, String message) {
                    pd.dismiss();
                    Utils.UIToast(context, Utils.TOAST_MESSAGES.CANT_REFRESH_SOURCE, Utils.formatConnectionError(code, message));
                }

                @Override
                public void onException(Exception ex) {
                    pd.dismiss();
                    Utils.UIToast(context, Utils.TOAST_MESSAGES.CANT_REFRESH_SOURCE, ex);
                }
            })).start();
        } else {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showThatDialog(localOptions);
                }
            });
        }
    }

    private void showThatDialog(final Map<String, Option> localOptions) {
        if (jta2 == null) return;

        final ProgressDialog pd = Utils.fastIndeterminateProgressDialog(context, R.string.gathering_information);
        Utils.showDialog(context, pd);

        final Set<String> quickOptions = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("a2_quickOptions", new HashSet<String>());

        jta2.getGlobalOption(new IOption() {
            @Override
            public void onOptions(final Map<String, String> options) {
                final List<OptionHeader> headers = new ArrayList<>();
                final Map<OptionHeader, OptionChild> children = new ArrayMap<>();

                for (String resLongOption : context.getResources().getStringArray(allowedOptions)) {
                    if (quickOptionsFilter && !quickOptions.contains(resLongOption)) continue;

                    Option opt = localOptions.get(resLongOption);
                    if (opt == null) {
                        OptionHeader header = new OptionHeader(resLongOption, null, options.get(resLongOption), quickOptions.contains(resLongOption));
                        headers.add(header);

                        children.put(header, new OptionChild(Option.TYPE.STRING, null, options.get(resLongOption), null));
                        continue;
                    }

                    OptionHeader header = new OptionHeader(resLongOption, opt.short_option, options.get(opt.long_option), quickOptions.contains(resLongOption));
                    headers.add(header);

                    children.put(header, new OptionChild(opt.type, opt.def, options.get(opt.long_option), opt.values));
                }

                pd.dismiss();

                ((ExpandableListView) view.findViewById(R.id.optionsDialog_list)).setAdapter(new OptionAdapter(context, headers, children, quickOptionsFilter, hideHearts));
                setView(view);

                setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Map<String, String> map = new HashMap<>();

                        for (Map.Entry<OptionHeader, OptionChild> item : children.entrySet()) {
                            if (!item.getValue().isChanged()) continue;
                            map.put(item.getKey().getOptionLong(), String.valueOf(item.getValue().getValue()));
                        }

                        handler.onApply(jta2, map);
                    }
                });

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final AlertDialog dialog = create();

                        Utils.showDialog(context, dialog);
                        Window window = dialog.getWindow();
                        if (window != null) 
                            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

                        ViewTreeObserver vto = view.getViewTreeObserver();
                        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                dialog.getWindow().setLayout(dialog.getWindow().getDecorView().getWidth(), dialog.getWindow().getDecorView().getHeight());
                                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        });
                    }
                });
            }

            @Override
            public void onException(Exception exception) {
                pd.dismiss();
                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
            }
        });
    }

    public interface IDialog {
        void onApply(JTA2 jta2, Map<String, String> options);
    }
}
