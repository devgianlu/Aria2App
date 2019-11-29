package com.gianlu.aria2app.Tutorial;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.tutorial.BaseTutorial;

import me.toptas.fancyshowcase.FocusShape;

public final class PeersServersTutorial extends BaseTutorial {
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

            add(forView(holder.itemView, R.string.tutorial_serverDetails)
                    .enableAutoTextPosition()
                    .roundRectRadius(8)
                    .focusShape(FocusShape.ROUNDED_RECTANGLE));
            return true;
        }

        return false;
    }

    public final boolean buildForPeers(@NonNull RecyclerView list) {
        RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(0);
        if (holder != null) {
            list.scrollToPosition(0);

            add(forView(holder.itemView, R.string.tutorial_peerDetails)
                    .enableAutoTextPosition()
                    .roundRectRadius(8)
                    .focusShape(FocusShape.ROUNDED_RECTANGLE));
            return true;
        }

        return false;
    }
}
