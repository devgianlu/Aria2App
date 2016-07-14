package com.gianlu.aria2app.FileListing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.gianlu.jtitan.Aria2Helper.File;
import com.unnamed.b.atv.model.TreeNode;

public class CustomItemHolder extends TreeNode.BaseNodeViewHolder<CustomTreeItem> {
    private ImageView toggle;

    public CustomItemHolder(Context context) {
        super(context);
    }

    @SuppressLint("InflateParams")
    @Override
    public View createNodeView(TreeNode node, CustomTreeItem value) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.filelisting_custom_node, null, false);
        TextView text = (TextView) view.findViewById(R.id.fileListing_customNode_text);
        ImageView fileTypeImageView = (ImageView) view.findViewById(R.id.fileListing_customNode_fileType);
        toggle = (ImageView) view.findViewById(R.id.fileListing_customNode_toggle);
        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.fileListing_customNode_progressBar);
        TextView percentage = (TextView) view.findViewById(R.id.fileListing_customNode_percentage);
        ImageView selected = (ImageView) view.findViewById(R.id.fileListing_customNode_selected);

        if (value.type == CustomTreeItem.TYPE.FILE) {
            File file = (File) value.file;

            // File/folder name
            text.setText(file.getName());

            // Selected
            selected.setImageDrawable(file.selected ? context.getDrawable(R.drawable.ic_cloud_download_black_48dp) : context.getDrawable(R.drawable.ic_cloud_off_black_48dp));
            if (file.getProgress() == 100)
                selected.setImageDrawable(context.getDrawable(R.drawable.ic_cloud_done_black_48dp));

            // Expandable
            if (node.getChildren().size() == 0) toggle.setVisibility(View.INVISIBLE);

            // File
            progressBar.setMax(100);
            progressBar.setProgress(Math.round(file.getProgress()));
            percentage.setText(file.getPercentage());
            fileTypeImageView.setImageDrawable(context.getDrawable(R.drawable.ic_insert_drive_file_black_48dp));
        } else {
            Directory directory = (Directory) value.file;

            // File/folder name
            text.setText(directory.name);

            // Expandable
            if (node.getChildren().size() == 0) toggle.setVisibility(View.INVISIBLE);

            // Directory
            progressBar.setMax(100);
            progressBar.setProgress(Math.round(directory.getProgress()));
            percentage.setText(directory.getPercentage());
            selected.setVisibility(View.INVISIBLE);
            fileTypeImageView.setImageDrawable(context.getDrawable(R.drawable.ic_folder_black_48dp));
        }

        return view;
    }

    @Override
    public void toggle(boolean active) {
        toggle.setImageDrawable(active ? context.getDrawable(R.drawable.ic_keyboard_arrow_down_black_48dp) : context.getDrawable(R.drawable.ic_keyboard_arrow_right_black_48dp));
    }
}
