package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gianlu.aria2app.DonutProgress;
import com.gianlu.aria2app.NetIO.JTA2.AriaFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.NiceBaseBottomSheet;
import com.gianlu.commonutils.SuperTextView;

public class FileBottomSheet extends NiceBaseBottomSheet {

    public FileBottomSheet(ViewGroup parent) {
        super(parent, R.layout.file_sheet_header, R.layout.file_sheet, true);
    }

    @Override
    protected void onCreateHeaderView(@NonNull ViewGroup parent, Object... payloads) {
        DonutProgress donut = parent.findViewById(R.id.fileSheet_donut);
        TextView title = parent.findViewById(R.id.fileSheet_title);

        Download download = (Download) payloads[0];
        AriaFile file = (AriaFile) payloads[1];

        int colorAccent = download.isTorrent() ? R.color.colorTorrent : R.color.colorAccent;
        parent.setBackgroundResource(colorAccent);

        title.setText(file.getName());
        donut.setProgress(file.getProgress());
        donut.setTextColorRes(R.color.white);
        donut.setFinishedStrokeColorRes(R.color.white);
    }

    @Override
    protected void onCreateContentView(@NonNull ViewGroup parent, Object... payloads) {
        SuperTextView index = parent.findViewById(R.id.fileSheet_index);
        SuperTextView path = parent.findViewById(R.id.fileSheet_path);
        SuperTextView length = parent.findViewById(R.id.fileSheet_length);
        SuperTextView completedLength = parent.findViewById(R.id.fileSheet_completedLength);
        CheckBox selected = parent.findViewById(R.id.fileSheet_selected);

        Download download = (Download) payloads[0];
        AriaFile file = (AriaFile) payloads[1];

        selected.setChecked(file.selected);
        index.setHtml(R.string.index, file.index);
        path.setHtml(R.string.path, file.path);
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(file.length, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(file.completedLength, false));
    }
}
