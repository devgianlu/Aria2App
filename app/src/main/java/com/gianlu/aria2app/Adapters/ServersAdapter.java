package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.NetIO.FreeGeoIP.FreeGeoIPApi;
import com.gianlu.aria2app.NetIO.FreeGeoIP.IPDetails;
import com.gianlu.aria2app.NetIO.JTA2.AriaFile;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.SuperTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int ITEM_SERVER = 0;
    private static final int ITEM_HEADER = 1;
    private final List<Object> objs;
    private final LayoutInflater inflater;
    private final IAdapter listener;
    private final Context context;
    private final FreeGeoIPApi freeGeoIPApi;
    private final CountryFlags flags = CountryFlags.get();

    public ServersAdapter(Context context, IAdapter listener) {
        this.context = context;
        this.listener = listener;
        this.objs = new ArrayList<>();
        this.freeGeoIPApi = FreeGeoIPApi.get();
        this.inflater = LayoutInflater.from(context);
        if (listener != null) listener.onItemCountUpdated(objs.size());
    }

    private void createObjs(SparseArray<List<Server>> servers, List<AriaFile> files) {
        objs.clear();

        for (AriaFile file : files) {
            List<Server> fileServers = servers.get(file.index, new ArrayList<Server>());
            if (!fileServers.isEmpty()) {
                objs.add(file);
                objs.addAll(fileServers);
            }
        }

        if (listener != null) listener.onItemCountUpdated(objs.size());
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_SERVER) return new ItemViewHolder(parent);
        else return new HeaderViewHolder(parent);
    }

    @Override
    public int getItemViewType(int position) {
        if (objs.get(position) instanceof Server) return ITEM_SERVER;
        else return ITEM_HEADER;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            final Server server = (Server) objs.get(position);
            ItemViewHolder castHolder = (ItemViewHolder) holder;

            castHolder.flag.setImageResource(R.drawable.ic_list_country_unknown);
            castHolder.address.setText(server.currentUri);
            castHolder.downloadSpeed.setText(CommonUtils.speedFormatter(server.downloadSpeed, false));

            castHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onServerSelected(server);
                }
            });

            freeGeoIPApi.getIPDetails(server.uri.getHost(), new FreeGeoIPApi.IIPDetails() {
                @Override
                public void onDetails(IPDetails details) {
                    notifyItemChanged(holder.getAdapterPosition(), details);
                }

                @Override
                public void onException(Exception ex) {
                    Logging.log(ex);
                }
            });
        } else if (holder instanceof HeaderViewHolder) {
            AriaFile file = (AriaFile) objs.get(position);
            HeaderViewHolder castHolder = (HeaderViewHolder) holder;

            castHolder.name.setText(file.getName());
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            if (holder instanceof ItemViewHolder) {
                ItemViewHolder castHolder = (ItemViewHolder) holder;
                Object payload = payloads.get(0);
                if (payload instanceof Server) {
                    final Server server = (Server) payloads.get(0);
                    castHolder.address.setText(server.currentUri);
                    castHolder.downloadSpeed.setText(CommonUtils.speedFormatter(server.downloadSpeed, false));
                } else if (payload instanceof IPDetails) {
                    castHolder.flag.setImageDrawable(flags.loadFlag(context, ((IPDetails) payload).countryCode));
                }
            } else if (holder instanceof HeaderViewHolder) {
                AriaFile file = (AriaFile) payloads.get(0);
                HeaderViewHolder castHolder = (HeaderViewHolder) holder;

                castHolder.name.setText(file.getName());
            }
        }
    }

    @Override
    public int getItemCount() {
        return objs.size();
    }

    private int indexOfHeader(int index) {
        for (int i = 0; i < objs.size(); i++)
            if (objs.get(i) instanceof AriaFile)
                if (((AriaFile) objs.get(i)).index == index)
                    return i;

        return -1;
    }

    private int indexOfServer(Uri uri) {
        for (int i = 0; i < objs.size(); i++)
            if (objs.get(i) instanceof Server)
                if (Objects.equals(((Server) objs.get(i)).uri, uri))
                    return i;

        return -1;
    }

    private void notifyServerChanged(Server server) {
        int pos = indexOfServer(server.uri);
        if (pos != -1) notifyItemChanged(pos, server);
    }

    public void notifyItemsChanged(SparseArray<List<Server>> servers, List<AriaFile> files) {
        createObjs(servers, files);
        for (AriaFile file : files) notifyHeaderChanged(file, servers.get(file.index));
    }

    private void notifyHeaderChanged(AriaFile file, List<Server> servers) {
        int pos = indexOfHeader(file.index);
        if (pos != -1) {
            notifyItemChanged(pos, file);
            for (Server server : servers) notifyServerChanged(server);
        }
    }

    public interface IAdapter {
        void onServerSelected(Server server);

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
