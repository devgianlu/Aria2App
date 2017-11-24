package com.gianlu.aria2app.Activities.EditProfile;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class WifisAdapter extends ArrayAdapter<String> implements Filterable {
    private final List<WifiConfiguration> wifis;
    private final List<WifiConfiguration> originalWifis;
    private CustomFilter filter;

    public WifisAdapter(Context context, @Nullable List<WifiConfiguration> wifis) {
        super(context, android.R.layout.simple_list_item_1);
        if (wifis == null) originalWifis = new ArrayList<>();
        else originalWifis = wifis;
        this.wifis = new ArrayList<>(originalWifis);
    }

    @Override
    public int getCount() {
        return wifis.size();
    }

    @Override
    @NonNull
    public String getItem(int position) {
        String ssid = wifis.get(position).SSID;
        return ssid.substring(1, ssid.length() - 1);
    }

    @Override
    public long getItemId(int position) {
        return wifis.get(position).hashCode();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TextView text = (TextView) super.getView(position, convertView, parent);
        text.setText(getItem(position));
        return text;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (filter == null) filter = new CustomFilter();
        return filter;
    }

    private class CustomFilter extends Filter {

        @Override
        @SuppressWarnings("ConstantConditions")
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if (constraint == null || constraint.length() == 0) {
                results.values = new ArrayList<>(originalWifis);
                results.count = originalWifis.size();
            } else {
                String query = constraint.toString().toLowerCase();

                List<WifiConfiguration> filteredWifis = new ArrayList<>();
                for (int i = 0; i < originalWifis.size(); i++) {
                    WifiConfiguration item = originalWifis.get(i);
                    String ssid = originalWifis.get(i).SSID;
                    if (ssid.substring(1, ssid.length() - 1).toLowerCase().contains(query))
                        filteredWifis.add(item);
                }

                results.values = filteredWifis;
                results.count = filteredWifis.size();
            }

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            wifis.clear();
            wifis.addAll((ArrayList<WifiConfiguration>) results.values);
            notifyDataSetChanged();
        }
    }
}
