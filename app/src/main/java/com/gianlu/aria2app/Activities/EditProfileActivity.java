package com.gianlu.aria2app.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioGroup;
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

import com.gianlu.aria2app.Activities.EditProfile.AuthenticationFragment;
import com.gianlu.aria2app.Activities.EditProfile.ConnectionFragment;
import com.gianlu.aria2app.Activities.EditProfile.DirectDownloadFragment;
import com.gianlu.aria2app.Activities.EditProfile.FieldErrorFragmentWithState;
import com.gianlu.aria2app.Activities.EditProfile.InvalidFieldException;
import com.gianlu.aria2app.Activities.EditProfile.TestFragment;
import com.gianlu.aria2app.Activities.EditProfile.WifisAdapter;
import com.gianlu.aria2app.Adapters.RadioConditionsAdapter;
import com.gianlu.aria2app.Adapters.SpinnerConditionsAdapter;
import com.gianlu.aria2app.Adapters.StatePagerAdapter;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.gianlu.aria2app.Activities.EditProfile.InvalidFieldException.Where;

public class EditProfileActivity extends ActivityWithDialog implements TestFragment.OnGetProfile {
    private final List<MultiProfile.ConnectivityCondition> conditions = new ArrayList<>();
    private final List<Bundle> states = new ArrayList<>();
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

    private boolean hasAlwaysCondition() {
        for (MultiProfile.ConnectivityCondition cond : conditions)
            if (cond.type == MultiProfile.ConnectivityCondition.Type.ALWAYS)
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
                Logging.log(ex);
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
            for (MultiProfile.UserProfile profile : editProfile.profiles) {
                conditions.add(profile.connectivityCondition);
                states.add(stateFromProfile(profile));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_profile, menu);
        return true;
    }

    private void saveCurrent() {
        if (pagerAdapter.pos() != -1) {
            try {
                Bundle saved = pagerAdapter.save();
                states.set(pagerAdapter.pos(), saved);
            } catch (IllegalStateException ex) {
                Logging.log("Failed saving state.", ex);
            }
        }
    }

    private void showConditionAt(int pos) {
        if (pos != -1) {
            saveCurrent();
            pagerAdapter.restore(pos, states.get(pos));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.editProfile_deleteProfile).setVisible(editProfile != null);
        return true;
    }

    private boolean hasDefault() {
        for (MultiProfile.ConnectivityCondition cond : conditions)
            if (cond.isDefault)
                return true;

        return false;
    }

