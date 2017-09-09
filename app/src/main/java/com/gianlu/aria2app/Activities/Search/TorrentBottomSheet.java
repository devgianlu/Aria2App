package com.gianlu.aria2app.Activities.Search;

import android.support.annotation.NonNull;
import android.view.View;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.BaseBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;

public class TorrentBottomSheet extends BaseBottomSheet<Torrent> {
    private SuperTextView size;
    private SuperTextView seeders;
    private SuperTextView leeches;

    public TorrentBottomSheet(View parent) {
        super(parent, R.layout.torrent_sheet, false);
    }

    @Override
    public void bindViews() {
        size = content.findViewById(R.id.torrentSheet_size);
        seeders = content.findViewById(R.id.torrentSheet_seeders);
        leeches = content.findViewById(R.id.torrentSheet_leeches);
    }

    @Override
    protected void setupView(@NonNull Torrent item) {
    }

    @Override
    protected void updateView(@NonNull Torrent item) {
        title.setText(item.title);

        size.setHtml(R.string.size, CommonUtils.dimensionFormatter(item.size, false));
        seeders.setHtml(R.string.numSeeder, item.seeders);
        leeches.setHtml(R.string.numLeeches, item.leeches);
    }
}
