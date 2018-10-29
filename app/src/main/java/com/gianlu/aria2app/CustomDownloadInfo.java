package com.gianlu.aria2app;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;

import org.apmem.tools.layouts.FlowLayout;

import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CustomDownloadInfo extends FlowLayout {
    private final int dp16;
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

        dp16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
    }

    public void setDisplayInfo(Info... info) {
        if (info == this.info) return;
        this.info = info;

        removeAllViews();

        for (int i = 0; i < this.info.length; i++)
            addView(new Item(getContext(), info[i]));
    }

    private void setChildText(int index, String text) {
        ((Item) getChildAt(index)).setText(text);
    }

    public void update(DownloadWithUpdate.SmallUpdate download) {
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

        public static String[] formalValues(@NonNull Context context) {
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

            Info[] arr = new Info[infos.size() - (!isTorrent && infos.contains(SEEDERS.name()) ? 1 : 0)];

            int i = 0;
            for (String infoStr : infos) {
                Info info = valueOf(infoStr);
                if (!isTorrent && info == SEEDERS) continue;

                arr[i] = info;
                i++;
            }

            return arr;
        }
    }

    private final class Item extends LinearLayout {
        public Item(Context context, Info info) {
            super(context);

            setGravity(Gravity.CENTER_VERTICAL);

            ImageView icon = new ImageView(context);
            icon.setColorFilter(Color.WHITE);
            switch (info) {
                case DOWNLOAD_SPEED:
                    icon.setImageResource(R.drawable.baseline_download_24);
                    break;
                case UPLOAD_SPEED:
                    icon.setImageResource(R.drawable.baseline_upload_24);
                    break;
                case REMAINING_TIME:
                    icon.setImageResource(R.drawable.baseline_access_time_24);
                    break;
                case COMPLETED_LENGTH:
                    icon.setImageResource(R.drawable.baseline_folder_24);
                    break;
                case CONNECTIONS:
                    icon.setImageResource(R.drawable.baseline_link_24);
                    break;
                case SEEDERS:
                    icon.setImageResource(R.drawable.baseline_people_24);
                    break;
            }

            LayoutParams params = new LayoutParams(dp16, dp16);
            params.rightMargin = dp16 / 8;
            icon.setLayoutParams(params);
            addView(icon);
        }

        public void setText(String text) {
            SuperTextView textView = (SuperTextView) getChildAt(1);
            if (textView == null) {
                textView = new SuperTextView(getContext(), text);
                textView.setPaddingRelative(0, 0, dp16 / 2, 0);
                addView(textView);
                return;
            }

            textView.setText(text);
        }
    }
}
