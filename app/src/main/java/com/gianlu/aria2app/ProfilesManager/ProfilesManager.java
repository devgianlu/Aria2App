package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ProfilesManager {
    private static ProfilesManager instance;
    private final File profilesPath;
    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;
    private MultiProfile currentProfile;

    private ProfilesManager(@NonNull Context context) {
        profilesPath = context.getFilesDir();
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @NonNull
    public static ProfilesManager get(@NonNull Context context) {
        if (instance == null) instance = new ProfilesManager(context.getApplicationContext());
        return instance;
    }

    @NonNull
    public static String getId(@NonNull String name) {
        return Base64.encodeToString(name.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void delete(@NonNull MultiProfile profile) {
        new File(profilesPath, profile.id + ".profile").delete();
    }

    @NonNull
    public MultiProfile.UserProfile getCurrentSpecific() throws NoCurrentProfileException {
        return getCurrent().getProfile(this);
    }

    @NonNull
    public MultiProfile getCurrent() throws NoCurrentProfileException {
        if (currentProfile == null) currentProfile = getLastProfile();
        if (currentProfile == null) throw new NoCurrentProfileException();
        return currentProfile;
    }

    public void setCurrent(@NonNull MultiProfile profile) {
        currentProfile = profile;
        setLastProfile(profile);
    }

    @Nullable
    public MultiProfile getLastProfile() {
        String id = Prefs.getString(PK.LAST_USED_PROFILE, null);
        if (id == null || !profileExists(id)) return null;
        try {
            return retrieveProfile(id);
        } catch (IOException | JSONException ex) {
            Logging.log(ex);
            return null;
        }
    }

    public void setLastProfile(@NonNull MultiProfile profile) {
        Prefs.putString(PK.LAST_USED_PROFILE, profile.id);
    }

    public void unsetLastProfile() {
        Prefs.remove(PK.LAST_USED_PROFILE);
    }

    @NonNull
    public MultiProfile retrieveProfile(@NonNull String id) throws IOException, JSONException {
        if (!profileExists(id))
            throw new FileNotFoundException("Profile " + id + " doesn't exists!");

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(profilesPath, id + ".profile"))));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) builder.append(line);

        return new MultiProfile(new JSONObject(builder.toString()));
    }

    public boolean profileExists(@NonNull String id) {
        File file = new File(profilesPath, id + ".profile");
        return file.exists() && file.canRead();
    }

    public boolean hasProfiles() {
        return getProfileIds().length > 0 || CommonUtils.isARM();
    }

    public boolean hasNotificationProfiles(@NonNull Context context) {
        context = context.getApplicationContext();
        if (CommonUtils.isARM() && context instanceof ThisApplication && ((ThisApplication) context).getLastAria2UiState())
            return true;

        for (String id : getProfileIds()) {
            try {
                if (retrieveProfile(id).notificationsEnabled)
                    return true;
            } catch (IOException | JSONException ex) {
                Logging.log(ex);
            }
        }

        return false;
    }

    @NonNull
    private String[] getProfileIds() {
        File[] profiles = profilesPath.listFiles((dir, name) -> name.endsWith(".profile"));

        if (profiles == null) return new String[0];

        String[] ids = new String[profiles.length];
        for (int i = 0; i < profiles.length; i++) {
            File file = profiles[i];
            String id = file.getName().substring(0, file.getName().length() - 8);
            if (id.contains("+")) {
                id = id.replace('+', '-');
                if (!file.renameTo(new File(file.getParentFile(), id + ".profile")))
                    Logging.log("Failed renaming profile: " + id, true);
            }

            ids[i] = id;
        }

        return ids;
    }

    @NonNull
    public List<MultiProfile> getNotificationProfiles(@NonNull Context context) {
        return getProfiles(true, context);
    }

    @NonNull
    public List<MultiProfile> getProfiles() {
        return getProfiles(false, null);
    }

    @NonNull
    private List<MultiProfile> getProfiles(boolean notification, @Nullable Context context) {
        context = context == null ? null : context.getApplicationContext();

        boolean hasInApp = false;
        List<MultiProfile> profiles = new ArrayList<>();
        for (String id : getProfileIds()) {
            try {
                MultiProfile profile = retrieveProfile(id);
                if (profile.isInAppDownloader()) hasInApp = true;
                if (notification) {
                    if (profile.isInAppDownloader()) {
                        if ((context instanceof ThisApplication) && ((ThisApplication) context).getLastAria2UiState())
                            profiles.add(profile);
                    } else if (profile.notificationsEnabled) {
                        profiles.add(profile);
                    }
                } else {
                    profiles.add(profile);
                }
            } catch (IOException | JSONException ex) {
                Logging.log(ex);
            }
        }

        if (CommonUtils.isARM() && !hasInApp) {
            MultiProfile inApp = MultiProfile.forInAppDownloader();
            profiles.add(inApp);

            try {
                save(inApp);
            } catch (IOException | JSONException | IllegalStateException ex) {
                Logging.log("Failed saving in-app downloader profile.", ex);
            }
        }

        return profiles;
    }

    public void save(@NonNull MultiProfile profile) throws IOException, JSONException {
        File file = new File(profilesPath, profile.id + ".profile");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(profile.toJson().toString().getBytes());
            out.flush();
        }
    }

    public void reloadCurrentProfile() throws IOException, JSONException, NoCurrentProfileException {
        setCurrent(retrieveProfile(getCurrent().id));
    }

    @NonNull
    public WifiManager getWifiManager() {
        return wifiManager;
    }

    @NonNull
    public ConnectivityManager getConnectivityManager() {
        return connectivityManager;
    }

    public static class NoCurrentProfileException extends Exception {
    }
}
