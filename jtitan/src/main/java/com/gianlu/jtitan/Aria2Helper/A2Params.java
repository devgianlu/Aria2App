package com.gianlu.jtitan.Aria2Helper;

import org.json.JSONArray;

public class A2Params extends JSONArray {
    A2Params() {
        if (JTA2.authNeeded) this.put("token:" + JTA2.authToken);
    }
}
