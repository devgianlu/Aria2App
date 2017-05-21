package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.aria2app.R;

import java.util.ArrayList;
import java.util.List;

public class UrisAdapter extends RecyclerView.Adapter<UrisAdapter.ViewHolder> {
    private final List<String> uris;
    private final LayoutInflater inflater;
    private final IAdapter handler;

    public UrisAdapter(Context context, IAdapter handler) {
        this.handler = handler;
        inflater = LayoutInflater.from(context);
        uris = new ArrayList<>();

        if (handler != null) handler.onUrisCountChanged(uris.size());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final String uri = uris.get(position);
        holder.uri.setText(uri);
        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onEditUri(holder.getAdapterPosition(), uri);
            }
        });
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeUri(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return uris.size();
    }

    public List<String> getUris() {
        return uris;
    }

    public void addUri(String uri) {
        if (uri == null || uri.isEmpty()) return;

        uris.add(uri);
        notifyItemInserted(uris.size() - 1);

        if (handler != null) handler.onUrisCountChanged(uris.size());
    }

    public void removeUri(int pos) {
        uris.remove(pos);
        notifyItemRemoved(pos);

        if (handler != null) handler.onUrisCountChanged(uris.size());
    }

    public boolean canAddUri() {
        for (String uri : uris)
            if (uri.startsWith("magnet:"))
                return false;

        return true;
    }

    public interface IAdapter {
        void onUrisCountChanged(int count);

        void onEditUri(int oldPos, String uri);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView uri;
        final ImageButton edit;
        final ImageButton remove;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.uri_item, parent, false));

            uri = (TextView) itemView.findViewById(R.id.uriItem_uri);
            edit = (ImageButton) itemView.findViewById(R.id.uriItem_edit);
            remove = (ImageButton) itemView.findViewById(R.id.uriItem_remove);
        }
    }
}
