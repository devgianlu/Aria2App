package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.BaseBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;

import java.util.List;

public class FileBottomSheet extends BaseBottomSheet<AFile> {
    private final SuperTextView index;
    private final SuperTextView path;
    private final SuperTextView length;
    private final SuperTextView completedLength;
    private final CheckBox selected;
    private final Download download;
    private final ISheet handler;

    public FileBottomSheet(View parent, Download download, ISheet handler) {
        super(parent, R.layout.file_sheet);
        this.download = download;
        this.handler = handler;

        index = (SuperTextView) content.findViewById(R.id.fileSheet_index);
        path = (SuperTextView) content.findViewById(R.id.fileSheet_path);
        length = (SuperTextView) content.findViewById(R.id.fileSheet_length);
        completedLength = (SuperTextView) content.findViewById(R.id.fileSheet_completedLength);
        selected = (CheckBox) content.findViewById(R.id.fileSheet_selected);
    }

    @Override
    protected int getRippleDark() {
        return R.drawable.ripple_effect_dark;
    }

    @Override
    protected void setupView(@NonNull final AFile item) {
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
                        if (handler != null) handler.onExceptionSelectingFile(ex);
                        return;
                    }

                    jta2.changeSelection(download, item, isChecked, new JTA2.IChangeSelection() {
                        @Override
                        public void onChangedSelection(final boolean selected) {
                            if (handler == null) return;

                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (selected) handler.onSelectedFile(item);
                                    else handler.onDeselectedFile(item);
                                }
                            });
                        }

                        @Override
                        public void onException(final Exception ex) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (handler != null) handler.onExceptionSelectingFile(ex);
                                }
                            });
                        }
                    });
                }
            });
        } else {
            selected.setEnabled(false);
        }
    }

    @Override
    protected void updateView(@NonNull AFile item) {
        index.setHtml(R.string.index, item.index);
        path.setHtml(R.string.path, item.path);
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(item.length, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(item.completedLength, false));
    }

    public void update(List<AFile> files) {
        if (current == null) return;
        update(AFile.find(files, current));
    }

    public interface ISheet {
        void onSelectedFile(AFile file);

        void onDeselectedFile(AFile file);

        void onExceptionSelectingFile(Exception ex);
    }
}
