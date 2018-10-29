package com.gianlu.aria2app.Services;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.os.Build;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import com.gianlu.aria2app.ProfilesManager.ChooserTargetsCache;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.commonutils.LettersIcons.DrawingHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.RequiresApi;

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
