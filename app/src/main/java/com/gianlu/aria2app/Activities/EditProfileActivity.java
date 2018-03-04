package com.gianlu.aria2app.Activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.gianlu.aria2app.Activities.EditProfile.AuthenticationFragment;
import com.gianlu.aria2app.Activities.EditProfile.ConnectionFragment;
import com.gianlu.aria2app.Activities.EditProfile.DirectDownloadFragment;
import com.gianlu.aria2app.Activities.EditProfile.FieldErrorFragment;
import com.gianlu.aria2app.Activities.EditProfile.InvalidFieldException;
import com.gianlu.aria2app.Activities.EditProfile.TestFragment;
import com.gianlu.aria2app.Activities.EditProfile.WifisAdapter;
import com.gianlu.aria2app.Adapters.PagerAdapter;
import com.gianlu.aria2app.Adapters.RadioConditionsAdapter;
import com.gianlu.aria2app.Adapters.SpinnerConditionsAdapter;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EditProfileActivity extends ActivityWithDialog implements TestFragment.IGetProfile {
    private MultiProfile editProfile;
    private TextInputLayout profileName;
    private CheckBox enableNotifs;
    private List<MultiProfile.ConnectivityCondition> conditions;
    private List<ConnectionFragment> connectionFragments;
    private List<AuthenticationFragment> authFragments;
    private List<DirectDownloadFragment> ddFragments;
    private TestFragment testFragment;
    private ViewPager pager;
    private PagerAdapter<Fragment> pagerAdapter;
    private Spinner conditionsSpinner;
    private TabLayout tabLayout;

    public static void start(Context context, boolean firstProfile) {
        context.startActivity(new Intent(context, EditProfileActivity.class)
                .putExtra("firstProfile", firstProfile));
    }

    public static void start(Context context, MultiProfile edit) {
        context.startActivity(new Intent(context, EditProfileActivity.class)
                .putExtra("firstProfile", false)
                .putExtra("edit", edit));
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

        editProfile = (MultiProfile) getIntent().getSerializableExtra("edit");

        if (editProfile == null) setTitle(R.string.addProfile);
        else setTitle(R.string.editProfile);

        profileName = findViewById(R.id.editProfile_profileName);
        CommonUtils.getEditText(profileName).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                profileName.setErrorEnabled(false);
            }
        });
        enableNotifs = findViewById(R.id.editProfile_enableNotifs);

        if (editProfile != null) {
            //noinspection ConstantConditions
            profileName.getEditText().setText(editProfile.name);
            enableNotifs.setChecked(editProfile.notificationsEnabled);
        }

        conditionsSpinner = findViewById(R.id.editProfile_conditionsSpinner);
        conditionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showFragmentsAt(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        pager = findViewById(R.id.editProfile_pager);
        pager.setOffscreenPageLimit(4);

        tabLayout = findViewById(R.id.editProfile_tabs);
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

        connectionFragments = new ArrayList<>();
        authFragments = new ArrayList<>();
        ddFragments = new ArrayList<>();

        createAllFragments();
        if (editProfile == null) createNewCondition(true);
        refreshSpinner();
    }

    private void createAllFragments() {
        conditions = new ArrayList<>();

        if (editProfile != null) {
            connectionFragments.clear();
            authFragments.clear();
            ddFragments.clear();

            for (MultiProfile.UserProfile profile : editProfile.profiles) {
                conditions.add(profile.connectivityCondition);
                connectionFragments.add(ConnectionFragment.getInstance(this, profile));
                authFragments.add(AuthenticationFragment.getInstance(this, profile));
                ddFragments.add(DirectDownloadFragment.getInstance(this, profile));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_profile, menu);
        return true;
    }

    private void showFragmentsAt(int pos) {
        int tabPos = pager.getCurrentItem();
        if (pos != -1) {
            if (testFragment == null) testFragment = TestFragment.getInstance(this, this);
            pagerAdapter = new PagerAdapter<>(getSupportFragmentManager(), connectionFragments.get(pos), authFragments.get(pos), ddFragments.get(pos), testFragment);
            pager.setAdapter(pagerAdapter);
            tabLayout.setupWithViewPager(pager);
            pager.setCurrentItem(tabPos, false);
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
            Toaster.show(this, Utils.Messages.HAS_ALWAYS_CONDITION);
            return;
        }

        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_new_condition, null, false);
        final RadioGroup connectivityCondition = layout.findViewById(R.id.editProfile_connectivityCondition);
        final TextInputLayout ssid = layout.findViewById(R.id.editProfile_ssid);
        final MultiAutoCompleteTextView ssidField = (MultiAutoCompleteTextView) CommonUtils.getEditText(ssid);
        ssidField.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        ssidField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                ssid.setErrorEnabled(false);
            }
        });
        connectivityCondition.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (checkedId == R.id.editProfile_connectivityCondition_wifi) {
                    WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    if (manager == null) return;
                    ssidField.setAdapter(new WifisAdapter(EditProfileActivity.this, manager.getConfiguredNetworks()));
                    ssidField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                    ssidField.setThreshold(1);
                    ssid.setVisibility(View.VISIBLE);
                } else {
                    ssid.setVisibility(View.GONE);
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.newCondition)
                .setView(layout)
                .setCancelable(!compulsory)
                .setPositiveButton(R.string.create, null);

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (compulsory) onBackPressed();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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
                            Toaster.show(EditProfileActivity.this, Utils.Messages.CANNOT_ADD_ALWAYS);
                            return;
                        }

                        if (conditions.contains(condition)) {
                            Toaster.show(EditProfileActivity.this, Utils.Messages.DUPLICATED_CONDITION);
                            return;
                        }

                        conditions.add(condition);
                        connectionFragments.add(ConnectionFragment.getInstance(EditProfileActivity.this, null));
                        authFragments.add(AuthenticationFragment.getInstance(EditProfileActivity.this, null));
                        ddFragments.add(DirectDownloadFragment.getInstance(EditProfileActivity.this, null));
                        refreshSpinner();
                        conditionsSpinner.setSelection(conditions.size() - 1);
                        dialog.dismiss();
                    }
                });
            }
        });

        showDialog(dialog);
    }

    private void refreshSpinner() {
        conditionsSpinner.setAdapter(new SpinnerConditionsAdapter(this, conditions));
    }

    @Nullable
    private MultiProfile buildProfile() throws InvalidFieldException {
        String profileName = CommonUtils.getText(this.profileName).trim();
        if (profileName.isEmpty() ||
                (ProfilesManager.get(this).profileExists(ProfilesManager.getId(profileName))
                        && editProfile == null)) {
            throw new InvalidFieldException(MainActivity.class, R.id.editProfile_profileName, getString(R.string.invalidProfileName));
        }

        MultiProfile profile = new MultiProfile(profileName, enableNotifs.isChecked());
        for (int i = 0; i < conditions.size(); i++) {
            ConnectionFragment.Fields connFields = connectionFragments.get(i).getFields(this, false);
            AuthenticationFragment.Fields authFields = authFragments.get(i).getFields(this);
            DirectDownloadFragment.Fields ddFields = ddFragments.get(i).getFields(this);

            if (connFields == null || authFields == null || ddFields == null) return null;
            profile.add(conditions.get(i), connFields, authFields, ddFields);
        }

        return profile;
    }

    private void doneAll() {
        try {
            MultiProfile profile = buildProfile();
            if (profile == null) {
                Toaster.show(this, Utils.Messages.CANNOT_SAVE_PROFILE);
                return;
            }

            ProfilesManager manager = ProfilesManager.get(this);
            manager.save(profile);
            if (editProfile != null && !Objects.equals(editProfile.id, profile.id))
                manager.delete(editProfile);
            onBackPressed();
        } catch (InvalidFieldException ex) {
            handleInvalidFieldException(ex);
        } catch (JSONException | IOException ex) {
            Toaster.show(this, Utils.Messages.CANNOT_SAVE_PROFILE, ex);
        }

        AnalyticsApplication.sendAnalytics(this, Utils.ACTION_NEW_PROFILE);
    }

    private void handleInvalidFieldException(InvalidFieldException ex) {
        if (ex.fragmentClass == MainActivity.class) {
            TextInputLayout field = findViewById(ex.fieldId);
            field.clearFocus();
            field.setErrorEnabled(true);
            field.setError(ex.reason);
            return;
        }

        int pos = pagerAdapter.indexOf(ex.fragmentClass);
        if (pos != -1) {
            pager.setCurrentItem(pos, true);
            Fragment fragment = pagerAdapter.getItem(pos);
            if (fragment instanceof FieldErrorFragment)
                ((FieldErrorFragment) fragment).onFieldError(ex.fieldId, ex.reason);
        }
    }

    private void deleteProfile() {
        if (editProfile == null) return;

        AnalyticsApplication.sendAnalytics(this, Utils.ACTION_DELETE_PROFILE);

        ProfilesManager.get(this).delete(editProfile);
        onBackPressed();
    }

    private void deleteCondition(int position) {
        conditions.remove(position);
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
                .setSingleChoiceItems(new RadioConditionsAdapter(this, conditions), def, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDefaultCondition(which);
                        dialog.dismiss();
                    }
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
        AnalyticsApplication.sendAnalytics(this, Utils.ACTION_STARTED_TEST);

        try {
            MultiProfile profile = buildProfile();
            return profile == null ? null : profile.getProfile(this);
        } catch (InvalidFieldException ex) {
            handleInvalidFieldException(ex);
            return null;
        }
    }
}
