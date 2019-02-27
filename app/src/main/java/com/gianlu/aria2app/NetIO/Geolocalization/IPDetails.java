package com.gianlu.aria2app.NetIO.Geolocalization;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IPDetails {
    public final String countryCode;
    public final String ip;
    public final String domain;
    private final String country;
    private final String continent;
    private final String continentCode;
    private final String city;

    public IPDetails(JSONObject obj) throws JSONException {
        ip = obj.getString("ip");
        domain = parseStupidNull(obj, "domain");
        country = obj.getString("country");
        countryCode = obj.getString("countryCode");
        continent = obj.getString("continent");
        continentCode = obj.getString("continentCode");
        city = parseStupidNull(obj, "city");
    }

    @Nullable
    private static String parseStupidNull(@NonNull JSONObject obj, @NonNull String key) {
        String str = obj.optString(key, null);
        if (Objects.equals(str, "null")) return null;
        else return str;
    }

    @NonNull
    public String getNiceLocalizationString() {
        StringBuilder builder = new StringBuilder();
        if (city != null && !city.isEmpty())
            builder.append(city).append(", ");

        if (country != null && !country.isEmpty())
            builder.append(country).append(" (").append(countryCode).append(")").append(", ");

        if (continent != null && !continent.isEmpty())
            builder.append(continent).append(" (").append(continentCode).append(")");

        return builder.toString();
    }
}
