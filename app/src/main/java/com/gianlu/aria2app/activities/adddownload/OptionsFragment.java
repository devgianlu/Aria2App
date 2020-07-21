package com.gianlu.aria2app.activities.adddownload;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.adapters.OptionsAdapter;
import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.AriaRequests;
import com.gianlu.aria2app.api.TrackersListFetch;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.Option;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.aria2app.options.OptionsUtils;
import com.gianlu.aria2app.options.OptionsView;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.preferences.Prefs;

import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class OptionsFragment extends FragmentWithDialog implements OptionsAdapter.Listener {
    private static final String TAG = OptionsFragment.class.getSimpleName();
    private EditText position;
    private EditText filename;
    private Switch pause;
    private OptionsAdapter adapter;
    private OptionsView optionsView;
    private MessageView message;

    @NonNull
    public static OptionsFragment getInstance(Context context, boolean isUri) {
        OptionsFragment fragment = new OptionsFragment();
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.options));
        args.putBoolean("isUri", isUri);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public static OptionsFragment getInstance(Context context, @NonNull AddDownloadBundle bundle) {
        OptionsFragment fragment = new OptionsFragment();
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.options));
        args.putBoolean("isUri", bundle instanceof AddUriBundle);
        args.putSerializable("edit", bundle);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_options, container, false);
        final ProgressBar loading = layout.findViewById(R.id.optionsFragment_loading);
        message = layout.findViewById(R.id.optionsFragment_message);
        position = layout.findViewById(R.id.optionsFragment_position);
        pause = layout.findViewById(R.id.optionsFragment_pause);
        filename = layout.findViewById(R.id.optionsFragment_filename);
        optionsView = layout.findViewById(R.id.optionsFragment_options);

        Aria2Helper helper;
        try {
            helper = Aria2Helper.instantiate(requireContext());
        } catch (Aria2Helper.InitializingException ex) {
            message.error(R.string.failedLoading);
            optionsView.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            return layout;
        }

        final AddDownloadBundle bundle;
        boolean isAddUri = false;
        Bundle args = getArguments();
        if (args != null) {
            bundle = (AddDownloadBundle) args.getSerializable("edit");
            isAddUri = args.getBoolean("isUri", false);
        } else {
            bundle = null;
        }

        layout.findViewById(R.id.optionsFragment_filenameContainer).setVisibility(isAddUri ? View.VISIBLE : View.GONE);
        if (isAddUri && bundle != null && bundle.options != null) {
            OptionsMap.OptionValue val = bundle.options.get("out");
            if (val != null) filename.setText(val.string());
        }

        helper.request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<OptionsMap>() {
            @Override
            public void onResult(@NonNull OptionsMap result) {
                if (getContext() == null) return;

                try {
                    adapter = OptionsAdapter.setup(getContext(), result, false, false, true, OptionsFragment.this);
                } catch (IOException | JSONException ex) {
                    onException(ex);
                    return;
                }

                if (bundle != null && bundle.options != null) {
                    for (Option option : Option.fromMapSimpleChanged(bundle.options))
                        adapter.optionChanged(option);
                }

                optionsView.setAdapter(adapter);
                optionsView.setVisibility(View.VISIBLE);
                loading.setVisibility(View.GONE);

                if (Prefs.getBoolean(PK.A2_ADD_BEST_TRACKERS))
                    addBestTrackers();
            }

            @Override
            public void onException(@NonNull Exception ex) {
                message.error(R.string.failedLoading);
                optionsView.setVisibility(View.GONE);
                loading.setVisibility(View.GONE);
            }
        });

        return layout;
    }

    private void addBestTrackers() {
        TrackersListFetch.get().getTrackers(TrackersListFetch.Type.BEST, getActivity(), new TrackersListFetch.Listener() {
            @Override
            public void onDone(@NonNull Collection<String> trackers) {
                Option btTracker = null;
                for (Option option : adapter.getOptions()) {
                    if (Objects.equals(option.name, "bt-tracker")) {
                        btTracker = option;
                        break;
                    }
                }

                if (btTracker == null) return;

                Set<String> set = new HashSet<>(trackers);
                String oldStr = (btTracker.newValue != null ? btTracker.newValue : btTracker.value).string();
                if (!oldStr.isEmpty()) {
                    String[] old = oldStr.split(",");
                    set.addAll(Arrays.asList(old));
                }

                String newStr = CommonUtils.join(set, ",");
                btTracker.setNewValue(newStr);
                adapter.optionChanged(btTracker);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Log.e(TAG, "Failed getting best trackers.", ex);
            }
        });
    }

    @Nullable
    public String getFilename() {
        String str = filename.getText().toString();
        if (str.isEmpty()) return null;
        else return str;
    }

    @NonNull
    public OptionsMap getOptions() {
        if (adapter == null) return new OptionsMap();
        List<Option> options = adapter.getOptions();
        OptionsMap map = new OptionsMap();

        for (Option option : options)
            if (option.isValueChanged())
                map.put(option.name, option.newValue);

        if (pause.isChecked()) map.put("pause", "true");

        return map;
    }

    @Nullable
    public Integer getPosition() {
        if (position.getText().toString().trim().isEmpty())
            return null;

        try {
            return Integer.parseInt(position.getText().toString());
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void onEditOption(@NonNull Option option) {
        if (getContext() == null) return;
        showDialog(OptionsUtils.getEditOptionDialog(getContext(), option, adapter));
    }
}
