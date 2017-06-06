package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.support.annotation.NonNull;
import android.view.View;

import com.gianlu.aria2app.NetIO.JTA2.ADir;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.BaseBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;

public class DirBottomSheet extends BaseBottomSheet<ADir> {
    private SuperTextView indexes;
    private SuperTextView path;
    private SuperTextView length;
    private SuperTextView completedLength;

    public DirBottomSheet(View parent) {
        super(parent, R.layout.dir_sheet, true);
    }

    @Override
    protected int getRippleDark() {
        return R.drawable.ripple_effect_dark;
    }

    @Override
    public void bindViews() {
        indexes = (SuperTextView) content.findViewById(R.id.dirSheet_indexes);
        path = (SuperTextView) content.findViewById(R.id.dirSheet_path);
        length = (SuperTextView) content.findViewById(R.id.dirSheet_length);
        completedLength = (SuperTextView) content.findViewById(R.id.dirSheet_completedLength);
    }

    @Override
    protected void setupView(@NonNull ADir item) {
        title.setText(item.name);
    }

    @Override
    protected void updateView(@NonNull ADir item) {
        indexes.setHtml(R.string.indexes, CommonUtils.join(item.indexes, ", "));
        path.setHtml(R.string.path, item.fullPath);
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(item.totalLength, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(item.completedLength, false));
    }
}
