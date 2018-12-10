package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.aria2app.R;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class UrisAdapter extends RecyclerView.Adapter<UrisAdapter.ViewHolder> {
    private final ArrayList<String> uris;
    private final LayoutInflater inflater;
    private final Listener listener;

    public UrisAdapter(Context context, Listener listener) {
        this.listener = listener;
        inflater = LayoutInflater.from(context);
        uris = new ArrayList<>();

        if (listener != null) listener.onUrisCountChanged(uris.size());
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final String uri = uris.get(position);
        holder.uri.setText(uri);
        holder.edit.setOnClickListener(v -> {
            if (listener != null) listener.onEditUri(holder.getAdapterPosition(), uri);
        });
        holder.remove.setOnClickListener(v -> removeUri(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return uris.size();
    }

    public ArrayList<String> getUris() {
        return uris;
    }

    public void addUri(String uri) {
        if (uri == null || uri.isEmpty()) return;

        uris.add(uri);
        notifyItemInserted(uris.size() - 1);

        if (listener != null) listener.onUrisCountChanged(uris.size());
    }

    public void addUris(Collection<String> newUris) {
        if (newUris.isEmpty()) return;

        uris.addAll(newUris);
        notifyItemRangeInserted(uris.size() - newUris.size(), newUris.size());

        if (listener != null) listener.onUrisCountChanged(uris.size());
    }

    public void removeUri(int pos) {
        uris.remove(pos);
        notifyItemRemoved(pos);

        if (listener != null) listener.onUrisCountChanged(uris.size());
    }

    public boolean canAddUri() {
        for (String uri : uris)
            if (uri.startsWith("magnet:"))
                return false;

        return true;
    }

    public interface Listener {
        void onUrisCountChanged(int count);

        void onEditUri(int oldPos, @NonNull String uri);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView uri;
        final ImageButton edit;
        final ImageButton remove;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_uri, parent, false));

            uri = itemView.findViewById(R.id.uriItem_uri);
            edit = itemView.findViewById(R.id.uriItem_edit);
            remove = itemView.findViewById(R.id.uriItem_remove);
        }
    }
}
