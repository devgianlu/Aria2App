package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.gianlu.aria2app.FileTypeTextView;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.AriaFiles;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.BottomSheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.FontsManager;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;

import java.util.Locale;

public class FileSheet extends ThemedModalBottomSheet<FileSheet.SetupPayload, AriaFiles> {
    private TextView percentage;
    private int fileIndex = -1;
    private SuperTextView length;
    private SuperTextView completedLength;
    private CheckBox selected;

    @NonNull
    public static FileSheet get() {
        return new FileSheet();
    }

    @Override
    protected int getCustomTheme(@NonNull FileSheet.SetupPayload payload) {
        return payload.download.update().getThemeResource();
    }

    @Override
    protected boolean onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull SetupPayload payload) {
        inflater.inflate(R.layout.sheet_header_file, parent, true);
        fileIndex = payload.file.index;

        parent.setBackgroundResource(payload.download.update().getBackgroundColor());

        FileTypeTextView fileType = parent.findViewById(R.id.fileSheet_fileType);
        fileType.setFilename(payload.file.getName());

        percentage = parent.findViewById(R.id.fileSheet_percentage);
        percentage.setTypeface(FontsManager.get().get(inflater.getContext(), FontsManager.ROBOTO_MEDIUM));

        TextView title = parent.findViewById(R.id.fileSheet_title);
        title.setText(payload.file.getName());

        return true;
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull SetupPayload payload) {
        inflater.inflate(R.layout.sheet_file, parent, true);

        final DownloadWithUpdate download = payload.download;
        final AriaFile file = payload.file;

        SuperTextView index = parent.findViewById(R.id.fileSheet_index);
        index.setHtml(R.string.index, file.index);

        SuperTextView path = parent.findViewById(R.id.fileSheet_path);
        path.setHtml(R.string.path, file.path);

        length = parent.findViewById(R.id.fileSheet_length);
        completedLength = parent.findViewById(R.id.fileSheet_completedLength);
        selected = parent.findViewById(R.id.fileSheet_selected);

        update(file);

        if (download.update().canDeselectFiles()) {
            selected.setEnabled(true);
            selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    download.changeSelection(new Integer[]{file.index}, isChecked, new AbstractClient.OnResult<Download.ChangeSelectionResult>() {
                        @Override
                        public void onResult(@NonNull Download.ChangeSelectionResult result) {
                            Toaster toaster = Toaster.build();
                            toaster.extra(result);
                            switch (result) {
                                case EMPTY:
                                    toaster.message(R.string.cannotDeselectAllFiles);
                                    break;
                                case SELECTED:
                                    file.selected = true;
                                    toaster.message(R.string.fileSelected);
                                    break;
                                case DESELECTED:
                                    file.selected = false;
                                    toaster.message(R.string.fileDeselected);
                                    break;
                                default:
                                    toaster.message(R.string.failedAction);
                                    break;
                            }

                            dismissAllowingStateLoss();
                            DialogUtils.showToast(getContext(), toaster);
                        }

                        @Override
                        public void onException(Exception ex, boolean shouldForce) {
                            dismissAllowingStateLoss();
                            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedFileChangeSelection).ex(ex));
                        }
                    });
                }
            });
        } else {
            selected.setEnabled(false);
        }

        isLoading(false);
    }

    @Override
    protected void onCustomizeToolbar(@NonNull Toolbar toolbar, @NonNull SetupPayload payload) {
        toolbar.setBackgroundResource(payload.download.update().getBackgroundColor());
        toolbar.setTitle(payload.file.getName());
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull final SetupPayload payload) {
        try {
            final MultiProfile profile = ProfilesManager.get(getContext()).getCurrent();
            if (payload.download.update().isMetadata() || profile.getProfile(getContext()).directDownload == null) {
                return false;
            } else {
                action.setImageResource(R.drawable.baseline_download_24);
                CommonUtils.setBackgroundColor(action, payload.download.update().getColorAccent());
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        payload.listener.onDownloadFile(profile, payload.file);
                    }
                });

                return true;
            }
        } catch (ProfilesManager.NoCurrentProfileException ex) {
            Logging.log(ex);
            return false;
        }
    }

    private void update(@NonNull AriaFile file) {
        percentage.setText(String.format(Locale.getDefault(), "%d%%", (int) file.getProgress()));
        selected.setChecked(file.selected);
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(file.length, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(file.completedLength, false));
    }

    @Override
    protected void onRequestedUpdate(@NonNull AriaFiles payload) {
        AriaFile file = payload.findFileByIndex(fileIndex);
        if (file != null) update(file);
    }

    public void show(@Nullable FragmentActivity activity, @NonNull DownloadWithUpdate download, @NonNull AriaFile file, @NonNull Listener listener) {
        super.show(activity, new SetupPayload(download, file, listener));
    }

    public interface Listener {
        void onDownloadFile(@NonNull MultiProfile profile, @NonNull AriaFile file);
    }

    protected static class SetupPayload {
        private final DownloadWithUpdate download;
        private final AriaFile file;
        private final Listener listener;

        SetupPayload(@NonNull DownloadWithUpdate download, @NonNull AriaFile file, @NonNull Listener listener) {
            this.download = download;
            this.file = file;
            this.listener = listener;
        }
    }
}
