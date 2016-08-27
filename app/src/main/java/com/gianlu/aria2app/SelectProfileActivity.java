package com.gianlu.aria2app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.ExpandableListView;

import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.Profile.AddProfileActivity;
import com.gianlu.aria2app.Main.Profile.MultiModeProfileItem;
import com.gianlu.aria2app.Main.Profile.ProfileItem;
import com.gianlu.aria2app.Main.Profile.ProfilesCustomAdapter;
import com.gianlu.aria2app.Main.Profile.SingleModeProfileItem;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class SelectProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_profile);
        setTitle(R.string.title_activity_select_profile);

        UncaughtExceptionHandler.application = getApplication();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        ExpandableListView listView = (ExpandableListView) findViewById(R.id.selectProfile_listView);

        List<ProfileItem> profiles = new ArrayList<>();
        File files[] = getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.toLowerCase().endsWith(".profile");
            }
        });

        for (File profile : files) {
            try {
                if (ProfileItem.isSingleMode(this, profile)) {
                    profiles.add(SingleModeProfileItem.fromFile(this, profile));
                } else {
                    profiles.add(MultiModeProfileItem.fromFile(this, profile));
                }
            } catch (FileNotFoundException ex) {
                Utils.UIToast(this, Utils.TOAST_MESSAGES.FILE_NOT_FOUND, ex);
            } catch (JSONException | IOException ex) {
                Utils.UIToast(this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex);
                ex.printStackTrace();
            }
        }

        listView.setAdapter(new ProfilesCustomAdapter(this, listView, profiles, new ProfilesCustomAdapter.OnItemSelected() {
            @Override
            public void onSelected(final String profileName, final ProfileItem item) {
                if (item.getStatus() != ProfileItem.STATUS.ONLINE) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SelectProfileActivity.this);
                    builder.setMessage(R.string.serverOffline)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SingleModeProfileItem profile = item.isSingleMode() ? ((SingleModeProfileItem) item) : ((MultiModeProfileItem) item).getCurrentProfile(SelectProfileActivity.this);
                                    Intent intent = new Intent(SelectProfileActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            .putExtra("profile", profile);
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            }).create().show();
                } else {
                    SingleModeProfileItem profile = item.isSingleMode() ? ((SingleModeProfileItem) item) : ((MultiModeProfileItem) item).getCurrentProfile(SelectProfileActivity.this);
                    Intent intent = new Intent(SelectProfileActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            .putExtra("profile", profile);
                    startActivity(intent);
                }
            }
        },
                new ProfilesCustomAdapter.OnItemEdit() {
                    @Override
                    public void onEdit(ProfileItem item) {
                        startActivity(new Intent(SelectProfileActivity.this, AddProfileActivity.class)
                                .putExtra("edit", true)
                                .putExtra("isSingleMode", item.isSingleMode())
                                .putExtra("name", item.getGlobalProfileName()));
                    }
                }));


        if (getIntent().getBooleanExtra("external", false)) {
            if (ProfileItem.exists(this, "Local device")) {
                startExternal(getIntent());
            } else {
                new AlertDialog.Builder(this).
                        setTitle(R.string.saveProfile)
                        .setMessage(R.string.saveProfile_message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    FileOutputStream fOut = openFileOutput("Local device.profile", Context.MODE_PRIVATE);
                                    OutputStreamWriter osw = new OutputStreamWriter(fOut);

                                    osw.write(new SingleModeProfileItem("Local device",
                                            "localhost",
                                            getIntent().getIntExtra("port", 6800),
                                            "/jsonrpc",
                                            JTA2.AUTH_METHOD.TOKEN,
                                            false,
                                            getIntent().getStringExtra("token"),
                                            false,
                                            null).toJSON().toString());
                                    osw.flush();
                                    osw.close();
                                } catch (IOException | JSONException ex) {
                                    Utils.UIToast(SelectProfileActivity.this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex);
                                    ex.printStackTrace();
                                }

                                startExternal(getIntent());
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startExternal(getIntent());
                            }
                        }).create().show();
            }
        }
    }

    private void startExternal(Intent intent) {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra("profile", new SingleModeProfileItem("Local device",
                        "localhost",
                        intent.getIntExtra("port", 6800),
                        "/jsonrpc",
                        JTA2.AUTH_METHOD.TOKEN,
                        false,
                        intent.getStringExtra("token"),
                        false,
                        null).setGlobalProfileName("Local device")));
    }
}
