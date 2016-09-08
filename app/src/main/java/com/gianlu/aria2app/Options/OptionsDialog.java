package com.gianlu.aria2app.Options;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.annotation.ArrayRes;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.ArrayMap;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ExpandableListView;

import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionsDialog extends AlertDialog.Builder {
    private ProgressDialog pd;
    private JTA2 jta2;
    private Activity context;
    private int allowedOptions;
    private int colorRes;
    private IDialog handler;
    private ExpandableListView view;

    @SuppressLint("InflateParams")
    public OptionsDialog(@NonNull final Activity context, @ArrayRes int allowedOptions, @ColorRes int colorRes, IDialog handler) {
        super(context);

        this.context = context;
        this.allowedOptions = allowedOptions;
        this.colorRes = colorRes;
        this.handler = handler;
        view = new ExpandableListView(context);
        setTitle(R.string.menu_globalOptions);
        setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        try {
            jta2 = JTA2.newInstance(context);
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
            return;
        }

        pd = Utils.fastProgressDialog(context, R.string.gathering_information, true, false);
        Utils.showDialog(context, pd);
    }

    public void showDialog() {
        jta2.getGlobalOption(new IOption() {
            @Override
            public void onOptions(final Map<String, String> options) {
                LocalParser.loadOptions(context, false, new LocalParser.IParsing() {
                    @SuppressLint("InflateParams")
                    @Override
                    public void onEnd() {
                        final List<OptionHeader> headers = new ArrayList<>();
                        final Map<OptionHeader, OptionChild> children = new ArrayMap<>();

                        for (String resOption : context.getResources().getStringArray(allowedOptions)) {
                            try {
                                OptionHeader header = new OptionHeader(resOption, LocalParser.getCommandLine(resOption), options.get(resOption), false);
                                headers.add(header);

                                children.put(header, new OptionChild(
                                        LocalParser.getDefinition(resOption),
                                        String.valueOf(LocalParser.getDefaultValue(resOption)),
                                        String.valueOf(options.get(resOption))));
                            } catch (JSONException ex) {
                                pd.dismiss();
                                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                            }
                        }

                        pd.dismiss();

                        view.setPadding(16, 16, 16, 16);
                        view.setAdapter(new OptionAdapter(context, colorRes, headers, children));
                        setView(view);

                        setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Map<String, String> map = new HashMap<>();

                                for (Map.Entry<OptionHeader, OptionChild> item : children.entrySet()) {
                                    if (!item.getValue().isChanged()) continue;
                                    map.put(item.getKey().getOptionName(), item.getValue().getValue());
                                }

                                handler.onApply(jta2, map);
                            }
                        });

                    }

                    @Override
                    public void onError(@Nullable Exception exception) {
                        pd.dismiss();
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                    }
                });

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final AlertDialog dialog = create();

                        Utils.showDialog(context, dialog);
                        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

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
