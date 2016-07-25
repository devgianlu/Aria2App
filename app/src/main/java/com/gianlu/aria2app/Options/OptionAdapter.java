package com.gianlu.aria2app.Options;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gianlu.aria2app.R;

import java.util.List;
import java.util.Map;

public class OptionAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<OptionHeader> headers;
    private Map<OptionHeader, OptionChild> children;

    public OptionAdapter(Context context, List<OptionHeader> headers, Map<OptionHeader, OptionChild> children) {
        this.context = context;
        this.headers = headers;
        this.children = children;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    @Override
    public int getGroupCount() {
        return headers.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public Map<OptionHeader, OptionChild> getChildren() {
        return children;
    }

    @Override
    public OptionChild getChild(int groupPosition, int childPosition) {
        return children.get(headers.get(groupPosition));
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public OptionHeader getGroup(int groupPosition) {
        return headers.get(groupPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup viewGroup) {
        convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.option_child, null);
        final OptionChild item = getChild(groupPosition, childPosition);

        ToggleButton toggleButton = (ToggleButton) convertView.findViewById(R.id.moreAboutDownload_option_toggleButton);
        EditText editText = (EditText) convertView.findViewById(R.id.moreAboutDownload_option_editText);
        Spinner spinner = (Spinner) convertView.findViewById(R.id.moreAboutDownload_option_spinner);
        TextView desc = (TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionDesc);

        switch (item.getOptionType()) {
            case BOOLEAN:
                spinner.setVisibility(View.INVISIBLE);
                editText.setVisibility(View.INVISIBLE);
                toggleButton.setVisibility(View.VISIBLE);

                toggleButton.setChecked(Boolean.valueOf(String.valueOf(item.getValue())));
                toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        item.setCurrentValue(b);
                    }
                });
                break;
            case INTEGER:
                spinner.setVisibility(View.INVISIBLE);
                toggleButton.setVisibility(View.INVISIBLE);
                editText.setVisibility(View.VISIBLE);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setHint("Default: " + item.getDefaultVal());

                editText.setText(String.valueOf(item.getValue()));
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        item.setCurrentValue(editable.toString());
                    }
                });
                break;
            case STRING:
                spinner.setVisibility(View.INVISIBLE);
                toggleButton.setVisibility(View.INVISIBLE);
                editText.setVisibility(View.VISIBLE);
                editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                if (!item.getDefaultVal().equals(""))
                    editText.setHint("Default: " + item.getDefaultVal());

                editText.setText(String.valueOf(item.getValue()));
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        item.setCurrentValue(editable.toString());
                    }
                });
                break;
            case MULTIPLE:
                toggleButton.setVisibility(View.INVISIBLE);
                editText.setVisibility(View.INVISIBLE);
                spinner.setVisibility(View.VISIBLE);

                spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, item.getPossibleValues()));
                spinner.setSelection(item.getPossibleValues().indexOf(String.valueOf(item.getDefaultVal())));
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        item.setCurrentValue(adapterView.getAdapter().getItem(i).toString());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        item.setCurrentValue(item.getDefaultVal());
                    }
                });
                break;
        }

        desc.setText(item.getDesc());

        return convertView;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup viewGroup) {
        convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.option_header, null);
        OptionHeader item = getGroup(groupPosition);

        ((TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionText)).setText(String.format("%s: ", item.getOptionName()));
        ((TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionTextVal)).setText(String.valueOf(item.getOptionValue()));
        ((TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionFull)).setText(item.getOptionFull());

        return convertView;
    }
}
