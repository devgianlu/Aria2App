package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.NiceBaseBottomSheet;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;

import java.util.List;
import java.util.Locale;

public class DirBottomSheet extends NiceBaseBottomSheet {
    private final ISheet listener;
    private SuperTextView length;
    private SuperTextView completedLength;
    private TextView percentage;
    private CheckBox selected;
    private AriaDirectory currentDir = null;

    public DirBottomSheet(ViewGroup parent, ISheet listener) {
        super(parent, R.layout.sheet_header_dir, R.layout.sheet_dir, true);
        this.listener = listener;
    }

    @Override
    protected boolean onPrepareAction(@NonNull FloatingActionButton fab, Object... payloads) {
        DownloadWithUpdate download = (DownloadWithUpdate) payloads[0];
        final AriaDirectory dir = (AriaDirectory) payloads[1];

        try {
            final MultiProfile profile = ProfilesManager.get(getContext()).getCurrent();
            if (download.update().isMetadata() || profile.getProfile(getContext()).directDownload == null) {
                return false;
            } else {
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) listener.onDownloadDirectory(profile, dir);
                    }
                });

                return true;
            }
        } catch (ProfilesManager.NoCurrentProfileException ex) {
            Logging.log(ex);
            return false;
        }
    }

    @Override
    protected void cleanUp() {
        currentDir = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onUpdateViews(Object... payloads) {
        if (currentDir != null) {
            currentDir = currentDir.update((DownloadWithUpdate) payloads[0], (List<AriaFile>) payloads[1]);
            updateContentViews(currentDir);
            updateHeaderViews(currentDir);
        }
    }

    private void updateHeaderViews(AriaDirectory dir) {
        percentage.setText(String.format(Locale.getDefault(), "%d%%", (int) dir.getProgress()));
    }

    @Override
    protected void onCreateHeaderView(@NonNull ViewGroup parent, Object... payloads) {
        percentage = parent.findViewById(R.id.dirSheet_percentage);
        percentage.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Medium.ttf"));
        TextView title = parent.findViewById(R.id.dirSheet_title);

        DownloadWithUpdate download = (DownloadWithUpdate) payloads[0];
        AriaDirectory dir = (AriaDirectory) payloads[1];

        int colorAccent = download.update().isTorrent() ? R.color.colorTorrent : R.color.colorAccent_light;
        parent.setBackgroundResource(colorAccent);

        title.setText(dir.name);
        updateHeaderViews(dir);
    }

    private void updateContentViews(AriaDirectory dir) {
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(dir.totalLength, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(dir.completedLength, false));
        selected.setChecked(dir.allSelected());
    }

    @Override
    protected void onCreateContentView(@NonNull ViewGroup parent, Object... payloads) {
        SuperTextView indexes = parent.findViewById(R.id.dirSheet_indexes);
        SuperTextView path = parent.findViewById(R.id.dirSheet_path);
        length = parent.findViewById(R.id.dirSheet_length);
        selected = parent.findViewById(R.id.dirSheet_selected);
        completedLength = parent.findViewById(R.id.dirSheet_completedLength);

        final DownloadWithUpdate download = (DownloadWithUpdate) payloads[0];
        final AriaDirectory dir = (AriaDirectory) payloads[1];

        indexes.setHtml(R.string.indexes, CommonUtils.join(dir.indexes, ", "));
        path.setHtml(R.string.path, dir.fullPath);
        updateContentViews(dir);

        if (download.update().canDeselectFiles()) {
            selected.setEnabled(true);
            selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    download.changeSelection(dir.allIndexes(), isChecked, new AbstractClient.OnResult<Download.ChangeSelectionResult>() {
                        @Override
                        public void onResult(@NonNull Download.ChangeSelectionResult result) {
                            switch (result) {
                                case EMPTY:
                                    listener.showToast(Utils.Messages.CANT_DESELECT_ALL_FILES);
                                    break;
                                case SELECTED:
                                    listener.showToast(Utils.Messages.DIR_SELECTED);
                                    break;
                                case DESELECTED:
                                    listener.showToast(Utils.Messages.DIR_DESELECTED);
                                    break;
                            }
                        }

                        @Override
                        public void onException(Exception ex, boolean shouldForce) {
                            Logging.log(ex);
                            listener.showToast(Utils.Messages.FAILED_CHANGE_FILE_SELECTION);
                        }
                    });
                }
            });
        } else {
            selected.setEnabled(false);
        }
    }

    public interface ISheet {
        void onDownloadDirectory(MultiProfile profile, AriaDirectory dir);

        void showToast(Toaster.Message message);
    }
}
