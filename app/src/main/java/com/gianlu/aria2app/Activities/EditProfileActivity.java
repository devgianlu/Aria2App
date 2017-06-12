package com.gianlu.aria2app.Activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.gianlu.aria2app.Activities.EditProfile.AuthenticationFragment;
import com.gianlu.aria2app.Activities.EditProfile.ConnectionFragment;
import com.gianlu.aria2app.Activities.EditProfile.DirectDownloadFragment;
import com.gianlu.aria2app.Activities.EditProfile.FieldErrorFragment;
import com.gianlu.aria2app.Activities.EditProfile.InvalidFieldException;
import com.gianlu.aria2app.Activities.EditProfile.SpinnerAdapter;
import com.gianlu.aria2app.Adapters.PagerAdapter;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// TODO: Delete condition
// TODO: Set default condition
public class EditProfileActivity extends AppCompatActivity {
    private MultiProfile editProfile;
    private TextInputLayout profileName;
    private CheckBox enableNotifs;
    private List<ConnectionFragment> connectionFragments;
    private List<AuthenticationFragment> authFragments;
    private List<DirectDownloadFragment> ddFragments;
    private ViewPager pager;
    private PagerAdapter<FieldErrorFragment> pagerAdapter;
    private Spinner conditionsSpinner;
    private TabLayout tabLayout;
    private List<MultiProfile.ConnectivityCondition> conditions;

    public static void start(Context context, boolean firstProfile) {
        context.startActivity(new Intent(context, EditProfileActivity.class)
                .putExtra("firstProfile", firstProfile));
    }

    public static void start(Context context, MultiProfile edit) {
        context.startActivity(new Intent(context, EditProfileActivity.class)
                .putExtra("firstProfile", false)
                .putExtra("edit", edit));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = (Toolbar) findViewById(R.id.editProfile_toolbar);
        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(!getIntent().getBooleanExtra("firstProfile", true));
            bar.setDisplayShowTitleEnabled(false);
        }

        editProfile = (MultiProfile) getIntent().getSerializableExtra("edit");

        if (editProfile == null) setTitle(R.string.addProfile);
        else setTitle(R.string.editProfile);

        profileName = (TextInputLayout) findViewById(R.id.editProfile_profileName);
        enableNotifs = (CheckBox) findViewById(R.id.editProfile_enableNotifs);

        if (editProfile != null) {
            //noinspection ConstantConditions
            profileName.getEditText().setText(editProfile.name);
            enableNotifs.setChecked(editProfile.notificationsEnabled);
        }

        pager = (ViewPager) findViewById(R.id.editProfile_pager);
        pager.setOffscreenPageLimit(3);

        tabLayout = (TabLayout) findViewById(R.id.editProfile_tabs);
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
        if (editProfile == null) {
            createNewProfile(true);
        }
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
            pagerAdapter = new PagerAdapter<>(getSupportFragmentManager(), connectionFragments.get(pos), authFragments.get(pos), ddFragments.get(pos));
            pager.setAdapter(pagerAdapter);
            tabLayout.setupWithViewPager(pager);
            pager.setCurrentItem(tabPos, false);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.editProfile_delete).setVisible(editProfile != null);
        MenuItem spinnerItem = menu.findItem(R.id.editProfile_profiles);
        conditionsSpinner = (Spinner) spinnerItem.getActionView();
        conditionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showFragmentsAt(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        refreshSpinner();

        return true;
    }

    private void createNewProfile(boolean compulsory) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.new_profile_dialog, null, false);
        final RadioGroup connectivityCondition = (RadioGroup) layout.findViewById(R.id.editProfile_connectivityCondition);
        final TextInputLayout ssid = (TextInputLayout) layout.findViewById(R.id.editProfile_ssid);
        ssid.getEditText().addTextChangedListener(new TextWatcher() {
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
                if (checkedId == R.id.editProfile_connectivityCondition_wifi)
                    ssid.setVisibility(View.VISIBLE);
                else
                    ssid.setVisibility(View.GONE);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.newProfile)
                .setView(layout)
                .setCancelable(!compulsory)
                .setPositiveButton(R.string.create, null);

        if (!compulsory) builder.setNegativeButton(android.R.string.cancel, null);

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
                                condition = MultiProfile.ConnectivityCondition.newMobileCondition(true);
                                break;
                            case R.id.editProfile_connectivityCondition_wifi:
                                if (ssid.getEditText().getText().toString().isEmpty()) {
                                    ssid.setError(getString(R.string.emptySSID));
                                    return;
                                }

                                condition = MultiProfile.ConnectivityCondition.newWiFiCondition(ssid.getEditText().getText().toString(), true);
                                break;
                            case R.id.editProfile_connectivityCondition_ethernet:
                                condition = MultiProfile.ConnectivityCondition.newEthernetCondition(true);
                                break;
                            case R.id.editProfile_connectivityCondition_bluetooth:
                                condition = MultiProfile.ConnectivityCondition.newBluetoothCondition(true);
                                break;
                        }

                        if (conditions.contains(condition)) {
                            CommonUtils.UIToast(EditProfileActivity.this, Utils.ToastMessages.DUPLICATED_CONDITION);
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

        CommonUtils.showDialog(this, dialog);
    }

    private void refreshSpinner() {
        conditionsSpinner.setAdapter(new SpinnerAdapter(this, conditions));
    }

    private void doneAll() {
        try {
            String profileName = this.profileName.getEditText().getText().toString().trim();
            if (profileName.isEmpty() ||
                    (ProfilesManager.get(this).profileExists(ProfilesManager.getId(profileName))
                            && editProfile == null)) {
                throw new InvalidFieldException(getClass(), R.id.editProfile_profileName, getString(R.string.invalidProfileName));
            }

            MultiProfile profile = new MultiProfile(profileName, enableNotifs.isChecked());
            for (int i = 0; i < conditions.size(); i++) {
                ConnectionFragment.Fields connFields = connectionFragments.get(i).getFields(false);
                AuthenticationFragment.Fields authFields = authFragments.get(i).getFields();
                DirectDownloadFragment.Fields ddFields = ddFragments.get(i).getFields();

                profile.add(conditions.get(i), connFields, authFields, ddFields);
            }

            ProfilesManager manager = ProfilesManager.get(this);
            manager.save(profile);
            if (editProfile != null && !Objects.equals(editProfile.id, profile.id))
                manager.delete(editProfile);
            onBackPressed();
        } catch (InvalidFieldException ex) {
            if (ex.fragmentClass == MainActivity.class) {
                ((TextInputLayout) findViewById(ex.fieldId)).setError(ex.reason);
                return;
            }

            int pos = pagerAdapter.indexOf(ex.fragmentClass);
            if (pos != -1) {
                pager.setCurrentItem(pos, true);
                pagerAdapter.getItem(pos).onFieldError(ex.fieldId, ex.reason);
            }
        } catch (JSONException | IOException ex) {
            CommonUtils.UIToast(this, Utils.ToastMessages.CANNOT_SAVE_PROFILE, ex);
        }

        ThisApplication.sendAnalytics(this, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_NEW_PROFILE)
                .build());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.editProfile_delete:
                if (editProfile == null) break;

                ThisApplication.sendAnalytics(this, new HitBuilders.EventBuilder()
                        .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                        .setAction(ThisApplication.ACTION_DELETE_PROFILE)
                        .build());

                ProfilesManager.get(this).delete(editProfile);
                onBackPressed();
                break;
            case R.id.editProfile_createNew:
                createNewProfile(true);
                break;
            case R.id.editProfile_doneAll:
                doneAll();
                break;
        }

        return true;
    }
}
