package com.gianlu.aria2app.Options;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorRes;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.util.List;
import java.util.Map;

public class OptionAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<OptionHeader> headers;
    private Map<OptionHeader, OptionChild> children;
    private String colorAccent;

    public OptionAdapter(Context context, @ColorRes int colorAccent, List<OptionHeader> headers, Map<OptionHeader, OptionChild> children) {
        this.context = context;
        this.colorAccent = Utils.colorToHex(context, colorAccent);
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

        EditText editText = (EditText) convertView.findViewById(R.id.moreAboutDownload_option_editText);

        editText.setVisibility(View.VISIBLE);
                editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        if (!item.getDefaultValue().isEmpty())
            editText.setHint(context.getString(R.string._default) + ": " + item.getDefaultValue());
        else
            editText.setHint(R.string.noDefault);

        editText.setText(item.getValue());
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


        // TODO: Search through options
        ((TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionDesc)).setText(Parser.formatDefinition(colorAccent, item.getDescription()));

        return convertView;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup viewGroup) {
        convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.option_header, null);
        OptionHeader item = getGroup(groupPosition);

        ((TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionText)).setText(String.format("%s: ", item.getOptionName()));
        ((TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionTextVal)).setText(item.getOptionValue());
        TextView optionCMD = (TextView) convertView.findViewById(R.id.moreAboutDownload_option_optionFull);
        optionCMD.setText(item.getOptionCommandLine());
        if (item.needRestart()) optionCMD.setTextColor(Color.RED);

        return convertView;
    }
}
