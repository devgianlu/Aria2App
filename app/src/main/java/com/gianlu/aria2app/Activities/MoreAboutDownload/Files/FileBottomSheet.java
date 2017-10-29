package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.gianlu.aria2app.NetIO.JTA2.AriaFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.BaseBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.SuperTextView;

import java.util.Collections;
import java.util.List;

public class FileBottomSheet extends BaseBottomSheet<AriaFile> {
    private final ISheet handler;
    private final Handler mainHandler;
    private Download download;
    private SuperTextView index;
    private SuperTextView path;
    private SuperTextView length;
    private SuperTextView completedLength;
    private Button downloadFile;
    private CheckBox selected;

    public FileBottomSheet(View parent, ISheet handler) {
        super(parent, R.layout.file_sheet, true);
        this.handler = handler;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setDownload(Download download) {
        this.download = download;
    }

    @Override
    public void bindViews() {
        index = content.findViewById(R.id.fileSheet_index);
        path = content.findViewById(R.id.fileSheet_path);
        length = content.findViewById(R.id.fileSheet_length);
        completedLength = content.findViewById(R.id.fileSheet_completedLength);
        selected = content.findViewById(R.id.fileSheet_selected);
        downloadFile = content.findViewById(R.id.fileSheet_downloadFile);
    }

    @Override
    protected void setupView(@NonNull final AriaFile item) {
        if (download == null) return;

        title.setText(item.getName());
        selected.setChecked(item.selected);

        if (download.supportsDeselectingFiles()) {
            selected.setEnabled(true);
            selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    JTA2 jta2;
                    try {
                        jta2 = JTA2.instantiate(buttonView.getContext());
                    } catch (JTA2InitializingException ex) {
                        if (handler != null) handler.onExceptionChangingSelection(ex);
                        return;
                    }

                    jta2.changeSelection(download, Collections.singletonList(item), isChecked, new JTA2.IChangeSelection() {
                        @Override
                        public void onChangedSelection(final boolean selected) {
                            if (handler == null) return;

                            item.selected = selected;

                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (selected) handler.onSelectedFile(item);
                                    else handler.onDeselectedFile(item);
                                }
                            });
                        }

                        @Override
                        public void cantDeselectAll() {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (handler != null) handler.onCantDeselectAll();
                                }
                            });
                        }

                        @Override
                        public void onException(final Exception ex) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (handler != null) handler.onExceptionChangingSelection(ex);
                                }
                            });
                        }
                    });
                }
            });
        } else {
            selected.setEnabled(false);
        }

        try {
            final MultiProfile profile = ProfilesManager.get(context).getCurrent(context);
            if (download.isMetadata() || !profile.getProfile(context).isDirectDownloadEnabled()) {
                downloadFile.setVisibility(View.GONE);
            } else {
                downloadFile.setVisibility(View.VISIBLE);
                downloadFile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (handler != null)
                                    handler.onWantsToDownload(profile, item);
                            }
                        });
                    }
                });
            }
        } catch (ProfilesManager.NoCurrentProfileException ex) {
            Logging.logMe(context, ex);
            downloadFile.setVisibility(View.GONE);
        }
    }

    @Override
    protected void updateView(@NonNull AriaFile item) {
        index.setHtml(R.string.index, item.index);
        path.setHtml(R.string.path, item.path);
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(item.length, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(item.completedLength, false));
    }

    public void update(List<AriaFile> files) {
        if (current == null || download == null) return;
        update(AriaFile.find(files, current));
    }

    public interface ISheet {
        void onSelectedFile(AriaFile file);

        void onDeselectedFile(AriaFile file);

        void onExceptionChangingSelection(Exception ex);

        void onCantDeselectAll();

        void onWantsToDownload(MultiProfile profile, @NonNull AriaFile file);
    }
}
