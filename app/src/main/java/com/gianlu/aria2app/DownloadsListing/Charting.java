package com.gianlu.aria2app.DownloadsListing;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;

public class Charting {

    public static LineChart setupChart(LineChart chart) {
        chart.clear();

        chart.setDescription("");
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.alpha(0));
        chart.setTouchEnabled(false);
        chart.getLegend().setEnabled(false);

        LineData data = new LineData();
        data.setValueTextColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        chart.setData(data);

        XAxis xa = chart.getXAxis();
        xa.setPosition(XAxis.XAxisPosition.BOTTOM);
        xa.setAxisLineColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        xa.setTextColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        xa.setTextSize(8);
        xa.setAxisLineWidth(2f);
        xa.setDrawGridLines(false);
        xa.setAvoidFirstLastClipping(true);
        xa.setSpaceBetweenLabels(5);
        xa.setEnabled(true);

        YAxis ya = chart.getAxisLeft();
        ya.setAxisLineColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        ya.setTextColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        ya.setTextSize(8);
        ya.setAxisLineWidth(2f);
        ya.setLabelCount(4, true);
        ya.setEnabled(true);
        ya.setAxisMinValue(0f);
        ya.setDrawGridLines(false);
        ya.setValueFormatter(new CustomYAxisValueFormatter());

        chart.getAxisRight().setEnabled(false);

        data.addDataSet(initDownloadSet(chart.getContext()));
        data.addDataSet(initUploadSet(chart.getContext()));

        return chart;
    }

    public static LineDataSet initDownloadSet(Context context) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.downloadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(0f);
        set.setDrawCircles(false);
        set.setFillAlpha(65);
        set.setFillColor(Color.rgb(154, 204, 0));
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        return set;
    }

    public static LineDataSet initUploadSet(Context context) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.uploadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(0f);
        set.setDrawCircles(false);
        set.setFillAlpha(65);
        set.setFillColor(Color.rgb(51, 181, 229));
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
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
