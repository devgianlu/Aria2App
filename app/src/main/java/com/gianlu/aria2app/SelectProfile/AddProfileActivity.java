package com.gianlu.aria2app.SelectProfile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.SelectProfileActivity;
import com.gianlu.aria2app.Utils;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AddProfileActivity extends AppCompatActivity {
    private Activity _this;
    private boolean isEditMode = true;

    private EditText profileName;
    private RadioGroup modeGroup;
    private ScrollView singleModeContainer;
    private RelativeLayout multiModeContainer;

    // Single mode
    private EditText sAddr;
    private EditText sPort;
    private EditText sEndpoint;
    private TextView sCompleteURL;
    private Switch sAuth;
    private TextView sTokenLabel;
    private EditText sToken;
    private CheckBox sSSL;
    private CheckBox sDirectDownload;
    private RelativeLayout sDirectDownloadContainer;
    private EditText sDirectDownloadAddr;
    private Switch sDirectDownloadAuth;
    private TextView sDirectDownloadUsernameLabel;
    private EditText sDirectDownloadUsername;
    private TextView sDirectDownloadPasswordLabel;
    private EditText sDirectDownloadPassword;

    // Multi mode
    private Map<ConnectivityCondition, SingleModeProfileItem> mProfiles = new ArrayMap<>();
    private ListView mListView;
    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_profile);
        _this = this;

        profileName = (EditText) findViewById(R.id.addProfile_name);
        assert profileName != null;
        modeGroup = (RadioGroup) findViewById(R.id.addProfile_modeGroup);
        assert modeGroup != null;
        singleModeContainer = (ScrollView) findViewById(R.id.addProfile_singleModeContainer);
        assert singleModeContainer != null;
        multiModeContainer = (RelativeLayout) findViewById(R.id.addProfile_multiModeContainer);
        assert multiModeContainer != null;

        modeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.addProfile_singleMode) {
                    singleModeContainer.setVisibility(View.VISIBLE);
                    multiModeContainer.setVisibility(View.INVISIBLE);
                } else {
                    multiModeContainer.setVisibility(View.VISIBLE);
                    singleModeContainer.setVisibility(View.INVISIBLE);
                }
            }
        });

        // Single mode
        SingleOnTextChangedListener sListener = new SingleOnTextChangedListener();
        sAddr = (EditText) findViewById(R.id.addProfile_serverAddr);
        assert sAddr != null;
        sAddr.addTextChangedListener(sListener);
        sPort = (EditText) findViewById(R.id.addProfile_serverPort);
        assert sPort != null;
        sPort.addTextChangedListener(sListener);
        sEndpoint = (EditText) findViewById(R.id.addProfile_serverEndpoint);
        assert sEndpoint != null;
        sEndpoint.addTextChangedListener(sListener);
        sCompleteURL = (TextView) findViewById(R.id.addProfile_completeURL);
        assert sCompleteURL != null;
        sTokenLabel = (TextView) findViewById(R.id.addProfile_tokenLabel);
        assert sTokenLabel != null;
        sToken = (EditText) findViewById(R.id.addProfile_serverToken);
        assert sToken != null;
        sAuth = (Switch) findViewById(R.id.addProfile_serverAuth);
        assert sAuth != null;
        sAuth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    sToken.setVisibility(View.VISIBLE);
                    sTokenLabel.setVisibility(View.VISIBLE);
                } else {
                    sToken.setVisibility(View.INVISIBLE);
                    sToken.setText("");
                    sTokenLabel.setVisibility(View.INVISIBLE);
                }
            }
        });
        sSSL = (CheckBox) findViewById(R.id.addProfile_serverSSL);
        assert sSSL != null;
        sDirectDownload = (CheckBox) findViewById(R.id.addProfile_directDownload);
        assert sDirectDownload != null;
        sDirectDownloadContainer = (RelativeLayout) findViewById(R.id.addProfile_directDownloadContainer);
        assert sDirectDownloadContainer != null;
        sDirectDownloadAddr = (EditText) sDirectDownloadContainer.findViewById(R.id.addProfile_directDownload_addr);
        assert sDirectDownloadAddr != null;
        sDirectDownloadAuth = (Switch) sDirectDownloadContainer.findViewById(R.id.addProfile_directDownload_auth);
        assert sDirectDownloadAuth != null;
        sDirectDownloadUsernameLabel = (TextView) findViewById(R.id.addProfile_directDownload_userLabel);
        assert sDirectDownloadUsernameLabel != null;
        sDirectDownloadUsername = (EditText) sDirectDownloadContainer.findViewById(R.id.addProfile_directDownload_user);
        assert sDirectDownloadUsername != null;
        sDirectDownloadPasswordLabel = (TextView) findViewById(R.id.addProfile_directDownload_passwdLabel);
        assert sDirectDownloadPasswordLabel != null;
        sDirectDownloadPassword = (EditText) sDirectDownloadContainer.findViewById(R.id.addProfile_directDownload_passwd);
        assert sDirectDownloadPassword != null;
        sDirectDownload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    sDirectDownloadContainer.setVisibility(View.VISIBLE);
                } else {
                    sDirectDownloadContainer.setVisibility(View.INVISIBLE);
                }
            }
        });
        sDirectDownloadAuth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    sDirectDownloadUsernameLabel.setVisibility(View.VISIBLE);
                    sDirectDownloadUsername.setVisibility(View.VISIBLE);
                    sDirectDownloadPassword.setVisibility(View.VISIBLE);
                    sDirectDownloadPasswordLabel.setVisibility(View.VISIBLE);
                } else {
                    sDirectDownloadUsernameLabel.setVisibility(View.INVISIBLE);
                    sDirectDownloadUsername.setVisibility(View.INVISIBLE);
                    sDirectDownloadPassword.setVisibility(View.INVISIBLE);
                    sDirectDownloadPasswordLabel.setVisibility(View.INVISIBLE);
                }
            }
        });


        // Multi mode
        mListView = (ListView) findViewById(R.id.addProfile_multiModeListView);
        assert mListView != null;
        mSpinner = (Spinner) findViewById(R.id.addProfile_multiModeSpinner);
        assert mSpinner != null;
        Button mAdd = (Button) findViewById(R.id.addProfile_multiModeAdd);
        assert mAdd != null;

        reloadSpinner();
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewCondition(ConnectivityCondition.getTypeFromString(mSpinner.getSelectedItem().toString()), null);
            }
        });

        Bundle edit = getIntent().getExtras();
        if (edit.getBoolean("edit", false)) {
            setTitle(R.string.title_activity_addProfile_edit);
            isEditMode = true;
            try {
                if (edit.getBoolean("isSingleMode", true)) {
                    modeGroup.check(R.id.addProfile_singleMode);
                    loadSingle(edit.getString("name"));
                } else {
                    modeGroup.check(R.id.addProfile_multiMode);
                    loadMulti(edit.getString("name"));
                }

                modeGroup.getChildAt(0).setEnabled(false);
                modeGroup.getChildAt(1).setEnabled(false);
            } catch (FileNotFoundException ex) {
                Utils.UIToast(this, Utils.TOAST_MESSAGES.FILE_NOT_FOUND);
                onBackPressed();
            } catch (IOException | JSONException ex) {
                Utils.UIToast(this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex.getMessage());
                ex.printStackTrace();
                onBackPressed();
            }
        } else {
            setTitle(R.string.title_activity_addProfile_new);
            isEditMode = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_profile, menu);
        if (!isEditMode) menu.getItem(0).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.addProfileMenu_done:
                if (modeGroup.getCheckedRadioButtonId() == R.id.addProfile_singleMode)
                    createSingle();
                else
                    createMulti();
                break;
            case R.id.addProfileMenu_delete:
                if (Analytics.isTrackingAllowed(this))
                    Analytics.getDefaultTracker(getApplication()).send(new HitBuilders.EventBuilder()
                            .setCategory(Analytics.CATEGORY_USER_INPUT)
                            .setAction(Analytics.ACTION_DELETE_PROFILE)
                            .build());
                deleteFile(profileName.getText().toString().trim() + ".profile");
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSingle(String name) throws IOException, JSONException {
        SingleModeProfileItem item = SingleModeProfileItem.fromString(this, name);

        profileName.setText(item.getProfileName());
        sAddr.setText(item.getServerAddr());
        sPort.setText(String.valueOf(item.getServerPort()));
        sEndpoint.setText(item.getServerEndpoint());
        sAuth.setChecked(item.isServerAuth());
        sToken.setText(item.getServerToken());
        sSSL.setChecked(item.isServerSSL());

        sDirectDownload.setChecked(item.isDirectDownloadEnabled());
        if (item.isDirectDownloadEnabled()) {
            sDirectDownloadAddr.setText(item.getDirectDownload().getAddress());
            sDirectDownloadAuth.setChecked(item.getDirectDownload().isAuth());
            sDirectDownloadUsername.setText(item.getDirectDownload().getUsername());
            sDirectDownloadPassword.setText(item.getDirectDownload().getPassword());
        }
    }

    private void loadMulti(String name) throws IOException, JSONException {
        MultiModeProfileItem item = MultiModeProfileItem.fromString(this, name);

        profileName.setText(item.getProfileName());
        mProfiles = item.getProfiles();
        mListView.setAdapter(new ConditionsCustomAdapter(this, mProfiles, new ConditionsCustomAdapter.OnClickListener() {
            @Override
            public void onClick(View view, int i, SingleModeProfileItem item, ConnectivityCondition condition) {
                createNewCondition(condition.getType(), new Pair<>(condition, item));
            }
        }));
        reloadSpinner();
    }

    private void createNewCondition(final ConnectivityCondition.TYPE type, final Pair<ConnectivityCondition, SingleModeProfileItem> edit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") final RelativeLayout view = (RelativeLayout) getLayoutInflater().inflate(R.layout.new_condition_dialog, null);

        AutoCompleteTextView ssid = null;
        switch (type) {
            case WIFI:
                view.findViewById(R.id.newConditionDialog_containerWiFi).setVisibility(View.VISIBLE);
                ssid = (AutoCompleteTextView) view.findViewById(R.id.newConditionDialog_containerWiFi).findViewById(R.id.newConditionDialog_ssid);

                List<String> configuredSSIDs = new ArrayList<>();
                List<WifiConfiguration> configured_wifis = ((WifiManager) getSystemService(WIFI_SERVICE)).getConfiguredNetworks();
                if (configured_wifis == null) break;
                for (WifiConfiguration net : configured_wifis) {
                    String _ssid = net.SSID;
                    if (_ssid.startsWith("\"") && _ssid.endsWith("\"")) {
                        _ssid = _ssid.substring(1, _ssid.length() - 1);
                    }
                    configuredSSIDs.add(_ssid);
                }

                ssid.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, configuredSSIDs));

                if (edit != null) ssid.setText(edit.first.getSSID());
                break;
            case MOBILE:
            case ETHERNET:
            case BLUETOOTH:
                view.removeView(view.findViewById(R.id.newConditionDialog_containerWiFi));
                break;
        }

        final ScrollView include = (ScrollView) view.findViewById(R.id.newConditionDialog_include);
        final TextView mTokenLabel = (TextView) include.findViewById(R.id.addProfile_tokenLabel);
        final EditText mToken = (EditText) include.findViewById(R.id.addProfile_serverToken);
        final TextView mCompleteURL = (TextView) include.findViewById(R.id.addProfile_completeURL);
        final Switch mAuth = (Switch) include.findViewById(R.id.addProfile_serverAuth);
        final EditText mAddr = (EditText) include.findViewById(R.id.addProfile_serverAddr);
        final EditText mPort = (EditText) include.findViewById(R.id.addProfile_serverPort);
        final EditText mEndpoint = (EditText) include.findViewById(R.id.addProfile_serverEndpoint);
        final CheckBox mSSL = (CheckBox) include.findViewById(R.id.addProfile_serverSSL);
        final CheckBox mDirectDownload = (CheckBox) include.findViewById(R.id.addProfile_directDownload);
        final RelativeLayout mDirectDownloadContainer = (RelativeLayout) view.findViewById(R.id.addProfile_directDownloadContainer);
        assert mDirectDownloadContainer != null;
        final EditText mDirectDownloadAddr = (EditText) mDirectDownloadContainer.findViewById(R.id.addProfile_directDownload_addr);
        final Switch mDirectDownloadAuth = (Switch) mDirectDownloadContainer.findViewById(R.id.addProfile_directDownload_auth);
        final TextView mDirectDownloadUsernameLabel = (TextView) mDirectDownloadContainer.findViewById(R.id.addProfile_directDownload_userLabel);
        final EditText mDirectDownloadUsername = (EditText) mDirectDownloadContainer.findViewById(R.id.addProfile_directDownload_user);
        final TextView mDirectDownloadPasswordLabel = (TextView) mDirectDownloadContainer.findViewById(R.id.addProfile_directDownload_passwdLabel);
        final EditText mDirectDownloadPassword = (EditText) mDirectDownloadContainer.findViewById(R.id.addProfile_directDownload_passwd);

        MultiOnTextChangedListener listener = new MultiOnTextChangedListener(mCompleteURL, mAddr, mPort, mEndpoint);
        mAddr.addTextChangedListener(listener);
        mPort.addTextChangedListener(listener);
        mEndpoint.addTextChangedListener(listener);
        mAuth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mToken.setVisibility(View.VISIBLE);
                    mTokenLabel.setVisibility(View.VISIBLE);
                } else {
                    mToken.setVisibility(View.INVISIBLE);
                    mToken.setText("");
                    mTokenLabel.setVisibility(View.INVISIBLE);
                }
            }
        });
        mDirectDownload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mDirectDownloadContainer.setVisibility(View.VISIBLE);
                } else {
                    mDirectDownloadContainer.setVisibility(View.INVISIBLE);
                }
            }
        });
        mDirectDownloadAuth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mDirectDownloadUsernameLabel.setVisibility(View.VISIBLE);
                    mDirectDownloadUsername.setVisibility(View.VISIBLE);
                    mDirectDownloadPassword.setVisibility(View.VISIBLE);
                    mDirectDownloadPasswordLabel.setVisibility(View.VISIBLE);
                } else {
                    mDirectDownloadUsernameLabel.setVisibility(View.INVISIBLE);
                    mDirectDownloadUsername.setVisibility(View.INVISIBLE);
                    mDirectDownloadPassword.setVisibility(View.INVISIBLE);
                    mDirectDownloadPasswordLabel.setVisibility(View.INVISIBLE);
                }
            }
        });

        builder.setPositiveButton(edit != null ? R.string.save : R.string.add, null)
                .setTitle(type.getFormal())
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setView(view);

        if (edit != null) {
            builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mProfiles.remove(edit.first);

                    mListView.setAdapter(new ConditionsCustomAdapter(_this, mProfiles, new ConditionsCustomAdapter.OnClickListener() {
                        @Override
                        public void onClick(View view, int i, SingleModeProfileItem item, ConnectivityCondition condition) {
                            createNewCondition(condition.getType(), new Pair<>(condition, item));
                        }
                    }));

                    reloadSpinner();

                    dialogInterface.dismiss();
                }
            });

            mAddr.setText(edit.second.getServerAddr());
            mPort.setText(String.valueOf(edit.second.getServerPort()));
            mEndpoint.setText(edit.second.getServerEndpoint());
            mCompleteURL.setText(edit.second.getFullServerAddr());
            mAuth.setChecked(edit.second.isServerAuth());
            mToken.setText(edit.second.getServerToken());
            mSSL.setChecked(edit.second.isServerSSL());
            mDirectDownload.setChecked(edit.second.isDirectDownloadEnabled());
            if (edit.second.isDirectDownloadEnabled()) {
                mDirectDownloadAddr.setText(edit.second.getDirectDownload().getAddress());
                mDirectDownloadAuth.setChecked(edit.second.getDirectDownload().isAuth());
                mDirectDownloadUsername.setText(edit.second.getDirectDownload().getUsername());
                mDirectDownloadPassword.setText(edit.second.getDirectDownload().getPassword());
            }
        }

        final AlertDialog dialog = builder.create();
        dialog.show();
        final AutoCompleteTextView finalSsid = ssid;
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityCondition condition = null;
                switch (type) {
                    case WIFI:
                        if (finalSsid.getText().toString().trim().isEmpty()) {
                            Utils.UIToast(_this, Utils.TOAST_MESSAGES.INVALID_SSID);
                            return;
                        }
                        condition = ConnectivityCondition.newWiFiCondition(finalSsid.getText().toString().trim());
                        break;
                    case MOBILE:
                        condition = ConnectivityCondition.newMobileCondition();
                        break;
                    case ETHERNET:
                        condition = ConnectivityCondition.newEthernetCondition();
                        break;
                    case BLUETOOTH:
                        condition = ConnectivityCondition.newBluetoothCondition();
                        break;
                }

                if (condition == null) {
                    Utils.UIToast(_this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, "CONDITION is null");
                    return;
                }

                if (mAddr.getText().toString().trim().isEmpty()) {
                    Utils.UIToast(_this, Utils.TOAST_MESSAGES.INVALID_SERVER_IP);
                    return;
                }

                if (!mPort.getText().toString().trim().isEmpty()) {
                    if (0 > Integer.parseInt(mPort.getText().toString()) && Integer.parseInt(mPort.getText().toString()) < 65536) {
                        Utils.UIToast(_this, Utils.TOAST_MESSAGES.INVALID_SERVER_PORT);
                        return;
                    }
                } else {
                    Utils.UIToast(_this, Utils.TOAST_MESSAGES.INVALID_SERVER_PORT);
                    return;
                }

                if (sEndpoint.getText().toString().trim().isEmpty() || (!mEndpoint.getText().toString().startsWith("/"))) {
                    Utils.UIToast(_this, Utils.TOAST_MESSAGES.INVALID_SERVER_ENDPOINT);
                    return;
                }

                if (mAuth.isChecked() && mToken.getText().toString().trim().isEmpty()) {
                    Utils.UIToast(_this, Utils.TOAST_MESSAGES.INVALID_SERVER_TOKEN);
                    return;
                }

                if (mDirectDownload.isChecked() && mDirectDownloadAddr.getText().toString().trim().isEmpty()) {
                    Utils.UIToast(_this, Utils.TOAST_MESSAGES.INVALID_DIRECTDOWNLOAD_ADDR);
                    return;
                }

                if (mDirectDownload.isChecked() && (!mDirectDownloadAddr.getText().toString().trim().endsWith("/"))) {
                    mDirectDownloadAddr.setText(String.format("%s/", mDirectDownloadAddr.getText().toString().trim()));
                }

                if (mDirectDownload.isChecked() && mDirectDownloadAuth.isChecked() && (mDirectDownloadUsername.getText().toString().trim().isEmpty() || mDirectDownloadPassword.getText().toString().trim().isEmpty())) {
                    Utils.UIToast(_this, Utils.TOAST_MESSAGES.INVALID_DIRECTDOWNLOAD_USERORPASSWD);
                    return;
                }

                SingleModeProfileItem profile = new SingleModeProfileItem(condition.getFormalName(),
                        mAddr.getText().toString().trim(),
                        Integer.parseInt(mPort.getText().toString().trim()),
                        mEndpoint.getText().toString().trim(),
                        mAuth.isChecked(),
                        mSSL.isChecked(),
                        mToken.getText().toString().trim(),
                        mDirectDownload.isChecked(),
                        new DirectDownload(mDirectDownloadAddr.getText().toString().trim(),
                                mDirectDownloadAuth.isChecked(),
                                mDirectDownloadUsername.getText().toString().trim(),
                                mDirectDownloadPassword.getText().toString().trim()));

                if (edit != null && edit.first != null) mProfiles.remove(edit.first);
                mProfiles.put(condition, profile);
                mListView.setAdapter(new ConditionsCustomAdapter(_this, mProfiles, new ConditionsCustomAdapter.OnClickListener() {
                    @Override
                    public void onClick(View view, int i, SingleModeProfileItem item, ConnectivityCondition condition) {
                        createNewCondition(condition.getType(), new Pair<>(condition, item));
                    }
                }));

                reloadSpinner();

                dialog.dismiss();
            }
        });
    }

    private void reloadSpinner() {
        List<ConnectivityCondition.TYPE> avConds = new ArrayList<>(
                Arrays.asList(
                        ConnectivityCondition.TYPE.WIFI,
                        ConnectivityCondition.TYPE.MOBILE,
                        ConnectivityCondition.TYPE.ETHERNET,
                        ConnectivityCondition.TYPE.BLUETOOTH));

        for (ConnectivityCondition _cond : mProfiles.keySet()) {
            if (_cond.getType() == ConnectivityCondition.TYPE.MOBILE)
                avConds.remove(ConnectivityCondition.TYPE.MOBILE);
            if (_cond.getType() == ConnectivityCondition.TYPE.BLUETOOTH)
                avConds.remove(ConnectivityCondition.TYPE.BLUETOOTH);
            if (_cond.getType() == ConnectivityCondition.TYPE.ETHERNET)
                avConds.remove(ConnectivityCondition.TYPE.ETHERNET);
        }

        mSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, avConds));
    }

    private void createSingle() {
        if (profileName.getText().toString().trim().isEmpty()) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.INVALID_PROFILE_NAME);
            return;
        }

        if (sAddr.getText().toString().trim().isEmpty()) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.INVALID_SERVER_IP);
            return;
        }

        if (!sPort.getText().toString().trim().isEmpty()) {
            if (0 > Integer.parseInt(sPort.getText().toString()) && Integer.parseInt(sPort.getText().toString()) < 65536) {
                Utils.UIToast(this, Utils.TOAST_MESSAGES.INVALID_SERVER_PORT);
                return;
            }
        } else {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.INVALID_SERVER_PORT);
            return;
        }

        if (sEndpoint.getText().toString().trim().isEmpty() || (!sEndpoint.getText().toString().startsWith("/"))) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.INVALID_SERVER_ENDPOINT);
            return;
        }

        if (sAuth.isChecked() && sToken.getText().toString().trim().isEmpty()) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.INVALID_SERVER_TOKEN);
            return;
        }

        if (sDirectDownload.isChecked() && sDirectDownloadAddr.getText().toString().trim().isEmpty()) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.INVALID_DIRECTDOWNLOAD_ADDR);
            return;
        }

        if (sDirectDownload.isChecked() && (!sDirectDownloadAddr.getText().toString().trim().endsWith("/"))) {
            sDirectDownloadAddr.setText(String.format("%s/", sDirectDownloadAddr.getText().toString().trim()));
        }

        if (sDirectDownload.isChecked() && sDirectDownloadAuth.isChecked() && (sDirectDownloadUsername.getText().toString().trim().isEmpty() || sDirectDownloadPassword.getText().toString().trim().isEmpty())) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.INVALID_DIRECTDOWNLOAD_USERORPASSWD);
            return;
        }

        JSONObject profile = new JSONObject();
        try {
            profile.put("name", profileName.getText().toString().trim())
                    .put("serverAddr", sAddr.getText().toString().trim())
                    .put("serverPort", Integer.parseInt(sPort.getText().toString().trim()))
                    .put("serverEndpoint", sEndpoint.getText().toString().trim())
                    .put("serverAuth", sAuth.isChecked())
                    .put("serverToken", sToken.getText().toString().trim())
                    .put("serverSSL", sSSL.isChecked());

            if (sDirectDownload.isChecked()) {
                JSONObject directDownload = new JSONObject();
                directDownload.put("addr", sDirectDownloadAddr.getText().toString().trim())
                        .put("auth", sDirectDownloadAuth.isChecked())
                        .put("username", sDirectDownloadUsername.getText().toString().trim())
                        .put("password", sDirectDownloadPassword.getText().toString().trim());
                profile.put("directDownload", directDownload);
            }
        } catch (JSONException ex) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex.getMessage());
            ex.printStackTrace();
            return;
        }

        try {
            deleteFile(profileName.getText().toString().trim() + ".profile");

            FileOutputStream fOut = openFileOutput(profileName.getText().toString().trim() + ".profile", Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write(profile.toString());
            osw.flush();
            osw.close();
        } catch (IOException ex) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex.getMessage());
            ex.printStackTrace();
        }

        if (Analytics.isTrackingAllowed(this))
            Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                    .setCategory(Analytics.CATEGORY_USER_INPUT)
                    .setAction(Analytics.ACTION_NEW_PROFILE)
                    .setLabel("Single profile")
                    .build());

        onBackPressed();
    }

    private void createMulti() {
        if (profileName.getText().toString().trim().isEmpty()) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.INVALID_PROFILE_NAME);
            return;
        }

        JSONObject profile = new JSONObject();
        try {
            JSONArray conditions = new JSONArray();
            for (ConnectivityCondition condition : mProfiles.keySet()) {
                JSONObject _profile = new JSONObject();
                _profile.put("type", condition.getType().toString());

                if (condition.getType() == ConnectivityCondition.TYPE.WIFI)
                    _profile.put("ssid", condition.getSSID());

                SingleModeProfileItem _item = mProfiles.get(condition);
                JSONObject jItem = new JSONObject();
                jItem.put("name", _item.getProfileName())
                        .put("default", _item.isDefault())
                        .put("serverAddr", _item.getServerAddr())
                        .put("serverPort", _item.getServerPort())
                        .put("serverEndpoint", _item.getServerEndpoint())
                        .put("serverAuth", _item.isServerAuth())
                        .put("serverToken", _item.getServerToken())
                        .put("serverSSL", _item.isServerSSL());

                if (_item.isDirectDownloadEnabled()) {
                    JSONObject _directDownload = new JSONObject();
                    _directDownload.put("addr", _item.getDirectDownload().getAddress())
                            .put("auth", _item.getDirectDownload().isAuth())
                            .put("username", _item.getDirectDownload().getUsername())
                            .put("password", _item.getDirectDownload().getPassword());

                    jItem.put("directDownload", _directDownload);
                }

                _profile.put("profile", jItem);
                conditions.put(_profile);
            }

            profile.put("name", profileName.getText().toString().trim())
                    .put("conditions", conditions);
        } catch (JSONException ex) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex.getMessage());
            ex.printStackTrace();
            return;
        }

        try {
            deleteFile(profileName.getText().toString().trim() + ".profile");

            FileOutputStream fOut = openFileOutput(profileName.getText().toString().trim() + ".profile", Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write(profile.toString());
            osw.flush();
            osw.close();
        } catch (IOException ex) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex.getMessage());
            ex.printStackTrace();
        }

        if (Analytics.isTrackingAllowed(this))
            Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                    .setCategory(Analytics.CATEGORY_USER_INPUT)
                    .setAction(Analytics.ACTION_NEW_PROFILE)
                    .setLabel("Multi profile")
                    .setValue(mProfiles.size())
                    .build());

        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, SelectProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    private class SingleOnTextChangedListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            sCompleteURL.setText(String.format("http://%s:%s%s", sAddr.getText().toString(), sPort.getText().toString(), sEndpoint.getText().toString()));
        }
    }

    private class MultiOnTextChangedListener implements TextWatcher {
        private TextView completeURL;
        private EditText addr;
        private EditText port;
        private EditText endpoint;

        public MultiOnTextChangedListener(TextView completeURL, EditText addr, EditText port, EditText endpoint) {
            this.completeURL = completeURL;
            this.addr = addr;
            this.port = port;
            this.endpoint = endpoint;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            completeURL.setText(String.format("http://%s:%s%s", addr.getText().toString(), port.getText().toString(), endpoint.getText().toString()));
        }
    }
}
