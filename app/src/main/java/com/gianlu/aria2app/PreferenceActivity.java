package com.gianlu.aria2app;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.commonutils.Preferences.BasePreferenceActivity;
import com.gianlu.commonutils.Preferences.BasePreferenceFragment;
import com.gianlu.commonutils.Preferences.MaterialAboutPreferenceItem;
import com.yarolegovich.mp.AbsMaterialTextValuePreference;
import com.yarolegovich.mp.MaterialCheckboxPreference;
import com.yarolegovich.mp.MaterialEditTextPreference;
import com.yarolegovich.mp.MaterialMultiChoicePreference;
import com.yarolegovich.mp.MaterialSeekBarPreference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class PreferenceActivity extends BasePreferenceActivity {
    @NonNull
    @Override
    protected List<MaterialAboutPreferenceItem> getPreferencesItems() {
        return Arrays.asList(new MaterialAboutPreferenceItem(R.string.general, R.drawable.baseline_settings_24, GeneralFragment.class),
                new MaterialAboutPreferenceItem(R.string.directDownload, R.drawable.baseline_cloud_download_24, DirectDownloadFragment.class),
                new MaterialAboutPreferenceItem(R.string.notifications, R.drawable.baseline_sms_24, NotificationsFragment.class));
    }

    public static class GeneralFragment extends BasePreferenceFragment {

        @Override
        protected void buildPreferences(@NonNull Context context) {
            MaterialCheckboxPreference nightMode = new MaterialCheckboxPreference.Builder(context)
                    .defaultValue(false)
                    .key(PK.NIGHT_MODE.getKey())
                    .build();
            nightMode.setTitle(R.string.nightMode); // FIXME
            nightMode.setSummary(R.string.nightMode); // FIXME
            addPreference(nightMode);

            MaterialCheckboxPreference hideMetadata = new MaterialCheckboxPreference.Builder(context)
                    .defaultValue(false)
                    .key(PK.A2_HIDE_METADATA.getKey())
                    .build();
            hideMetadata.setTitle(R.string.prefs_hideMetadata);
            hideMetadata.setSummary(R.string.prefs_hideMetadata_summary);
            addPreference(hideMetadata);

            MaterialSeekBarPreference updateRate = new MaterialSeekBarPreference.Builder(context)
                    .minValue(1).maxValue(10).showValue(true)
                    .key(PK.A2_UPDATE_INTERVAL.getKey())
                    .defaultValue(1)
                    .build();
            updateRate.setTitle(R.string.prefs_updateRate);
            updateRate.setSummary(R.string.prefs_updateRate_summary);
            addPreference(updateRate);

            MaterialMultiChoicePreference customDownloadInfo = new MaterialMultiChoicePreference.Builder(context)
                    .entryValues(CustomDownloadInfo.Info.stringValues())
                    .entries(CustomDownloadInfo.Info.formalValues(context))
                    .defaultValue(CustomDownloadInfo.Info.DOWNLOAD_SPEED.name(), CustomDownloadInfo.Info.REMAINING_TIME.name())
                    .showValueMode(AbsMaterialTextValuePreference.SHOW_ON_BOTTOM)
                    .key(PK.A2_CUSTOM_INFO.getKey())
                    .build();
            customDownloadInfo.setTitle(R.string.downloadDisplayInfo); // FIXME
            customDownloadInfo.setSummary(R.string.downloadDisplayInfoSummary); // FIXME
            addPreference(customDownloadInfo);

            MaterialCheckboxPreference forceAction = new MaterialCheckboxPreference.Builder(context)
                    .key(PK.A2_FORCE_ACTION.getKey())
                    .defaultValue(true)
                    .build();
            forceAction.setTitle(R.string.forceAction); // FIXME
            forceAction.setSummary(R.string.forceAction); // FIXME
            addPreference(forceAction);

            MaterialCheckboxPreference versionCheck = new MaterialCheckboxPreference.Builder(context)
                    .key(PK.A2_CHECK_VERSION.getKey())
                    .defaultValue(true)
                    .build();
            versionCheck.setTitle(R.string.versionCheck); // FIXME
            versionCheck.setSummary(R.string.versionCheck); // FIXME
            addPreference(versionCheck);
        }

        @Override
        public int getTitleRes() {
            return R.string.general;
        }
    }

    public static class DirectDownloadFragment extends BasePreferenceFragment {

        @Override
        protected void buildPreferences(@NonNull Context context) {
            MaterialEditTextPreference downloadPath = new MaterialEditTextPreference.Builder(context)
                    .showValueMode(AbsMaterialTextValuePreference.SHOW_ON_BOTTOM)
                    .key(PK.DD_DOWNLOAD_PATH.getKey())
                    .defaultValue(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath())
                    .build();
            downloadPath.setTitle(R.string.dd_downloadPath); // FIXME
            downloadPath.setSummary(R.string.dd_downloadPath_summary); // FIXME
            addPreference(downloadPath);

            MaterialSeekBarPreference concurrentDownloads = new MaterialSeekBarPreference.Builder(context)
                    .minValue(1).maxValue(10).showValue(true)
                    .key(PK.DD_MAX_SIMULTANEOUS_DOWNLOADS.getKey())
                    .defaultValue(3)
                    .build();
            concurrentDownloads.setTitle(R.string.dd_maxSimultaneousDownloads); // FIXME
            concurrentDownloads.setSummary(R.string.dd_maxSimultaneousDownloads_summary); // FIXME
            addPreference(concurrentDownloads);

            MaterialCheckboxPreference resume = new MaterialCheckboxPreference.Builder(context)
                    .key(PK.DD_RESUME.getKey())
                    .defaultValue(true)
                    .build();
            resume.setTitle(R.string.dd_resume); // FIXME
            resume.setSummary(R.string.dd_resume_summary); // FIXME
            addPreference(resume);
        }

        @Override
        public int getTitleRes() {
            return R.string.directDownload;
        }
    }

    public static class NotificationsFragment extends BasePreferenceFragment {

        @Override
        protected void buildPreferences(@NonNull Context context) {
            MaterialCheckboxPreference enable = new MaterialCheckboxPreference.Builder(context)
                    .key(PK.A2_ENABLE_NOTIFS.getKey())
                    .defaultValue(true)
                    .build();
            enable.setTitle(R.string.enableNotifications); // FIXME
            enable.setSummary(R.string.enableNotifications); // FIXME
            addPreference(enable);

            MaterialCheckboxPreference startAtBoot = new MaterialCheckboxPreference.Builder(context)
                    .key(PK.A2_NOTIFS_AT_BOOT.getKey())
                    .defaultValue(true)
                    .build();
            startAtBoot.setTitle(R.string.enableNotificationsAtBoot); // FIXME
            startAtBoot.setSummary(R.string.enableNotificationsAtBoot); // FIXME
            addPreference(startAtBoot);

            MaterialMultiChoicePreference types = new MaterialMultiChoicePreference.Builder(context)
                    .entryValues(NotificationService.EventType.prefsValues())
                    .entries(NotificationService.EventType.formalValues(context))
                    .showValueMode(AbsMaterialTextValuePreference.SHOW_ON_BOTTOM)
                    .key(PK.A2_SELECTED_NOTIFS_TYPE.getKey())
                    .defaultValue(new HashSet<>(Arrays.asList(NotificationService.EventType.prefsValues())))
                    .build();
            types.setTitle(R.string.notificationType); // FIXME
            types.setSummary(R.string.notificationTypeSummary); // FIXME
            addPreference(types);

            addController(enable, true, startAtBoot, types);
        }

        @Override
        public int getTitleRes() {
            return R.string.notifications;
        }
    }
}
