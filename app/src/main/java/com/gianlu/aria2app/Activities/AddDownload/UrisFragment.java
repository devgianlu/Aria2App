package com.gianlu.aria2app.Activities.AddDownload;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.Adapters.UrisAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.Toaster;

import java.net.URI;
import java.net.URL;
import java.util.List;


public class UrisFragment extends Fragment implements UrisAdapter.IAdapter {
    private LinearLayout layout;
    private UrisAdapter adapter;
    private RecyclerView list;

    public static UrisFragment getInstance(Context context, boolean compulsory, @Nullable URI uri) {
        UrisFragment fragment = new UrisFragment();
        Bundle args = new Bundle();
        args.putBoolean("compulsory", compulsory);
        args.putString("title", context.getString(R.string.uris));
        if (uri != null) args.putSerializable("uri", uri);
        fragment.setArguments(args);
        return fragment;
    }

    private void showAddUriDialog(final int oldPos, @Nullable String edit) {
        if (getContext() == null) return;

        final EditText uri = new EditText(getContext());
        uri.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        uri.setText(edit);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(edit == null ? R.string.addUri : R.string.editUri)
                .setView(uri)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(edit == null ? R.string.add : R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (uri.getText().toString().trim().startsWith("magnet:")) {
                            if (!adapter.getUris().isEmpty()) {
                                Toaster.show(getContext(), Utils.Messages.ONLY_ONE_TORRENT);
                                return;
                            }
                        }

                        if (oldPos != -1) adapter.removeUri(oldPos);
                        adapter.addUri(uri.getText().toString());
                    }
                });

        DialogUtils.showDialog(getActivity(), builder);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getContext() == null) return null;

        layout = (LinearLayout) inflater.inflate(R.layout.fragment_uris, container, false);
        list = layout.findViewById(R.id.urisFragment_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        Button addNew = layout.findViewById(R.id.urisFragment_addNew);
        addNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.canAddUri()) showAddUriDialog(-1, null);
                else Toaster.show(getContext(), Utils.Messages.ONLY_ONE_TORRENT);
            }
        });

        TextView disclaimer = layout.findViewById(R.id.urisFragment_disclaimer);
        if (getArguments() != null && getArguments().getBoolean("compulsory", false))
            disclaimer.setText(R.string.uris_disclaimer);
        else
            disclaimer.setText(R.string.torrentUris_disclaimer);

        adapter = new UrisAdapter(getContext(), this);
        list.setAdapter(adapter);

        return layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getContext() == null || getArguments() == null || !getArguments().getBoolean("compulsory", false))
            return;

        URI uri = (URI) getArguments().getSerializable("uri");
        if (uri != null) {
            showAddUriDialog(-1, uri.toASCIIString());
            return;
        } else {
            ClipboardManager manager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) {
                ClipData clip = manager.getPrimaryClip();
                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        ClipData.Item item = clip.getItemAt(i);
                        String clipUri = item.coerceToText(getContext()).toString();

                        try {
                            new URL(clipUri);
                            showAddUriDialog(-1, clipUri);
                            return;
                        } catch (Exception ignored) {
                        }

                        if (clipUri.startsWith("magnet:")) {
                            showAddUriDialog(-1, clipUri);
                            return;
                        }
                    }
                }
            }
        }

        showAddUriDialog(-1, null);
    }

    @Nullable
    public List<String> getUris() {
        return adapter != null ? adapter.getUris() : null;
    }

    @Override
    public void onUrisCountChanged(int count) {
        if (count == 0) {
            list.setVisibility(View.GONE);
            if (getArguments() != null && getArguments().getBoolean("compulsory", false))
                MessageLayout.show(layout, R.string.noUris_help, R.drawable.ic_info_outline_black_48dp);
            else
                MessageLayout.show(layout, R.string.noUris, R.drawable.ic_info_outline_black_48dp);
        } else {
            list.setVisibility(View.VISIBLE);
            MessageLayout.hide(layout);
        }
    }

    @Override
    public void onEditUri(int oldPos, String uri) {
        showAddUriDialog(oldPos, uri);
    }
}
