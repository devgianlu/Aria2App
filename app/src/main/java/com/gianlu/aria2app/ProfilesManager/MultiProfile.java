package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;

import com.gianlu.commonutils.Drawer.BaseDrawerProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class MultiProfile extends BaseProfile implements BaseDrawerProfile, Serializable {
    private final Map<ConnectivityCondition, UserProfile> profiles;

    MultiProfile(JSONObject obj) throws JSONException {
        super(obj);

        JSONArray profilesArray = obj.getJSONArray("conditions");
        profiles = new HashMap<>();
        for (int i = 0; i < profilesArray.length(); i++) {
            JSONObject conditionObj = profilesArray.getJSONObject(i);
            ConnectivityCondition.Type type = ConnectivityCondition.Type.valueOf(conditionObj.getString("type").toUpperCase());

            boolean isDefault; // Needed for backward compatibility
            if (conditionObj.has("isDefault")) isDefault = conditionObj.getBoolean("isDefault");
            else isDefault = conditionObj.getJSONObject("profile").getBoolean("default");

            ConnectivityCondition condition;
            switch (type) {
                case WIFI:
                    condition = ConnectivityCondition.newWiFiCondition(conditionObj.getString("ssid"), isDefault);
                    break;
                case MOBILE:
                    condition = ConnectivityCondition.newMobileCondition(isDefault);
                    break;
                case ETHERNET:
                    condition = ConnectivityCondition.newEthernetCondition(isDefault);
                    break;
                case BLUETOOTH:
                    condition = ConnectivityCondition.newBluetoothCondition(isDefault);
                    break;
                default:
                case UNKNOWN:
                    continue;
            }

            profiles.put(condition, new UserProfile(conditionObj.getJSONObject("profile")));
        }

        if (profiles.isEmpty()) {
            // TODO: Must get angry here
        }
    }

    private UserProfile getDefaultProfile() {
        for (Map.Entry<ConnectivityCondition, UserProfile> entry : profiles.entrySet())
            if (entry.getKey().isDefault)
                return entry.getValue();

        return profiles.values().iterator().next();
    }

    @Nullable
    private UserProfile findFor(ConnectivityCondition.Type type) {
        for (Map.Entry<ConnectivityCondition, UserProfile> entry : profiles.entrySet())
            if (entry.getKey().type == type)
                return entry.getValue();

        return null;
    }

    @Nullable
    private UserProfile findForWifi(String ssid) {
        for (Map.Entry<ConnectivityCondition, UserProfile> entry : profiles.entrySet())
            if (entry.getKey().type == ConnectivityCondition.Type.WIFI && Objects.equals(entry.getKey().ssid, ssid))
                return entry.getValue();

        return null;
    }

    @Override
    public UserProfile getProfile(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (connManager.getActiveNetworkInfo() == null) return getDefaultProfile();

        UserProfile profile = null;
        switch (connManager.getActiveNetworkInfo().getType()) {
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_WIFI:
                profile = findForWifi(wifiManager.getConnectionInfo().getSSID());
                break;
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE:
                profile = findFor(ConnectivityCondition.Type.MOBILE);
                break;
            case ConnectivityManager.TYPE_ETHERNET:
                profile = findFor(ConnectivityCondition.Type.ETHERNET);
                break;
            case ConnectivityManager.TYPE_BLUETOOTH:
                profile = findFor(ConnectivityCondition.Type.BLUETOOTH);
                break;
        }

        if (profile == null) return getDefaultProfile();
        else return profile;
    }

    @Override
    public String getProfileName(Context context) {
        return getProfile(context).getProfileName(context);
    }

    @Override
    public String getSecondaryText(Context context) {
        return getProfile(context).getSecondaryText(context);
    }

    @Override
    public String getInitials(Context context) {
        return getProfile(context).getInitials(context);
    }
}
