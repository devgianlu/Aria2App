package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gianlu.aria2app.MoreAboutDownload.InfoFragment.UpdateUI;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.File;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.util.List;
import java.util.Map;

public class FilesAdapter {
    private Tree tree;
    private LinearLayout view;
    private Activity context;

    public FilesAdapter(Activity context, final Tree tree, final LinearLayout view) {
        this.tree = tree;
        this.view = view;
        this.context = context;

        setupViews(context, tree.getCommonRoot());
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                populateDirectory(view, tree.getCommonRoot(), 1);
            }
        });
    }

    @SuppressLint("InflateParams")
    private static void setupViews(final Activity context, TreeDirectory parent) {
        for (TreeDirectory child : parent.getChildren()) {
            DirectoryViewHolder holder = new DirectoryViewHolder(View.inflate(context, R.layout.directory_item, null));
            holder.name.setText(child.getName());
            holder.progressBar.setProgress(child.getProgress().intValue());
            holder.percentage.setText(child.getPercentage());
            child.viewHolder = holder;

            setupViews(context, child);
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
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);

                    final LinearLayout view = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.file_about_dialog, null);
                    ((TextView) view.findViewById(R.id.fileAboutDialog_index)).setText(Html.fromHtml(context.getString(R.string.index, file.file.index)));
                    ((TextView) view.findViewById(R.id.fileAboutDialog_path)).setText(Html.fromHtml(context.getString(R.string.path, file.file.path)));
                    ((TextView) view.findViewById(R.id.fileAboutDialog_length)).setText(Html.fromHtml(context.getString(R.string.total_length, Utils.dimensionFormatter(file.file.length))));
                    ((TextView) view.findViewById(R.id.fileAboutDialog_completedLength)).setText(Html.fromHtml(context.getString(R.string.completed_length, Utils.dimensionFormatter(file.file.completedLength))));
                    CheckBox selected = (CheckBox) view.findViewById(R.id.fileAboutDialog_selected);
                    if (!UpdateUI.isTorrent) {
                        selected.setEnabled(false);
                        selected.setText(R.string.selectFileNotTorrent);
                    } else if (UpdateUI.isSingleFile) {
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
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            // TODO
                        }
                    });

                    View.OnClickListener uriListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            manager.setPrimaryClip(ClipData.newPlainText("uri", (String) view.getTag()));

                            Utils.UIToast(context, context.getString(R.string.copiedClipboard));
                        }
                    };

                    LinearLayout urisLayout = (LinearLayout) view.findViewById(R.id.fileAboutDialog_uris);
                    urisLayout.removeAllViews();
                    for (Map.Entry<File.URI_STATUS, String> uri : file.file.uris.entrySet()) {
                        TextView _uri = new TextView(context);
                        _uri.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        _uri.setPadding(50, 10, 0, 10);
                        _uri.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_effect_dark));
                        _uri.setText(Html.fromHtml(uri.getValue() + " (<b>" + uri.getKey() + "</b>)"));
                        _uri.setTag(uri.getValue());
                        _uri.setOnClickListener(uriListener);

                        urisLayout.addView(_uri);
                    }

                    builder.setView(view)
                            .setTitle(file.file.getName());

                    builder.create().show();
                }
            });

            file.viewHolder = holder;
        }
    }

    private static void populateDirectory(LinearLayout parentView, TreeDirectory parentNode, int paddingMultiplier) {
        for (TreeDirectory subDir : parentNode.getChildren()) {
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
                    if (isExpanded(subView))
                        collapse(subView);
                    else
                        expand(subView);
                }
            });

            populateDirectory(subView, subDir, paddingMultiplier + 1);
        }

        for (TreeFile file : parentNode.getFiles()) {
            parentView.addView(file.viewHolder.rootView);
        }
    }

    public static boolean isExpanded(View v) {
        return v.getVisibility() == View.VISIBLE;
    }

    public static void expand(final View v) {
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

    public static void collapse(final View v) {
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

                view.removeViewAt(pos);
                view.addView(found.viewHolder.rootView, pos);
            }
        }

        // TODO: Now calls directories sum
    }
}
