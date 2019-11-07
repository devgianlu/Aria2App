package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;


import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.gianlu.aria2app.FileTypeTextView;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.AriaFiles;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.bottomsheet.ModalBottomSheetHeaderView;
import com.gianlu.commonutils.bottomsheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.typography.FontsManager;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView parent, @NonNull SetupPayload payload) {
        inflater.inflate(R.layout.sheet_header_file, parent, true);
        fileIndex = payload.file.index;

        parent.setBackgroundColorRes(payload.download.update().getColor());
        parent.setTitle(payload.file.getName());

        FileTypeTextView fileType = parent.findViewById(R.id.fileSheet_fileType);
        fileType.setFilename(payload.file.getName());

        percentage = parent.findViewById(R.id.fileSheet_percentage);
        FontsManager.set(FontsManager.ROBOTO_MEDIUM, percentage);
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
            selected.setOnCheckedChangeListener((buttonView, isChecked) -> download.changeSelection(new Integer[]{file.index}, isChecked, new AbstractClient.OnResult<Download.ChangeSelectionResult>() {
                @Override
                public void onResult(@NonNull Download.ChangeSelectionResult result) {
                    if (!isAdded()) return;

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
                public void onException(@NonNull Exception ex) {
                    if (!isAdded() || getContext() == null) return;

                    dismissAllowingStateLoss();
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedFileChangeSelection).ex(ex));
                }
            }));
        } else {
            selected.setEnabled(false);
        }

        isLoading(false);
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull SetupPayload payload) {
        try {
            MultiProfile profile = ProfilesManager.get(requireContext()).getCurrent();
            if (payload.download.update().isMetadata() || profile.getProfile(getContext()).directDownload == null) {
                return false;
            } else {
                action.setImageResource(R.drawable.baseline_download_24);
                action.setSupportImageTintList(ColorStateList.valueOf(Color.WHITE));
                CommonUtils.setBackgroundColor(action, payload.download.update().getColor());
                action.setOnClickListener(v -> payload.listener.onDownloadFile(profile, payload.file, false));
                action.setOnLongClickListener(v -> {
                    payload.listener.onDownloadFile(profile, payload.file, true);
                    return true;
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
    protected void onReceivedUpdate(@NonNull AriaFiles payload) {
        AriaFile file = payload.findFileByIndex(fileIndex);
        if (file != null) update(file);
    }

    public void show(@Nullable FragmentActivity activity, @NonNull DownloadWithUpdate download, @NonNull AriaFile file, @NonNull Listener listener) {
        super.show(activity, new SetupPayload(download, file, listener));
    }

    public interface Listener {
        void onDownloadFile(@NonNull MultiProfile profile, @NonNull AriaFile file, boolean share);
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
