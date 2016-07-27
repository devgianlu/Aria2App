package com.gianlu.jtitan.Aria2Helper;

import org.json.JSONArray;

public class A2Params extends JSONArray {
    public A2Params() {
        if (JTA2.useToken)
            this.put("token:" + JTA2.authToken);
    }
}
