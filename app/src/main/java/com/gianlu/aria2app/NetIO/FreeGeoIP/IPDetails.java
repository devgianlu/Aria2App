package com.gianlu.aria2app.NetIO.FreeGeoIP;

import org.json.JSONException;
import org.json.JSONObject;

public class IPDetails {
    public final String ip;
    public final float latitude;
    public final float longitude;
    public final String countryCode;
    public final String countryName;
    public final String regionCode;
    public final String regionName;
    public final String city;
    public final int zipCode;
    public final String timeZone;

    public IPDetails(JSONObject obj) throws JSONException {
        ip = obj.getString("ip");
        latitude = (float) obj.getDouble("latitude");
        longitude = (float) obj.getDouble("longitude");
        countryCode = obj.getString("country_code");
        countryName = obj.getString("country_name");
        regionCode = obj.getString("region_code");
        regionName = obj.getString("region_name");
        city = obj.getString("city");
        zipCode = obj.optInt("zip_code", 0);
        timeZone = obj.getString("time_zone");
    }

    public String getNiceLocalizationString() {
        StringBuilder builder = new StringBuilder();
        if (!city.isEmpty())
            builder.append(city).append(", ");

        if (!regionName.isEmpty() && !regionCode.isEmpty())
            builder.append(regionName).append(" (").append(regionCode).append("), ");

        builder.append(countryName).append(" (").append(countryCode).append(")");
        return builder.toString();
    }
}
