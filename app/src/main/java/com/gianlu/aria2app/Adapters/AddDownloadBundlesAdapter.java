package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.aria2app.Activities.AddDownload.AddDownloadBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddMetalinkBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddTorrentBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddUriBundle;
import com.gianlu.aria2app.R;

import java.util.ArrayList;
import java.util.List;

public class AddDownloadBundlesAdapter extends RecyclerView.Adapter<AddDownloadBundlesAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final Listener listener;
    private final List<AddDownloadBundle> bundles;
    private final Context context;

    public AddDownloadBundlesAdapter(Context context, Listener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        this.bundles = new ArrayList<>();

        if (listener != null) listener.onItemCountUpdated(0);
    }

    @NonNull
    public List<AddDownloadBundle> getBundles() {
        return bundles;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        AddDownloadBundle bundle = bundles.get(position);

        holder.text.setText(bundle.toString()); // TODO: This doesn't mean a lot to the user
        if (bundle instanceof AddUriBundle) {
            holder.text.setTextColor(ContextCompat.getColor(context, R.color.colorURI_pressed));
        } else if (bundle instanceof AddTorrentBundle) {
            holder.text.setTextColor(ContextCompat.getColor(context, R.color.colorTorrent_pressed));
        } else if (bundle instanceof AddMetalinkBundle) {
            holder.text.setTextColor(ContextCompat.getColor(context, R.color.colorMetalink_pressed));
        }

        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeItem(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return bundles.size();
    }

    public void addItem(@NonNull AddDownloadBundle bundle) {
        bundles.add(bundle);
        notifyItemInserted(bundles.size() - 1);

        if (listener != null) listener.onItemCountUpdated(getItemCount());
    }

    private void removeItem(int pos) {
        bundles.remove(pos);
        notifyItemRemoved(pos);

        if (listener != null) listener.onItemCountUpdated(getItemCount());
    }

    public interface Listener {
        void onItemCountUpdated(int count);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView text;
        final ImageButton remove;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_add_download_bundle, parent, false));

            text = itemView.findViewById(R.id.addDownloadBundleItem_text);
            remove = itemView.findViewById(R.id.addDownloadBundleItem_remove);
        }
    }
}
