package com.gianlu.aria2app.Main;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Options.BooleanOptionChild;
import com.gianlu.aria2app.Options.IntegerOptionChild;
import com.gianlu.aria2app.Options.LocalParser;
import com.gianlu.aria2app.Options.MultipleOptionChild;
import com.gianlu.aria2app.Options.OptionAdapter;
import com.gianlu.aria2app.Options.OptionChild;
import com.gianlu.aria2app.Options.OptionHeader;
import com.gianlu.aria2app.Options.SourceOption;
import com.gianlu.aria2app.Options.StringOptionChild;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class URIFragment extends Fragment {
    private List<String> urisList = new ArrayList<>();
    private EditText position;
    private ExpandableListView optionsListView;

    public static URIFragment newInstance() {
        return new URIFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.uri_fragment, container, false);

        final ListView uris = (ListView) view.findViewById(R.id.uriFragment_uris);
        ImageButton addUri = (ImageButton) view.findViewById(R.id.uriFragment_newUri);
        position = (EditText) view.findViewById(R.id.uriFragment_position);
        optionsListView = (ExpandableListView) view.findViewById(R.id.uriFragment_options);

        addUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                final EditText uri = new EditText(getContext());
                uri.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                dialog.setView(uri)
                        .setTitle(R.string.uri)
                        .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (uri.getText().toString().trim().isEmpty()) return;
                                urisList.add(uri.getText().toString().trim());
                                uris.setAdapter(new URIAdapter(getContext(), urisList));
                            }
                        }).create().show();
            }
        });

        try {
            final List<OptionHeader> headers = new ArrayList<>();
            final Map<OptionHeader, OptionChild> children = new HashMap<>();

            JTA2 jta2 = Utils.readyJTA2(getActivity());

            final ProgressDialog pd = Utils.fastProgressDialog(getContext(), R.string.gathering_information, true, false);
            pd.show();

            jta2.getGlobalOption(new IOption() {
                @Override
                public void onOptions(Map<String, String> options) {
                    LocalParser localOptions;
                    try {
                        localOptions = new LocalParser(getContext(), false);
                    } catch (IOException | JSONException ex) {
                        pd.dismiss();
                        Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                        return;
                    }

                    for (String resOption : getResources().getStringArray(R.array.downloadOptions)) {
                        try {
                            OptionHeader header = new OptionHeader(resOption, localOptions.getCommandLine(resOption), options.get(resOption), false);
                            headers.add(header);

                            if (getResources().getIdentifier("__" + resOption.replace("-", "_"), "array", "com.gianlu.aria2app") == 0) {
                                children.put(header, new StringOptionChild(
                                        localOptions.getDefinition(resOption),
                                        String.valueOf(localOptions.getDefaultValue(resOption)),
                                        String.valueOf(options.get(resOption))));
                                continue;
                            }

                            switch (SourceOption.OPTION_TYPE.valueOf(getResources().getStringArray(getResources().getIdentifier("__" + resOption.replace("-", "_"), "array", "com.gianlu.aria2app"))[0])) {
                                case INTEGER:
                                    children.put(header, new IntegerOptionChild(
                                            localOptions.getDefinition(resOption),
                                            Utils.parseInt(localOptions.getDefaultValue(resOption)),
                                            Utils.parseInt(options.get(resOption))));
                                    break;
                                case BOOLEAN:
                                    children.put(header, new BooleanOptionChild(
                                            localOptions.getDefinition(resOption),
                                            Utils.parseBoolean(localOptions.getDefaultValue(resOption)),
                                            Utils.parseBoolean(options.get(resOption))));
                                    break;
                                case STRING:
                                    children.put(header, new StringOptionChild(
                                            localOptions.getDefinition(resOption),
                                            String.valueOf(localOptions.getDefaultValue(resOption)),
                                            String.valueOf(options.get(resOption))));
                                    break;
                                case MULTIPLE:
                                    children.put(header, new MultipleOptionChild(
                                            localOptions.getDefinition(resOption),
                                            String.valueOf(localOptions.getDefaultValue(resOption)),
                                            String.valueOf(options.get(resOption)),
                                            Arrays.asList(
                                                    getResources().getStringArray(
                                                            getResources().getIdentifier("__" + resOption.replace("-", "_"), "array", "com.gianlu.aria2app"))[1].split(","))));
                                    break;
                            }
                        } catch (JSONException ex) {
                            pd.dismiss();
                            Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                        }
                    }

                    pd.dismiss();
                }

                @Override
                public void onException(Exception exception) {
                    pd.dismiss();
                    Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                }
            });

            optionsListView.setAdapter(new OptionAdapter(getContext(), headers, children));
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.uriFragment_newUri).performClick();
    }

    public List<String> getUris() {
        return urisList;
    }

    public Integer getPosition() {
        try {
            return Integer.parseInt(position.getText().toString());
        } catch (Exception ex) {
            return null;
        }
    }

    public Map<String, String> getOptions() {
        Map<String, String> map = new HashMap<>();

        for (Map.Entry<OptionHeader, OptionChild> item : ((OptionAdapter) optionsListView.getExpandableListAdapter()).getChildren().entrySet()) {
            if (!item.getValue().isChanged()) continue;
            map.put(item.getKey().getOptionName(), item.getValue().getStringValue());
        }

        return map;
    }
}
