package com.gianlu.aria2app.DirectDownload;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.R;

import java.util.List;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.ViewHolder> {
    private final Context context;
    private final List<DDDownload> objs;

    public DownloadsAdapter(Context context, List<DDDownload> objs) {
        this.context = context;
        this.objs = objs;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.direct_download_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DDDownload item = getItem(position);

        holder.name.setText(item.name);
        holder.id.setText(String.valueOf(item.id));
    }

    public DDDownload getItem(int pos) {
        return objs.get(pos);
    }

    @Override
    public int getItemCount() {
        return objs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public TextView id;

        public ViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.directDownloadItem_name);
            id = (TextView) itemView.findViewById(R.id.directDownloadItem_id);
        }
    }
}
