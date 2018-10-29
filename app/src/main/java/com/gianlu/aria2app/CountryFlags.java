package com.gianlu.aria2app;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public final class CountryFlags {
    private static final int MAX_CACHE_SIZE = 8 * 1024; // 8 MiB
    private static CountryFlags instance;
    private final LruCache<String, Drawable> cache;

    private CountryFlags() {
        cache = new LruCache<String, Drawable>(MAX_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Drawable value) {
                if (value instanceof BitmapDrawable)
                    return ((BitmapDrawable) value).getBitmap().getByteCount();
                else
                    return value.getIntrinsicHeight() * value.getIntrinsicWidth();
            }
        };
    }

    @NonNull
    public static CountryFlags get() {
        if (instance == null) instance = new CountryFlags();
        return instance;
    }

    @SuppressWarnings("ConstantConditions")
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
