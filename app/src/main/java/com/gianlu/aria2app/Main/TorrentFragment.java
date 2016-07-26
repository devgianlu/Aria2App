package com.gianlu.aria2app.Main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

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
import com.gianlu.jtitan.Aria2Helper.IOption;
import com.gianlu.jtitan.Aria2Helper.JTA2;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TorrentFragment extends Fragment {
    private List<String> urisList = new ArrayList<>();
    private EditText position;
    private TextView path;
    private Uri data;
    private ExpandableListView optionsListView;

    public static TorrentFragment newInstance(boolean torrentMode) {
        Bundle args = new Bundle();
        args.putBoolean("torrentMode", torrentMode);
        TorrentFragment fragment = new TorrentFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.torrent_fragment, null);

        final ListView uris = (ListView) view.findViewById(R.id.torrentFragment_uris);
        final Button pick = (Button) view.findViewById(R.id.torrentFragment_pick);
        path = (TextView) view.findViewById(R.id.torrentFragment_pickHelp);
        ImageButton addUri = (ImageButton) view.findViewById(R.id.torrentFragment_newUri);
        position = (EditText) view.findViewById(R.id.torrentFragment_position);
        optionsListView = (ExpandableListView) view.findViewById(R.id.torrentFragment_options);

        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pickFile = new Intent(Intent.ACTION_GET_CONTENT)
                        .setType(getArguments().getBoolean("torrentMode") ? "application/x-bittorrent" : "application/metalink4+xml")
                        .addCategory(Intent.CATEGORY_OPENABLE);

                TorrentFragment.this.startActivityForResult(Intent.createChooser(pickFile, getString(getArguments().getBoolean("torrentMode") ? R.string.pickTorrent : R.string.pickMetalink)), 1);
            }
        });

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
                    Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex.getMessage());
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
                        Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex.getMessage());
                    }
                }

                pd.dismiss();
            }

            @Override
            public void onException(Exception exception) {
                pd.dismiss();
                Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception.getMessage());
            }
        });

        optionsListView.setAdapter(new OptionAdapter(getContext(), headers, children));

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    this.data = data.getData();

                    String fileName = null;
                    String scheme = data.getData().getScheme();
                    if (scheme.equals("file")) {
                        fileName = data.getData().getLastPathSegment();
                    } else if (scheme.equals("content")) {
                        String[] proj = {MediaStore.Images.Media.DISPLAY_NAME};
                        Cursor cursor = getContext().getContentResolver().query(data.getData(), proj, null, null, null);
                        if (cursor != null && cursor.getCount() != 0) {
                            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                            cursor.moveToFirst();
                            fileName = cursor.getString(columnIndex);
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    path.setText(fileName);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Uri getData() {
        return data;
    }

    public List<String> getUris() {
        if (urisList.size() == 0) return null;
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
            if (!item.getValue().isChanged()) continue;
            map.put(item.getKey().getOptionName(), item.getValue().getStringValue());
        }

        return map;
    }
}
