package com.gianlu.aria2app;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.AlertDialog;

import com.gianlu.aria2app.Options.Parser;

public class MainSettingsActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_pref);

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
                    Utils.UIToast(MainSettingsActivity.this, Utils.TOAST_MESSAGES.NO_EMAIL_CLIENT);
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
                startActivity(new Intent(MainSettingsActivity.this, LogsActivity.class));
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
                builder.setTitle("nv-websocket-client")
                        .setMessage(R.string.nv_websocket_client_license)
                        .create().show();
                return true;
            }
        });

        findPreference("mpAndroidChart").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                builder.setTitle("MPAndroidChart")
                        .setMessage(R.string.mpAndroidChart_details)
                        .create().show();
                return true;
            }
        });

        findPreference("apacheLicense").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.apache.org/licenses/LICENSE-2.0")));
                return true;
            }
        });

        findPreference("updateOptions").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final ProgressDialog pd = Utils.fastProgressDialog(MainSettingsActivity.this, R.string.gathering_information, true, false);

                new Parser().refreshSource(MainSettingsActivity.this, new Parser.ISourceProcessor() {
                    @Override
                    public void onStarted() {
                        MainSettingsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.show();
                            }
                        });
                    }

                    @Override
                    public void onDownloadEnded(String source) {
                        MainSettingsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.setMessage(getString(R.string.processing_data));
                            }
                        });
                    }

                    @Override
                    public void onConnectionError(int code, String message) {
                        pd.dismiss();
                        Utils.UIToast(MainSettingsActivity.this, Utils.TOAST_MESSAGES.CANT_REFRESH_SOURCE, code + ": " + message);
                    }

                    @Override
                    public void onError(Exception ex) {
                        pd.dismiss();
                        Utils.UIToast(MainSettingsActivity.this, Utils.TOAST_MESSAGES.CANT_REFRESH_SOURCE, ex);
                    }

                    @Override
                    public void onFailed() {
                        Utils.UIToast(MainSettingsActivity.this, Utils.TOAST_MESSAGES.CANT_REFRESH_SOURCE);
                    }

                    @Override
                    public void onEnd() {
                        pd.dismiss();
                        Utils.UIToast(MainSettingsActivity.this, Utils.TOAST_MESSAGES.SOURCE_REFRESHED);
                    }
                });
                return true;
            }
        });
    }
}
