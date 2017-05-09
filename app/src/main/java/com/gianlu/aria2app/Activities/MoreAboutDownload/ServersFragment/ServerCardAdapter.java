package com.gianlu.aria2app.Activities.MoreAboutDownload.ServersFragment;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ServerCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private final CardView noDataCardView;
    private final List<Item> items = new ArrayList<>();

    ServerCardAdapter(Context context, Map<Integer, List<Server>> objs, CardView noDataCardView) {
        this.context = context;
        this.noDataCardView = noDataCardView;

        for (Integer index : objs.keySet()) {
            HeaderItem header = new HeaderItem(index);
            items.add(header);

            for (Server server : objs.get(index)) {
                server.membershipIndex = index;
                items.add(server);
            }
        }
    }

    void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == Item.HEADER) {
            TextView title = new TextView(context);
            title.setPadding(0, 15, 0, 5);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextSize(14);
            return new HeaderViewHolder(title);
        } else {
            return new ServerCardViewHolder(LayoutInflater.from(context).inflate(R.layout.server_cardview, parent, false));
        }
    }

    void onUpdate(Map<Integer, List<Server>> servers) {
        if (items == null || servers == null) return;

        for (Integer index : servers.keySet()) {
            for (Server newServer : servers.get(index)) {
                for (Item item : items) {
                    if (item.getItemType() == Item.SERVER
                            && ((Server) item).membershipIndex == index
                            && newServer.currentUri.equals(((Server) item).currentUri)) {
                        notifyItemChanged(items.indexOf(item), newServer);
                    }
                }
            }
        }
    }

    void onDisplayNoData(String message) {
        noDataCardView.setVisibility(View.VISIBLE);
        ((TextView) noDataCardView.findViewById(R.id.serversFragment_noDataLabel)).setText(context.getString(R.string.noServersMessage, message));
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getItemType();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder cHolder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(cHolder, position);
            return;
        }

        if (payloads.get(0) instanceof Server && cHolder instanceof ServerCardViewHolder) {
            Server server = (Server) payloads.get(0);
            ServerCardViewHolder holder = (ServerCardViewHolder) cHolder;

            holder.currentUri.setText(server.currentUri);
            holder.uri.setText(server.uri);
            holder.downloadSpeed.setText(CommonUtils.speedFormatter(server.downloadSpeed));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder cHolder, int position) {
        if (items.isEmpty()) noDataCardView.setVisibility(View.VISIBLE);
        else noDataCardView.setVisibility(View.GONE);

        if (getItemViewType(position) == Item.HEADER) {
            HeaderItem header = (HeaderItem) getItem(position);
            final HeaderViewHolder holder = (HeaderViewHolder) cHolder;

            holder.title.setText(header.getTitle());
        } else {
            Server server = (Server) getItem(position);
            final ServerCardViewHolder holder = (ServerCardViewHolder) cHolder;

            holder.currentUri.setText(server.currentUri);
            holder.uri.setText(server.uri);
            holder.downloadSpeed.setText(CommonUtils.speedFormatter(server.downloadSpeed));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private Item getItem(int position) {
        return items.get(position);
    }
}
