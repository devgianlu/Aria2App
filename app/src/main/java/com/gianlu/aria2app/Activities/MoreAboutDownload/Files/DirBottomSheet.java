package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.AriaDirectory;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.NiceBaseBottomSheet;
import com.gianlu.commonutils.SuperTextView;


public class DirBottomSheet extends NiceBaseBottomSheet {
    public DirBottomSheet(ViewGroup parent) {
        super(parent, R.layout.dir_sheet_header, R.layout.dir_sheet, true);
    }

    @Override
    protected void onCreateHeaderView(@NonNull ViewGroup parent, Object... payloads) {
        TextView title = parent.findViewById(R.id.dirSheet_title);

        Download download = (Download) payloads[0];
        AriaDirectory dir = (AriaDirectory) payloads[1];

        int colorAccent = download.isTorrent() ? R.color.colorTorrent : R.color.colorAccent;
        parent.setBackgroundResource(colorAccent);

        title.setText(dir.name);
    }

    @Override
    protected void onCreateContentView(@NonNull ViewGroup parent, Object... payloads) {
        SuperTextView indexes = parent.findViewById(R.id.dirSheet_indexes);
        SuperTextView path = parent.findViewById(R.id.dirSheet_path);
        SuperTextView length = parent.findViewById(R.id.dirSheet_length);
        SuperTextView completedLength = parent.findViewById(R.id.dirSheet_completedLength);

        Download download = (Download) payloads[0];
        AriaDirectory dir = (AriaDirectory) payloads[1];

        indexes.setHtml(R.string.indexes, CommonUtils.join(dir.indexes, ", "));
        path.setHtml(R.string.path, dir.fullPath);
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(dir.totalLength, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(dir.completedLength, false));
    }
}
