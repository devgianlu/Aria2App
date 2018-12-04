package com.gianlu.aria2app.Tutorial;

import android.graphics.Rect;

import com.gianlu.aria2app.Adapters.DownloadCardsAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Tutorial.BaseTutorial;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DownloadCardsTutorial extends BaseTutorial {

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

            if (CommonUtils.isExpanded(holder.details))
                CommonUtils.collapse(holder.details, null);

            Rect rect = new Rect();
            holder.itemView.getGlobalVisibleRect(rect);
            rect.offset((int) (-holder.itemView.getWidth() * 0.2), (int) (-holder.itemView.getHeight() * 0.2));

            forBounds(rect, R.string.moreDetails, R.string.moreDetails_desc)
                    .tintTarget(false)
                    .transparentTarget(true);
            forView(holder.more, R.string.evenMoreDetails, R.string.evenMoreDetails_desc);

            return true;
        }

        return false;
    }
}
