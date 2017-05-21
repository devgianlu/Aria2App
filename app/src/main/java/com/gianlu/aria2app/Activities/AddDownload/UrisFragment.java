package com.gianlu.aria2app.Activities.AddDownload;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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

import com.gianlu.aria2app.Adapters.UrisAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.MessageLayout;

import java.util.List;


// TODO: Disclaimer
public class UrisFragment extends Fragment implements UrisAdapter.IAdapter {
    private LinearLayout layout;
    private UrisAdapter adapter;
    private RecyclerView list;

    public static UrisFragment getInstance(Context context) {
        UrisFragment fragment = new UrisFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.uris));
        fragment.setArguments(args);
        return fragment;
    }

    private void showAddUriDialog(final int oldPos, @Nullable String edit) {
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
                        if (oldPos != -1) adapter.removeUri(oldPos);
                        adapter.addUri(uri.getText().toString());
                    }
                });

        CommonUtils.showDialog(getActivity(), builder);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.uris_fragment, container, false);
        list = (RecyclerView) layout.findViewById(R.id.urisFragment_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        Button addNew = (Button) layout.findViewById(R.id.urisFragment_addNew);
        addNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddUriDialog(-1, null);
            }
        });

        adapter = new UrisAdapter(getContext(), this);
        list.setAdapter(adapter);

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        showAddUriDialog(-1, null); // TODO: Pick uri from clipboard if available
    }

    public List<String> getUris() {
        return adapter.getUris();
    }

    @Override
    public void onUrisCountChanged(int count) {
        if (count == 0) {
            list.setVisibility(View.GONE);
            MessageLayout.show(layout, R.string.noUris_help, R.drawable.ic_info_outline_black_48dp);
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
