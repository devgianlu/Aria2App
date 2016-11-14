package com.gianlu.aria2app;

import android.content.Context;
import android.preference.PreferenceManager;

import com.gianlu.aria2app.Profile.MultiModeProfileItem;
import com.gianlu.aria2app.Profile.ProfileItem;
import com.gianlu.aria2app.Profile.SingleModeProfileItem;
import com.gianlu.commonutils.CommonUtils;

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
                        profile = SingleModeProfileItem.fromString(context, lastProfile);
                    else
                        profile = MultiModeProfileItem.fromString(context, lastProfile).getCurrentProfile(context);
                } catch (JSONException | IOException ex) {
                    CommonUtils.logMe(context, ex);
                    profile = SingleModeProfileItem.defaultProfile();
                }
            } else {
                profile = SingleModeProfileItem.defaultProfile();
            }
        }

        return profile;
    }

    public static void setCurrentProfile(SingleModeProfileItem profile) {
        CurrentProfile.profile = profile;
    }
}
