package com.gianlu.aria2app.NetIO.Geolocalization;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

public class IPDetailsView extends LinearLayout {
    private final ImageView flag;
    private final SuperTextView ip;
    private final SuperTextView localization;
    private final SuperTextView isp;
    private final CountryFlags flags = CountryFlags.get();

    public IPDetailsView(Context context) {
        this(context, null, 0);
    }

    public IPDetailsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IPDetailsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(getContext()).inflate(R.layout.view_ip_details, this, true);

        flag = findViewById(R.id.ipDetailView_flag);
        ip = findViewById(R.id.ipDetailView_ip);
        localization = findViewById(R.id.ipDetailView_localization);
        isp = findViewById(R.id.ipDetailView_isp);
    }

    public void setup(@NonNull IPDetails details) {
        flag.setImageDrawable(flags.loadFlag(getContext(), details.countryCode));
        ip.setHtml(R.string.ip, details.getIp());
        localization.setHtml(R.string.localization, details.getNiceLocalizationString());
        isp.setHtml(R.string.isp, details.getIsp());
    }
}
