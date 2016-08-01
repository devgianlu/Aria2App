package com.gianlu.aria2app.DownloadsListing;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;

public class Charting {

    public static final int DOWNLOAD_SET = 1;
    public static final int UPLOAD_SET = 0;

    public static LineChart setupChart(LineChart chart, boolean isCardView) {
        chart.clear();

        chart.setDescription("");
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.alpha(0));
        chart.setTouchEnabled(false);
        Legend l = chart.getLegend();
        l.setCustom(
                new int[]{ContextCompat.getColor(chart.getContext(), R.color.downloadColor), ContextCompat.getColor(chart.getContext(), R.color.uploadColor)},
                new String[]{chart.getContext().getString(R.string.downloadSpeed), chart.getContext().getString(R.string.uploadSpeed)});
        l.setEnabled(true);

        LineData data = new LineData();
        data.setValueTextColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        chart.setData(data);

        YAxis ya = chart.getAxisLeft();
        ya.setAxisLineColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        ya.setTextColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        ya.setTextSize(isCardView ? 8 : 9);
        ya.setDrawAxisLine(false);
        ya.setLabelCount(isCardView ? 4 : 8, true);
        ya.setEnabled(true);
        ya.setAxisMinValue(0f);
        ya.setDrawGridLines(true);
        ya.setValueFormatter(new CustomYAxisValueFormatter());

        chart.getAxisRight().setEnabled(false);
        XAxis xa = chart.getXAxis();
        xa.setEnabled(!isCardView);
        if (!isCardView) {
            xa.setDrawGridLines(false);
            xa.setTextSize(9);
        }

        data.addDataSet(initUploadSet(chart.getContext()));
        data.addDataSet(initDownloadSet(chart.getContext()));

        return chart;
    }

    public static LineDataSet initDownloadSet(Context context) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.downloadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(2f);
        set.setColor(ContextCompat.getColor(context, R.color.downloadColor));
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(false);
        return set;
    }

    public static LineDataSet initUploadSet(Context context) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.uploadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(2f);
        set.setColor(ContextCompat.getColor(context, R.color.uploadColor));
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(false);
        return set;
    }

    public static class CustomYAxisValueFormatter implements YAxisValueFormatter {
        @Override
        public String getFormattedValue(float v, YAxis yAxis) {
            return Utils.speedFormatter(v);
        }
    }
}
