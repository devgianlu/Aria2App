package com.gianlu.aria2app.Tutorial;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.Adapters.DownloadCardsAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.tutorial.BaseTutorial;

import me.toptas.fancyshowcase.FocusShape;

public final class DownloadCardsTutorial extends BaseTutorial {

    @Keep
    public DownloadCardsTutorial() {
        super(Discovery.DOWNLOADS_CARDS);
    }

    public boolean canShow(DownloadCardsAdapter adapter) {
        return adapter != null && adapter.getItemCount() >= 1;
    }

    public boolean buildSequence(@NonNull RecyclerView list) {
        LinearLayoutManager llm = (LinearLayoutManager) list.getLayoutManager();
        if (llm == null) return false;

        int pos = llm.findFirstCompletelyVisibleItemPosition();
        if (pos == -1) pos = 0;

        DownloadCardsAdapter.ViewHolder holder = (DownloadCardsAdapter.ViewHolder) list.findViewHolderForLayoutPosition(pos);
        if (holder != null) {
            list.scrollToPosition(pos);

            add(forView(holder.donutProgress, R.string.tutorial_moreDetails)
                    .fitSystemWindows(true)
                    .focusShape(FocusShape.CIRCLE)
                    .enableAutoTextPosition());
            add(forView(holder.more, R.string.tutorial_evenMoreDetails)
                    .fitSystemWindows(true)
                    .focusShape(FocusShape.ROUNDED_RECTANGLE)
                    .roundRectRadius(8)
                    .enableAutoTextPosition());
            return true;
        }

        return false;
    }
}
