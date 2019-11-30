package com.gianlu.aria2app.services;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.os.Build;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import androidx.annotation.RequiresApi;

import com.gianlu.aria2app.profiles.ChooserTargetsCache;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.aria2app.profiles.ProfilesManager;
import com.gianlu.commonutils.lettersicon.DrawingHelper;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.M)
public class ProfileChooserService extends ChooserTargetService {
    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName, IntentFilter matchedFilter) {
        DrawingHelper helper = new DrawingHelper(this);
        List<MultiProfile> profiles = ProfilesManager.get(this).getProfiles();
        List<ChooserTarget> targets = new ArrayList<>();
        ChooserTargetsCache cache = ChooserTargetsCache.get();
        for (MultiProfile profile : profiles)
            targets.add(cache.getOrGenerate(profile, helper, targetActivityName));
        return targets;
    }
}
