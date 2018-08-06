package com.gianlu.aria2app;

import android.support.annotation.NonNull;

import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.gianlu.commonutils.Preferences.BasePreferenceActivity;

import java.util.ArrayList;
import java.util.List;

public class NewPreferenceActivity extends BasePreferenceActivity {
    @NonNull
    @Override
    protected List<MaterialAboutItem> getPreferencesItems() {
        return new ArrayList<>();
    }
}
