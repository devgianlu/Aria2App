package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.gianlu.aria2app.FileTypeTextView;
import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.AriaFiles;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int ITEM_DIR = 0;
    private static final int ITEM_FILE = 1;
    private final LayoutInflater inflater;
    private final Listener listener;
    private final Set<AriaFile> selectedFiles = new HashSet<>();
    private AriaDirectory currentDir;
    private boolean isInActionMode = false;

    public FilesAdapter(Context context, @NonNull Listener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void enteredActionMode(@NonNull AriaFile trigger) {
        isInActionMode = true;
        selectedFiles.clear();
        selectedFiles.add(trigger);
        notifyDataSetChanged();
    }

    public void selectAllInDirectory() {
        if (!isInActionMode || currentDir == null) return;
        selectedFiles.addAll(currentDir.files);
        notifyDataSetChanged();
    }

    @UiThread
    public void update(@NonNull DownloadWithUpdate download, @NonNull AriaFiles files) {
        AriaDirectory dir = AriaDirectory.createRoot(download, files);

        if (currentDir == null) {
            changeDir(dir);
        } else {
            AriaDirectory currentDirUpdated = dir.findDirectory(currentDir.path);
            if (currentDirUpdated == null) changeDir(dir);
            else updateDir(currentDirUpdated);
        }
    }

    public void navigateUp() {
        if (currentDir != null && !currentDir.isRoot()) changeDir(currentDir.parent);
    }

    @UiThread
    private void updateDir(@NonNull AriaDirectory dir) {
        currentDir = dir;
        int dirs = currentDir.dirs.size();
        int total = getItemCount();
        for (int i = dirs; i < total; i++)
            notifyItemChanged(i, new WeakReference<>(currentDir.files.get(i - dirs)));
    }

    @UiThread
    public void changeDir(@NonNull AriaDirectory dir) {
        currentDir = dir;
        listener.onDirectoryChanged(dir);
        notifyDataSetChanged();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            if (holder instanceof FileViewHolder) {
                FileViewHolder castHolder = (FileViewHolder) holder;
                WeakReference<AriaFile> ref = (WeakReference<AriaFile>) payloads.get(0);
                AriaFile file = ref.get();
                if (file != null) {
                    castHolder.progressBar.setProgress((int) file.getProgress());
                    castHolder.percentage.setText(String.format(Locale.getDefault(), "%.1f%%", file.getProgress()));
                    castHolder.updateStatus(file);
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= currentDir.dirs.size()) return ITEM_FILE;
        else return ITEM_DIR;
    }

    @Override
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_FILE) return new FileViewHolder(parent);
        else return new DirViewHolder(parent);
    }

    public boolean canGoUp() {
        return currentDir != null && !currentDir.isRoot();
    }

    public void exitedActionMode() {
        isInActionMode = false;
        selectedFiles.clear();
        notifyDataSetChanged();
    }

    @NonNull
    public Set<AriaFile> getSelectedFiles() {
        return selectedFiles;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FileViewHolder) {
            FileViewHolder castHolder = (FileViewHolder) holder;
            final AriaFile file = currentDir.files.get(position - currentDir.dirs.size());

            castHolder.name.setText(file.getName());
            castHolder.fileType.setFilename(file.getName());
            castHolder.progressBar.setProgress((int) file.getProgress());
            castHolder.percentage.setText(String.format(Locale.getDefault(), "%.1f%%", file.getProgress()));
            castHolder.updateStatus(file);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onFileSelected(file);
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return listener.onFileLongClick(file);
                }
            });

            if (isInActionMode) {
                castHolder.select.setVisibility(View.VISIBLE);
                castHolder.select.setChecked(selectedFiles.contains(file));
                castHolder.select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) selectedFiles.add(file);
                        else selectedFiles.remove(file);

                        if (selectedFiles.isEmpty()) {
                            listener.exitActionMode();
                            exitedActionMode();
                        }
                    }
                });
            } else {
                castHolder.select.setVisibility(View.GONE);
                castHolder.select.setOnCheckedChangeListener(null);
            }
        } else if (holder instanceof DirViewHolder) {
            DirViewHolder castHolder = (DirViewHolder) holder;
            final AriaDirectory dir = currentDir.dirs.get(position);
            castHolder.name.setText(dir.name);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeDir(dir);
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return listener.onDirectoryLongClick(dir);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (currentDir == null) return 0;
        else return currentDir.dirs.size() + currentDir.files.size();
    }

    @Nullable
    public AriaDirectory getCurrentDir() {
        return currentDir;
    }

    public interface Listener {
        void onFileSelected(@NonNull AriaFile file);

        void onDirectoryChanged(@NonNull AriaDirectory dir);

        boolean onFileLongClick(@NonNull AriaFile file);

        boolean onDirectoryLongClick(@NonNull AriaDirectory dir);

        void exitActionMode();
    }

    private class DirViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView name;

        DirViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_directory, parent, false));

            name = itemView.findViewById(R.id.directoryItem_name);
        }
    }

    private class FileViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView name;
        final ProgressBar progressBar;
        final SuperTextView percentage;
        final ImageView status;
        final FileTypeTextView fileType;
        final CheckBox select;

        FileViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_file, parent, false));

            name = itemView.findViewById(R.id.fileItem_name);
            select = itemView.findViewById(R.id.fileItem_select);
            progressBar = itemView.findViewById(R.id.fileItem_progressBar);
            percentage = itemView.findViewById(R.id.fileItem_percentage);
            status = itemView.findViewById(R.id.fileItem_status);
            fileType = itemView.findViewById(R.id.fileItem_fileType);
        }

        void updateStatus(@NonNull AriaFile file) {
            if (file.completed())
                status.setImageResource(R.drawable.ic_cloud_done_black_48dp);
            else if (file.selected)
                status.setImageResource(R.drawable.ic_cloud_download_black_48dp);
            else
                status.setImageResource(R.drawable.ic_cloud_off_black_48dp);
        }
    }
}
