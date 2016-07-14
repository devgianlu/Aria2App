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

import com.gianlu.aria2app.Options.OptionAdapter;
import com.gianlu.aria2app.Options.OptionChild;
import com.gianlu.aria2app.Options.OptionHeader;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.jtitan.Aria2Helper.IOption;
import com.gianlu.jtitan.Aria2Helper.JTA2;

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

        final List<OptionHeader> headers = new ArrayList<>();
        final Map<OptionHeader, OptionChild> children = new HashMap<>();

        final JTA2 jta2 = Utils.readyJTA2(getContext());
        final ProgressDialog progressDialog = Utils.fastProgressDialog(getContext(), R.string.gathering_information, true, false);
        progressDialog.show();

        jta2.getGlobalOption(new IOption() {
            @Override
            public void onOptions(Map<String, String> options) {
                String[] availableOptions;
                availableOptions = getResources().getStringArray(R.array.globalOptions);

                for (String option : availableOptions) {
                    String optionn = option;
                    if (option.equals("continue")) optionn += "e";

                    String[] optionIdentifier = getResources().getStringArray(getResources().getIdentifier(optionn.replace("-", "_"), "array", "com.gianlu.aria2app"));
                    String optionName = optionIdentifier[0];
                    String optionDesc = optionIdentifier[1];
                    OptionChild.OPTION_TYPE optionType;
                    Object optionDefaultVal;

                    String responseOption = options.get(option);
                    if (responseOption == null) responseOption = "";
                    OptionHeader thisHeader = new OptionHeader(optionName, "--" + option, responseOption);
                    headers.add(thisHeader);
                    OptionChild thisChild;

                    switch (optionIdentifier[2]) {
                        case "string":
                            optionType = OptionChild.OPTION_TYPE.STRING;
                            optionDefaultVal = String.valueOf(optionIdentifier[3]);

                            thisChild = new OptionChild(option, responseOption, optionDefaultVal, optionType, optionDesc);
                            break;
                        case "integer":
                            optionType = OptionChild.OPTION_TYPE.INTEGER;
                            optionDefaultVal = Integer.parseInt(optionIdentifier[3]);

                            thisChild = new OptionChild(option, responseOption, optionDefaultVal, optionType, optionDesc);
                            break;
                        case "boolean":
                            optionType = OptionChild.OPTION_TYPE.BOOLEAN;
                            optionDefaultVal = Boolean.parseBoolean(optionIdentifier[3]);

                            thisChild = new OptionChild(option, responseOption, optionDefaultVal, optionType, optionDesc);
                            break;
                        default:
                            // optionType = OptionChild.OPTION_TYPE.MULTIPLE;
                            String[] possibleValues = optionIdentifier[2].split(",");
                            optionDefaultVal = optionIdentifier[3];
                            thisChild = new OptionChild(option, responseOption, optionDefaultVal, optionDesc, Arrays.asList(possibleValues));
                            break;
                    }

                    children.put(thisHeader, thisChild);
                }

                progressDialog.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        optionsListView.setAdapter(new OptionAdapter(getContext(), headers, children));
                    }
                });
            }

            @Override
            public void onException(Exception exception) {
                progressDialog.dismiss();
                Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception.getMessage());
            }
        });

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
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Map<String, String> getOptions() {
        Map<String, String> map = new HashMap<>();

        for (Map.Entry<OptionHeader, OptionChild> item : ((OptionAdapter) optionsListView.getExpandableListAdapter()).getChildren().entrySet()) {
            if (item.getValue().getCurrentValue() == null) continue;
            if (item.getValue().getCurrentValue().equals("")) {
                map.put(item.getValue().getOption(), String.valueOf(item.getValue().getDefaultVal()));
            } else {
                map.put(item.getValue().getOption(), String.valueOf(item.getValue().getCurrentValue()));
            }
        }

        return map;
    }
}
