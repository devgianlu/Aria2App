package com.gianlu.aria2app.Google.Billing;

import org.json.JSONException;
import org.json.JSONObject;

public class Product {
    public String productId;
    public String price;
    public String title;
    public String type;
    public String description;

    public Product(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        productId = obj.getString("productId");
        price = obj.getString("price");
        title = obj.getString("title");
        type = obj.getString("type");
        description = obj.getString("description");
    }
}
