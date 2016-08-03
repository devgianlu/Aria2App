package com.gianlu.aria2app.MoreAboutDownload.ServersFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.NetIO.JTA2.IServers;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public class ServersPagerFragment extends Fragment {
    private UpdateUI updateUI;

    // TODO: Order by
    public static ServersPagerFragment newInstance(String title, String gid) {
        ServersPagerFragment fragment = new ServersPagerFragment();

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("gid", gid);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        try {
            Utils.readyJTA2(getActivity()).getServers(getArguments().getString("gid"), new IServers() {
                @Override
                public void onServers(Map<Integer, List<Server>> servers) {
                    final ServerCardAdapter adapter = new ServerCardAdapter(getContext(), servers);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((RecyclerView) view.findViewById(R.id.serversFragment_recyclerView)).setAdapter(adapter);
                        }
                    });

                    updateUI = new UpdateUI(getActivity(), getArguments().getString("gid"), adapter);
                    new Thread(updateUI).start();
                }

                @Override
                public void onException(Exception exception) {
                    Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                }
            });
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final SwipeRefreshLayout rootView = (SwipeRefreshLayout) inflater.inflate(R.layout.servers_fragment, container, false);
        rootView.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        rootView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                UpdateUI.stop(updateUI, new IThread() {
                    @Override
                    public void stopped() {
                        onViewCreated(rootView, savedInstanceState);
                        rootView.setRefreshing(false);
                    }
                });
            }
        });

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.serversFragment_recyclerView);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        return rootView;
    }
}