package com.gianlu.aria2app.Options;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
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
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gianlu.aria2app.R;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class OptionAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final View.OnClickListener helpClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://aria2.github.io/manual/en/html/aria2c.html#cmdoption--" + view.getTag())));
        }
    };
    private final List<OptionHeader> headers;
    private final Map<OptionHeader, OptionChild> children;
    private final boolean quickOptionsFilter;
    private final boolean hideHearts;

    OptionAdapter(Context context, List<OptionHeader> headers, Map<OptionHeader, OptionChild> children, boolean quickOptionsFilter, boolean hideHearts) {
        this.context = context;
        this.headers = headers;
        this.children = children;
        this.quickOptionsFilter = quickOptionsFilter;
        this.hideHearts = hideHearts;
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

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup viewGroup) {
        ChildViewHolder holder = new ChildViewHolder(LayoutInflater.from(context).inflate(R.layout.option_child, null));
        final OptionChild item = getChild(groupPosition, childPosition);

        holder.help.setTag(getGroup(groupPosition).getOptionLong());
        holder.help.setOnClickListener(helpClick);

        switch (item.getType()) {
            case BOOLEAN:
                holder.optionEditText.setVisibility(View.GONE);
                holder.optionSpinner.setVisibility(View.GONE);
                holder.optionToggle.setVisibility(View.VISIBLE);

                holder.optionToggle.setChecked(Boolean.parseBoolean(item.getValue()));
                holder.optionToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        item.setCurrentValue(String.valueOf(b));
                    }
                });
                break;
            case PATH_DIR:
            case PATH_FILE:
                holder.optionEditText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
            case STRING:
                holder.optionEditText.setVisibility(View.VISIBLE);
                holder.optionSpinner.setVisibility(View.GONE);
                holder.optionToggle.setVisibility(View.GONE);

                holder.optionEditText.addTextChangedListener(new TextWatcher() {
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
                holder.optionEditText.setText(item.getValue());
                holder.optionEditText.setHint(item.getDefaultValue().isEmpty() ? context.getString(R.string.noDefault) : context.getString(R.string._default) + ": " + item.getDefaultValue());
                break;
            case MULTICHOICHE:
                holder.optionEditText.setVisibility(View.GONE);
                holder.optionSpinner.setVisibility(View.VISIBLE);
                holder.optionToggle.setVisibility(View.GONE);

                holder.optionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        item.setCurrentValue(adapterView.getItemAtPosition(i).toString());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        item.setCurrentValue(null);
                    }
                });
                if (item.getValues() != null)
                    holder.optionSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, item.getValues()));
                break;
        }

        return holder.rootView;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup viewGroup) {
        final GroupViewHolder holder = new GroupViewHolder(LayoutInflater.from(context).inflate(R.layout.option_header, null));
        final OptionHeader item = getGroup(groupPosition);

        holder.optionLong.setText(String.format("%s: ", item.getOptionLong()));
        holder.optionValue.setText(item.getOptionValue());
        holder.optionShort.setText(item.getOptionShort());

        if (quickOptionsFilter || hideHearts) {
            holder.toggleQuick.setVisibility(View.GONE);
        } else {
            holder.toggleQuick.setVisibility(View.VISIBLE);
            holder.toggleQuick.setFocusable(false);

            holder.toggleQuick.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    Set<String> quickOptions = preferences.getStringSet("a2_quickOptions", new HashSet<String>());

                    item.setQuick(!item.isQuick());
                    if (item.isQuick()) {
                        holder.toggleQuick.setImageResource(R.drawable.ic_favorite_black_48dp);
                        quickOptions.add(item.getOptionLong());
                    } else {
                        holder.toggleQuick.setImageResource(R.drawable.ic_favorite_border_black_48dp);
                        quickOptions.remove(item.getOptionLong());
                    }

                    preferences.edit().putStringSet("a2_quickOptions", quickOptions).apply();
                }
            });

            if (item.isQuick())
                holder.toggleQuick.setImageResource(R.drawable.ic_favorite_black_48dp);
            else
                holder.toggleQuick.setImageResource(R.drawable.ic_favorite_border_black_48dp);
        }

        return holder.rootView;
    }

    private class GroupViewHolder {
        final ImageButton toggleQuick;
        final TextView optionLong;
        final TextView optionShort;
        final TextView optionValue;
        private final View rootView;

        GroupViewHolder(View rootView) {
            this.rootView = rootView;
            toggleQuick = (ImageButton) rootView.findViewById(R.id.optionHeader_toggleQuick);
            optionLong = (TextView) rootView.findViewById(R.id.optionHeader_optionLong);
            optionShort = (TextView) rootView.findViewById(R.id.optionHeader_optionShort);
            optionValue = (TextView) rootView.findViewById(R.id.optionHeader_optionValue);
        }
    }

    private class ChildViewHolder {
        final ToggleButton optionToggle;
        final Spinner optionSpinner;
        final EditText optionEditText;
        final ImageButton help;
        private final View rootView;

        ChildViewHolder(View rootView) {
            this.rootView = rootView;
            optionSpinner = (Spinner) rootView.findViewById(R.id.optionChild_spinner);
            optionToggle = (ToggleButton) rootView.findViewById(R.id.optionChild_toggle);
            optionEditText = (EditText) rootView.findViewById(R.id.optionChild_editText);
            help = (ImageButton) rootView.findViewById(R.id.optionChild_help);
        }
    }
}
