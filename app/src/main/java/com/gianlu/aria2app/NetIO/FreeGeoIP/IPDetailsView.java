package com.gianlu.aria2app.NetIO.FreeGeoIP;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

public class IPDetailsView extends LinearLayout {
    private final ImageView flag;
    private final SuperTextView ip;
    private final SuperTextView localization;
    private final SuperTextView timezone;

    public IPDetailsView(Context context) {
        this(context, null, 0);
    }

    public IPDetailsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IPDetailsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(getContext()).inflate(R.layout.ip_details_view, this, true);

        flag = findViewById(R.id.ipDetailView_flag);
        ip = findViewById(R.id.ipDetailView_ip);
        localization = findViewById(R.id.ipDetailView_localization);
        timezone = findViewById(R.id.ipDetailView_timezone);
    }

    public void setup(@NonNull IPDetails details) {
        flag.setImageDrawable(CountryFlags.loadFlag(getContext(), details.countryCode));
        ip.setHtml(R.string.ip, details.ip);
        localization.setHtml(R.string.localization, details.getNiceLocalizationString());

        if (details.timeZone.isEmpty()) {
            timezone.setVisibility(View.GONE);
        } else {
            timezone.setHtml(R.string.timezone, details.timeZone); // TODO: Format better
            timezone.setVisibility(View.VISIBLE);
        }
    }
}
