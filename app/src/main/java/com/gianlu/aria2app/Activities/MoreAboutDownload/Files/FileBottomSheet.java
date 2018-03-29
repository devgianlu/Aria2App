package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.gianlu.aria2app.FileTypeTextView;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithHelper;
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

public class FileBottomSheet extends NiceBaseBottomSheet {
    private final ISheet listener;
    private int currentFileIndex = -1;
    private SuperTextView completedLength;
    private SuperTextView length;
    private CheckBox selected;
    private TextView percentage;

    public FileBottomSheet(ViewGroup parent, ISheet listener) {
        super(parent, R.layout.sheet_header_file, R.layout.sheet_file, true);
        this.listener = listener;
    }

    @Override
    protected void cleanUp() {
        currentFileIndex = -1;
    }

    @Override
    protected boolean onPrepareAction(@NonNull FloatingActionButton fab, Object... payloads) {
        Download download = ((DownloadWithHelper) payloads[0]).get();
        final AriaFile file = (AriaFile) payloads[1];

        try {
            final MultiProfile profile = ProfilesManager.get(getContext()).getCurrent();
            if (download.isMetadata() || profile.getProfile(getContext()).directDownload == null) {
                return false;
            } else {
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) listener.onDownloadFile(profile, file);
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
    @SuppressWarnings("unchecked")
    protected void onUpdateViews(Object... payloads) {
        AriaFile file = findCurrent((List<AriaFile>) payloads[0]);
        if (file != null) {
            updateHeaderViews(file);
            updateContentViews(file);
        }
    }

    private void updateHeaderViews(AriaFile file) {
        percentage.setText(String.format(Locale.getDefault(), "%d%%", (int) file.getProgress()));
    }

    private void updateContentViews(AriaFile file) {
        selected.setChecked(file.selected);
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(file.length, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(file.completedLength, false));
    }

    @Nullable
    private AriaFile findCurrent(List<AriaFile> files) {
        for (AriaFile file : files)
            if (file.index == currentFileIndex)
                return file;

        return null;
    }

    @Override
    protected void onCreateHeaderView(@NonNull ViewGroup parent, Object... payloads) {
        FileTypeTextView fileType = parent.findViewById(R.id.fileSheet_fileType);
        fileType.setWidth(48);
        percentage = parent.findViewById(R.id.fileSheet_percentage);
        percentage.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Medium.ttf"));
        TextView title = parent.findViewById(R.id.fileSheet_title);

        Download download = ((DownloadWithHelper) payloads[0]).get();
        AriaFile file = (AriaFile) payloads[1];
        currentFileIndex = file.index;

        int colorAccent = download.isTorrent() ? R.color.colorTorrent : R.color.colorAccent_light;
        parent.setBackgroundResource(colorAccent);

        fileType.setFilename(file.getName());
        title.setText(file.getName());
        updateHeaderViews(file);
    }

    @Override
    protected void onCreateContentView(@NonNull ViewGroup parent, Object... payloads) {
        SuperTextView index = parent.findViewById(R.id.fileSheet_index);
        SuperTextView path = parent.findViewById(R.id.fileSheet_path);
        length = parent.findViewById(R.id.fileSheet_length);
        completedLength = parent.findViewById(R.id.fileSheet_completedLength);
        selected = parent.findViewById(R.id.fileSheet_selected);

        final DownloadWithHelper download = (DownloadWithHelper) payloads[0];
        final AriaFile file = (AriaFile) payloads[1];

        index.setHtml(R.string.index, file.index);
        path.setHtml(R.string.path, file.path);
        updateContentViews(file);

        if (download.get().canDeselectFiles()) {
            selected.setEnabled(true);
            selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    download.changeSelection(new Integer[]{file.index}, isChecked, new AbstractClient.OnResult<DownloadWithHelper.ChangeSelectionResult>() {
                        @Override
                        public void onResult(DownloadWithHelper.ChangeSelectionResult result) {
                            switch (result) {
                                case EMPTY:
                                    listener.showToast(Utils.Messages.CANT_DESELECT_ALL_FILES);
                                    break;
                                case SELECTED:
                                    file.selected = true;
                                    listener.showToast(Utils.Messages.FILE_SELECTED);
                                    break;
                                case DESELECTED:
                                    file.selected = false;
                                    listener.showToast(Utils.Messages.FILE_DESELECTED);
                                    break;
                            }
                        }

                        @Override
                        public void onException(Exception ex) {
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
        void onDownloadFile(MultiProfile profile, AriaFile file);

        void showToast(Toaster.Message message);
    }
}
