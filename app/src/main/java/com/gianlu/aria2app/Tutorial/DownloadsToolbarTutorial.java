package com.gianlu.aria2app.Tutorial;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.aria2app.Adapters.DownloadCardsAdapter;
import com.gianlu.aria2app.R;

public class DownloadsToolbarTutorial extends BaseTutorial {

    @Keep
    public DownloadsToolbarTutorial() {
        super(TutorialManager.Discovery.DOWNLOADS_TOOLBAR);
    }

    public final boolean canShow(Toolbar toolbar, DownloadCardsAdapter adapter) {
        return toolbar != null && adapter != null && adapter.getItemCount() >= 5;
    }

    public final void buildSequence(@NonNull Context context, @NonNull TapTargetSequence seq, @NonNull Toolbar toolbar) {
        if (toolbar.findViewById(R.id.main_search) != null)
            seq.target(TapTarget.forToolbarMenuItem(toolbar, R.id.main_search, context.getString(R.string.search), context.getString(R.string.search_desc)));

        if (toolbar.findViewById(R.id.main_filter) != null)
            seq.target(TapTarget.forToolbarMenuItem(toolbar, R.id.main_filter, context.getString(R.string.filters), context.getString(R.string.filters_desc)));
    }
}
