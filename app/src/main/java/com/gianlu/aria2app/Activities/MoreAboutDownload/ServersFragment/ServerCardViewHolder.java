package com.gianlu.aria2app.Activities.MoreAboutDownload.ServersFragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.gianlu.aria2app.R;

class ServerCardViewHolder extends RecyclerView.ViewHolder {
    public final TextView uri;
    public final TextView downloadSpeed;
    final TextView currentUri;

    ServerCardViewHolder(View itemView) {
        super(itemView);

        currentUri = (TextView) itemView.findViewById(R.id.serverCardView_currentUri);
        uri = (TextView) itemView.findViewById(R.id.serverCardView_uri);
        downloadSpeed = (TextView) itemView.findViewById(R.id.serverCardView_downloadSpeed);
    }
}
