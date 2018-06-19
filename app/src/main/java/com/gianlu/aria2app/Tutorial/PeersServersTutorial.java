package com.gianlu.aria2app.Tutorial;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Tutorial.BaseTutorial;

public class PeersServersTutorial extends BaseTutorial {
    @Keep
    public PeersServersTutorial() {
        super(Discovery.PEERS_SERVERS);
    }

    public final boolean canShow(Fragment fragment, RecyclerView.Adapter adapter) {
        return fragment != null && CommonUtils.isVisible(fragment) && adapter != null && adapter.getItemCount() >= 1;
    }

    public final boolean buildForServers(@NonNull Context context, @NonNull TapTargetSequence seq, @NonNull RecyclerView list) {
        RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(0);
        if (holder != null) {
            list.scrollToPosition(0);

            Rect rect = new Rect();
            holder.itemView.getGlobalVisibleRect(rect);
            rect.offset((int) -(holder.itemView.getWidth() * 0.3), 0);

            seq.target(TapTarget.forBounds(rect, context.getString(R.string.serverDetails), context.getString(R.string.serverDetails_desc))
                    .tintTarget(false)
                    .transparentTarget(true));

            return true;
        }

        return false;
    }

    public final boolean buildForPeers(@NonNull Context context, @NonNull TapTargetSequence seq, @NonNull RecyclerView list) {
        RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(0);
        if (holder != null) {
            list.scrollToPosition(0);

            Rect rect = new Rect();
            holder.itemView.getGlobalVisibleRect(rect);
            rect.offset((int) -(holder.itemView.getWidth() * 0.3), 0);

            seq.target(TapTarget.forBounds(rect, context.getString(R.string.peerDetails), context.getString(R.string.peerDetails_desc))
                    .tintTarget(false)
                    .transparentTarget(true));

            return true;
        }

        return false;
    }
}
