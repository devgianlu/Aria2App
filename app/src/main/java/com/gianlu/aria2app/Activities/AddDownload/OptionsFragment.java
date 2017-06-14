package com.gianlu.aria2app.Activities.AddDownload;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.gianlu.aria2app.Adapters.OptionsAdapter;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.Options.Option;
import com.gianlu.aria2app.Options.OptionsManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.MessageLayout;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionsFragment extends Fragment {
    private EditText position;
    private OptionsAdapter adapter;
    private RecyclerView list;

    public static OptionsFragment getInstance(Context context) {
        OptionsFragment fragment = new OptionsFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.options));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.options_fragment, container, false);
        final ProgressBar loading = (ProgressBar) layout.findViewById(R.id.optionsFragment_loading);
        position = (EditText) layout.findViewById(R.id.optionsFragment_position);
        list = (RecyclerView) layout.findViewById(R.id.optionsFragment_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        JTA2 jta2;
        final List<String> downloadOptions;
        try {
            jta2 = JTA2.instantiate(getContext());
            downloadOptions = OptionsManager.get(getContext()).loadDownloadOptions();
        } catch (JTA2InitializingException | IOException | JSONException ex) {
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);
            list.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            return layout;
        }

        jta2.getGlobalOption(new JTA2.IOption() {
            @Override
            public void onOptions(Map<String, String> optionsMap) {
                List<Option> options = Option.fromOptionsMap(optionsMap, downloadOptions);
                adapter = new OptionsAdapter(getContext(), options, false);
                list.setAdapter(adapter);

                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            list.setVisibility(View.VISIBLE);
                            loading.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onException(Exception ex) {
                MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);

                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            list.setVisibility(View.GONE);
                            loading.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });

        return layout;
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
