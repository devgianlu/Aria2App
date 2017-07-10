package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.gianlu.aria2app.NetIO.JTA2.ADir;
import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.BaseBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;

import java.util.List;

public class DirBottomSheet extends BaseBottomSheet<ADir> {
    private final Download download;
    private final ISheet handler;
    private SuperTextView indexes;
    private SuperTextView path;
    private SuperTextView length;
    private SuperTextView completedLength;
    private CheckBox selected;
    private Button downloadDir;

    public DirBottomSheet(View parent, Download download, ISheet handler) {
        super(parent, R.layout.dir_sheet, true);
        this.download = download;
        this.handler = handler;
    }

    @Override
    public void bindViews() {
        indexes = (SuperTextView) content.findViewById(R.id.dirSheet_indexes);
        path = (SuperTextView) content.findViewById(R.id.dirSheet_path);
        length = (SuperTextView) content.findViewById(R.id.dirSheet_length);
        completedLength = (SuperTextView) content.findViewById(R.id.dirSheet_completedLength);
        selected = (CheckBox) content.findViewById(R.id.dirSheet_selected);
        downloadDir = (Button) content.findViewById(R.id.dirSheet_downloadDir);
    }

    public void update(List<AFile> files) {
        if (current == null) return;
        update(current.update(download, files));
    }

    @Override
    protected void setupView(@NonNull final ADir item) {
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
                    } catch (JTA2InitializingException ex) {
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


        if (download.isMetadata() || !ProfilesManager.get(context).getCurrent(context).getProfile(context).isDirectDownloadEnabled()) {
            downloadDir.setVisibility(View.GONE);
        } else {
            downloadDir.setVisibility(View.VISIBLE);
            downloadDir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (handler != null) handler.onWantsToDownload(download, item);
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void updateView(@NonNull ADir item) {
        indexes.setHtml(R.string.indexes, CommonUtils.join(item.indexes, ", "));
        path.setHtml(R.string.path, item.fullPath);
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(item.totalLength, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(item.completedLength, false));
    }

    public interface ISheet {
        void onSelectedDir(ADir dir);

        void onDeselectedDir(ADir dir);

        void onCantDeselectAll();

        void onExceptionChangingSelection(Exception ex);

        void onWantsToDownload(Download download, ADir dir);
    }
}
