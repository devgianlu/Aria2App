package com.gianlu.aria2app.MoreAboutDownload.ServersFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.MoreAboutDownload.CommonFragment;
import com.gianlu.aria2app.NetIO.JTA2.IServers;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public class ServersPagerFragment extends CommonFragment {
    private UpdateUI updateUI;

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
            JTA2.newInstance(getActivity()).getServers(getArguments().getString("gid"), new IServers() {
                @Override
                public void onServers(Map<Integer, List<Server>> servers) {
                    final ServerCardAdapter adapter = new ServerCardAdapter(getContext(), servers, (CardView) view.findViewById(R.id.serversFragment_noData));
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

                @Override
                public void onDownloadNotActive(Exception exception) {
                    view.findViewById(R.id.serversFragment_noData).setVisibility(View.VISIBLE);
                    ((TextView) view.findViewById(R.id.serversFragment_noDataLabel)).setText(getString(R.string.noServersMessage, exception.getMessage()));
                }
            });
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.servers_fragment, container, false);
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.serversFragment_swipeLayout);

        swipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                UpdateUI.stop(updateUI, new IThread() {
                    @Override
                    public void stopped() {
                        onViewCreated(rootView, savedInstanceState);
                        swipeLayout.setRefreshing(false);
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

    @Override
    public void stopUpdater() {
        UpdateUI.stop(updateUI);
    }
}