package com.gianlu.aria2app.NetIO.Aria2;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class VersionInfo {
    public final String version;
    public final List<String> enabledFeatures;

    public VersionInfo(JSONObject obj) throws JSONException {
        version = obj.getString("version");
        enabledFeatures = CommonUtils.toStringsList(obj.getJSONArray("enabledFeatures"), false);
    }
}
