package com.gianlu.aria2app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

public class CountryFlags {

    @NonNull
    public static Drawable loadFlag(Context context, String countryCode) {
        int id = context.getResources().getIdentifier("ic_list_country_" + countryCode.toLowerCase(), "drawable", context.getPackageName());
        if (id == 0) return ContextCompat.getDrawable(context, R.drawable.ic_list_country_unknown);
        else return ContextCompat.getDrawable(context, id);
    }
}
