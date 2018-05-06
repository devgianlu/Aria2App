package com.gianlu.aria2app.Services;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import android.support.annotation.RequiresApi;

import com.gianlu.aria2app.R;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.M)
public class ProfileChooserService extends ChooserTargetService { // FIXME
    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName, IntentFilter matchedFilter) {
        List<ChooserTarget> targets = new ArrayList<>();
        targets.add(new ChooserTarget("TEST123", Icon.createWithResource(getApplicationContext(), R.drawable.ic_announcement_black_24dp), 1, targetActivityName, new Bundle()));
        return targets;
    }
}
