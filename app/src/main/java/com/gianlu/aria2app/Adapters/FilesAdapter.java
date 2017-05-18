package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.TreeNode;
import com.gianlu.aria2app.NetIO.JTA2.AFile;

import java.util.ArrayList;
import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {
    private final List<AFile> currDirFiles = new ArrayList<>();
    private final LayoutInflater inflater;
    private TreeNode rootNode;

    public FilesAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void update(List<AFile> files, String commonRoot) {
        if (rootNode == null) {
            rootNode = TreeNode.create(files, commonRoot);
            return;
        }

        // TODO: Update only current dir
    }

    public void navigateUp() {
        // TODO
    }

    public void navigatoInto(String name) {
        // TODO
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return currDirFiles.size();
    }

    public interface IAdapter {
        void onFileSelected(AFile file);

        void onDirectoryChanged(String dir);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
