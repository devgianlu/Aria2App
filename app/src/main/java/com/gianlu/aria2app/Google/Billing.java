package com.gianlu.aria2app.Google;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class Billing {
    public static final int RESULT_OK = 0;
    public static final int RESULT_USER_CANCELED = 1;
    public static final int RESULT_BILLING_UNAVAILABLE = 3;
    public static final int RESULT_ITEM_UNAVAILABLE = 4;
    public static final int RESULT_DEVELOPER_ERROR = 5;
    public static final int RESULT_ERROR = 6;
    public static final int RESULT_ITEM_ALREADY_OWNED = 7;
    public static final int RESULT_ITEM_NOT_OWNED = 8;

    public static void requestProductsDetails(final Context context, final IInAppBillingService service, @NonNull ArrayList<String> products, @NonNull final IRequestProductDetails handler) {
        if (service == null) {
            handler.onFailed(new NullPointerException("IInAppBillingService is null"));
            return;
        }

        if (products.isEmpty()) {
            handler.onReceivedDetails(new ArrayList<Product>());
            return;
        }

        final Bundle bundle = new Bundle();
        bundle.putStringArrayList("ITEM_ID_LIST", products);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle response;
                try {
                    response = service.getSkuDetails(3, context.getApplicationContext().getPackageName(), "inapp", bundle);
                } catch (RemoteException ex) {
                    handler.onFailed(ex);
                    return;
                }

                if (response.getInt("RESPONSE_CODE") == RESULT_OK) {
                    List<String> jResponse = response.getStringArrayList("DETAILS_LIST");

                    if (jResponse == null) {
                        handler.onFailed(new NullPointerException("Response bundle is null"));
                        return;
                    }

                    List<Product> products = new ArrayList<>();
                    for (String productDetails : jResponse) {
                        try {
                            products.add(new Product(productDetails));
                        } catch (JSONException ignored) {
                        }
                    }

                    handler.onReceivedDetails(products);
                } else {
                    // TODO: Handle all possible error codes
                }
            }
        }).start();
    }

    public static void buyProduct(final Context context, final IInAppBillingService service, @NonNull final Product product, @NonNull final IBuyProduct handler) {
        if (service == null) {
            handler.onFailed(new NullPointerException("IInAppBillingService is null"));
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle response;
                try {
                    response = service.getBuyIntent(3, context.getApplicationContext().getPackageName(), product.productId, product.type, "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ"); // TODO: Developer string
                } catch (RemoteException ex) {
                    handler.onFailed(ex);
                    return;
                }

                if (response == null) {
                    handler.onFailed(new NullPointerException("Response bundle is null"));
                    return;
                }

                if (response.getInt("RESPONSE_CODE") == RESULT_OK) {
                    handler.onGotIntent((PendingIntent) response.getParcelable("BUY_INTENT"));
                } else {
                    // TODO: Handle all possible error codes
                }
            }
        }).start();
    }

    public interface IRequestProductDetails {
        void onReceivedDetails(List<Product> products);

        void onFailed(Exception ex);
    }

    public interface IBuyProduct {
        void onGotIntent(PendingIntent intent);

        void onFailed(Exception ex);
    }
}
