package com.gianlu.aria2app.NetIO.Geolocalization;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class IPDetails {
    public final String countryCode;
    private final String countryName;
    private final String regionName;
    private final String city;
    private final String ipName;
    private final String query;
    private final String isp;
    private final String org;

    public IPDetails(JSONObject obj) throws JSONException {
        ipName = obj.getString("ipName");
        query = obj.getString("query");
        countryCode = obj.getString("countryCode");
        countryName = obj.getString("country");
        regionName = obj.getString("region");
        city = obj.getString("city");
        isp = obj.getString("isp");
        org = obj.getString("org");
    }

    @NonNull
    public String getIsp() {
        return isp.isEmpty() ? org : isp;
    }

    @NonNull
    public String getIp() {
        return ipName.isEmpty() ? query : ipName;
    }

    @NonNull
    public String getNiceLocalizationString() {
        StringBuilder builder = new StringBuilder();
        if (!city.isEmpty()) builder.append(city).append(", ");
        if (!regionName.isEmpty()) builder.append(regionName).append(", ");
        builder.append(countryName).append(" (").append(countryCode).append(")");
        return builder.toString();
    }
}
