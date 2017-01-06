package com.gianlu.aria2app;

import android.app.PendingIntent;
import android.app.ProgressDialog;
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
import com.gianlu.aria2app.Google.Billing.Billing;
import com.gianlu.aria2app.Google.Billing.Product;
import com.gianlu.aria2app.Google.Billing.ProductAdapter;
import com.gianlu.aria2app.Google.Billing.PurchasedProduct;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.LogsActivity;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class PreferencesActivity extends PreferenceActivity {
    private IInAppBillingService billingService;
    private ProgressDialog pd;
    private int requestCode;
    private String devString;
    private ServiceConnection serviceConnection;

    @Override
    protected void onStart() {
        super.onStart();

        if (billingService == null) {
            serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceDisconnected(ComponentName name) {
                    billingService = null;
                }

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    billingService = IInAppBillingService.Stub.asInterface(service);
                    if (pd != null && pd.isShowing()) {
                        donate();
                    }
                }
            };

            bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND").setPackage("com.android.vending"),
                    serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_pref);
        pd = CommonUtils.fastIndeterminateProgressDialog(this, R.string.connectingBillingService);

        findPreference("email").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                CommonUtils.sendEmail(PreferencesActivity.this, getString(R.string.app_name));
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

        findPreference("dd_downloadPath").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                File path = new File((String) o);
                if (!path.exists() || !path.isDirectory()) {
                    CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.INVALID_DOWNLOAD_PATH, (String) o);
                }

                return false;
            }
        });

        findPreference("fileDownloader").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                CommonUtils.showDialog(PreferencesActivity.this, builder
                        .setTitle("FileDownloader")
                        .setMessage(R.string.fileDownloader_details));
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
        CommonUtils.showDialog(this, pd);
        if (billingService == null)
            return;

        Billing.requestProductsDetails(this, billingService, new Billing.IRequestProductDetails() {
            @Override
            public void onReceivedDetails(final Billing.IRequestProductDetails handler, final List<Product> products) {
                final Billing.IBuyProduct buyHandler = new Billing.IBuyProduct() {
                    @Override
                    public void onGotIntent(PendingIntent intent, String developerString) {
                        devString = developerString;
                        requestCode = new Random().nextInt();

                        try {
                            PreferencesActivity.this.startIntentSenderForResult(intent.getIntentSender(), requestCode, new Intent(), 0, 0, 0);
                        } catch (IntentSender.SendIntentException ex) {
                            CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.FAILED_CONNECTION_BILLING_SERVICE, ex);
                        }
                    }

                    @Override
                    public void onAPIException(int code) {
                        handler.onAPIException(code);
                    }

                    @Override
                    public void onUserCancelled() {
                        handler.onUserCancelled();
                    }

                    @Override
                    public void onFailed(Exception ex) {
                        CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.FAILED_CONNECTION_BILLING_SERVICE, ex);
                    }
                };
                pd.dismiss();

                CommonUtils.showDialog(PreferencesActivity.this, new AlertDialog.Builder(PreferencesActivity.this)
                        .setTitle(getString(R.string.donate))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setAdapter(new ProductAdapter(PreferencesActivity.this, products, new ProductAdapter.IAdapter() {
                            @Override
                            public void onItemSelected(Product product) {
                                Billing.buyProduct(PreferencesActivity.this, billingService, product, buyHandler);
                            }
                        }), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Billing.buyProduct(PreferencesActivity.this, billingService, products.get(i), buyHandler);
                            }
                        }));

                ThisApplication.sendAnalytics(PreferencesActivity.this, new HitBuilders.EventBuilder()
                        .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                        .setAction(ThisApplication.ACTION_DONATE_OPEN)
                            .build());
            }

            @Override
            public void onAPIException(int code) {
                if (code == Billing.RESULT_BILLING_UNAVAILABLE)
                    CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.FAILED_CONNECTION_BILLING_SERVICE, "Code: " + code);
                else
                    CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.FAILED_BUYING_ITEM, "Code: " + code);
            }

            @Override
            public void onUserCancelled() {
                CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.BILLING_USER_CANCELLED);
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
                try {
                    PurchasedProduct purchasedProduct = new PurchasedProduct(data.getStringExtra("INAPP_PURCHASE_DATA"));
                    if (Objects.equals(purchasedProduct.developerPayload, devString)) {
                        if (purchasedProduct.purchaseState == PurchasedProduct.PURCHASED) {
                            CommonUtils.UIToast(this, Utils.ToastMessages.THANK_YOU, "Purchased " + purchasedProduct.productId + " with order ID " + purchasedProduct.orderId);
                        } else if (purchasedProduct.purchaseState == PurchasedProduct.CANCELED) {
                            CommonUtils.UIToast(this, Utils.ToastMessages.PURCHASING_CANCELED);
                        }
                    } else {
                        CommonUtils.UIToast(this, Utils.ToastMessages.FAILED_BUYING_ITEM, new Exception("Payloads mismatch!"));
                    }
                } catch (JSONException ex) {
                    CommonUtils.UIToast(this, Utils.ToastMessages.FAILED_BUYING_ITEM, ex);
                }
            } else {
                CommonUtils.UIToast(this, Utils.ToastMessages.PURCHASING_CANCELED);
            }
        }

    }
}
