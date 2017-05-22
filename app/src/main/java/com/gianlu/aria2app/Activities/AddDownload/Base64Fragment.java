package com.gianlu.aria2app.Activities.AddDownload;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.aria2app.R;

public class Base64Fragment extends Fragment {

    public static Base64Fragment getInstance(Context context) {
        Base64Fragment fragment = new Base64Fragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.pickFromFile));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Nullable
    public String getBase64() {
        return null;
    }
}
