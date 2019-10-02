package com.gianlu.aria2app.Tutorial;

import android.graphics.Rect;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.tutorial.BaseTutorial;

public class PeersServersTutorial extends BaseTutorial {
    @Keep
    public PeersServersTutorial() {
        super(Discovery.PEERS_SERVERS);
    }

    public final boolean canShow(Fragment fragment, RecyclerView.Adapter adapter) {
        return fragment != null && CommonUtils.isVisible(fragment) && adapter != null && adapter.getItemCount() >= 1;
    }

    public final boolean buildForServers(@NonNull RecyclerView list) {
        RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(0);
        if (holder != null) {
            list.scrollToPosition(0);

            Rect rect = new Rect();
            holder.itemView.getGlobalVisibleRect(rect);
            rect.offset((int) -(holder.itemView.getWidth() * 0.3), 0);

            forBounds(rect, R.string.serverDetails, R.string.serverDetails_desc)
                    .tintTarget(false)
                    .transparentTarget(true);

            return true;
        }

        return false;
    }

    public final boolean buildForPeers(@NonNull RecyclerView list) {
        RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(0);
        if (holder != null) {
            list.scrollToPosition(0);

            Rect rect = new Rect();
            holder.itemView.getGlobalVisibleRect(rect);
            rect.offset((int) -(holder.itemView.getWidth() * 0.3), 0);

            forBounds(rect, R.string.peerDetails, R.string.peerDetails_desc)
                    .tintTarget(false)
                    .transparentTarget(true);

            return true;
        }

        return false;
    }
}
