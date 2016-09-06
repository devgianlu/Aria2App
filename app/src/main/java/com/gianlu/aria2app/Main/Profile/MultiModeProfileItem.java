package com.gianlu.aria2app.Main.Profile;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.ArrayMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class MultiModeProfileItem extends ProfileItem implements Parcelable {
    public static final Creator<MultiModeProfileItem> CREATOR = new Creator<MultiModeProfileItem>() {
        @Override
        public MultiModeProfileItem createFromParcel(Parcel in) {
            return new MultiModeProfileItem(in);
        }

        @Override
        public MultiModeProfileItem[] newArray(int size) {
            return new MultiModeProfileItem[size];
        }
    };
    private Map<ConnectivityCondition, SingleModeProfileItem> profiles = new ArrayMap<>();

    private MultiModeProfileItem() {
        this.singleMode = false;
        this.status = ProfileItem.STATUS.UNKNOWN;
    }

    protected MultiModeProfileItem(Parcel in) {
        super(in);
    }

    public static MultiModeProfileItem fromJSON(String fileName, String json) throws JSONException, IOException {
        JSONObject jProfile = new JSONObject(json);
        MultiModeProfileItem item = new MultiModeProfileItem();
        item.fileName = fileName;
        item.globalProfileName = jProfile.getString("name");

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

    public static MultiModeProfileItem fromString(Context context, String base64name) throws IOException, JSONException {
        FileInputStream in = context.openFileInput(base64name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return fromJSON(base64name, builder.toString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void addProfile(ConnectivityCondition condition, SingleModeProfileItem profile) {
        profiles.put(condition, profile);
    }

    @NonNull
    public SingleModeProfileItem getDefaultProfile() {
        for (ConnectivityCondition cond : profiles.keySet()) {
            if (profiles.get(cond).isDefault()) return profiles.get(cond);
        }
        return profiles.values().toArray(new SingleModeProfileItem[profiles.size()])[0];
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
        }

        if (item == null) return getDefaultProfile();

        return item;
    }
}
