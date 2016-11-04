package com.gianlu.aria2app;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.AlertDialog;

import com.android.vending.billing.IInAppBillingService;
import com.gianlu.aria2app.Google.Billing;
import com.gianlu.aria2app.Google.Product;
import com.gianlu.aria2app.Google.PurchasedProduct;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.LogsActivity;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PreferencesActivity extends PreferenceActivity {
    private IInAppBillingService billingService;
    private int requestCode;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            billingService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            billingService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_pref);

        bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND")
                .setPackage("com.android.vending"), serviceConnection, Context.BIND_AUTO_CREATE);

        findPreference("email").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email)});
                i.putExtra(Intent.EXTRA_SUBJECT, "Aria2App");
                i.putExtra(Intent.EXTRA_TEXT, "OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")" +
                        "\nOS API Level: " + android.os.Build.VERSION.SDK_INT +
                        "\nDevice: " + android.os.Build.DEVICE +
                        "\nModel (and Product): " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")");
                try {
                    startActivity(Intent.createChooser(i, "Send mail to the developer..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.NO_EMAIL_CLIENT);
                }
                return true;
            }
        });

        try {
            findPreference("app_version").setSummary(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException ex) {
            findPreference("app_version").setSummary(R.string.unknown);
        }

        findPreference("logs").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(PreferencesActivity.this, LogsActivity.class));
                return true;
            }
        });

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        findPreference("nv-websocket-client").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                CommonUtils.showDialog(PreferencesActivity.this, builder
                        .setTitle("nv-websocket-client")
                        .setMessage(R.string.nv_websocket_client_license));
                return true;
            }
        });

        findPreference("mpAndroidChart").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                CommonUtils.showDialog(PreferencesActivity.this, builder
                        .setTitle("MPAndroidChart")
                        .setMessage(R.string.mpAndroidChart_details));
                return true;
            }
        });

        findPreference("apacheLicense").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.apache.org/licenses/LICENSE-2.0")));
                return true;
            }
        });

        findPreference("donate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                donate();
                return true;
            }
        });
    }

    private void donate() {
        Billing.requestProductsDetails(this, billingService, new ArrayList<>(Arrays.asList("", "")), new Billing.IRequestProductDetails() {
            @Override
            public void onReceivedDetails(List<Product> products) {
                // TODO: Dialog to choose the product

                Billing.buyProduct(PreferencesActivity.this, billingService, products.get(0), new Billing.IBuyProduct() {
                    @Override
                    public void onGotIntent(PendingIntent intent) {
                        try {
                            requestCode = new Random().nextInt();
                            PreferencesActivity.this.startIntentSenderForResult(intent.getIntentSender(), requestCode, new Intent(), 0, 0, 0);
                        } catch (IntentSender.SendIntentException ex) {
                            CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.FAILED_CONNECTION_BILLING_SERVICE, ex);
                        }
                    }

                    @Override
                    public void onFailed(Exception ex) {
                        CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.FAILED_CONNECTION_BILLING_SERVICE, ex);
                    }
                });
            }

            @Override
            public void onFailed(Exception ex) {
                CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.FAILED_CONNECTION_BILLING_SERVICE, ex);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingService != null)
            unbindService(serviceConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == this.requestCode) {
            if (data.getIntExtra("RESPONSE_CODE", RESULT_CANCELED) == RESULT_OK) {
                String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE"); // TODO: Do I need this?

                try {
                    PurchasedProduct purchasedProduct = new PurchasedProduct(data.getStringExtra("INAPP_PURCHASE_DATA"));
                    if (purchasedProduct.purchaseState == PurchasedProduct.PURCHASED) {
                        switch (purchasedProduct.productId) {
                            // TODO
                        }

                        CommonUtils.logMe(this, "Purchased " + purchasedProduct.productId + " with order ID " + purchasedProduct.orderId, false);
                    } else if (purchasedProduct.purchaseState == PurchasedProduct.CANCELED) {
                        CommonUtils.UIToast(this, Utils.ToastMessages.PURCHASING_CANCELED);
                    }
                } catch (JSONException ex) {
                    CommonUtils.UIToast(this, Utils.ToastMessages.FAILED_BUYING_ITEM, ex);
                }
            }
        }

    }
}
