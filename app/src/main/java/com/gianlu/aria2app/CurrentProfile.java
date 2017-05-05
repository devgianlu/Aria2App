package com.gianlu.aria2app;

import android.content.Context;
import android.preference.PreferenceManager;

import com.gianlu.aria2app.Profile.MultiModeProfileItem;
import com.gianlu.aria2app.Profile.ProfileItem;
import com.gianlu.aria2app.Profile.SingleModeProfileItem;
import com.gianlu.commonutils.Logging;

import org.json.JSONException;

import java.io.IOException;

public class CurrentProfile {
    private static SingleModeProfileItem profile;

    public static SingleModeProfileItem getCurrentProfile(Context context) {
        if (profile == null) {
            String lastProfile = PreferenceManager.getDefaultSharedPreferences(context).getString("lastUsedProfile", null);

            if (ProfileItem.exists(context, lastProfile)) {
                try {
                    if (ProfileItem.isSingleMode(context, lastProfile))
                        profile = SingleModeProfileItem.fromName(context, lastProfile);
                    else
                        profile = MultiModeProfileItem.fromName(context, lastProfile).getCurrentProfile(context);
                } catch (JSONException | IOException ex) {
                    Logging.logMe(context, ex);
                    profile = SingleModeProfileItem.defaultProfile();
                }
            } else {
                profile = SingleModeProfileItem.defaultProfile();
            }
        }

        return profile;
    }

    public static void setCurrentProfile(Context context, SingleModeProfileItem profile) {
        CurrentProfile.profile = profile;

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("lastUsedProfile", profile.fileName)
                .apply();
    }
}
