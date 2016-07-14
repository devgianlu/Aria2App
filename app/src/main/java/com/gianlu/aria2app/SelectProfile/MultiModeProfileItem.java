package com.gianlu.aria2app.SelectProfile;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.ArrayMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class MultiModeProfileItem extends ProfileItem {
    private Map<ConnectivityCondition, SingleModeProfileItem> profiles = new ArrayMap<>();

    private MultiModeProfileItem() {
        this.singleMode = false;
        this.status = ProfileItem.STATUS.UNKNOWN;
    }

    public static MultiModeProfileItem fromJSON(String json) throws JSONException, IOException {
        JSONObject jProfile = new JSONObject(json);
        MultiModeProfileItem item = new MultiModeProfileItem();
        item.profileName = jProfile.getString("name");

        JSONArray conditions = jProfile.getJSONArray("conditions");

        for (int i = 0; i < conditions.length(); i++) {
            JSONObject condition = conditions.getJSONObject(i);

            SingleModeProfileItem profile = SingleModeProfileItem.fromJSON(condition.getJSONObject("profile").toString());
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

    public static MultiModeProfileItem fromString(Context context, String name) throws IOException, JSONException {
        return fromFile(context, new File(name + ".profile"));
    }

    public static MultiModeProfileItem fromFile(Context context, File file) throws IOException, JSONException {
        FileInputStream in = context.openFileInput(file.getName());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return fromJSON(builder.toString());
    }

    public void addProfile(ConnectivityCondition condition, SingleModeProfileItem profile) {
        profiles.put(condition, profile);
    }

    @NonNull
    public SingleModeProfileItem getDefaultProfile() {
        for (ConnectivityCondition cond : profiles.keySet()) {
            if (profiles.get(cond).isDefault()) return profiles.get(cond);
        }
        return new SingleModeProfileItem("Default", "127.0.0.1", 6800, "/jsonrpc", false, false, "", false, null);
    }

    @Nullable
    public SingleModeProfileItem getWiFiProfile(String ssid) {
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
    public SingleModeProfileItem getMobileProfile() {
        for (ConnectivityCondition cond : profiles.keySet()) {
            if (cond.getType() == ConnectivityCondition.TYPE.MOBILE) return profiles.get(cond);
        }
        return null;
    }

    @Nullable
    public SingleModeProfileItem getBluetoothProfile() {
        for (ConnectivityCondition cond : profiles.keySet()) {
            if (cond.getType() == ConnectivityCondition.TYPE.BLUETOOTH) return profiles.get(cond);
        }
        return null;
    }

    @Nullable
    public SingleModeProfileItem getEthernetProfile() {
        for (ConnectivityCondition cond : profiles.keySet()) {
            if (cond.getType() == ConnectivityCondition.TYPE.BLUETOOTH) return profiles.get(cond);
        }
        return null;
    }

    public Map<ConnectivityCondition, SingleModeProfileItem> getProfiles() {
        return profiles;
    }

    public SingleModeProfileItem getCurrentProfile(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        SingleModeProfileItem item;
        if (manager.getActiveNetworkInfo() == null) return getDefaultProfile();
        switch (manager.getActiveNetworkInfo().getType()) {
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_WIFI:
                item = getWiFiProfile(((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID());
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
            default:
                item = null;
                break;
        }

        if (item == null) {
            item = getDefaultProfile();
        }

        return item;
    }
}
