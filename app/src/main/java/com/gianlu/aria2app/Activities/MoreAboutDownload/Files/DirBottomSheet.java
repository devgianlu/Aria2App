package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.gianlu.aria2app.NetIO.JTA2.AriaDirectory;
import com.gianlu.aria2app.NetIO.JTA2.AriaFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.BaseBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.SuperTextView;

import java.util.List;

public class DirBottomSheet extends BaseBottomSheet<AriaDirectory> {
    private final ISheet handler;
    private Download download;
    private SuperTextView indexes;
    private SuperTextView path;
    private SuperTextView length;
    private SuperTextView completedLength;
    private CheckBox selected;
    private Button downloadDir;

    public DirBottomSheet(View parent, ISheet handler) {
        super(parent, R.layout.dir_sheet, true);
        this.handler = handler;
    }

    @Override
    public void bindViews() {
        indexes = content.findViewById(R.id.dirSheet_indexes);
        path = content.findViewById(R.id.dirSheet_path);
        length = content.findViewById(R.id.dirSheet_length);
        completedLength = content.findViewById(R.id.dirSheet_completedLength);
        selected = content.findViewById(R.id.dirSheet_selected);
        downloadDir = content.findViewById(R.id.dirSheet_downloadDir);
    }

    public void update(List<AriaFile> files) {
        if (current == null || download == null) return;
        update(current.update(download, files));
    }

    @Override
    protected void setupView(@NonNull final AriaDirectory item) {
        if (download == null) return;

        title.setText(item.name);
        selected.setChecked(item.allSelected());

        if (download.supportsDeselectingFiles()) {
            selected.setEnabled(true);
            selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    JTA2 jta2;
                    try {
                        jta2 = JTA2.instantiate(buttonView.getContext());
                    } catch (JTA2.InitializingException ex) {
                        if (handler != null) handler.onExceptionChangingSelection(ex);
                        return;
                    }

                    jta2.changeSelection(download, item.allObjs(), isChecked, new JTA2.IChangeSelection() {
                        @Override
                        public void onChangedSelection(final boolean selected) {
                            if (handler == null) return;

                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (selected) handler.onSelectedDir(item);
                                    else handler.onDeselectedDir(item);
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
                downloadDir.setVisibility(View.GONE);
            } else {
                downloadDir.setVisibility(View.VISIBLE);
                downloadDir.setOnClickListener(new View.OnClickListener() {
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
            Logging.logMe(ex);
            downloadDir.setVisibility(View.GONE);
        }
    }

    @Override
    protected void updateView(@NonNull AriaDirectory item) {
        indexes.setHtml(R.string.indexes, CommonUtils.join(item.indexes, ", "));
        path.setHtml(R.string.path, item.fullPath);
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(item.totalLength, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(item.completedLength, false));
    }

    public void setDownload(Download download) {
        this.download = download;
    }

    public interface ISheet {
        void onSelectedDir(AriaDirectory dir);

        void onDeselectedDir(AriaDirectory dir);

        void onCantDeselectAll();

        void onExceptionChangingSelection(Exception ex);

        void onWantsToDownload(MultiProfile profile, @NonNull AriaDirectory dir);
    }
}
