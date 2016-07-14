package com.gianlu.jtitan.JTRequester;

import org.json.JSONException;
import org.json.JSONObject;

public class JTResponse {
    private String resp;
    private Integer respCode;
    private Exception ex;

    public JTResponse() {
    }

    public JTResponse(Integer responseCode) {
        respCode = responseCode;
        resp = null;
        ex = null;
    }

    public JTResponse(Integer responseCode, Exception exception) {
        respCode = responseCode;
        ex = exception;
        resp = null;
    }

    public JTResponse(Integer responseCode, String response) {
        respCode = responseCode;
        resp = response;
        ex = null;
    }

    @Override
    public String toString() {
        return resp;
    }

    public JSONObject toJSON() {
        JSONObject respJSON;
        try {
            respJSON = new JSONObject(resp);
        } catch (JSONException ex) {
            //Error #1101
            respJSON = null;
        }

        return respJSON;
    }

    public Integer Code() {
        return respCode;
    }

    public Exception Exception() {
        return ex;
    }
}
