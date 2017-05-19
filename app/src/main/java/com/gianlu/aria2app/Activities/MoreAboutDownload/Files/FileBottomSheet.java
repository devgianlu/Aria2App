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

    public FileBottomSheet(View parent, Download download) {
        super(parent, R.layout.file_sheet);
        this.download = download;

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
        selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                JTA2 jta2;
                try {
                    jta2 = JTA2.instantiate(buttonView.getContext());
                } catch (JTA2InitializingException e) {
                    e.printStackTrace();
                    return;
                }

                jta2.changeSelection(download, item, isChecked, new JTA2.IChangeSelection() {
                    @Override
                    public void onChangedSelection(boolean selected) {
                        System.out.println("DONE!!");
                    }

                    @Override
                    public void onException(Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });
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
}
