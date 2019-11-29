package com.gianlu.aria2app.activities.moreabout.files;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.aria2.AriaDirectory;
import com.gianlu.aria2app.api.aria2.AriaFiles;
import com.gianlu.aria2app.api.aria2.Download;
import com.gianlu.aria2app.api.aria2.DownloadWithUpdate;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.aria2app.profiles.ProfilesManager;
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

public class DirectorySheet extends ThemedModalBottomSheet<DirectorySheet.SetupPayload, DirectorySheet.UpdatePayload> {
    private SuperTextView length;
    private CheckBox selected;
    private SuperTextView completedLength;
    private TextView percentage;
    private AriaDirectory currentDir;

    @NonNull
    public static DirectorySheet get() {
        return new DirectorySheet();
    }

    @Override
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView parent, @NonNull SetupPayload payload) {
        inflater.inflate(R.layout.sheet_header_dir, parent, true);
        parent.setBackgroundColorRes(payload.download.update().getColor());
        parent.setTitle(payload.dir.name);

        percentage = parent.findViewById(R.id.dirSheet_percentage);
        FontsManager.set(FontsManager.ROBOTO_MEDIUM, percentage);
    }

    public void update(@NonNull DownloadWithUpdate download, @NonNull AriaFiles files) {
        update(new UpdatePayload(download, files));
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull SetupPayload payload) {
        inflater.inflate(R.layout.sheet_dir, parent, true);
        currentDir = payload.dir;

        final DownloadWithUpdate download = payload.download;

        SuperTextView indexes = parent.findViewById(R.id.dirSheet_indexes);
        indexes.setHtml(R.string.indexes, CommonUtils.join(currentDir.indexes, ", "));

        SuperTextView path = parent.findViewById(R.id.dirSheet_path);
        path.setHtml(R.string.path, download.update().dir + currentDir.path);

        length = parent.findViewById(R.id.dirSheet_length);
        selected = parent.findViewById(R.id.dirSheet_selected);
        completedLength = parent.findViewById(R.id.dirSheet_completedLength);

        update(currentDir);

        if (download.update().canDeselectFiles()) {
            selected.setEnabled(true);
            selected.setOnCheckedChangeListener((buttonView, isChecked) -> download.changeSelection(currentDir.indexes.toArray(new Integer[0]), isChecked, new AbstractClient.OnResult<Download.ChangeSelectionResult>() {
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
                            toaster.message(R.string.fileSelected);
                            break;
                        case DESELECTED:
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
                    if (!isAdded()) return;

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
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull final SetupPayload payload) {
        try {
            final MultiProfile profile = ProfilesManager.get(getContext()).getCurrent();
            if (payload.download.update().isMetadata() || profile.getProfile(getContext()).directDownload == null) {
                return false;
            } else {
                action.setImageResource(R.drawable.baseline_download_24);
                CommonUtils.setBackgroundColor(action, payload.download.update().getColor());
                action.setSupportImageTintList(ColorStateList.valueOf(Color.WHITE));
                action.setOnClickListener(v -> payload.listener.onDownloadDirectory(profile, currentDir));
                return true;
            }
        } catch (ProfilesManager.NoCurrentProfileException ex) {
            Logging.log(ex);
            return false;
        }
    }

    private void update(AriaDirectory dir) {
        percentage.setText(String.format(Locale.getDefault(), "%d%%", (int) dir.getProgress()));
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(dir.length(), false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(dir.completedLength(), false));
        selected.setChecked(dir.areAllFilesSelected());
    }

    @Override
    protected void onReceivedUpdate(@NonNull UpdatePayload payload) {
        AriaDirectory dir = AriaDirectory.createRoot(payload.download, payload.files);
        AriaDirectory currentDirUpdated = dir.findDirectory(currentDir.path);

        if (currentDirUpdated == null) currentDir = dir;
        else currentDir = currentDirUpdated;
        update(currentDir);
    }

    public void show(FragmentActivity activity, DownloadWithUpdate download, AriaDirectory dir, Listener listener) {
        show(activity, new SetupPayload(download, dir, listener));
    }

    @Override
    protected int getCustomTheme(@NonNull SetupPayload payload) {
        return payload.download.update().getThemeResource();
    }

    public interface Listener {
        void onDownloadDirectory(@NonNull MultiProfile profile, @NonNull AriaDirectory dir);
    }

    protected static class SetupPayload {
        private final DownloadWithUpdate download;
        private final AriaDirectory dir;
        private final DirectorySheet.Listener listener;

        SetupPayload(@NonNull DownloadWithUpdate download, @NonNull AriaDirectory dir, @NonNull DirectorySheet.Listener listener) {
            this.download = download;
            this.dir = dir;
            this.listener = listener;
        }
    }

    protected static class UpdatePayload {
        private final DownloadWithUpdate download;
        private final AriaFiles files;

        UpdatePayload(@NonNull DownloadWithUpdate download, @NonNull AriaFiles files) {
            this.download = download;
            this.files = files;
        }
    }
}
