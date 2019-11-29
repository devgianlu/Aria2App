package com.gianlu.aria2app.Tutorial;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.gianlu.aria2app.Adapters.DownloadCardsAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.tutorial.BaseTutorial;

import me.toptas.fancyshowcase.FocusShape;

public final class DownloadsToolbarTutorial extends BaseTutorial {

    @Keep
    public DownloadsToolbarTutorial() {
        super(Discovery.DOWNLOADS_TOOLBAR);
    }

    public final boolean canShow(Toolbar toolbar, DownloadCardsAdapter adapter) {
        return toolbar != null && adapter != null && adapter.getItemCount() >= 5;
    }

    public final void buildSequence(@NonNull Toolbar toolbar) {
        if (toolbar.findViewById(R.id.main_search) != null)
            add(forToolbarMenuItem(toolbar, R.id.main_search, R.string.tutorial_search)
                    .fitSystemWindows(true)
                    .enableAutoTextPosition()
                    .focusShape(FocusShape.CIRCLE));

        if (toolbar.findViewById(R.id.main_filter) != null)
            add(forToolbarMenuItem(toolbar, R.id.main_filter, R.string.tutorial_filters)
                    .fitSystemWindows(true)
                    .enableAutoTextPosition()
                    .focusShape(FocusShape.CIRCLE));
    }
}
