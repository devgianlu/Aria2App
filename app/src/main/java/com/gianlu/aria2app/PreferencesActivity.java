package com.gianlu.aria2app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

import com.gianlu.commonutils.AppCompatPreferenceActivity;
import com.gianlu.commonutils.AppCompatPreferenceFragment;
import com.gianlu.commonutils.BaseAboutFragment;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.LogsActivity;
import com.gianlu.commonutils.Toaster;

import java.io.File;
import java.util.List;

public class PreferencesActivity extends AppCompatPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.menu_preferences);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        if (header.iconRes == R.drawable.ic_announcement_black_24dp) {
            startActivity(new Intent(this, LogsActivity.class));
            return;
        }

        super.onHeaderClick(header, position);
    }

    protected boolean isValidFragment(String fragmentName) {
        return AppCompatPreferenceFragment.class.getName().equals(fragmentName)
                || GeneralFragment.class.getName().equals(fragmentName)
                || DirectDownloadFragment.class.getName().equals(fragmentName)
                || NotificationsFragment.class.getName().equals(fragmentName)
                || ThirdPartFragment.class.getName().equals(fragmentName)
                || AboutFragment.class.getName().equals(fragmentName);
    }

    public static class GeneralFragment extends AppCompatPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.general_pref);
            getActivity().setTitle(R.string.general);
            setHasOptionsMenu(true);

            findPreference("restartTutorial").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    TutorialManager.restartTutorial(getActivity());
                    NavUtils.navigateUpFromSameTask(getActivity());
                    return true;
                }
            });
        }

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }

    public static class DirectDownloadFragment extends AppCompatPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.direct_download_pref);
            getActivity().setTitle(R.string.directDownload);
            setHasOptionsMenu(true);

            findPreference("dd_downloadPath").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    File path = new File(((String) o).trim());
                    if (!path.exists() || !path.isDirectory()) {
                        Toaster.show(getActivity(), Utils.Messages.INVALID_DOWNLOAD_PATH, (String) o);
                        return false;
                    }

                    if (!path.canWrite()) {
                        Toaster.show(getActivity(), Utils.Messages.INVALID_DOWNLOAD_PATH, (String) o);
                        return false;
                    }

                    return true;
                }
            });
        }

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }

    public static class NotificationsFragment extends AppCompatPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.notifications_pref);
            getActivity().setTitle(R.string.notifications);
            setHasOptionsMenu(true);
        }

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }

    public static class ThirdPartFragment extends AppCompatPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.thrid_part_pref);
            getActivity().setTitle(R.string.third_part);
            setHasOptionsMenu(true);

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setPositiveButton(android.R.string.ok, null);

            findPreference("mpAndroidChart").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    CommonUtils.showDialog(getActivity(), builder
                            .setTitle("MPAndroidChart")
                            .setMessage(R.string.mpAndroidChart_details));
                    return true;
                }
            });

            findPreference("nv-websocket-client").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    CommonUtils.showDialog(getActivity(), builder
                            .setTitle("nv-websocket-client")
                            .setMessage(R.string.nv_websocket_client_license));
                    return true;
                }
            });

            findPreference("jsoup").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CommonUtils.showDialog(getActivity(), builder
                            .setTitle("jsoup")
                            .setMessage(R.string.jsoup_details));
                    return true;
                }
            });

            findPreference("tapTargetView").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CommonUtils.showDialog(getActivity(), builder
                            .setTitle("TapTargetView")
                            .setMessage(R.string.tapTargetView_details));
                    return true;
                }
            });

            findPreference("apacheLicense").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.apache.org/licenses/LICENSE-2.0")));
                    return true;
                }
            });

            findPreference("mitLicense").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://opensource.org/licenses/MIT")));
                    return true;
                }
            });
        }

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }

    public static class AboutFragment extends BaseAboutFragment {
        @Override
        protected int getAppNameRes() {
            return R.string.app_name;
        }

        @NonNull
        @Override
        protected String getPackageName() {
            return "com.gianlu.aria2app";
        }

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }
}
