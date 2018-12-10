package com.gianlu.aria2app.Activities.AddDownload;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
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
import com.gianlu.commonutils.Dialogs.FragmentWithDialog;
import com.gianlu.commonutils.MessageView;
import com.gianlu.commonutils.Toaster;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class UrisFragment extends FragmentWithDialog implements UrisAdapter.Listener {
    private UrisAdapter adapter;
    private RecyclerView list;
    private MessageView message;

    @NonNull
    public static UrisFragment getInstance(Context context, boolean compulsory, @Nullable URI uri) {
        UrisFragment fragment = new UrisFragment();
        Bundle args = new Bundle();
        args.putBoolean("compulsory", compulsory);
        args.putString("title", context.getString(R.string.uris));
        if (uri != null) args.putSerializable("uri", uri);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public static UrisFragment getInstance(Context context, @NonNull AddDownloadBundle bundle) {
        UrisFragment fragment = new UrisFragment();
        Bundle args = new Bundle();
        args.putBoolean("compulsory", bundle instanceof AddUriBundle);
        args.putString("title", context.getString(R.string.uris));
        args.putSerializable("edit", bundle);
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
                .setPositiveButton(edit == null ? R.string.add : R.string.save, (dialog, which) -> {
                    if (uri.getText().toString().trim().startsWith("magnet:")) {
                        if (!adapter.getUris().isEmpty()) {
                            showToast(Toaster.build().message(R.string.onlyOneTorrentUri));
                            return;
                        }
                    }

                    if (oldPos != -1) adapter.removeUri(oldPos);
                    adapter.addUri(uri.getText().toString());
                });

        showDialog(builder);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_uris, container, false);
        message = layout.findViewById(R.id.urisFragment_message);
        list = layout.findViewById(R.id.urisFragment_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        Button addNew = layout.findViewById(R.id.urisFragment_addNew);
        addNew.setOnClickListener(v -> {
            if (adapter.canAddUri()) showAddUriDialog(-1, null);
            else showToast(Toaster.build().message(R.string.onlyOneTorrentUri));
        });

        AddDownloadBundle bundle = null;
        boolean compulsory = false;
        Bundle args = getArguments();
        if (args != null) {
            compulsory = args.getBoolean("compulsory", false);
            bundle = (AddDownloadBundle) args.getSerializable("edit");
        }

        TextView disclaimer = layout.findViewById(R.id.urisFragment_disclaimer);
        if (compulsory)
            disclaimer.setText(R.string.uris_disclaimer);
        else
            disclaimer.setText(R.string.torrentUris_disclaimer);

        adapter = new UrisAdapter(requireContext(), this);
        list.setAdapter(adapter);

        if (bundle != null) {
            List<String> uris = null;
            if (bundle instanceof AddUriBundle) uris = ((AddUriBundle) bundle).uris;
            else if (bundle instanceof AddTorrentBundle) uris = ((AddTorrentBundle) bundle).uris;

            if (uris != null) adapter.addUris(uris);
        }

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

        AddDownloadBundle bundle = (AddDownloadBundle) getArguments().getSerializable("edit");
        boolean hasUri = false;
        if (bundle instanceof AddUriBundle) hasUri = !((AddUriBundle) bundle).uris.isEmpty();
        else if (bundle instanceof AddTorrentBundle)
            hasUri = !((AddTorrentBundle) bundle).uris.isEmpty();

        if (!hasUri) showAddUriDialog(-1, null);
    }

    @Nullable
    public ArrayList<String> getUris() {
        return adapter != null ? adapter.getUris() : null;
    }

    @Override
    public void onUrisCountChanged(int count) {
        if (count == 0) {
            list.setVisibility(View.GONE);
            if (getArguments() != null && getArguments().getBoolean("compulsory", false))
                message.setInfo(R.string.noUris_help);
            else
                message.setInfo(R.string.noUris);
        } else {
            list.setVisibility(View.VISIBLE);
            message.hide();
        }
    }

    @Override
    public void onEditUri(int oldPos, @NonNull String uri) {
        showAddUriDialog(oldPos, uri);
    }
}
