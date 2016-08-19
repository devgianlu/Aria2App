package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.gianlu.aria2app.NetIO.JTA2.File;
import com.gianlu.aria2app.R;

import java.util.List;

public class FilesAdapter {
    private final Tree tree;

    public FilesAdapter(Tree tree, LinearLayout view) {
        this.tree = tree;

        setupViews(view.getContext(), tree.getCommonRoot());
        populateDirectory(view, tree.getCommonRoot(), 1);
    }

    @SuppressLint("InflateParams")
    private static void setupViews(Context context, TreeDirectory parent) {
        for (TreeDirectory child : parent.getChildren()) {
            DirectoryViewHolder holder = new DirectoryViewHolder(((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.directory_item, null));
            holder.name.setText(child.getName());
            child.viewHolder = holder;
            child.viewHolder.rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: Directory details
                }
            });

            setupViews(context, child);
        }

        for (TreeFile file : parent.getFiles()) {
            FileViewHolder holder = new FileViewHolder(((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.file_item, null));
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
                    // TODO: File details
                }
            });

            file.viewHolder = holder;
        }
    }

    private static void setupView(TreeFile file) {
        file.viewHolder.name.setText(file.file.getName());
        file.viewHolder.progressBar.setProgress(file.file.getProgress().intValue());
        file.viewHolder.percentage.setText(file.file.getPercentage());
        if (file.file.isCompleted()) {
            file.viewHolder.status.setImageResource(R.drawable.ic_cloud_done_black_48dp);
        } else if (file.file.selected) {
            file.viewHolder.status.setImageResource(R.drawable.ic_cloud_download_black_48dp);
        } else {
            file.viewHolder.status.setImageResource(R.drawable.ic_cloud_off_black_48dp);
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

    public void onUpdate(List<File> files) {
        // TODO: Check me
        for (File newFile : files) {
            TreeFile listFile = tree.findFile(newFile.path);

            if (listFile != null) {
                listFile.file = newFile;
                setupView(listFile);
            }
        }
    }
}
