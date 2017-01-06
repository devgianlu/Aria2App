package com.gianlu.aria2app.Main;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import java.util.List;

class URIsAdapter extends BaseAdapter {
    private final Activity context;
    private final List<String> objs;
    private final IAdapter handler;

    URIsAdapter(Activity context, List<String> objs, @Nullable IAdapter handler) {
        this.context = context;
        this.objs = objs;
        this.handler = handler;
    }

    List<String> getURIs() {
        return objs;
    }

    public void add(final String uri) {
        if (uri == null || uri.isEmpty())
            return;

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                objs.add(uri);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getCount() {
        if (handler != null)
            handler.onListChanged(objs);

        return objs.size();
    }

    @Override
    public String getItem(int i) {
        return objs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(context).inflate(R.layout.uri_item, viewGroup, false));

        holder.uri.setText(getItem(i));
        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText uri = new EditText(context);
                uri.setText(getItem(i));
                uri.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

                CommonUtils.showDialog(context, new AlertDialog.Builder(context)
                        .setView(uri)
                        .setTitle(R.string.addUri)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int ii) {
                                if (uri.getText().toString().trim().isEmpty()) return;
                                objs.remove(i);
                                objs.add(uri.getText().toString().trim());

                                notifyDataSetChanged();
                            }
                        }));
            }
        });

        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                objs.remove(i);
                notifyDataSetChanged();
            }
        });

        return holder.rootView;
    }

    interface IAdapter {
        void onListChanged(List<String> uris);
    }

    public class ViewHolder {
        public final LinearLayout rootView;
        public final TextView uri;
        public final ImageButton edit;
        public final ImageButton remove;

        ViewHolder(View rootView) {
            this.rootView = (LinearLayout) rootView;
            uri = (TextView) rootView.findViewById(R.id.uriItem_uri);
            edit = (ImageButton) rootView.findViewById(R.id.uriItem_edit);
            remove = (ImageButton) rootView.findViewById(R.id.uriItem_remove);
        }
    }
}
