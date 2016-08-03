package com.gianlu.aria2app.MoreAboutDownload.ServersFragment;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gianlu.aria2app.DownloadsListing.Charting;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ServerCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Item> items = new ArrayList<>();

    public ServerCardAdapter(Context context, Map<Integer, List<Server>> objs) {
        this.context = context;

        for (Integer index : objs.keySet()) {
            HeaderItem header = new HeaderItem(index);
            items.add(header);

            for (Server server : objs.get(index)) {
                items.add(server);
            }
        }
    }

    public static boolean isExpanded(View v) {
        return v.getVisibility() == View.VISIBLE;
    }

    public static void expand(final View v) {
        v.measure(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? RelativeLayout.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    public static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == Item.HEADER) {
            return new HeaderViewHolder(new TextView(context));
        } else {
            return new ServerViewHolder(LayoutInflater.from(context).inflate(R.layout.server_cardview, parent, false));
        }
    }

    public void onUpdate(Map<Integer, List<Server>> servers) {
        if (items == null || servers == null) return;

        /**
         for (Integer index : servers.keySet()) {
         for (Item item : items) {
         if (item.getItemType() == Item.HEADER)
         if (((HeaderItem) item).getIndex() == index)
         }
         }

         for (Server newServer : servers) {
         for (Server listServer : objs) {
         if (listServer.currentUri.equals(newServer.currentUri))
         notifyItemChanged(objs.indexOf(listServer), newServer);
         }
         }
         **/
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getItemType();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder cHolder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(cHolder, position);
            return;
        }

        if (payloads.get(0) instanceof Server && cHolder instanceof ServerViewHolder) {
            Server server = (Server) payloads.get(0);
            ServerViewHolder holder = (ServerViewHolder) cHolder;

            LineData data = holder.chart.getData();
            data.addXValue(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));
            data.addEntry(new Entry(server.downloadSpeed, data.getDataSetByIndex(Charting.DOWNLOAD_SET).getEntryCount()), Charting.DOWNLOAD_SET);

            holder.chart.notifyDataSetChanged();
            holder.chart.setVisibleXRangeMaximum(60);
            holder.chart.moveViewToX(data.getXValCount() - 61);

            holder.downloadSpeed.setText(Utils.speedFormatter(server.downloadSpeed));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder cHolder, int position) {
        if (getItemViewType(position) == Item.HEADER) {
            HeaderItem header = (HeaderItem) getItem(position);
            final HeaderViewHolder holder = (HeaderViewHolder) cHolder;

            holder.title.setText(header.getTitle());
        } else {
            Server server = (Server) getItem(position);
            final ServerViewHolder holder = (ServerViewHolder) cHolder;

            holder.chart = Charting.setupPeerChart(holder.chart);
            holder.downloadSpeed.setText(Utils.speedFormatter(server.downloadSpeed));

            holder.header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isExpanded(holder.details))
                        collapse(holder.details);
                    else
                        expand(holder.details);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Item getItem(int position) {
        return items.get(position);
    }
}
