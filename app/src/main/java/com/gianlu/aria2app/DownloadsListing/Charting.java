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

        data.addDataSet(initUploadSet(chart.getContext(), 2f));
        data.addDataSet(initDownloadSet(chart.getContext(), 2f));

        return chart;
    }

    public static LineChart setupPeerChart(LineChart chart) {
        chart.clear();

        chart.setDescription("");
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.alpha(0));
        chart.setTouchEnabled(false);
        chart.getLegend().setEnabled(false);

        LineData data = new LineData();
        data.setValueTextColor(ContextCompat.getColor(chart.getContext(), R.color.white));
        chart.setData(data);

        YAxis ya = chart.getAxisLeft();
        ya.setAxisLineColor(ContextCompat.getColor(chart.getContext(), R.color.white));
        ya.setTextColor(ContextCompat.getColor(chart.getContext(), R.color.white));
        ya.setTextSize(8);
        ya.setDrawAxisLine(false);
        ya.setLabelCount(3, true);
        ya.setEnabled(true);
        ya.setAxisMinValue(0f);
        ya.setDrawGridLines(true);
        ya.setValueFormatter(new CustomYAxisValueFormatter());

        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);

        data.addDataSet(initUploadSet(chart.getContext(), 1f));
        data.addDataSet(initDownloadSet(chart.getContext(), 1f));

        return chart;
    }

    public static LineDataSet initDownloadSet(Context context, float lineWidth) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.downloadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(lineWidth);
        set.setColor(ContextCompat.getColor(context, R.color.downloadColor));
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(false);
        return set;
    }

    public static LineDataSet initUploadSet(Context context, float lineWidth) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.uploadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(lineWidth);
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
