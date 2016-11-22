package com.gianlu.aria2app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class DirectDownloadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_download);
        setTitle(R.string.directDownload);
    }
}
