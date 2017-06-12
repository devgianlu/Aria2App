package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.BuildConfig;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Prefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ProfilesManager {
    private static ProfilesManager instance;
    private final File PROFILES_PATH;
    private MultiProfile currentProfile;

    private ProfilesManager(Context context) {
        PROFILES_PATH = context.getFilesDir();
    }

    public static ProfilesManager get(Context context) {
        if (instance == null) instance = new ProfilesManager(context);
        return instance;
    }

    public static String getId(String name) {
        return Base64.encodeToString(name.getBytes(), Base64.NO_WRAP);
    }

    @Nullable
    public static MultiProfile createExternalProfile(Intent intent) {
        String token = intent.getStringExtra("token");
        int port = intent.getIntExtra("port", -1);

        if (token == null || port == -1) {
            return null;
        }

        return MultiProfile.createForExternal(token, port);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void delete(MultiProfile profile) {
        new File(PROFILES_PATH, profile.id + ".profile").delete();
    }

    @Nullable
    public MultiProfile getCurrent() {
        return currentProfile;
    }

    public void setCurrent(Context context, MultiProfile profile) {
        currentProfile = profile;
        setLastProfile(context, profile);
    }

    @NonNull
    public MultiProfile getCurrentAssert() throws NullPointerException {
        if (currentProfile == null) throw new NullPointerException("currentProfile is null!");
        return currentProfile;
    }

    @Nullable
    public MultiProfile getLastProfile(Context context) {
        String id = Prefs.getString(context, Prefs.Keys.LAST_USED_PROFILE, null);
        if (id == null || !profileExists(id)) return null;
        try {
            return retrieveProfile(id);
        } catch (IOException | JSONException ex) {
            Logging.logMe(context, ex);
            return null;
        }
    }

    public void setLastProfile(Context context, MultiProfile profile) {
        Prefs.editString(context, Prefs.Keys.LAST_USED_PROFILE, profile.id);
    }

    public MultiProfile retrieveProfile(@NonNull String id) throws IOException, JSONException {
        if (!profileExists(id))
            throw new FileNotFoundException("Profile " + id + " doesn't exists!");

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(PROFILES_PATH, id + ".profile"))));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) builder.append(line);

        return new MultiProfile(new JSONObject(builder.toString()));
    }

    public boolean profileExists(@NonNull String id) {
        File file = new File(PROFILES_PATH, id + ".profile");
        return file.exists() && file.canRead();
    }

    public boolean hasProfiles() {
        String[] profiles = PROFILES_PATH.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".profile");
            }
        });

        return profiles.length > 0;
    }

    public List<MultiProfile> getProfiles() {
        String[] profilesId = PROFILES_PATH.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".profile");
            }
        });

        List<MultiProfile> profiles = new ArrayList<>();
        for (String id : profilesId) {
            try {
                profiles.add(retrieveProfile(id.replace(".profile", "")));
            } catch (IOException | JSONException ex) {
                if (BuildConfig.DEBUG) ex.printStackTrace();
            }
        }

        return profiles;
    }

    public void save(MultiProfile profile) throws IOException, JSONException {
        File file = new File(PROFILES_PATH, profile.id + ".profile");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(profile.toJSON().toString().getBytes());
            out.flush();
        }
    }

    public void reloadCurrentProfile(Context context) throws IOException, JSONException {
        setCurrent(context, retrieveProfile(getCurrentAssert().id));
    }
}
