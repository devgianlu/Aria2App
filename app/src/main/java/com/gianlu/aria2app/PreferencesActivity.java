package com.gianlu.aria2app;

import android.content.Intent;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;

import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.commonutils.LogsActivity;
import com.gianlu.commonutils.Preferences.AppCompatPreferenceActivity;
import com.gianlu.commonutils.Preferences.AppCompatPreferenceFragment;
import com.gianlu.commonutils.Preferences.BaseAboutFragment;
import com.gianlu.commonutils.Preferences.BaseThirdPartProjectsFragment;
import com.gianlu.commonutils.Toaster;

import java.io.File;
import java.util.List;

public class PreferencesActivity extends AppCompatPreferenceActivity {

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

    public static class GeneralFragment extends AppCompatPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.general_pref);
            getActivity().setTitle(R.string.general);
            setHasOptionsMenu(true);

            MultiSelectListPreference customInfo = ((MultiSelectListPreference) findPreference(PKeys.A2_CUSTOM_INFO.getKey()));
            customInfo.setEntryValues(CustomDownloadInfo.Info.stringValues());
            customInfo.setEntries(CustomDownloadInfo.Info.formalValues(getActivity()));

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

            findPreference(PKeys.DD_DOWNLOAD_PATH.getKey()).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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

            findPreference(PKeys.DD_MAX_SIMULTANEOUS_DOWNLOADS.getKey()).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Integer val = Integer.parseInt((String) newValue);
                    if (val > 10 || val <= 0) {
                        Toaster.show(getActivity(), Utils.Messages.INVALID_MAX_SIMULTANEOUS_DOWNLOADS, String.valueOf(val));
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

            findPreference(PKeys.A2_ENABLE_NOTIFS.getKey()).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) NotificationService.start(getActivity(), false);
                    else NotificationService.stop(getActivity());
                    return true;
                }
            });
        }

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }

    public static class ThirdPartFragment extends BaseThirdPartProjectsFragment {

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }

        @NonNull
        @Override
        protected ThirdPartProject[] getProjects() {
            return new ThirdPartProject[]{
                    new ThirdPartProject(R.string.mpAndroidChart, R.string.mpAndroidChart_details, ThirdPartProject.License.APACHE),
                    new ThirdPartProject(R.string.tapTargetView, R.string.tapTargetView_details, ThirdPartProject.License.APACHE),
                    new ThirdPartProject(R.string.flowLayout, R.string.flowLayout_details, ThirdPartProject.License.APACHE)
            };
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
