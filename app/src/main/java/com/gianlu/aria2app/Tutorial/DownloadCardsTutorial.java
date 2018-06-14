package com.gianlu.aria2app.Tutorial;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.aria2app.Adapters.DownloadCardsAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

public class DownloadCardsTutorial extends BaseTutorial {

    @Keep
    public DownloadCardsTutorial() {
        super(TutorialManager.Discovery.DOWNLOADS_CARDS);
    }

    public boolean canShow(DownloadCardsAdapter adapter) {
        return adapter != null && adapter.getItemCount() >= 1;
    }

    public boolean buildSequence(@NonNull Context context, @NonNull TapTargetSequence seq, @NonNull RecyclerView list) {
        LinearLayoutManager llm = (LinearLayoutManager) list.getLayoutManager();
        int pos = llm.findFirstCompletelyVisibleItemPosition();
        if (pos == -1) pos = 0;

        DownloadCardsAdapter.ViewHolder holder = (DownloadCardsAdapter.ViewHolder) list.findViewHolderForLayoutPosition(pos);
        if (holder != null) {
            list.scrollToPosition(pos);

            if (CommonUtils.isExpanded(holder.details))
                CommonUtils.collapse(holder.details, null);

            Rect rect = new Rect();
            holder.itemView.getGlobalVisibleRect(rect);
            rect.offset((int) (holder.itemView.getWidth() * 0.3), (int) (-holder.itemView.getHeight() * 0.2));

            seq.targets(TapTarget.forBounds(rect, context.getString(R.string.moreDetails), context.getString(R.string.moreDetails_desc)).tintTarget(false),
                    TapTarget.forView(holder.more, context.getString(R.string.evenMoreDetails), context.getString(R.string.evenMoreDetails_desc)));

            return true;
        }

        return false;
    }
}
