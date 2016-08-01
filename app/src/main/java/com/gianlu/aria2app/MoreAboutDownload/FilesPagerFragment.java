package com.gianlu.aria2app.MoreAboutDownload;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.aria2app.R;


public class FilesPagerFragment extends Fragment {
    private ViewHolder holder;

    public static FilesPagerFragment newInstance(String title, String gid) {
        FilesPagerFragment fragment = new FilesPagerFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("gid", gid);

        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        holder = new ViewHolder(inflater.inflate(R.layout.files_fragment, container, false));

        return holder.rootView;
    }

    public class ViewHolder {
        public View rootView;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
        }
    }
}
