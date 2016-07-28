package com.gianlu.aria2app.Options;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.Html;
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
import com.gianlu.aria2app.Utils;

import java.util.List;
import java.util.Map;

public class OptionAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<OptionHeader> headers;
    private Map<OptionHeader, OptionChild> children;
    private String colorAccent;

    public OptionAdapter(Context context, String hexColorAccent, List<OptionHeader> headers, Map<OptionHeader, OptionChild> children) {
        this.context = context;
        this.colorAccent = hexColorAccent;
        if (this.colorAccent.length() == 8) this.colorAccent = this.colorAccent.substring(2);
        this.headers = headers;
        this.children = children;
    }

    public OptionAdapter(Context context, List<OptionHeader> headers, Map<OptionHeader, OptionChild> children) {
        this.context = context;
        this.children = children;
        this.headers = headers;
        this.colorAccent = Utils.colorToHex(context, R.color.colorAccent);
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

        switch (item.getType()) {
            case BOOLEAN:
                spinner.setVisibility(View.INVISIBLE);
                editText.setVisibility(View.INVISIBLE);
                toggleButton.setVisibility(View.VISIBLE);

                toggleButton.setChecked(((BooleanOptionChild) item).getCurrentValue());
                toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        ((BooleanOptionChild) item).setCurrentValue(b);
                    }
                });
                break;
            case INTEGER:
                spinner.setVisibility(View.INVISIBLE);
                toggleButton.setVisibility(View.INVISIBLE);
                editText.setVisibility(View.VISIBLE);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                if (((IntegerOptionChild) item).getDefaultValue() != null)
                    editText.setHint(context.getString(R.string._default) + ": " + ((IntegerOptionChild) item).getDefaultValue());
                else
                    editText.setHint(R.string.noDefault);

                editText.setText(item.getStringValue());
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        try {
                            ((IntegerOptionChild) item).setCurrentValue(Integer.parseInt(editable.toString()));
                        } catch (Exception ex) {
                            ((IntegerOptionChild) item).setCurrentValue(null);
                        }
                    }
                });
                break;
            case STRING:
                spinner.setVisibility(View.INVISIBLE);
                toggleButton.setVisibility(View.INVISIBLE);
                editText.setVisibility(View.VISIBLE);
                editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                if (!((StringOptionChild) item).getDefaultValue().isEmpty())
                    editText.setHint(context.getString(R.string._default) + ": " + ((StringOptionChild) item).getDefaultValue());
                else
                    editText.setHint(R.string.noDefault);

                editText.setText(item.getStringValue());
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        ((StringOptionChild) item).setCurrentValue(editable.toString());
                    }
                });
                break;
            case MULTIPLE:
                toggleButton.setVisibility(View.INVISIBLE);
                editText.setVisibility(View.INVISIBLE);
                spinner.setVisibility(View.VISIBLE);

                spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, ((MultipleOptionChild) item).getPossibleValues()));
                spinner.setSelection(((MultipleOptionChild) item).getPossibleValues().indexOf(((MultipleOptionChild) item).getCurrentValue()));
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        ((MultipleOptionChild) item).setCurrentValue(adapterView.getAdapter().getItem(i).toString());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        ((MultipleOptionChild) item).setCurrentValue(((MultipleOptionChild) item).getDefaultValue());
                    }
                });
                break;
        }

        // TODO: Parse ::code-block (--header)
        System.out.println(colorAccent);
        ((TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionDesc)).setText(Html.fromHtml(item.getDescription().replaceAll("``(\\S*)``", "<b>$1</b>").replaceAll(":option:`(\\S*)`", "<font color='#" + colorAccent + "'>$1</font>")));

        return convertView;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup viewGroup) {
        convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.option_header, null);
        OptionHeader item = getGroup(groupPosition);

        ((TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionText)).setText(String.format("%s: ", item.getOptionName()));
        ((TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionTextVal)).setText(item.getOptionStringValue());
        TextView optionCMD = (TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionFull);
        optionCMD.setText(item.getOptionCommandLine());
        if (item.needRestart()) optionCMD.setTextColor(Color.RED);

        return convertView;
    }
}
