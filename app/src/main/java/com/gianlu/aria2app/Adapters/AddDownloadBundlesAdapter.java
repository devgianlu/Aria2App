package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.Activities.AddDownload.AddDownloadBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddMetalinkBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddTorrentBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddUriBundle;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class AddDownloadBundlesAdapter extends RecyclerView.Adapter<AddDownloadBundlesAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final Listener listener;
    private final List<AddDownloadBundle> bundles;

    public AddDownloadBundlesAdapter(Context context, Listener listener) {
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
        final AddDownloadBundle bundle = bundles.get(position);

        String text = null;
        int textColorRes = 0;
        if (bundle instanceof AddUriBundle) {
            text = CommonUtils.join(((AddUriBundle) bundle).uris, ", ");
            textColorRes = R.color.colorSecondaryVariant;
        } else if (bundle instanceof AddTorrentBundle) {
            text = ((AddTorrentBundle) bundle).filename;
            textColorRes = R.color.colorTorrent_pressed;
        } else if (bundle instanceof AddMetalinkBundle) {
            text = ((AddMetalinkBundle) bundle).filename;
            textColorRes = R.color.colorMetalink_pressed;
        }

        if (text != null)
            holder.text.setText(text);
        if (textColorRes != 0)
            CommonUtils.setTextColor(holder.text, textColorRes);

        holder.remove.setOnClickListener(v -> removeItem(holder.getAdapterPosition()));
        holder.edit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(holder.getAdapterPosition(), bundle);
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

    public void addItems(List<? extends AddDownloadBundle> newBundles) {
        bundles.addAll(newBundles);
        notifyItemRangeInserted(bundles.size() - newBundles.size(), newBundles.size());

        if (listener != null) listener.onItemCountUpdated(getItemCount());
    }

    private void removeItem(int pos) {
        if (pos == -1) return;

        bundles.remove(pos);
        notifyItemRemoved(pos);

        if (listener != null) listener.onItemCountUpdated(getItemCount());
    }

    public void itemChanged(int pos, @NonNull AddDownloadBundle bundle) {
        if (pos == -1) return;

        bundles.set(pos, bundle);
        notifyItemChanged(pos);
    }

    public interface Listener {
        void onItemCountUpdated(int count);

        void onEdit(int pos, @NonNull AddDownloadBundle bundle);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView text;
        final ImageButton remove;
        final ImageButton edit;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_add_download_bundle, parent, false));

            text = itemView.findViewById(R.id.addDownloadBundleItem_text);
            remove = itemView.findViewById(R.id.addDownloadBundleItem_remove);
            edit = itemView.findViewById(R.id.addDownloadBundleItem_edit);
        }
    }
}
