package com.gianlu.aria2app.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.activities.editprofile.AuthenticationFragment;
import com.gianlu.aria2app.activities.editprofile.ConnectionFragment;
import com.gianlu.aria2app.activities.editprofile.DirectDownloadFragment;
import com.gianlu.aria2app.activities.editprofile.FieldErrorFragmentWithState;
import com.gianlu.aria2app.activities.editprofile.InvalidFieldException;
import com.gianlu.aria2app.activities.editprofile.TestFragment;
import com.gianlu.aria2app.activities.editprofile.WifisAdapter;
import com.gianlu.aria2app.adapters.RadioConditionsAdapter;
import com.gianlu.aria2app.adapters.SpinnerConditionsAdapter;
import com.gianlu.aria2app.adapters.StatePagerAdapter;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.aria2app.profiles.MultiProfile.ConnectivityCondition;
import com.gianlu.aria2app.profiles.ProfilesManager;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.permissions.AskPermission;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.gianlu.aria2app.activities.editprofile.InvalidFieldException.Where;

public class EditProfileActivity extends ActivityWithDialog implements TestFragment.OnGetProfile {
    private static final String TAG = EditProfileActivity.class.getSimpleName();
    private final List<ConditionWithState> conditions = new ArrayList<>();
    private ProfileFragmentsAdapter pagerAdapter;
    private MultiProfile editProfile;
    private TextInputLayout profileName;
    private CheckBox enableNotifs;
    private ViewPager pager;
    private Spinner conditionsSpinner;

    public static void start(Context context, boolean firstProfile) {
        context.startActivity(new Intent(context, EditProfileActivity.class)
                .putExtra("firstProfile", firstProfile));
    }

    public static void start(Context context, String edit) {
        context.startActivity(new Intent(context, EditProfileActivity.class)
                .putExtra("firstProfile", false)
                .putExtra("edit", edit));
    }

    @NonNull
    private static Bundle stateFromProfile(@NonNull MultiProfile.UserProfile profile) {
        Bundle result = new Bundle();
        result.putBundle("connection", ConnectionFragment.stateFromProfile(profile));
        result.putBundle("authentication", AuthenticationFragment.stateFromProfile(profile));
        result.putBundle("directDownload", DirectDownloadFragment.stateFromProfile(profile));
        return result;
    }

    @NonNull
    private static Bundle emptyConditionBundle() {
        Bundle result = new Bundle();
        result.putBundle("connection", new Bundle());
        result.putBundle("authentication", new Bundle());
        result.putBundle("directDownload", new Bundle());
        return result;
    }

    private boolean hasAlwaysCondition() {
        for (ConditionWithState cs : conditions)
            if (cs.condition.type == ConnectivityCondition.Type.ALWAYS)
                return true;

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = findViewById(R.id.editProfile_toolbar);
        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(!getIntent().getBooleanExtra("firstProfile", true));
            bar.setDisplayShowTitleEnabled(false);
        }

        String editId = getIntent().getStringExtra("edit");
        if (editId == null) {
            editProfile = null;
        } else {
            try {
                editProfile = ProfilesManager.get(this).retrieveProfile(editId);
            } catch (IOException | JSONException ex) {
                Log.e(TAG, "Failed getting profile: " + editId, ex);
                editProfile = null;
            }
        }

        if (editProfile == null) setTitle(R.string.addProfile);
        else setTitle(R.string.editProfile);

        profileName = findViewById(R.id.editProfile_profileName);
        CommonUtils.clearErrorOnEdit(profileName);

        enableNotifs = findViewById(R.id.editProfile_enableNotifs);

        if (editProfile != null) {
            CommonUtils.getEditText(profileName).setText(editProfile.name);
            enableNotifs.setChecked(editProfile.notificationsEnabled);
        }

