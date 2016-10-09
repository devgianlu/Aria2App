package com.gianlu.aria2app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.AlertDialog;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.LogsActivity;

public class PreferencesActivity extends PreferenceActivity {

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
    }
}
