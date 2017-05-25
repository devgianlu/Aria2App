package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.TreeNode;
import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int ITEM_DIR = 0;
    private static final int ITEM_FILE = 1;
    private final List<TreeNode> currentNodes = new ArrayList<>();
    private final LayoutInflater inflater;
    private final int color;
    private final IAdapter handler;
    private TreeNode currentNode;

    public FilesAdapter(Context context, @ColorRes int colorRes, IAdapter handler) {
        this.inflater = LayoutInflater.from(context);
        this.color = ContextCompat.getColor(context, colorRes);
        this.handler = handler;
    }

    public void update(List<AFile> files, String commonRoot) {
        if (currentNode == null) {
            currentNode = TreeNode.create(files, commonRoot);
            notifyDirChanged();
            return;
        }

        for (AFile file : files) notifyItemChanged(file);
    }

    public void notifyItemChanged(AFile file) {
        int pos = currentNode.indexOfObj(file);
        if (pos != -1) {
            TreeNode newNode = currentNodes.get(pos + currentNode.dirs.size()).update(file);
            currentNode.files.set(pos, newNode);
            currentNodes.set(pos + currentNode.dirs.size(), newNode);
            notifyItemChanged(pos + currentNode.dirs.size(), file);
        }
    }

    public void navigateUp() {
        if (currentNode == null) return;
        if (!currentNode.isRoot()) cd(currentNode.parent);
    }

    private void cd(TreeNode node) {
        currentNode = node;
        notifyDirChanged();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            if (holder instanceof FileViewHolder) {
                FileViewHolder castHolder = (FileViewHolder) holder;
                AFile payload = (AFile) payloads.get(0);

                castHolder.progressBar.setProgress((int) payload.getProgress());
                castHolder.percentage.setText(String.format(Locale.getDefault(), "%.1f%%", payload.getProgress()));
                castHolder.updateStatus(payload);
            }
        }
    }

    public void notifyDirChanged() {
        if (currentNode == null) return;
        currentNodes.clear();
        currentNodes.addAll(currentNode.dirs);
        currentNodes.addAll(currentNode.files);
        notifyDataSetChanged();

        if (handler != null) handler.onDirectoryChanged(currentNode);
    }

    public void navigateInto(TreeNode node) {
        if (node != null && !node.isFile() && currentNode.indexOfDir(node) != -1) cd(node);
    }

    @Override
    public int getItemViewType(int position) {
        if (currentNodes.get(position).isFile()) return ITEM_FILE;
        else return ITEM_DIR;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_FILE) return new FileViewHolder(parent);
        else return new DirViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FileViewHolder) {
            FileViewHolder castHolder = (FileViewHolder) holder;
            TreeNode file = currentNodes.get(position);

            castHolder.name.setText(file.name);
            castHolder.progressBar.setProgress((int) file.obj.getProgress());
            castHolder.percentage.setText(String.format(Locale.getDefault(), "%.1f%%", file.obj.getProgress()));
            castHolder.updateStatus(file.obj);
        } else if (holder instanceof DirViewHolder) {
            DirViewHolder castHolder = (DirViewHolder) holder;
            TreeNode dir = currentNodes.get(position);

            castHolder.name.setText(dir.name);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TreeNode node = currentNodes.get(holder.getAdapterPosition());
                if (node == null) return;
                if (node.isFile()) {
                    if (handler != null) handler.onFileSelected(node.obj);
                } else {
                    navigateInto(node);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return currentNodes.size();
    }

    public void rebaseTo(TreeNode node) {
        currentNode = node;
        notifyDirChanged();
    }

    public boolean canGoUp() {
        return currentNode != null && !currentNode.isRoot();
    }

    public interface IAdapter {
        void onFileSelected(AFile file);

        void onDirectoryChanged(TreeNode dir);
    }

    private class DirViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView name;

        DirViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.directory_item, parent, false));

            name = (SuperTextView) itemView.findViewById(R.id.directoryItem_name);
        }
    }

    private class FileViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView name;
        final ProgressBar progressBar;
        final SuperTextView percentage;
        final ImageView status;

        FileViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.file_item, parent, false));

            name = (SuperTextView) itemView.findViewById(R.id.fileItem_name);
            progressBar = (ProgressBar) itemView.findViewById(R.id.fileItem_progressBar);
            progressBar.setProgressTintList(ColorStateList.valueOf(color));
            percentage = (SuperTextView) itemView.findViewById(R.id.fileItem_percentage);
            status = (ImageView) itemView.findViewById(R.id.fileItem_status);
        }

        public void updateStatus(AFile file) {
            if (file.completed()) {
                status.setImageResource(R.drawable.ic_cloud_done_black_48dp);
            } else if (file.selected) {
                status.setImageResource(R.drawable.ic_cloud_download_black_48dp);
            } else {
                status.setImageResource(R.drawable.ic_cloud_off_black_48dp);
            }
        }
    }
}
