package com.gianlu.aria2app.DownloadsListing;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;

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
    public static void newChart(Activity context, final LineChart chart) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newChart(chart);
            }
        });
    }

    public static void newChart(final LineChart chart) {
        chart.clear();

        chart.setDescription("");
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.alpha(0));
        chart.setTouchEnabled(false);

        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        chart.setData(data);
        Legend l = chart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);

        XAxis xl = chart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setSpaceBetweenLabels(5);
        xl.setEnabled(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setAxisMinValue(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setValueFormatter(new CustomYAxisValueFormatter());

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    public static LineDataSet InitDownloadSet(Context context) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.downloadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.rgb(154, 204, 0));
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setFillAlpha(65);
        set.setFillColor(Color.rgb(154, 204, 0));
        set.setDrawValues(false);
        set.setDrawCubic(true);
        set.setDrawFilled(true);
        return set;
    }

    public static LineDataSet InitUploadSet(Context context) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.uploadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.rgb(51, 181, 229));
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setFillAlpha(65);
        set.setFillColor(Color.rgb(51, 181, 229));
        set.setDrawValues(false);
        set.setDrawCubic(true);
        set.setDrawFilled(true);
        return set;
    }

    public static class CustomYAxisValueFormatter implements YAxisValueFormatter {
        @Override
        public String getFormattedValue(float v, YAxis yAxis) {
            return Utils.SpeedFormatter(v);
        }
    }
}
