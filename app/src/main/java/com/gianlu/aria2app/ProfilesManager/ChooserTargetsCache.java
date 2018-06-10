package com.gianlu.aria2app.ProfilesManager;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.LruCache;

import com.gianlu.commonutils.LettersIcons.DrawingHelper;

@RequiresApi(api = Build.VERSION_CODES.M)
public class ChooserTargetsCache {
    private static ChooserTargetsCache instance;
    private final LruCache<String, ChooserTarget> cache;

    private ChooserTargetsCache() {
        this.cache = new LruCache<>(20);
    }

    @NonNull
    public static ChooserTargetsCache get() {
        if (instance == null) instance = new ChooserTargetsCache();
        return instance;
    }

    @Nullable
    private ChooserTarget get(@NonNull String id) {
        return cache.get(id);
    }

    @NonNull
    public ChooserTarget getOrGenerate(@NonNull MultiProfile profile, @NonNull DrawingHelper helper, @NonNull ComponentName targetActivity) {
        ChooserTarget target = get(profile.id);
        if (target == null) {
            Bundle bundle = new Bundle();
            bundle.putString("profileId", profile.id);

            Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            helper.draw(profile.name.length() <= 2 ? profile.name : profile.name.substring(0, 2), false, new Canvas(bitmap));
            target = new ChooserTarget(profile.name, Icon.createWithBitmap(bitmap), 1, targetActivity, bundle);
            cache.put(profile.id, target);
        }

        return target;
    }
}
