package com.gianlu.aria2app.Activities.AddDownload;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.gianlu.aria2app.Options.OptionsManager;
import com.gianlu.aria2app.Options.OptionsUtils;
import com.gianlu.aria2app.Options.OptionsView;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.MessageLayout;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionsFragment extends Fragment {
    private EditText position;
    private EditText filename;
    private OptionsAdapter adapter;
    private OptionsView optionsView;

    public static OptionsFragment getInstance(Context context, boolean isUri) {
        OptionsFragment fragment = new OptionsFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.options));
        args.putBoolean("isUri", isUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_options, container, false);
        final ProgressBar loading = layout.findViewById(R.id.optionsFragment_loading);
        position = layout.findViewById(R.id.optionsFragment_position);
        filename = layout.findViewById(R.id.optionsFragment_filename);
        optionsView = layout.findViewById(R.id.optionsFragment_options);
        optionsView.setIsDialog(false);

        layout.findViewById(R.id.optionsFragment_filenameContainer)
                .setVisibility(getArguments() != null && getArguments().getBoolean("isUri", false) ? View.VISIBLE : View.GONE);

        Aria2Helper helper;
        final List<String> downloadOptions;
        try {
            helper = Aria2Helper.instantiate(getContext());
            downloadOptions = OptionsManager.get(getContext()).loadDownloadOptions();
        } catch (Aria2Helper.InitializingException | IOException | JSONException ex) {
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);
            optionsView.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            return layout;
        }
        helper.request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<Map<String, String>>() {
            @Override
            public void onResult(Map<String, String> result) {
                final Activity activity = getActivity();
                if (activity != null) {
                    List<Option> options = Option.fromOptionsMap(result, downloadOptions);
                    adapter = new OptionsAdapter(activity, options, false, true);
                    adapter.setHandler(new OptionsAdapter.IAdapter() {
                        @Override
                        public void onEditOption(Option option) {
                            OptionsUtils.showEditOptionDialog(activity, adapter, option);
                        }
                    });

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            optionsView.setAdapter(adapter);
                            optionsView.setVisibility(View.VISIBLE);
                            loading.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onException(Exception ex, boolean shouldForce) {
                MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);

                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            optionsView.setVisibility(View.GONE);
                            loading.setVisibility(View.GONE);
                        }
                    });
                }
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
            return null;
        }
    }
}
