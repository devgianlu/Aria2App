package com.gianlu.aria2app.activities.moreabout.servers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.api.aria2.AriaFile;
import com.gianlu.aria2app.api.aria2.AriaFiles;
import com.gianlu.aria2app.api.aria2.Server;
import com.gianlu.aria2app.api.aria2.Servers;
import com.gianlu.aria2app.api.aria2.SparseServers;
import com.gianlu.aria2app.api.geolocalization.GeoIP;
import com.gianlu.aria2app.api.geolocalization.IPDetails;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.SuperTextView;

import java.util.ArrayList;
import java.util.List;

@UiThread
public class ServersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int ITEM_SERVER = 0;
    private static final int ITEM_HEADER = 1;
    private final List<Object> objs;
    private final LayoutInflater inflater;
    private final Listener listener;
    private final GeoIP geoIP;
    private final CountryFlags flags = CountryFlags.get();

    ServersAdapter(Context context, @NonNull Listener listener) {
        this.listener = listener;
        this.objs = new ArrayList<>();
        this.geoIP = GeoIP.get();
        this.inflater = LayoutInflater.from(context);
        listener.onItemCountUpdated(objs.size());
    }

    @Override
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_SERVER) return new ItemViewHolder(parent);
        else return new HeaderViewHolder(parent);
    }

    @Override
    public int getItemViewType(int position) {
        if (objs.get(position) instanceof Server) return ITEM_SERVER;
        else return ITEM_HEADER;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            final Server server = (Server) objs.get(position);
            ItemViewHolder castHolder = (ItemViewHolder) holder;

            castHolder.flag.setImageResource(R.drawable.ic_list_unknown);
            castHolder.address.setText(server.currentUri);
            castHolder.downloadSpeed.setText(CommonUtils.speedFormatter(server.downloadSpeed, false));

            castHolder.itemView.setOnClickListener(v -> listener.onServerSelected(server));

            String host = server.uri.getHost();
            if (host != null) {
                geoIP.getIPDetails(host, null, new GeoIP.OnIpDetails() {
                    @Override
                    public void onDetails(@NonNull IPDetails details) {
                        notifyItemChanged(holder.getAdapterPosition(), details);
                    }

                    @Override
                    public void onException(@NonNull Exception ex) {
                    }
                });
            }
        } else if (holder instanceof HeaderViewHolder) {
            AriaFile file = (AriaFile) objs.get(position);
            HeaderViewHolder castHolder = (HeaderViewHolder) holder;

            castHolder.name.setText(file.getName());
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            if (holder instanceof ItemViewHolder) {
                ItemViewHolder castHolder = (ItemViewHolder) holder;
                Object payload = payloads.get(0);
                if (payload instanceof Server) {
                    Server server = (Server) payloads.get(0);
                    castHolder.address.setText(server.currentUri);
                    castHolder.downloadSpeed.setText(CommonUtils.speedFormatter(server.downloadSpeed, false));
                } else if (payload instanceof IPDetails) {
                    castHolder.flag.setImageDrawable(flags.loadFlag(castHolder.flag.getContext(), ((IPDetails) payload).countryCode));
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return objs.size();
    }

    private int nextHeaderIndex(int start) {
        for (int i = start + 1; i < objs.size(); i++)
            if (objs.get(i) instanceof AriaFile)
                return i;

        return -1;
    }

    private void updateHeaderIfNeeded(@NonNull AriaFile file, @NonNull Servers servers) {
        int index = objs.indexOf(file);
        if (index == -1) {
            objs.add(file);
            objs.addAll(servers);
            notifyItemRangeInserted(objs.size() - 1 - servers.size(), 1 + servers.size());
            return;
        }

        int serversToUpdate;
        int nextHeader = nextHeaderIndex(index);
        if (nextHeader == -1) serversToUpdate = objs.size() - index - 1;
        else serversToUpdate = nextHeader - index;

        List<Server> toUpdate = new ArrayList<>(servers);

        loopObjs:
        for (int i = index + 1 + serversToUpdate - 1; i >= index + 1; i--) {
            for (Server server : servers) {
                if (server.equals(objs.get(i))) {
                    objs.set(i, server);
                    notifyItemChanged(i, server);
                    toUpdate.remove(server);
                    continue loopObjs;
                }
            }

            objs.remove(i);
            notifyItemRemoved(i);
        }

        if (!toUpdate.isEmpty()) {
            objs.addAll(index, toUpdate);
            notifyItemRangeInserted(index, toUpdate.size());
        }
    }

    public void notifyItemsChanged(@NonNull SparseServers servers, @NonNull AriaFiles files) {
        for (AriaFile file : files) updateHeaderIfNeeded(file, servers.get(file.index));
    }

    public interface Listener {
        void onServerSelected(@NonNull Server server);

        void onItemCountUpdated(int count);
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView name;

        HeaderViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_file_header, parent, false));

            name = itemView.findViewById(R.id.fileHeaderItem_name);
        }
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView address;
        final SuperTextView downloadSpeed;
        final ImageView flag;

        ItemViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_server, parent, false));

            address = itemView.findViewById(R.id.serverItem_address);
            downloadSpeed = itemView.findViewById(R.id.serverItem_downloadSpeed);
            flag = itemView.findViewById(R.id.serverItem_flag);
        }
    }
}
