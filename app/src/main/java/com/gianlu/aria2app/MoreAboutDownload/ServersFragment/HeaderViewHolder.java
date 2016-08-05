package com.gianlu.aria2app.MoreAboutDownload.ServersFragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class HeaderViewHolder extends RecyclerView.ViewHolder {
    public TextView title;

    public HeaderViewHolder(View itemView) {
        super(itemView);

        title = (TextView) itemView;
    }
}
