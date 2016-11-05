package com.gianlu.aria2app.Google.Billing;

import org.json.JSONException;
import org.json.JSONObject;

public class PurchasedProduct {
    public static final int PURCHASED = 0;
    public static final int CANCELED = 1;

    public String orderId;
    public String productId;
    public int purchaseState;
    public String developerPayload;

    public PurchasedProduct(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        orderId = obj.getString("orderId");
        productId = obj.getString("productId");
        purchaseState = obj.getInt("purchaseState");
        developerPayload = obj.getString("developerPayload");
    }
}