        conditionsSpinner = findViewById(R.id.editProfile_conditionsSpinner);
        conditionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showConditionAt(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        TabLayout tabLayout = findViewById(R.id.editProfile_tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        pager = findViewById(R.id.editProfile_pager);
        pager.setOffscreenPageLimit(4);

        pagerAdapter = new ProfileFragmentsAdapter(this, getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(pager);

        createAllFragments();
        if (editProfile == null) createNewCondition(true);
        refreshSpinner();
    }

    private void createAllFragments() {
        if (editProfile != null) {
            for (MultiProfile.UserProfile profile : editProfile.profiles)
                conditions.add(new ConditionWithState(profile.connectivityCondition,
                        stateFromProfile(profile)));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_profile, menu);
        return true;
    }

    private void saveCurrent() {
        int adapterPos = pagerAdapter.pos();
        if (adapterPos != -1 && adapterPos < conditions.size()) {
            try {
                Bundle saved = pagerAdapter.save();
                ConditionWithState cs = conditions.get(adapterPos);
                if (cs == null) throw new IllegalStateException();
                cs.setState(saved);
            } catch (IllegalStateException ex) {
                Log.e(TAG, "Failed saving state.", ex);
            }
        }
    }

    private void showConditionAt(int pos) {
        if (pos != -1) {
            saveCurrent();

            ConditionWithState cs = conditions.get(pos);
            if (cs == null) throw new IllegalStateException();
            pagerAdapter.restore(pos, cs.state);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.editProfile_deleteProfile).setVisible(editProfile != null);
        return true;
    }

    private boolean hasDefault() {
        for (ConditionWithState cs : conditions)
            if (cs.condition.isDefault)
                return true;

        return false;
    }

    private boolean hasCondition(@NonNull ConnectivityCondition cond) {
        for (ConditionWithState cs : conditions)
            if (cs.condition.equals(cond))
                return true;

        return false;
    }

    private void createNewCondition(final boolean compulsory) {
        if (hasAlwaysCondition()) {
            Toaster.with(this).message(R.string.hasAlwaysCondition).show();
            return;
        }

        ScrollView layout = (ScrollView) getLayoutInflater().inflate(R.layout.dialog_new_condition, null, false);

        TextInputLayout ssid = layout.findViewById(R.id.editProfile_ssid);
        CommonUtils.clearErrorOnEdit(ssid);

        MultiAutoCompleteTextView ssidField = (MultiAutoCompleteTextView) CommonUtils.getEditText(ssid);
        ssidField.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        RadioGroup connectivityCondition = layout.findViewById(R.id.editProfile_connectivityCondition);
        connectivityCondition.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.editProfile_connectivityCondition_wifi) {
                ssid.setVisibility(View.VISIBLE);

                WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                if (manager == null || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)
                    return;

                ssidField.setAdapter(new WifisAdapter(this, manager.getConfiguredNetworks()));
                ssidField.setThreshold(1);
            } else {
                ssid.setVisibility(View.GONE);
            }
        });

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.newCondition)
                .setView(layout)
                .setCancelable(!compulsory)
                .setPositiveButton(R.string.create, null);

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            if (compulsory) onBackPressed();
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            ConnectivityCondition condition;
            switch (connectivityCondition.getCheckedRadioButtonId()) {
                default:
                case R.id.editProfile_connectivityCondition_always:
                    condition = ConnectivityCondition.newUniqueCondition();
                    break;
                case R.id.editProfile_connectivityCondition_mobile:
                    condition = ConnectivityCondition.newMobileCondition(!hasDefault());
                    break;
                case R.id.editProfile_connectivityCondition_wifi:
                    String[] ssidsArray = ConnectivityCondition.parseSSIDs(CommonUtils.getText(ssid));
                    if (ssidsArray.length == 0) {
                        ssidField.setText("");
                        ssid.setError(getString(R.string.emptySSID));
                        return;
                    }

                    condition = ConnectivityCondition.newWiFiCondition(ssidsArray, !hasDefault());
                    break;
                case R.id.editProfile_connectivityCondition_ethernet:
                    condition = ConnectivityCondition.newEthernetCondition(!hasDefault());
                    break;
                case R.id.editProfile_connectivityCondition_bluetooth:
                    condition = ConnectivityCondition.newBluetoothCondition(!hasDefault());
                    break;
            }

            if (condition.type == ConnectivityCondition.Type.ALWAYS && !conditions.isEmpty()) {
                Toaster.with(this).message(R.string.cannotAddAlwaysCondition).show();
                return;
            }

            if (hasCondition(condition)) {
                Toaster.with(this).message(R.string.duplicatedCondition).show();
                return;
            }

            if (condition.type == ConnectivityCondition.Type.WIFI
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                AskPermission.ask(this, Manifest.permission.ACCESS_FINE_LOCATION, new AskPermission.Listener() {
                    @Override
                    public void permissionGranted(@NonNull String permission) {
                        addCondition(dialog, condition);
                    }

                    @Override
                    public void permissionDenied(@NonNull String permission) {
                        Toaster.with(EditProfileActivity.this).message(R.string.locationAccessDenied).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void askRationale(@NonNull AlertDialog.Builder builder) {
                        builder.setTitle(R.string.locationAccess_title).setMessage(R.string.locationAccess_message);
                    }
                });
                return;
            }

            addCondition(dialog, condition);
        }));

        showDialog(dialog);
    }

    private void addCondition(@NonNull AlertDialog dialog, @NonNull ConnectivityCondition condition) {
        conditions.add(new ConditionWithState(condition, emptyConditionBundle()));

        refreshSpinner();
        conditionsSpinner.setSelection(conditions.size() - 1);
        pager.setCurrentItem(0, false);
        dialog.dismiss();
    }

    private void refreshSpinner() {
        conditionsSpinner.setAdapter(new SpinnerConditionsAdapter(this, conditionsList()));
    }

    @NonNull
    private List<ConnectivityCondition> conditionsList() {
        List<ConnectivityCondition> list = new ArrayList<>(conditions.size());
        for (ConditionWithState cs : conditions) list.add(cs.condition);
        return list;
    }

    @NonNull
    private MultiProfile buildProfile() throws InvalidFieldException {
        String profileName = CommonUtils.getText(this.profileName).trim();
        if (profileName.isEmpty() || (ProfilesManager.get(this).profileExists(ProfilesManager.getId(profileName)) && editProfile == null)
                || profileName.equals(MultiProfile.IN_APP_DOWNLOADER_NAME)) {
            throw new InvalidFieldException(Where.ACTIVITY, R.id.editProfile_profileName, R.string.invalidProfileName);
        }

        saveCurrent();

        MultiProfile profile = new MultiProfile(profileName, enableNotifs.isChecked());
        for (int i = 0; i < conditions.size(); i++) {
            Bundle state = conditions.get(i).state;

            try {
                Bundle connState = state.getBundle("connection");
                if (connState == null) throw new IllegalStateException();
                ConnectionFragment.Fields conn = ConnectionFragment.validateStateAndCreateFields(connState, false);

                Bundle authState = state.getBundle("authentication");
                if (authState == null) throw new IllegalStateException();
                AuthenticationFragment.Fields auth = AuthenticationFragment.validateStateAndCreateFields(authState);

                Bundle ddState = state.getBundle("directDownload");
                if (ddState == null) throw new IllegalStateException();
                DirectDownloadFragment.Fields dd = DirectDownloadFragment.validateStateAndCreateFields(ddState);

                profile.add(conditions.get(i).condition, conn, auth, dd);
            } catch (InvalidFieldException ex) {
                ex.pos = i;
                throw ex;
            }
        }

        return profile;
    }

    private void doneAll() {
        try {
            ProfilesManager manager = ProfilesManager.get(this);
            MultiProfile profile = buildProfile();
            manager.save(profile);

            if (editProfile != null && !Objects.equals(editProfile.id, profile.id))
                manager.delete(editProfile);

            onBackPressed();
        } catch (InvalidFieldException ex) {
            handleInvalidFieldException(ex);
        } catch (JSONException | IOException | IllegalStateException ex) {
            Log.e(TAG, "Failed saving profile.", ex);
            Toaster.with(this).message(R.string.cannotSaveProfile).show();
        }

        AnalyticsApplication.sendAnalytics(Utils.ACTION_NEW_PROFILE);
    }

    private void handleInvalidFieldException(@NonNull InvalidFieldException ex) {
        if (ex.where == Where.ACTIVITY) {
            TextInputLayout field = findViewById(ex.fieldId);
            field.clearFocus();
            field.setErrorEnabled(true);
            field.setError(getString(ex.reasonRes));
            return;
        }

        if (ex.pos == -1) return;
        showConditionAt(ex.pos);

        FieldErrorFragmentWithState fragment = (FieldErrorFragmentWithState) pagerAdapter.get(ex.where);
        pager.setCurrentItem(ex.where.pagerPos(), true);
        fragment.onFieldError(ex.fieldId, getString(ex.reasonRes));
    }

    private void deleteProfile() {
        if (editProfile == null) return;

        AnalyticsApplication.sendAnalytics(Utils.ACTION_DELETE_PROFILE);

        ProfilesManager.get(this).delete(editProfile);
        onBackPressed();
    }

    private void deleteCondition(int position) {
        if (position != -1) conditions.remove(position);

        if (conditions.size() == 0) {
            onBackPressed();
        } else {
            refreshSpinner();
            conditionsSpinner.setSelection(conditions.size() - 1);
        }
    }

    private int findDefaultCondition() {
        for (int i = 0; i < conditions.size(); i++)
            if (conditions.get(i).condition.isDefault)
                return i;

        return 0;
    }

    private void setDefaultCondition(int pos) {
        conditions.get(findDefaultCondition()).setDefault(false);
        conditions.get(pos).setDefault(true);
    }

    private void setDefaultCondition() {
        int def = findDefaultCondition();
        if (def == -1) {
            setDefaultCondition(0);
            def = 0;
        }

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.setDefaultCondition)
                .setSingleChoiceItems(new RadioConditionsAdapter(this, conditionsList()), def, (dialog, which) -> {
                    setDefaultCondition(which);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null);

        showDialog(builder);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.editProfile_deleteProfile:
                deleteProfile();
                return true;
            case R.id.editProfile_deleteCondition:
                deleteCondition(conditionsSpinner.getSelectedItemPosition());
                return true;
            case R.id.editProfile_setDefault:
                setDefaultCondition();
                return true;
            case R.id.editProfile_createNew:
                createNewCondition(false);
                return true;
            case R.id.editProfile_doneAll:
                doneAll();
                return true;
            default:
                return false;
        }
    }

    @Nullable
    @Override
    public MultiProfile.UserProfile getProfile() {
        try {
            MultiProfile profile = buildProfile();
            if (profile.isEmpty()) {
                onBackPressed();
                return null;
            }

            return profile.getProfile(this);
        } catch (InvalidFieldException ex) {
            handleInvalidFieldException(ex);
            return null;
        }
    }

    private static class ConditionWithState {
        private ConnectivityCondition condition;
        private Bundle state;

        ConditionWithState(@NonNull ConnectivityCondition condition, @NonNull Bundle state) {
            this.condition = condition;
            this.state = state;
        }

        void setState(Bundle state) {
            this.state = state;
        }

        void setDefault(boolean val) {
            condition = condition.changeDefaultValue(val);
        }
    }

    private static final class ProfileFragmentsAdapter extends StatePagerAdapter<Fragment> {
        private final ConnectionFragment connection;
        private final AuthenticationFragment authentication;
        private final DirectDownloadFragment directDownload;
        private final TestFragment test;
        private int currentPos = -1;

        ProfileFragmentsAdapter(@NonNull Context context, @NonNull FragmentManager fm) {
            super(fm, ConnectionFragment.getInstance(context), AuthenticationFragment.getInstance(context),
                    DirectDownloadFragment.getInstance(context), TestFragment.getInstance(context));

            connection = (ConnectionFragment) getItem(0);
            authentication = (AuthenticationFragment) getItem(1);
            directDownload = (DirectDownloadFragment) getItem(2);
            test = (TestFragment) getItem(3);
        }

        void restore(int pos, @NonNull Bundle bundle) {
            currentPos = pos;

            connection.restore(bundle.getBundle("connection"));
            authentication.restore(bundle.getBundle("authentication"));
            directDownload.restore(bundle.getBundle("directDownload"));
            test.clearViews();
        }

        /**
         * @return The current condition position in the {@link EditProfileActivity#conditions} list
         */
        int pos() {
            return currentPos;
        }

        @NonNull
        Bundle save() {
            Bundle result = new Bundle();
            result.putBundle("connection", connection.save());
            result.putBundle("authentication", authentication.save());
            result.putBundle("directDownload", directDownload.save());
            return result;
        }

        @NonNull
        public Fragment get(@NonNull Where where) {
            switch (where) {
                default:
                case ACTIVITY:
                    throw new IllegalStateException();
                case CONNECTION:
                    return connection;
                case AUTHENTICATION:
                    return authentication;
                case DIRECT_DOWNLOAD:
                    return directDownload;
            }
        }
    }
}
