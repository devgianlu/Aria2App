package com.gianlu.aria2app.Activities.MoreAboutDownload.ServersFragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

import java.util.List;

public class ServersPagerFragment extends Fragment {
    // private UpdateUI updateUI;

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
        // UpdateUI.stop(updateUI);

        try {
            JTA2.instantiate(getActivity()).getServers(getArguments().getString("gid"), new JTA2.IServers() {
                @Override
                public void onServers(SparseArray<List<Server>> servers) {
                    /* TODO
                    final ServerCardAdapter adapter = new ServerCardAdapter(getContext(), servers, (CardView) view.findViewById(R.id.serversFragment_noData));
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((RecyclerView) view.findViewById(R.id.serversFragment_recyclerView)).setAdapter(adapter);
                            }
                        });
                    }

                    updateUI = new UpdateUI(getActivity(), getArguments().getString("gid"), adapter);
                    new Thread(updateUI).start();
                    */
                }

                @Override
                public void onException(Exception exception) {
                    CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_GATHERING_INFORMATION, exception);
                }

                @Override
                public void onDownloadNotActive(final Exception exception) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ServerCardAdapter adapter = ((ServerCardAdapter) ((RecyclerView) view.findViewById(R.id.serversFragment_recyclerView)).getAdapter());
                                if (adapter != null)
                                    adapter.clear();
                                view.findViewById(R.id.serversFragment_noData).setVisibility(View.VISIBLE);
                                ((TextView) view.findViewById(R.id.serversFragment_noDataLabel)).setText(getString(R.string.noServersMessage, exception.getMessage()));
                            }
                        });
                    }
                }
            });
        } catch (JTA2InitializingException ex) {
            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_GATHERING_INFORMATION, ex);
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
                // TODO
            }
        });

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.serversFragment_recyclerView);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        return rootView;
    }
}