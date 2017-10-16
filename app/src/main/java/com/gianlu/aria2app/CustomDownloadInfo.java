package com.gianlu.aria2app;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;

import java.util.Collection;

// TODO: Should handle overflow
public class CustomDownloadInfo extends LinearLayout {
    private Info[] info = new Info[0];

    public CustomDownloadInfo(Context context) {
        this(context, null, 0);
    }

    public CustomDownloadInfo(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomDownloadInfo(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(HORIZONTAL);
    }

    public void setDisplayInfo(Info... info) {
        if (info == this.info) return;
        this.info = info;

        removeAllViews();

        for (int i = 0; i < this.info.length; i++) {
            inflate(getContext(), R.layout.custom_download_info_item, this);
            switch (this.info[i]) {
                case DOWNLOAD_SPEED:
                    setChildImage(i, R.drawable.ic_file_download_white_48dp);
                    break;
                case UPLOAD_SPEED:
                    setChildImage(i, R.drawable.ic_file_upload_white_48dp);
                    break;
                case REMAINING_TIME:
                    setChildImage(i, R.drawable.ic_access_time_white_48dp);
                    break;
                case COMPLETED_LENGTH:
                    setChildImage(i, R.drawable.ic_folder_white_48dp);
                    break;
                case CONNECTIONS:
                    setChildImage(i, R.drawable.ic_link_white_48dp);
                    break;
                case SEEDERS:
                    setChildImage(i, R.drawable.ic_people_white_48dp);
                    break;
            }
        }
    }

    private void setChildImage(int index, @DrawableRes int icon) {
        ((ImageView) getChildAt(index * 2)).setImageResource(icon);
    }

    private void setChildText(int index, String text) {
        ((SuperTextView) getChildAt(index * 2 + 1)).setText(text);
    }

    public void update(Download download) {
        if (info.length == 0) return;

        for (int i = 0; i < this.info.length; i++) {
            switch (info[i]) {
                case DOWNLOAD_SPEED:
                    setChildText(i, CommonUtils.speedFormatter(download.downloadSpeed, false));
                    break;
                case UPLOAD_SPEED:
                    setChildText(i, CommonUtils.speedFormatter(download.uploadSpeed, false));
                    break;
                case REMAINING_TIME:
                    setChildText(i, CommonUtils.timeFormatter(download.getMissingTime()));
                    break;
                case COMPLETED_LENGTH:
                    setChildText(i, CommonUtils.dimensionFormatter(download.completedLength, false));
                    break;
                case CONNECTIONS:
                    setChildText(i, String.valueOf(download.connections));
                    break;
                case SEEDERS:
                    setChildText(i, String.valueOf(download.numSeeders));
                    break;
            }
        }
    }

    public enum Info {
        DOWNLOAD_SPEED,
        UPLOAD_SPEED,
        REMAINING_TIME,
        COMPLETED_LENGTH,
        CONNECTIONS,
        SEEDERS;

        public static String[] stringValues() {
            Info[] values = values();
            String[] strValues = new String[values.length];
            for (int i = 0; i < values.length; i++) strValues[i] = values[i].name();
            return strValues;
        }

        public static String[] formalValues(Context context) {
            Info[] values = values();
            String[] strValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                switch (values[i]) {
                    case DOWNLOAD_SPEED:
                        strValues[i] = context.getString(R.string.downloadSpeed);
                        break;
                    case UPLOAD_SPEED:
                        strValues[i] = context.getString(R.string.uploadSpeed);
                        break;
                    case REMAINING_TIME:
                        strValues[i] = context.getString(R.string.remainingTime);
                        break;
                    case COMPLETED_LENGTH:
                        strValues[i] = context.getString(R.string.completedLengthLabel);
                        break;
                    case CONNECTIONS:
                        strValues[i] = context.getString(R.string.connectionsLabel);
                        break;
                    case SEEDERS:
                        strValues[i] = context.getString(R.string.numSeederLabel);
                        break;
                }
            }

            return strValues;
        }

        public static Info[] toArray(Collection<String> infos, boolean isTorrent) {
            if (infos == null || infos.isEmpty()) return new Info[]{DOWNLOAD_SPEED, REMAINING_TIME};

            Info[] arr = new Info[infos.size() - (isTorrent && infos.contains(CONNECTIONS.name()) || !isTorrent && infos.contains(SEEDERS.name()) ? 1 : 0)];

            int i = 0;
            for (String infoStr : infos) {
                Info info = valueOf(infoStr);
                if (isTorrent) {
                    if (info != CONNECTIONS) {
                        arr[i] = info;
                        i++;
                    }
                } else {
                    if (info != SEEDERS) {
                        arr[i] = info;
                        i++;
                    }
                }
            }

            return arr;
        }
    }
}
