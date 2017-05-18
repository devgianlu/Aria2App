package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.support.annotation.NonNull;
import android.view.View;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.BaseBottomSheet;

// TODO: FileBottomSheet
public class FileBottomSheet extends BaseBottomSheet<AFile> {
    public FileBottomSheet(View parent) {
        super(parent, R.layout.file_sheet);
    }

    @Override
    protected int getRippleDark() {
        return R.drawable.ripple_effect_dark;
    }

    @Override
    protected void setupView(@NonNull AFile item) {

    }

    @Override
    protected void updateView(@NonNull AFile item) {

    }
}
