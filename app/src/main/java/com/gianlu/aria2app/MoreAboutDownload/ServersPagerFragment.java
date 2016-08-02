package com.gianlu.aria2app.MoreAboutDownload;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.aria2app.MoreAboutDownload.PeersFragment.PeersPagerFragment;

public class ServersPagerFragment extends Fragment {
    public static PeersPagerFragment newInstance(String title, String gid) {
        PeersPagerFragment fragment = new PeersPagerFragment();

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("gid", gid);

        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}