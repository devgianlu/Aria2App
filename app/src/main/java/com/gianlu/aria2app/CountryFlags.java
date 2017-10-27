package com.gianlu.aria2app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.LruCache;

public class CountryFlags {
    private static CountryFlags instance;
    private final LruCache<String, Drawable> cache;

    private CountryFlags() {
        cache = new LruCache<>(50);
    }

    public static CountryFlags get() {
        if (instance == null) instance = new CountryFlags();
        return instance;
    }

    @NonNull
    public Drawable loadFlag(Context context, String countryCode) {
        Drawable cachedDrawable = cache.get(countryCode);
        if (cachedDrawable == null) {
            int id = context.getResources().getIdentifier("ic_list_country_" + countryCode.toLowerCase(), "drawable", context.getPackageName());
            if (id == 0) {
                // Don't cache the unknown flag
                return ContextCompat.getDrawable(context, R.drawable.ic_list_country_unknown);
            } else {
                Drawable drawable = ContextCompat.getDrawable(context, id);
                cache.put(countryCode, drawable);
                return drawable;
            }
        } else {
            return cachedDrawable;
        }
    }
}
