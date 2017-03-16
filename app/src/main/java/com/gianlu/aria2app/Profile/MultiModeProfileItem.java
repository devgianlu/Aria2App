package com.gianlu.aria2app.Profile;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MultiModeProfileItem extends ProfileItem implements Serializable {
    private final Map<ConnectivityCondition, SingleModeProfileItem> profiles = new HashMap<>();

    private MultiModeProfileItem() {
        this.singleMode = false;
        this.status = ProfileItem.STATUS.UNKNOWN;
    }

    private static MultiModeProfileItem fromJSON(String fileName, String json) throws JSONException, IOException {
        JSONObject jProfile = new JSONObject(json);
        MultiModeProfileItem item = new MultiModeProfileItem();
        item.fileName = fileName;
        item.globalProfileName = jProfile.getString("name");
        item.notificationsEnabled = jProfile.optBoolean("notificationsEnabled", true);

        JSONArray conditions = jProfile.getJSONArray("conditions");

        for (int i = 0; i < conditions.length(); i++) {
            JSONObject condition = conditions.getJSONObject(i);

            SingleModeProfileItem profile = SingleModeProfileItem.fromJSON(fileName, condition.getJSONObject("profile").toString());
            profile.setGlobalProfileName(item.globalProfileName);
            switch (ConnectivityCondition.getTypeFromString(condition.getString("type"))) {
                case WIFI:
                    item.addProfile(ConnectivityCondition.newWiFiCondition(condition.getString("ssid")), profile);
                    break;
                case MOBILE:
                    item.addProfile(ConnectivityCondition.newMobileCondition(), profile);
                    break;
                case ETHERNET:
                    item.addProfile(ConnectivityCondition.newEthernetCondition(), profile);
                    break;
                case BLUETOOTH:
                    item.addProfile(ConnectivityCondition.newBluetoothCondition(), profile);
                    break;
            }
        }

        return item;
    }

    public static MultiModeProfileItem fromName(Context context, String fileName) throws IOException, JSONException {
        FileInputStream in = context.openFileInput(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return fromJSON(fileName, builder.toString());
    }

    private void addProfile(ConnectivityCondition condition, SingleModeProfileItem profile) {
        profiles.put(condition, profile);
    }

    @NonNull
    private SingleModeProfileItem getDefaultProfile() {
        for (ConnectivityCondition cond : profiles.keySet()) {
            if (profiles.get(cond).isDefault) return profiles.get(cond);
        }
        return profiles.values().toArray(new SingleModeProfileItem[profiles.size()])[0];
    }

    @Nullable
    private SingleModeProfileItem getWiFiProfile(String ssid) {
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        for (ConnectivityCondition cond : profiles.keySet()) {
            if (cond.getType() == ConnectivityCondition.TYPE.WIFI && cond.getSSID().equals(ssid))
                return profiles.get(cond);
        }
        return null;
    }

    @Nullable
    private SingleModeProfileItem getMobileProfile() {
        for (ConnectivityCondition cond : profiles.keySet()) {
            if (cond.getType() == ConnectivityCondition.TYPE.MOBILE) return profiles.get(cond);
        }
        return null;
    }

    @Nullable
    private SingleModeProfileItem getBluetoothProfile() {
        for (ConnectivityCondition cond : profiles.keySet()) {
            if (cond.getType() == ConnectivityCondition.TYPE.BLUETOOTH) return profiles.get(cond);
        }
        return null;
    }

    @Nullable
    private SingleModeProfileItem getEthernetProfile() {
        for (ConnectivityCondition cond : profiles.keySet()) {
            if (cond.getType() == ConnectivityCondition.TYPE.BLUETOOTH) return profiles.get(cond);
        }
        return null;
    }

    public String getGlobalProfileName() {
        return globalProfileName;
    }

    public Map<ConnectivityCondition, SingleModeProfileItem> getProfiles() {
        return profiles;
    }

    public SingleModeProfileItem getCurrentProfile(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        SingleModeProfileItem item = null;
        if (manager.getActiveNetworkInfo() == null) return getDefaultProfile();
        switch (manager.getActiveNetworkInfo().getType()) {
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_WIFI:
                item = getWiFiProfile(((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID());
                break;
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE:
                item = getMobileProfile();
                break;
            case ConnectivityManager.TYPE_ETHERNET:
                item = getEthernetProfile();
                break;
            case ConnectivityManager.TYPE_BLUETOOTH:
                item = getBluetoothProfile();
                break;
        }

        if (item == null) return getDefaultProfile();

        return item;
    }
}
