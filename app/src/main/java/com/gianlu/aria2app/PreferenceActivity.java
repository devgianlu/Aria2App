package com.gianlu.aria2app;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gianlu.commonutils.Preferences.BasePreferenceActivity;
import com.gianlu.commonutils.Preferences.BasePreferenceFragment;
import com.gianlu.commonutils.Preferences.MaterialAboutPreferenceItem;
import com.gianlu.commonutils.Preferences.Views.KeyedMaterialCheckboxPreference;

import java.util.Collections;
import java.util.List;

public class PreferenceActivity extends BasePreferenceActivity {
    @NonNull
    @Override
    protected List<MaterialAboutPreferenceItem> getPreferencesItems() {
        return Collections.singletonList(new MaterialAboutPreferenceItem(R.string.general, R.drawable.baseline_settings_24, GeneralFragment.class));
    }

    public static class GeneralFragment extends BasePreferenceFragment {

        @Override
        protected void buildPreferences(@NonNull Context context) {
            KeyedMaterialCheckboxPreference hideMetadata = new KeyedMaterialCheckboxPreference(context, PK.A2_HIDE_METADATA, false);
            hideMetadata.setTitle(R.string.prefs_hideMetadata);
            hideMetadata.setSummary(R.string.prefs_hideMetadata_summary);
            addPreference(hideMetadata);

            /*
            KeyedMaterialSeekBarPreference updateRate = new KeyedMaterialSeekBarPreference(context, PK.A2_UPDATE_INTERVAL, 1);
            updateRate.setTitle(R.string.prefs_updateRate);
            updateRate.setSummary(R.string.prefs_updateRate_summary);
            addPreference(updateRate);
            */
        }

        @Override
        public int getTitleRes() {
            return R.string.general;
        }
    }
}
