package com.gianlu.aria2app.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.gianlu.aria2app.Activities.EditProfile.AuthenticationFragment;
import com.gianlu.aria2app.Activities.EditProfile.ConnectionFragment;
import com.gianlu.aria2app.Activities.EditProfile.DirectDownloadFragment;
import com.gianlu.aria2app.Activities.EditProfile.FieldErrorFragment;
import com.gianlu.aria2app.Activities.EditProfile.GeneralFragment;
import com.gianlu.aria2app.Activities.EditProfile.InvalidFieldException;
import com.gianlu.aria2app.Adapters.PagerAdapter;
import com.gianlu.aria2app.ProfilesManager.BaseProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.IOException;
import java.util.Objects;

public class EditProfileActivity extends AppCompatActivity {
    private UserProfile editProfile;
    private GeneralFragment generalFragment;
    private ConnectionFragment connectionFragment;
    private AuthenticationFragment authFragment;
    private DirectDownloadFragment ddFragment;
    private ViewPager pager;
    private PagerAdapter<FieldErrorFragment> pagerAdapter;

    public static void start(Context context, boolean firstProfile) {
        context.startActivity(new Intent(context, EditProfileActivity.class)
                .putExtra("firstProfile", firstProfile));
    }

    public static void start(Context context, BaseProfile edit) {
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
        if (bar != null)
            bar.setDisplayHomeAsUpEnabled(!getIntent().getBooleanExtra("firstProfile", true));

        BaseProfile editBase = (BaseProfile) getIntent().getSerializableExtra("edit");
        if (editBase instanceof UserProfile) {
            editProfile = (UserProfile) editBase;
        } else {
            // TODO: SHIITTTT
        }

        if (editProfile == null) setTitle(R.string.addProfile);
        else setTitle(R.string.editProfile);

        pager = (ViewPager) findViewById(R.id.editProfile_pager);
        pager.setOffscreenPageLimit(3);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.editProfile_tabs);
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

        generalFragment = GeneralFragment.getInstance(this, editProfile);
        connectionFragment = ConnectionFragment.getInstance(this, editProfile);
        authFragment = AuthenticationFragment.getInstance(this, editProfile);
        ddFragment = DirectDownloadFragment.getInstance(this, editProfile);

        pagerAdapter = new PagerAdapter<>(getSupportFragmentManager(), generalFragment, connectionFragment, authFragment, ddFragment);
        pager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(pager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_profile, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (editProfile == null) menu.findItem(R.id.editProfile_delete).setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    private void done() {
        try {
            GeneralFragment.Fields generalFields = generalFragment.getFields();
            ConnectionFragment.Fields connFields = connectionFragment.getFields(false);
            AuthenticationFragment.Fields authFields = authFragment.getFields();
            DirectDownloadFragment.Fields ddFields = ddFragment.getFields();

            UserProfile profile = new UserProfile(generalFields, connFields, authFields, ddFields);
            ProfilesManager manager = ProfilesManager.get(this);
            manager.save(profile);
            if (editProfile != null && !Objects.equals(editProfile.id, profile.id))
                manager.delete(editProfile);
            onBackPressed();
        } catch (InvalidFieldException ex) {
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
            case R.id.editProfile_done:
                done();
                break;
        }

        return true;
    }
}
