package com.gianlu.aria2app.DownloadsListing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.util.ArrayList;
import java.util.List;

public class DownloadItemAdapter extends BaseAdapter {
    static List<Download.STATUS> filterOut = new ArrayList<>();
    List<DownloadItem> objs;
    private Context context;

    public DownloadItemAdapter(Context context, List<DownloadItem> objects) {
        this.context = context;
        objs = objects;
    }

    public DownloadItem getItem(String gid) {
        for (DownloadItem item : objs) if (item.download.GID.equals(gid)) return item;
        return null;
    }

    @Override
    public int getCount() {
        int c = 0;
        for (int i = 0; i < objs.size(); i++) {
            if (!filterOut.contains(objs.get(i).getDownloadStatus())) c++;
        }
        return c;
    }

    @Override
    public DownloadItem getItem(int i) {
        return objs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void addFilter(Download.STATUS filter) {
        filterOut.add(filter);
        notifyDataSetChanged();
    }

    public void removeFilter(Download.STATUS filter) {
        filterOut.remove(filter);
        notifyDataSetChanged();
    }

    @SuppressLint({"InflateParams", "ViewHolder"})
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DownloadItem item = getItem(position);

        if (filterOut.contains(item.getDownloadStatus())) {
            position++;
            return getView(position, null, null);
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.download_custom_item, null);
        TextView downloadNameTextView = (TextView) convertView.findViewById(R.id.a2_customItem_name);
        ImageView downloadStatusImageView = (ImageView) convertView.findViewById(R.id.a2_customItem_status);
        ProgressBar downloadProgressBar = (ProgressBar) convertView.findViewById(R.id.a2_customItem_progress);
        TextView downloadPercentageTextView = (TextView) convertView.findViewById(R.id.a2_customItem_percentage);
        TextView downloadSpeedTextView = (TextView) convertView.findViewById(R.id.a2_customItem_DSpeed);
        TextView downloadTimeTextView = (TextView) convertView.findViewById(R.id.a2_customItem_time);

        //Download name
        downloadNameTextView.setText(item.getDownloadName());

        //Download percentage progress
        downloadPercentageTextView.setText(item.getDownloadPercentage());
        downloadProgressBar.setMax(100);
        downloadProgressBar.setProgress(Math.round(item.getDownloadProgress()));

        //Download speed
        downloadSpeedTextView.setText(Utils.SpeedFormatter(item.getDownloadSpeed()));

        //Time remaining
        downloadTimeTextView.setText(Utils.TimeFormatter(item.getDownloadTime()));

        // Status image and various
        switch (item.getDownloadStatus()) {
            case ACTIVE:
                downloadStatusImageView.setImageDrawable(context.getDrawable(R.drawable.ic_play_arrow_black_48dp));
                break;
            case WAITING:
                downloadStatusImageView.setImageDrawable(context.getDrawable(R.drawable.ic_queue_black_48dp));
                downloadSpeedTextView.setText(Utils.SpeedFormatter(0));
                downloadTimeTextView.setText(R.string.downloadStatus_waiting);
                break;
            case ERROR:
                downloadStatusImageView.setImageDrawable(context.getDrawable(R.drawable.ic_error_black_48dp));
                downloadSpeedTextView.setText(Utils.SpeedFormatter(0));
                downloadTimeTextView.setText(R.string.downloadStatus_error);
                break;
            case PAUSED:
                downloadStatusImageView.setImageDrawable(context.getDrawable(R.drawable.ic_pause_black_48dp));
                downloadSpeedTextView.setText(Utils.SpeedFormatter(0));
                downloadTimeTextView.setText(R.string.downloadStatus_paused);
                break;
            case COMPLETE:
                downloadStatusImageView.setImageDrawable(context.getDrawable(R.drawable.ic_done_black_48dp));
                downloadSpeedTextView.setText(Utils.SpeedFormatter(0));
                downloadTimeTextView.setText(R.string.downloadStatus_complete);
                break;
            case REMOVED:
                downloadStatusImageView.setImageDrawable(context.getDrawable(R.drawable.ic_clear_black_48dp));
                downloadSpeedTextView.setText(Utils.SpeedFormatter(0));
                downloadTimeTextView.setText(R.string.downloadStatus_removed);
                break;
            default:
                downloadStatusImageView.setImageDrawable(context.getDrawable(R.drawable.ic_help_black_48dp));
                downloadSpeedTextView.setText(Utils.SpeedFormatter(0));
                downloadTimeTextView.setText(R.string.downloadStatus_unknown);
                break;
        }

        return convertView;
    }
}
