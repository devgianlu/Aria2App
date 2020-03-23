package com.gianlu.aria2app;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.documentfile.provider.DocumentFile;

import com.gianlu.aria2app.downloader.FetchHelper;
import com.gianlu.aria2app.profiles.ProfilesManager;
import com.gianlu.aria2app.services.NotificationService;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.preferences.BasePreferenceActivity;
import com.gianlu.commonutils.preferences.BasePreferenceFragment;
import com.gianlu.commonutils.preferences.MaterialAboutPreferenceItem;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.yarolegovich.mp.AbsMaterialTextValuePreference;
import com.yarolegovich.mp.MaterialCheckboxPreference;
import com.yarolegovich.mp.MaterialMultiChoicePreference;
import com.yarolegovich.mp.MaterialSeekBarPreference;
import com.yarolegovich.mp.MaterialStandardPreference;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class PreferenceActivity extends BasePreferenceActivity {
    @NonNull
    @Override
    protected List<MaterialAboutPreferenceItem> getPreferencesItems() {
        return Arrays.asList(new MaterialAboutPreferenceItem(R.string.general, R.drawable.baseline_settings_24, GeneralFragment.class),
                new MaterialAboutPreferenceItem(R.string.profiles, R.drawable.baseline_supervisor_account_24, ProfilesFragment.class),
                new MaterialAboutPreferenceItem(R.string.directDownload, R.drawable.baseline_cloud_download_24, DirectDownloadFragment.class),
                new MaterialAboutPreferenceItem(R.string.notifications, R.drawable.baseline_sms_24, NotificationsFragment.class));
    }

    @Override
    protected int getAppIconRes() {
        return R.drawable.ic_launcher;
    }

    @Override
    protected boolean hasTutorial() {
        return true;
    }

    @Nullable
    @Override
    protected String getOpenSourceUrl() {
        return "https://github.com/devgianlu/Aria2App";
    }

    @Override
    protected boolean disableOtherDonationsOnGooglePlay() {
        return true;
    }

    public static class GeneralFragment extends BasePreferenceFragment {

        @Override
        protected void buildPreferences(@NonNull Context context) {
            MaterialCheckboxPreference nightMode = new MaterialCheckboxPreference.Builder(context)
                    .defaultValue(PK.NIGHT_MODE.fallback())
                    .key(PK.NIGHT_MODE.key())
                    .build();
            nightMode.setTitle(R.string.prefs_nightMode);
            nightMode.setSummary(R.string.prefs_nightMode_summary);
            nightMode.setIcon(R.drawable.baseline_opacity_24);
            addPreference(nightMode);

            MaterialCheckboxPreference hideMetadata = new MaterialCheckboxPreference.Builder(context)
                    .defaultValue(PK.A2_HIDE_METADATA.fallback())
                    .key(PK.A2_HIDE_METADATA.key())
                    .build();
            hideMetadata.setTitle(R.string.prefs_hideMetadata);
            hideMetadata.setSummary(R.string.prefs_hideMetadata_summary);
            hideMetadata.setIcon(R.drawable.baseline_link_24);
            addPreference(hideMetadata);

            MaterialSeekBarPreference updateRate = new MaterialSeekBarPreference.Builder(context)
                    .minValue(1).maxValue(10).showValue(true)
                    .key(PK.A2_UPDATE_INTERVAL.key())
                    .defaultValue(PK.A2_UPDATE_INTERVAL.fallback())
                    .build();
            updateRate.setTitle(R.string.prefs_updateRate);
            updateRate.setSummary(R.string.prefs_updateRate_summary);
            updateRate.setIcon(R.drawable.baseline_update_24);
            addPreference(updateRate);

            MaterialMultiChoicePreference customDownloadInfo = new MaterialMultiChoicePreference.Builder(context)
                    .entryValues(CustomDownloadInfo.Info.stringValues())
                    .entries(CustomDownloadInfo.Info.formalValues(context))
                    .defaultValue(CustomDownloadInfo.Info.DOWNLOAD_SPEED.name(), CustomDownloadInfo.Info.REMAINING_TIME.name())
                    .showValueMode(AbsMaterialTextValuePreference.SHOW_ON_BOTTOM)
                    .key(PK.A2_CUSTOM_INFO.key())
                    .build();
            customDownloadInfo.setTitle(R.string.prefs_downloadDisplayInfo);
            customDownloadInfo.setSummary(R.string.prefs_downloadDisplayInfo_summary);
            customDownloadInfo.setIcon(R.drawable.baseline_info_outline_24);
            addPreference(customDownloadInfo);

            MaterialCheckboxPreference bestTrackers = new MaterialCheckboxPreference.Builder(context)
                    .key(PK.A2_ADD_BEST_TRACKERS.key())
                    .defaultValue(PK.A2_ADD_BEST_TRACKERS.fallback())
                    .build();
            bestTrackers.setTitle(R.string.prefs_addBestTrackers);
            bestTrackers.setSummary(R.string.prefs_addBestTrackers_summary);
            bestTrackers.setIcon(R.drawable.baseline_track_changes_24);
            addPreference(bestTrackers);

            MaterialCheckboxPreference skipWebViewDialog = new MaterialCheckboxPreference.Builder(context)
                    .key(PK.A2_SKIP_WEBVIEW_DIALOG.key())
                    .defaultValue(PK.A2_SKIP_WEBVIEW_DIALOG.fallback())
                    .build();
            skipWebViewDialog.setTitle(R.string.prefs_skipWebViewDialog);
            skipWebViewDialog.setSummary(R.string.prefs_skipWebViewDialog_summary);
            // skipWebViewDialog.setIcon(R.drawable.baseline_track_changes_24);
            addPreference(skipWebViewDialog);

            if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                MaterialStandardPreference webviewShortcut = new MaterialStandardPreference(context);
                webviewShortcut.setTitle(R.string.addWebViewShortcutToHomePage);
                webviewShortcut.setSummary(R.string.addWebViewShortcutToHomePage_summary);
                webviewShortcut.setIcon(R.drawable.baseline_language_24);
                addPreference(webviewShortcut);

                webviewShortcut.setOnClickListener(v -> {
                    ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(context, "#1")
                            .setIntent(new Intent(context, LoadingActivity.class)
                                    .setAction(LoadingActivity.SHORTCUT_WEB_VIEW))
                            .setShortLabel(getString(R.string.webView) + " - " + getString(R.string.app_name))
                            .setIcon(IconCompat.createWithResource(context, R.drawable.baseline_language_colored_24))
                            .build();
                    ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null);
                });
            }
        }

        @Override
        public int getTitleRes() {
            return R.string.general;
        }
    }

    public static class ProfilesFragment extends BasePreferenceFragment {
        private static final int IMPORT_PROFILES_CODE = 7;
        private static final String TAG = ProfilesFragment.class.getSimpleName();

        @Override
        protected void buildPreferences(@NonNull Context context) {
            MaterialStandardPreference importProfiles = new MaterialStandardPreference(context);
            importProfiles.setTitle(R.string.importProfiles);
            importProfiles.setSummary(R.string.importProfiles_summary);
            addPreference(importProfiles);

            importProfiles.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                try {
                    startActivityForResult(Intent.createChooser(intent, "Select a file"), IMPORT_PROFILES_CODE);
                } catch (ActivityNotFoundException ex) {
                    showToast(Toaster.build().message(R.string.noFilemanager));
                }
            });

            MaterialStandardPreference exportProfiles = new MaterialStandardPreference(context);
            exportProfiles.setTitle(R.string.exportProfiles);
            exportProfiles.setSummary(R.string.exportProfiles_summary);
            addPreference(exportProfiles);

            exportProfiles.setOnClickListener(v -> doExport());
        }

        private void doExport() {
            try {
                JSONArray json = ProfilesManager.get(requireContext()).exportAllProfiles();

                File file = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "aria2app-profiles-" + CommonUtils.randomString(5) + ".json");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(json.toString().getBytes());
                }

                showToast(Toaster.build().message(R.string.profilesExportedSuccessfully, file.getAbsolutePath()));
            } catch (JSONException | IOException ex) {
                Log.e(TAG, "Failed exporting profiles.", ex);
                showToast(Toaster.build().message(R.string.failedExportingProfiles));
            }
        }

        private void doImport(@NonNull Uri uri) {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                    if (in == null) return;

                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = in.read(buffer)) != -1)
                        bout.write(buffer, 0, count);
                }

                ProfilesManager.get(requireContext()).importProfiles(new JSONArray(new String(bout.toByteArray())));

                showToast(Toaster.build().message(R.string.profilesImportedSuccessfully));
            } catch (JSONException | IOException ex) {
                Log.e(TAG, "Failed importing profiles.", ex);
                showToast(Toaster.build().message(R.string.failedImportingProfiles));
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (requestCode == IMPORT_PROFILES_CODE) {
                if (resultCode == RESULT_OK) {
                    if (getContext() == null || data == null || data.getData() == null)
                        return;

                    doImport(data.getData());
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        @Override
        public int getTitleRes() {
            return R.string.profiles;
        }
    }

    public static class DirectDownloadFragment extends BasePreferenceFragment {
        private static final int DOWNLOAD_PATH_CODE = 34;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == DOWNLOAD_PATH_CODE) {
                if (resultCode == RESULT_OK && isAdded() && data.getData() != null) {
                    DocumentFile file = DocumentFile.fromTreeUri(requireContext(), data.getData());
                    if (file != null)
                        Prefs.putString(PK.DD_DOWNLOAD_PATH, file.getUri().toString());
                }

                return;
            }

            super.onActivityResult(requestCode, resultCode, data);
        }

        @Override
        protected void buildPreferences(@NonNull Context context) {
            MaterialCheckboxPreference external = new MaterialCheckboxPreference.Builder(context)
                    .key(PK.DD_USE_EXTERNAL.key())
                    .defaultValue(PK.DD_USE_EXTERNAL.fallback())
                    .build();
            external.setTitle(R.string.prefs_ddUseExternal);
            external.setSummary(R.string.prefs_ddUseExternal_summary);
            external.setIcon(R.drawable.baseline_share_24);
            addPreference(external);

            MaterialStandardPreference downloadPath = new MaterialStandardPreference.Builder(context)
                    .key(PK.DD_DOWNLOAD_PATH.key())
                    .defaultValue(PK.DD_DOWNLOAD_PATH.fallback())
                    .build();
            downloadPath.setTitle(R.string.prefs_ddDownloadPath);
            downloadPath.setSummary(R.string.prefs_ddDownloadPath_summary);
            downloadPath.setIcon(R.drawable.baseline_folder_24);
            downloadPath.addPreferenceClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    startActivityForResult(intent, DOWNLOAD_PATH_CODE);
                } catch (ActivityNotFoundException ex) {
                    showToast(Toaster.build().message(R.string.noFilemanager));
                }
            });
            addPreference(downloadPath);

            MaterialSeekBarPreference concurrentDownloads = new MaterialSeekBarPreference.Builder(context)
                    .minValue(1).maxValue(10).showValue(true)
                    .key(PK.DD_MAX_SIMULTANEOUS_DOWNLOADS.key())
                    .defaultValue(PK.DD_MAX_SIMULTANEOUS_DOWNLOADS.fallback())
                    .build();
            concurrentDownloads.setTitle(R.string.prefs_ddMaxSimultaneousDownloads);
            concurrentDownloads.setSummary(R.string.prefs_ddMaxSimultaneousDownloads_summary);
            concurrentDownloads.setIcon(R.drawable.baseline_download_24);
            addPreference(concurrentDownloads);

            MaterialCheckboxPreference resume = new MaterialCheckboxPreference.Builder(context)
                    .key(PK.DD_RESUME.key())
                    .defaultValue(PK.DD_RESUME.fallback())
                    .build();
            resume.setTitle(R.string.prefs_ddResume);
            resume.setSummary(R.string.prefs_ddResume_summary);
            resume.setIcon(R.drawable.baseline_pause_24);
            addPreference(resume);

            addController(external, false, downloadPath, concurrentDownloads, resume);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            FetchHelper.invalidate();
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
                    .key(PK.A2_ENABLE_NOTIFS.key())
                    .defaultValue(PK.A2_ENABLE_NOTIFS.fallback())
                    .build();
            enable.setTitle(R.string.prefs_enableNotifications);
            enable.setSummary(R.string.prefs_enableNotifications_summary);
            enable.setIcon(R.drawable.baseline_notifications_24);
            addPreference(enable);

            MaterialMultiChoicePreference types = new MaterialMultiChoicePreference.Builder(context)
                    .entryValues(NotificationService.EventType.prefsValues())
                    .entries(NotificationService.EventType.formalValues(context))
                    .showValueMode(AbsMaterialTextValuePreference.SHOW_ON_BOTTOM)
                    .key(PK.A2_SELECTED_NOTIFS_TYPE.key())
                    .defaultValue(PK.A2_SELECTED_NOTIFS_TYPE.fallback())
                    .build();
            types.setTitle(R.string.prefs_notificationType);
            types.setSummary(R.string.prefs_notificationType_summary);
            types.setIcon(R.drawable.baseline_more_24);
            addPreference(types);

            MaterialCheckboxPreference startAtBoot = new MaterialCheckboxPreference.Builder(context)
                    .key(PK.A2_NOTIFS_AT_BOOT.key())
                    .defaultValue(PK.A2_NOTIFS_AT_BOOT.fallback())
                    .build();
            startAtBoot.setTitle(R.string.prefs_enableNotificationsAtBoot);
            startAtBoot.setSummary(R.string.prefs_enableNotificationsAtBoot_summary);
            startAtBoot.setIcon(R.drawable.baseline_android_24);
            addPreference(startAtBoot);

            addController(enable, true, startAtBoot, types);
        }

        @Override
        public int getTitleRes() {
            return R.string.notifications;
        }
    }
}
