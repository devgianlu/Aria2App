package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.NetIO.FreeGeoIP.FreeGeoIPApi;
import com.gianlu.aria2app.NetIO.FreeGeoIP.IPDetails;
import com.gianlu.aria2app.NetIO.JTA2.AriaFile;
import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.github.mikephil.charting.data.PieEntry;

import org.apmem.tools.layouts.FlowLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TopCountriesView extends FlowLayout {
    private final static int DISPLAYED_COUNTRIES = 3;
    private final FreeGeoIPApi freeGeoIp = FreeGeoIPApi.get();
    private final Map<String, Integer> topCountries;
    private final Map<String, String> countryCodeToName;
    private final LayoutInflater inflater;
    private final CountryFlags flags = CountryFlags.get();
    private final List<PieEntry> pieDownloadEntries;
    private final List<PieEntry> pieUploadEntries;

    public TopCountriesView(Context context) {
        this(context, null, 0);
    }

    public TopCountriesView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TopCountriesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        topCountries = new HashMap<>();
        countryCodeToName = new HashMap<>();
        pieDownloadEntries = new ArrayList<>();
        pieUploadEntries = new ArrayList<>();
        inflater = LayoutInflater.from(context);
    }

    private void add(String countryCode, String countryName, Integer speed) {
        Integer totalSpeed = topCountries.get(countryCode);
        if (totalSpeed == null) totalSpeed = 0;
        topCountries.put(countryCode, totalSpeed + speed);
        countryCodeToName.put(countryCode, countryName);
    }

    private void buildLayout() {
        List<Map.Entry<String, Integer>> list = new LinkedList<>(topCountries.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        pieDownloadEntries.clear();
        topCountries.clear();
        for (Map.Entry<String, Integer> entry : list) {
            topCountries.put(entry.getKey(), entry.getValue());
            if (entry.getValue() > 0)
                pieDownloadEntries.add(new PieEntry(entry.getValue(), countryCodeToName.get(entry.getKey())));
        }

        removeAllViews();
        for (int i = 0; i < DISPLAYED_COUNTRIES && i < list.size(); i++) {
            Map.Entry<String, Integer> entry = list.get(i);
            if (entry.getValue() > 0) addView(new ItemView(getContext(), entry));
        }
    }

    public void setPeers(List<Peer> peers) {
        topCountries.clear();

        for (Peer peer : peers) {
            IPDetails details = freeGeoIp.getCached(peer.ip);
            if (details != null) add(details.countryCode, details.countryName, peer.downloadSpeed);
        }

        buildLayout();
    }

    public void setServers(SparseArray<List<Server>> servers, List<AriaFile> files) {
        topCountries.clear();

        for (AriaFile file : files) {
            List<Server> list = servers.get(file.index);
            for (Server server : list) {
                IPDetails details = freeGeoIp.getCached(server.uri.getHost());
                if (details != null)
                    add(details.countryCode, details.countryName, server.downloadSpeed);
            }
        }

        buildLayout();
    }

    public List<PieEntry> getDownloadPieEntries() {
        return pieDownloadEntries;
    }

    public List<PieEntry> getUploadPieEntries() { // TODO
        return pieUploadEntries;
    }

    private class ItemView extends LinearLayout {

        public ItemView(Context context, Map.Entry<String, Integer> entry) {
            super(context);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);

            inflater.inflate(R.layout.item_top_country, this, true);

            ((ImageView) getChildAt(0)).setImageDrawable(flags.loadFlag(context, entry.getKey()));
            ((TextView) getChildAt(1)).setText(CommonUtils.speedFormatter(entry.getValue(), false));
        }
    }
}