    private void createNewCondition(final boolean compulsory) {
        if (hasAlwaysCondition()) {
            Toaster.with(this).message(R.string.hasAlwaysCondition).show();
            return;
        }

        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_new_condition, null, false);

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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.newCondition)
                .setView(layout)
                .setCancelable(!compulsory)
                .setPositiveButton(R.string.create, null);

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            if (compulsory) onBackPressed();
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            MultiProfile.ConnectivityCondition condition;
            switch (connectivityCondition.getCheckedRadioButtonId()) {
                default:
                case R.id.editProfile_connectivityCondition_always:
                    condition = MultiProfile.ConnectivityCondition.newUniqueCondition();
                    break;
                case R.id.editProfile_connectivityCondition_mobile:
                    condition = MultiProfile.ConnectivityCondition.newMobileCondition(!hasDefault());
                    break;
                case R.id.editProfile_connectivityCondition_wifi:
                    String[] ssidsArray = MultiProfile.ConnectivityCondition.parseSSIDs(CommonUtils.getText(ssid));
                    if (ssidsArray.length == 0) {
                        ssidField.setText("");
                        ssid.setError(getString(R.string.emptySSID));
                        return;
                    }

                    condition = MultiProfile.ConnectivityCondition.newWiFiCondition(ssidsArray, !hasDefault());
                    break;
                case R.id.editProfile_connectivityCondition_ethernet:
                    condition = MultiProfile.ConnectivityCondition.newEthernetCondition(!hasDefault());
                    break;
                case R.id.editProfile_connectivityCondition_bluetooth:
                    condition = MultiProfile.ConnectivityCondition.newBluetoothCondition(!hasDefault());
                    break;
            }

            if (condition.type == MultiProfile.ConnectivityCondition.Type.ALWAYS && !conditions.isEmpty()) {
                Toaster.with(this).message(R.string.cannotAddAlwaysCondition).show();
                return;
            }

            if (conditions.contains(condition)) {
                Toaster.with(this).message(R.string.duplicatedCondition).show();
                return;
            }

            if (condition.type == MultiProfile.ConnectivityCondition.Type.WIFI
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

    private void addCondition(@NonNull AlertDialog dialog, @NonNull MultiProfile.ConnectivityCondition condition) {
        conditions.add(condition);
        states.add(new Bundle());

        refreshSpinner();
        conditionsSpinner.setSelection(conditions.size() - 1);
        pager.setCurrentItem(0, false);
        dialog.dismiss();
    }

    private void refreshSpinner() {
        conditionsSpinner.setAdapter(new SpinnerConditionsAdapter(this, conditions));
    }

    @NonNull
    private MultiProfile buildProfile() throws InvalidFieldException {
        String profileName = CommonUtils.getText(this.profileName).trim();
        if (profileName.isEmpty() || (ProfilesManager.get(this).profileExists(ProfilesManager.getId(profileName)) && editProfile == null)
                || profileName.equals(MultiProfile.IN_APP_DOWNLOADER_NAME)) {
            throw new InvalidFieldException(Where.ACTIVITY, R.id.editProfile_profileName, R.string.invalidProfileName);
        }

        saveCurrent();

        if (states.size() != conditions.size())
            throw new IllegalStateException();

        MultiProfile profile = new MultiProfile(profileName, enableNotifs.isChecked());
        for (int i = 0; i < conditions.size(); i++) {
            Bundle state = states.get(i);

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

                profile.add(conditions.get(i), conn, auth, dd);
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
            Toaster.with(this).message(R.string.cannotSaveProfile).ex(ex).show();
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
        if (position != -1) {
            conditions.remove(position);
            states.remove(position);
        }

        refreshSpinner();
        conditionsSpinner.setSelection(conditions.size() - 1);
    }

    private int findDefaultCondition() {
        for (int i = 0; i < conditions.size(); i++)
            if (conditions.get(i).isDefault)
                return i;

        return 0;
    }

    private void setDefaultCondition(int pos) {
        int oldDefPos = findDefaultCondition();
        MultiProfile.ConnectivityCondition oldDefault = conditions.get(oldDefPos);
        conditions.set(oldDefPos, new MultiProfile.ConnectivityCondition(oldDefault.type, oldDefault.ssids, false));

        MultiProfile.ConnectivityCondition newDefault = conditions.get(pos);
        conditions.set(pos, new MultiProfile.ConnectivityCondition(newDefault.type, newDefault.ssids, true));
    }

    private void setDefaultCondition() {
        int def = findDefaultCondition();
        if (def == -1) {
            setDefaultCondition(0);
            def = 0;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.setDefaultCondition)
                .setSingleChoiceItems(new RadioConditionsAdapter(this, conditions), def, (dialog, which) -> {
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
                break;
            case R.id.editProfile_deleteProfile:
                deleteProfile();
                break;
            case R.id.editProfile_deleteCondition:
                if (conditions.size() == 1) deleteProfile();
                else deleteCondition(conditionsSpinner.getSelectedItemPosition());
                break;
            case R.id.editProfile_setDefault:
                setDefaultCondition();
                break;
            case R.id.editProfile_createNew:
                createNewCondition(false);
                break;
            case R.id.editProfile_doneAll:
                doneAll();
                break;
        }

        return true;
    }

    @Nullable
    @Override
    public MultiProfile.UserProfile getProfile() {
        AnalyticsApplication.sendAnalytics(Utils.ACTION_STARTED_TEST);

        try {
            return buildProfile().getProfile(this);
        } catch (InvalidFieldException ex) {
            handleInvalidFieldException(ex);
            return null;
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
