package com.gianlu.aria2app.Tutorial;

import com.gianlu.aria2app.Adapters.DownloadCardsAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Tutorial.BaseTutorial;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

public class DownloadsToolbarTutorial extends BaseTutorial {

    @Keep
    public DownloadsToolbarTutorial() {
        super(Discovery.DOWNLOADS_TOOLBAR);
    }

    public final boolean canShow(Toolbar toolbar, DownloadCardsAdapter adapter) {
        return toolbar != null && adapter != null && adapter.getItemCount() >= 5;
    }

    public final void buildSequence(@NonNull Toolbar toolbar) {
        if (toolbar.findViewById(R.id.main_search) != null)
            forToolbarMenuItem(toolbar, R.id.main_search, R.string.search, R.string.search_desc);

        if (toolbar.findViewById(R.id.main_filter) != null)
            forToolbarMenuItem(toolbar, R.id.main_filter, R.string.filters, R.string.filters_desc);
    }
}
