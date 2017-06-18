package com.gianlu.aria2app.Activities.AddDownload;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Base64Fragment extends Fragment {
    private final int FILE_SELECT_CODE = 7;
    private TextView path;

    public static Base64Fragment getInstance(Context context, boolean torrent, @Nullable File file) {
        Base64Fragment fragment = new Base64Fragment();
        Bundle args = new Bundle();
        args.putBoolean("torrent", torrent);
        args.putString("title", context.getString(R.string.file));
        if (file != null) args.putSerializable("file", file);
        fragment.setArguments(args);
        return fragment;
    }

    private void showFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (getArguments().getBoolean("torrent", true)) intent.setType("application/x-bittorrent");
        else intent.setType("application/metalink4+xml,application/metalink+xml");

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.NO_FILE_MANAGER, ex);
            return;
        }

        path.setText(null);
    }

    private void setFile(File file) {
        if (!file.exists() || !file.canRead()) {
            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.INVALID_FILE, new Exception("File doesn't exist or can't be read."));
        } else {
            path.setText(file.getAbsolutePath());
            path.setTag(file);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    String filePath = Utils.resolveUri(getContext(), data.getData());
                    if (filePath == null) {
                        CommonUtils.UIToast(getActivity(), Utils.ToastMessages.INVALID_FILE, new Exception("Uri cannot be resolved!"));
                    } else {
                        setFile(new File(filePath));
                    }
                }
                break;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        File file = (File) getArguments().getSerializable("file");
        if (file != null) setFile(file);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.base64_fragment, container, false);
        path = (TextView) layout.findViewById(R.id.base64Fragment_path);
        Button pick = (Button) layout.findViewById(R.id.base64Fragment_pick);
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Utils.requestReadPermission(getActivity(), R.string.readExternalStorageRequest_base64Message, 12);
                } else {
                    showFilePicker();
                }
            }
        });

        SuperTextView help = (SuperTextView) layout.findViewById(R.id.base64Fragment_help);
        if (getArguments().getBoolean("torrent", true)) help.setHtml(R.string.pickTorrent_help);
        else help.setHtml(R.string.pickMetalink_help);

        return layout;
    }

    @Nullable
    public String getBase64() {
        File file = (File) path.getTag();

        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (IOException ex) {
            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.INVALID_FILE, ex);
            return null;
        }
    }
}
