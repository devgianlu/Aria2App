package com.gianlu.aria2app.Google.Billing;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Billing {
    public static final int RESULT_BILLING_UNAVAILABLE = 3;
    private static final ArrayList<String> donationProducts = new ArrayList<>(Arrays.asList(
            "donation.lemonade",
            "donation.coffee",
            "donation.hamburger",
            "donation.pizza",
            "donation.sushi",
            "donation.champagne"
    ));
    private static final int RESULT_OK = 0;
    private static final int RESULT_USER_CANCELED = 1;

    @NonNull
    private static String generateRandomString() {
        StringBuilder builder = new StringBuilder();
        String letters = "abcdefghijklmnopqrstuvwxyz";

        for (int i = 0; i <= 30; i++) {
            if (new Random().nextBoolean()) {
                builder.append(String.valueOf(new Random().nextInt(10)));
            } else {
                builder.append(letters.charAt(new Random().nextInt(26)));
            }
        }

        return builder.toString();
    }

    public static void requestProductsDetails(final Context context, final IInAppBillingService service, @NonNull final IRequestProductDetails handler) {
        if (service == null) {
            handler.onFailed(new NullPointerException("IInAppBillingService is null"));
            return;
        }

        final Bundle bundle = new Bundle();
        bundle.putStringArrayList("ITEM_ID_LIST", Billing.donationProducts);

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

                int respCode = response.getInt("RESPONSE_CODE");
                if (respCode == RESULT_OK) {
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

                    handler.onReceivedDetails(handler, products);
                } else if (respCode == RESULT_USER_CANCELED) {
                    handler.onUserCancelled();
                } else {
                    handler.onAPIException(respCode);
                }
            }
        }).start();
    }

    public static void buyProduct(final Context context, final IInAppBillingService service, @NonNull final Product product, @NonNull final IBuyProduct handler) {
        if (service == null) {
            handler.onFailed(new NullPointerException("IInAppBillingService is null"));
            return;
        }

        final String developerString = generateRandomString();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle response;
                try {
                    response = service.getBuyIntent(3, context.getApplicationContext().getPackageName(), product.productId, product.type, developerString);
                } catch (RemoteException ex) {
                    handler.onFailed(ex);
                    return;
                }

                if (response == null) {
                    handler.onFailed(new NullPointerException("Response bundle is null"));
                    return;
                }

                int respCode = response.getInt("RESPONSE_CODE");
                if (respCode == RESULT_OK) {
                    handler.onGotIntent((PendingIntent) response.getParcelable("BUY_INTENT"), developerString);
                } else if (respCode == RESULT_USER_CANCELED) {
                    handler.onUserCancelled();
                } else {
                    handler.onAPIException(respCode);
                }
            }
        }).start();
    }

    public interface IRequestProductDetails {
        void onReceivedDetails(IRequestProductDetails handler, List<Product> products);

        void onAPIException(int code);

        void onUserCancelled();

        void onFailed(Exception ex);
    }

    public interface IBuyProduct {
        void onGotIntent(PendingIntent intent, String developerString);

        void onAPIException(int code);

        void onUserCancelled();

        void onFailed(Exception ex);
    }
}
