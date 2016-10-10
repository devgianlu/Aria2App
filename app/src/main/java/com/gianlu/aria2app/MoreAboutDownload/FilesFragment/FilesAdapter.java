package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gianlu.aria2app.MoreAboutDownload.InfoFragment.UpdateUI;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.File;
import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.ISuccess;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FilesAdapter {
    public static String dir;
    private final Tree tree;
    private final LinearLayout view;

    FilesAdapter(final Tree tree, final LinearLayout view) {
        this.tree = tree;
        this.view = view;
    }

    static void setupAsync(final Activity context, final String gid, final Tree tree, final IAsync handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LinearLayout view = new LinearLayout(context);
                view.setOrientation(LinearLayout.VERTICAL);

                setupViews(context, gid, tree.getCommonRoot());
                populateDirectory(view, tree.getCommonRoot(), 1);

                handler.onSetup(tree, view);
            }
        }).start();
    }

    @SuppressLint("InflateParams")
    private static void setupViews(final Activity context, final String gid, TreeDirectory parent) {
        if (parent == null) return;

        for (TreeDirectory child : parent.getChildren()) {
            DirectoryViewHolder holder = new DirectoryViewHolder(View.inflate(context, R.layout.directory_item, null));
            holder.name.setText(child.getName());
            child.viewHolder = holder;

            setupViews(context, gid, child);
        }

        for (final TreeFile file : parent.getFiles()) {
            FileViewHolder holder = new FileViewHolder(View.inflate(context, R.layout.file_item, null));
            holder.name.setText(file.file.getName());
            holder.progressBar.setProgress(file.file.getProgress().intValue());
            holder.percentage.setText(file.file.getPercentage());
            if (file.file.isCompleted()) {
                holder.status.setImageResource(R.drawable.ic_cloud_done_black_48dp);
            } else if (file.file.selected) {
                holder.status.setImageResource(R.drawable.ic_cloud_download_black_48dp);
            } else {
                holder.status.setImageResource(R.drawable.ic_cloud_off_black_48dp);
            }
            holder.rootView.setOnClickListener(new View.OnClickListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onClick(View v) {
                    final LinearLayout view = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.file_about_dialog, null);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        ((TextView) view.findViewById(R.id.fileAboutDialog_index)).setText(Html.fromHtml(context.getString(R.string.index, file.file.index), Html.FROM_HTML_MODE_COMPACT));
                        ((TextView) view.findViewById(R.id.fileAboutDialog_path)).setText(Html.fromHtml(context.getString(R.string.path, file.file.path), Html.FROM_HTML_MODE_COMPACT));
                        ((TextView) view.findViewById(R.id.fileAboutDialog_length)).setText(Html.fromHtml(context.getString(R.string.total_length, CommonUtils.dimensionFormatter(file.file.length)), Html.FROM_HTML_MODE_COMPACT));
                        ((TextView) view.findViewById(R.id.fileAboutDialog_completedLength)).setText(Html.fromHtml(context.getString(R.string.completed_length, CommonUtils.dimensionFormatter(file.file.completedLength)), Html.FROM_HTML_MODE_COMPACT));
                    } else {
                        ((TextView) view.findViewById(R.id.fileAboutDialog_index)).setText(Html.fromHtml(context.getString(R.string.index, file.file.index)));
                        ((TextView) view.findViewById(R.id.fileAboutDialog_path)).setText(Html.fromHtml(context.getString(R.string.path, file.file.path)));
                        ((TextView) view.findViewById(R.id.fileAboutDialog_length)).setText(Html.fromHtml(context.getString(R.string.total_length, CommonUtils.dimensionFormatter(file.file.length))));
                        ((TextView) view.findViewById(R.id.fileAboutDialog_completedLength)).setText(Html.fromHtml(context.getString(R.string.completed_length, CommonUtils.dimensionFormatter(file.file.completedLength))));
                    }

                    CheckBox selected = (CheckBox) view.findViewById(R.id.fileAboutDialog_selected);
                    selected.setChecked(file.file.selected);
                    if (!UpdateUI.isTorrent) {
                        selected.setEnabled(false);
                        selected.setText(R.string.selectFileNotTorrent);
                    } else if (UpdateUI.fileNum <= 1) {
                        selected.setEnabled(false);
                        selected.setText(R.string.selectFileSingleFile);
                    } else if (UpdateUI.status != Download.STATUS.PAUSED) {
                        selected.setEnabled(false);
                        selected.setText(R.string.selectFileNotPaused);
                    } else {
                        selected.setEnabled(true);
                    }
                    selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                            final JTA2 jta2;
                            try {
                                jta2 = JTA2.newInstance(context);
                            } catch (IOException | NoSuchAlgorithmException ex) {
                                CommonUtils.UIToast(context, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, ex);
                                return;
                            }

                            final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(context, R.string.gathering_information);
                            CommonUtils.showDialog(context, pd);

                            jta2.getOption(gid, new IOption() {
                                @Override
                                public void onOptions(Map<String, String> options) {
                                    String selected = options.get("select-file");
                                    if (selected == null)
                                        selected = "";

                                    List<Integer> selected_files = new ArrayList<>();

                                    for (String i : selected.split(",")) {
                                        try {
                                            selected_files.add(Integer.parseInt(i));
                                        } catch (Exception ignored) {
                                        }
                                    }

                                    if (selected_files.size() == 0) {
                                        for (int i = 1; i <= UpdateUI.fileNum; i++) {
                                            selected_files.add(i);
                                        }
                                    }

                                    if (isChecked) {
                                        if (!selected_files.contains(file.file.index))
                                            selected_files.add(file.file.index);
                                    } else {
                                        selected_files.remove(file.file.index);
                                    }

                                    Map<String, String> newOptions = new ArrayMap<>();
                                    String newSelected = "";
                                    boolean firstItem = true;
                                    for (Integer i : selected_files) {
                                        if (!firstItem)
                                            newSelected += ",";

                                        newSelected += String.valueOf(i);

                                        firstItem = false;
                                    }
                                    newOptions.put("select-file", newSelected);

                                    jta2.changeOption(gid, newOptions, new ISuccess() {
                                        @Override
                                        public void onSuccess() {
                                            pd.dismiss();
                                            CommonUtils.UIToast(context, Utils.ToastMessages.CHANGED_SELECTION);
                                        }

                                        @Override
                                        public void onException(Exception exception) {
                                            pd.dismiss();
                                            CommonUtils.UIToast(context, Utils.ToastMessages.FAILED_CHANGE_FILE_SELECTION, exception);
                                        }
                                    });
                                }

                                @Override
                                public void onException(Exception exception) {
                                    pd.dismiss();
                                    CommonUtils.UIToast(context, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, exception);
                                }
                            });
                        }
                    });

                    View.OnClickListener uriListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            manager.setPrimaryClip(ClipData.newPlainText("uri", (String) view.getTag()));

                            CommonUtils.UIToast(context, context.getString(R.string.copiedClipboard));
                        }
                    };

                    LinearLayout urisLayout = (LinearLayout) view.findViewById(R.id.fileAboutDialog_uris);
                    urisLayout.removeAllViews();
                    if (file.file.uris.size() <= 0) {
                        view.findViewById(R.id.fileAboutDialog_urisLabel).setVisibility(View.GONE);
                    } else {
                        view.findViewById(R.id.fileAboutDialog_urisLabel).setVisibility(View.VISIBLE);

                        for (Map.Entry<File.URI_STATUS, String> uri : file.file.uris.entrySet()) {
                            TextView _uri;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                _uri = CommonUtils.fastTextView(context, Html.fromHtml(uri.getValue() + " (<b>" + uri.getKey() + "</b>)", Html.FROM_HTML_MODE_COMPACT));
                            } else {
                                _uri = CommonUtils.fastTextView(context, Html.fromHtml(uri.getValue() + " (<b>" + uri.getKey() + "</b>)"));
                            }
                            _uri.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            _uri.setPadding(50, 10, 0, 10);
                            _uri.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_effect_dark));
                            _uri.setTag(uri.getValue());
                            _uri.setOnClickListener(uriListener);

                            urisLayout.addView(_uri);
                        }
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                            .setView(view)
                            .setTitle(file.file.getName());

                    if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_directDownload", false) && dir != null) {
                        builder.setNeutralButton(R.string.downloadFile, new DownloadFileListener(context, file.file, dir));
                    }

                    CommonUtils.showDialog(context, builder);
                }
            });

            file.viewHolder = holder;
        }
    }

    private static void populateDirectory(LinearLayout parentView, TreeDirectory parentNode, int paddingMultiplier) {
        if (parentNode == null) return;

        for (final TreeDirectory subDir : parentNode.getChildren()) {
            parentView.addView(subDir.viewHolder.rootView);

            final LinearLayout subView = new LinearLayout(parentView.getContext());
            subView.setOrientation(LinearLayout.VERTICAL);
            subView.setVisibility(View.GONE);
            subView.setPadding(6 + 36 * paddingMultiplier, 6, 6, 6);
            parentView.addView(subView);

            subDir.subView = subView;
            subDir.viewHolder.toggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.animateCollapsingArrowList((ImageButton) v, isExpanded(subView));

                    if (isExpanded(subView)) {
                        collapse(subView);
                    } else {
                        expand(subView);
                    }
                }
            });

            populateDirectory(subView, subDir, paddingMultiplier + 1);
        }

        for (TreeFile file : parentNode.getFiles()) {
            parentView.addView(file.viewHolder.rootView);
        }
    }

    private static boolean isExpanded(View v) {
        return v.getVisibility() == View.VISIBLE;
    }

    private static void expand(final View v) {
        v.measure(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? RelativeLayout.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    @SuppressLint("InflateParams")
    public void onUpdate(final List<File> files) {
        if (files == null) return;

        for (File _file : files) {
            final TreeFile found = tree.findFile(_file.path);

            if (found != null) {
                int pos = view.indexOfChild(found.viewHolder.rootView);

                found.file = _file;
                found.viewHolder.percentage.setText(_file.getPercentage());
                found.viewHolder.progressBar.setProgress(_file.getProgress().intValue());
                if (found.file.isCompleted()) {
                    found.viewHolder.status.setImageResource(R.drawable.ic_cloud_done_black_48dp);
                } else if (found.file.selected) {
                    found.viewHolder.status.setImageResource(R.drawable.ic_cloud_download_black_48dp);
                } else {
                    found.viewHolder.status.setImageResource(R.drawable.ic_cloud_off_black_48dp);
                }

                if (view.getChildAt(pos) != null) {
                    view.removeViewAt(pos);
                    view.addView(found.viewHolder.rootView, pos);
                }
            }
        }
    }

    interface IAsync {
        void onSetup(Tree tree, LinearLayout view);
    }
}
