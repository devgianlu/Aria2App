package com.gianlu.aria2app.NetIO.Geolocalization;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.misc.SuperTextView;

public class IPDetailsView extends LinearLayout {
    private final ImageView flag;
    private final SuperTextView ip;
    private final SuperTextView localization;
    private final SuperTextView domain;
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
        domain = findViewById(R.id.ipDetailView_isp);
    }

    public void setup(@NonNull IPDetails details) {
        flag.setImageDrawable(flags.loadFlag(getContext(), details.countryCode));
        ip.setHtml(R.string.ip, details.ip);
        localization.setHtml(R.string.localization, details.getNiceLocalizationString());
        if (details.domain == null || details.domain.isEmpty()) {
            domain.setVisibility(GONE);
        } else {
            domain.setVisibility(VISIBLE);
            domain.setHtml(R.string.domain, details.domain);
        }
    }
}
