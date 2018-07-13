package com.gianlu.aria2app.Activities.AddDownload;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.gianlu.aria2app.Adapters.OptionsAdapter;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.Option;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.Options.OptionsUtils;
import com.gianlu.aria2app.Options.OptionsView;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Dialogs.FragmentWithDialog;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageView;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionsFragment extends FragmentWithDialog implements OptionsAdapter.Listener {
    private EditText position;
    private EditText filename;
    private OptionsAdapter adapter;
    private OptionsView optionsView;

    @NonNull
    public static OptionsFragment getInstance(Context context, boolean isUri) {
        OptionsFragment fragment = new OptionsFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.options));
        args.putBoolean("isUri", isUri);
        fragment.setArguments(args);
        return fragment;
    }

    private MessageView message;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_options, container, false);
        final ProgressBar loading = layout.findViewById(R.id.optionsFragment_loading);
        message = layout.findViewById(R.id.optionsFragment_message);
        position = layout.findViewById(R.id.optionsFragment_position);
        filename = layout.findViewById(R.id.optionsFragment_filename);
        optionsView = layout.findViewById(R.id.optionsFragment_options);

        layout.findViewById(R.id.optionsFragment_filenameContainer)
                .setVisibility(getArguments() != null && getArguments().getBoolean("isUri", false) ? View.VISIBLE : View.GONE);

        Aria2Helper helper;
        try {
            helper = Aria2Helper.instantiate(requireContext());
        } catch (Aria2Helper.InitializingException ex) {
            message.setError(R.string.failedLoading);
            optionsView.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            return layout;
        }

        helper.request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<Map<String, String>>() {
            @Override
            public void onResult(@NonNull Map<String, String> result) {
                if (getContext() == null) return;

                try {
                    adapter = OptionsAdapter.setup(getContext(), result, false, false, true, OptionsFragment.this);
                } catch (IOException | JSONException ex) {
                    onException(ex, false);
                    return;
                }

                optionsView.setAdapter(adapter);
                optionsView.setVisibility(View.VISIBLE);
                loading.setVisibility(View.GONE);

            }

            @Override
            public void onException(Exception ex, boolean shouldForce) {
                message.setError(R.string.failedLoading);
                optionsView.setVisibility(View.GONE);
                loading.setVisibility(View.GONE);
            }
        });

        return layout;
    }

    @Nullable
    public String getFilename() {
        String str = filename.getText().toString();
        if (str.isEmpty()) return null;
        else return str;
    }

    @NonNull
    public Map<String, String> getOptions() {
        if (adapter == null) return new HashMap<>();
        List<Option> options = adapter.getOptions();
        Map<String, String> map = new HashMap<>();

        for (Option option : options)
            if (option.isValueChanged())
                map.put(option.name, option.newValue);

        return map;
    }

    @Nullable
    public Integer getPosition() {
        try {
            return Integer.parseInt(position.getText().toString());
        } catch (Exception ex) {
            Logging.log(ex);
            return null;
        }
    }

    @Override
    public void onEditOption(@NonNull Option option) {
        if (getContext() == null) return;
        showDialog(OptionsUtils.getEditOptionDialog(getContext(), option, adapter));
    }
}
