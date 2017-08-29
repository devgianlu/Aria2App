package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
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

    public ServersAdapter(Context context, IAdapter listener) {
        this.listener = listener;
        this.objs = new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
        if (listener != null) listener.onItemCountUpdated(objs.size());
    }

    private void createObjs(SparseArray<List<Server>> servers, List<AFile> files) {
        objs.clear();

        for (AFile file : files) {
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
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            final Server server = (Server) objs.get(position);
            ItemViewHolder castHolder = (ItemViewHolder) holder;

            castHolder.address.setText(server.currentUri);
            castHolder.downloadSpeed.setText(CommonUtils.speedFormatter(server.downloadSpeed, false));

            castHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onServerSelected(server);
                }
            });
        } else if (holder instanceof HeaderViewHolder) {
            AFile file = (AFile) objs.get(position);
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
                final Server server = (Server) payloads.get(0);
                ItemViewHolder castHolder = (ItemViewHolder) holder;

                castHolder.address.setText(server.currentUri);
                castHolder.downloadSpeed.setText(CommonUtils.speedFormatter(server.downloadSpeed, false));
            } else if (holder instanceof HeaderViewHolder) {
                AFile file = (AFile) payloads.get(0);
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
            if (objs.get(i) instanceof AFile)
                if (((AFile) objs.get(i)).index == index)
                    return i;

        return -1;
    }

    private int indexOfServer(String uri) {
        for (int i = 0; i < objs.size(); i++)
            if (objs.get(i) instanceof Server)
                if (Objects.equals(((Server) objs.get(i)).uri, uri) || Objects.equals(((Server) objs.get(i)).currentUri, uri))
                    return i;

        return -1;
    }

    public void notifyServerChanged(Server server) {
        int pos = indexOfServer(server.uri);
        if (pos != -1) notifyItemChanged(pos, server);
    }

    public void notifyItemsChanged(SparseArray<List<Server>> servers, List<AFile> files) {
        createObjs(servers, files);
        for (AFile file : files) notifyHeaderChanged(file, servers.get(file.index));
    }

    private void notifyHeaderChanged(AFile file, List<Server> servers) {
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
            super(inflater.inflate(R.layout.file_header_item, parent, false));

            name = itemView.findViewById(R.id.fileHeaderItem_name);
        }
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView address;
        final SuperTextView downloadSpeed;

        ItemViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.server_item, parent, false));

            address = itemView.findViewById(R.id.serverItem_address);
            downloadSpeed = itemView.findViewById(R.id.serverItem_downloadSpeed);
        }
    }
}
